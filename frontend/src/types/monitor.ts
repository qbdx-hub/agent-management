// ==================== 监控 ====================

export interface MonitorOverview {
  activeAgentCount: number
  runningTaskCount: number
  todayCallCount: number
  successRate: number
  avgLatencyMs: number
  p99LatencyMs: number
  totalTokensToday: number
  trends: {
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

export interface CallTrendPoint {
  time: string
  count: number
  failCount: number
}

export interface AgentHealthItem {
  agentId: number
  agentName: string
  status: 'healthy' | 'warning' | 'critical'
  successRate: number
  avgLatencyMs: number
  callCount24h: number
  errorSummary: string
}

export interface ErrorLogItem {
  errorId: number
  agentId: number
  agentName: string
  sessionId: number
  stepSequence: number
  errorType: string
  errorMessage: string
  occurredAt: string
}

export type AlertSeverity = 'info' | 'warning' | 'critical'

export interface AlertRule {
  id: number
  name: string
  metric: string
  targetType: 'agent' | 'workspace'
  targetId: number | null
  condition: 'lt' | 'gt' | 'lte' | 'gte'
  threshold: number
  duration: string
  severity: AlertSeverity
  enabled: boolean
  notifyChannels: string[]
}

export interface AlertRecord {
  recordId: number
  ruleName: string
  severity: AlertSeverity
  message: string
  triggeredAt: string
  resolvedAt: string | null
  status: 'triggered' | 'resolved'
}

export interface TraceDetail {
  traceId: string
  agentId: number
  agentName: string
  sessionId: number
  status: string
  steps: import('./session').ExecutionStep[]
  totalDurationMs: number
  tokenUsage: import('./session').TokenUsage
  createdAt: string
}
