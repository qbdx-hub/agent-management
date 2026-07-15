package com.agentmanagement.controller;

import com.agentmanagement.common.Result;
import com.agentmanagement.entity.KnowledgeBase;
import com.agentmanagement.form.KnowledgeBaseCreateForm;
import com.agentmanagement.service.KnowledgeBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 知识库接口（前缀 /api/v1 由 context-path 统一加）。
 */
@RestController
@RequestMapping("/knowledge-bases")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    /**
     * POST /api/v1/knowledge-bases —— 创建知识库。
     * workspaceId 由请求头 X-Workspace-Id 传入（拦截器/网关统一注入）。
     */
    @PostMapping
    public Result<KnowledgeBase> create(
            @Valid @RequestBody KnowledgeBaseCreateForm form,
            @RequestHeader("X-Workspace-Id") Long workspaceId,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "1") Long userId) {
        return Result.success(knowledgeBaseService.create(form, userId, workspaceId));
    }

    /**
     * GET /api/v1/knowledge-bases —— 查询当前工作空间下的知识库列表。
     */
    @GetMapping
    public Result<List<KnowledgeBase>> list(
            @RequestHeader("X-Workspace-Id") Long workspaceId) {
        return Result.success(knowledgeBaseService.listByWorkspace(workspaceId));
    }

    /**
     * GET /api/v1/knowledge-bases/{id} —— 查询单条知识库详情。
     */
    @GetMapping("/{id}")
    public Result<KnowledgeBase> detail(
            @PathVariable Long id,
            @RequestHeader("X-Workspace-Id") Long workspaceId) {
        return Result.success(knowledgeBaseService.getByIdChecked(id, workspaceId));
    }
}
