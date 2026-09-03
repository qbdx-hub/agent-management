package com.agentmanagement.service;

import java.util.List;

/**
 * 知识检索服务 —— 根据用户问题检索知识库中最相关的文档片段。
 */
public interface RetrievalService {

    /**
     * 检索结果
     */
    class SearchResult {
        private Long chunkId;
        private Long documentId;
        private String content;
        private float score;

        public SearchResult(Long chunkId, Long documentId, String content, float score) {
            this.chunkId = chunkId;
            this.documentId = documentId;
            this.content = content;
            this.score = score;
        }

        public Long getChunkId() { return chunkId; }
        public Long getDocumentId() { return documentId; }
        public String getContent() { return content; }
        public float getScore() { return score; }
    }

    /**
     * 在指定知识库中检索与 query 最相关的 topK 个片段。
     * 账户隔离：仅检索当前用户可见的知识库（admin 全部/本人创建/历史 NULL，且属于当前工作空间），
     * 不可见时返回空列表。
     *
     * @param knowledgeBaseId 知识库 ID
     * @param query           用户查询文本
     * @param topK            返回前 K 个结果
     * @param userId          当前用户 ID（显式传参：SSE 子线程中无 SecurityContext）
     * @param workspaceId     当前工作空间 ID
     * @return 按相似度降序排列的检索结果
     */
    List<SearchResult> search(Long knowledgeBaseId, String query, int topK, Long userId, Long workspaceId);
}
