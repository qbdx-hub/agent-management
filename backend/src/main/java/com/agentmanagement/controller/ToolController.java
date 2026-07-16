package com.agentmanagement.controller;

import com.agentmanagement.common.PageResult;
import com.agentmanagement.common.Result;
import com.agentmanagement.common.annotation.AuditLog;
import com.agentmanagement.form.ToolQueryForm;
import com.agentmanagement.form.ToolRegisterForm;
import com.agentmanagement.form.ToolUpdateForm;
import com.agentmanagement.service.ToolService;
import com.agentmanagement.vo.ToolSummaryVO;
import com.agentmanagement.vo.ToolVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 工具 RESTful 接口（前缀 /api/v1 由 context-path 统一加）。
 * 对齐前端 api/tool.ts：GET /tools、GET /tools/{id}、POST /tools、
 * PUT /tools/{id}、DELETE /tools/{id}。
 * 注：POST /tools/mcp、POST /tools/{id}/test、GET /tools/{id}/stats 为附加能力，本期暂不实现。
 */
@RestController
@RequestMapping("/tools")
public class ToolController {

    @Autowired
    private ToolService toolService;

    /** GET /tools —— 分页列表 */
    @GetMapping
    public Result<PageResult<ToolSummaryVO>> list(ToolQueryForm form) {
        return Result.success(toolService.pageTools(form));
    }

    /** GET /tools/{id} —— 详情 */
    @GetMapping("/{id}")
    public Result<ToolVO> get(@PathVariable("id") Long id) {
        return Result.success(toolService.getToolDetail(id));
    }

    /** POST /tools —— 注册工具 */
    @AuditLog(action = "tool.create", label = "注册工具", resourceType = "tool")
    @PostMapping
    public Result<ToolVO> create(@Valid @RequestBody ToolRegisterForm form) {
        return Result.success(toolService.registerTool(form));
    }

    /** PUT /tools/{id} —— 编辑工具 */
    @AuditLog(action = "tool.update", label = "更新工具", resourceType = "tool")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
                               @Valid @RequestBody ToolUpdateForm form) {
        toolService.updateTool(id, form);
        return Result.success();
    }

    /** DELETE /tools/{id} —— 删除工具 */
    @AuditLog(action = "tool.delete", label = "删除工具", resourceType = "tool")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        toolService.removeTool(id);
        return Result.success();
    }
}
