package com.agentmanagement.service.impl;

import com.agentmanagement.common.BusinessException;
import com.agentmanagement.common.PageResult;
import com.agentmanagement.common.ResultCode;
import com.agentmanagement.entity.Agent;
import com.agentmanagement.entity.Approval;
import com.agentmanagement.entity.User;
import com.agentmanagement.entity.Workflow;
import com.agentmanagement.entity.WorkflowEdge;
import com.agentmanagement.entity.WorkflowNode;
import com.agentmanagement.entity.WorkflowRun;
import com.agentmanagement.mapper.AgentMapper;
import com.agentmanagement.mapper.ApprovalMapper;
import com.agentmanagement.mapper.UserMapper;
import com.agentmanagement.mapper.WorkflowEdgeMapper;
import com.agentmanagement.mapper.WorkflowMapper;
import com.agentmanagement.mapper.WorkflowNodeMapper;
import com.agentmanagement.mapper.WorkflowRunMapper;
import com.agentmanagement.security.SecurityUtils;
import com.agentmanagement.service.AiService;
import com.agentmanagement.service.ToolService;
import com.agentmanagement.service.WorkflowExecutionService;
import com.agentmanagement.vo.ToolTestResultVO;
import com.agentmanagement.vo.WorkflowRunVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 工作流执行引擎实现。
 * <p>执行模型：从 start 节点沿边拓扑前行，每个节点执行后把结果写入 run.nodeResults 并落库
 * （前端轮询即可看到进度）。节点语义：
 * <ul>
 *   <li>agent：取节点关联 Agent 的 AI 配置非流式调用（system=Agent 提示词，user=节点任务+上游输出），真实计费</li>
 *   <li>tool：调用 ToolService.testTool 真实发起 HTTP（参数取 config.params），输出取映射结果</li>
 *   <li>condition：按出边条件（contains:/not_contains:/always，空=默认分支）选择走向</li>
 *   <li>approval：创建 approval 单并将 run 置为 waiting_approval 暂停；审批通过后从该节点下游恢复</li>
 *   <li>start/end：占位节点</li>
 * </ul>
 * 防护：单次运行最多 100 步且不允许重复经过同一节点（环路保护）。
 */
@Slf4j
@Service
public class WorkflowExecutionServiceImpl implements WorkflowExecutionService {

    @Autowired
    private WorkflowMapper workflowMapper;

    @Autowired
    private WorkflowNodeMapper workflowNodeMapper;

    @Autowired
    private WorkflowEdgeMapper workflowEdgeMapper;

    @Autowired
    private WorkflowRunMapper workflowRunMapper;

    @Autowired
    private ApprovalMapper approvalMapper;

    @Autowired
    private AgentMapper agentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AiService aiService;

    @Autowired
    private ToolService toolService;

    @Autowired
    @Qualifier("workflowExecutor")
    private Executor workflowExecutor;

    /** 环路/失控保护：单次运行最大节点执行数 */
    private static final int MAX_STEPS = 100;

    /** 单节点输出入库上限（字符），避免 nodeResults JSON 膨胀 */
    private static final int MAX_NODE_OUTPUT = 8_000;

    @Override
    public Long runWorkflow(Long workflowId, Map<String, Object> input, Long userId) {
        Workflow wf = workflowMapper.selectById(workflowId);
        if (wf == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "工作流不存在");
        }
        Long nodeCount = workflowNodeMapper.selectCount(
                new LambdaQueryWrapper<WorkflowNode>().eq(WorkflowNode::getWorkflowId, workflowId));
        if (nodeCount == null || nodeCount == 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "画布为空，请先编排节点");
        }
        Long startCount = workflowNodeMapper.selectCount(new LambdaQueryWrapper<WorkflowNode>()
                .eq(WorkflowNode::getWorkflowId, workflowId)
                .eq(WorkflowNode::getType, "start"));
        if (startCount == null || startCount == 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "缺少开始节点，请先在画布中添加");
        }

        WorkflowRun run = new WorkflowRun();
        run.setWorkflowId(workflowId);
        run.setStatus("running");
        run.setInput(input != null ? input : new HashMap<>());
        run.setNodeResults(new ArrayList<>());
        run.setTotalTokens(0L);
        run.setTotalCost(BigDecimal.ZERO);
        run.setStartedAt(LocalDateTime.now());
        run.setTriggeredBy(userId);
        run.setCreatedAt(LocalDateTime.now());
        workflowRunMapper.insert(run);

        Long runId = run.getId();
        workflowExecutor.execute(() -> {
            try {
                executeFrom(runId, null);
            } catch (Exception e) {
                log.error("工作流执行线程异常: runId={}", runId, e);
            }
        });
        return runId;
    }

    @Override
    public WorkflowRunVO getRun(Long runId) {
        return toVO(requireRunInWorkspace(runId));
    }

    @Override
    public PageResult<WorkflowRunVO> pageRuns(Long workflowId, int page, int pageSize) {
        Workflow wf = workflowMapper.selectById(workflowId);
        if (wf == null || !SecurityUtils.currentWorkspaceId().equals(wf.getWorkspaceId())) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "工作流不存在");
        }
        Page<WorkflowRun> p = new Page<>(page, pageSize);
        Page<WorkflowRun> result = workflowRunMapper.selectPage(p, new LambdaQueryWrapper<WorkflowRun>()
                .eq(WorkflowRun::getWorkflowId, workflowId)
                .orderByDesc(WorkflowRun::getId));
        List<WorkflowRunVO> list = result.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public void approveRun(Long runId, boolean approved, String reason, Long approverId) {
        WorkflowRun run = requireRunInWorkspace(runId);
        if (!"waiting_approval".equals(run.getStatus())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该运行不在等待审批状态");
        }

        // 回填审批单
        Approval approval = approvalMapper.selectOne(new LambdaQueryWrapper<Approval>()
                .eq(Approval::getResourceType, "workflow_run")
                .eq(Approval::getResourceId, runId)
                .eq(Approval::getStatus, "pending")
                .orderByDesc(Approval::getCreatedAt)
                .last("LIMIT 1"));
        if (approval != null) {
            Approval upd = new Approval();
            upd.setId(approval.getId());
            upd.setStatus(approved ? "approved" : "rejected");
            upd.setApproverId(approverId);
            upd.setApproverName(loadUserName(approverId));
            upd.setReason(reason);
            upd.setResolvedAt(LocalDateTime.now());
            approvalMapper.updateById(upd);
        }

        List<Map<String, Object>> results = run.getNodeResults() != null
                ? new ArrayList<>(run.getNodeResults()) : new ArrayList<>();

        if (!approved) {
            // 拒绝：waiting 节点标记为 rejected，运行置为失败
            for (Map<String, Object> r : results) {
                if ("waiting".equals(r.get("status"))) {
                    r.put("status", "rejected");
                    r.put("output", "审批拒绝" + (StringUtils.hasText(reason) ? "：" + reason : ""));
                }
            }
            run.setStatus("failed");
            run.setError("审批拒绝" + (StringUtils.hasText(reason) ? "：" + reason : ""));
            run.setNodeResults(results);
            run.setEndedAt(LocalDateTime.now());
            run.setDuration((int) Duration.between(run.getStartedAt(), run.getEndedAt()).toMillis());
            workflowRunMapper.updateById(run);
            return;
        }

        // 通过：从 waiting 节点的下游恢复执行（引擎内会重放该节点状态）
        String approvalNodeId = results.stream()
                .filter(r -> "waiting".equals(r.get("status")))
                .map(r -> (String) r.get("nodeId"))
                .findFirst().orElse(null);
        workflowExecutor.execute(() -> {
            try {
                executeFrom(runId, approvalNodeId);
            } catch (Exception e) {
                log.error("工作流恢复执行异常: runId={}", runId, e);
            }
        });
    }

    // ==================== 执行核心 ====================

    /**
     * 执行入口。resumeApprovalNodeId 为 null 时从 start 节点开始；
     * 否则为审批通过恢复：上下文从已落库的 nodeResults 重建，从审批节点的下游继续。
     */
    private void executeFrom(Long runId, String resumeApprovalNodeId) {
        WorkflowRun run = workflowRunMapper.selectById(runId);
        if (run == null) {
            return;
        }
        Workflow wf = workflowMapper.selectById(run.getWorkflowId());
        if (wf == null) {
            failRun(run, new ArrayList<>(), 0L, BigDecimal.ZERO, "工作流已被删除");
            return;
        }
        List<WorkflowNode> nodes = workflowNodeMapper.selectList(
                new LambdaQueryWrapper<WorkflowNode>().eq(WorkflowNode::getWorkflowId, wf.getId()));
        List<WorkflowEdge> edges = workflowEdgeMapper.selectList(
                new LambdaQueryWrapper<WorkflowEdge>().eq(WorkflowEdge::getWorkflowId, wf.getId()));
        Map<String, WorkflowNode> nodeMap = nodes.stream()
                .collect(Collectors.toMap(WorkflowNode::getNodeId, n -> n, (a, b) -> a));

        List<Map<String, Object>> results = run.getNodeResults() != null
                ? new ArrayList<>(run.getNodeResults()) : new ArrayList<>();
        long totalTokens = run.getTotalTokens() != null ? run.getTotalTokens() : 0L;
        BigDecimal totalCost = run.getTotalCost() != null ? run.getTotalCost() : BigDecimal.ZERO;
        // 上游输出：首节点以运行输入为上游；恢复执行时取最后成功节点的输出
        String lastOutput = inputText(run.getInput());

        String currentId;
        if (resumeApprovalNodeId != null) {
            // 审批通过：waiting 节点改写为 success，从其下游继续
            for (Map<String, Object> r : results) {
                if (resumeApprovalNodeId.equals(r.get("nodeId")) && "waiting".equals(r.get("status"))) {
                    r.put("status", "success");
                    r.put("output", "审批通过");
                }
            }
            currentId = nextNodeId(resumeApprovalNodeId, edges);
        } else {
            currentId = nodes.stream()
                    .filter(n -> "start".equals(n.getType()))
                    .map(WorkflowNode::getNodeId)
                    .findFirst().orElse(null);
        }
        persistRun(run, results, totalTokens, totalCost, "running", null);

        int steps = 0;
        Set<String> visited = new HashSet<>();
        while (currentId != null && steps++ < MAX_STEPS) {
            if (!visited.add(currentId)) {
                failRun(run, results, totalTokens, totalCost, "节点「" + currentId + "」重复执行，检测到环路");
                return;
            }
            WorkflowNode node = nodeMap.get(currentId);
            if (node == null) {
                failRun(run, results, totalTokens, totalCost, "节点「" + currentId + "」不存在（画布数据异常）");
                return;
            }

            long startMs = System.currentTimeMillis();
            Map<String, Object> rec = new LinkedHashMap<>();
            rec.put("nodeId", node.getNodeId());
            rec.put("label", node.getLabel());
            rec.put("type", node.getType());
            rec.put("sequence", results.size() + 1);

            String type = node.getType() != null ? node.getType() : "";
            switch (type) {
                case "start": {
                    rec.put("status", "success");
                    rec.put("output", "");
                    rec.put("durationMs", 0);
                    results.add(rec);
                    persistRun(run, results, totalTokens, totalCost, "running", null);
                    currentId = nextNodeId(node.getNodeId(), edges);
                    break;
                }
                case "end": {
                    rec.put("status", "success");
                    rec.put("output", "流程结束");
                    rec.put("durationMs", System.currentTimeMillis() - startMs);
                    results.add(rec);
                    completeRun(run, results, totalTokens, totalCost, lastOutput);
                    return;
                }
                case "agent": {
                    try {
                        Agent agent = node.getAgentId() != null ? agentMapper.selectById(node.getAgentId()) : null;
                        if (agent == null || !StringUtils.hasText(agent.getAiBaseUrl())
                                || !StringUtils.hasText(agent.getAiApiKey())) {
                            throw new IllegalStateException("节点未关联 Agent，或该 Agent 未配置 AI 连接");
                        }
                        String userMsg = promptOf(node, run.getInput(), lastOutput);
                        List<Map<String, String>> messages = new ArrayList<>();
                        if (StringUtils.hasText(agent.getSystemPrompt())) {
                            Map<String, String> sys = new HashMap<>();
                            sys.put("role", "system");
                            sys.put("content", agent.getSystemPrompt());
                            messages.add(sys);
                        }
                        Map<String, String> usr = new HashMap<>();
                        usr.put("role", "user");
                        usr.put("content", userMsg);
                        messages.add(usr);

                        String model = StringUtils.hasText(agent.getAiModel()) ? agent.getAiModel() : "gpt-4o";
                        AiService.ChatCompletionResult r = aiService.chatCompletion(
                                agent.getAiBaseUrl(), agent.getAiApiKey(), model, messages);
                        long in = r.getPromptTokens() != null ? r.getPromptTokens() : userMsg.length() * 15 / 10;
                        long out = r.getCompletionTokens() != null ? r.getCompletionTokens()
                                : (r.getContent() != null ? r.getContent().length() * 12 / 10 : 0);
                        totalTokens += in + out;
                        totalCost = totalCost.add(calcCost(agent, in, out));
                        lastOutput = r.getContent() != null ? r.getContent() : "";

                        rec.put("status", "success");
                        rec.put("output", abbreviate(lastOutput, MAX_NODE_OUTPUT));
                        rec.put("tokens", in + out);
                    } catch (Exception e) {
                        rec.put("status", "error");
                        rec.put("error", abbreviate(e.getMessage() != null ? e.getMessage() : "执行异常", 500));
                        rec.put("durationMs", System.currentTimeMillis() - startMs);
                        results.add(rec);
                        failRun(run, results, totalTokens, totalCost,
                                "节点「" + node.getLabel() + "」执行失败：" + e.getMessage());
                        return;
                    }
                    rec.put("durationMs", System.currentTimeMillis() - startMs);
                    results.add(rec);
                    persistRun(run, results, totalTokens, totalCost, "running", null);
                    currentId = nextNodeId(node.getNodeId(), edges);
                    break;
                }
                case "tool": {
                    try {
                        Map<String, Object> params = nodeConfigParams(node);
                        // 执行线程无 SecurityContext，workspaceId 用工作流自己的（agentId 未知传 null，sessionId 关联运行 id 归因）
                        ToolTestResultVO tr = toolService.testTool(node.getToolId(), params,
                                wf.getWorkspaceId(), null, run.getId());
                        if (tr == null || !Boolean.TRUE.equals(tr.getSuccess())) {
                            throw new IllegalStateException("HTTP "
                                    + (tr != null ? tr.getResponseStatus() : 0) + "，调用失败");
                        }
                        lastOutput = tr.getMappedOutput() != null ? tr.getMappedOutput() : "";
                        rec.put("status", "success");
                        rec.put("output", abbreviate(lastOutput, MAX_NODE_OUTPUT));
                    } catch (Exception e) {
                        rec.put("status", "error");
                        rec.put("error", abbreviate(e.getMessage() != null ? e.getMessage() : "调用异常", 500));
                        rec.put("durationMs", System.currentTimeMillis() - startMs);
                        results.add(rec);
                        failRun(run, results, totalTokens, totalCost,
                                "节点「" + node.getLabel() + "」执行失败：" + e.getMessage());
                        return;
                    }
                    rec.put("durationMs", System.currentTimeMillis() - startMs);
                    results.add(rec);
                    persistRun(run, results, totalTokens, totalCost, "running", null);
                    currentId = nextNodeId(node.getNodeId(), edges);
                    break;
                }
                case "condition": {
                    String next = pickConditionBranch(node, edges, lastOutput);
                    if (next == null) {
                        rec.put("status", "error");
                        rec.put("error", "条件节点无可匹配分支（所有出边条件均不成立且无默认分支）");
                        rec.put("durationMs", System.currentTimeMillis() - startMs);
                        results.add(rec);
                        failRun(run, results, totalTokens, totalCost,
                                "节点「" + node.getLabel() + "」无可匹配分支");
                        return;
                    }
                    rec.put("status", "success");
                    WorkflowNode target = nodeMap.get(next);
                    rec.put("output", "命中分支 → " + (target != null ? target.getLabel() : next));
                    rec.put("durationMs", System.currentTimeMillis() - startMs);
                    results.add(rec);
                    persistRun(run, results, totalTokens, totalCost, "running", null);
                    currentId = next;
                    break;
                }
                case "approval": {
                    // 暂停：创建审批单，等待 approveRun 恢复
                    rec.put("status", "waiting");
                    rec.put("output", "等待人工审批");
                    rec.put("durationMs", System.currentTimeMillis() - startMs);
                    results.add(rec);

                    Approval approval = new Approval();
                    approval.setWorkspaceId(wf.getWorkspaceId());
                    approval.setResourceType("workflow_run");
                    approval.setResourceId(run.getId());
                    approval.setResourceName(wf.getName() + " · " + node.getLabel());
                    approval.setAction("approve");
                    approval.setDetail("工作流「" + wf.getName() + "」节点「" + node.getLabel() + "」等待审批");
                    approval.setApplicantId(run.getTriggeredBy());
                    approval.setApplicantName(loadUserName(run.getTriggeredBy()));
                    approval.setStatus("pending");
                    approval.setCreatedAt(LocalDateTime.now());
                    approvalMapper.insert(approval);

                    persistRun(run, results, totalTokens, totalCost, "waiting_approval", null);
                    log.info("工作流暂停等待审批: runId={}, node={}", run.getId(), node.getNodeId());
                    return;
                }
                default: {
                    // 未知类型节点：跳过不阻断
                    rec.put("status", "skipped");
                    rec.put("output", "未知节点类型：" + type);
                    rec.put("durationMs", System.currentTimeMillis() - startMs);
                    results.add(rec);
                    persistRun(run, results, totalTokens, totalCost, "running", null);
                    currentId = nextNodeId(node.getNodeId(), edges);
                }
            }
        }

        if (currentId == null) {
            // 走到无后继节点：视为正常完成
            completeRun(run, results, totalTokens, totalCost, lastOutput);
        } else {
            failRun(run, results, totalTokens, totalCost, "执行步数超过上限（" + MAX_STEPS + "），疑似环路");
        }
    }

    // ==================== 辅助 ====================

    /** node 的后继（第一条以它为起点的边；画布连线顺序即保存顺序） */
    private String nextNodeId(String nodeId, List<WorkflowEdge> edges) {
        for (WorkflowEdge e : edges) {
            if (nodeId != null && nodeId.equals(e.getSourceNodeId())) {
                return e.getTargetNodeId();
            }
        }
        return null;
    }

    /**
     * 条件分支选择：按出边顺序求值，第一个成立的条件边命中；
     * 空条件边（condition 为 null/无 op）作为默认分支兜底（所有条件边都不成立时走它）。
     */
    private String pickConditionBranch(WorkflowNode node, List<WorkflowEdge> edges, String lastOutput) {
        String fallback = null;
        for (WorkflowEdge e : edges) {
            if (!node.getNodeId().equals(e.getSourceNodeId())) {
                continue;
            }
            if (!hasConditionOp(e.getCondition())) {
                if (fallback == null) {
                    fallback = e.getTargetNodeId();
                }
                continue;
            }
            if (evalCondition(e.getCondition(), lastOutput)) {
                return e.getTargetNodeId();
            }
        }
        return fallback;
    }

    /** condition JSON 是否带有 op（没有则视为默认分支） */
    private boolean hasConditionOp(Map<String, Object> condition) {
        return condition != null && condition.get("op") != null
                && StringUtils.hasText(condition.get("op").toString());
    }

    /**
     * 边条件求值（condition 为 JSON 对象，画布属性面板编辑），约定格式：
     * {"op":"contains","value":"文本"}    → 上游输出包含该文本
     * {"op":"not_contains","value":"文本"} → 上游输出不包含该文本
     * {"op":"always"}                      → 恒真
     * 无 op 的边不会走到这里（已按默认分支处理）
     */
    private boolean evalCondition(Map<String, Object> condition, String output) {
        String op = condition.get("op").toString();
        String value = condition.get("value") != null ? condition.get("value").toString() : "";
        switch (op) {
            case "contains":
                return output != null && !value.isEmpty() && output.contains(value);
            case "not_contains":
                return output == null || !output.contains(value);
            case "always":
                return true;
            default:
                return false;
        }
    }

    /** Agent 节点的用户消息：节点配置 prompt 优先，其次运行输入 question，附上一节点输出 */
    private String promptOf(WorkflowNode node, Map<String, Object> input, String lastOutput) {
        String task = null;
        if (node.getConfig() != null && node.getConfig().get("prompt") != null) {
            task = node.getConfig().get("prompt").toString();
        }
        if (!StringUtils.hasText(task) && input != null && input.get("question") != null) {
            task = input.get("question").toString();
        }
        if (!StringUtils.hasText(task)) {
            task = "请继续执行工作流任务";
        }
        if (StringUtils.hasText(lastOutput)) {
            task = task + "\n\n[上一节点输出]\n" + abbreviate(lastOutput, MAX_NODE_OUTPUT);
        }
        return task;
    }

    /** 工具节点参数：config.params（Map） */
    @SuppressWarnings("unchecked")
    private Map<String, Object> nodeConfigParams(WorkflowNode node) {
        if (node.getConfig() != null && node.getConfig().get("params") instanceof Map) {
            return (Map<String, Object>) node.getConfig().get("params");
        }
        return new HashMap<>();
    }

    private String inputText(Map<String, Object> input) {
        if (input != null && input.get("question") != null) {
            return input.get("question").toString();
        }
        return "";
    }

    /** Agent 节点费用（与 SessionController 同口径；工作流调用无缓存命中） */
    private BigDecimal calcCost(Agent agent, long inputTokens, long outputTokens) {
        BigDecimal inputPrice = agent.getInputPricePerMillion() != null
                ? agent.getInputPricePerMillion() : new BigDecimal("0.14");
        BigDecimal outputPrice = agent.getOutputPricePerMillion() != null
                ? agent.getOutputPricePerMillion() : new BigDecimal("0.28");
        BigDecimal inputCost = inputPrice.multiply(new BigDecimal(inputTokens))
                .divide(new BigDecimal("1000000"), 10, RoundingMode.HALF_UP);
        BigDecimal outputCost = outputPrice.multiply(new BigDecimal(outputTokens))
                .divide(new BigDecimal("1000000"), 10, RoundingMode.HALF_UP);
        return inputCost.add(outputCost).setScale(6, RoundingMode.HALF_UP);
    }

    private void persistRun(WorkflowRun run, List<Map<String, Object>> results,
                            long totalTokens, BigDecimal totalCost, String status, String error) {
        run.setStatus(status);
        run.setNodeResults(results);
        run.setTotalTokens(totalTokens);
        run.setTotalCost(totalCost);
        run.setError(error);
        workflowRunMapper.updateById(run);
    }

    private void completeRun(WorkflowRun run, List<Map<String, Object>> results,
                             long totalTokens, BigDecimal totalCost, String lastOutput) {
        Map<String, Object> output = new HashMap<>();
        output.put("result", abbreviate(lastOutput, 20_000));
        LocalDateTime now = LocalDateTime.now();
        run.setStatus("completed");
        run.setOutput(output);
        run.setNodeResults(results);
        run.setTotalTokens(totalTokens);
        run.setTotalCost(totalCost);
        run.setError(null);
        run.setEndedAt(now);
        run.setDuration(run.getStartedAt() != null
                ? (int) Duration.between(run.getStartedAt(), now).toMillis() : 0);
        workflowRunMapper.updateById(run);
        log.info("工作流运行完成: runId={}, tokens={}, cost={}", run.getId(), totalTokens, totalCost);
    }

    private void failRun(WorkflowRun run, List<Map<String, Object>> results,
                         long totalTokens, BigDecimal totalCost, String error) {
        LocalDateTime now = LocalDateTime.now();
        run.setStatus("failed");
        run.setNodeResults(results);
        run.setTotalTokens(totalTokens);
        run.setTotalCost(totalCost);
        run.setError(abbreviate(error, 1000));
        run.setEndedAt(now);
        run.setDuration(run.getStartedAt() != null
                ? (int) Duration.between(run.getStartedAt(), now).toMillis() : 0);
        workflowRunMapper.updateById(run);
        log.warn("工作流运行失败: runId={}, error={}", run.getId(), error);
    }

    private WorkflowRun requireRunInWorkspace(Long runId) {
        WorkflowRun run = workflowRunMapper.selectById(runId);
        if (run == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "运行记录不存在");
        }
        Workflow wf = workflowMapper.selectById(run.getWorkflowId());
        if (wf == null || !SecurityUtils.currentWorkspaceId().equals(wf.getWorkspaceId())) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "运行记录不存在");
        }
        return run;
    }

    private WorkflowRunVO toVO(WorkflowRun run) {
        WorkflowRunVO vo = new WorkflowRunVO();
        vo.setId(run.getId());
        vo.setWorkflowId(run.getWorkflowId());
        vo.setStatus(run.getStatus());
        vo.setInput(run.getInput());
        vo.setOutput(run.getOutput());
        vo.setNodeResults(run.getNodeResults() != null ? run.getNodeResults() : new ArrayList<>());
        vo.setError(run.getError());
        vo.setTotalTokens(run.getTotalTokens());
        vo.setTotalCost(run.getTotalCost());
        vo.setDuration(run.getDuration());
        vo.setStartedAt(run.getStartedAt());
        vo.setEndedAt(run.getEndedAt());
        vo.setTriggeredBy(run.getTriggeredBy());
        vo.setTriggeredByName(loadUserName(run.getTriggeredBy()));
        vo.setCreatedAt(run.getCreatedAt());
        return vo;
    }

    private String loadUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        return user == null ? null : user.getUsername();
    }

    private String abbreviate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }
}
