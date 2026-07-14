<script setup lang="ts">
import { mockSessions } from '@/mock/sessions'
import { SESSION_STATUS_MAP } from '@/utils/constants'
import { timeAgo } from '@/utils/format'

const recentSessions = mockSessions.slice(0, 5)

function statusIcon(status: string) {
  switch (status) {
    case 'completed': return '✅'
    case 'active': return '🔄'
    case 'error': return '❌'
    case 'stopped': return '⏹️'
    default: return '⏳'
  }
}
</script>

<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>最近任务</span>
        <el-button text type="primary" size="small" @click="$router.push('/agents/1/sessions')">查看全部</el-button>
      </div>
    </template>
    <div class="task-list">
      <div v-for="session in recentSessions" :key="session.sessionId" class="task-item">
        <span class="task-icon">{{ statusIcon(session.status) }}</span>
        <div class="task-info">
          <div class="task-title">{{ session.title }}</div>
          <div class="task-meta text-muted">
            {{ SESSION_STATUS_MAP[session.status] }} · {{ timeAgo(session.lastMessageAt) }}
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
