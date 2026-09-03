<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAgentStore } from '@/stores/agent'
import { getSessionList } from '@/api/session'
import { SESSION_STATUS_MAP } from '@/utils/constants'
import { timeAgo, formatTokens } from '@/utils/format'
import type { SessionSummary } from '@/types/session'

const router = useRouter()
const agentStore = useAgentStore()
const recentSessions = ref<SessionSummary[]>([])

onMounted(async () => {
  try {
    if (agentStore.list.length === 0) {
      await agentStore.fetchAgentList()
    }
    if (agentStore.list.length > 0) {
      const res = await getSessionList(agentStore.list[0].id, { page: 1, pageSize: 5 })
      if (res.code === 0 && res.data) {
        recentSessions.value = res.data.list || []
      }
    }
  } catch { /* ignore */ }
})

// 状态语义色：只表达真实状态（运行中绿点、已完成灰点、错误红点）
function statusDotClass(status: string) {
  switch (status) {
    case 'active': return 'run'
    case 'error': return 'err'
    default: return 'done'
  }
}

function openSession(session: SessionSummary) {
  // 列表本身取自第一个 Agent 的会话，回跳同一 Agent 的控制台并定位会话
  const agentId = agentStore.list[0]?.id
  if (agentId) {
    router.push(`/agents/${agentId}/chat?sessionId=${session.sessionId}`)
  }
}

function goToAgent() {
  if (agentStore.list.length > 0) {
    router.push(`/agents/${agentStore.list[0].id}/sessions`)
  } else {
    router.push('/agents')
  }
}
</script>

<template>
  <div class="panel">
    <div class="panel-head">
      <span>最近任务</span>
      <el-button text type="primary" size="small" @click="goToAgent">查看全部</el-button>
    </div>
    <div v-if="recentSessions.length === 0" class="empty-state">
      <p>暂无会话记录</p>
    </div>
    <template v-else>
      <div
        v-for="session in recentSessions"
        :key="session.sessionId"
        class="task-row"
        @click="openSession(session)"
      >
        <span class="dot" :class="statusDotClass(session.status)"></span>
        <div class="task-body">
          <div class="task-title">{{ session.title }}</div>
          <div class="task-meta">
            {{ SESSION_STATUS_MAP[session.status] || session.status }} · {{ timeAgo(session.createdAt) }}
            · <span class="num">{{ formatTokens(session.totalTokens) }}</span> tokens
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.panel {
  background: var(--bg-surface);
  border: 1px solid var(--border-1);
  border-radius: var(--r-card);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px 18px;
  border-bottom: 1px solid var(--border-1);
  font-size: 14px;
  font-weight: 700;
}
.empty-state {
  text-align: center;
  padding: 28px 0;
}
.empty-state p {
  font-size: 13px;
  color: var(--text-3);
}
.task-row {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 13px 18px;
  cursor: pointer;
  transition: background 0.15s ease-out;
}
.task-row:hover {
  background: var(--bg-hover);
}
.task-row + .task-row {
  border-top: 1px solid var(--border-1);
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.dot.run { background: var(--st-running); }
.dot.done { background: #d3d5dd; }
.dot.err { background: var(--st-danger); }
.task-body {
  flex: 1;
  min-width: 0;
}
.task-title {
  font-size: 13.5px;
  font-weight: 500;
  color: var(--text-1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.task-meta {
  font-size: 11.5px;
  color: var(--text-3);
  margin-top: 1px;
}
@media (prefers-reduced-motion: reduce) {
  .task-row { transition: none; }
}
</style>
