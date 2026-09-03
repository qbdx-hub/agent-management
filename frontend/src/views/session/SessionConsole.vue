<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSessionStore } from '@/stores/session'
import { useAgentStore } from '@/stores/agent'
import { sendMessageSse, getSessionList, stopSession } from '@/api/session'
import { EXECUTION_MODE_MAP } from '@/utils/constants'
import { formatTokens, formatCost } from '@/utils/format'
import type { Message, ExecutionStep, ExecutionMode } from '@/types/session'

const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()
const agentStore = useAgentStore()

const agentId = computed(() => Number(route.params.id))
const inputText = ref('')
const messagesRef = ref<HTMLElement | null>(null)
const showSteps = ref(true)
// 沙箱外运行授权：开启后本次页面内发送的消息，其文件/命令工具作用于服务器真实文件系统
const outsideSandbox = ref(false)
let abortController: AbortController | null = null

// 组件卸载时中止进行中的 SSE 请求，避免切页后轮询定时器与 XHR 继续运行（资源泄漏）
onUnmounted(() => {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
  sessionStore.isStreaming = false
})

onMounted(async () => {
  // 切换 Agent 时清空旧会话
  sessionStore.clearSession()
  await agentStore.fetchAgentDetail(agentId.value)
  const sid = route.query.sessionId
  if (sid) {
    // 加载指定会话
    await sessionStore.fetchSessionDetail(Number(sid))
  } else {
    // 自动加载该 Agent 最近一次会话
    try {
      const res = await getSessionList(agentId.value, { page: 1, pageSize: 1 })
      if (res.code === 0 && res.data?.list?.length) {
        const latestSession = res.data.list[0]
        await sessionStore.fetchSessionDetail(latestSession.sessionId)
      }
    } catch { /* 新 Agent 还没有会话，正常 */ }
  }
  await nextTick()
  scrollToBottom()
})

function scrollToBottom() {
  if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight
}

watch(() => sessionStore.messages.length, () => {
  nextTick(scrollToBottom)
})

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || sessionStore.isStreaming) return
  inputText.value = ''

  // 确保有 session
  if (!sessionStore.currentSessionId) {
    await sessionStore.createSession(agentId.value)
  }

  // 添加用户消息到本地
  const userMsg: Message = {
    messageId: Date.now(), role: 'user', content: text,
    createdAt: new Date().toISOString(),
  }
  sessionStore.messages.push(userMsg)
  nextTick(scrollToBottom)

  // 添加空的 assistant 消息（逐步填充）
  const assistantMsg: Message = {
    messageId: Date.now() + 1, role: 'assistant', content: '',
    steps: [{ stepId: 1, sequence: 1, type: 'thinking', status: 'running', content: '正在思考...', startedAt: new Date().toISOString(), completedAt: '', durationMs: 0 }],
    createdAt: new Date().toISOString(),
  }
  sessionStore.messages.push(assistantMsg)
  sessionStore.isStreaming = true
  nextTick(scrollToBottom)

  // 调用 SSE 接口
  abortController = new AbortController()
  try {
    await sendMessageSse(
      sessionStore.currentSessionId!,
      { content: text, mode: sessionStore.executionMode, outsideSandbox: outsideSandbox.value || undefined },
      (event: string, data: any) => {
        if (event === 'thinking') {
          assistantMsg.steps![0].content = data.content || '正在思考...'
          assistantMsg.steps![0].status = 'running'
        } else if (event === 'content') {
          // 清除 thinking 步骤
          if (assistantMsg.steps![0].status === 'running') {
            assistantMsg.steps![0].status = 'success'
            assistantMsg.steps![0].completedAt = new Date().toISOString()
            assistantMsg.steps![0].durationMs = Date.now() - new Date(assistantMsg.steps![0].startedAt).getTime()
          }
          assistantMsg.content += data.content
          sessionStore.updateLastAssistantContent(assistantMsg.content)
          nextTick(scrollToBottom)
        } else if (event === 'tool_call') {
          // 后端真实执行了绑定工具：追加工具调用步骤（工具名/参数/结果/耗时）
          const thinking = assistantMsg.steps![0]
          if (thinking && thinking.status === 'running') {
            thinking.status = 'success'
            thinking.completedAt = new Date().toISOString()
            thinking.durationMs = Date.now() - new Date(thinking.startedAt).getTime()
          }
          assistantMsg.steps!.push({
            stepId: assistantMsg.steps!.length + 1,
            sequence: assistantMsg.steps!.length + 1,
            type: 'tool_call',
            status: data.success ? 'success' : 'error',
            toolName: data.toolName || '工具',
            outsideSandbox: !!data.outsideSandbox,
            request: data.params || {},
            response: data.result != null ? { output: String(data.result) } : undefined,
            errorMessage: data.success ? undefined : String(data.error || data.result || '执行失败'),
            startedAt: new Date().toISOString(),
            completedAt: new Date().toISOString(),
            durationMs: data.durationMs || 0,
          })
          nextTick(scrollToBottom)
        } else if (event === 'done') {
          // 后端 done 事件携带真实 usage 与费用；缺失时才按字符数估算兜底
          assistantMsg.tokenUsage = {
            input: data.usage?.input ?? Math.floor(text.length * 1.5),
            output: data.usage?.output ?? Math.floor(assistantMsg.content.length * 1.2),
            total: data.usage?.total ?? Math.floor((text.length + assistantMsg.content.length) * 1.3),
            cost: data.cost ?? 0,
          }
          sessionStore.isStreaming = false
          abortController = null
          nextTick(scrollToBottom)
        } else if (event === 'error') {
          assistantMsg.content += '\n\n[错误] ' + (data.error || '未知错误')
          sessionStore.updateLastAssistantContent(assistantMsg.content)
          sessionStore.isStreaming = false
          abortController = null
        }
      },
      abortController.signal
    )
  } catch (err: any) {
    if (err.name !== 'AbortError') {
      assistantMsg.content += '\n\n[连接异常] ' + (err.message || '网络错误')
      sessionStore.updateLastAssistantContent(assistantMsg.content)
    }
    sessionStore.isStreaming = false
    abortController = null
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

async function stopStreaming() {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
  // 通知后端终止执行（会话状态落库为 stopped，停止继续产生 token 消耗）
  if (sessionStore.currentSessionId) {
    try { await stopSession(sessionStore.currentSessionId) } catch { /* 停止失败不阻塞界面 */ }
  }
  sessionStore.stopStreaming()
}

function setMode(mode: ExecutionMode) {
  sessionStore.setExecutionMode(mode)
}

function renderMarkdown(text: string): string {
  return text
    .replace(/### (.*)/g, '<h3>$1</h3>')
    .replace(/## (.*)/g, '<h2>$1</h2>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>')
}
</script>

<template>
  <div class="session-console">
    <!-- 顶部栏 -->
    <div class="console-header">
      <div class="console-header-left">
        <el-button text @click="router.push(`/agents/${agentId}`)"><UiIcon name="arrow-left" /></el-button>
        <span class="agent-name"><AgentAvatar :avatar="agentStore.current?.avatar" :size="28" /> {{ agentStore.current?.name || 'Agent' }}</span>
      </div>
      <div class="console-header-right">
        <el-radio-group v-model="sessionStore.executionMode" size="small" @change="setMode">
          <el-radio-button v-for="(label, key) in EXECUTION_MODE_MAP" :key="key" :value="key">{{ label }}</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 消息区 -->
    <div class="messages-area" ref="messagesRef">
      <div v-if="sessionStore.messages.length === 0" class="empty-chat">
        <span class="empty-icon"><UiIcon name="message" :size="26" /></span>
        <p>开始与 Agent 对话</p>
        <p class="text-muted">输入你的问题，Agent 会自动分析并回答</p>
      </div>

      <div v-for="msg in sessionStore.messages" :key="msg.messageId" class="message-row" :class="msg.role">
        <!-- 用户消息 -->
        <div v-if="msg.role === 'user'" class="message-bubble user-bubble">
          {{ msg.content }}
        </div>

        <!-- AI 消息 -->
        <div v-else class="message-bubble assistant-bubble">
          <!-- 步骤展示 -->
          <div v-if="msg.steps && msg.steps.length > 0" class="steps-section">
            <div class="steps-header" @click="showSteps = !showSteps">
              <span><UiIcon name="list" class="ii" />执行步骤 ({{ msg.steps.length }})</span>
              <UiIcon name="chevron-down" class="steps-arrow" :class="{ collapsed: !showSteps }" />
            </div>
            <div v-show="showSteps" class="steps-list">
              <div v-for="step in msg.steps" :key="step.stepId" class="step-item">
                <div class="step-icon">
                  <UiIcon name="wand" v-if="step.type === 'thinking'" :class="{ 'is-loading': step.status === 'running' }" />
                  <UiIcon name="tool" v-else-if="step.type === 'tool_call'" />
                  <UiIcon name="chart-line" v-else />
                </div>
                <div class="step-content">
                  <div class="step-title">
                    <span v-if="step.type === 'thinking'">思考</span>
                    <span v-else>{{ step.toolName }}</span>
                    <el-tag :type="step.status === 'success' ? 'success' : step.status === 'error' ? 'danger' : 'info'" size="small" round style="margin-left:8px">
                      {{ step.status === 'success' ? '成功' : step.status === 'error' ? '失败' : '执行中' }}
                    </el-tag>
                    <el-tag v-if="step.outsideSandbox" type="warning" size="small" round style="margin-left:4px">沙箱外</el-tag>
                    <span v-if="step.durationMs > 0" class="step-latency num">{{ step.durationMs }}ms</span>
                  </div>
                  <div v-if="step.type === 'thinking' && step.content" class="step-detail text-muted">{{ step.content }}</div>
                  <div v-if="step.type === 'tool_call' && step.request" class="step-detail">
                    <pre class="code-block">{{ JSON.stringify(step.request, null, 2) }}</pre>
                  </div>
                  <div v-if="step.type === 'tool_call' && step.response" class="step-detail">
                    <pre class="code-block">{{ JSON.stringify(step.response, null, 2) }}</pre>
                  </div>
                  <div v-if="step.errorMessage" class="step-error"><UiIcon name="circle-x" class="ii" />{{ step.errorMessage }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 消息内容 -->
          <div v-if="msg.content" class="message-content" v-html="renderMarkdown(msg.content)"></div>
          <div v-else-if="sessionStore.isStreaming && msg === sessionStore.messages[sessionStore.messages.length - 1]" class="typing-indicator">
            <span class="dot"></span><span class="dot"></span><span class="dot"></span>
          </div>

          <!-- Token 用量 -->
          <div v-if="msg.tokenUsage" class="token-usage text-muted">
            <UiIcon name="coins" class="ii" />{{ formatTokens(msg.tokenUsage.total) }} tokens · <UiIcon name="wallet" class="ii" />{{ formatCost(msg.tokenUsage.cost || 0) }}
          </div>
        </div>
      </div>
    </div>

    <!-- 输入栏 -->
    <div class="input-bar">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="2"
        placeholder="输入消息... (Enter 发送，Shift+Enter 换行)"
        resize="none"
        @keydown="handleKeydown"
        :disabled="sessionStore.isStreaming"
      />
      <div class="input-actions">
        <el-tooltip
          content="开启后 Agent 将在服务器真实文件系统执行文件读写与命令（支持绝对路径），不再限制在会话沙箱内，请谨慎授权"
          placement="top"
        >
          <div class="sandbox-toggle" :class="{ active: outsideSandbox }">
            <el-switch v-model="outsideSandbox" size="small" :disabled="sessionStore.isStreaming" />
            <span class="toggle-label"><UiIcon name="alert" />沙箱外运行</span>
          </div>
        </el-tooltip>
        <el-button v-if="sessionStore.isStreaming" type="danger" @click="stopStreaming">
          <UiIcon name="pause" /> 停止
        </el-button>
        <el-button v-else type="primary" @click="handleSend" :disabled="!inputText.trim()">
          <UiIcon name="send" /> 发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 会话控制台：一张带边框的完整表面，消息区用 muted 底色分层 */
.session-console {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 100px);
  background: var(--bg-surface);
  border: 1px solid var(--border-1);
  border-radius: var(--r-card);
  overflow: hidden;
}
.console-header { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border-bottom: 1px solid var(--border-1); background: var(--bg-surface); }
.console-header-left { display: flex; align-items: center; gap: 8px; }
.agent-name { font-weight: 600; font-size: 16px; color: var(--text-1); }
.messages-area { flex: 1; overflow-y: auto; padding: 20px; background: var(--bg-hover); }
.empty-chat { text-align: center; padding: 80px 0; }
.empty-icon {
  width: 64px; height: 64px; margin-bottom: 16px;
  border-radius: 18px; background: var(--accent-soft); color: var(--accent);
  display: inline-flex; align-items: center; justify-content: center;
}
.message-row { margin-bottom: 16px; }
.message-row.user { display: flex; justify-content: flex-end; }
.message-bubble { max-width: 70%; padding: 12px 16px; border-radius: 12px; }
.user-bubble { background: var(--el-color-primary); color: #fff; border-bottom-right-radius: 4px; }
.assistant-bubble { background: var(--bg-surface); border: 1px solid var(--border-1); border-bottom-left-radius: 4px; }
.steps-section { margin-bottom: 12px; border-bottom: 1px solid var(--border-1); padding-bottom: 12px; }
.steps-header { display: flex; align-items: center; justify-content: space-between; cursor: pointer; font-size: 13px; color: var(--text-3); padding: 4px 0; user-select: none; }
.steps-header:hover { color: var(--text-2); }
.steps-arrow { transition: transform 0.15s ease-out; }
.steps-arrow.collapsed { transform: rotate(-90deg); }
.steps-list { display: flex; flex-direction: column; gap: 8px; margin-top: 8px; }
.step-item { display: flex; gap: 10px; }
.step-icon { font-size: 16px; flex-shrink: 0; margin-top: 2px; color: var(--text-2); }
.step-content { flex: 1; min-width: 0; }
.step-title { font-size: 13px; font-weight: 500; color: var(--text-1); display: flex; align-items: center; }
.step-latency { margin-left: 8px; font-size: 12px; color: var(--text-3); }
.step-detail { font-size: 12px; margin-top: 4px; }
.step-error { font-size: 12px; color: var(--st-danger); margin-top: 4px; }
.code-block { background: var(--bg-hover); border: 1px solid var(--border-1); padding: 8px; border-radius: 8px; font-family: var(--font-num); font-size: 12px; overflow-x: auto; margin: 4px 0; }
.message-content { font-size: 14px; line-height: 1.6; }
.message-content :deep(h2) { font-size: 16px; margin: 12px 0 8px; }
.message-content :deep(h3) { font-size: 15px; margin: 10px 0 6px; }
.message-content :deep(code) { background: var(--bg-hover); border: 1px solid var(--border-1); padding: 1px 5px; border-radius: 4px; font-family: var(--font-num); font-size: 13px; }
.message-content :deep(strong) { font-weight: 600; }
.token-usage { font-size: 12px; margin-top: 8px; padding-top: 8px; border-top: 1px solid var(--border-1); }
.typing-indicator { display: flex; gap: 4px; padding: 8px 0; }
.dot { width: 8px; height: 8px; border-radius: 50%; background: var(--text-3); animation: bounce 1.4s infinite ease-in-out; }
.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }
@keyframes bounce { 0%,80%,100% { transform: scale(0); } 40% { transform: scale(1); } }
@media (prefers-reduced-motion: reduce) {
  .steps-arrow { transition: none; }
  .dot { animation: none; }
}
.input-bar { padding: 12px 16px; border-top: 1px solid var(--border-1); background: var(--bg-surface); }
.input-actions { display: flex; align-items: center; justify-content: flex-end; gap: 16px; margin-top: 8px; }
.sandbox-toggle { display: flex; align-items: center; gap: 6px; cursor: default; }
.sandbox-toggle .toggle-label { display: flex; align-items: center; gap: 2px; font-size: 12px; color: var(--text-3); user-select: none; }
.sandbox-toggle.active .toggle-label { color: var(--st-warning); font-weight: 600; }
</style>
