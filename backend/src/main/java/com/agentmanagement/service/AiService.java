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
     * 聊天补全（非流式）—— 返回结果含 token usage
     * @param baseUrl  API Base URL（如 https://api.openai.com/v1）
     * @param apiKey   API Key
     * @param model    模型名（如 gpt-4o）
     * @param messages 消息列表 [{role, content}, ...]
     * @return 包含回复文本和 token 用量的结果
     */
    ChatCompletionResult chatCompletion(String baseUrl, String apiKey, String model,
                                        List<Map<String, String>> messages);

    /**
     * 聊天补全（流式）—— 通过 SseEmitter 逐块推送
     */
    void chatCompletionStream(String baseUrl, String apiKey, String model,
                              List<Map<String, String>> messages, SseEmitter emitter);

    /**
     * 生成文本的 embedding 向量
     */
    float[] generateEmbedding(String baseUrl, String apiKey, String model, String text);

    /**
     * AI 聊天补全结果（含 token usage）
     */
    class ChatCompletionResult {
        private String content;
        private Long promptTokens;
        private Long completionTokens;
        private Long totalTokens;
        private Long cachedTokens;

        public ChatCompletionResult() {}

        public ChatCompletionResult(String content, Long promptTokens, Long completionTokens, Long totalTokens, Long cachedTokens) {
            this.content = content;
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
            this.totalTokens = totalTokens;
            this.cachedTokens = cachedTokens;
        }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public Long getPromptTokens() { return promptTokens; }
        public void setPromptTokens(Long promptTokens) { this.promptTokens = promptTokens; }
        public Long getCompletionTokens() { return completionTokens; }
        public void setCompletionTokens(Long completionTokens) { this.completionTokens = completionTokens; }
        public Long getTotalTokens() { return totalTokens; }
        public void setTotalTokens(Long totalTokens) { this.totalTokens = totalTokens; }
        public Long getCachedTokens() { return cachedTokens; }
        public void setCachedTokens(Long cachedTokens) { this.cachedTokens = cachedTokens; }
    }
}
