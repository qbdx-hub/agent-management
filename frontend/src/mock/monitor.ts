import type { MonitorOverview, TokenTrendPoint, AgentHealthItem, ErrorLogItem, AlertRule, AlertRecord } from '@/types/monitor'

export const mockMonitorOverview: MonitorOverview = {
  activeAgentCount: 12, runningTaskCount: 3, todayCallCount: 1247,
  successRate: 0.982, avgLatencyMs: 1200, p99LatencyMs: 4500,
  totalTokensToday: 850000,
  trends: { callCountChange: 0.12, successRateChange: -0.003, latencyChange: 0.05 },
}

export const mockTokenTrend: TokenTrendPoint[] = Array.from({ length: 24 }, (_, i) => ({
  time: `${String(i).padStart(2, '0')}:00`,
  input: Math.floor(15000 + Math.random() * 20000),
  output: Math.floor(3000 + Math.random() * 8000),
}))

export const mockAgentHealth: AgentHealthItem[] = [
  { agentId: 1, agentName: '代码审查助手', status: 'healthy', successRate: 0.991, avgLatencyMs: 800, callCount24h: 120, errorSummary: '无' },
  { agentId: 2, agentName: '文档生成器', status: 'healthy', successRate: 0.985, avgLatencyMs: 650, callCount24h: 85, errorSummary: '无' },
  { agentId: 3, agentName: '数据分析 Agent', status: 'warning', successRate: 0.942, avgLatencyMs: 2100, callCount24h: 45, errorSummary: '最近 3 次调用中 1 次数据库超时' },
  { agentId: 4, agentName: 'PR 审查机器人', status: 'healthy', successRate: 0.905, avgLatencyMs: 1500, callCount24h: 38, errorSummary: '无' },
  { agentId: 5, agentName: '部署检查 Agent', status: 'critical', successRate: 0.821, avgLatencyMs: 3500, callCount24h: 12, errorSummary: 'SSH 连接超时，最近 5 次 3 次失败' },
]

export const mockErrors: ErrorLogItem[] = [
  { errorId: 1, agentId: 5, agentName: '部署检查 Agent', sessionId: 2010, stepSequence: 3, errorType: 'tool_timeout', errorMessage: 'SSH 连接超时: 10.0.1.50:22', occurredAt: '2026-07-14T11:00:00+08:00' },
  { errorId: 2, agentId: 3, agentName: '数据分析 Agent', sessionId: 1892, stepSequence: 2, errorType: 'db_timeout', errorMessage: '数据库查询超时: SELECT * FROM logs WHERE ...', occurredAt: '2026-07-14T10:30:00+08:00' },
  { errorId: 3, agentId: 5, agentName: '部署检查 Agent', sessionId: 2008, stepSequence: 3, errorType: 'tool_timeout', errorMessage: 'SSH 连接超时: 10.0.1.52:22', occurredAt: '2026-07-14T09:45:00+08:00' },
]

export const mockAlertRules: AlertRule[] = [
  { id: 1, name: 'Agent 成功率过低', metric: 'success_rate', targetType: 'agent', targetId: null, condition: 'lt', threshold: 0.85, duration: '5m', severity: 'warning', enabled: true, notifyChannels: ['feishu', 'email'] },
  { id: 2, name: 'P99 延迟过高', metric: 'p99_latency', targetType: 'agent', targetId: null, condition: 'gt', threshold: 5000, duration: '3m', severity: 'warning', enabled: true, notifyChannels: ['feishu'] },
  { id: 3, name: '日 Token 超限', metric: 'daily_tokens', targetType: 'workspace', targetId: null, condition: 'gt', threshold: 500000, duration: '0m', severity: 'critical', enabled: true, notifyChannels: ['feishu', 'email', 'webhook'] },
]

export const mockAlertRecords: AlertRecord[] = [
  { recordId: 1, ruleName: 'Agent 成功率过低', severity: 'warning', message: '部署检查Agent 成功率降至 82.1%，低于阈值 85%', triggeredAt: '2026-07-14T14:32:00+08:00', resolvedAt: null, status: 'triggered' },
  { recordId: 2, ruleName: '日 Token 超限', severity: 'critical', message: '工作空间日 Token 消耗 480K，已达阈值 96%', triggeredAt: '2026-07-14T13:15:00+08:00', resolvedAt: null, status: 'triggered' },
  { recordId: 3, ruleName: 'Agent 成功率过低', severity: 'warning', message: '数据分析 Agent 成功率降至 88%，低于阈值 90%', triggeredAt: '2026-07-13T16:00:00+08:00', resolvedAt: '2026-07-13T17:30:00+08:00', status: 'resolved' },
]
