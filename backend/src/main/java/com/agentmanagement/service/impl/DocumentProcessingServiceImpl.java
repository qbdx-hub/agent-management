package com.agentmanagement.service.impl;

import com.agentmanagement.entity.Agent;
import com.agentmanagement.entity.Document;
import com.agentmanagement.entity.DocumentChunk;
import com.agentmanagement.entity.KnowledgeBase;
import com.agentmanagement.mapper.AgentMapper;
import com.agentmanagement.mapper.DocumentChunkMapper;
import com.agentmanagement.mapper.DocumentMapper;
import com.agentmanagement.mapper.KnowledgeBaseMapper;
import com.agentmanagement.service.AiService;
import com.agentmanagement.service.DocumentProcessingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档处理服务实现 —— 文本提取 → 分块 → embedding → 存储。
 * 支持 txt/md/json/csv/yaml 等纯文本格式。
 */
@Slf4j
@Service
public class DocumentProcessingServiceImpl implements DocumentProcessingService {

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private DocumentChunkMapper documentChunkMapper;

    @Autowired
    private AiService aiService;

    @Autowired
    private AgentMapper agentMapper;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    /** 默认分块大小（字符数） */
    private static final int DEFAULT_CHUNK_SIZE = 500;
    /** 默认分块重叠（字符数） */
    private static final int DEFAULT_CHUNK_OVERLAP = 50;

    @Override
    @Async
    public void processDocument(Long documentId) {
        Document doc = documentMapper.selectById(documentId);
        if (doc == null) {
            log.warn("文档不存在: {}", documentId);
            return;
        }

        // 更新状态为 processing
        doc.setStatus("processing");
        documentMapper.updateById(doc);

        try {
            // 1. 读取文件内容
            String filePath = uploadDir + File.separator + doc.getFileUrl();
            File file = new File(filePath);
            if (!file.exists()) {
                throw new IOException("文件不存在: " + filePath);
            }

            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);

            // 2. 分块
            KnowledgeBase kb = knowledgeBaseMapper.selectById(doc.getKnowledgeBaseId());
            int chunkSize = DEFAULT_CHUNK_SIZE;
            int overlap = DEFAULT_CHUNK_OVERLAP;
            if (kb != null && kb.getConfig() != null) {
                Object cs = kb.getConfig().get("chunk_size");
                if (cs instanceof Number) chunkSize = ((Number) cs).intValue();
                Object ol = kb.getConfig().get("chunk_overlap");
                if (ol instanceof Number) overlap = ((Number) ol).intValue();
            }

            List<String> chunks = splitText(content, chunkSize, overlap);
            log.info("文档分块完成: documentId={}, chunks={}", documentId, chunks.size());

            // 3. 删除旧的分块（重新处理场景）
            LambdaQueryWrapper<DocumentChunk> deleteWrapper = new LambdaQueryWrapper<DocumentChunk>()
                    .eq(DocumentChunk::getDocumentId, documentId);
            documentChunkMapper.delete(deleteWrapper);

            // 4. 生成 embedding 并存储
            // 获取 embedding 配置（使用知识库的 embeddingModel 或默认）
            String embeddingModel = kb != null ? kb.getEmbeddingModel() : null;
            // 从 agent 继承 AI 配置来调用 embedding
            String aiBaseUrl = null;
            String aiApiKey = null;
            if (embeddingModel == null || embeddingModel.isEmpty()) {
                embeddingModel = "text-embedding-3-small"; // 默认模型
            }

            // 尝试从绑定到该知识库的 Agent 获取 AI 配置
            // 如果没有配置 embedding API，跳过向量化，只存储文本
            boolean hasEmbeddingConfig = false;

            // 查找绑定了此知识库的 Agent
            List<com.agentmanagement.entity.Agent> agents = findAgentsWithKnowledgeBase(doc.getKnowledgeBaseId());
            if (!agents.isEmpty()) {
                com.agentmanagement.entity.Agent agent = agents.get(0);
                aiBaseUrl = agent.getAiBaseUrl();
                aiApiKey = agent.getAiApiKey();
                if (aiBaseUrl != null && aiApiKey != null) {
                    hasEmbeddingConfig = true;
                }
            }

            int totalTokens = 0;
            for (int i = 0; i < chunks.size(); i++) {
                String chunkText = chunks.get(i);
                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocumentId(documentId);
                chunk.setKnowledgeBaseId(doc.getKnowledgeBaseId());
                chunk.setChunkIndex(i);
                chunk.setContent(chunkText);
                chunk.setTokenCount(estimateTokens(chunkText));
                chunk.setCreatedAt(LocalDateTime.now());

                // 尝试生成 embedding
                if (hasEmbeddingConfig) {
                    try {
                        float[] vec = aiService.generateEmbedding(aiBaseUrl, aiApiKey, embeddingModel, chunkText);
                        List<Float> embeddingList = new ArrayList<>();
                        for (float v : vec) {
                            embeddingList.add(v);
                        }
                        chunk.setEmbedding(embeddingList);
                    } catch (Exception e) {
                        log.warn("生成 embedding 失败: documentId={}, chunk={}, error={}",
                                documentId, i, e.getMessage());
                        // embedding 失败不影响文本存储
                    }
                }

                documentChunkMapper.insert(chunk);
                totalTokens += chunk.getTokenCount();
            }

            // 5. 更新文档状态为 completed
            doc.setStatus("completed");
            doc.setChunkCount(chunks.size());
            doc.setTotalTokens((long) totalTokens);
            documentMapper.updateById(doc);

            // 6. 更新知识库统计
            if (kb != null) {
                kb.setDocumentCount(kb.getDocumentCount());
                updateKnowledgeBaseStats(doc.getKnowledgeBaseId());
            }

            log.info("文档处理完成: documentId={}, chunks={}, tokens={}", documentId, chunks.size(), totalTokens);

        } catch (Exception e) {
            log.error("文档处理失败: documentId={}", documentId, e);
            doc.setStatus("failed");
            doc.setError(e.getMessage());
            documentMapper.updateById(doc);
        }
    }

    /**
     * 按 chunkSize + overlap 滑动窗口切分文本。
     * 优先在段落边界（\n\n）或句子边界（。！？.!?）处断开。
     */
    private List<String> splitText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            // 尝试在段落边界断开
            if (end < text.length()) {
                int breakPoint = text.lastIndexOf("\n\n", end);
                if (breakPoint > start + chunkSize / 2) {
                    end = breakPoint + 2;
                } else {
                    // 尝试在句子边界断开
                    breakPoint = findSentenceBreak(text, start + chunkSize / 2, end);
                    if (breakPoint > start) {
                        end = breakPoint;
                    }
                }
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            // 下一块的起始位置（考虑 overlap）
            start = end - overlap;
            // 防止死循环：如果 start 没有前进，强制跳到 end
            if (start <= (chunks.isEmpty() ? 0 : text.indexOf(chunks.get(chunks.size() - 1)))) {
                if (end >= text.length()) break;
                start = end;
            }
        }

        return chunks;
    }

    /** 在 [from, to) 范围内找最后一个句子结束符的位置 */
    private int findSentenceBreak(String text, int from, int to) {
        String delimiters = "。！？.!?；;\n";
        int lastBreak = -1;
        for (int i = from; i < to && i < text.length(); i++) {
            if (delimiters.indexOf(text.charAt(i)) >= 0) {
                lastBreak = i + 1;
            }
        }
        return lastBreak;
    }

    /** 估算 token 数（中文约 1.5 字/token，英文约 4 字符/token） */
    private int estimateTokens(String text) {
        if (text == null) return 0;
        // 简单估算：中文字符数 * 1.5 + 英文字符数 / 4
        int chinese = 0, other = 0;
        for (char c : text.toCharArray()) {
            if (c >= '一' && c <= '鿿') {
                chinese++;
            } else {
                other++;
            }
        }
        return (int) (chinese * 1.5 + other / 4.0);
    }

    /** 查找绑定了指定知识库的 Agent 列表 */
    private List<Agent> findAgentsWithKnowledgeBase(Long knowledgeBaseId) {
        // 查询所有 agent，检查 knowledgeBaseIds JSON 数组是否包含该 ID
        // MyBatis-Plus 的 JSON 查询支持有限，这里加载后过滤
        LambdaQueryWrapper<Agent> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Agent::getKnowledgeBaseIds);
        List<Agent> allAgents = agentMapper.selectList(wrapper);
        List<Agent> result = new ArrayList<>();
        for (Agent agent : allAgents) {
            if (agent.getKnowledgeBaseIds() != null && agent.getKnowledgeBaseIds().contains(knowledgeBaseId)) {
                result.add(agent);
            }
        }
        return result;
    }

    /** 更新知识库的统计字段 */
    private void updateKnowledgeBaseStats(Long kbId) {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<Document>()
                .eq(Document::getKnowledgeBaseId, kbId)
                .eq(Document::getStatus, "completed");
        List<Document> docs = documentMapper.selectList(wrapper);
        int totalChunks = 0;
        long totalTokens = 0;
        for (Document d : docs) {
            totalChunks += d.getChunkCount() != null ? d.getChunkCount() : 0;
            totalTokens += d.getTotalTokens() != null ? d.getTotalTokens() : 0;
        }

        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        if (kb != null) {
            kb.setDocumentCount(docs.size());
            kb.setTotalTokens(totalTokens);
            knowledgeBaseMapper.updateById(kb);
        }
    }
}
