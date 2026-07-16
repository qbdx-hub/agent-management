# Agent 管理系统 - 数据库 ER 关系图

## 📊 数据库概览

- **数据库名**: agent_management
- **表总数**: 30 张
- **字符集**: utf8mb4
- **核心设计**: 以工作空间(workspace)为隔离单元，实现多租户数据隔离

---

## 🏗️ 模块划分

### 1️⃣ 用户与权限模块（5张表）

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| `user` | 用户表 | id, username, password, email, status |
| `role` | 角色表 | id, workspace_id, name, permissions(JSON) |
| `user_role` | 用户角色关联表 | user_id, role_id, workspace_id |
| `workspace` | 工作空间表 | id, name, owner_id, max_agents, max_members |
| `workspace_member` | 工作空间成员表 | workspace_id, user_id, role(owner/admin/member) |

### 2️⃣ Agent 管理模块（3张表）

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| `agent` | Agent 表 | id, workspace_id, name, model_provider, system_prompt |
| `agent_prompt_version` | 提示词版本表 | agent_id, version_number, system_prompt |
| `agent_tool_binding` | Agent工具绑定表 | agent_id, tool_id, enabled |

### 3️⃣ 工具市场模块（2张表）

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| `tool` | 工具表 | id, workspace_id, name, type(api/mcp/builtin), endpoint_url |
| `tool_call_record` | 工具调用记录表 | tool_id, agent_id, session_id, success, latency_ms |

### 4️⃣ 会话模块（4张表）

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| `session` | 会话表 | id, workspace_id, agent_id, status, total_tokens |
| `message` | 消息表 | session_id, role(user/assistant/system/tool), content |
| `execution_step` | 执行步骤表 | session_id, step_type, tool_name, status |
| `error_log` | 错误日志表 | workspace_id, agent_id, session_id, error_type |

### 5️⃣ 监控告警模块（2张表）

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| `alert_rule` | 告警规则表 | workspace_id, metric, condition, threshold, severity |
| `alert_record` | 告警记录表 | rule_id, agent_id, severity, status, triggered_at |

### 6️⃣ 成本管理模块（2张表）

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| `cost_record` | 费用记录表 | workspace_id, agent_id, model_name, tokens, cost |
| `budget` | 预算配置表 | workspace_id, scope, period, limit_amount, current_amount |

### 7️⃣ 安全审计模块（4张表）

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| `audit_log` | 审计日志表 | workspace_id, user_id, action, resource_type, resource_id |
| `approval_rule` | 审批规则表 | workspace_id, resource_type, trigger_action, approver_role |
| `approval` | 审批记录表 | rule_id, applicant_id, approver_id, status |
| `api_key` | API Key表 | workspace_id, provider, key_hash, status |

### 8️⃣ 工作流模块（4张表）

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| `workflow` | 工作流表 | id, workspace_id, name, status, trigger_type |
| `workflow_node` | 工作流节点表 | workflow_id, node_id, type, agent_id, tool_id |
| `workflow_edge` | 工作流边表 | workflow_id, edge_id, source_node_id, target_node_id |
| `workflow_run` | 运行记录表 | workflow_id, status, input, output, node_results |

### 9️⃣ 知识库模块（2张表）

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| `knowledge_base` | 知识库表 | id, workspace_id, name, type(vector/keyword/hybrid) |
| `document` | 文档表 | knowledge_base_id, name, file_type, chunk_count, status |

### 🔟 其他表（2张表）

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| `activity_log` | 活动日志表 | workspace_id, user_id, type, description |
| `model_pricing` | 模型定价表 | provider, model_name, input_price_per_1k, output_price_per_1k |

---

## 🔗 ER 关系图（文字版）

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              用户与权限模块                                           │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                      │
│   ┌──────────┐    N:N     ┌──────────┐    N:1     ┌──────────┐                      │
│   │   user   │◄──────────►│ user_role│◄──────────►│   role   │                      │
│   └────┬─────┘            └──────────┘            └──────────┘                      │
│        │                                                                             │
│        │ 1:N                                                                         │
│        ▼                                                                             │
│   ┌──────────┐    1:N     ┌────────────────┐                                        │
│   │workspace │◄───────────│workspace_member│                                        │
│   └────┬─────┘            └────────────────┘                                        │
│        │                                                                             │
└────────┼─────────────────────────────────────────────────────────────────────────────┘
         │
         │ 1:N (所有业务表通过 workspace_id 关联)
         │
         ├──────────────────────────────────────────────────────────────────────────────┐
         │                              Agent 管理模块                                    │
         ├──────────────────────────────────────────────────────────────────────────────┤
         │                                                                              │
         │   ┌──────────┐    1:N     ┌─────────────────────┐                           │
         │   │  agent   │◄───────────│  agent_prompt_version│                           │
         │   └────┬─────┘            └─────────────────────┘                           │
         │        │                                                                     │
         │        │ N:N (通过 agent_tool_binding)                                       │
         │        ▼                                                                     │
         │   ┌──────────┐                                                              │
         │   │   tool   │                                                              │
         │   └──────────┘                                                              │
         │                                                                              │
         ├──────────────────────────────────────────────────────────────────────────────┤
         │                              会话模块                                         │
         ├──────────────────────────────────────────────────────────────────────────────┤
         │                                                                              │
         │   ┌──────────┐    1:N     ┌──────────┐    1:N     ┌────────────────┐        │
         │   │  agent   │◄───────────│ session  │◄───────────│    message     │        │
         │   └──────────┘            └────┬─────┘            └────────────────┘        │
         │                                │                                             │
         │                                │ 1:N                                         │
         │                                ▼                                             │
         │                          ┌────────────────┐    N:1    ┌──────────┐          │
         │                          │ execution_step │◄─────────►│error_log │          │
         │                          └────────────────┘           └──────────┘          │
         │                                                                              │
         ├──────────────────────────────────────────────────────────────────────────────┤
         │                              监控告警模块                                      │
         ├──────────────────────────────────────────────────────────────────────────────┤
         │                                                                              │
         │   ┌──────────┐    1:N     ┌──────────────┐                                  │
         │   │alert_rule│◄───────────│ alert_record │                                  │
         │   └──────────┘            └──────────────┘                                  │
         │                                                                              │
         ├──────────────────────────────────────────────────────────────────────────────┤
         │                              成本管理模块                                      │
         ├──────────────────────────────────────────────────────────────────────────────┤
         │                                                                              │
         │   ┌──────────┐         ┌──────────────┐                                     │
         │   │  budget  │         │ cost_record  │                                     │
         │   └──────────┘         └──────────────┘                                     │
         │                                                                              │
         ├──────────────────────────────────────────────────────────────────────────────┤
         │                              安全审计模块                                      │
         ├──────────────────────────────────────────────────────────────────────────────┤
         │                                                                              │
         │   ┌──────────────┐    1:N     ┌──────────┐                                  │
         │   │ approval_rule│◄───────────│ approval │                                  │
         │   └──────────────┘            └──────────┘                                  │
         │                                                                              │
         │   ┌──────────┐    ┌──────────┐                                              │
         │   │audit_log │    │ api_key  │                                              │
         │   └──────────┘    └──────────┘                                              │
         │                                                                              │
         ├──────────────────────────────────────────────────────────────────────────────┤
         │                              工作流模块                                        │
         ├──────────────────────────────────────────────────────────────────────────────┤
         │                                                                              │
         │   ┌──────────┐    1:N     ┌──────────────┐    1:N     ┌──────────────┐      │
         │   │ workflow │◄───────────│ workflow_node│            │ workflow_edge│      │
         │   └────┬─────┘            └──────────────┘            └──────────────┘      │
         │        │                                                                     │
         │        │ 1:N                                                                 │
         │        ▼                                                                     │
         │   ┌──────────────┐                                                          │
         │   │ workflow_run │                                                          │
         │   └──────────────┘                                                          │
         │                                                                              │
         ├──────────────────────────────────────────────────────────────────────────────┤
         │                              知识库模块                                        │
         ├──────────────────────────────────────────────────────────────────────────────┤
         │                                                                              │
         │   ┌───────────────┐    1:N     ┌──────────┐                                 │
         │   │knowledge_base │◄───────────│ document │                                 │
         │   └───────────────┘            └──────────┘                                 │
         │                                                                              │
         └──────────────────────────────────────────────────────────────────────────────┘
```

---

## 📝 核心关系详解

### 1. 多租户隔离关系
```
workspace (1) ──────► (N) agent
workspace (1) ──────► (N) tool
workspace (1) ──────► (N) session
workspace (1) ──────► (N) workflow
workspace (1) ──────► (N) knowledge_base
workspace (1) ──────► (N) alert_rule
workspace (1) ──────► (N) budget
workspace (1) ──────► (N) audit_log
workspace (1) ──────► (N) api_key
```
> 所有业务表都通过 `workspace_id` 与工作空间关联，实现数据隔离

### 2. Agent 核心关系
```
agent (1) ──────► (N) agent_prompt_version    // 提示词版本管理
agent (N) ◄──────► (N) tool                   // 通过 agent_tool_binding 多对多
agent (1) ──────► (N) session                 // 会话关联
agent (1) ──────► (N) cost_record             // 费用记录
agent (1) ──────► (N) error_log               // 错误日志
```

### 3. 会话执行链路
```
session (1) ──────► (N) message               // 消息列表
session (1) ──────► (N) execution_step         // 执行步骤
message (1) ──────► (N) execution_step         // 步骤关联消息
execution_step (1) ──► (N) tool_call_record    // 工具调用记录
```

### 4. 工作流编排关系
```
workflow (1) ──────► (N) workflow_node         // 节点列表
workflow (1) ──────► (N) workflow_edge         // 连线列表
workflow_node.agent_id ──► agent.id            // 节点关联Agent
workflow_node.tool_id ──► tool.id              // 节点关联工具
workflow (1) ──────► (N) workflow_run          // 运行记录
```

### 5. 知识库 RAG 关系
```
knowledge_base (1) ──────► (N) document        // 文档列表
agent.knowledge_base_ids ──► knowledge_base.id // Agent绑定知识库(多对多)
```

### 6. 权限控制关系
```
user (N) ◄──────► (N) role                     // 通过 user_role 多对多
role.workspace_id ──► workspace.id             // 角色关联工作空间
workspace_member.role ──► 角色标识              // 成员角色(owner/admin/member)
```

### 7. 审批流程关系
```
approval_rule (1) ──────► (N) approval         // 审批规则触发审批记录
approval.applicant_id ──► user.id              // 申请人
approval.approver_id ──► user.id               // 审批人
```

### 8. 告警与监控关系
```
alert_rule (1) ──────► (N) alert_record        // 规则触发告警记录
alert_rule.target_id ──► agent.id              // 规则关联Agent(可选)
alert_record.agent_id ──► agent.id             // 告警关联Agent
```

---

## 🔑 外键关系汇总

| 源表.字段 | → | 目标表.字段 | 关系类型 |
|-----------|---|------------|----------|
| workspace.owner_id | → | user.id | N:1 |
| workspace_member.workspace_id | → | workspace.id | N:1 |
| workspace_member.user_id | → | user.id | N:1 |
| user_role.user_id | → | user.id | N:1 |
| user_role.role_id | → | role.id | N:1 |
| agent.workspace_id | → | workspace.id | N:1 |
| agent.created_by | → | user.id | N:1 |
| agent_prompt_version.agent_id | → | agent.id | N:1 |
| agent_tool_binding.agent_id | → | agent.id | N:1 |
| agent_tool_binding.tool_id | → | tool.id | N:1 |
| tool.workspace_id | → | workspace.id | N:1 |
| tool_call_record.tool_id | → | tool.id | N:1 |
| tool_call_record.agent_id | → | agent.id | N:1 |
| session.workspace_id | → | workspace.id | N:1 |
| session.agent_id | → | agent.id | N:1 |
| message.session_id | → | session.id | N:1 |
| execution_step.session_id | → | session.id | N:1 |
| execution_step.message_id | → | message.id | N:1 |
| workflow.workspace_id | → | workspace.id | N:1 |
| workflow_node.workflow_id | → | workflow.id | N:1 |
| workflow_node.agent_id | → | agent.id | N:1 (可选) |
| workflow_node.tool_id | → | tool.id | N:1 (可选) |
| workflow_edge.workflow_id | → | workflow.id | N:1 |
| workflow_run.workflow_id | → | workflow.id | N:1 |
| knowledge_base.workspace_id | → | workspace.id | N:1 |
| document.knowledge_base_id | → | knowledge_base.id | N:1 |
| alert_rule.workspace_id | → | workspace.id | N:1 |
| alert_record.rule_id | → | alert_rule.id | N:1 |
| cost_record.workspace_id | → | workspace.id | N:1 |
| budget.workspace_id | → | workspace.id | N:1 |
| audit_log.workspace_id | → | workspace.id | N:1 |
| approval_rule.workspace_id | → | workspace.id | N:1 |
| approval.rule_id | → | approval_rule.id | N:1 |
| api_key.workspace_id | → | workspace.id | N:1 |

---

## 📌 设计特点

1. **多租户隔离**: 所有业务表通过 `workspace_id` 实现数据隔离
2. **冗余字段**: 常用查询字段做冗余（如 agent_name, user_name），减少关联查询
3. **JSON 字段**: 复杂配置使用 JSON 类型（如 permissions, config, parameters）
4. **软删除**: 部分表使用 status 字段标记状态而非物理删除
5. **审计追踪**: 统一 created_at, updated_at, created_by 字段
6. **统计冗余**: agent, tool 表维护统计字段（total_sessions, success_rate 等）

---

## 📊 表统计

| 模块 | 表数量 | 核心业务 |
|------|--------|----------|
| 用户与权限 | 5 | 用户、角色、工作空间、成员管理 |
| Agent 管理 | 3 | Agent配置、提示词版本、工具绑定 |
| 工具市场 | 2 | 工具注册、调用记录 |
| 会话模块 | 4 | 会话、消息、执行步骤、错误日志 |
| 监控告警 | 2 | 告警规则、告警记录 |
| 成本管理 | 2 | 费用记录、预算配置 |
| 安全审计 | 4 | 审计日志、审批流程、API Key |
| 工作流 | 4 | 工作流、节点、边、运行记录 |
| 知识库 | 2 | 知识库、文档 |
| 其他 | 2 | 活动日志、模型定价 |
| **合计** | **30** | - |
