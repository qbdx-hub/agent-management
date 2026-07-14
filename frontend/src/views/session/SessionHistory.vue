<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { mockSessions } from '@/mock/sessions'
import { SESSION_STATUS_MAP } from '@/utils/constants'
import { formatTokens, formatCost, formatDateTime } from '@/utils/format'
import type { SessionSummary } from '@/types/session'

const route = useRoute()
const router = useRouter()
const agentId = computed(() => Number(route.params.id))
const sessions = ref<SessionSummary[]>([])

onMounted(() => { sessions.value = [...mockSessions] })

function statusIcon(status: string) {
  switch (status) { case 'completed': return '✅'; case 'active': return '🔄'; case 'error': return '❌'; default: return '⏹️' }
}

function openSession(sid: number) {
  router.push(`/agents/${agentId.value}/chat?sessionId=${sid}`)
}
</script>

<template>
  <div class="session-history-page">
    <div class="page-header">
      <h2>会话历史</h2>
      <el-button type="primary" @click="router.push(`/agents/${agentId}/chat`)"><el-icon><Plus /></el-icon> 新会话</el-button>
    </div>
    <el-card>
      <el-table :data="sessions" @row-click="(row: SessionSummary) => openSession(row.sessionId)" style="cursor:pointer">
        <el-table-column width="40"><template #default="{ row }"><span>{{ statusIcon(row.status) }}</span></template></el-table-column>
        <el-table-column prop="title" label="标题" />
        <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 'completed' ? 'success' : row.status === 'error' ? 'danger' : 'info'" size="small">{{ SESSION_STATUS_MAP[row.status] }}</el-tag></template></el-table-column>
        <el-table-column prop="messageCount" label="消息数" width="80" />
        <el-table-column label="Token" width="100"><template #default="{ row }">{{ formatTokens(row.totalTokens) }}</template></el-table-column>
        <el-table-column label="费用" width="80"><template #default="{ row }">{{ formatCost(row.totalCost) }}</template></el-table-column>
        <el-table-column label="时间" width="160"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>.session-history-page { max-width: 1200px; }</style>
