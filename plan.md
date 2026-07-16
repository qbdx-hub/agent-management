# 实现计划：接入真实 AI + 知识库检索

## 目标
1. Agent 对话接入真实 AI（用户配置 API Key / Base URL / 模型名）
2. 知识库文档上传后自动分块 + 向量化
3. 对话时自动检索相关知识片段注入上下文
4. SSE 流式返回 AI 回复

## 一、数据库变更

### 1.1 agent 表新增 3 列
```sql
ALTER TABLE agent ADD COLUMN ai_base_url VARCHAR(500) DEFAULT NULL COMMENT 'AI API Base URL';
ALTER TABLE agent ADD COLUMN ai_api_key VARCHAR(500) DEFAULT NULL COMMENT 'AI API Key(加密存储)';
ALTER TABLE agent ADD COLUMN ai_model VARCHAR(100) DEFAULT NULL COMMENT 'AI 模型名称';
```

### 1.2 新建 document_chunk 表
```sql
CREATE TABLE IF NOT EXISTS document_chunk (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  document_id BIGINT NOT NULL,
  knowledge_base_id BIGINT NOT NULL,
  chunk_index INT NOT NULL COMMENT '块序号',
  content TEXT NOT NULL COMMENT '文本内容',
  token_count INT DEFAULT 0,
  embedding JSON DEFAULT NULL COMMENT '向量JSON数组',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_doc_id (document_id),
  KEY idx_kb_id (knowledge_base_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 二、后端新增依赖（pom.xml）

```xml
<!-- OkHttp：调用 AI API -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
```

> Hutool 已有 HttpUtil，但 OkHttp 的 SSE 支持更好（EventSource / EventListener）。
> 如需解析 PDF/DOCX 可后续加 Apache Tika，当前阶段先支持 txt/md/json/csv 等纯文本格式。

## 三、后端新增/修改文件

### 3.1 实体层
- **Agent.java** — 新增 `aiBaseUrl`, `aiApiKey`, `aiModel` 字段
- **DocumentChunk.java** — 新建实体（id, documentId, knowledgeBaseId, chunkIndex, content, tokenCount, embedding）
- **DocumentChunkMapper.java** — 新建 BaseMapper

### 3.2 AI 服务层（新建）
- **AiService.java** — 接口
  - `chatCompletion(String baseUrl, String apiKey, String model, List<ChatMessage> messages, boolean stream)` → 非流式返回完整文本 / 流式返回 SseEmitter
  - `generateEmbedding(String baseUrl, String apiKey, String model, String text)` → 返回 float[] 向量
- **AiServiceImpl.java** — 实现
  - 使用 OkHttp 调用 OpenAI 兼容 API（`/v1/chat/completions`, `/v1/embeddings`）
  - 流式：读取 SSE `data:` 行，逐块通过 SseEmitter 推送
  - 非流式：读取完整 JSON 响应

### 3.3 文档处理服务（新建）
- **DocumentProcessingService.java** — 接口
  - `processDocument(Long documentId)` — 异步处理：提取文本 → 分块 → 生成 embedding → 存入 document_chunk
- **DocumentProcessingServiceImpl.java** — 实现
  - 文本提取：txt/md/json/csv 直接读取，其他格式跳过（后续加 Tika）
  - 分块：按 chunk_size（默认 500 字符）+ overlap（默认 50 字符）滑动窗口切分
  - 向量化：调用 AiService.generateEmbedding
  - 存储：批量插入 document_chunk
  - 更新 document 状态：pending → processing → completed/failed

### 3.4 检索服务（新建）
- **RetrievalService.java** — 接口
  - `search(Long knowledgeBaseId, String query, int topK)` → 返回 List<SearchResult>
- **RetrievalServiceImpl.java** — 实现
  - 对 query 调用 embedding API
  - 从 MySQL 加载该知识库所有 chunk 的 embedding
  - Java 层计算余弦相似度，返回 topK 结果

### 3.5 会话服务改造
- **SessionServiceImpl.sendMessage()** 改造：
  1. 加载 Agent 配置（aiBaseUrl/aiApiKey/aiModel/systemPrompt）
  2. 如果 Agent 绑定了知识库，调用 RetrievalService 检索相关片段
  3. 构建 messages 数组：system prompt + 检索到的知识片段 + 历史消息 + 用户消息
  4. 调用 AiService.chatCompletion（流式）
  5. 通过 SseEmitter 将 AI 回复流式推送给前端

### 3.6 控制器层
- **SessionController.java** — 修改 sendMessage 端点
  - `POST /sessions/{sessionId}/messages` 返回 `text/event-stream`（SSE）
  - 保存用户消息后，启动 AI 流式回复

### 3.7 知识库控制器增强
- **KnowledgeBaseController.java** — 新增端点
  - `POST /knowledge-bases/{kbId}/documents/{docId}/process` — 触发文档处理
  - `GET /knowledge-bases/{kbId}/search?q=xxx&topK=5` — 知识检索
  - 上传文档后自动触发异步处理

## 四、前端修改

### 4.1 Agent 配置页
- **AgentConfig.vue** — 新增 "AI 模型配置" 区域
  - Base URL 输入框（placeholder: `https://api.openai.com/v1`）
  - API Key 输入框（password 类型）
  - 模型名输入框（placeholder: `gpt-4o`）

### 4.2 对话改造
- **SessionConsole.vue** — 删除 `simulateAssistantReply()`，改为 SSE 真实流式
  - `handleSend()` 发送消息后，创建 `EventSource` 或 fetch + ReadableStream
  - 监听 SSE 事件：`thinking` / `tool_call` / `content` / `done`
  - 逐步更新 assistant 消息内容
- **session.ts (store)** — `sendMessage` 改为返回 SSE 连接
- **session.ts (api)** — `sendMessage` 改为 fetch + stream 读取

### 4.3 知识库页面
- **KnowledgeDetail.vue** — 文档列表显示处理状态，搜索框调用真实检索 API
- 上传文档后自动触发处理，状态从 pending → processing → completed 实时更新

## 五、实现顺序

1. **数据库建表 + Agent 表加列**（SQL 脚本）
2. **Agent 实体 + 表单加字段**（后端最小改动）
3. **OkHttp 依赖 + AiService**（核心 AI 能力）
4. **DocumentChunk 实体 + Mapper**
5. **DocumentProcessingService**（文档分块 + 向量化）
6. **RetrievalService**（知识检索）
7. **SessionController SSE 改造**（对话流式返回）
8. **前端 AgentConfig AI 配置**
9. **前端 SessionConsole SSE 对接**
10. **前端 KnowledgeDetail 检索对接**
11. **编译验证 + 端到端测试**

## 六、关键技术决策

| 决策 | 选择 | 理由 |
|---|---|---|
| HTTP 客户端 | OkHttp 4.12 | SSE 支持好，JDK1.8 兼容 |
| AI API 格式 | OpenAI 兼容 | DeepSeek/OpenAI/大部分国产模型都兼容 |
| Embedding | 复用 AI 提供商 | 用户只需配一套 Key |
| 向量存储 | MySQL JSON 列 | 不依赖外部向量库 |
| 相似度计算 | Java 应用层余弦 | chunk 数量有限时性能可接受 |
| 流式协议 | SSE (Server-Sent Events) | 浏览器原生支持，比 WebSocket 简单 |
| 文本提取 | 纯文本直接读取 | 先支持 txt/md/json/csv，后续加 Tika |
