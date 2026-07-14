import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as sessionApi from '@/api/session'
import type { SessionSummary, Message, ExecutionMode } from '@/types/session'

export const useSessionStore = defineStore('session', () => {
  const currentSessionId = ref<number | null>(null)
  const messages = ref<Message[]>([])
  const isStreaming = ref(false)
  const executionMode = ref<ExecutionMode>('auto')

  async function createSession(agentId: number): Promise<number> {
    const res = await sessionApi.createSession(agentId, { title: '新会话' })
    if (res.code === 0) {
      currentSessionId.value = res.data.sessionId
      messages.value = []
      return res.data.sessionId
    }
    throw new Error('创建会话失败')
  }

  async function fetchSessionDetail(sessionId: number) {
    const res = await sessionApi.getSessionMessages(sessionId)
    if (res.code === 0) {
      currentSessionId.value = sessionId
      messages.value = res.data.messages
    }
  }

  async function sendMessage(agentId: number, content: string) {
    if (!currentSessionId.value) {
      await createSession(agentId)
    }
    // Add user message to list
    const userMsg: Message = {
      messageId: Date.now(),
      role: 'user',
      content,
      createdAt: new Date().toISOString(),
    }
    messages.value.push(userMsg)

    // Send to API, SSE flow handled by component using useSSE
    try {
      await sessionApi.sendMessage(currentSessionId.value!, { content, mode: executionMode.value })
    } catch {
      // Will be handled by SSE error event
    }
  }

  function addAssistantMessage(msg: Message) {
    messages.value.push(msg)
  }

  function updateLastAssistantContent(content: string) {
    const last = messages.value[messages.value.length - 1]
    if (last && last.role === 'assistant') {
      last.content = content
    }
  }

  function stopStreaming() {
    isStreaming.value = false
  }

  function setExecutionMode(mode: ExecutionMode) {
    executionMode.value = mode
  }

  return { currentSessionId, messages, isStreaming, executionMode, createSession, fetchSessionDetail, sendMessage, addAssistantMessage, updateLastAssistantContent, stopStreaming, setExecutionMode }
})
