package com.agentmanagement.controller;

import com.agentmanagement.common.PageResult;
import com.agentmanagement.common.Result;
import com.agentmanagement.common.annotation.AuditLog;
import com.agentmanagement.form.WorkflowCreateForm;
import com.agentmanagement.form.WorkflowQueryForm;
import com.agentmanagement.form.WorkflowSaveForm;
import com.agentmanagement.service.WorkflowService;
import com.agentmanagement.vo.WorkflowSummaryVO;
import com.agentmanagement.vo.WorkflowVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 工作流（Agent 编排）RESTful 接口（前缀 /api/v1 由 context-path 统一加）。
 * GET /workflows 列表、GET /workflows/{id} 详情（含画布 nodes/edges）、
 * POST /workflows 创建、PUT /workflows/{id} 保存画布、DELETE /workflows/{id} 删除。
 */
@RestController
@RequestMapping("/workflows")
public class WorkflowController {

    @Autowired
    private WorkflowService workflowService;

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
}
