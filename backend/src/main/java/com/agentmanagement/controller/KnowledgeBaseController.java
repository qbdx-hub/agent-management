package com.agentmanagement.controller;

import com.agentmanagement.common.Result;
import com.agentmanagement.entity.Document;
import com.agentmanagement.entity.KnowledgeBase;
import com.agentmanagement.form.KnowledgeBaseCreateForm;
import com.agentmanagement.service.DocumentService;
import com.agentmanagement.service.KnowledgeBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

/**
 * 知识库 + 文档接口（前缀 /api/v1 由 context-path 统一加）。
 * 合并到同一 Controller 避免路径冲突。
 */
@RestController
@RequestMapping("/knowledge-bases")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private DocumentService documentService;

    // ==================== 知识库接口 ====================

    /**
     * POST /api/v1/knowledge-bases —— 创建知识库。
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

    // ==================== 文档接口 ====================

    /**
     * GET /api/v1/knowledge-bases/{kbId}/documents —— 查询知识库下的文档列表。
     */
    @GetMapping("/{kbId}/documents")
    public Result<List<Document>> listDocuments(@PathVariable Long kbId) {
        return Result.success(documentService.listByKnowledgeBase(kbId));
    }

    /**
     * POST /api/v1/knowledge-bases/{kbId}/documents —— 上传文档。
     * Content-Type: multipart/form-data
     * 字段名: file
     */
    @PostMapping("/{kbId}/documents")
    public Result<Document> uploadDocument(
            @PathVariable Long kbId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "1") Long userId) {
        return Result.success(documentService.upload(kbId, file, userId));
    }

    /**
     * DELETE /api/v1/knowledge-bases/{kbId}/documents/{docId} —— 删除文档。
     */
    @DeleteMapping("/{kbId}/documents/{docId}")
    public Result<Void> deleteDocument(
            @PathVariable Long kbId,
            @PathVariable Long docId) {
        documentService.deleteById(docId, kbId);
        return Result.success();
    }
}
