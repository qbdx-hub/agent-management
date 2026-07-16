<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAgentStore } from '@/stores/agent'
import { getSessionList } from '@/api/session'
import { SESSION_STATUS_MAP } from '@/utils/constants'
import { timeAgo } from '@/utils/format'
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

function statusIcon(status: string) {
  switch (status) {
    case 'completed': return 'CircleCheckFilled'
    case 'active': return 'Loading'
    case 'error': return 'CircleCloseFilled'
    case 'stopped': return 'VideoPause'
    default: return 'Clock'
  }
}
function statusColor(status: string) {
  switch (status) {
    case 'completed': return '#67c23a'
    case 'active': return '#409eff'
    case 'error': return '#f56c6c'
    default: return '#909399'
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
  <el-card>
    <template #header>
      <div class="card-header">
        <span>最近任务</span>
        <el-button text type="primary" size="small" @click="goToAgent">查看全部</el-button>
      </div>
    </template>
    <div v-if="recentSessions.length === 0" class="empty-state">
      <el-icon style="font-size:32px;color:#c0c4cc"><Clock /></el-icon>
      <p class="text-muted">暂无会话记录</p>
    </div>
    <div v-else class="task-list">
      <div v-for="session in recentSessions" :key="session.sessionId" class="task-item">
        <el-icon
          class="task-icon"
          :style="{ color: statusColor(session.status) }"
          :class="{ 'is-loading': session.status === 'active' }"
        >
          <component :is="statusIcon(session.status)" />
        </el-icon>
        <div class="task-info">
          <div class="task-title">{{ session.title }}</div>
          <div class="task-meta text-muted">
            {{ SESSION_STATUS_MAP[session.status] || session.status }} · {{ timeAgo(session.createdAt) }}
          </div>
        </div>
        <span class="task-cost text-muted">{{ session.totalTokens }} tokens</span>
      </div>
    </div>
  </el-card>
</template>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.empty-state { text-align: center; padding: 24px 0; }
.empty-state p { margin-top: 8px; font-size: 13px; }
.task-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.task-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.task-item:last-child {
  border-bottom: none;
}
.task-icon {
  font-size: 18px;
  flex-shrink: 0;
}
.task-info {
  flex: 1;
  min-width: 0;
}
.task-title {
  font-size: 14px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.task-meta {
  font-size: 12px;
}
.task-cost {
  font-size: 12px;
  flex-shrink: 0;
}
</style>
