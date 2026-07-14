// ==================== Agent 枚举 ====================

export type AgentStatus = 'draft' | 'testing' | 'published' | 'paused' | 'archived'

export type ExecutionMode = 'auto' | 'step_by_step' | 'plan_only'

export type MemoryStrategy = 'sliding_window' | 'summary' | 'full'

// ==================== Agent 列表 ====================

export interface AgentSummary {
  id: number
  name: string
  description: string
  avatar: string
  status: AgentStatus
  modelName: string
  tags: string[]
  toolCount: number
  totalSessions: number
  successRate: number
  avgLatencyMs: number
  createdBy: number
  creatorName: string
  updatedAt: string
}

// ==================== Agent 详情 ====================

export interface PromptVariable {
  name: string
  label: string
  type: 'string' | 'number' | 'boolean' | 'select'
  options?: string[]
  defaultValue?: any
  required: boolean
}

export interface BoundTool {
  toolId: number
  toolName: string
  toolIcon: string
  enabled: boolean
}

export interface MemoryConfig {
  workingWindow: number
  shortTermStrategy: MemoryStrategy
  longTermEnabled: boolean
  knowledgeBaseIds: number[]
}

export interface ExecutionConfig {
  maxSteps: number
  timeoutSeconds: number
  reflectionEnabled: boolean
  reflectionDepth: number
  outputSchema?: Record<string, any>
}

export interface AgentConfig {
  modelProvider: string
  modelName: string
  temperature: number
  maxTokens: number
  topP: number
  systemPrompt: string
  promptVariables: PromptVariable[]
  boundTools: BoundTool[]
  memory: MemoryConfig
  execution: ExecutionConfig
}

export interface AgentStats {
  totalSessions: number
  totalMessages: number
  totalTokens: number
  totalCost: number
  successRate: number
  avgLatencyMs: number
  avgStepsPerSession: number
}

export interface AgentDetail {
  id: number
  name: string
  description: string
  avatar: string
  status: AgentStatus
  tags: string[]
  createdBy: number
  creatorName: string
  createdAt: string
  updatedAt: string
  config: AgentConfig
  stats: AgentStats
}

// ==================== 创建/更新 ====================

export interface CreateAgentDTO {
  name: string
  description: string
  avatar: string
  tags: string[]
  status: AgentStatus
}

export interface UpdateAgentDTO {
  name?: string
  description?: string
  avatar?: string
  tags?: string[]
}

// ==================== 模型供应商 ====================

export interface ModelPricing {
  inputPer1k: number
  outputPer1k: number
}

export interface ModelInfo {
  name: string
  displayName: string
  maxTokens: number
  pricing: ModelPricing
}

export interface ModelProvider {
  key: string
  name: string
  models: ModelInfo[]
}

// ==================== Prompt 版本 ====================

export interface PromptVersion {
  versionId: number
  versionNumber: string
  systemPrompt: string
  promptVariables: PromptVariable[]
  changeNote: string
  changedBy: number
  changerName: string
  changedAt: string
}
