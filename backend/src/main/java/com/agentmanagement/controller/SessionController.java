package com.agentmanagement.controller;

import com.agentmanagement.common.PageResult;
import com.agentmanagement.common.Result;
import com.agentmanagement.entity.Agent;
import com.agentmanagement.entity.Message;
import com.agentmanagement.entity.Session;
import com.agentmanagement.form.SendMessageForm;
import com.agentmanagement.form.SessionCreateForm;
import com.agentmanagement.entity.CostRecord;
import com.agentmanagement.entity.User;
import com.agentmanagement.mapper.AgentMapper;
import com.agentmanagement.mapper.CostRecordMapper;
import com.agentmanagement.mapper.UserMapper;
import com.agentmanagement.mapper.MessageMapper;
import com.agentmanagement.mapper.SessionMapper;
import com.agentmanagement.security.SecurityUtils;
import com.agentmanagement.service.AiService;
import com.agentmanagement.service.RetrievalService;
import com.agentmanagement.service.SessionService;
import com.agentmanagement.vo.SessionDetailVO;
import com.agentmanagement.vo.SessionSummaryVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

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

        // 更新会话统计
        session.setMessageCount(session.getMessageCount() + 1);
        sessionMapper.updateById(session);

        // 3. 获取 Agent 配置
        Agent agent = agentMapper.selectById(session.getAgentId());
        if (agent == null || agent.getAiBaseUrl() == null || agent.getAiApiKey() == null) {
            // Agent 未配置 AI，返回模拟回复
            return sendMockResponse(form.getContent(), sessionId);
        }

        // 4. 创建 SSE emitter
        SseEmitter emitter = new SseEmitter(120_000L); // 2 分钟超时

        // 发送 thinking 事件
        try {
            emitter.send(SseEmitter.event().name("thinking")
                    .data("{\"content\":\"正在思考...\"}"));
            log.info("SSE 已发送 thinking 事件");
        } catch (Exception e) {
            log.error("SSE thinking 发送失败", e);
        }

        // 使用线程异步处理（buildMessages 移到线程内，异常可被捕获）
        new Thread(() -> {
            try {
                // 5. 构建消息上下文（含知识检索）
                List<Map<String, String>> messages = buildMessages(agent, session, form.getContent());

                // 6. 调用 AI（获取真实 token usage）
                String model = agent.getAiModel() != null ? agent.getAiModel() : "gpt-4o";
                AiService.ChatCompletionResult aiResult = aiService.chatCompletion(
                        agent.getAiBaseUrl(), agent.getAiApiKey(), model, messages);
                String reply = aiResult.getContent();

                // 发送内容
                emitter.send(SseEmitter.event().name("content")
                        .data("{\"content\":\"" + escapeJson(reply) + "\"}"));
                log.info("SSE 已发送 content 事件, length={}", reply.length());

                // 7. 使用真实 token 数据计算成本
                long inputTokens = aiResult.getPromptTokens() != null ? aiResult.getPromptTokens() : (long)(form.getContent().length() * 1.5);
                long outputTokens = aiResult.getCompletionTokens() != null ? aiResult.getCompletionTokens() : (long)(reply.length() * 1.2);
                long totalTokens = aiResult.getTotalTokens() != null ? aiResult.getTotalTokens() : inputTokens + outputTokens;
                long cachedTokens = aiResult.getCachedTokens() != null ? aiResult.getCachedTokens() : 0L;

                // 计算费用：cached 部分按缓存价，剩余 input 按正常价，output 按输出价
                BigDecimal cost = calculateCost(agent, inputTokens, outputTokens, cachedTokens);
                log.info("Token cost: input={}, output={}, cached={}, cost=${}", inputTokens, outputTokens, cachedTokens, cost);

                // 保存 AI 回复到数据库
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

                // 更新会话统计
                session.setMessageCount(session.getMessageCount() + 1);
                session.setTotalTokens(session.getTotalTokens() + totalTokens);
                session.setTotalCost(session.getTotalCost().add(cost));
                sessionMapper.updateById(session);

                // 更新 Agent 统计
                agent.setTotalTokens((agent.getTotalTokens() != null ? agent.getTotalTokens() : 0L) + totalTokens);
                agent.setTotalCost((agent.getTotalCost() != null ? agent.getTotalCost() : BigDecimal.ZERO).add(cost));
                agent.setTotalMessages((agent.getTotalMessages() != null ? agent.getTotalMessages() : 0L) + 1);
                Agent agentUpdate = new Agent();
                agentUpdate.setId(agent.getId());
                agentUpdate.setTotalTokens(agent.getTotalTokens());
                agentUpdate.setTotalCost(agent.getTotalCost());
                agentUpdate.setTotalMessages(agent.getTotalMessages());
                agentMapper.updateById(agentUpdate);

                // 发送 done 事件
                emitter.send(SseEmitter.event().name("done")
                        .data("{\"messageId\":" + assistantMsg.getId() + "}"));
                log.info("SSE 已发送 done 事件, messageId={}", assistantMsg.getId());
                emitter.complete();

            } catch (Exception e) {
                log.error("AI 回复异常: sessionId={}", sessionId, e);
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data("{\"error\":\"" + escapeJson(e.getMessage()) + "\"}"));
                    emitter.complete();
                } catch (Exception ignored) {}
            }
        }).start();

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
     * 构建发送给 AI 的消息数组：system prompt + 知识检索结果 + 历史消息。
     */
    private List<Map<String, String>> buildMessages(Agent agent, Session session, String userQuery) {
        List<Map<String, String>> messages = new ArrayList<>();

        // 1. System prompt
        StringBuilder systemPrompt = new StringBuilder();
        log.info("Agent systemPrompt=[{}], isEmpty={}", agent.getSystemPrompt(), agent.getSystemPrompt() == null || agent.getSystemPrompt().isEmpty());
        if (agent.getSystemPrompt() != null && !agent.getSystemPrompt().isEmpty()) {
            systemPrompt.append(agent.getSystemPrompt());
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
                    List<RetrievalService.SearchResult> results = retrievalService.search(kbId, userQuery, 3);
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

        if (systemPrompt.length() > 0) {
            Map<String, String> sysMsg = new HashMap<>();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt.toString());
            messages.add(sysMsg);
        }

        // 3. 加载历史消息（最近 workingWindow 轮）
        int window = agent.getWorkingWindow() != null ? agent.getWorkingWindow() : 10;
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getSessionId, session.getId())
                .in(Message::getRole, Arrays.asList("user", "assistant"))
                .orderByAsc(Message::getCreatedAt);
        List<Message> history = messageMapper.selectList(wrapper);

        // 只取最近 window*2 条（每轮 = user + assistant）
        int start = Math.max(0, history.size() - window * 2);
        for (int i = start; i < history.size(); i++) {
            Message m = history.get(i);
            Map<String, String> msg = new HashMap<>();
            msg.put("role", m.getRole());
            msg.put("content", m.getContent());
            messages.add(msg);
        }

        // 4. 当前用户消息（已经在 history 中了，不需要重复添加）
        // 检查最后一条是否已经是当前消息
        if (messages.isEmpty() || !messages.get(messages.size() - 1).get("content").equals(userQuery)) {
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userQuery);
            messages.add(userMsg);
        }

        return messages;
    }

    /** Agent 未配置 AI 时返回模拟回复 */
    private SseEmitter sendMockResponse(String userQuery, Long sessionId) {
        SseEmitter emitter = new SseEmitter(30_000L);
        new Thread(() -> {
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
                    emitter.send(SseEmitter.event().name("error").data("{\"error\":\"" + e.getMessage() + "\"}"));
                    emitter.complete();
                } catch (Exception ignored) {}
            }
        }).start();
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

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
