import http from './index'
import type { ApiResponse, PaginatedData, PaginationQuery } from '@/types/common'
import type { AgentSummary, AgentDetail, CreateAgentDTO, UpdateAgentDTO, AgentStatus } from '@/types/agent'
import { mockAgents, mockAgentDetail } from '@/mock/agents'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

export async function getAgentList(params: PaginationQuery): Promise<ApiResponse<PaginatedData<AgentSummary>>> {
  if (USE_MOCK) {
    let list = [...mockAgents]
    if (params.keyword) list = list.filter(a => a.name.includes(params.keyword!) || a.description.includes(params.keyword!))
    if ((params as any).status) list = list.filter(a => a.status === (params as any).status)
    return { code: 0, message: 'ok', data: { list, total: list.length, page: params.page, pageSize: params.pageSize } }
  }
  const res = await http.get<ApiResponse<PaginatedData<AgentSummary>>>('/agents', { params })
  return res.data
}

export async function getAgentDetail(id: number): Promise<ApiResponse<AgentDetail>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: { ...mockAgentDetail, id } }
  const res = await http.get<ApiResponse<AgentDetail>>(`/agents/${id}`)
  return res.data
}

export async function createAgent(data: CreateAgentDTO): Promise<ApiResponse<AgentDetail>> {
  if (USE_MOCK) {
    const agent: AgentDetail = { ...mockAgentDetail, ...data, id: Date.now(), createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() }
    return { code: 0, message: 'ok', data: agent }
  }
  const res = await http.post<ApiResponse<AgentDetail>>('/agents', data)
  return res.data
}

export async function updateAgent(id: number, data: UpdateAgentDTO): Promise<ApiResponse<null>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: null }
  const res = await http.put<ApiResponse<null>>(`/agents/${id}`, data)
  return res.data
}

export async function deleteAgent(id: number): Promise<ApiResponse<null>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: null }
  const res = await http.delete<ApiResponse<null>>(`/agents/${id}`)
  return res.data
}

export async function updateAgentStatus(id: number, status: AgentStatus): Promise<ApiResponse<null>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: null }
  const res = await http.patch<ApiResponse<null>>(`/agents/${id}/status`, { status })
  return res.data
}

// ==================== Agent Config ====================
// 说明：模型/Prompt/记忆/执行等配置没有独立路由，统一走 updateAgent（PUT /agents/{id}）全量更新。

export async function updatePromptConfig(agentId: number, data: { systemPrompt: string; promptVariables: any[] }): Promise<ApiResponse<null>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: null }
  return updateAgent(agentId, data as any)
}

export async function updateToolBindings(agentId: number, tools: { toolId: number; enabled: boolean }[]): Promise<ApiResponse<null>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: null }
  // 工具绑定暂存为 agent 配置的一部分
  return updateAgent(agentId, { toolBindings: tools } as any)
}

export async function updateMemoryConfig(agentId: number, data: any): Promise<ApiResponse<null>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: null }
  return updateAgent(agentId, data)
}
