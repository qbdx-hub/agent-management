package com.agentmanagement.controller;

import com.agentmanagement.common.PageResult;
import com.agentmanagement.common.Result;
import com.agentmanagement.common.annotation.AuditLog;
import com.agentmanagement.form.WorkflowApproveForm;
import com.agentmanagement.form.WorkflowCreateForm;
import com.agentmanagement.form.WorkflowQueryForm;
import com.agentmanagement.form.WorkflowRunForm;
import com.agentmanagement.form.WorkflowSaveForm;
import com.agentmanagement.security.SecurityUtils;
import com.agentmanagement.service.WorkflowExecutionService;
import com.agentmanagement.service.WorkflowService;
import com.agentmanagement.vo.WorkflowRunVO;
import com.agentmanagement.vo.WorkflowSummaryVO;
import com.agentmanagement.vo.WorkflowVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * 工作流（Agent 编排）RESTful 接口（前缀 /api/v1 由 context-path 统一加）。
 * CRUD：GET /workflows 列表、GET /workflows/{id} 详情（含画布 nodes/edges）、
 * POST /workflows 创建、PUT /workflows/{id} 保存画布、DELETE /workflows/{id} 删除。
 * 执行：POST /workflows/{id}/run 运行、GET /workflows/runs/{runId} 运行详情（前端轮询）、
 * GET /workflows/{id}/runs 运行历史、POST /workflows/runs/{runId}/approve 处理审批。
 */
@RestController
@RequestMapping("/workflows")
public class WorkflowController {

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowExecutionService workflowExecutionService;

    /** GET /workflows —— 分页列表 */
    @GetMapping
    public Result<PageResult<WorkflowSummaryVO>> list(WorkflowQueryForm form) {
        return Result.success(workflowService.pageWorkflows(form));
    }

    /** GET /workflows/{id} —— 详情（含画布） */
    @GetMapping("/{id}")
    public Result<WorkflowVO> get(@PathVariable("id") Long id) {
        return Result.success(workflowService.getWorkflowDetail(id));
    }

    /** POST /workflows —— 创建空工作流 */
    @AuditLog(action = "workflow.create", label = "创建工作流", resourceType = "workflow")
    @PostMapping
    public Result<WorkflowVO> create(@Valid @RequestBody WorkflowCreateForm form) {
        return Result.success(workflowService.createWorkflow(form));
    }

    /** PUT /workflows/{id} —— 保存画布（全量替换 nodes/edges） */
    @AuditLog(action = "workflow.save", label = "保存工作流", resourceType = "workflow")
    @PutMapping("/{id}")
    public Result<WorkflowVO> save(@PathVariable("id") Long id,
                                   @Valid @RequestBody WorkflowSaveForm form) {
        return Result.success(workflowService.saveWorkflow(id, form));
    }

    /** DELETE /workflows/{id} —— 删除 */
    @AuditLog(action = "workflow.delete", label = "删除工作流", resourceType = "workflow")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        workflowService.removeWorkflow(id);
        return Result.success();
    }

    /** POST /workflows/{id}/run —— 启动一次运行（异步执行），返回 runId 供跳转运行页 */
    @AuditLog(action = "workflow.run", label = "运行工作流", resourceType = "workflow")
    @PostMapping("/{id}/run")
    public Result<Map<String, Object>> run(@PathVariable("id") Long id,
                                           @RequestBody(required = false) WorkflowRunForm form) {
        Map<String, Object> input = form != null ? form.getInput() : null;
        Long runId = workflowExecutionService.runWorkflow(id, input, SecurityUtils.currentUserId());
        Map<String, Object> data = new HashMap<>();
        data.put("runId", runId);
        return Result.success(data);
    }

    /** GET /workflows/runs/{runId} —— 运行详情（前端轮询看进度） */
    @GetMapping("/runs/{runId}")
    public Result<WorkflowRunVO> getRun(@PathVariable("runId") Long runId) {
        return Result.success(workflowExecutionService.getRun(runId));
    }

    /** GET /workflows/{id}/runs —— 某工作流的运行历史（分页） */
    @GetMapping("/{id}/runs")
    public Result<PageResult<WorkflowRunVO>> runs(@PathVariable("id") Long id,
                                                  @RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(workflowExecutionService.pageRuns(id, page, pageSize));
    }

    /** POST /workflows/runs/{runId}/approve —— 审批处理（通过则从暂停点恢复，拒绝则失败） */
    @AuditLog(action = "workflow.approve", label = "工作流审批", resourceType = "workflow_run")
    @PostMapping("/runs/{runId}/approve")
    public Result<Void> approve(@PathVariable("runId") Long runId,
                                @RequestBody WorkflowApproveForm form) {
        boolean approved = form.getApproved() != null && form.getApproved();
        workflowExecutionService.approveRun(runId, approved, form.getReason(), SecurityUtils.currentUserId());
        return Result.success();
    }
}
