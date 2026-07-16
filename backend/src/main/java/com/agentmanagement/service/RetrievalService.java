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
     *
     * @param knowledgeBaseId 知识库 ID
     * @param query           用户查询文本
     * @param topK            返回前 K 个结果
     * @return 按相似度降序排列的检索结果
     */
    List<SearchResult> search(Long knowledgeBaseId, String query, int topK);
}
