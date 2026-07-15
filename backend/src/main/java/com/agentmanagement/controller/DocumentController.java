package com.agentmanagement.controller;

import com.agentmanagement.common.Result;
import com.agentmanagement.entity.Document;
import com.agentmanagement.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文档接口（前缀 /api/v1 由 context-path 统一加）。
 */
@RestController
@RequestMapping("/knowledge-bases")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    /**
     * GET /api/v1/knowledge-bases/{kbId}/documents —— 查询知识库下的文档列表。
     */
    @GetMapping("/{kbId}/documents")
    public Result<List<Document>> list(@PathVariable Long kbId) {
        return Result.success(documentService.listByKnowledgeBase(kbId));
    }

    /**
     * POST /api/v1/knowledge-bases/{kbId}/documents —— 上传文档。
     * Content-Type: multipart/form-data
     * 字段名: file
     */
    @PostMapping("/{kbId}/documents")
    public Result<Document> upload(
            @PathVariable Long kbId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "1") Long userId) {
        return Result.success(documentService.upload(kbId, file, userId));
    }

    /**
     * DELETE /api/v1/documents/{docId} —— 删除文档。
     * 注意：这里用独立路径，不嵌套在 knowledge-bases 下，简化前端调用。
     */
    @DeleteMapping("/documents/{docId}")
    public Result<Void> delete(
            @PathVariable Long docId,
            @RequestParam("kbId") Long kbId) {
        documentService.deleteById(docId, kbId);
        return Result.success();
    }
}
