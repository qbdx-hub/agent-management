package com.agentmanagement.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * AI 服务接口 —— 调用 OpenAI 兼容 API。
 * 支持 chat completion（流式/非流式）和 embedding。
 */
public interface AiService {

    /**
     * 聊天补全（非流式）
     * @param baseUrl  API Base URL（如 https://api.openai.com/v1）
     * @param apiKey   API Key
     * @param model    模型名（如 gpt-4o）
     * @param messages 消息列表 [{role, content}, ...]
     * @return AI 回复文本
     */
    String chatCompletion(String baseUrl, String apiKey, String model,
                          List<Map<String, String>> messages);

    /**
     * 聊天补全（流式）—— 通过 SseEmitter 逐块推送
     * @param baseUrl  API Base URL
     * @param apiKey   API Key
     * @param model    模型名
     * @param messages 消息列表
     * @param emitter  SSE 发射器
     */
    void chatCompletionStream(String baseUrl, String apiKey, String model,
                              List<Map<String, String>> messages, SseEmitter emitter);

    /**
     * 生成文本的 embedding 向量
     * @param baseUrl API Base URL
     * @param apiKey  API Key
     * @param model   Embedding 模型名（如 text-embedding-3-small）
     * @param text    要向量化的文本
     * @return 向量数组
     */
    float[] generateEmbedding(String baseUrl, String apiKey, String model, String text);
}
