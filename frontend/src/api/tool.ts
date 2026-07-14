import http from './index'
import type { ApiResponse, PaginatedData, PaginationQuery } from '@/types/common'
import type { ToolSummary, ToolDetail, ToolRegisterDTO, ToolTestResult, ToolStats, MCPConfig } from '@/types/tool'
import { mockTools, mockToolDetail, mockToolTestSuccess, mockToolStats } from '@/mock/tools'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

export async function getToolList(params: PaginationQuery): Promise<ApiResponse<PaginatedData<ToolSummary>>> {
  if (USE_MOCK) {
    let list = [...mockTools]
    if (params.keyword) list = list.filter(t => t.name.includes(params.keyword!) || t.displayName.includes(params.keyword!))
    if ((params as any).category) list = list.filter(t => t.category === (params as any).category)
    return { code: 0, message: 'ok', data: { list, total: list.length, page: params.page, pageSize: params.pageSize } }
  }
  const res = await http.get<ApiResponse<PaginatedData<ToolSummary>>>('/tools', { params })
  return res.data
}

export async function getToolDetail(id: number): Promise<ApiResponse<ToolDetail>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: { ...mockToolDetail, id } }
  const res = await http.get<ApiResponse<ToolDetail>>(`/tools/${id}`)
  return res.data
}

export async function registerTool(data: ToolRegisterDTO): Promise<ApiResponse<ToolDetail>> {
  if (USE_MOCK) {
    const tool: ToolDetail = { ...mockToolDetail, ...data, id: Date.now(), status: 'active', totalCalls: 0, successRate: 0, avgLatencyMs: 0, recentCalls: [], bindAgentCount: 0, createdAt: new Date().toISOString() }
    return { code: 0, message: 'ok', data: tool }
  }
  const res = await http.post<ApiResponse<ToolDetail>>('/tools', data)
  return res.data
}

export async function updateTool(id: number, data: Partial<ToolRegisterDTO>): Promise<ApiResponse<null>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: null }
  const res = await http.put<ApiResponse<null>>(`/tools/${id}`, data)
  return res.data
}

export async function deleteTool(id: number): Promise<ApiResponse<null>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: null }
  const res = await http.delete<ApiResponse<null>>(`/tools/${id}`)
  return res.data
}

export async function testTool(id: number, parameters: Record<string, any>): Promise<ApiResponse<ToolTestResult>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: mockToolTestSuccess }
  const res = await http.post<ApiResponse<ToolTestResult>>(`/tools/${id}/test`, { parameters })
  return res.data
}

export async function registerMCP(data: MCPConfig): Promise<ApiResponse<any>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: { id: Date.now(), ...data } }
  const res = await http.post<ApiResponse<any>>('/tools/mcp', data)
  return res.data
}

export async function getToolStats(id: number, startDate?: string, endDate?: string): Promise<ApiResponse<ToolStats>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: mockToolStats }
  const res = await http.get<ApiResponse<ToolStats>>(`/tools/${id}/stats`, { params: { startDate, endDate } })
  return res.data
}
