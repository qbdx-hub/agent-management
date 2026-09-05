/** 后端统一响应包装 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

/** 分页响应 data */
export interface PaginatedData<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

export interface UserVO {
  id: number
  username: string
  nickname: string
  avatar: string
  email: string
  role: string
  permissions: string[]
  workspaces: { id: number; name: string }[]
}

export interface LoginVO {
  token: string
  expiresAt: string
  user: UserVO
}

/** 与后端 AgentVO 一一对应（config/stats 为嵌套结构） */
export interface AgentSummary {
  id: number
  name: string
  description: string
  avatar: string
  status: string
  tags: string[]
  createdBy: number
  creatorName: string
  createdAt: string
  updatedAt: string
  config: {
    modelProvider: string
    modelName: string
    temperature: number
    maxTokens: number
    topP: number
    systemPrompt: string
    memory: { knowledgeBaseIds: number[] } | null
    execution: { maxSteps: number; timeoutSeconds: number } | null
  } | null
  stats: {
    totalSessions: number
    totalMessages: number
    totalTokens: number
    totalCost: number
    successRate: number
    avgLatencyMs: number
    avgStepsPerSession: number
  } | null
}

/** 详情与列表同构（后端 /agents/{id} 返回完整 AgentVO） */
export type AgentDetail = AgentSummary

export interface TokenUsage {
  input: number
  output: number
  total: number
  cost: number
}

export interface ExecutionStep {
  stepId: number
  sequence: number
  type: string
  status: string
  content: string
  toolName: string
  errorMessage: string
  durationMs: number
}

export interface Message {
  messageId: number
  role: 'user' | 'assistant'
  content: string
  steps?: ExecutionStep[]
  tokenUsage?: TokenUsage
  createdAt: string
}

export interface SessionSummary {
  sessionId: number
  title: string
  status: string
  messageCount: number
  totalTokens: number
  totalCost: number
  lastMessageAt: string
  createdAt: string
}

export interface SessionDetail {
  sessionId: number
  title: string
  status: string
  messages: Message[]
}

export interface MonitorOverview {
  activeAgentCount: number
  runningTaskCount: number
  todayCallCount: number
  successRate: number
  avgLatencyMs: number
  p99LatencyMs: number
  totalTokensToday: number
  trends?: {
    callCountChange: number
    successRateChange: number
    latencyChange: number
  }
}

export interface TokenTrendPoint {
  time: string
  input: number
  output: number
}

export interface TokenTrendResp {
  series: TokenTrendPoint[]
  summary: { totalInput: number; totalOutput: number; totalCost: number }
}

export interface CostOverview {
  totalCost: number
  budgetLimit: number
  budgetRemaining: number
  budgetPercent: number
  todayCost: number
  yesterdayCost: number
  projectedMonthCost: number
  meltdownStatus: string
}

/** 与后端 KnowledgeBase 实体一一对应 */
export interface KnowledgeBase {
  id: number
  workspaceId: number
  name: string
  description: string
  type: 'vector' | 'keyword' | 'hybrid'
  embeddingModel: string
  documentCount: number
  totalTokens: number
  status: 'active' | 'building' | 'error'
  createdBy: number
  createdAt: string
  updatedAt: string
}

/** 与后端 Document 实体一一对应 */
export interface KbDocument {
  id: number
  knowledgeBaseId: number
  name: string
  fileType: string
  fileSize: number
  chunkCount: number
  totalTokens: number
  status: 'pending' | 'processing' | 'completed' | 'failed'
  error: string | null
  createdAt: string
  updatedAt: string
}

/** 与后端 RetrievalService.SearchResult 一一对应（文档名由前端按 documentId 映射） */
export interface SearchChunk {
  chunkId: number
  documentId: number
  content: string
  score: number
}

export interface ApiKeyItem {
  id: number
  name: string
  mask: string
  status: 'active' | 'disabled'
  createdAt: string
  lastUsedAt: string | null
  key?: string // 明文，仅创建响应返回一次
}

export interface Preferences {
  defaultModel: string
  temperature: number
  maxTokens: number
  replyStyle: string
  notifications: {
    agentFinished: boolean
    taskFailed: boolean
    instanceAlert: boolean
    tokenUsage80: boolean
    quietHours: { enabled: boolean; from: string; to: string }
    channels: string[]
  }
}

/** 系统真实接入的模型（backend model_pricing 表） */
export interface ModelCatalogItem {
  id: number
  provider: string
  modelName: string
  displayName: string
  maxTokens: number
  inputPricePer1k: number
  outputPricePer1k: number
  enabled: number
}

/** 修改个人信息表单（对齐后端 UserProfileForm，PUT /auth/profile；newPassword 为空表示不改密码） */
export interface UserProfileForm {
  username: string
  nickname: string
  email?: string
  oldPassword?: string
  newPassword?: string
}

/** 新建 Agent 表单（对齐后端 AgentCreateForm） */
export interface AgentCreateForm {
  name: string
  description?: string
  avatar?: string
  modelProvider?: string
  modelName?: string
  temperature?: number
  maxTokens?: number
}

/** GET /terminal/info —— 终端环境信息 */
export interface TerminalInfo {
  os: string
  role: string
  sandboxPath: string
  /** 空间是否对成员开放终端（owner/admin 不受限） */
  memberTerminalEnabled: boolean
}

/** POST /terminal/exec —— 命令执行结果（timedOut 仍为 code=0，前端按系统行渲染） */
export interface TerminalExecResult {
  output: string
  exitCode: number
  durationMs: number
  cwd: string
  truncated: boolean
  timedOut: boolean
}
