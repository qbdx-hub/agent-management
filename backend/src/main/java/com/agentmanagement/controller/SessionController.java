package com.agentmanagement.controller;

import com.agentmanagement.common.BusinessException;
import com.agentmanagement.common.BusinessException;
import com.agentmanagement.common.PageResult;
import com.agentmanagement.common.Result;
import com.agentmanagement.entity.Agent;
import com.agentmanagement.entity.AgentToolBinding;
import com.agentmanagement.entity.ErrorLog;
import com.agentmanagement.entity.Message;
import com.agentmanagement.entity.Session;
import com.agentmanagement.entity.Tool;
import com.agentmanagement.form.SendMessageForm;
import com.agentmanagement.form.SessionCreateForm;
import com.agentmanagement.entity.CostRecord;
import com.agentmanagement.entity.User;
import com.agentmanagement.mapper.AgentMapper;
import com.agentmanagement.mapper.AgentToolBindingMapper;
import com.agentmanagement.mapper.CostRecordMapper;
import com.agentmanagement.mapper.ErrorLogMapper;
import com.agentmanagement.mapper.ToolMapper;
import com.agentmanagement.mapper.UserMapper;
import com.agentmanagement.mapper.MessageMapper;
import com.agentmanagement.mapper.SessionMapper;
import com.agentmanagement.security.SecurityUtils;
import com.agentmanagement.service.AiService;
import com.agentmanagement.service.builtin.BuiltinToolResult;
import com.agentmanagement.service.builtin.BuiltinToolService;
import com.agentmanagement.service.BudgetService;
import com.agentmanagement.service.RetrievalService;
import com.agentmanagement.service.SessionService;
import com.agentmanagement.service.ToolService;
import com.agentmanagement.vo.SessionDetailVO;
import com.agentmanagement.vo.SessionSummaryVO;
import com.agentmanagement.vo.ToolTestResultVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.validation.Valid;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 会话 RESTful 接口。
 * sendMessage 返回 SSE 流式响应（text/event-stream）。
 */
@Slf4j
@RestController
public class SessionController {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private AgentMapper agentMapper;

    @Autowired
    private AiService aiService;

    @Autowired
    private CostRecordMapper costRecordMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RetrievalService retrievalService;

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private ErrorLogMapper errorLogMapper;

    @Autowired
    private AgentToolBindingMapper agentToolBindingMapper;

    @Autowired
    private ToolMapper toolMapper;

    @Autowired
    private ToolService toolService;

    @Autowired
    private BuiltinToolService builtinToolService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 阶段2 上下文经济学：历史消息 token 预算（估算值），超出部分压缩成前情提要 */
    private static final long HISTORY_BUDGET_TOKENS = 24_000L;

    /** 触发一次 LLM 压缩所需的最小被挤出消息数（太少不值得一次压缩调用） */
    private static final int COMPACTION_MIN_DROPPED = 4;

    /** 对话推流专用线程池（见 AsyncConfig#chatExecutor） */
    @Autowired
    @Qualifier("chatExecutor")
    private Executor chatExecutor;

    /** POST /agents/{agentId}/sessions —— 创建会话 */
    @PostMapping("/agents/{agentId}/sessions")
    public Result<Map<String, Long>> create(@PathVariable("agentId") Long agentId,
                                            @RequestBody(required = false) SessionCreateForm form) {
        if (form == null) {
            form = new SessionCreateForm();
        }
        Long sessionId = sessionService.createSession(agentId, form);
        Map<String, Long> data = new HashMap<>();
        data.put("sessionId", sessionId);
        return Result.success(data);
    }

    /** GET /agents/{agentId}/sessions —— 分页列表 */
    @GetMapping("/agents/{agentId}/sessions")
    public Result<PageResult<SessionSummaryVO>> list(@PathVariable("agentId") Long agentId,
                                                     @RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int pageSize) {
        return Result.success(sessionService.pageSessions(agentId, page, pageSize));
    }

    /** GET /sessions/{sessionId}/messages —— 会话详情+消息列表 */
    @GetMapping("/sessions/{sessionId}/messages")
    public Result<SessionDetailVO> messages(@PathVariable("sessionId") Long sessionId) {
        return Result.success(sessionService.getSessionMessages(sessionId));
    }

    /**
     * POST /sessions/{sessionId}/messages —— 发送消息（SSE 流式返回 AI 回复）
     * 1. 保存用户消息到数据库
     * 2. 构建上下文（system prompt + 知识检索 + 历史消息）
     * 3. 调用 AI 流式回复，通过 SSE 推送给前端
     */
    @PostMapping(value = "/sessions/{sessionId}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendMessageSse(@PathVariable("sessionId") Long sessionId,
                                     @Valid @RequestBody SendMessageForm form) {
        Long workspaceId = SecurityUtils.currentWorkspaceId();
        Long currentUserId = SecurityUtils.currentUserId();

        // 1. 校验会话
        Session session = sessionMapper.selectById(sessionId);
        if (session == null || !workspaceId.equals(session.getWorkspaceId())
                || !currentUserId.equals(session.getCreatedBy())) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().name("error").data("{\"error\":\"会话不存在\"}"));
                emitter.complete();
            } catch (Exception ignored) {}
            return emitter;
        }

        // 2. 保存用户消息
        Message userMsg = new Message();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(form.getContent());
        userMsg.setMode(form.getMode() != null ? form.getMode() : session.getExecutionMode());
        userMsg.setAttachments(form.getAttachments());
        userMsg.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(userMsg);

        // 更新会话统计（原子自增，避免并发消息 read-modify-write 互相覆盖）
        sessionMapper.update(null, new LambdaUpdateWrapper<Session>()
                .eq(Session::getId, sessionId)
                .setSql("message_count = IFNULL(message_count, 0) + 1"));

        // 3. 获取 Agent 配置
        Agent agent = agentMapper.selectById(session.getAgentId());
        if (agent == null || agent.getAiBaseUrl() == null || agent.getAiApiKey() == null) {
            // Agent 未配置 AI，返回模拟回复
            return sendMockResponse(form.getContent(), sessionId);
        }

        // 4. 预算熔断检查：超支时拒绝发起 AI 调用，经 SSE error 事件告知前端
        try {
            budgetService.assertBudgetAllowed(workspaceId, currentUserId, agent.getId());
        } catch (BusinessException e) {
            recordErrorLog(workspaceId, agent, sessionId, e);
            SseEmitter refused = new SseEmitter();
            try {
                refused.send(SseEmitter.event().name("error")
                        .data("{\"error\":\"" + escapeJson(shortErrorMessage(e)) + "\"}"));
                refused.complete();
            } catch (Exception ignored) {}
            return refused;
        }

        // 3.5 加载 Agent 启用中的 API 工具并构建 Function Calling 定义
        //     （必须在请求线程校验工作空间；chatExecutor 异步线程无 SecurityContext）
        List<Tool> enabledTools = loadEnabledTools(agent.getId(), workspaceId);
        final List<Map<String, Object>> openAiTools = enabledTools.isEmpty()
                ? null : buildOpenAiTools(enabledTools);
        final Map<String, Tool> toolByName = new HashMap<>();
        for (Tool t : enabledTools) {
            toolByName.put(t.getName(), t);
        }

        // 4. 创建 SSE emitter（5 分钟超时，覆盖 OkHttp 300s 读超时；注册生命周期回调，超时/断连可观测）
        SseEmitter emitter = new SseEmitter(300_000L);
        emitter.onTimeout(() -> log.warn("SSE 超时中断: sessionId={}", sessionId));
        emitter.onError(t -> log.warn("SSE 连接异常: sessionId={}, {}", sessionId, t.getMessage()));
        emitter.onCompletion(() -> log.debug("SSE 完成: sessionId={}", sessionId));

        // 发送 thinking 事件
        try {
            emitter.send(SseEmitter.event().name("thinking")
                    .data("{\"content\":\"正在思考...\"}"));
            log.info("SSE 已发送 thinking 事件");
        } catch (Exception e) {
            log.error("SSE thinking 发送失败", e);
        }

        // 使用专用线程池异步处理（buildMessages 移到线程内，异常可被捕获）
        // 用户授权「沙箱外运行」：本次消息的文件/命令工具作用于真实文件系统（默认限会话沙箱内）
        final boolean outsideSandbox = Boolean.TRUE.equals(form.getOutsideSandbox());
        chatExecutor.execute(() -> {
            long startMs = System.currentTimeMillis();
            try {
                // 5. 构建消息上下文（含知识检索 + 历史压缩；身份在请求线程取出后显式传入，子线程无 SecurityContext）
                //    compactionTokens[0] 回传压缩调用自身消耗的 token，计入本次消息费用
                long[] compactionTokens = new long[1];
                List<Map<String, Object>> messages = buildMessages(agent, session, form.getContent(),
                        currentUserId, workspaceId, compactionTokens);
                if (outsideSandbox) {
                    // 告知模型本次已授权沙箱外执行，可用绝对路径操作真实文件系统（含安全提醒）
                    injectOutsideSandboxNotice(messages);
                }

                // 6. 流式调用 AI（Function Calling 多轮）：模型请求工具 → 真实执行 → 结果回传 → 继续生成
                String model = agent.getAiModel() != null ? agent.getAiModel() : "gpt-4o";
                long inputTokens = compactionTokens[0];
                long outputTokens = 0;
                long totalTokens = compactionTokens[0];
                long cachedTokens = 0;
                boolean usageSeen = compactionTokens[0] > 0;
                // 工具轮次 = Agent 配置的最大迭代次数（默认 3，不再封顶）；失控防护改由
                // 「轮次上限 + 累计 token 预算」双保险承担（阶段0：maxIterations 语义修正）
                final int maxRounds = 1 + (agent.getMaxIterations() != null ? Math.max(agent.getMaxIterations(), 1) : 3);
                final long loopTokenBudget = 100_000L;
                // 阶段0：反思接线 —— 工具失败时注入反思提示，reflectionDepth 限制每次消息最多反思次数
                final boolean reflectionOn = agent.getReflectionEnabled() != null && agent.getReflectionEnabled() == 1;
                final int reflectionDepth = agent.getReflectionDepth() != null && agent.getReflectionDepth() > 0
                        ? agent.getReflectionDepth() : 2;
                int reflectionUsed = 0;
                AiService.ChatCompletionResult aiResult = null;
                boolean toolRoundExhausted = false;
                for (int round = 0; round < maxRounds; round++) {
                    // 阶段2 工具结果老化：早期工具结果精简保留，最新 2 条保持全量
                    if (round > 0) {
                        ageToolResults(messages, 2);
                    }
                    aiResult = aiService.chatCompletionStream(
                            agent.getAiBaseUrl(), agent.getAiApiKey(), model, messages, openAiTools, emitter);
                    if (aiResult.getPromptTokens() != null) { inputTokens += aiResult.getPromptTokens(); usageSeen = true; }
                    if (aiResult.getCompletionTokens() != null) outputTokens += aiResult.getCompletionTokens();
                    if (aiResult.getTotalTokens() != null) totalTokens += aiResult.getTotalTokens();
                    if (aiResult.getCachedTokens() != null) cachedTokens += aiResult.getCachedTokens();

                    List<AiService.ToolCall> toolCalls = aiResult.getToolCalls();
                    if (toolCalls == null || toolCalls.isEmpty()) {
                        break;
                    }

                    // 累计 token 预算熔断：不再发起新的工具轮，强制直接作答
                    if (totalTokens > loopTokenBudget) {
                        log.warn("会话循环累计 token {} 超预算 {}，强制收敛作答: sessionId={}",
                                totalTokens, loopTokenBudget, sessionId);
                        toolRoundExhausted = true;
                        break;
                    }

                    // 模型的工具调用请求并入上下文（assistant 消息带 tool_calls）
                    List<Map<String, Object>> tcPayload = new ArrayList<>();
                    for (AiService.ToolCall tc : toolCalls) {
                        Map<String, Object> fn = new HashMap<>();
                        fn.put("name", tc.getName());
                        fn.put("arguments", tc.getArguments());
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", tc.getId());
                        item.put("type", "function");
                        item.put("function", fn);
                        tcPayload.add(item);
                    }
                    Map<String, Object> assistantToolMsg = new HashMap<>();
                    assistantToolMsg.put("role", "assistant");
                    assistantToolMsg.put("content", aiResult.getContent() != null ? aiResult.getContent() : "");
                    assistantToolMsg.put("tool_calls", tcPayload);
                    messages.add(assistantToolMsg);

                    // 逐个真实执行工具，结果以 role=tool 消息回传，并向前端推 tool_call 事件
                    boolean anyToolFailed = false;
                    for (AiService.ToolCall tc : toolCalls) {
                        boolean callOk = executeAndReportToolCall(tc, toolByName, workspaceId, agent, sessionId,
                                messages, emitter, outsideSandbox);
                        anyToolFailed = anyToolFailed || !callOk;
                    }

                    // 阶段0 反思接线：有工具失败且反思次数未达 depth 时注入反思提示，引导模型修正而非重复失败
                    if (reflectionOn && anyToolFailed && reflectionUsed < reflectionDepth
                            && round < maxRounds - 1) {
                        reflectionUsed++;
                        Map<String, Object> reflect = new HashMap<>();
                        reflect.put("role", "system");
                        reflect.put("content", "反思提示：上一步工具调用失败。请反思失败原因（参数错误？路径不存在？工具不适用？），"
                                + "修正参数重试、换用其他工具完成，或如实向用户说明。不要重复完全相同的失败调用。");
                        messages.add(reflect);
                        log.info("注入反思提示: sessionId={}, round={}, reflectionUsed={}/{}",
                                sessionId, round, reflectionUsed, reflectionDepth);
                    }

                    if (round == maxRounds - 1) {
                        toolRoundExhausted = true;
                    }
                }

                // 工具轮次用尽模型仍要调工具：不带 tools 再补一轮，强制直接作答
                if (toolRoundExhausted) {
                    log.warn("工具调用轮次达上限，跳过工具直接生成回复: sessionId={}", sessionId);
                    aiResult = aiService.chatCompletionStream(
                            agent.getAiBaseUrl(), agent.getAiApiKey(), model, messages, null, emitter);
                    if (aiResult.getPromptTokens() != null) { inputTokens += aiResult.getPromptTokens(); usageSeen = true; }
                    if (aiResult.getCompletionTokens() != null) outputTokens += aiResult.getCompletionTokens();
                    if (aiResult.getTotalTokens() != null) totalTokens += aiResult.getTotalTokens();
                    if (aiResult.getCachedTokens() != null) cachedTokens += aiResult.getCachedTokens();
                }

                String reply = aiResult.getContent() != null ? aiResult.getContent() : "";
                long latencyMs = System.currentTimeMillis() - startMs;

                // 7. token 用量（usage 缺失时按字符数估算兜底）与费用
                if (!usageSeen) {
                    inputTokens = (long) (form.getContent().length() * 1.5);
                    outputTokens = (long) (reply.length() * 1.2);
                    totalTokens = inputTokens + outputTokens;
                } else if (totalTokens == 0) {
                    totalTokens = inputTokens + outputTokens;
                }

                // 计算费用：cached 部分按缓存价，剩余 input 按正常价，output 按输出价
                BigDecimal cost = calculateCost(agent, inputTokens, outputTokens, cachedTokens);
                log.info("AI 回复完成: sessionId={}, latency={}ms, tokens={}({}+{}), cost=${}",
                        sessionId, latencyMs, totalTokens, inputTokens, outputTokens, cost);

                // 8. 保存 AI 回复到数据库
                Message assistantMsg = new Message();
                assistantMsg.setSessionId(sessionId);
                assistantMsg.setRole("assistant");
                assistantMsg.setContent(reply);
                assistantMsg.setMode(session.getExecutionMode());
                assistantMsg.setTokenInput(inputTokens);
                assistantMsg.setTokenOutput(outputTokens);
                assistantMsg.setTokenTotal(totalTokens);
                assistantMsg.setTokenCost(cost);
                assistantMsg.setCreatedAt(LocalDateTime.now());
                messageMapper.insert(assistantMsg);

                // 写入 cost_record
                try {
                    CostRecord costRecord = new CostRecord();
                    costRecord.setWorkspaceId(workspaceId);
                    costRecord.setAgentId(agent.getId());
                    costRecord.setAgentName(agent.getName());
                    costRecord.setSessionId(sessionId);
                    costRecord.setModelProvider(agent.getModelProvider());
                    costRecord.setModelName(model);
                    costRecord.setTokenInput(inputTokens);
                    costRecord.setTokenOutput(outputTokens);
                    costRecord.setTotalTokens(totalTokens);
                    costRecord.setCost(cost);
                    costRecord.setUserId(currentUserId);
                    // 冗余用户名
                    try {
                        User user = userMapper.selectById(currentUserId);
                        costRecord.setUserName(user != null ? user.getUsername() : null);
                    } catch (Exception ignored) {}
                    costRecord.setRecordedAt(LocalDateTime.now());
                    costRecordMapper.insert(costRecord);
                    log.info("cost_record 写入成功: agentId={}, cost=${}, tokens={}", agent.getId(), cost, totalTokens);
                } catch (Exception ce) {
                    log.warn("写入 cost_record 失败", ce);
                }

                // 8.1 预算扣减（匹配 global/user/agent 生效预算，累加 current_amount；失败不影响对话）
                try {
                    budgetService.deductAfterCall(workspaceId, currentUserId, agent.getId(), cost);
                } catch (Exception be) {
                    log.warn("预算扣减失败: sessionId={}", sessionId, be);
                }

                // 9. 原子更新会话统计与最近一次延迟（read-modify-write 会并发丢更新，改为 SQL 自增）
                //    totalTokens 为 long、cost.toPlainString() 为纯数字字符串，拼接无注入风险
                sessionMapper.update(null, new LambdaUpdateWrapper<Session>()
                        .eq(Session::getId, sessionId)
                        .setSql("message_count = IFNULL(message_count, 0) + 1")
                        .setSql("total_tokens = IFNULL(total_tokens, 0) + " + totalTokens)
                        .setSql("total_cost = IFNULL(total_cost, 0) + " + cost.toPlainString())
                        .set(Session::getLatency, (int) Math.min(latencyMs, Integer.MAX_VALUE)));

                // 10. 原子更新 Agent 统计
                agentMapper.update(null, new LambdaUpdateWrapper<Agent>()
                        .eq(Agent::getId, agent.getId())
                        .setSql("total_tokens = IFNULL(total_tokens, 0) + " + totalTokens)
                        .setSql("total_cost = IFNULL(total_cost, 0) + " + cost.toPlainString())
                        .setSql("total_messages = IFNULL(total_messages, 0) + 1"));

                // 11. done 事件（携带真实 usage 与费用，前端不再自行编造）
                emitter.send(SseEmitter.event().name("done")
                        .data("{\"messageId\":" + assistantMsg.getId()
                                + ",\"usage\":{\"input\":" + inputTokens + ",\"output\":" + outputTokens
                                + ",\"total\":" + totalTokens + ",\"cached\":" + cachedTokens + "}"
                                + ",\"cost\":" + cost.doubleValue() + "}"));
                log.info("SSE 已发送 done 事件, messageId={}", assistantMsg.getId());
                emitter.complete();

            } catch (Exception e) {
                log.error("AI 回复异常: sessionId={}", sessionId, e);
                // 错误落库（供监控页错误列表与告警评估使用）
                recordErrorLog(workspaceId, agent, sessionId, e);
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data("{\"error\":\"" + escapeJson(shortErrorMessage(e)) + "\"}"));
                    emitter.complete();
                } catch (Exception ignored) {}
            }
        });

        return emitter;
    }

    /** POST /sessions/{sessionId}/stop —— 停止会话 */
    @PostMapping("/sessions/{sessionId}/stop")
    public Result<Void> stop(@PathVariable("sessionId") Long sessionId) {
        sessionService.stopSession(sessionId);
        return Result.success();
    }

    /** POST /sessions/{sessionId}/continue —— 继续会话（预留接口） */
    @PostMapping("/sessions/{sessionId}/continue")
    public Result<Void> continueSession(@PathVariable("sessionId") Long sessionId) {
        return Result.success();
    }

    /** DELETE /sessions/{sessionId} —— 删除会话 */
    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> delete(@PathVariable("sessionId") Long sessionId) {
        sessionService.deleteSession(sessionId);
        return Result.success();
    }

    // ===== 内部方法 =====

    /**
     * 构建发送给 AI 的消息数组：system prompt（提示词变量替换 + 知识检索 + 前情提要 + 输出 Schema）
     * + token 预算内的历史消息（超预算旧消息压缩成前情提要，阶段2 上下文经济学）。
     * 值类型为 Object 以兼容 Function Calling 的 tool_calls / role=tool 消息形态。
     *
     * @param compactionTokensOut 长度为 1 的出参，回传本次压缩调用消耗的 token（计入消息费用）
     */
    private List<Map<String, Object>> buildMessages(Agent agent, Session session, String userQuery,
                                                    Long userId, Long workspaceId, long[] compactionTokensOut) {
        List<Map<String, Object>> messages = new ArrayList<>();

        // 1. System prompt（阶段0：{{变量}} 替换）
        StringBuilder systemPrompt = new StringBuilder();
        if (agent.getSystemPrompt() != null && !agent.getSystemPrompt().isEmpty()) {
            systemPrompt.append(agent.getSystemPrompt());
            substitutePromptVariables(systemPrompt, agent.getPromptVariables());
        }

        // 2. 知识检索
        log.info("Agent knowledgeBaseIds={}", agent.getKnowledgeBaseIds());
        if (agent.getKnowledgeBaseIds() != null && !agent.getKnowledgeBaseIds().isEmpty()) {
            log.info("开始知识检索, kbIds={}", agent.getKnowledgeBaseIds());
            List<String> contextParts = new ArrayList<>();
            for (Object rawId : agent.getKnowledgeBaseIds()) {
                Long kbId = rawId instanceof Number ? ((Number) rawId).longValue() : Long.valueOf(rawId.toString());
                try {
                    log.info("检索知识库: kbId={}, query={}", kbId, userQuery);
                    List<RetrievalService.SearchResult> results = retrievalService.search(kbId, userQuery, 3, userId, workspaceId);
                    log.info("检索结果: kbId={}, count={}", kbId, results.size());
                    for (RetrievalService.SearchResult r : results) {
                        contextParts.add(r.getContent());
                    }
                } catch (Exception e) {
                    log.warn("知识检索失败: kbId={}", kbId, e);
                }
            }
            if (!contextParts.isEmpty()) {
                systemPrompt.append("\n\n## 参考知识\n以下是从知识库中检索到的相关内容，请参考回答：\n");
                for (int i = 0; i < contextParts.size(); i++) {
                    systemPrompt.append("\n### 片段 ").append(i + 1).append("\n").append(contextParts.get(i));
                }
            }
        }

        // 3. 历史：token 预算内从最新往前保留；被挤出的旧消息压缩成前情提要（阶段2）
        int window = agent.getWorkingWindow() != null ? agent.getWorkingWindow() : 10;
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getSessionId, session.getId())
                .in(Message::getRole, Arrays.asList("user", "assistant"))
                .orderByAsc(Message::getCreatedAt);
        if (session.getSummarizedMessageId() != null) {
            // 已被前情提要覆盖的历史不再重复进入上下文
            wrapper.gt(Message::getId, session.getSummarizedMessageId());
        }
        List<Message> history = messageMapper.selectList(wrapper);

        int start = history.size();
        long acc = 0;
        int kept = 0;
        while (start > 0 && kept < window * 2) {
            long t = estimateTokens(history.get(start - 1).getContent());
            if (kept > 0 && acc + t > HISTORY_BUDGET_TOKENS) {
                break;
            }
            start--;
            acc += t;
            kept++;
        }

        String freshSummary = null;
        List<Message> dropped = history.subList(0, start);
        boolean hasAi = agent.getAiBaseUrl() != null && agent.getAiApiKey() != null;
        if (dropped.size() >= COMPACTION_MIN_DROPPED && hasAi) {
            long[] tok = new long[1];
            freshSummary = compactHistory(agent, session, dropped, tok);
            compactionTokensOut[0] += tok[0];
            log.info("历史压缩完成: sessionId={}, dropped={}, summaryChars={}, tokens={}",
                    session.getId(), dropped.size(), freshSummary != null ? freshSummary.length() : 0, tok[0]);
        }

        String activeSummary = freshSummary != null ? freshSummary
                : (session.getContextSummary() != null && !session.getContextSummary().isEmpty()
                        ? session.getContextSummary() : null);
        if (activeSummary != null) {
            systemPrompt.append("\n\n## 前情提要\n以下是本会话更早对话的压缩摘要（细节可能省略，需要时请向用户确认）：\n")
                    .append(activeSummary);
        }

        // 4. 输出 Schema（阶段0：outputSchema 接线）
        appendOutputSchema(systemPrompt, agent.getOutputSchema());

        if (systemPrompt.length() > 0) {
            Map<String, Object> sysMsg = new HashMap<>();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt.toString());
            messages.add(sysMsg);
        }

        // 5. 预算内历史
        for (int i = start; i < history.size(); i++) {
            Message m = history.get(i);
            Map<String, Object> msg = new HashMap<>();
            msg.put("role", m.getRole());
            msg.put("content", m.getContent());
            messages.add(msg);
        }

        // 6. 当前用户消息（已经在 history 中了，不需要重复添加）
        // 检查最后一条是否已经是当前消息（content 可能为 null，用 Objects.equals 防 NPE）
        Object lastContent = messages.isEmpty() ? null : messages.get(messages.size() - 1).get("content");
        if (!Objects.equals(lastContent, userQuery)) {
            Map<String, Object> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userQuery);
            messages.add(userMsg);
        }

        return messages;
    }

    // ===== 阶段0/阶段2 辅助 =====

    /**
     * 沙箱外运行授权告知：插入 system 消息让模型知道本次文件/命令工具不再限会话沙箱，
     * 可用绝对路径操作真实文件系统、命令工作目录为服务器进程目录，并附安全提醒。
     */
    private void injectOutsideSandboxNotice(List<Map<String, Object>> messages) {
        String cwd = Paths.get("").toAbsolutePath().normalize().toString();
        Map<String, Object> notice = new HashMap<>();
        notice.put("role", "system");
        notice.put("content", "## 执行环境授权\n用户已授权本次消息在【沙箱外】运行：文件与命令工具不再限制在会话沙箱内，"
                + "可读写真实文件系统（支持绝对路径），命令执行的工作目录为服务器目录：" + cwd
                + "。\n请谨慎操作：只执行与任务相关的读写与命令，不要删除或覆盖用户未明确要求的文件。");
        messages.add(0, notice);
        log.info("沙箱外运行已授权: cwd={}", cwd);
    }

    /** 阶段0 提示词变量：{{key}} 替换为变量值（字段命名兼容 name/key，取值顺序 value → content → defaultValue） */
    private void substitutePromptVariables(StringBuilder sb, List<Map<String, Object>> variables) {
        if (variables == null) {
            return;
        }
        for (Map<String, Object> v : variables) {
            if (v == null) {
                continue;
            }
            String key = v.get("key") != null ? v.get("key").toString()
                    : (v.get("name") != null ? v.get("name").toString() : null);
            Object val = v.get("value") != null ? v.get("value")
                    : (v.get("content") != null ? v.get("content") : v.get("defaultValue"));
            if (key == null || key.isEmpty() || val == null) {
                continue;
            }
            String placeholder = "{{" + key + "}}";
            int idx;
            while ((idx = sb.indexOf(placeholder)) != -1) {
                sb.replace(idx, idx + placeholder.length(), val.toString());
            }
        }
    }

    /** 阶段0 outputSchema：附加 JSON Schema 输出约束到 system prompt */
    private void appendOutputSchema(StringBuilder sb, Map<String, Object> schema) {
        if (schema == null || schema.isEmpty()) {
            return;
        }
        try {
            sb.append("\n\n## 输出格式要求\n你的最终回答必须是严格符合以下 JSON Schema 的合法 JSON，"
                    + "不要输出 JSON 之外的任何解释文字：\n")
                    .append(objectMapper.writeValueAsString(schema));
        } catch (Exception e) {
            log.warn("outputSchema 序列化失败，忽略输出约束", e);
        }
    }

    /**
     * 历史压缩：被挤出预算的旧消息（连同上一版提要）交给 LLM 压缩成「前情提要」，
     * 落库（session.context_summary / summarized_message_id）供后续轮次复用，避免重复压缩。
     */
    private String compactHistory(Agent agent, Session session, List<Message> dropped, long[] tokensOut) {
        try {
            StringBuilder convo = new StringBuilder();
            if (session.getContextSummary() != null && !session.getContextSummary().isEmpty()) {
                convo.append("【此前提要】\n").append(session.getContextSummary()).append("\n\n【新增对话】\n");
            }
            for (Message m : dropped) {
                convo.append("user".equals(m.getRole()) ? "用户：" : "助手：")
                        .append(abbreviateText(m.getContent(), 4000)).append('\n');
            }
            List<Map<String, String>> msgs = new ArrayList<>();
            Map<String, String> sys = new HashMap<>();
            sys.put("role", "system");
            sys.put("content", "你是对话压缩器。把输入的对话历史压缩成一份简洁的「前情提要」，必须保留：用户的目标与约束、"
                    + "已完成的步骤及其结果、关键文件路径/数据/名称、未完成事项。用简练的中文陈述句直接输出提要正文，"
                    + "不要任何开场白，500 字以内。");
            msgs.add(sys);
            Map<String, String> usr = new HashMap<>();
            usr.put("role", "user");
            usr.put("content", convo.toString());
            msgs.add(usr);

            String model = agent.getAiModel() != null ? agent.getAiModel() : "gpt-4o";
            AiService.ChatCompletionResult r = aiService.chatCompletion(
                    agent.getAiBaseUrl(), agent.getAiApiKey(), model, msgs);
            String summary = r.getContent() != null ? r.getContent().trim() : "";
            if (summary.isEmpty()) {
                return null;
            }
            long tokens = (r.getPromptTokens() != null ? r.getPromptTokens() : 0)
                    + (r.getCompletionTokens() != null ? r.getCompletionTokens() : 0);
            tokensOut[0] += tokens;
            summary = abbreviateText(summary, 8000);

            Long untilId = dropped.get(dropped.size() - 1).getId();
            sessionMapper.update(null, new LambdaUpdateWrapper<Session>()
                    .eq(Session::getId, session.getId())
                    .set(Session::getContextSummary, summary)
                    .set(Session::getSummarizedMessageId, untilId));
            // 回写内存对象：本轮直接生效，后续轮次也不再重复压缩
            session.setContextSummary(summary);
            session.setSummarizedMessageId(untilId);
            return summary;
        } catch (Exception e) {
            // 压缩失败不阻断对话：退化为原行为（旧消息本次缺席）
            log.warn("历史压缩失败，本次跳过: sessionId={}", session.getId(), e);
            return null;
        }
    }

    /** 阶段2 工具结果老化：除最近 keepRecent 条外，更早的 role=tool 结果精简到 1500 字符 */
    private void ageToolResults(List<Map<String, Object>> messages, int keepRecent) {
        int seen = 0;
        for (int i = messages.size() - 1; i >= 0; i--) {
            Map<String, Object> m = messages.get(i);
            if (!"tool".equals(m.get("role"))) {
                continue;
            }
            seen++;
            if (seen > keepRecent) {
                Object c = m.get("content");
                if (c != null && c.toString().length() > 1500) {
                    m.put("content", c.toString().substring(0, 1500)
                            + "\n…[早期工具结果已精简，如需完整信息请重新调用工具]");
                }
            }
        }
    }

    /** 粗估 token 数：CJK ≈0.7 token/字符，其他 ≈0.3（估算用途，无需精确） */
    private static long estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int cjk = 0;
        int other = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) > 0x2E80) {
                cjk++;
            } else {
                other++;
            }
        }
        return Math.max(1, (cjk * 7 + other * 3) / 10);
    }

    private String abbreviateText(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    // ===== Function Calling 辅助 =====

    /** 加载 Agent 启用中的工具：api（HTTP 工具）+ builtin（沙箱内置工具，见 BuiltinToolService） */
    private List<Tool> loadEnabledTools(Long agentId, Long workspaceId) {
        List<AgentToolBinding> bindings = agentToolBindingMapper.selectList(
                new LambdaQueryWrapper<AgentToolBinding>()
                        .eq(AgentToolBinding::getAgentId, agentId)
                        .eq(AgentToolBinding::getEnabled, 1));
        if (bindings.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> toolIds = bindings.stream()
                .map(AgentToolBinding::getToolId).collect(Collectors.toList());
        List<Tool> tools = toolMapper.selectBatchIds(toolIds);
        List<Tool> result = new ArrayList<>();
        // 工具市场全局共享：不再按工作空间过滤；api + builtin 两类可真实执行
        for (Tool t : tools) {
            if (t != null && ("api".equalsIgnoreCase(t.getType()) || "builtin".equalsIgnoreCase(t.getType()))) {
                result.add(t);
            }
        }
        return result;
    }

    /** Tool 列表 → OpenAI function calling 工具定义 [{type:"function", function:{name,description,parameters}}] */
    private List<Map<String, Object>> buildOpenAiTools(List<Tool> tools) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Tool tool : tools) {
            StringBuilder desc = new StringBuilder();
            if (tool.getDisplayName() != null && !tool.getDisplayName().isEmpty()) {
                desc.append(tool.getDisplayName());
            }
            if (tool.getDescription() != null && !tool.getDescription().isEmpty()) {
                if (desc.length() > 0) {
                    desc.append("：");
                }
                desc.append(tool.getDescription());
            }
            Map<String, Object> fn = new HashMap<>();
            fn.put("name", tool.getName());
            fn.put("description", desc.length() > 0 ? desc.toString() : tool.getName());
            fn.put("parameters", buildJsonSchema(tool.getParameters()));
            Map<String, Object> def = new HashMap<>();
            def.put("type", "function");
            def.put("function", fn);
            result.add(def);
        }
        return result;
    }

    /** tool.parameters JSON（[{name,type,required,description}]）→ JSON Schema（type:object） */
    private Map<String, Object> buildJsonSchema(List<Map<String, Object>> parameters) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        if (parameters != null) {
            for (Map<String, Object> p : parameters) {
                String name = p.get("name") != null ? p.get("name").toString() : null;
                if (name == null || name.isEmpty()) {
                    continue;
                }
                Map<String, Object> prop = new HashMap<>();
                prop.put("type", mapParamType(p.get("type")));
                if (p.get("description") != null) {
                    prop.put("description", p.get("description").toString());
                }
                properties.put(name, prop);
                if (Boolean.TRUE.equals(p.get("required"))) {
                    required.add(name);
                }
            }
        }
        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    /** 工具参数类型 → JSON Schema 类型 */
    private String mapParamType(Object type) {
        String t = type != null ? type.toString().toLowerCase() : "string";
        switch (t) {
            case "number":
                return "number";
            case "integer":
            case "int":
                return "integer";
            case "boolean":
            case "bool":
                return "boolean";
            default:
                return "string";
        }
    }

    /**
     * 执行一次模型发起的工具调用并回传结果：
     * builtin 走沙箱执行器（或授权后的真实文件系统）、api 走真实 HTTP → SSE tool_call 事件 → role=tool 消息并入上下文。
     *
     * @param outsideSandbox 用户授权的沙箱外运行标志（透传给内置工具执行器与前端事件）
     * @return 本次调用是否成功（供反思逻辑判断）
     */
    private boolean executeAndReportToolCall(AiService.ToolCall tc, Map<String, Tool> toolByName,
                                          Long workspaceId, Agent agent, Long sessionId,
                                          List<Map<String, Object>> messages, SseEmitter emitter,
                                          boolean outsideSandbox) {
        Tool tool = toolByName.get(tc.getName());
        Map<String, Object> params = parseToolArguments(tc.getArguments());

        long startMs = System.currentTimeMillis();
        boolean ok = false;
        String resultText;
        try {
            if (tool == null) {
                resultText = "工具不存在: " + tc.getName();
            } else if ("builtin".equalsIgnoreCase(tool.getType())) {
                // 内置工具：默认会话沙箱内执行；用户授权后切换到真实文件系统（文件/命令/网页），无远端 HTTP
                BuiltinToolResult br = builtinToolService.execute(tool, params, agent.getId(), sessionId, workspaceId,
                        outsideSandbox);
                ok = br.isSuccess();
                resultText = ok ? br.getOutput()
                        : (br.getErrorMessage() != null ? br.getErrorMessage() : "内置工具执行失败");
            } else {
                ToolTestResultVO tr = toolService.testTool(tool.getId(), params, workspaceId, agent.getId(), sessionId);
                ok = Boolean.TRUE.equals(tr.getSuccess());
                if (ok) {
                    resultText = tr.getMappedOutput() != null && !tr.getMappedOutput().isEmpty()
                            ? tr.getMappedOutput() : tr.getResponseBody();
                } else {
                    resultText = "HTTP " + tr.getResponseStatus() + "，调用失败";
                }
            }
        } catch (Exception te) {
            log.warn("会话工具调用失败: sessionId={}, tool={}", sessionId, tc.getName(), te);
            resultText = te.getMessage() != null ? te.getMessage() : "工具执行异常";
        }
        long durationMs = System.currentTimeMillis() - startMs;
        String truncated = resultText != null && resultText.length() > 6000
                ? resultText.substring(0, 6000) + "…" : (resultText != null ? resultText : "");

        // 前端步骤展示：工具名/参数/结果/耗时/成败（沙箱外授权时前端打「沙箱外」警示标）
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("stepId", tc.getId());
            event.put("toolName", tc.getName());
            event.put("params", params);
            event.put("success", ok);
            event.put("durationMs", durationMs);
            event.put("result", abbreviatedResult(truncated));
            if (outsideSandbox) {
                event.put("outsideSandbox", true);
            }
            if (!ok) {
                event.put("error", abbreviatedResult(truncated));
            }
            emitter.send(SseEmitter.event().name("tool_call")
                    .data(objectMapper.writeValueAsString(event)));
        } catch (Exception se) {
            log.warn("tool_call SSE 事件发送失败: sessionId={}", sessionId, se);
        }

        // role=tool 消息回传给模型（工具失败也要回传，模型可据此调整或向用户说明）
        Map<String, Object> toolMsg = new HashMap<>();
        toolMsg.put("role", "tool");
        toolMsg.put("tool_call_id", tc.getId());
        toolMsg.put("content", truncated);
        messages.add(toolMsg);
        return ok;
    }

    /** 解析模型输出的工具参数 JSON（非法/为空时返回空 Map，工具按缺省参数执行） */
    private Map<String, Object> parseToolArguments(String arguments) {
        if (arguments == null || arguments.trim().isEmpty()) {
            return new HashMap<>();
        }
        try {
            Map<String, Object> params = objectMapper.readValue(
                    arguments, new TypeReference<Map<String, Object>>() {});
            return params != null ? params : new HashMap<>();
        } catch (Exception e) {
            log.warn("工具参数 JSON 解析失败: {}", arguments, e);
            return new HashMap<>();
        }
    }

    /** SSE 事件里的结果文本截断（前端展示用；role=tool 回传用完整截断版） */
    private String abbreviatedResult(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 2000 ? s.substring(0, 2000) + "…" : s;
    }

    /** Agent 未配置 AI 时返回模拟回复 */
    private SseEmitter sendMockResponse(String userQuery, Long sessionId) {
        SseEmitter emitter = new SseEmitter(30_000L);
        chatExecutor.execute(() -> {
            try {
                emitter.send(SseEmitter.event().name("thinking")
                        .data("{\"content\":\"正在思考...\"}"));
                Thread.sleep(500);

                String reply = "您好！我是 AI Agent 助手。\n\n"
                        + "您的问题我已收到：「" + userQuery.substring(0, Math.min(userQuery.length(), 50)) + "」\n\n"
                        + "⚠️ **提示**：当前 Agent 尚未配置 AI API。请在 Agent 配置页面填写：\n"
                        + "- **Base URL**（如 https://api.openai.com/v1）\n"
                        + "- **API Key**\n"
                        + "- **模型名**（如 gpt-4o, deepseek-chat）\n\n"
                        + "配置完成后即可使用真实 AI 对话。";

                emitter.send(SseEmitter.event().name("content")
                        .data("{\"content\":\"" + escapeJson(reply) + "\"}"));

                // 保存到数据库
                Message assistantMsg = new Message();
                assistantMsg.setSessionId(sessionId);
                assistantMsg.setRole("assistant");
                assistantMsg.setContent(reply);
                assistantMsg.setCreatedAt(LocalDateTime.now());
                messageMapper.insert(assistantMsg);

                emitter.send(SseEmitter.event().name("done")
                        .data("{\"messageId\":" + assistantMsg.getId() + "}"));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data("{\"error\":\"" + escapeJson(shortErrorMessage(e)) + "\"}"));
                    emitter.complete();
                } catch (Exception ignored) {}
            }
        });
        return emitter;
    }

    /**
     * 根据 Agent 配置的 token 价格计算本次对话费用（美元）。
     * cachedTokens 按缓存价计费，剩余 inputTokens 按正常输入价计费。
     * 如 Agent 未配置价格，使用 DeepSeek-chat 默认价。
     */
    private BigDecimal calculateCost(Agent agent, long inputTokens, long outputTokens, long cachedTokens) {
        // 默认价格（DeepSeek-chat 参考价，美元/百万 token）
        BigDecimal inputPrice = agent.getInputPricePerMillion() != null
                ? agent.getInputPricePerMillion() : new BigDecimal("0.14");
        BigDecimal cachedPrice = agent.getCachedInputPricePerMillion() != null
                ? agent.getCachedInputPricePerMillion() : new BigDecimal("0.014");
        BigDecimal outputPrice = agent.getOutputPricePerMillion() != null
                ? agent.getOutputPricePerMillion() : new BigDecimal("0.28");

        // cached 部分按缓存价
        BigDecimal cachedCost = cachedPrice.multiply(new BigDecimal(cachedTokens))
                .divide(new BigDecimal("1000000"), 10, BigDecimal.ROUND_HALF_UP);
        // 非 cached 的 input 部分
        long nonCachedInput = Math.max(0, inputTokens - cachedTokens);
        BigDecimal inputCost = inputPrice.multiply(new BigDecimal(nonCachedInput))
                .divide(new BigDecimal("1000000"), 10, BigDecimal.ROUND_HALF_UP);
        // output 部分
        BigDecimal outputCost = outputPrice.multiply(new BigDecimal(outputTokens))
                .divide(new BigDecimal("1000000"), 10, BigDecimal.ROUND_HALF_UP);

        return cachedCost.add(inputCost).add(outputCost).setScale(6, BigDecimal.ROUND_HALF_UP);
    }

    /** AI 调用失败落 error_log，供监控页错误列表与告警评估使用 */
    private void recordErrorLog(Long workspaceId, Agent agent, Long sessionId, Exception e) {
        try {
            ErrorLog errorLog = new ErrorLog();
            errorLog.setWorkspaceId(workspaceId);
            errorLog.setAgentId(agent.getId());
            errorLog.setAgentName(agent.getName());
            errorLog.setSessionId(sessionId);
            errorLog.setErrorType("llm_error");
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            errorLog.setErrorMessage(msg.length() > 1000 ? msg.substring(0, 1000) : msg);
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String stack = sw.toString();
            errorLog.setStackTrace(stack.length() > 4000 ? stack.substring(0, 4000) : stack);
            errorLog.setOccurredAt(LocalDateTime.now());
            errorLogMapper.insert(errorLog);
        } catch (Exception ignored) {
            log.warn("error_log 写入失败", ignored);
        }
    }

    /** 面向用户的简短错误信息（不透出上游响应体与堆栈） */
    private String shortErrorMessage(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : "系统繁忙，请稍后重试";
        return msg.length() > 200 ? msg.substring(0, 200) : msg;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
