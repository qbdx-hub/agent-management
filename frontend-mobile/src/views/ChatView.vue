<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createSession, getSessionDetail, getSessionList, sendMessageSse, stopSession } from '@/api/session'
import { getAgentDetail } from '@/api/agent'
import type { AgentDetail, Message } from '@/types'
import { toast } from '@/utils/toast'
import { formatTokens } from '@/utils/format'
import AppIcon from '@/components/AppIcon.vue'
import AgentAvatar from '@/components/AgentAvatar.vue'

const route = useRoute()
const router = useRouter()
const agentId = Number(route.params.agentId)

const agent = ref<AgentDetail | null>(null)
const sessionId = ref<number | null>(null)
const messages = ref<(Message & { streaming?: boolean; thinking?: boolean })[]>([])
const input = ref('')
const busy = ref(false)
const scrollEl = ref<HTMLElement | null>(null)

let abort: AbortController | null = null

function scrollToBottom() {
  nextTick(() => {
    if (scrollEl.value) scrollEl.value.scrollTop = scrollEl.value.scrollHeight
  })
}

onMounted(async () => {
  try {
    const [ad, sl] = await Promise.all([
      getAgentDetail(agentId),
      getSessionList(agentId, { page: 1, pageSize: 1 }),
    ])
    agent.value = ad.data as AgentDetail
    const latest = sl.data.list?.[0]
    if (latest) {
      sessionId.value = latest.sessionId
      const sd = await getSessionDetail(latest.sessionId)
      messages.value = (sd.data.messages || []) as Message[]
      scrollToBottom()
    }
  } catch {
    /* 拦截器已 toast */
  }
})

onBeforeUnmount(() => {
  abort?.abort()
})

async function send() {
  const content = input.value.trim()
  if (!content || busy.value) return
  input.value = ''
  busy.value = true

  try {
    if (!sessionId.value) {
      const res = await createSession(agentId, { title: content.slice(0, 20) })
      sessionId.value = res.data.sessionId
    }
  } catch {
    busy.value = false
    return
  }

  messages.value.push({ messageId: Date.now(), role: 'user', content, createdAt: '' })
  const ai: Message & { streaming?: boolean; thinking?: boolean } = { messageId: Date.now() + 1, role: 'assistant', content: '', createdAt: '', streaming: true, thinking: true }
  messages.value.push(ai)
  scrollToBottom()

  abort = new AbortController()
  try {
    await sendMessageSse(
      sessionId.value,
      { content },
      (event, data) => {
        if (event === 'thinking') {
          ai.thinking = true
        } else if (event === 'content') {
          ai.thinking = false
          ai.content += data?.content ?? ''
          scrollToBottom()
        } else if (event === 'done') {
          ai.thinking = false
          if (data?.tokenUsage) ai.tokenUsage = data.tokenUsage
        } else if (event === 'error') {
          ai.thinking = false
          toast(data?.message || '生成失败', 'error')
        }
      },
      abort.signal
    )
  } catch (e: any) {
    if (e?.message !== 'AbortError') toast('回复中断，请重试', 'error')
  } finally {
    ai.streaming = false
    ai.thinking = false
    busy.value = false
    abort = null
    scrollToBottom()
  }
}

async function stop() {
  abort?.abort()
  if (sessionId.value) {
    try {
      await stopSession(sessionId.value)
    } catch {
      /* 忽略 */
    }
  }
}
</script>

<template>
  <div class="page">
    <div class="navbar">
      <div class="back-btn" @click="router.back()">
        <AppIcon name="back" :size="16" />
      </div>
      <div style="flex: 1">
        <div style="font-size: 17px; font-weight: 600">{{ agent?.name || '对话' }}</div>
        <div style="font-size: 11px; color: var(--success); margin-top: 1px">在线 · {{ agent?.config?.modelName || 'AI' }}</div>
      </div>
      <div class="menu-ico" style="background: var(--card); color: var(--text-2)" @click="toast('会话菜单开发中')">
        <AppIcon name="dots" :size="16" />
      </div>
    </div>

    <div ref="scrollEl" class="scroll" style="padding: 8px 20px">
      <div class="chat-list">
        <div v-if="!messages.length" class="empty" style="margin-top: 30vh">
          <AgentAvatar :name="agent?.name" :avatar="agent?.avatar" :size="52" style="margin: 0 auto 12px" />
          {{ agent?.description || '开始你的第一句提问' }}
        </div>

        <template v-for="m in messages" :key="m.messageId">
          <div class="bubble" :class="m.role === 'user' ? 'user' : 'ai'">
            <template v-if="m.thinking">
              <span style="color: var(--text-2)">思考中<span class="dots-anim">…</span></span>
            </template>
            <template v-else>
              <div style="white-space: pre-wrap; word-break: break-word">{{ m.content }}<span v-if="m.streaming && !m.content" class="cursor">▍</span></div>
              <div v-if="m.tokenUsage" class="meta-row">Token：{{ formatTokens(m.tokenUsage.total) }}</div>
            </template>
          </div>
        </template>
      </div>
    </div>

    <div class="inputbar">
      <textarea v-model="input" rows="1" placeholder="输入消息…" @keydown.enter.exact.prevent="send" />
      <button v-if="!busy" class="send-btn" :disabled="!input.trim()" @click="send">
        <AppIcon name="send" :size="16" />
      </button>
      <button v-else class="send-btn stop" @click="stop">
        <span style="width: 12px; height: 12px; background: #fff; border-radius: 3px; display: block" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.cursor {
  display: inline-block;
  animation: blink 1s step-start infinite;
  color: var(--brand);
}
@keyframes blink {
  50% { opacity: 0; }
}
.dots-anim {
  animation: blink 1s step-start infinite;
}
</style>
