# Agent 管理系统 — 前端架构设计

> Vue 3 + TypeScript + Vite | 对应 PRD 全部 10 个模块

---

## 一、技术选型

| 类别 | 选型 | 说明 |
|------|------|------|
| 框架 | Vue 3.5+ (Composition API) | `<script setup lang="ts">` |
| 语言 | TypeScript 6.0 | 全量 TS |
| 构建 | Vite 8.0+ | 极速 HMR |
| 路由 | Vue Router 4 | 动态路由 + 路由守卫 |
| 状态管理 | Pinia | 模块化 Store |
| HTTP 客户端 | Axios | 请求/响应拦截器 |
| UI 组件库 | Element Plus | 表格/表单/弹窗/菜单 开箱即用 |
| 图标 | `@element-plus/icons-vue` | 统一图标 |
| CSS 方案 | UnoCSS / Tailwind CSS | 原子化 CSS，按需生成 |
| 代码编辑器 | Monaco Editor (`@guolao/vue-monaco-editor`) | Prompt 编辑、JSON Schema 编辑 |
| 流程图 | Vue Flow (`@vue-flow/core`) | 编排画布拖拽连线 |
| 图表 | ECharts | 监控仪表盘图表 |
| Markdown | `markdown-it` + `highlight.js` | 会话消息渲染 |
| SSE | `@microsoft/fetch-event-source` | Agent 流式输出 |
| 工具函数 | VueUse | 通用 composables |
| 包管理 | pnpm | workspace monorepo |

---

## 二、目录结构

```
frontend/
├── public/
│   └── favicon.svg
├── src/
│   ├── api/                          # 接口请求层
│   │   ├── index.ts                  # Axios 实例 + 拦截器
│   │   ├── types.ts                  # 通用 API 类型
│   │   ├── agent.ts                  # Agent CRUD 接口
│   │   ├── agentConfig.ts            # 配置相关接口
│   │   ├── tool.ts                   # 工具市场接口
│   │   ├── session.ts                # 会话接口
│   │   ├── execution.ts              # Agent 执行接口（SSE）
│   │   ├── monitor.ts               # 监控接口
│   │   ├── cost.ts                   # 成本接口
│   │   ├── security.ts              # 安全审计接口
│   │   ├── workspace.ts             # 工作空间接口
│   │   └── auth.ts                   # 认证接口
│   │
│   ├── router/                       # 路由
│   │   ├── index.ts                  # 路由实例
│   │   ├── routes.ts                 # 路由表
│   │   └── guards.ts                 # 路由守卫（权限）
│   │
│   ├── stores/                       # Pinia 状态管理
│   │   ├── user.ts                   # 用户信息、权限
│   │   ├── workspace.ts             # 当前工作空间
│   │   ├── agent.ts                  # Agent 列表 + 当前编辑的 Agent
│   │   ├── session.ts               # 当前会话 + 消息列表
│   │   └── app.ts                    # 全局 UI 状态（侧边栏折叠等）
│   │
│   ├── views/                        # 页面
│   │   ├── workspace/               # 模块一：Agent 工作台
│   │   │   ├── DashboardView.vue     #   工作台首页
│   │   │   └── components/
│   │   │       ├── AgentCard.vue     #     Agent 卡片
│   │   │       ├── RecentTasks.vue   #     最近任务列表
│   │   │       └── QuickCreate.vue   #     快速创建弹窗
│   │   │
│   │   ├── agent/                    # 模块二：Agent 配置中心
│   │   │   ├── AgentList.vue         #    Agent 列表页
│   │   │   ├── AgentCreate.vue       #    创建 Agent 向导
│   │   │   ├── AgentDetail.vue       #    Agent 详情（Tab 容器）
│   │   │   ├── AgentConfig.vue       #    基础信息 + 模型配置
│   │   │   ├── PromptEditor.vue      #    System Prompt 编辑器
│   │   │   ├── ToolBinding.vue       #    工具绑定页
│   │   │   ├── MemoryConfig.vue      #    记忆策略配置
│   │   │   └── components/
│   │   │       ├── ModelSelector.vue #      模型选择器
│   │   │       ├── VariableEditor.vue#      变量编辑器
│   │   │       └── VersionDiff.vue   #      版本对比
│   │   │
│   │   ├── tool/                     # 模块三：工具与插件管理
│   │   │   ├── ToolMarket.vue        #    工具市场列表
│   │   │   ├── ToolRegister.vue      #    注册 API 工具
│   │   │   ├── ToolDetail.vue        #    工具详情 + 测试
│   │   │   └── components/
│   │   │       ├── ToolCard.vue      #      工具卡片
│   │   │       ├── ParamSchemaEditor.vue  #  参数 Schema 编辑
│   │   │       └── ToolTestPanel.vue #      工具测试面板
│   │   │
│   │   ├── orchestration/           # 模块四：Agent 编排引擎
│   │   │   ├── WorkflowList.vue      #    工作流列表
│   │   │   ├── WorkflowEditor.vue    #    编排画布（Vue Flow）
│   │   │   ├── WorkflowRun.vue       #    运行记录详情
│   │   │   └── components/
│   │   │       ├── AgentNode.vue     #     画布Agent节点
│   │   │       ├── ConditionNode.vue #     条件判断节点
│   │   │       └── NodeEditor.vue    #     节点属性编辑面板
│   │   │
│   │   ├── knowledge/               # 模块五：记忆与知识库
│   │   │   ├── KnowledgeList.vue     #    知识库列表
│   │   │   ├── KnowledgeDetail.vue   #    知识库详情
│   │   │   └── components/
│   │   │       ├── DocumentUpload.vue#      文档上传
│   │   │       └── ChunkPreview.vue  #      分块预览
│   │   │
│   │   ├── session/                  # 模块六：会话与任务控制台
│   │   │   ├── SessionConsole.vue    #    对话界面（核心）
│   │   │   ├── SessionHistory.vue    #    会话历史列表
│   │   │   └── components/
│   │   │       ├── ChatMessage.vue   #      聊天消息气泡
│   │   │       ├── StepTimeline.vue  #      步骤执行时间线
│   │   │       ├── ToolCallCard.vue  #      工具调用详情卡片
│   │   │       ├── ThinkingBlock.vue #      思考过程展示
│   │   │       └── ExecutionBar.vue  #      执行模式切换栏
│   │   │
│   │   ├── monitor/                  # 模块七：监控与可观测性
│   │   │   ├── MonitorDashboard.vue  #    监控总览
│   │   │   ├── TraceDetail.vue       #    调用链路详情
│   │   │   ├── AlertConfig.vue       #    告警规则配置
│   │   │   └── components/
│   │   │       ├── MetricCard.vue    #      指标卡片
│   │   │       ├── LatencyChart.vue  #      延迟图表
│   │   │       └── AgentHealth.vue   #      Agent 健康排行
│   │   │
│   │   ├── cost/                     # 模块八：成本与资源管理
│   │   │   ├── CostDashboard.vue     #    成本总览
│   │   │   ├── BudgetConfig.vue      #    预算配置
│   │   │   └── components/
│   │   │       ├── CostBreakdown.vue #      费用拆解图
│   │   │       └── CostTrend.vue     #      成本趋势图
│   │   │
│   │   ├── security/                # 模块九：安全与治理
│   │   │   ├── AuditLog.vue          #    审计日志
│   │   │   ├── RoleManage.vue        #    角色权限管理
│   │   │   ├── ApprovalList.vue      #    审批列表
│   │   │   └── components/
│   │   │       ├── PermissionTree.vue#      权限树
│   │   │       └── PolicyEditor.vue  #      安全策略编辑
│   │   │
│   │   └── workspace/               # 模块十：团队与工作空间
│   │       ├── WorkspaceSettings.vue  #   空间设置
│   │       ├── MemberManage.vue       #   成员管理
│   │       └── components/
│   │           ├── MemberTable.vue    #     成员表格
│   │           └── InviteModal.vue    #     邀请弹窗
│   │
│   ├── components/                   # 全局公共组件
│   │   ├── layout/
│   │   │   ├── AppLayout.vue         #   整体布局（侧边栏+顶栏+内容）
│   │   │   ├── SidebarMenu.vue       #   侧边导航菜单
│   │   │   ├── TopHeader.vue         #   顶部栏
│   │   │   └── BreadcrumbNav.vue     #   面包屑
│   │   ├── common/
│   │   │   ├── StatusBadge.vue       #   状态标签（草稿/已发布等）
│   │   │   ├── ConfirmDialog.vue     #   确认弹窗
│   │   │   ├── EmptyState.vue        #   空状态占位
│   │   │   ├── SearchInput.vue       #   搜索输入框
│   │   │   ├── FilterBar.vue         #   筛选条
│   │   │   ├── Pagination.vue        #   分页
│   │   │   └── JsonEditor.vue        #   JSON 编辑器（Monaco）
│   │   └── agent/
│   │       ├── AgentSelector.vue     #   Agent 下拉选择器
│   │       └── ToolSelector.vue      #   工具下拉选择器
│   │
│   ├── composables/                  # 组合式函数
│   │   ├── useSSE.ts                 #   流式输出 SSE 封装
│   │   ├── useAgent.ts               #   Agent 增删改查逻辑
│   │   ├── usePermission.ts          #   权限检查
│   │   └── useDebounce.ts            #   防抖/throttle
│   │
│   ├── utils/
│   │   ├── format.ts                 #   日期/数字/Token 格式化
│   │   ├── validate.ts               #   表单校验规则
│   │   └── constants.ts             #   枚举常量
│   │
│   ├── types/                        # 全局 TypeScript 类型
│   │   ├── agent.ts                  #   Agent 相关类型
│   │   ├── tool.ts                   #   工具相关类型
│   │   ├── session.ts               #   会话相关类型
│   │   ├── monitor.ts               #   监控相关类型
│   │   └── common.ts                 #   通用类型（分页、响应体等）
│   │
│   ├── assets/
│   │   └── logo.svg
│   ├── App.vue
│   ├── main.ts
│   └── style.css
│
├── index.html
├── package.json
├── vite.config.ts
├── tsconfig.json
├── tsconfig.app.json
├── tsconfig.node.json
└── .env.development
```

---

## 三、路由表

```typescript
// src/router/routes.ts

import type { RouteRecordRaw } from 'vue-router'

export const routes: RouteRecordRaw[] = [
  // ==================== 布局容器 ====================
  {
    path: '/',
    component: () => import('@/components/layout/AppLayout.vue'),
    redirect: '/dashboard',
    children: [

      // ===== 模块一：工作台 =====
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/workspace/DashboardView.vue'),
        meta: { title: '工作台', icon: 'HomeFilled' },
      },

      // ===== 模块二：Agent 管理 =====
      {
        path: 'agents',
        name: 'AgentList',
        component: () => import('@/views/agent/AgentList.vue'),
        meta: { title: 'Agent 管理', icon: 'Cpu' },
      },
      {
        path: 'agents/create',
        name: 'AgentCreate',
        component: () => import('@/views/agent/AgentCreate.vue'),
        meta: { title: '创建 Agent', hidden: true },
      },
      {
        path: 'agents/:id',
        name: 'AgentDetail',
        component: () => import('@/views/agent/AgentDetail.vue'),
        meta: { title: 'Agent 详情', hidden: true },
        children: [
          { path: '', redirect: { name: 'AgentConfig' } },
          { path: 'config', name: 'AgentConfig', component: () => import('@/views/agent/AgentConfig.vue'), meta: { title: '基础配置' } },
          { path: 'prompt', name: 'PromptEditor', component: () => import('@/views/agent/PromptEditor.vue'), meta: { title: 'Prompt' } },
          { path: 'tools', name: 'ToolBinding', component: () => import('@/views/agent/ToolBinding.vue'), meta: { title: '工具绑定' } },
          { path: 'memory', name: 'MemoryConfig', component: () => import('@/views/agent/MemoryConfig.vue'), meta: { title: '记忆策略' } },
        ],
      },

      // ===== 模块三：工具市场 =====
      {
        path: 'tools',
        name: 'ToolMarket',
        component: () => import('@/views/tool/ToolMarket.vue'),
        meta: { title: '工具市场', icon: 'SetUp' },
      },
      {
        path: 'tools/register',
        name: 'ToolRegister',
        component: () => import('@/views/tool/ToolRegister.vue'),
        meta: { title: '注册工具', hidden: true },
      },
      {
        path: 'tools/:id',
        name: 'ToolDetail',
        component: () => import('@/views/tool/ToolDetail.vue'),
        meta: { title: '工具详情', hidden: true },
      },

      // ===== 模块四：Agent 编排 =====
      {
        path: 'orchestration',
        name: 'WorkflowList',
        component: () => import('@/views/orchestration/WorkflowList.vue'),
        meta: { title: 'Agent 编排', icon: 'Share' },
      },
      {
        path: 'orchestration/:id/edit',
        name: 'WorkflowEditor',
        component: () => import('@/views/orchestration/WorkflowEditor.vue'),
        meta: { title: '编排画布', hidden: true },
      },
      {
        path: 'orchestration/:id/run/:runId',
        name: 'WorkflowRun',
        component: () => import('@/views/orchestration/WorkflowRun.vue'),
        meta: { title: '运行记录', hidden: true },
      },

      // ===== 模块五：知识库 =====
      {
        path: 'knowledge',
        name: 'KnowledgeList',
        component: () => import('@/views/knowledge/KnowledgeList.vue'),
        meta: { title: '知识库', icon: 'Collection' },
      },
      {
        path: 'knowledge/:id',
        name: 'KnowledgeDetail',
        component: () => import('@/views/knowledge/KnowledgeDetail.vue'),
        meta: { title: '知识库详情', hidden: true },
      },

      // ===== 模块六：会话控制台 =====
      {
        path: 'agents/:id/chat',
        name: 'SessionConsole',
        component: () => import('@/views/session/SessionConsole.vue'),
        meta: { title: '会话', hidden: true },
      },
      {
        path: 'agents/:id/sessions',
        name: 'SessionHistory',
        component: () => import('@/views/session/SessionHistory.vue'),
        meta: { title: '会话历史', hidden: true },
      },

      // ===== 模块七：监控 =====
      {
        path: 'monitor',
        name: 'MonitorDashboard',
        component: () => import('@/views/monitor/MonitorDashboard.vue'),
        meta: { title: '监控面板', icon: 'Monitor' },
      },
      {
        path: 'monitor/trace/:traceId',
        name: 'TraceDetail',
        component: () => import('@/views/monitor/TraceDetail.vue'),
        meta: { title: '链路追踪', hidden: true },
      },
      {
        path: 'monitor/alerts',
        name: 'AlertConfig',
        component: () => import('@/views/monitor/AlertConfig.vue'),
        meta: { title: '告警配置', hidden: true },
      },

      // ===== 模块八：成本管理 =====
      {
        path: 'cost',
        name: 'CostDashboard',
        component: () => import('@/views/cost/CostDashboard.vue'),
        meta: { title: '成本管理', icon: 'Money' },
      },
      {
        path: 'cost/budget',
        name: 'BudgetConfig',
        component: () => import('@/views/cost/BudgetConfig.vue'),
        meta: { title: '预算配置', hidden: true },
      },

      // ===== 模块九：安全 =====
      {
        path: 'security/audit',
        name: 'AuditLog',
        component: () => import('@/views/security/AuditLog.vue'),
        meta: { title: '审计日志', icon: 'Lock' },
      },
      {
        path: 'security/roles',
        name: 'RoleManage',
        component: () => import('@/views/security/RoleManage.vue'),
        meta: { title: '角色权限', hidden: true },
      },
      {
        path: 'security/approvals',
        name: 'ApprovalList',
        component: () => import('@/views/security/ApprovalList.vue'),
        meta: { title: '审批列表', hidden: true },
      },

      // ===== 模块十：工作空间 =====
      {
        path: 'workspace/settings',
        name: 'WorkspaceSettings',
        component: () => import('@/views/workspace/WorkspaceSettings.vue'),
        meta: { title: '空间设置', icon: 'Setting' },
      },
      {
        path: 'workspace/members',
        name: 'MemberManage',
        component: () => import('@/views/workspace/MemberManage.vue'),
        meta: { title: '成员管理', hidden: true },
      },
    ],
  },

  // ==================== 不套布局的页面 ====================
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFound.vue'),
    meta: { title: '404' },
  },
]
```

### 路由守卫逻辑

```typescript
// src/router/guards.ts

router.beforeEach(async (to, _from, next) => {
  // 1. 登录检查（除 /login 外都需要登录）
  const userStore = useUserStore()
  if (to.path !== '/login' && !userStore.isLoggedIn) {
    return next('/login')
  }

  // 2. 工作空间检查（登录后必须有活跃工作空间）
  const wsStore = useWorkspaceStore()
  if (to.path !== '/login' && to.path !== '/workspace/settings' && !wsStore.currentId) {
    return next('/workspace/settings')
  }

  // 3. 权限检查（按 meta.permission 匹配角色）
  if (to.meta.permission && !userStore.hasPermission(to.meta.permission as string)) {
    return next('/dashboard')
  }

  next()
})
```

---

## 四、Pinia Store 设计

### 4.1 userStore — 用户与权限

```typescript
// src/stores/user.ts
interface UserState {
  user: UserInfo | null
  token: string
  permissions: string[]          // 用户拥有的权限码列表
}

// Actions
login(username: string, password: string): Promise<void>
logout(): void
fetchUserInfo(): Promise<UserInfo>
hasPermission(code: string): boolean
```

### 4.2 workspaceStore — 工作空间

```typescript
// src/stores/workspace.ts
interface WorkspaceState {
  currentId: number | null
  current: Workspace | null
  list: WorkspaceItem[]           // 用户所在的所有空间
  members: Member[]
}

// Actions
fetchMyWorkspaces(): Promise<WorkspaceItem[]>
switchWorkspace(id: number): void
inviteMember(email: string, role: string): Promise<void>
removeMember(userId: number): Promise<void>
```

### 4.3 agentStore — Agent 管理

```typescript
// src/stores/agent.ts
interface AgentState {
  list: AgentSummary[]            // 列表用
  current: AgentDetail | null     // 当前正在查看/编辑的 Agent 详情
  filter: {                      // 列表筛选状态
    keyword: string
    status: AgentStatus | ''
    tag: string
  }
  pagination: { page: number; pageSize: number; total: number }
}

// Actions
fetchAgentList(): Promise<void>                   // 分页列表
fetchAgentDetail(id: number): Promise<void>       // 单个详情
createAgent(data: CreateAgentDTO): Promise<number> // 返回新 ID
updateAgent(id: number, data: UpdateAgentDTO): Promise<void>
deleteAgent(id: number): Promise<void>
updateStatus(id: number, status: AgentStatus): Promise<void>
```

### 4.4 sessionStore — 会话

```typescript
// src/stores/session.ts
interface SessionState {
  currentSessionId: number | null
  messages: Message[]             // 当前会话的消息列表（含步骤信息）
  isStreaming: boolean            // 是否正在接收流式输出
  executionMode: 'auto' | 'step-by-step' | 'plan-only'
}

// Actions
createSession(agentId: number): Promise<number>
sendMessage(agentId: number, content: string): void  // 触发 SSE 流
stopStreaming(): void
fetchHistory(agentId: number): Promise<SessionSummary[]>
fetchSessionDetail(sessionId: number): Promise<Message[]>
```

### 4.5 appStore — 全局 UI

```typescript
// src/stores/app.ts
interface AppState {
  sidebarCollapsed: boolean
  globalLoading: boolean
  breadcrumbs: BreadcrumbItem[]
}

// Actions
toggleSidebar(): void
setLoading(v: boolean): void
```

---

## 五、API 层架构

### 5.1 Axios 实例封装

```typescript
// src/api/index.ts

import axios from 'axios'
import type { ApiResponse } from '@/types/common'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL + '/api/v1',
  timeout: 30_000,
  headers: { 'Content-Type': 'application/json' },
})

// 请求拦截器：注入 Token + WorkspaceId
http.interceptors.request.use((config) => {
  const userStore = useUserStore()
  const wsStore = useWorkspaceStore()

  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  if (wsStore.currentId) {
    config.headers['X-Workspace-Id'] = String(wsStore.currentId)
  }
  return config
})

// 响应拦截器：统一错误处理
http.interceptors.response.use(
  (res) => {
    const data = res.data as ApiResponse
    if (data.code !== 0) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    return res
  },
  (err) => {
    if (err.response?.status === 401) {
      useUserStore().logout()
      window.location.href = '/login'
    }
    ElMessage.error(err.response?.data?.message || '网络错误')
    return Promise.reject(err)
  }
)

export default http
```

### 5.2 请求/响应通用类型

```typescript
// src/types/common.ts

/** 后端统一响应体 */
export interface ApiResponse<T = any> {
  code: number          // 0=成功，非0=业务错误
  message: string
  data: T
}

/** 分页请求 */
export interface PaginationQuery {
  page: number
  pageSize: number
  keyword?: string
  sortBy?: string
  sortDir?: 'asc' | 'desc'
}

/** 分页响应 */
export interface PaginatedData<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}
```

---

## 六、核心页面交互设计

### 6.1 会话控制台（核心页面）

这是整个系统最复杂也最重要的页面，交互流程如下：

```
用户输入消息 → [Enter]
    ↓
前端调用 POST /sessions/:id/messages (创建用户消息)
    ↓
前端建立 SSE 连接 GET /sessions/:id/stream
    ↓
后端依次推送事件：
  event: thinking       → {"content": "我需要先检查..."}         → 展示"思考中"
  event: tool_call      → {"tool": "search_web", "params": {...}} → 展示"调用工具"
  event: tool_result    → {"tool": "search_web", "result": {...}} → 展示工具返回
  event: thinking       → {"content": "根据搜索结果..."}          → 展示推理
  event: message        → {"content": "完整回答..."}             → 追加消息
  event: done           → {}                                    → 关闭连接
  event: error          → {"message": "执行失败"}                → 错误提示
```

```typescript
// src/composables/useSSE.ts
import { fetchEventSource } from '@microsoft/fetch-event-source'

export function useSSE() {
  const abortController = ref<AbortController | null>(null)

  function connect(sessionId: number, callbacks: {
    onThinking: (content: string) => void
    onToolCall: (tool: string, params: any) => void
    onToolResult: (tool: string, result: any) => void
    onMessage: (content: string) => void
    onDone: () => void
    onError: (message: string) => void
  }) {
    abortController.value = new AbortController()

    fetchEventSource(`/api/v1/sessions/${sessionId}/stream`, {
      signal: abortController.value.signal,
      headers: {
        Authorization: `Bearer ${useUserStore().token}`,
        'X-Workspace-Id': String(useWorkspaceStore().currentId),
      },
      onmessage(event) {
        const data = JSON.parse(event.data)
        switch (event.event) {
          case 'thinking':   callbacks.onThinking(data.content); break
          case 'tool_call':  callbacks.onToolCall(data.tool, data.params); break
          case 'tool_result':callbacks.onToolResult(data.tool, data.result); break
          case 'message':    callbacks.onMessage(data.content); break
          case 'done':       callbacks.onDone(); break
          case 'error':      callbacks.onError(data.message); break
        }
      },
      onerror(err) {
        callbacks.onError('连接中断')
        throw err  // 终止重连
      },
    })
  }

  function disconnect() {
    abortController.value?.abort()
  }

  onUnmounted(disconnect)

  return { connect, disconnect }
}
```

### 6.2 编排画布（Vue Flow）

```
画布核心操作：
1. 从左侧 Agent 列表拖拽一个 Agent 节点到画布
2. 连线定义执行顺序
3. 点击节点→右侧属性面板配置节点参数
4. 点击连线→配置数据映射（上游输出 → 下游输入）
5. 工具栏：运行 / 暂停 / 保存 / 导入 / 导出
```

---

## 七、环境变量

```env
# .env.development
VITE_API_BASE_URL=http://localhost:8080
VITE_APP_TITLE=Agent 管理系统
```

---

## 八、package.json 依赖清单

```json
{
  "dependencies": {
    "vue": "^3.5.39",
    "vue-router": "^4.5.1",
    "pinia": "^2.3.1",
    "axios": "^1.7.9",
    "element-plus": "^2.9.8",
    "@element-plus/icons-vue": "^2.3.1",
    "markdown-it": "^14.1.0",
    "highlight.js": "^11.11.1",
    "@microisoft/fetch-event-source": "^2.0.2",
    "@vue-flow/core": "^1.45.2",
    "@vue-flow/background": "^1.3.2",
    "@vue-flow/controls": "^1.1.2",
    "echarts": "^5.6.0",
    "vue-echarts": "^7.0.3",
    "@vueuse/core": "^12.5.0"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^6.0.7",
    "@vue/tsconfig": "^0.9.1",
    "typescript": "~6.0.2",
    "vite": "^8.1.1",
    "vue-tsc": "^3.3.5",
    "@types/node": "^24.13.2",
    "@types/markdown-it": "^14.1.1",
    "unocss": "^66.1.2",
    "@guolao/vue-monaco-editor": "^1.5.4",
    "eslint": "^9.23.0",
    "@vue/eslint-config-typescript": "^14.5.0",
    "prettier": "^3.5.3",
    "vitest": "^3.1.3"
  }
}
```

---

## 九、样式规范约定

```
1. 布局：Element Plus 的 el-container / el-aside / el-main 体系
2. 间距：Tailwind 风格 p-4 / m-2 / gap-4（UnoCSS 编译）
3. 颜色：Element Plus CSS 变量 --el-color-primary 主色
4. 暗色模式：Element Plus 内置暗色主题切换
5. 响应式：侧边栏在小屏自动折叠，表格在窄屏横向滚动
```

---

## 十、前端开发顺序建议

| 阶段 | 内容 | 产出 |
|------|------|------|
| **P0-1 骨架** | AppLayout + 路由 + Pinia + Axios 封装 + Element Plus 集成 | 能跑通的空架子 |
| **P0-2 登录+空间** | 登录页、工作空间切换、用户权限 Store | 登录→进空间闭环 |
| **P0-3 Agent CRUD** | Agent 列表+创建+详情+删除，3 步向导 | 能创建和管理 Agent |
| **P0-4 工具市场** | 工具列表+注册+测试面板 | 能注册和测试工具 |
| **P0-5 会话控制台** | 聊天界面 + SSE 流式 + 步骤时间线 | **核心闭环** |
| **P0-6 监控+成本** | 仪表盘图表 + 成本拆解 | 能看到 Agent 运行情况 |
| **P1-1 配置中心** | Prompt 编辑器(Monaco) + 版本对比 + 变量系统 | 高级配置能力 |
| **P1-2 安全治理** | 角色权限 + 审计日志 + 审批流 | 企业级能力 |
| **P2 编排+知识库** | Vue Flow 画布 + 知识库 RAG | V2 编排放量 |
