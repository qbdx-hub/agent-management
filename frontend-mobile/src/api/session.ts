import http from './http'
import type { ApiResponse, PaginatedData, SessionSummary, SessionDetail, Message } from '@/types'
import { useAuthStore } from '@/stores/auth'

export async function getSessionList(agentId: number, params: { page: number; pageSize: number }): Promise<ApiResponse<PaginatedData<SessionSummary>>> {
  const res = await http.get<ApiResponse<PaginatedData<SessionSummary>>>(`/agents/${agentId}/sessions`, { params })
  return res.data
}

export async function createSession(agentId: number, data: { title?: string; executionMode?: string } = {}): Promise<ApiResponse<{ sessionId: number }>> {
  const res = await http.post<ApiResponse<{ sessionId: number }>>(`/agents/${agentId}/sessions`, data)
  return res.data
}

export async function getSessionDetail(sessionId: number): Promise<ApiResponse<SessionDetail>> {
  const res = await http.get<ApiResponse<SessionDetail>>(`/sessions/${sessionId}`)
  return res.data
}

export async function deleteSession(sessionId: number): Promise<ApiResponse<null>> {
  const res = await http.delete<ApiResponse<null>>(`/sessions/${sessionId}`)
  return res.data
}

export async function stopSession(sessionId: number): Promise<ApiResponse<null>> {
  const res = await http.post<ApiResponse<null>>(`/sessions/${sessionId}/stop`)
  return res.data
}

/**
 * 发送消息（SSE 流式返回 AI 回复）。
 * 用 XHR + onprogress 增量解析（POST + Authorization 头，EventSource 做不到）；
 * 后端事件仅 4 种：thinking / content / done / error。
 */
export function sendMessageSse(
  sessionId: number,
  data: { content: string; mode?: string },
  onEvent: (event: string, data: any) => void,
  signal?: AbortSignal
): Promise<void> {
  return new Promise((resolve, reject) => {
    const auth = useAuthStore()
    const baseUrl = (import.meta.env.VITE_API_BASE_URL || '') + '/api/v1'

    const xhr = new XMLHttpRequest()
    xhr.open('POST', `${baseUrl}/sessions/${sessionId}/messages`, true)
    xhr.setRequestHeader('Content-Type', 'application/json')
    if (auth.token) xhr.setRequestHeader('Authorization', `Bearer ${auth.token}`)
    if (auth.workspaceId) xhr.setRequestHeader('X-Workspace-Id', String(auth.workspaceId))

    let lastProcessedIndex = 0
    let currentEvent = ''
    let settled = false
    let pollTimer: ReturnType<typeof setInterval> | null = null

    function processNewData() {
      const text = xhr.responseText
      if (text.length <= lastProcessedIndex) return
      const newPart = text.substring(lastProcessedIndex)
      lastProcessedIndex = text.length
      for (const line of newPart.split('\n')) {
        const trimmed = line.trim()
        if (trimmed.startsWith('event:')) {
          currentEvent = trimmed.substring(6).trim()
        } else if (trimmed.startsWith('data:')) {
          const jsonStr = trimmed.substring(5).trim()
          if (jsonStr) {
            try {
              onEvent(currentEvent || 'message', JSON.parse(jsonStr))
            } catch {
              // 非 JSON data（如心跳）跳过
            }
          }
        }
      }
    }

    pollTimer = setInterval(processNewData, 100)

    const settle = (fn: () => void) => {
      if (pollTimer) clearInterval(pollTimer)
      if (!settled) { settled = true; fn() }
    }

    xhr.onload = () => {
      processNewData()
      settle(() => {
        if (xhr.status >= 200 && xhr.status < 300) resolve()
        else reject(new Error(`HTTP ${xhr.status}`))
      })
    }
    xhr.onerror = () => settle(() => reject(new Error('网络错误')))

    if (signal) {
      signal.addEventListener('abort', () => {
        xhr.abort()
        settle(() => reject(new Error('AbortError')))
      })
    }

    xhr.send(JSON.stringify(data))
  })
}

export type { Message }
