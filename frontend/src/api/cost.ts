import http from './index'
import type { ApiResponse, PaginatedData } from '@/types/common'
import type { CostOverview, CostBreakdownItem, CostTrendPoint, BudgetConfig, CostRecord } from '@/types/cost'
import { mockCostOverview, mockCostByModel, mockCostByAgent, mockCostTrend, mockBudgets, mockCostRecords } from '@/mock/cost'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

export async function getCostOverview(period: string = 'this_month'): Promise<ApiResponse<CostOverview>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: mockCostOverview }
  const res = await http.get<ApiResponse<CostOverview>>('/cost/overview', { params: { period } })
  return res.data
}

export async function getCostBreakdown(dimension: string, period: string = 'this_month'): Promise<ApiResponse<CostBreakdownItem[]>> {
  if (USE_MOCK) {
    const data = dimension === 'agent' ? mockCostByAgent : mockCostByModel
    return { code: 0, message: 'ok', data }
  }
  const res = await http.get<ApiResponse<CostBreakdownItem[]>>('/cost/breakdown', { params: { dimension, period } })
  return res.data
}

export async function getCostTrend(period: string = '30d', granularity: string = 'day'): Promise<ApiResponse<{ series: CostTrendPoint[] }>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: { series: mockCostTrend } }
  const res = await http.get('/cost/trend', { params: { period, granularity } })
  return res.data
}

export async function getBudgets(): Promise<ApiResponse<BudgetConfig[]>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: mockBudgets }
  const res = await http.get<ApiResponse<BudgetConfig[]>>('/cost/budgets')
  return res.data
}

export async function createBudget(data: Omit<BudgetConfig, 'id'>): Promise<ApiResponse<BudgetConfig>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: { ...data, id: Date.now() } }
  const res = await http.post<ApiResponse<BudgetConfig>>('/cost/budgets', data)
  return res.data
}

export async function deleteBudget(id: number): Promise<ApiResponse<null>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: null }
  const res = await http.delete<ApiResponse<null>>(`/cost/budgets/${id}`)
  return res.data
}

export async function getCostRecords(params: any): Promise<ApiResponse<PaginatedData<CostRecord>>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: { list: mockCostRecords, total: mockCostRecords.length, page: 1, pageSize: 20 } }
  const res = await http.get<ApiResponse<PaginatedData<CostRecord>>>('/cost/records', { params })
  return res.data
}
