import http from './index'
import type { ApiResponse, PaginatedData } from '@/types/common'
import type { SessionSummary, Message, SessionCreateDTO, SendMessageDTO } from '@/types/session'
import { mockSessions, mockMessages } from '@/mock/sessions'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

export async function createSession(agentId: number, data: SessionCreateDTO): Promise<ApiResponse<{ sessionId: number }>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: { sessionId: 1001 } }
  const res = await http.post<ApiResponse<{ sessionId: number }>>(`/agents/${agentId}/sessions`, data)
  return res.data
}

export async function getSessionList(agentId: number, params: { page: number; pageSize: number }): Promise<ApiResponse<PaginatedData<SessionSummary>>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: { list: mockSessions, total: mockSessions.length, page: params.page, pageSize: params.pageSize } }
  const res = await http.get<ApiResponse<PaginatedData<SessionSummary>>>(`/agents/${agentId}/sessions`, { params })
  return res.data
}

export async function getSessionMessages(sessionId: number): Promise<ApiResponse<{ sessionId: number; title: string; status: string; messages: Message[] }>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: { sessionId, title: '审查 PR #42', status: 'completed', messages: mockMessages } }
  const res = await http.get<ApiResponse<any>>(`/sessions/${sessionId}/messages`)
  return res.data
}

export async function sendMessage(sessionId: number, data: SendMessageDTO): Promise<ApiResponse<{ messageId: number }>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: { messageId: Date.now() } }
  const res = await http.post<ApiResponse<{ messageId: number }>>(`/sessions/${sessionId}/messages`, data)
  return res.data
}

export async function stopSession(sessionId: number): Promise<ApiResponse<null>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: null }
  const res = await http.post<ApiResponse<null>>(`/sessions/${sessionId}/stop`)
  return res.data
}

export async function continueSession(sessionId: number, data: { additionalInput?: string; mode: string }): Promise<ApiResponse<null>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: null }
  const res = await http.post<ApiResponse<null>>(`/sessions/${sessionId}/continue`, data)
  return res.data
}

export async function deleteSession(sessionId: number): Promise<ApiResponse<null>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: null }
  const res = await http.delete<ApiResponse<null>>(`/sessions/${sessionId}`)
  return res.data
}
