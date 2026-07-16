import http from './index'
import type { ApiResponse, PaginatedData } from '@/types/common'
import type { MonitorOverview, TokenTrendPoint, AgentHealthItem, ErrorLogItem, AlertRule, AlertRecord, TraceDetail } from '@/types/monitor'

export async function getMonitorOverview(period: string = 'today'): Promise<ApiResponse<MonitorOverview>> {
  const res = await http.get<ApiResponse<MonitorOverview>>('/monitor/overview', { params: { period } })
  return res.data
}

export async function getTokenTrend(period: string = '7d', granularity: string = 'hour'): Promise<ApiResponse<{ series: TokenTrendPoint[]; summary: { totalInput: number; totalOutput: number; totalCost: number } }>> {
  const res = await http.get('/monitor/token-trend', { params: { period, granularity } })
  return res.data
}

export async function getAgentHealth(): Promise<ApiResponse<AgentHealthItem[]>> {
  const res = await http.get<ApiResponse<AgentHealthItem[]>>('/monitor/agent-health')
  return res.data
}

export async function getErrorLogs(params: any): Promise<ApiResponse<PaginatedData<ErrorLogItem>>> {
  const res = await http.get<ApiResponse<PaginatedData<ErrorLogItem>>>('/monitor/errors', { params })
  return res.data
}

export async function getTraceDetail(traceId: string): Promise<ApiResponse<TraceDetail>> {
  const res = await http.get<ApiResponse<TraceDetail>>(`/monitor/trace/${traceId}`)
  return res.data
}

export async function getAlertRules(): Promise<ApiResponse<AlertRule[]>> {
  const res = await http.get<ApiResponse<AlertRule[]>>('/monitor/alerts/rules')
  return res.data
}

export async function createAlertRule(data: Omit<AlertRule, 'id'>): Promise<ApiResponse<AlertRule>> {
  const res = await http.post<ApiResponse<AlertRule>>('/monitor/alerts/rules', data)
  return res.data
}

export async function toggleAlertRule(ruleId: number, enabled: boolean): Promise<ApiResponse<null>> {
  const res = await http.put<ApiResponse<null>>(`/monitor/alerts/rules/${ruleId}/status`, null, { params: { enabled } })
  return res.data
}

export async function deleteAlertRule(ruleId: number): Promise<ApiResponse<null>> {
  const res = await http.delete<ApiResponse<null>>(`/monitor/alerts/rules/${ruleId}`)
  return res.data
}

export async function getAlertRecords(params: any): Promise<ApiResponse<PaginatedData<AlertRecord>>> {
  const res = await http.get<ApiResponse<PaginatedData<AlertRecord>>>('/monitor/alerts/records', { params })
  return res.data
}
