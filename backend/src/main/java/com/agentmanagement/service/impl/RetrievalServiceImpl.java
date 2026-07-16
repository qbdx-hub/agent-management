package com.agentmanagement.service.impl;

import com.agentmanagement.entity.Agent;
import com.agentmanagement.entity.DocumentChunk;
import com.agentmanagement.entity.KnowledgeBase;
import com.agentmanagement.mapper.AgentMapper;
import com.agentmanagement.mapper.DocumentChunkMapper;
import com.agentmanagement.mapper.KnowledgeBaseMapper;
import com.agentmanagement.service.AiService;
import com.agentmanagement.service.RetrievalService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * 知识检索服务实现 —— embedding + 余弦相似度。
 * 从 MySQL 加载向量，在应用层计算余弦相似度。
 */
@Slf4j
@Service
public class RetrievalServiceImpl implements RetrievalService {

    @Autowired
    private DocumentChunkMapper documentChunkMapper;

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private AgentMapper agentMapper;

    @Autowired
    private AiService aiService;

    @Override
    public List<SearchResult> search(Long knowledgeBaseId, String query, int topK) {
        log.info("RetrievalService.search 开始: kbId={}, query={}", knowledgeBaseId, query);
        // 1. 获取 AI 配置（从绑定到该知识库的 Agent 获取）
        KnowledgeBase kb = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (kb == null) {
            log.warn("知识库不存在: {}", knowledgeBaseId);
            return Collections.emptyList();
        }
        log.info("知识库存在: name={}", kb.getName());

        // 查找绑定了此知识库的 Agent 获取 AI 配置
        String aiBaseUrl = null;
        String aiApiKey = null;
        String embeddingModel = kb.getEmbeddingModel();
        if (embeddingModel == null || embeddingModel.isEmpty()) {
            embeddingModel = "text-embedding-3-small";
        }

        List<Agent> agents = findAgentsWithKnowledgeBase(knowledgeBaseId);
        if (!agents.isEmpty()) {
            Agent agent = agents.get(0);
            aiBaseUrl = agent.getAiBaseUrl();
            aiApiKey = agent.getAiApiKey();
        }

        if (aiBaseUrl == null || aiApiKey == null) {
            log.warn("知识库 {} 未配置 AI API，无法生成 embedding，使用关键词回退", knowledgeBaseId);
            return keywordFallback(knowledgeBaseId, query, topK);
        }
        log.info("AI 配置: baseUrl={}, model={}", aiBaseUrl, embeddingModel);

        // 2. 生成 query 的 embedding
        float[] queryVec;
        try {
            queryVec = aiService.generateEmbedding(aiBaseUrl, aiApiKey, embeddingModel, query);
        } catch (Exception e) {
            log.error("生成 query embedding 失败，回退到关键词检索: {}", e.getMessage());
            return keywordFallback(knowledgeBaseId, query, topK);
        }

        // 3. 加载该知识库所有有 embedding 的 chunk
        LambdaQueryWrapper<DocumentChunk> wrapper = new LambdaQueryWrapper<DocumentChunk>()
                .eq(DocumentChunk::getKnowledgeBaseId, knowledgeBaseId)
                .isNotNull(DocumentChunk::getEmbedding);
        List<DocumentChunk> chunks = documentChunkMapper.selectList(wrapper);

        if (chunks.isEmpty()) {
            return keywordFallback(knowledgeBaseId, query, topK);
        }

        // 4. 计算余弦相似度，用 PriorityQueue 取 topK
        PriorityQueue<SearchResult> heap = new PriorityQueue<>(topK + 1,
                Comparator.comparingDouble(SearchResult::getScore));

        for (DocumentChunk chunk : chunks) {
            if (chunk.getEmbedding() == null || chunk.getEmbedding().isEmpty()) continue;

            float[] chunkVec = listToArray(chunk.getEmbedding());
            float score = cosineSimilarity(queryVec, chunkVec);

            heap.offer(new SearchResult(chunk.getId(), chunk.getDocumentId(), chunk.getContent(), score));
            if (heap.size() > topK) {
                heap.poll(); // 移除最小的
            }
        }

        // 5. 转为降序列表
        List<SearchResult> results = new ArrayList<>(heap);
        results.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));
        return results;
    }

    /** 关键词回退检索（无 embedding 时使用 LIKE 模糊匹配） */
    private List<SearchResult> keywordFallback(Long knowledgeBaseId, String query, int topK) {
        // 中文分词：按标点和空格分词，再按 2-4 字滑窗切词
        List<String> keywords = new ArrayList<>();
        // 按标点分段
        String[] segments = query.split("[\\s,，。！？；;\\.!?\\?？、]+");
        for (String seg : segments) {
            if (seg.isEmpty()) continue;
            // 整段作为关键词
            if (seg.length() >= 2) keywords.add(seg);
            // 滑窗切词（2-4 字）
            for (int len = 2; len <= Math.min(4, seg.length()); len++) {
                for (int i = 0; i <= seg.length() - len; i++) {
                    keywords.add(seg.substring(i, i + len));
                }
            }
        }

        if (keywords.isEmpty()) {
            // 回退：返回前 topK 条
            LambdaQueryWrapper<DocumentChunk> wrapper = new LambdaQueryWrapper<DocumentChunk>()
                    .eq(DocumentChunk::getKnowledgeBaseId, knowledgeBaseId)
                    .last("LIMIT " + topK);
            return toResults(documentChunkMapper.selectList(wrapper));
        }

        // 去重
        List<String> uniqueKeywords = new ArrayList<>(new java.util.LinkedHashSet<>(keywords));

        // 用 OR 条件匹配任意关键词
        LambdaQueryWrapper<DocumentChunk> wrapper = new LambdaQueryWrapper<DocumentChunk>()
                .eq(DocumentChunk::getKnowledgeBaseId, knowledgeBaseId);
        wrapper.and(w -> {
            for (int i = 0; i < uniqueKeywords.size(); i++) {
                String kw = uniqueKeywords.get(i);
                if (i == 0) {
                    w.like(DocumentChunk::getContent, kw);
                } else {
                    w.or().like(DocumentChunk::getContent, kw);
                }
            }
        });
        wrapper.last("LIMIT " + topK);

        return toResults(documentChunkMapper.selectList(wrapper));
    }

    private List<SearchResult> toResults(List<DocumentChunk> chunks) {
        List<SearchResult> results = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            results.add(new SearchResult(chunk.getId(), chunk.getDocumentId(), chunk.getContent(), 0.5f));
        }
        return results;
    }

    /** 余弦相似度 */
    private float cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0;
        float dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (float) (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private float[] listToArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    private List<Agent> findAgentsWithKnowledgeBase(Long knowledgeBaseId) {
        LambdaQueryWrapper<Agent> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Agent::getKnowledgeBaseIds);
        List<Agent> allAgents = agentMapper.selectList(wrapper);
        List<Agent> result = new ArrayList<>();
        for (Agent agent : allAgents) {
            if (agent.getKnowledgeBaseIds() != null) {
                // JSON 反序列化可能是 Integer，需要转 Long 再比较
                for (Object rawId : agent.getKnowledgeBaseIds()) {
                    Long id = rawId instanceof Number ? ((Number) rawId).longValue() : Long.valueOf(rawId.toString());
                    if (id.equals(knowledgeBaseId)) {
                        result.add(agent);
                        break;
                    }
                }
            }
        }
        return result;
    }
}
