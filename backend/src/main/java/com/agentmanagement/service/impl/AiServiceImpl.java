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
    public String chatCompletion(String baseUrl, String apiKey, String model,
                                 List<Map<String, String>> messages) {
        String url = normalizeUrl(baseUrl) + "/chat/completions";
        String body = buildChatBody(model, messages, false);

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
            return root.path("choices").path(0).path("message").path("content").asText("");
        } catch (IOException e) {
            log.error("AI chat completion 异常", e);
            throw new RuntimeException("AI 调用异常: " + e.getMessage(), e);
        }
    }

    @Override
    public void chatCompletionStream(String baseUrl, String apiKey, String model,
                                     List<Map<String, String>> messages, SseEmitter emitter) {
        String url = normalizeUrl(baseUrl) + "/chat/completions";
        String body = buildChatBody(model, messages, true);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body, JSON_TYPE))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errBody = response.body() != null ? response.body().string() : "";
                log.error("AI stream 失败: code={}, body={}", response.code(), errBody);
                emitter.send(SseEmitter.event().name("error").data("AI 调用失败: HTTP " + response.code()));
                emitter.complete();
                return;
            }

            // 读取 SSE 流
            BufferedSource source = response.body().source();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(source.inputStream(), StandardCharsets.UTF_8));

            String line;
            StringBuilder contentBuffer = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) {
                        // 流结束
                        emitter.send(SseEmitter.event().name("done")
                                .data("{\"content\":\"\"}"));
                        break;
                    }
                    try {
                        JsonNode node = objectMapper.readTree(data);
                        String delta = node.path("choices").path(0)
                                .path("delta").path("content").asText("");
                        if (!delta.isEmpty()) {
                            contentBuffer.append(delta);
                            emitter.send(SseEmitter.event().name("content")
                                    .data("{\"content\":\"" + escapeJson(delta) + "\"}"));
                        }
                    } catch (Exception e) {
                        // 解析单行失败，跳过
                        log.debug("SSE 行解析跳过: {}", data);
                    }
                }
            }
            emitter.complete();
        } catch (IOException e) {
            log.error("AI stream 异常", e);
            try {
                emitter.send(SseEmitter.event().name("error").data("AI 调用异常: " + e.getMessage()));
                emitter.completeWithError(e);
            } catch (IOException ignored) {}
        }
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
        // 如果用户已经包含了 /v1，不再重复添加
        if (baseUrl.endsWith("/v1")) {
            return baseUrl;
        }
        return baseUrl + "/v1";
    }

    private String buildChatBody(String model, List<Map<String, String>> messages, boolean stream) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("stream", stream);
            body.put("max_tokens", 4096);

            ArrayNode msgArray = body.putArray("messages");
            for (Map<String, String> msg : messages) {
                ObjectNode msgNode = msgArray.addObject();
                msgNode.put("role", msg.get("role"));
                msgNode.put("content", msg.get("content"));
            }
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException("构建 chat 请求失败", e);
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
