package com.agentmanagement.controller;

import com.agentmanagement.common.PageResult;
import com.agentmanagement.common.Result;
import com.agentmanagement.common.annotation.AuditLog;
import com.agentmanagement.form.AgentCreateForm;
import com.agentmanagement.form.AgentQueryForm;
import com.agentmanagement.form.AgentUpdateForm;
import com.agentmanagement.service.AgentService;
import com.agentmanagement.vo.AgentSummaryVO;
import com.agentmanagement.vo.AgentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

/**
 * Agent RESTful 接口（前缀 /api/v1 由 context-path 统一加，类上不再写）。
 * 对齐前端 api/agent.ts：GET /agents、GET /agents/{id}、POST /agents、
 * PUT /agents/{id}、PATCH /agents/{id}/status、DELETE /agents/{id}。
 */
@RestController
@RequestMapping("/agents")
public class AgentController {

    @Autowired
    private AgentService agentService;

    /** GET /agents —— 分页列表 */
    @GetMapping
    public Result<PageResult<AgentSummaryVO>> list(AgentQueryForm form) {
        return Result.success(agentService.pageAgents(form));
    }

    /** GET /agents/{id} —— 详情 */
    @GetMapping("/{id}")
    public Result<AgentVO> get(@PathVariable("id") Long id) {
        return Result.success(agentService.getAgentDetail(id));
    }

    /** POST /agents —— 新建（含模型配置） */
    @AuditLog(action = "agent.create", label = "创建 Agent", resourceType = "agent")
    @PostMapping
    public Result<AgentVO> create(@Valid @RequestBody AgentCreateForm form) {
        return Result.success(agentService.createAgent(form));
    }

    /** PUT /agents/{id} —— 编辑基本信息 */
    @AuditLog(action = "agent.update", label = "更新 Agent", resourceType = "agent")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
                               @Valid @RequestBody AgentUpdateForm form) {
        agentService.updateAgent(id, form);
        return Result.success();
    }

    /** PATCH /agents/{id}/status —— 修改状态 */
    @AuditLog(action = "agent.status_change", label = "变更 Agent 状态", resourceType = "agent")
    @PatchMapping("/{id}/status")
    public Result<Void> patchStatus(@PathVariable("id") Long id,
                                    @RequestBody Map<String, String> body) {
        agentService.updateAgentStatus(id, body.get("status"));
        return Result.success();
    }

    /** DELETE /agents/{id} —— 删除 */
    @AuditLog(action = "agent.delete", label = "删除 Agent", resourceType = "agent")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        agentService.removeAgent(id);
        return Result.success();
    }
}
