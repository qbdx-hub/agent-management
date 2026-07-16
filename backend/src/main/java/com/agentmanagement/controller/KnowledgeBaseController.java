package com.agentmanagement.controller;

import com.agentmanagement.common.Result;
import com.agentmanagement.entity.Document;
import com.agentmanagement.entity.KnowledgeBase;
import com.agentmanagement.form.KnowledgeBaseCreateForm;
import com.agentmanagement.service.AuditLogService;
import com.agentmanagement.service.DocumentProcessingService;
import com.agentmanagement.service.DocumentService;
import com.agentmanagement.service.KnowledgeBaseService;
import com.agentmanagement.service.RetrievalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * 知识库 + 文档接口（前缀 /api/v1 由 context-path 统一加）。
 */
@RestController
@RequestMapping("/knowledge-bases")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private DocumentProcessingService documentProcessingService;

    @Autowired
    private RetrievalService retrievalService;

    // ==================== 知识库接口 ====================

    /**
     * POST /api/v1/knowledge-bases —— 创建知识库。
     */
    @PostMapping
    public Result<KnowledgeBase> create(
            @Valid @RequestBody KnowledgeBaseCreateForm form,
            @RequestHeader("X-Workspace-Id") Long workspaceId,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "1") Long userId,
            @RequestHeader(value = "X-User-Name", required = false, defaultValue = "") String userName,
            HttpServletRequest request) {
        KnowledgeBase kb = knowledgeBaseService.create(form, userId, workspaceId);

        // 记录审计日志
        auditLogService.log(workspaceId, userId, userName,
                "knowledge.create", "创建知识库",
                "knowledge_base", kb.getId(), kb.getName(),
                "创建知识库: " + kb.getName(), "success",
                getClientIp(request), request.getHeader("User-Agent"));

        return Result.success(kb);
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

    /**
     * DELETE /api/v1/knowledge-bases/{id} —— 删除知识库（级联删除文档）。
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @PathVariable Long id,
            @RequestHeader("X-Workspace-Id") Long workspaceId,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "1") Long userId,
            @RequestHeader(value = "X-User-Name", required = false, defaultValue = "") String userName,
            HttpServletRequest request) {
        // 先查询知识库名称用于日志记录
        KnowledgeBase kb = knowledgeBaseService.getByIdChecked(id, workspaceId);
        String kbName = kb.getName();

        knowledgeBaseService.deleteById(id, workspaceId);

        // 记录审计日志
        auditLogService.log(workspaceId, userId, userName,
                "knowledge.delete", "删除知识库",
                "knowledge_base", id, kbName,
                "删除知识库: " + kbName + "（级联删除文档）", "success",
                getClientIp(request), request.getHeader("User-Agent"));

        return Result.success();
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
     */
    @PostMapping("/{kbId}/documents")
    public Result<Document> uploadDocument(
            @PathVariable Long kbId,
            @RequestParam("file") MultipartFile file,
            @RequestHeader("X-Workspace-Id") Long workspaceId,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "1") Long userId,
            @RequestHeader(value = "X-User-Name", required = false, defaultValue = "") String userName,
            HttpServletRequest request) {
        Document doc = documentService.upload(kbId, file, userId);

        // 记录审计日志
        auditLogService.log(workspaceId, userId, userName,
                "document.upload", "上传文档",
                "document", doc.getId(), doc.getName(),
                "上传文档到知识库[" + kbId + "]: " + doc.getName(), "success",
                getClientIp(request), request.getHeader("User-Agent"));

        return Result.success(doc);
    }

    /**
     * DELETE /api/v1/knowledge-bases/{kbId}/documents/{docId} —— 删除文档。
     */
    @DeleteMapping("/{kbId}/documents/{docId}")
    public Result<Void> deleteDocument(
            @PathVariable Long kbId,
            @PathVariable Long docId,
            @RequestHeader("X-Workspace-Id") Long workspaceId,
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "1") Long userId,
            @RequestHeader(value = "X-User-Name", required = false, defaultValue = "") String userName,
            HttpServletRequest request) {
        documentService.deleteById(docId, kbId);

        // 记录审计日志
        auditLogService.log(workspaceId, userId, userName,
                "document.delete", "删除文档",
                "document", docId, null,
                "从知识库[" + kbId + "]删除文档[" + docId + "]", "success",
                getClientIp(request), request.getHeader("User-Agent"));

        return Result.success();
    }

    // ==================== 检索 + 处理接口 ====================

    /**
     * GET /api/v1/knowledge-bases/{kbId}/search —— 知识检索（向量搜索）。
     */
    @GetMapping("/{kbId}/search")
    public Result<List<RetrievalService.SearchResult>> search(
            @PathVariable Long kbId,
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "5") int topK) {
        return Result.success(retrievalService.search(kbId, query, topK));
    }

    /**
     * POST /api/v1/knowledge-bases/{kbId}/documents/{docId}/process —— 触发文档处理（分块+向量化）。
     */
    @PostMapping("/{kbId}/documents/{docId}/process")
    public Result<Void> processDocument(@PathVariable Long kbId, @PathVariable Long docId) {
        documentProcessingService.processDocument(docId);
        return Result.success();
    }

    // ==================== 工具方法 ====================

    /**
     * 获取客户端真实 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
