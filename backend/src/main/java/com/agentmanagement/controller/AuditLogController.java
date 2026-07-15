package com.agentmanagement.controller;

import com.agentmanagement.common.Result;
import com.agentmanagement.entity.AuditLog;
import com.agentmanagement.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审计日志接口（前缀 /api/v1 由 context-path 统一加）。
 */
@RestController
@RequestMapping("/audit-logs")
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    /**
     * GET /api/v1/audit-logs —— 查询审计日志列表。
     *
     * @param workspaceId 工作空间 ID（请求头）
     * @param limit       返回条数，默认 50
     */
    @GetMapping
    public Result<List<AuditLog>> list(
            @RequestHeader("X-Workspace-Id") Long workspaceId,
            @RequestParam(defaultValue = "50") int limit) {
        return Result.success(auditLogService.listByWorkspace(workspaceId, limit));
    }
}
