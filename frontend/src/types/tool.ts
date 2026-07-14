// ==================== 工具枚举 ====================

export type ToolCategory = 'search' | 'compute' | 'operate' | 'perceive' | 'notify' | 'custom'

export type ToolType = 'api' | 'mcp' | 'builtin'

// ==================== 工具列表 ====================

export interface ToolSummary {
  id: number
  name: string
  displayName: string
  description: string
  category: ToolCategory
  categoryLabel: string
  icon: string
  type: ToolType
  status: 'active' | 'inactive'
  bindAgentCount: number
  totalCalls: number
  successRate: number
  avgLatencyMs: number
  createdAt: string
}

// ==================== 工具详情 ====================

export interface ToolEndpoint {
  url: string
  method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
  headers: Record<string, string>
  timeoutMs: number
}

export interface ToolParameter {
  name: string
  type: 'string' | 'integer' | 'number' | 'boolean' | 'object' | 'array'
  required: boolean
  description: string
  defaultValue?: any
  enumValues?: any[]
}

export interface ToolDetail {
  id: number
  name: string
  displayName: string
  description: string
  category: ToolCategory
  categoryLabel: string
  icon: string
  type: ToolType
  status: 'active' | 'inactive'
  endpoint?: ToolEndpoint
  parameters: ToolParameter[]
  responseMapping?: string
  credentialRef?: string
  retryOnFail: boolean
  maxRetries: number
  bindAgentCount: number
  totalCalls: number
  successRate: number
  avgLatencyMs: number
  recentCalls: ToolCallRecord[]
  createdAt: string
}

export interface ToolCallRecord {
  time: string
  params: Record<string, any>
  resultSummary: string
  success: boolean
  latencyMs: number
}

// ==================== 创建/更新 ====================

export interface ToolRegisterDTO {
  name: string
  displayName: string
  description: string
  category: ToolCategory
  icon: string
  type: ToolType
  endpoint?: ToolEndpoint
  parameters: ToolParameter[]
  responseMapping?: string
  credentialRef?: string
  retryOnFail: boolean
  maxRetries: number
}

// ==================== 工具测试 ====================

export interface ToolTestResult {
  success: boolean
  latencyMs: number
  requestUrl: string
  requestBody: string
  responseStatus: number
  responseBody: string
  mappedOutput: string
}

// ==================== MCP ====================

export interface MCPConfig {
  serverName: string
  transport: 'stdio' | 'sse'
  command?: string
  args?: string[]
  envVars?: Record<string, string>
}

// ==================== 工具统计 ====================

export interface ToolStats {
  totalCalls: number
  successRate: number
  avgLatencyMs: number
  p99LatencyMs: number
  dailyCallCount: { date: string; count: number; failCount: number }[]
  topAgents: { agentId: number; agentName: string; callCount: number }[]
}
