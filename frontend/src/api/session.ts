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

/**
 * 发送消息（SSE 流式返回 AI 回复）
 * 使用 XMLHttpRequest + setInterval 轮询读取 SSE，确保不丢失事件。
 */
export function sendMessageSse(
  sessionId: number,
  data: SendMessageDTO,
  onEvent: (event: string, data: any) => void,
  signal?: AbortSignal
): Promise<void> {
  return new Promise((resolve, reject) => {
    const baseUrl = import.meta.env.VITE_API_BASE_URL + '/api/v1'
    const token = localStorage.getItem('token')
    const workspaceId = localStorage.getItem('workspaceId')

    const xhr = new XMLHttpRequest()
    xhr.open('POST', `${baseUrl}/sessions/${sessionId}/messages`, true)
    xhr.setRequestHeader('Content-Type', 'application/json')
    if (token) xhr.setRequestHeader('Authorization', `Bearer ${token}`)
    if (workspaceId) xhr.setRequestHeader('X-Workspace-Id', workspaceId)

    let lastProcessedIndex = 0
    let currentEvent = ''
    let settled = false
    let pollTimer: ReturnType<typeof setInterval> | null = null

    function processNewData() {
      const text = xhr.responseText
      if (text.length <= lastProcessedIndex) return

      const newPart = text.substring(lastProcessedIndex)
      lastProcessedIndex = text.length

      const lines = newPart.split('\n')
      for (const line of lines) {
        const trimmed = line.trim()
        if (trimmed.startsWith('event:')) {
          currentEvent = trimmed.substring(6).trim()
        } else if (trimmed.startsWith('data:')) {
          const jsonStr = trimmed.substring(5).trim()
          if (jsonStr) {
            try {
              const parsed = JSON.parse(jsonStr)
              console.log('[SSE]', currentEvent || 'message', parsed)
              onEvent(currentEvent || 'message', parsed)
            } catch {
              // 解析失败跳过
            }
          }
        }
      }
    }

    // 轮询读取（每 100ms 检查一次新数据）
    pollTimer = setInterval(processNewData, 100)

    xhr.onload = () => {
      if (pollTimer) clearInterval(pollTimer)
      processNewData() // 处理剩余数据
      if (!settled) {
        settled = true
        if (xhr.status >= 200 && xhr.status < 300) {
          resolve()
        } else {
          reject(new Error(`HTTP ${xhr.status}: ${xhr.statusText}`))
        }
      }
    }

    xhr.onerror = () => {
      if (pollTimer) clearInterval(pollTimer)
      if (!settled) { settled = true; reject(new Error('网络错误')) }
    }

    if (signal) {
      signal.addEventListener('abort', () => {
        if (pollTimer) clearInterval(pollTimer)
        xhr.abort()
        if (!settled) { settled = true; reject(new Error('AbortError')) }
      })
    }

    xhr.send(JSON.stringify(data))
  })
}
