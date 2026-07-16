package com.agentmanagement.service;

/**
 * 文档处理服务 —— 提取文本、分块、生成 embedding。
 */
public interface DocumentProcessingService {

    /**
     * 异步处理文档：提取文本 → 分块 → 向量化 → 存入 document_chunk 表。
     * 处理过程中 document 状态：pending → processing → completed/failed。
     *
     * @param documentId 文档 ID
     */
    void processDocument(Long documentId);
}
