import http from './http'
import type { ApiResponse, PaginatedData, AgentSummary, AgentDetail, AgentCreateForm } from '@/types'

export async function getAgentList(params: { page?: number; pageSize?: number; keyword?: string; status?: string }): Promise<ApiResponse<PaginatedData<AgentSummary>>> {
  const res = await http.get<ApiResponse<PaginatedData<AgentSummary>>>('/agents', { params })
  return res.data
}

export async function getAgentDetail(id: number): Promise<ApiResponse<AgentDetail>> {
  const res = await http.get<ApiResponse<AgentDetail>>(`/agents/${id}`)
  return res.data
}

export async function createAgent(form: AgentCreateForm): Promise<ApiResponse<AgentDetail>> {
  const res = await http.post<ApiResponse<AgentDetail>>('/agents', form)
  return res.data
}
