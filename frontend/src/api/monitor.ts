import http from './index'
import type { ApiResponse, PaginatedData } from '@/types/common'
import type { MonitorOverview, TokenTrendPoint, AgentHealthItem, ErrorLogItem, AlertRule, AlertRecord, TraceDetail } from '@/types/monitor'
import { mockMonitorOverview, mockTokenTrend, mockAgentHealth, mockErrors, mockAlertRules, mockAlertRecords } from '@/mock/monitor'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

export async function getMonitorOverview(period: string = 'today'): Promise<ApiResponse<MonitorOverview>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: mockMonitorOverview }
  const res = await http.get<ApiResponse<MonitorOverview>>('/monitor/overview', { params: { period } })
  return res.data
}

export async function getTokenTrend(period: string = '7d', granularity: string = 'hour'): Promise<ApiResponse<{ series: TokenTrendPoint[]; summary: { totalInput: number; totalOutput: number; totalCost: number } }>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: { series: mockTokenTrend, summary: { totalInput: 520000, totalOutput: 130000, totalCost: 12.50 } } }
  const res = await http.get('/monitor/token-trend', { params: { period, granularity } })
  return res.data
}

export async function getAgentHealth(): Promise<ApiResponse<AgentHealthItem[]>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: mockAgentHealth }
  const res = await http.get<ApiResponse<AgentHealthItem[]>>('/monitor/agent-health')
  return res.data
}

export async function getErrorLogs(params: any): Promise<ApiResponse<PaginatedData<ErrorLogItem>>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: { list: mockErrors, total: mockErrors.length, page: 1, pageSize: 20 } }
  const res = await http.get<ApiResponse<PaginatedData<ErrorLogItem>>>('/monitor/errors', { params })
  return res.data
}

export async function getTraceDetail(traceId: string): Promise<ApiResponse<TraceDetail>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: { traceId, agentId: 1, agentName: '代码审查助手', sessionId: 1001, status: 'completed', steps: [], totalDurationMs: 5000, tokenUsage: { input: 0, output: 0, total: 0 }, createdAt: new Date().toISOString() } }
  const res = await http.get<ApiResponse<TraceDetail>>(`/monitor/trace/${traceId}`)
  return res.data
}

export async function getAlertRules(): Promise<ApiResponse<AlertRule[]>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: mockAlertRules }
  const res = await http.get<ApiResponse<AlertRule[]>>('/monitor/alerts/rules')
  return res.data
}

export async function createAlertRule(data: Omit<AlertRule, 'id'>): Promise<ApiResponse<AlertRule>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: { ...data, id: Date.now() } }
  const res = await http.post<ApiResponse<AlertRule>>('/monitor/alerts/rules', data)
  return res.data
}

export async function deleteAlertRule(ruleId: number): Promise<ApiResponse<null>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: null }
  const res = await http.delete<ApiResponse<null>>(`/monitor/alerts/rules/${ruleId}`)
  return res.data
}

export async function getAlertRecords(params: any): Promise<ApiResponse<PaginatedData<AlertRecord>>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: { list: mockAlertRecords, total: mockAlertRecords.length, page: 1, pageSize: 20 } }
  const res = await http.get<ApiResponse<PaginatedData<AlertRecord>>>('/monitor/alerts/records', { params })
  return res.data
}
