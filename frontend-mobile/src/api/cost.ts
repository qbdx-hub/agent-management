import http from './http'
import type { ApiResponse, CostOverview } from '@/types'

export async function getCostOverview(period = 'this_month'): Promise<ApiResponse<CostOverview>> {
  const res = await http.get<ApiResponse<CostOverview>>('/cost/overview', { params: { period } })
  return res.data
}
