package com.agentmanagement.service;

import com.agentmanagement.common.PageResult;
import com.agentmanagement.vo.WorkflowRunVO;

/**
 * 工作流执行引擎 —— 拓扑遍历执行节点，支持 Agent 节点（真实 AI 调用）、
 * 工具节点（真实 HTTP 调用）、条件分支、人工审批（暂停/恢复）。
 */
public interface WorkflowExecutionService {

    /** 启动一次运行（异步执行），返回 runId */
    Long runWorkflow(Long workflowId, java.util.Map<String, Object> input, Long userId);

    /** 运行详情（前端轮询） */
    WorkflowRunVO getRun(Long runId);

    /** 某工作流的运行历史 */
    PageResult<WorkflowRunVO> pageRuns(Long workflowId, int page, int pageSize);

    /** 处理审批：通过则从暂停点恢复执行，拒绝则运行置为失败 */
    void approveRun(Long runId, boolean approved, String reason, Long approverId);
}
