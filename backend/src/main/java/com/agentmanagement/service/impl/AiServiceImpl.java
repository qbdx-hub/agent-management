package com.agentmanagement.service.impl;

import com.agentmanagement.service.AiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.BufferedSource;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * AI 服务实现 —— 调用 OpenAI 兼容 API（/v1/chat/completions, /v1/embeddings）。
 * 使用 OkHttp 发起 HTTP 请求，支持 SSE 流式响应。
 */
@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    @Override
    public ChatCompletionResult chatCompletion(String baseUrl, String apiKey, String model,
                                               List<Map<String, String>> messages) {
        String url = normalizeUrl(baseUrl) + "/chat/completions";
        String body = buildChatBody(model, toGenericMessages(messages), null, false, false);

        log.info("AI chat 请求: url={}, model={}", url, model);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body, JSON_TYPE))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errBody = response.body() != null ? response.body().string() : "";
                log.error("AI chat completion 失败: url={}, code={}, body={}", url, response.code(), errBody);
                throw new RuntimeException("AI 调用失败: HTTP " + response.code() + " " + errBody);
            }
            String respBody = response.body().string();
            JsonNode root = objectMapper.readTree(respBody);
            String content = root.path("choices").path(0).path("message").path("content").asText("");

            // 解析 token usage
            JsonNode usage = root.path("usage");
            Long promptTokens = usage.path("prompt_tokens").isMissingNode() ? null : usage.path("prompt_tokens").asLong();
            Long completionTokens = usage.path("completion_tokens").isMissingNode() ? null : usage.path("completion_tokens").asLong();
            Long totalTokens = usage.path("total_tokens").isMissingNode() ? null : usage.path("total_tokens").asLong();
            // DeepSeek 等兼容 API 可能返回 prompt_tokens_details.cached_tokens
            Long cachedTokens = null;
            JsonNode cachedNode = usage.path("prompt_tokens_details").path("cached_tokens");
            if (!cachedNode.isMissingNode()) {
                cachedTokens = cachedNode.asLong();
            }
            log.info("AI chat usage: prompt={}, completion={}, total={}, cached={}", promptTokens, completionTokens, totalTokens, cachedTokens);

            return new ChatCompletionResult(content, promptTokens, completionTokens, totalTokens, cachedTokens);
        } catch (IOException e) {
            log.error("AI chat completion 异常", e);
            throw new RuntimeException("AI 调用异常: " + e.getMessage(), e);
        }
    }

    @Override
    public ChatCompletionResult chatCompletionStream(String baseUrl, String apiKey, String model,
                                                     List<Map<String, String>> messages, SseEmitter emitter) {
        return chatCompletionStream(baseUrl, apiKey, model, toGenericMessages(messages), null, emitter);
    }

    @Override
    public ChatCompletionResult chatCompletionStream(String baseUrl, String apiKey, String model,
                                                     List<Map<String, Object>> messages,
                                                     List<Map<String, Object>> tools, SseEmitter emitter) {
        String url = normalizeUrl(baseUrl) + "/chat/completions";

        StringBuilder contentBuffer = new StringBuilder();
        Long promptTokens = null;
        Long completionTokens = null;
        Long totalTokens = null;
        Long cachedTokens = null;
        // 流式 tool_calls 按 index 分片到达，逐片累积（id/name/arguments 都可能拆成多段）
        List<AiService.ToolCall> pendingToolCalls = new ArrayList<>();

        // 首选带 stream_options(include_usage) 以获取真实 usage；
        // 个别网关不认识该参数会返回 400，此时降级重试一次（无 usage，由调用方估算兜底）
        for (int attempt = 0; attempt < 2; attempt++) {
            String body = buildChatBody(model, messages, tools, true, attempt == 0);
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(body, JSON_TYPE))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.code() == 400 && attempt == 0) {
                    String errBody = response.body() != null ? response.body().string() : "";
                    if (errBody.contains("stream_options")) {
                        log.warn("网关不支持 stream_options，降级重试(无 usage): url={}", url);
                        continue;
                    }
                }
                if (!response.isSuccessful()) {
                    String errBody = response.body() != null ? response.body().string() : "";
                    log.error("AI stream 失败: url={}, code={}, body={}", url, response.code(), errBody);
                    // 不透出上游响应体，避免泄露内部信息
                    throw new RuntimeException("AI 调用失败: HTTP " + response.code());
                }

                // 逐行读取上游 SSE 流，实时把 delta 推给前端
                BufferedSource source = response.body().source();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(source.inputStream(), StandardCharsets.UTF_8));

                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data: ")) {
                        continue;
                    }
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    try {
                        JsonNode node = objectMapper.readTree(data);
                        // include_usage 模式下最后一个 chunk 只带 usage、choices 为空
                        JsonNode usage = node.path("usage");
                        if (usage.isObject()) {
                            if (usage.hasNonNull("prompt_tokens")) promptTokens = usage.path("prompt_tokens").asLong();
                            if (usage.hasNonNull("completion_tokens")) completionTokens = usage.path("completion_tokens").asLong();
                            if (usage.hasNonNull("total_tokens")) totalTokens = usage.path("total_tokens").asLong();
                            JsonNode cachedNode = usage.path("prompt_tokens_details").path("cached_tokens");
                            if (!cachedNode.isMissingNode()) cachedTokens = cachedNode.asLong();
                        }
                        String delta = node.path("choices").path(0)
                                .path("delta").path("content").asText("");
                        if (!delta.isEmpty()) {
                            contentBuffer.append(delta);
                            emitter.send(SseEmitter.event().name("content")
                                    .data("{\"content\":\"" + escapeJson(delta) + "\"}"));
                        }
                        // 工具调用分片：{index, id?, function:{name?, arguments?}}
                        JsonNode toolCallDeltas = node.path("choices").path(0).path("delta").path("tool_calls");
                        if (toolCallDeltas.isArray()) {
                            for (JsonNode tc : toolCallDeltas) {
                                int idx = tc.path("index").asInt(0);
                                while (pendingToolCalls.size() <= idx) {
                                    pendingToolCalls.add(new AiService.ToolCall("", "", ""));
                                }
                                AiService.ToolCall acc = pendingToolCalls.get(idx);
                                if (tc.hasNonNull("id")) {
                                    acc.setId(tc.get("id").asText());
                                }
                                JsonNode fn = tc.path("function");
                                if (fn.hasNonNull("name")) {
                                    acc.setName(acc.getName() + fn.get("name").asText());
                                }
                                if (fn.hasNonNull("arguments")) {
                                    acc.setArguments(acc.getArguments() + fn.get("arguments").asText());
                                }
                            }
                        }
                    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                        // 解析单行失败，跳过
                        log.debug("SSE 行解析跳过: {}", data);
                    }
                }
                break; // 读取完成，退出重试循环
            } catch (IOException e) {
                log.error("AI stream 异常: url={}", url, e);
                throw new RuntimeException("AI 调用异常: " + e.getMessage(), e);
            }
        }

        // 汇总本轮请求到的工具调用（过滤空槽；缺 id 的补一个稳定兜底 id 供 role=tool 回传关联）
        List<AiService.ToolCall> toolCalls = null;
        for (int i = 0; i < pendingToolCalls.size(); i++) {
            AiService.ToolCall tc = pendingToolCalls.get(i);
            if (tc == null || tc.getName() == null || tc.getName().isEmpty()) {
                continue;
            }
            if (tc.getId() == null || tc.getId().isEmpty()) {
                tc.setId("call_" + i);
            }
            if (tc.getArguments() == null) {
                tc.setArguments("{}");
            }
            if (toolCalls == null) {
                toolCalls = new ArrayList<>();
            }
            toolCalls.add(tc);
        }

        log.info("AI stream 完成: length={}, prompt={}, completion={}, total={}, cached={}, toolCalls={}",
                contentBuffer.length(), promptTokens, completionTokens, totalTokens, cachedTokens,
                toolCalls != null ? toolCalls.size() : 0);
        ChatCompletionResult result = new ChatCompletionResult(
                contentBuffer.toString(), promptTokens, completionTokens, totalTokens, cachedTokens);
        result.setToolCalls(toolCalls);
        return result;
    }

    @Override
    public float[] generateEmbedding(String baseUrl, String apiKey, String model, String text) {
        String url = normalizeUrl(baseUrl) + "/embeddings";

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("input", text);
        String bodyStr;
        try {
            bodyStr = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException("构建 embedding 请求失败", e);
        }

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(bodyStr, JSON_TYPE))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errBody = response.body() != null ? response.body().string() : "";
                log.error("Embedding 失败: code={}, body={}", response.code(), errBody);
                throw new RuntimeException("Embedding 调用失败: HTTP " + response.code());
            }
            String respBody = response.body().string();
            JsonNode root = objectMapper.readTree(respBody);
            JsonNode embeddingNode = root.path("data").path(0).path("embedding");

            float[] vec = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                vec[i] = (float) embeddingNode.get(i).asDouble();
            }
            return vec;
        } catch (IOException e) {
            log.error("Embedding 异常", e);
            throw new RuntimeException("Embedding 调用异常: " + e.getMessage(), e);
        }
    }

    // ===== 内部方法 =====

    private String normalizeUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        // 已带版本号路径(/v1、/v4 等)的不再追加 /v1，兼容智谱 /api/paas/v4 等非 /v1 网关
        if (baseUrl.matches(".*\\/v\\d+$")) {
            return baseUrl;
        }
        return baseUrl + "/v1";
    }

    private String buildChatBody(String model, List<Map<String, Object>> messages,
                                 List<Map<String, Object>> tools, boolean stream, boolean includeUsage) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("stream", stream);
            body.put("max_tokens", 4096);
            if (stream && includeUsage) {
                // 请求最后一个 usage chunk（OpenAI/DeepSeek 兼容；不支持的网关由调用方降级重试）
                body.putObject("stream_options").put("include_usage", true);
            }
            if (tools != null && !tools.isEmpty()) {
                body.set("tools", objectMapper.valueToTree(tools));
            }

            ArrayNode msgArray = body.putArray("messages");
            for (Map<String, Object> msg : messages) {
                ObjectNode msgNode = msgArray.addObject();
                msgNode.put("role", String.valueOf(msg.get("role")));
                Object content = msg.get("content");
                // content 为空（assistant tool_calls 消息）时置空串，兼容性优于 null
                msgNode.put("content", content != null ? content.toString() : "");
                // assistant 的工具调用请求（[{id,type,function:{name,arguments}}]）
                Object toolCalls = msg.get("tool_calls");
                if (toolCalls != null) {
                    msgNode.set("tool_calls", objectMapper.valueToTree(toolCalls));
                }
                // role=tool 消息的工具调用关联 id
                Object toolCallId = msg.get("tool_call_id");
                if (toolCallId != null) {
                    msgNode.put("tool_call_id", toolCallId.toString());
                }
            }
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException("构建 chat 请求失败", e);
        }
    }

    /** Map<String,String> 消息 → Map<String,Object>（供统一的 body 构建器使用） */
    private List<Map<String, Object>> toGenericMessages(List<Map<String, String>> messages) {
        List<Map<String, Object>> generic = new ArrayList<>();
        for (Map<String, String> m : messages) {
            generic.add(new LinkedHashMap<String, Object>(m));
        }
        return generic;
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
