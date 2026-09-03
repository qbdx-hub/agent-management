import http from './http'
import type { ApiResponse, MonitorOverview, TokenTrendResp } from '@/types'

export async function getMonitorOverview(period = 'today'): Promise<ApiResponse<MonitorOverview>> {
  const res = await http.get<ApiResponse<MonitorOverview>>('/monitor/overview', { params: { period } })
  return res.data
}

export async function getTokenTrend(period = '7d', granularity = 'day'): Promise<ApiResponse<TokenTrendResp>> {
  const res = await http.get<ApiResponse<TokenTrendResp>>('/monitor/token-trend', { params: { period, granularity } })
  return res.data
}
