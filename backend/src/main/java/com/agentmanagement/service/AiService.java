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
     * 聊天补全（流式）—— 通过 SseEmitter 逐块推送 delta 给前端，
     * 流结束后返回累计的完整回复与 token usage（上游支持 include_usage 时为真实值，否则为 null）。
     * 不负责发送 done 事件（由调用方持久化后统一发送）；失败抛 RuntimeException，由调用方统一处理。
     */
    ChatCompletionResult chatCompletionStream(String baseUrl, String apiKey, String model,
                                              List<Map<String, String>> messages, SseEmitter emitter);

    /**
     * 聊天补全（流式，带工具）—— Function Calling 版本。
     * @param tools OpenAI 格式工具定义 [{type:"function", function:{name, description, parameters}}]，可为 null
     *              消息列表支持三种形态：{role, content} 普通消息、
     *              {role:"assistant", tool_calls:[{id,type,function:{name,arguments}}]} 模型工具调用、
     *              {role:"tool", tool_call_id, content} 工具执行结果。
     *              模型请求工具时 content 为空、toolCalls 非空（delta 不推给前端，由调用方执行工具后再次调用）。
     */
    ChatCompletionResult chatCompletionStream(String baseUrl, String apiKey, String model,
                                              List<Map<String, Object>> messages,
                                              List<Map<String, Object>> tools, SseEmitter emitter);

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

        private List<ToolCall> toolCalls;

        /** 模型请求的工具调用列表；非工具轮次为 null */
        public List<ToolCall> getToolCalls() { return toolCalls; }
        public void setToolCalls(List<ToolCall> toolCalls) { this.toolCalls = toolCalls; }
    }

    /** 模型发起的一次工具调用（function calling） */
    class ToolCall {
        private String id;
        private String name;
        /** JSON 字符串形式的参数 */
        private String arguments;

        public ToolCall() {}

        public ToolCall(String id, String name, String arguments) {
            this.id = id;
            this.name = name;
            this.arguments = arguments;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getArguments() { return arguments; }
        public void setArguments(String arguments) { this.arguments = arguments; }
    }
}
