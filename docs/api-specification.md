# Agent 管理系统 — API 接口文档

> Version: v1.0 | Base URL: `http://{host}:8080/api/v1` | Content-Type: `application/json`

---

## 目录

1. [通用约定](#通用约定)
2. [认证与鉴权](#一认证与鉴权)
3. [工作空间](#二工作空间-workspace)
4. [Agent 管理](#三agent-管理)
5. [Agent 配置](#四agent-配置)
6. [工具管理](#五工具管理)
7. [Agent 会话与执行](#六agent-会话与执行)
8. [Agent 编排（V2）](#七agent-编排工作流)
9. [知识库（V2）](#八知识库)
10. [监控与统计](#九监控与统计)
11. [成本管理](#十成本管理)
12. [安全与治理](#十一安全与治理)
13. [团队与成员](#十二团队与成员)
14. [枚举字典](#附录枚举字典)

---

## 通用约定

### 请求头

```
Authorization: Bearer <token>
X-Workspace-Id: <当前工作空间ID>
Content-Type: application/json
```

### 统一响应体

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

### 业务错误码

| code | 含义 | HTTP Status |
|------|------|-------------|
| 0 | 成功 | 200 |
| 1001 | 参数校验失败 | 400 |
| 1002 | 资源不存在 | 404 |
| 1003 | 无权限操作 | 403 |
| 1004 | 未登录/token过期 | 401 |
| 2001 | Agent 执行失败 | 500 |
| 2002 | 工具调用超时 | 504 |
| 2003 | 模型返回异常 | 502 |
| 3001 | 预算超限/已熔断 | 429 |
| 3002 | 速率限制 | 429 |

### 分页请求参数

```json
{
  "page": 1,
  "pageSize": 20,
  "keyword": "搜索关键字",
  "sortBy": "createdAt",
  "sortDir": "desc"
}
```

### 分页响应结构

```json
{
  "code": 0,
  "data": {
    "list": [],
    "total": 100,
    "page": 1,
    "pageSize": 20
  }
}
```

### 通用时间字段（所有实体含以下字段）

```json
{
  "id": 1,
  "createdAt": "2026-07-14T10:30:00+08:00",
  "updatedAt": "2026-07-14T15:20:00+08:00",
  "createdBy": 1,
  "updatedBy": 1
}
```

---

## 一、认证与鉴权

### 1.1 登录

```
POST /auth/login
```

**请求体：**
```json
{
  "username": "zhangsan",
  "password": "123456"
}
```

**返回：**
```json
{
  "code": 0,
  "data": {
    "token": "eyJhbGciOi...",
    "expiresAt": "2026-07-21T10:30:00+08:00",
    "user": {
      "id": 1,
      "username": "zhangsan",
      "nickname": "张三",
      "avatar": "https://avatar.example.com/zhangsan.png",
      "email": "zhangsan@company.com",
      "role": "ADMIN"
    }
  }
}
```

### 1.2 获取当前用户信息

```
GET /auth/me
```

**返回：** 同 1.1 中 user 结构 + `permissions: string[]` + `workspaces: WorkspaceItem[]`

### 1.3 修改密码

```
PUT /auth/password
```

```json
{
  "oldPassword": "123456",
  "newPassword": "654321"
}
```

---

## 二、工作空间 (Workspace)

### 2.1 获取我的工作空间列表

```
GET /workspaces
```

**返回：**
```json
{
  "data": [
    {
      "id": 1,
      "name": "Agent开发团队",
      "description": "负责Agent平台研发",
      "role": "ADMIN",
      "memberCount": 5,
      "agentCount": 12,
      "createdAt": "2026-07-01T09:00:00+08:00"
    }
  ]
}
```

### 2.2 创建工作空间

```
POST /workspaces
```

```json
{
  "name": "新团队空间",
  "description": "描述信息"
}
```

### 2.3 更新工作空间

```
PUT /workspaces/{workspaceId}
```

```json
{
  "name": "Agent开发团队(改)",
  "description": "新描述"
}
```

### 2.4 删除工作空间

```
DELETE /workspaces/{workspaceId}
```
— 仅 ADMIN 可操作，空间内无 Agent 时才能删除

### 2.5 获取空间设置

```
GET /workspaces/{workspaceId}/settings
```

```json
{
  "data": {
    "defaultModelProvider": "openai",
    "sessionRetentionDays": 90,
    "autoArchiveDays": 30,
    "maxTokensPerTask": 100000,
    "language": "zh-CN"
  }
}
```

### 2.6 更新空间设置

```
PUT /workspaces/{workspaceId}/settings
```
— 请求体同 2.5 返回结构

---

## 三、Agent 管理

### 3.1 Agent 列表（分页 + 筛选）

```
GET /agents?page=1&pageSize=20&keyword=审查&status=published&tag=code&sortBy=updatedAt&sortDir=desc
```

**返回：**
```json
{
  "data": {
    "total": 45,
    "page": 1,
    "pageSize": 20,
    "list": [
      {
        "id": 1,
        "name": "代码审查助手",
        "description": "自动审查 PR 代码质量，输出改进建议",
        "avatar": "🔍",
        "status": "published",
        "modelName": "Claude Opus 4.8",
        "tags": ["code", "review"],
        "toolCount": 3,
        "totalSessions": 230,
        "successRate": 0.982,
        "avgLatencyMs": 1200,
        "createdBy": 1,
        "creatorName": "张三",
        "updatedAt": "2026-07-14T09:00:00+08:00"
      }
    ]
  }
}
```

### 3.2 创建 Agent（向导步骤1：基础信息）

```
POST /agents
```

```json
{
  "name": "代码审查助手",
  "description": "自动审查 PR 代码质量",
  "avatar": "🔍",
  "tags": ["code", "review"],
  "status": "draft"
}
```

**返回：** 返回新创建的 Agent 完整对象，含 `id`

### 3.3 获取 Agent 详情

```
GET /agents/{agentId}
```

**返回：**
```json
{
  "data": {
    "id": 1,
    "name": "代码审查助手",
    "description": "自动审查 PR 代码质量，输出改进建议",
    "avatar": "🔍",
    "status": "published",
    "tags": ["code", "review"],
    "createdBy": 1,
    "creatorName": "张三",
    "createdAt": "2026-07-14T09:00:00+08:00",
    "updatedAt": "2026-07-14T10:00:00+08:00",

    "config": {
      "modelProvider": "anthropic",
      "modelName": "claude-opus-4-8",
      "temperature": 0.7,
      "maxTokens": 4096,
      "topP": 0.95,
      "systemPrompt": "你是一个资深代码审查专家...",
      "promptVariables": [
        { "name": "language", "label": "语言", "type": "string", "defaultValue": "Java", "required": true },
        { "name": "strictness", "label": "严格度", "type": "select", "options": ["宽松", "标准", "严格"], "defaultValue": "标准" }
      ],
      "boundTools": [
        { "toolId": 1, "toolName": "搜索网页", "toolIcon": "🔍", "enabled": true },
        { "toolId": 3, "toolName": "读取文件", "toolIcon": "📁", "enabled": true }
      ],
      "memory": {
        "workingWindow": 20,
        "shortTermStrategy": "summary",
        "longTermEnabled": true,
        "knowledgeBaseIds": [1, 2]
      },
      "execution": {
        "maxSteps": 20,
        "timeoutSeconds": 300,
        "stopConditions": ["连续3次工具失败", "用户主动停止"],
        "reflectionEnabled": true,
        "reflectionDepth": 2
      },
      "outputSchema": {
        "type": "object",
        "properties": {
          "issues": {
            "type": "array",
            "items": {
              "type": "object",
              "properties": {
                "file": { "type": "string" },
                "line": { "type": "integer" },
                "severity": { "type": "string", "enum": ["critical", "major", "minor"] },
                "description": { "type": "string" },
                "suggestion": { "type": "string" }
              }
            }
          },
          "summary": { "type": "string" }
        }
      }
    },

    "stats": {
      "totalSessions": 230,
      "totalMessages": 1840,
      "totalTokens": 5200000,
      "totalCost": 123.50,
      "successRate": 0.982,
      "avgLatencyMs": 1200,
      "avgStepsPerSession": 5.3
    }
  }
}
```

### 3.4 更新 Agent 基础信息

```
PUT /agents/{agentId}
```

```json
{
  "name": "代码审查助手 V2",
  "description": "...",
  "avatar": "🔍",
  "tags": ["code", "review", "devops"]
}
```

### 3.5 删除 Agent

```
DELETE /agents/{agentId}
```
— 状态须是 archived 或 draft 才可删除

### 3.6 更新 Agent 状态

```
PATCH /agents/{agentId}/status
```

```json
{
  "status": "published"
}
```

状态流转规则：
```
draft → testing → published → paused → archived
  ↑        ↓                    ↓
  └────────┴────────────────────┘
```

### 3.7 复制 Agent

```
POST /agents/{agentId}/clone
```

```json
{
  "name": "代码审查助手(副本)",
  "copyConfig": true,
  "copyTools": true
}
```

---

## 四、Agent 配置

### 4.1 更新模型配置

```
PUT /agents/{agentId}/config/model
```

```json
{
  "modelProvider": "anthropic",
  "modelName": "claude-opus-4-8",
  "temperature": 0.5,
  "maxTokens": 8192,
  "topP": 0.9
}
```

### 4.2 更新 System Prompt

```
PUT /agents/{agentId}/config/prompt
```

```json
{
  "systemPrompt": "你是一个资深代码审查专家。\n当用户提交代码时，你需要：\n1. 检查 {{aspect}} 方面的问题\n2. 按严重程度分类...",
  "promptVariables": [
    { "name": "aspect", "label": "审查维度", "type": "select", "options": ["安全性", "性能", "可读性", "全面"], "defaultValue": "全面", "required": true }
  ]
}
```

### 4.3 获取 Prompt 版本历史

```
GET /agents/{agentId}/config/prompt/versions?page=1&pageSize=10
```

```json
{
  "data": {
    "total": 8,
    "list": [
      {
        "versionId": 8,
        "versionNumber": "v8",
        "systemPrompt": "你是一个资深代码审查专家...",
        "promptVariables": [],
        "changeNote": "新增审查维度变量",
        "changedBy": 1,
        "changerName": "张三",
        "changedAt": "2026-07-14T09:30:00+08:00"
      }
    ]
  }
}
```

### 4.4 Prompt 版本对比

```
GET /agents/{agentId}/config/prompt/versions/diff?v1=7&v2=8
```

```json
{
  "data": {
    "v1": { "versionNumber": "v7", "systemPrompt": "..." },
    "v2": { "versionNumber": "v8", "systemPrompt": "..." },
    "diff": "- 旧版本内容\n+ 新版本内容"
  }
}
```

### 4.5 回滚 Prompt 到指定版本

```
POST /agents/{agentId}/config/prompt/rollback
```

```json
{
  "versionId": 7,
  "changeNote": "v8 效果不好，回滚到 v7"
}
```

### 4.6 更新工具绑定

```
PUT /agents/{agentId}/config/tools
```

```json
{
  "tools": [
    { "toolId": 1, "enabled": true },
    { "toolId": 2, "enabled": true },
    { "toolId": 3, "enabled": false }
  ]
}
```

### 4.7 更新记忆配置

```
PUT /agents/{agentId}/config/memory
```

```json
{
  "workingWindow": 20,
  "shortTermStrategy": "summary",
  "longTermEnabled": true,
  "knowledgeBaseIds": [1, 2]
}
```

### 4.8 更新执行配置

```
PUT /agents/{agentId}/config/execution
```

```json
{
  "maxSteps": 20,
  "timeoutSeconds": 300,
  "reflectionEnabled": true,
  "reflectionDepth": 2,
  "outputSchema": {}
}
```

### 4.9 获取模型供应商列表

```
GET /model-providers
```

```json
{
  "data": [
    {
      "key": "openai",
      "name": "OpenAI",
      "models": [
        { "name": "gpt-4o", "displayName": "GPT-4o", "maxTokens": 128000, "pricing": { "inputPer1k": 0.0025, "outputPer1k": 0.01 } },
        { "name": "gpt-4o-mini", "displayName": "GPT-4o Mini", "maxTokens": 128000, "pricing": { "inputPer1k": 0.00015, "outputPer1k": 0.0006 } }
      ]
    },
    {
      "key": "anthropic",
      "name": "Anthropic",
      "models": [
        { "name": "claude-opus-4-8", "displayName": "Claude Opus 4.8", "maxTokens": 200000, "pricing": { "inputPer1k": 0.015, "outputPer1k": 0.075 } },
        { "name": "claude-sonnet-4-6", "displayName": "Claude Sonnet 4.6", "maxTokens": 200000, "pricing": { "inputPer1k": 0.003, "outputPer1k": 0.015 } }
      ]
    }
  ]
}
```

---

## 五、工具管理

### 5.1 工具列表

```
GET /tools?page=1&pageSize=20&keyword=&category=api&status=active
```

**返回：**
```json
{
  "data": {
    "total": 25,
    "list": [
      {
        "id": 1,
        "name": "search_web",
        "displayName": "网页搜索",
        "description": "搜索互联网获取最新信息",
        "category": "search",
        "categoryLabel": "查询类",
        "icon": "🔍",
        "type": "api",
        "status": "active",
        "bindAgentCount": 3,
        "totalCalls": 1500,
        "successRate": 0.995,
        "avgLatencyMs": 320,
        "createdAt": "2026-07-10T10:00:00+08:00"
      }
    ]
  }
}
```

### 5.2 注册 API 工具

```
POST /tools
```

```json
{
  "name": "search_web",
  "displayName": "网页搜索",
  "description": "搜索互联网获取最新信息",
  "category": "search",
  "icon": "🔍",
  "type": "api",
  "endpoint": {
    "url": "https://api.search.com/v1/search",
    "method": "POST",
    "headers": {
      "Authorization": "Bearer {{api_key}}",
      "Content-Type": "application/json"
    },
    "timeoutMs": 10000
  },
  "parameters": [
    {
      "name": "query",
      "type": "string",
      "required": true,
      "description": "搜索关键词",
      "defaultValue": null,
      "enumValues": null
    },
    {
      "name": "max_results",
      "type": "integer",
      "required": false,
      "description": "最大返回数量",
      "defaultValue": 10,
      "enumValues": null
    }
  ],
  "responseMapping": "找到 {{total}} 条结果：\n{{#each results}}\n- {{title}}: {{snippet}}\n{{/each}}",
  "credentialRef": "api_key",
  "retryOnFail": true,
  "maxRetries": 2
}
```

### 5.3 获取工具详情

```
GET /tools/{toolId}
```

返回结构同上 + `recentCalls: [{时间, 参数, 结果摘要}]`（最近 10 条调用记录）

### 5.4 更新工具

```
PUT /tools/{toolId}
```
— 请求体同 5.2

### 5.5 删除工具

```
DELETE /tools/{toolId}
```
— 所有 Agent 已解绑此工具才可删除

### 5.6 测试工具

```
POST /tools/{toolId}/test
```

```json
{
  "parameters": {
    "query": "测试搜索",
    "max_results": 3
  }
}
```

**返回：**
```json
{
  "data": {
    "success": true,
    "latencyMs": 280,
    "requestUrl": "https://api.search.com/v1/search",
    "requestBody": "{\"query\":\"测试搜索\",\"max_results\":3}",
    "responseStatus": 200,
    "responseBody": "{\"total\":100,\"results\":[...]}",
    "mappedOutput": "找到 100 条结果：\n- 第一条: 摘要内容..."
  }
}
```

### 5.7 注册 MCP 服务器

```
POST /tools/mcp
```

```json
{
  "serverName": "GitHub MCP",
  "transport": "stdio",
  "command": "npx",
  "args": ["-y", "@anthropic/mcp-server-github"],
  "envVars": {
    "GITHUB_TOKEN": "{{github_token}}"
  }
}
```

### 5.8 MCP 服务器——刷新工具列表

```
POST /tools/mcp/{mcpServerId}/discover
```

MCP 服务器的 `tools/list` 结果写入数据库，自动注册为可绑定工具

### 5.9 获取工具调用统计

```
GET /tools/{toolId}/stats?startDate=2026-07-01&endDate=2026-07-14
```

```json
{
  "data": {
    "totalCalls": 1500,
    "successRate": 0.995,
    "avgLatencyMs": 320,
    "p99LatencyMs": 850,
    "dailyCallCount": [
      { "date": "2026-07-01", "count": 110, "failCount": 1 },
      { "date": "2026-07-02", "count": 125, "failCount": 0 }
    ],
    "topAgents": [
      { "agentId": 1, "agentName": "代码审查助手", "callCount": 800 }
    ]
  }
}
```

---

## 六、Agent 会话与执行

### 6.1 创建新会话

```
POST /agents/{agentId}/sessions
```

```json
{
  "title": "审查 PR #42",
  "variables": {
    "language": "Java",
    "strictness": "严格"
  }
}
```

**返回：**
```json
{
  "data": {
    "sessionId": 1001,
    "agentId": 1,
    "title": "审查 PR #42",
    "status": "active",
    "createdAt": "2026-07-14T14:30:00+08:00"
  }
}
```

### 6.2 会话列表

```
GET /agents/{agentId}/sessions?page=1&pageSize=20&status=active
```

```json
{
  "data": {
    "total": 50,
    "list": [
      {
        "sessionId": 1001,
        "title": "审查 PR #42",
        "status": "completed",
        "messageCount": 8,
        "totalTokens": 12000,
        "totalCost": 0.15,
        "lastMessageAt": "2026-07-14T14:35:00+08:00",
        "createdAt": "2026-07-14T14:30:00+08:00"
      }
    ]
  }
}
```

### 6.3 获取会话消息

```
GET /sessions/{sessionId}/messages
```

```json
{
  "data": {
    "sessionId": 1001,
    "title": "审查 PR #42",
    "status": "completed",
    "messages": [
      {
        "messageId": 1,
        "role": "user",
        "content": "帮我审查 https://github.com/xxx/pull/42",
        "createdAt": "2026-07-14T14:30:01+08:00"
      },
      {
        "messageId": 2,
        "role": "assistant",
        "content": "审查完成，发现以下问题...",
        "steps": [
          {
            "stepId": 1,
            "sequence": 1,
            "type": "thinking",
            "status": "success",
            "content": "用户要求审查 PR #42，我需要先获取 PR 内容，然后逐文件分析代码变更",
            "startedAt": "2026-07-14T14:30:02+08:00",
            "completedAt": "2026-07-14T14:30:03+08:00",
            "durationMs": 1200
          },
          {
            "stepId": 2,
            "sequence": 2,
            "type": "tool_call",
            "status": "success",
            "toolName": "GitHub 获取 PR",
            "toolIcon": "🐙",
            "request": { "prUrl": "https://github.com/xxx/pull/42" },
            "response": { "files": ["AuthService.java", "LoginController.java"], "additions": 45, "deletions": 12 },
            "startedAt": "2026-07-14T14:30:03+08:00",
            "completedAt": "2026-07-14T14:30:04+08:00",
            "durationMs": 800
          },
          {
            "stepId": 3,
            "sequence": 3,
            "type": "thinking",
            "status": "success",
            "content": "已获取 PR 内容，涉及 2 个文件。现在逐文件分析：AuthService.java 中...",
            "startedAt": "2026-07-14T14:30:04+08:00",
            "completedAt": "2026-07-14T14:30:07+08:00",
            "durationMs": 3000
          },
          {
            "stepId": 4,
            "sequence": 4,
            "type": "tool_call",
            "status": "error",
            "toolName": "代码静态分析",
            "toolIcon": "📊",
            "request": { "file": "AuthService.java" },
            "response": null,
            "errorMessage": "静态分析服务连接超时",
            "retryCount": 2,
            "startedAt": "2026-07-14T14:30:07+08:00",
            "completedAt": "2026-07-14T14:30:12+08:00",
            "durationMs": 5200
          }
        ],
        "tokenUsage": { "input": 2500, "output": 800, "total": 3300 },
        "cost": 0.04,
        "createdAt": "2026-07-14T14:30:20+08:00"
      }
    ],
    "summary": {
      "totalMessages": 8,
      "totalTokens": 12000,
      "totalCost": 0.15,
      "totalSteps": 22,
      "totalDurationMs": 48000
    }
  }
}
```

### 6.4 发送消息 + 启动 Agent 执行

```
POST /sessions/{sessionId}/messages
```

```json
{
  "content": "帮我审查 https://github.com/xxx/pull/42",
  "mode": "auto",
  "attachments": []
}
```

**返回（同步）：**
```json
{
  "data": {
    "messageId": 1,
    "sessionId": 1001,
    "role": "user",
    "content": "帮我审查 https://github.com/xxx/pull/42",
    "createdAt": "2026-07-14T14:30:01+08:00"
  }
}
```

> 前端收到 200 后，立即连接 SSE 流（见 6.5）

### 6.5 SSE 流式推送 Agent 执行过程 ⭐

```
GET /sessions/{sessionId}/stream
```

**SSE 事件类型：**

```
event: thinking
data: {"stepId":1,"content":"我需要先获取 PR 的内容，然后逐文件分析","timestamp":"2026-07-14T14:30:02+08:00"}

event: tool_call
data: {"stepId":2,"toolName":"GitHub 获取 PR","toolIcon":"🐙","params":{"prUrl":"https://github.com/xxx/pull/42"},"timestamp":"2026-07-14T14:30:03+08:00"}

event: tool_result
data: {"stepId":2,"toolName":"GitHub 获取 PR","success":true,"result":{"files":["AuthService.java"],"additions":45},"durationMs":800,"timestamp":"2026-07-14T14:30:04+08:00"}

event: tool_error
data: {"stepId":4,"toolName":"代码静态分析","error":"连接超时","retrying":true,"timestamp":"2026-07-14T14:30:10+08:00"}

event: thinking
data: {"stepId":3,"content":"根据分析结果，AuthService.java 存在以下问题...","timestamp":"2026-07-14T14:30:07+08:00"}

event: message
data: {"messageId":2,"content":"审查完成，发现以下问题：\n\n## 严重问题\n1. ...","timestamp":"2026-07-14T14:30:20+08:00"}

event: token_usage
data: {"input":2500,"output":800,"total":3300,"cost":0.04}

event: done
data: {"messageId":2,"sessionStatus":"completed","totalSteps":4,"totalDurationMs":18000}
```

### 6.6 停止执行

```
POST /sessions/{sessionId}/stop
```

### 6.7 会话续接（从上一次中断处继续）

```
POST /sessions/{sessionId}/continue
```

```json
{
  "additionalInput": "请继续，但跳过静态分析步骤",
  "mode": "auto"
}
```

### 6.8 导出会话

```
GET /sessions/{sessionId}/export?format=markdown
```

返回 `Content-Type: text/markdown` 文件下载

### 6.9 删除会话

```
DELETE /sessions/{sessionId}
```

---

## 七、Agent 编排（工作流 V2）

### 7.1 工作流列表

```
GET /workflows?page=1&pageSize=20
```

### 7.2 创建工作流

```
POST /workflows
```

```json
{
  "name": "代码审查流水线",
  "description": "代码编写→审查→测试的自动化流水线",
  "nodes": [
    {
      "nodeId": "node_1",
      "type": "agent",
      "label": "代码审查",
      "agentId": 1,
      "position": { "x": 200, "y": 100 },
      "config": {
        "inputMapping": { "prUrl": "$.trigger.pr_url" },
        "outputKey": "reviewResult"
      }
    },
    {
      "nodeId": "node_2",
      "type": "agent",
      "label": "代码修复",
      "agentId": 2,
      "position": { "x": 200, "y": 300 },
      "config": {
        "inputMapping": {
          "issues": "$.node_1.reviewResult.issues",
          "files": "$.trigger.files"
        }
      }
    },
    {
      "nodeId": "node_3",
      "type": "condition",
      "label": "是否有严重问题?",
      "position": { "x": 200, "y": 200 },
      "config": {
        "condition": "$.node_1.reviewResult.severityCount.critical > 0",
        "trueBranch": "node_2",
        "falseBranch": null
      }
    },
    {
      "nodeId": "node_4",
      "type": "approval",
      "label": "人工确认发布",
      "position": { "x": 200, "y": 500 }
    }
  ],
  "edges": [
    { "source": "node_1", "target": "node_3" },
    { "source": "node_3", "target": "node_2" },
    { "source": "node_2", "target": "node_4" }
  ],
  "triggers": [
    { "type": "webhook", "config": {} },
    { "type": "cron", "config": { "expression": "0 9 * * 1-5" } }
  ],
  "errorStrategy": {
    "retryCount": 2,
    "retryDelaySeconds": 30,
    "onFinalFailure": "notify"
  }
}
```

### 7.3 获取工作流详情

```
GET /workflows/{workflowId}
```

### 7.4 更新工作流

```
PUT /workflows/{workflowId}
```

### 7.5 运行工作流

```
POST /workflows/{workflowId}/run
```

```json
{
  "inputs": {
    "pr_url": "https://github.com/xxx/pull/42",
    "files": ["AuthService.java"]
  }
}
```

### 7.6 工作流运行记录

```
GET /workflows/{workflowId}/runs?page=1&pageSize=20
```

### 7.7 运行记录详情

```
GET /workflows/{workflowId}/runs/{runId}
```

---

## 八、知识库

### 8.1 知识库列表

```
GET /knowledge-bases?page=1&pageSize=20
```

```json
{
  "data": {
    "total": 5,
    "list": [
      {
        "id": 1,
        "name": "Agent业务知识库",
        "description": "Agent相关业务文档和规范",
        "documentCount": 12,
        "vectorCount": 350,
        "boundAgentCount": 2,
        "status": "active",
        "createdAt": "2026-07-01T09:00:00+08:00"
      }
    ]
  }
}
```

### 8.2 创建知识库

```
POST /knowledge-bases
```

```json
{
  "name": "Agent业务知识库",
  "description": "Agent相关业务文档和规范",
  "embeddingModel": "text-embedding-3-small",
  "chunkSize": 500,
  "chunkOverlap": 50
}
```

### 8.3 上传文档到知识库

```
POST /knowledge-bases/{kbId}/documents
Content-Type: multipart/form-data
```

| 字段 | 类型 | 说明 |
|------|------|------|
| file | File | PDF/Markdown/TXT/代码文件 |
| title | string | 文档标题 |

### 8.4 文档列表

```
GET /knowledge-bases/{kbId}/documents?page=1&pageSize=20
```

### 8.5 删除文档

```
DELETE /knowledge-bases/{kbId}/documents/{docId}
```

### 8.6 获取文档分块（预览向量化结果）

```
GET /knowledge-bases/{kbId}/documents/{docId}/chunks
```

```json
{
  "data": {
    "chunks": [
      { "index": 1, "content": "Agent是一种能够自主感知环境、做出决策并执行动作的智能系统...", "tokenCount": 120 },
      { "index": 2, "content": "...", "tokenCount": 115 }
    ]
  }
}
```

### 8.7 知识库检索（测试用）

```
POST /knowledge-bases/{kbId}/search
```

```json
{
  "query": "Agent的配置步骤",
  "topK": 5,
  "minScore": 0.7
}
```

**返回：**
```json
{
  "data": {
    "results": [
      {
        "chunkIndex": 3,
        "documentTitle": "Agent配置指南",
        "content": "配置Agent需要以下步骤：1. 选择模型 2. 编写System Prompt...",
        "score": 0.92
      }
    ]
  }
}
```

---

## 九、监控与统计

### 9.1 监控总览数据

```
GET /monitor/overview?period=today
```

```json
{
  "data": {
    "activeAgentCount": 12,
    "runningTaskCount": 3,
    "todayCallCount": 1247,
    "successRate": 0.982,
    "avgLatencyMs": 1200,
    "p99LatencyMs": 4500,
    "totalTokensToday": 850000,
    "trends": {
      "callCountChange": 0.12,
      "successRateChange": -0.003,
      "latencyChange": 0.05
    }
  }
}
```

### 9.2 Token 用量趋势

```
GET /monitor/token-trend?period=7d&granularity=hour
```

```json
{
  "data": {
    "series": [
      { "time": "2026-07-14T00:00:00+08:00", "input": 20000, "output": 5000 },
      { "time": "2026-07-14T01:00:00+08:00", "input": 15000, "output": 3000 }
    ],
    "summary": {
      "totalInput": 520000,
      "totalOutput": 130000,
      "totalCost": 12.50
    }
  }
}
```

### 9.3 调用量趋势

```
GET /monitor/call-trend?period=7d&granularity=hour
```

### 9.4 Agent 健康排行

```
GET /monitor/agent-health?sortBy=successRate&order=asc
```

```json
{
  "data": [
    {
      "agentId": 1,
      "agentName": "代码审查助手",
      "status": "healthy",
      "successRate": 0.991,
      "avgLatencyMs": 800,
      "callCount24h": 120,
      "errorSummary": "无"
    },
    {
      "agentId": 5,
      "agentName": "部署检查Agent",
      "status": "warning",
      "successRate": 0.821,
      "avgLatencyMs": 3500,
      "callCount24h": 45,
      "errorSummary": "最近3次调用，2次SSH连接超时"
    }
  ]
}
```

### 9.5 调用链路详情

```
GET /monitor/trace/{traceId}
```

返回完整的单次执行 Trace（结构同 6.3 中 messages 的 steps 数组，追加 Agent 信息、工作空间信息）

### 9.6 错误日志

```
GET /monitor/errors?page=1&pageSize=20&agentId=1&startDate=2026-07-01&endDate=2026-07-14
```

```json
{
  "data": {
    "total": 35,
    "list": [
      {
        "errorId": 1,
        "agentId": 5,
        "agentName": "部署检查Agent",
        "sessionId": 2010,
        "stepSequence": 3,
        "errorType": "tool_timeout",
        "errorMessage": "SSH连接超时: 10.0.1.50:22",
        "occurredAt": "2026-07-14T11:00:00+08:00"
      }
    ]
  }
}
```

### 9.7 告警规则 CRUD

```
GET    /monitor/alerts/rules              # 告警规则列表
POST   /monitor/alerts/rules              # 创建告警规则
PUT    /monitor/alerts/rules/{ruleId}     # 更新
DELETE /monitor/alerts/rules/{ruleId}     # 删除
```

```json
{
  "name": "Agent成功率过低",
  "metric": "success_rate",
  "targetType": "agent",
  "targetId": null,
  "condition": "lt",
  "threshold": 0.85,
  "duration": "5m",
  "severity": "warning",
  "enabled": true,
  "notifyChannels": ["feishu", "email"]
}
```

### 9.8 告警记录

```
GET /monitor/alerts/records?page=1&pageSize=20&status=triggered
```

---

## 十、成本管理

### 10.1 成本总览

```
GET /cost/overview?period=this_month
```

```json
{
  "data": {
    "totalCost": 1247.30,
    "budgetLimit": 2000.00,
    "budgetRemaining": 752.70,
    "budgetPercent": 62.4,
    "todayCost": 45.20,
    "yesterdayCost": 52.10,
    "projectedMonthCost": 1450.00,
    "meltdownStatus": "normal"
  }
}
```

### 10.2 按维度拆分

```
GET /cost/breakdown?dimension=model|agent|member&period=this_month
```

```json
{
  "data": [
    { "label": "GPT-4o", "cost": 680.00, "percent": 54.5, "tokenInput": 2000000, "tokenOutput": 500000 },
    { "label": "Claude Opus 4.8", "cost": 320.00, "percent": 25.7, "tokenInput": 800000, "tokenOutput": 200000 },
    { "label": "DeepSeek V4", "cost": 180.00, "percent": 14.4, "tokenInput": 1500000, "tokenOutput": 600000 },
    { "label": "Gemini 2.5", "cost": 67.30, "percent": 5.4, "tokenInput": 500000, "tokenOutput": 150000 }
  ]
}
```

### 10.3 成本趋势

```
GET /cost/trend?period=30d&granularity=day
```

```json
{
  "data": {
    "series": [
      { "date": "2026-07-01", "cost": 38.50, "tokens": 150000 },
      { "date": "2026-07-02", "cost": 42.10, "tokens": 165000 }
    ]
  }
}
```

### 10.4 预算配置

```
GET    /cost/budgets          # 获取所有预算配置
POST   /cost/budgets          # 创建预算
PUT    /cost/budgets/{id}     # 更新
DELETE /cost/budgets/{id}     # 删除
```

```json
{
  "name": "个人日预算",
  "scope": "user",
  "scopeId": null,
  "period": "daily",
  "limit": 100.00,
  "warnPercent": 80,
  "meltdownEnabled": true,
  "notifyChannels": ["feishu"]
}
```

### 10.5 费用明细（消费记录）

```
GET /cost/records?page=1&pageSize=20&agentId=1&modelProvider=&startDate=&endDate=
```

```json
{
  "data": {
    "total": 2500,
    "list": [
      {
        "recordId": 1,
        "agentId": 1,
        "agentName": "代码审查助手",
        "sessionId": 1001,
        "modelProvider": "anthropic",
        "modelName": "claude-opus-4-8",
        "tokenInput": 2500,
        "tokenOutput": 800,
        "cost": 0.04,
        "userId": 1,
        "userName": "张三",
        "createdAt": "2026-07-14T14:30:20+08:00"
      }
    ]
  }
}
```

---

## 十一、安全与治理

### 11.1 角色列表

```
GET /security/roles
```

```json
{
  "data": [
    {
      "id": 1,
      "name": "ADMIN",
      "label": "管理员",
      "description": "拥有所有权限",
      "isSystem": true,
      "memberCount": 2,
      "permissions": ["agent:*", "tool:*", "workspace:*", "security:*", "cost:*"]
    },
    {
      "id": 2,
      "name": "MANAGER",
      "label": "管理者",
      "description": "可管理Agent和工具，查看所有报表",
      "isSystem": true,
      "memberCount": 3,
      "permissions": ["agent:*", "tool:read", "tool:register", "monitor:*", "cost:*", "workspace:member:invite"]
    },
    {
      "id": 3,
      "name": "DEVELOPER",
      "label": "开发者",
      "description": "可创建和使用Agent",
      "isSystem": true,
      "memberCount": 10,
      "permissions": ["agent:read", "agent:create", "agent:update:own", "tool:read", "session:*", "cost:read:own"]
    },
    {
      "id": 4,
      "name": "VIEWER",
      "label": "只读用户",
      "description": "只能查看，不能修改",
      "isSystem": true,
      "memberCount": 2,
      "permissions": ["agent:read", "tool:read", "monitor:read", "cost:read"]
    }
  ]
}
```

### 11.2 创建/更新/删除 自定义角色

```
POST   /security/roles
PUT    /security/roles/{roleId}
DELETE /security/roles/{roleId}
```

### 11.3 审计日志

```
GET /security/audit-logs?page=1&pageSize=20&userId=&action=&resource=&startDate=&endDate=
```

```json
{
  "data": {
    "total": 5000,
    "list": [
      {
        "logId": 1,
        "userId": 1,
        "userName": "张三",
        "action": "agent.create",
        "actionLabel": "创建Agent",
        "resource": "Agent/1",
        "detail": "创建了Agent：代码审查助手",
        "ip": "192.168.1.100",
        "userAgent": "Mozilla/5.0...",
        "result": "success",
        "createdAt": "2026-07-14T09:00:00+08:00"
      }
    ]
  }
}
```

审计操作码枚举：

| action | 含义 |
|--------|------|
| agent.create | 创建 Agent |
| agent.update | 更新 Agent |
| agent.delete | 删除 Agent |
| agent.status_change | 状态变更 |
| tool.create | 注册工具 |
| tool.delete | 删除工具 |
| session.start | 开始会话 |
| session.message | 发送消息 |
| api_key.create | 添加 API Key |
| api_key.delete | 删除 API Key |
| member.invite | 邀请成员 |
| member.remove | 移除成员 |
| role.change | 角色变更 |
| budget.update | 预算修改 |
| security.policy | 安全策略变更 |

### 11.4 审批流配置

```
GET    /security/approvals/rules
POST   /security/approvals/rules
PUT    /security/approvals/rules/{ruleId}
DELETE /security/approvals/rules/{ruleId}
```

```json
{
  "name": "Agent发布审批",
  "triggerAction": "agent.status_change",
  "triggerCondition": "{\"to\":\"published\"}",
  "approverRole": "MANAGER",
  "requiredApprovals": 1,
  "enabled": true
}
```

### 11.5 审批列表（需要我审批的）

```
GET /security/approvals/pending?page=1&pageSize=20
```

```json
{
  "data": {
    "list": [
      {
        "approvalId": 1,
        "ruleName": "Agent发布审批",
        "applicantId": 3,
        "applicantName": "王五",
        "action": "agent.status_change",
        "detail": "将Agent「部署检查Agent」从 testing 改为 published",
        "status": "pending",
        "createdAt": "2026-07-14T10:00:00+08:00"
      }
    ]
  }
}
```

### 11.6 审批操作

```
POST /security/approvals/{approvalId}/approve
POST /security/approvals/{approvalId}/reject
```

```json
{
  "comment": "确认通过，已检查Agent绑定的工具无敏感权限"
}
```

### 11.7 API Key 管理（模型供应商密钥）

```
GET    /security/api-keys                          # 密钥列表（脱敏展示）
POST   /security/api-keys                          # 添加密钥
DELETE /security/api-keys/{keyId}                 # 删除密钥
```

```json
{
  "provider": "openai",
  "keyName": "公司OpenAI账号",
  "apiKey": "sk-xxxxxxxxxxxxxxxxxxxxx",
  "isDefault": true
}
```

列表返回脱敏：
```json
{
  "id": 1,
  "provider": "openai",
  "keyName": "公司OpenAI账号",
  "apiKeyMasked": "sk-***...***xxxx",
  "isDefault": true,
  "lastUsedAt": "2026-07-14T14:30:00+08:00",
  "createdBy": 1,
  "createdAt": "2026-07-01T09:00:00+08:00"
}
```

---

## 十二、团队与成员

### 12.1 成员列表

```
GET /workspaces/{workspaceId}/members
```

```json
{
  "data": [
    {
      "userId": 1,
      "username": "zhangsan",
      "nickname": "张三",
      "avatar": "https://...",
      "email": "zhangsan@company.com",
      "role": "ADMIN",
      "roleLabel": "管理员",
      "joinedAt": "2026-07-01T09:00:00+08:00",
      "lastActiveAt": "2026-07-14T14:30:00+08:00",
      "agentCount": 5,
      "sessionCount30d": 230
    }
  ]
}
```

### 12.2 邀请成员

```
POST /workspaces/{workspaceId}/members
```

```json
{
  "email": "wangwu@company.com",
  "role": "DEVELOPER"
}
```

### 12.3 修改成员角色

```
PUT /workspaces/{workspaceId}/members/{userId}
```

```json
{
  "role": "MANAGER"
}
```

### 12.4 移除成员

```
DELETE /workspaces/{workspaceId}/members/{userId}
```

### 12.5 获取空间活动日志

```
GET /workspaces/{workspaceId}/activities?page=1&pageSize=20
```

```json
{
  "data": {
    "list": [
      {
        "type": "agent.created",
        "userId": 1,
        "userName": "张三",
        "description": "创建了 Agent「代码审查助手」",
        "relatedId": 1,
        "relatedType": "agent",
        "createdAt": "2026-07-14T09:00:00+08:00"
      }
    ]
  }
}
```

---

## 附录：枚举字典

### AgentStatus

| 值 | 含义 |
|----|------|
| draft | 草稿 |
| testing | 调试中 |
| published | 已发布 |
| paused | 已暂停 |
| archived | 已归档 |

### SessionStatus

| 值 | 含义 |
|----|------|
| active | 进行中 |
| completed | 已完成 |
| stopped | 用户停止 |
| error | 执行错误 |

### StepType

| 值 | 含义 |
|----|------|
| thinking | 推理/思考 |
| tool_call | 工具调用 |
| tool_result | 工具返回 |

### StepStatus

| 值 | 含义 |
|----|------|
| pending | 等待中 |
| running | 执行中 |
| success | 成功 |
| error | 失败 |
| skipped | 跳过 |

### ExecutionMode

| 值 | 含义 |
|----|------|
| auto | 全自动执行 |
| step_by_step | 逐步确认 |
| plan_only | 只出计划不执行 |

### ToolCategory

| 值 | 含义 |
|----|------|
| search | 查询类 |
| compute | 计算类 |
| operate | 操作类 |
| perceive | 感知类 |
| notify | 通知类 |
| custom | 自定义 |

### MemoryStrategy

| 值 | 含义 |
|----|------|
| sliding_window | 滑动窗口 |
| summary | 摘要压缩 |
| full | 全量保留 |

### BudgetPeriod

| 值 | 含义 |
|----|------|
| daily | 日预算 |
| monthly | 月预算 |

### BudgetScope

| 值 | 含义 |
|----|------|
| workspace | 工作空间 |
| user | 个人 |
| agent | 单个 Agent |

### NotifyChannel

| 值 | 含义 |
|----|------|
| feishu | 飞书 |
| wecom | 企业微信 |
| email | 邮件 |
| webhook | Webhook |

### WorkflowNodeType

| 值 | 含义 |
|----|------|
| agent | Agent 节点 |
| condition | 条件分支 |
| parallel | 并行 |
| approval | 人工审批 |
| sub_workflow | 子流程 |

### WorkflowTriggerType

| 值 | 含义 |
|----|------|
| manual | 手动触发 |
| cron | 定时触发 |
| webhook | HTTP 回调触发 |
