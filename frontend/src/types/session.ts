// ==================== 枚举 ====================

export type SessionStatus = 'active' | 'completed' | 'stopped' | 'error'

export type StepType = 'thinking' | 'tool_call' | 'tool_result'

export type StepStatus = 'pending' | 'running' | 'success' | 'error' | 'skipped'

export type ExecutionMode = 'auto' | 'step_by_step' | 'plan_only'

// ==================== 会话 ====================

export interface SessionSummary {
  sessionId: number
  title: string
  status: SessionStatus
  messageCount: number
  totalTokens: number
  totalCost: number
  lastMessageAt: string
  createdAt: string
}

export interface SessionCreateDTO {
  title?: string
  variables?: Record<string, string>
}

// ==================== 消息 ====================

export interface ExecutionStep {
  stepId: number
  sequence: number
  type: StepType
  status: StepStatus
  content?: string
  toolName?: string
  toolIcon?: string
  request?: Record<string, any>
  response?: Record<string, any>
  errorMessage?: string
  retryCount?: number
  startedAt: string
  completedAt: string
  durationMs: number
}

export interface TokenUsage {
  input: number
  output: number
  total: number
  cost?: number
}

export interface Message {
  messageId: number
  role: 'user' | 'assistant' | 'system'
  content: string
  steps?: ExecutionStep[]
  tokenUsage?: TokenUsage
  createdAt: string
}

export interface SendMessageDTO {
  content: string
  mode: ExecutionMode
  attachments?: string[]
}

// ==================== SSE 事件 ====================

export interface SSEThinkingEvent {
  stepId: number
  content: string
  timestamp: string
}

export interface SSEToolCallEvent {
  stepId: number
  toolName: string
  toolIcon: string
  params: Record<string, any>
  timestamp: string
}

export interface SSEToolResultEvent {
  stepId: number
  toolName: string
  success: boolean
  result: Record<string, any>
  durationMs: number
  timestamp: string
}

export interface SSEToolErrorEvent {
  stepId: number
  toolName: string
  error: string
  retrying: boolean
  timestamp: string
}

export interface SSEMessageEvent {
  messageId: number
  content: string
  timestamp: string
}

export interface SSEDoneEvent {
  messageId: number
  sessionStatus: SessionStatus
  totalSteps: number
  totalDurationMs: number
}
