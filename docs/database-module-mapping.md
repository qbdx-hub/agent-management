# 数据库表与前端模块对应关系

> 本文档描述每张数据库表对应哪个前端业务模块、在哪个页面使用、主要的增删改查操作。
> 表结构以 `docs/database-schema.sql` 为准。

---

## 一、认证模块 (Auth)

前端路径：`/login`
前端文件：`src/views/auth/LoginView.vue`、`src/api/auth.ts`、`src/stores/user.ts`

| 表名 | 中文名 | 用途 | 操作 |
|------|--------|------|------|
| `user` | 用户表 | 登录验证、获取当前用户信息、修改密码 | 登录查询、更新 last_login_at |

**关联说明：**
- 登录成功后返回 `user` 基本信息 + 该用户关联的 `role` 权限列表 + 所属 `workspace` 列表
- Token 由后端生成（JWT），不需要存数据库

---

## 二、仪表盘模块 (Dashboard)

前端路径：`/dashboard`
前端文件：`src/views/dashboard/DashboardView.vue`

| 表名 | 中文名 | 用途 | 操作 |
|------|--------|------|------|
| `agent` | Agent表 | 展示 Agent 卡片列表、统计数据 | 只读查询 |
| `session` | 会话表 | 最近任务列表 | 只读查询 |
| `workspace` | 工作空间表 | 当前工作空间信息 | 只读查询 |

**关联说明：**
- 仪表盘是**聚合页面**，从多张表读取统计值，不直接写入任何表
- 快速创建 Agent 操作实际调用的是 Agent 模块的创建接口

---

## 三、Agent 管理模块 (Agent)

前端路径：`/agent/list`、`/agent/create`、`/agent/:id/*`
前端文件：`src/views/agent/*.vue`、`src/api/agent.ts`、`src/stores/agent.ts`

| 表名 | 中文名 | 用途 | 操作 |
|------|--------|------|------|
| `agent` | Agent表 | Agent 的增删改查、状态管理、配置 | 增删改查 |
| `agent_prompt_version` | Agent提示词版本表 | 提示词版本历史、回滚 | 增查 |
| `agent_tool_binding` | Agent工具绑定表 | Agent 绑定/解绑工具 | 增删改查 |
| `model_pricing` | 模型定价参考表 | 配置模型时展示可选模型列表和价格 | 只读查询 |

**关联说明：**

| 前端页面 | 涉及表 | 说明 |
|---------|--------|------|
| AgentList.vue | `agent` | 分页列表、搜索筛选、状态筛选 |
| AgentCreate.vue | `agent` | 3 步向导创建（基本信息→模型配置→确认） |
| AgentDetail.vue | `agent` | Agent 详情（Tab 容器，包含下面 4 个子页面） |
| AgentConfig.vue | `agent` | 修改名称/描述/头像/标签/模型配置 |
| PromptEditor.vue | `agent`(system_prompt)、`agent_prompt_version` | 编辑提示词、查看版本历史、回滚版本 |
| ToolBinding.vue | `agent_tool_binding`、`tool` | 展示工具列表、绑定/解绑工具 |
| MemoryConfig.vue | `agent`(memory 字段) | 配置工作窗口、记忆策略、知识库绑定 |
| AgentConfig.vue (执行配置) | `agent`(execution 字段) | 配置最大步数、超时、反思深度 |

---

## 四、工具市场模块 (Tool)

前端路径：`/tool/market`、`/tool/register`、`/tool/:id`
前端文件：`src/views/tool/*.vue`、`src/api/tool.ts`

| 表名 | 中文名 | 用途 | 操作 |
|------|--------|------|------|
| `tool` | 工具表 | 工具的增删改查、MCP 注册 | 增删改查 |
| `tool_call_record` | 工具调用记录表 | 工具详情页的"最近调用"和统计 | 只读查询、后端写入 |

**关联说明：**

| 前端页面 | 涉及表 | 说明 |
|---------|--------|------|
| ToolMarket.vue | `tool` | 分类 Tab + 搜索 + 工具卡片网格 |
| ToolRegister.vue | `tool` | 完整注册表单（基础信息、端点、参数、响应映射、高级） |
| ToolDetail.vue (信息) | `tool` | 展示工具详情 |
| ToolDetail.vue (测试) | `tool` | 动态参数表单 → 调用测试 → 展示结果（测试结果不入库） |
| ToolDetail.vue (统计) | `tool_call_record` | 调用量趋势、成功率、延迟、Top Agent |
| ToolDetail.vue (最近调用) | `tool_call_record` | 调用历史记录表 |

---

## 五、会话控制台模块 (Session)

前端路径：`/session`、`/session/:id`
前端文件：`src/views/session/*.vue`、`src/api/session.ts`、`src/stores/session.ts`

| 表名 | 中文名 | 用途 | 操作 |
|------|--------|------|------|
| `session` | 会话表 | 创建会话、会话列表、状态管理 | 增删改查 |
| `message` | 消息表 | 发送消息、消息列表 | 增查 |
| `execution_step` | 执行步骤表 | Agent 执行的每一步（思考、工具调用、反思） | 增查、后端写入 |

**关联说明：**

| 前端页面 | 涉及表 | 说明 |
|---------|--------|------|
| SessionHistory.vue | `session` | 历史会话列表、状态筛选 |
| SessionConsole.vue | `session`、`message`、`execution_step` | 完整聊天界面 |

**SessionConsole 数据流：**
```
用户发送消息 → 写入 message(role=user)
    ↓
后端执行 Agent → 写入 execution_step(每一步)
    ↓
Agent 回复完成 → 写入 message(role=assistant)
    ↓
前端通过 SSE 实时接收 steps + message 更新
```

**SessionConsole 支持 3 种执行模式：**
- `auto` — 全自动，完成后一次性返回
- `step_by_step` — 逐步执行，每步返回
- `plan_only` — 只出计划，不执行

---

## 六、监控模块 (Monitor)

前端路径：`/monitor`、`/monitor/trace/:id`、`/monitor/alerts`
前端文件：`src/views/monitor/*.vue`、`src/api/monitor.ts`

| 表名 | 中文名 | 用途 | 操作 |
|------|--------|------|------|
| `error_log` | 错误日志表 | 错误列表、错误类型筛选 | 只读查询、后端写入 |
| `alert_rule` | 告警规则表 | 告警规则的增删改查 | 增删查 |
| `alert_record` | 告警记录表 | 告警触发记录、告警历史 | 只读查询、后端写入 |
| `agent` | Agent表 | Agent 健康状态（成功率、延迟统计） | 只读查询 |
| `session` | 会话表 | 运行中任务数、今日调用量 | 只读查询 |
| `execution_step` | 执行步骤表 | Trace 详情（执行链路追踪） | 只读查询 |

**关联说明：**

| 前端页面 | 涉及表 | 说明 |
|---------|--------|------|
| MonitorDashboard.vue (指标卡片) | `agent`、`session`、`execution_step` | 聚合统计：活跃 Agent 数、运行任务数、今日调用、成功率、平均延迟、P99 |
| MonitorDashboard.vue (Token趋势) | `cost_record` 或 `message` | 按时间聚合 Token 用量 |
| MonitorDashboard.vue (Agent健康) | `agent`、`session`、`execution_step` | 每个 Agent 的成功率、延迟、错误摘要 |
| MonitorDashboard.vue (告警列表) | `alert_record` | 最近触发的告警 |
| MonitorDashboard.vue (错误日志) | `error_log` | 最近错误列表 |
| TraceDetail.vue | `session`、`execution_step` | 某个会话的完整执行链路（el-timeline） |
| AlertConfig.vue | `alert_rule` | 告警规则列表 + 创建/删除规则 |

**监控模块的聚合指标（后端需要计算，不是直接查表）：**

| 指标 | 计算方式 |
|------|---------|
| activeAgentCount | `SELECT COUNT(*) FROM agent WHERE status='published'` |
| runningTaskCount | `SELECT COUNT(*) FROM session WHERE status='active'` |
| todayCallCount | `SELECT COUNT(*) FROM session WHERE DATE(created_at)=CURDATE()` |
| successRate | `SELECT ... FROM execution_step` 聚合 |
| avgLatencyMs | `SELECT AVG(duration_ms) FROM execution_step` |
| p99LatencyMs | `SELECT ... PERCENTILE ... FROM execution_step` |
| Agent 健康状态 | 按 agent_id 分组聚合 execution_step 的成功率和延迟 |

---

## 七、费用模块 (Cost)

前端路径：`/cost`
前端文件：`src/views/cost/*.vue`、`src/api/cost.ts`

| 表名 | 中文名 | 用途 | 操作 |
|------|--------|------|------|
| `cost_record` | 费用记录表 | 费用明细、按模型/Agent 分组统计、趋势图 | 只读查询、后端写入 |
| `budget` | 预算配置表 | 预算规则的增删改查 | 增删查 |
| `model_pricing` | 模型定价参考表 | 费用计算基础数据 | 只读查询 |

**关联说明：**

| 前端页面 | 涉及表 | 说明 |
|---------|--------|------|
| CostDashboard.vue (概览卡片) | `cost_record` | 本月总费用、本月 Token、今日费用、预算使用率 |
| CostDashboard.vue (预算进度) | `budget`、`cost_record` | 各预算规则的使用进度条 |
| CostDashboard.vue (模型分布) | `cost_record` | 按 model_name 分组的费用占比 |
| CostDashboard.vue (Agent分布) | `cost_record` | 按 agent_id 分组的费用排名 |
| CostDashboard.vue (趋势图) | `cost_record` | 按天聚合的费用趋势（30 天） |
| CostDashboard.vue (费用明细) | `cost_record` | 费用记录表格、筛选 |
| BudgetConfig.vue | `budget` | 预算规则列表 + 创建/删除 |

**费用写入时机：** 每次 Agent 执行完成（message 写入时），后端根据 token 数量 × 模型单价，自动写入 `cost_record`。

---

## 八、安全模块 (Security)

前端路径：`/security/audit`、`/security/roles`、`/security/approvals`
前端文件：`src/views/security/*.vue`、`src/api/security.ts`

| 表名 | 中文名 | 用途 | 操作 |
|------|--------|------|------|
| `role` | 角色表 | 角色列表、权限展示 | 查 |
| `user_role` | 用户角色关联表 | 用户与角色的绑定关系 | 后端维护 |
| `audit_log` | 审计日志表 | 操作审计记录 | 只读查询、后端写入 |
| `approval_rule` | 审批规则表 | 审批规则列表 | 查（前端未做创建页面） |
| `approval` | 审批记录表 | 待审批列表、审批操作 | 查、更新状态(批准/拒绝) |
| `api_key` | API Key表 | API Key 管理 | 增删查 |

**关联说明：**

| 前端页面 | 涉及表 | 说明 |
|---------|--------|------|
| RoleManage.vue | `role`、`user_role` | 角色卡片 + 权限标签展示（系统角色不可编辑） |
| AuditLog.vue | `audit_log` | 操作日志表格、按操作类型/用户/时间筛选 |
| ApprovalList.vue | `approval` | 待审批/已批准/已拒绝三个 Tab，可批准/拒绝待审批项 |
| (无独立页面) | `api_key` | API Key 创建和管理（前端有接口但页面可集成到设置中） |

**审计日志写入时机：** 后端中间件拦截所有写操作，自动记录到 `audit_log`。主要记录的操作：
- Agent 的创建、修改、删除、发布
- 工具的注册、修改、删除
- 成员的邀请、移除、角色变更
- 审批的批准、拒绝

---

## 九、工作空间模块 (Workspace)

前端路径：`/workspace/settings`、`/workspace/members`
前端文件：`src/views/workspace/*.vue`、`src/api/workspace.ts`、`src/stores/workspace.ts`

| 表名 | 中文名 | 用途 | 操作 |
|------|--------|------|------|
| `workspace` | 工作空间表 | 工作空间列表、设置修改 | 查改、创建 |
| `workspace_member` | 工作空间成员表 | 成员列表、邀请/移除成员、角色变更 | 增删改查 |
| `activity_log` | 活动日志表 | 工作空间动态（最近操作记录） | 只读查询、后端写入 |

**关联说明：**

| 前端页面 | 涉及表 | 说明 |
|---------|--------|------|
| TopHeader.vue (工作空间切换) | `workspace`、`workspace_member` | 下拉切换工作空间 |
| WorkspaceSettings.vue | `workspace` | 修改名称/描述/设置、危险区（删除工作空间） |
| MemberManage.vue | `workspace_member`、`user` | 成员表格 + 邀请弹窗 + 角色下拉 + 移除 |
| DashboardView.vue (最近动态) | `activity_log` | 工作空间最近操作列表 |

**工作空间设置字段（存在 workspace 表中）：**
- `default_model_provider` — 默认模型提供商
- `session_retention_days` — 会话保留天数
- `auto_archive_days` — 自动归档天数
- `max_tokens_per_task` — 单任务最大 Token
- `language` — 语言

---

## 十、编排模块 (Orchestration)

前端路径：`/orchestration`、`/orchestration/editor/:id`、`/orchestration/run/:id`
前端文件：`src/views/orchestration/*.vue`

| 表名 | 中文名 | 用途 | 操作 |
|------|--------|------|------|
| `workflow` | 工作流表 | 工作流列表、创建、基本信息 | 增删改查 |
| `workflow_node` | 工作流节点表 | 画布中的节点（Agent/工具/条件/开始/结束） | 增删改查 |
| `workflow_edge` | 工作流边表 | 节点之间的连线 | 增删改查 |
| `workflow_run` | 工作流运行记录表 | 执行历史、执行结果 | 查、后端写入 |

**关联说明：**

| 前端页面 | 涉及表 | 说明 |
|---------|--------|------|
| WorkflowList.vue | `workflow` | 工作流表格列表、创建/删除 |
| WorkflowEditor.vue | `workflow`、`workflow_node`、`workflow_edge` | SVG 画布编辑器，支持拖拽添加节点、连线、配置属性 |
| WorkflowRun.vue | `workflow_run`、`workflow_node` | 执行记录详情，el-timeline 展示各节点执行状态 |

**工作流编辑器数据流：**
```
编辑画布 → 前端维护 nodes[] + edges[] 状态
    ↓ 保存
调用 API → 后端写入 workflow_node + workflow_edge（先删后插）
    ↓ 执行
前端调用执行 API → 后端创建 workflow_run，按拓扑顺序执行各节点
    ↓ 执行完成
后端更新 workflow_run 状态 + node_results JSON
```

---

## 十一、知识库模块 (Knowledge)

前端路径：`/knowledge`、`/knowledge/:id`
前端文件：`src/views/knowledge/*.vue`

| 表名 | 中文名 | 用途 | 操作 |
|------|--------|------|------|
| `knowledge_base` | 知识库表 | 知识库列表、创建、设置 | 增删改查 |
| `document` | 文档表 | 文档上传、文档列表、文档处理状态 | 增删查 |

**关联说明：**

| 前端页面 | 涉及表 | 说明 |
|---------|--------|------|
| KnowledgeList.vue | `knowledge_base` | 知识库卡片网格 + 创建弹窗 |
| KnowledgeDetail.vue (文档) | `document` | 文档上传 + 文档表格（状态: pending/processing/completed/failed） |
| KnowledgeDetail.vue (搜索测试) | `document` | 输入查询 → 后端向量化搜索 → 返回相关文档片段 |
| KnowledgeDetail.vue (设置) | `knowledge_base` | 修改知识库配置（分块大小、重叠、嵌入模型等） |

**知识库与 Agent 的关系：**
- Agent 的 `knowledge_base_ids` 字段关联知识库（一对多）
- Agent 执行时，会检索绑定的知识库作为上下文

---

## 十二、跨模块/基础数据表

| 表名 | 中文名 | 被哪些模块使用 | 说明 |
|------|--------|---------------|------|
| `model_pricing` | 模型定价参考表 | Agent（配置选模型）、Cost（费用计算） | 模型名、提供商、单价 |

---

## 表-模块完整对照表

| # | 表名 | 中文名 | 所属模块 | 前端读 | 前端写 | 后端写 |
|---|------|--------|---------|--------|--------|--------|
| 1 | `user` | 用户表 | Auth | ✅ | ❌ | ✅ |
| 2 | `workspace` | 工作空间表 | Workspace | ✅ | ✅ | ✅ |
| 3 | `workspace_member` | 工作空间成员表 | Workspace | ✅ | ✅ | ✅ |
| 4 | `role` | 角色表 | Security | ✅ | ❌ | ✅ |
| 5 | `user_role` | 用户角色关联表 | Security | ✅ | ❌ | ✅ |
| 6 | `agent` | Agent表 | Agent | ✅ | ✅ | ✅ |
| 7 | `agent_prompt_version` | 提示词版本表 | Agent | ✅ | ✅ | ✅ |
| 8 | `agent_tool_binding` | 工具绑定表 | Agent | ✅ | ✅ | ✅ |
| 9 | `tool` | 工具表 | Tool | ✅ | ✅ | ✅ |
| 10 | `tool_call_record` | 工具调用记录表 | Tool | ✅ | ❌ | ✅ |
| 11 | `session` | 会话表 | Session | ✅ | ✅ | ✅ |
| 12 | `message` | 消息表 | Session | ✅ | ✅ | ✅ |
| 13 | `execution_step` | 执行步骤表 | Session / Monitor | ✅ | ❌ | ✅ |
| 14 | `error_log` | 错误日志表 | Monitor | ✅ | ❌ | ✅ |
| 15 | `alert_rule` | 告警规则表 | Monitor | ✅ | ✅ | ✅ |
| 16 | `alert_record` | 告警记录表 | Monitor | ✅ | ❌ | ✅ |
| 17 | `cost_record` | 费用记录表 | Cost | ✅ | ❌ | ✅ |
| 18 | `budget` | 预算配置表 | Cost | ✅ | ✅ | ✅ |
| 19 | `audit_log` | 审计日志表 | Security | ✅ | ❌ | ✅ |
| 20 | `approval_rule` | 审批规则表 | Security | ✅ | ❌ | ✅ |
| 21 | `approval` | 审批记录表 | Security | ✅ | ✅ | ✅ |
| 22 | `api_key` | API Key表 | Security | ✅ | ✅ | ✅ |
| 23 | `workflow` | 工作流表 | Orchestration | ✅ | ✅ | ✅ |
| 24 | `workflow_node` | 工作流节点表 | Orchestration | ✅ | ✅ | ✅ |
| 25 | `workflow_edge` | 工作流边表 | Orchestration | ✅ | ✅ | ✅ |
| 26 | `workflow_run` | 工作流运行记录表 | Orchestration | ✅ | ❌ | ✅ |
| 27 | `knowledge_base` | 知识库表 | Knowledge | ✅ | ✅ | ✅ |
| 28 | `document` | 文档表 | Knowledge | ✅ | ✅ | ✅ |
| 29 | `activity_log` | 活动日志表 | Workspace | ✅ | ❌ | ✅ |
| 30 | `model_pricing` | 模型定价参考表 | 跨模块 | ✅ | ❌ | ✅ |

---

## 后端开发优先级建议

| 优先级 | 模块 | 表 | 原因 |
|--------|------|-----|------|
| P0 | Auth + Workspace | `user`、`workspace`、`workspace_member` | 登录和工作空间是所有功能的基础 |
| P0 | Agent | `agent`、`agent_prompt_version`、`agent_tool_binding` | 核心业务 |
| P0 | Tool | `tool` | Agent 依赖工具 |
| P0 | Session | `session`、`message`、`execution_step` | 核心交互 |
| P1 | Monitor | `error_log`、`alert_rule`、`alert_record` | 运维必需 |
| P1 | Cost | `cost_record`、`budget` | 费用管控 |
| P1 | Security | `role`、`user_role`、`audit_log`、`approval`、`api_key` | 权限控制 |
| P2 | Knowledge | `knowledge_base`、`document` | 增强功能 |
| P2 | Orchestration | `workflow`、`workflow_node`、`workflow_edge`、`workflow_run` | 高级功能 |
