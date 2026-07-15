<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSessionStore } from '@/stores/session'
import { useAgentStore } from '@/stores/agent'
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

onMounted(async () => {
  await agentStore.fetchAgentDetail(agentId.value)
  // 如果有 sessionId 参数，加载已有会话
  const sid = route.query.sessionId
  if (sid) {
    await sessionStore.fetchSessionDetail(Number(sid))
  }
})

// 自动滚动到底部
watch(() => sessionStore.messages.length, () => {
  nextTick(() => { if (messagesRef.value) messagesRef.value.scrollTop = messagesRef.value.scrollHeight })
})

function handleSend() {
  const text = inputText.value.trim()
  if (!text) return
  inputText.value = ''
  sessionStore.sendMessage(agentId.value, text)

  // Mock SSE 模拟
  simulateSSE()
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

function simulateSSE() {
  sessionStore.isStreaming = true

  // 模拟 assistant 消息
  const assistantMsg: Message = {
    messageId: Date.now() + 1, role: 'assistant', content: '',
    steps: [], createdAt: new Date().toISOString(),
  }
  sessionStore.addAssistantMessage(assistantMsg)

  // Step 1: thinking
  setTimeout(() => {
    assistantMsg.steps!.push({
      stepId: 1, sequence: 1, type: 'thinking', status: 'success',
      content: '我正在分析用户的问题，需要调用相关工具获取信息...',
      startedAt: new Date().toISOString(), completedAt: new Date().toISOString(), durationMs: 1200,
    })
    sessionStore.updateLastAssistantContent('')
  }, 800)

  // Step 2: tool_call
  setTimeout(() => {
    assistantMsg.steps!.push({
      stepId: 2, sequence: 2, type: 'tool_call', status: 'success',
      toolName: '搜索网页', toolIcon: '09-search',
      request: { query: '相关信息' }, response: { results: ['结果1', '结果2'] },
      startedAt: new Date().toISOString(), completedAt: new Date().toISOString(), durationMs: 800,
    })
  }, 2000)

  // Step 3: thinking
  setTimeout(() => {
    assistantMsg.steps!.push({
      stepId: 3, sequence: 3, type: 'thinking', status: 'success',
      content: '根据搜索结果，我来整理回答...',
      startedAt: new Date().toISOString(), completedAt: new Date().toISOString(), durationMs: 1500,
    })
  }, 3500)

  // Final message
  setTimeout(() => {
    assistantMsg.content = '## 分析结果\n\n根据我的分析，以下是相关信息：\n\n1. **要点一** — 这是一个重要的发现\n2. **要点二** — 需要进一步确认\n3. **要点三** — 建议采取行动\n\n如果需要更详细的信息，请告诉我。'
    assistantMsg.tokenUsage = { input: 1200, output: 450, total: 1650, cost: 0.02 }
    sessionStore.updateLastAssistantContent(assistantMsg.content)
    sessionStore.isStreaming = false
  }, 5000)
}

function stopStreaming() {
  sessionStore.stopStreaming()
}

function setMode(mode: ExecutionMode) {
  sessionStore.setExecutionMode(mode)
}
</script>

<template>
  <div class="session-console">
    <!-- 顶部栏 -->
    <div class="console-header">
      <div class="console-header-left">
        <el-button text @click="router.push(`/agents/${agentId}`)"><el-icon><ArrowLeft /></el-icon></el-button>
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
        <img src="/icons/01-chat.png" class="empty-icon" alt="" />
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
          <div v-if="msg.steps && msg.steps.length > 0 && showSteps" class="steps-section">
            <div class="steps-header" @click="showSteps = !showSteps">
              <span><el-icon class="ii"><List /></el-icon>执行步骤 ({{ msg.steps.length }})</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <div class="steps-list">
              <div v-for="step in msg.steps" :key="step.stepId" class="step-item">
                <div class="step-icon">
                  <el-icon v-if="step.type === 'thinking'"><MagicStick /></el-icon>
                  <el-icon v-else-if="step.type === 'tool_call'"><Tools /></el-icon>
                  <el-icon v-else><TrendCharts /></el-icon>
                </div>
                <div class="step-content">
                  <div class="step-title">
                    <span v-if="step.type === 'thinking'">思考</span>
                    <span v-else>{{ step.toolName }}</span>
                    <el-tag :type="step.status === 'success' ? 'success' : step.status === 'error' ? 'danger' : 'info'" size="small" style="margin-left:8px">
                      {{ step.status === 'success' ? '成功' : step.status === 'error' ? '失败' : '执行中' }}
                    </el-tag>
                    <span class="text-muted" style="margin-left:8px;font-size:12px">{{ step.durationMs }}ms</span>
                  </div>
                  <div v-if="step.type === 'thinking' && step.content" class="step-detail text-muted">{{ step.content }}</div>
                  <div v-if="step.type === 'tool_call' && step.request" class="step-detail">
                    <pre class="code-block">{{ JSON.stringify(step.request, null, 2) }}</pre>
                  </div>
                  <div v-if="step.type === 'tool_call' && step.response" class="step-detail">
                    <pre class="code-block">{{ JSON.stringify(step.response, null, 2) }}</pre>
                  </div>
                  <div v-if="step.errorMessage" class="step-error"><el-icon class="ii"><CircleCloseFilled /></el-icon>{{ step.errorMessage }}</div>
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
            <el-icon class="ii"><Coin /></el-icon>{{ formatTokens(msg.tokenUsage.total) }} tokens · <el-icon class="ii"><Wallet /></el-icon>{{ formatCost(msg.tokenUsage.cost || 0) }}
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
        <el-button v-if="sessionStore.isStreaming" type="danger" @click="stopStreaming">
          <el-icon><VideoPause /></el-icon> 停止
        </el-button>
        <el-button v-else type="primary" @click="handleSend" :disabled="!inputText.trim()">
          <el-icon><Promotion /></el-icon> 发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
function renderMarkdown(text: string): string {
  // 简单 Markdown 渲染
  return text
    .replace(/### (.*)/g, '<h3>$1</h3>')
    .replace(/## (.*)/g, '<h2>$1</h2>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>')
}
</script>

<style scoped>
.session-console { display: flex; flex-direction: column; height: calc(100vh - 100px); }
.console-header { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border-bottom: 1px solid var(--el-border-color-light); background: #fff; }
.console-header-left { display: flex; align-items: center; gap: 8px; }
.agent-name { font-weight: 600; font-size: 16px; }
.messages-area { flex: 1; overflow-y: auto; padding: 20px; background: #f5f7fa; }
.empty-chat { text-align: center; padding: 80px 0; }
.empty-icon { width: 56px; height: 56px; object-fit: contain; margin-bottom: 16px; }
.message-row { margin-bottom: 16px; }
.message-row.user { display: flex; justify-content: flex-end; }
.message-bubble { max-width: 70%; padding: 12px 16px; border-radius: 12px; }
.user-bubble { background: var(--el-color-primary); color: #fff; border-bottom-right-radius: 4px; }
.assistant-bubble { background: #fff; border: 1px solid var(--el-border-color-lighter); border-bottom-left-radius: 4px; }
.steps-section { margin-bottom: 12px; border-bottom: 1px solid var(--el-border-color-lighter); padding-bottom: 12px; }
.steps-header { display: flex; align-items: center; justify-content: space-between; cursor: pointer; font-size: 13px; color: #909399; padding: 4px 0; }
.steps-list { display: flex; flex-direction: column; gap: 8px; margin-top: 8px; }
.step-item { display: flex; gap: 10px; }
.step-icon { font-size: 16px; flex-shrink: 0; margin-top: 2px; color: #606266; }
.step-content { flex: 1; min-width: 0; }
.step-title { font-size: 13px; font-weight: 500; display: flex; align-items: center; }
.step-detail { font-size: 12px; margin-top: 4px; }
.step-error { font-size: 12px; color: var(--el-color-danger); margin-top: 4px; }
.code-block { background: #f5f7fa; padding: 8px; border-radius: 4px; font-size: 12px; overflow-x: auto; margin: 4px 0; }
.message-content { font-size: 14px; line-height: 1.6; }
.message-content :deep(h2) { font-size: 16px; margin: 12px 0 8px; }
.message-content :deep(h3) { font-size: 15px; margin: 10px 0 6px; }
.message-content :deep(code) { background: #f0f0f0; padding: 2px 4px; border-radius: 3px; font-size: 13px; }
.message-content :deep(strong) { font-weight: 600; }
.token-usage { font-size: 12px; margin-top: 8px; padding-top: 8px; border-top: 1px solid var(--el-border-color-lighter); }
.typing-indicator { display: flex; gap: 4px; padding: 8px 0; }
.dot { width: 8px; height: 8px; border-radius: 50%; background: #909399; animation: bounce 1.4s infinite ease-in-out; }
.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }
@keyframes bounce { 0%,80%,100% { transform: scale(0); } 40% { transform: scale(1); } }
.input-bar { padding: 12px 16px; border-top: 1px solid var(--el-border-color-light); background: #fff; }
.input-actions { display: flex; justify-content: flex-end; margin-top: 8px; }
</style>
