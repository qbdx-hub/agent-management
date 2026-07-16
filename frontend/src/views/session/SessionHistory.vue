<script setup lang="ts">
import { ref, onMounted, onActivated, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getSessionList, deleteSession } from '@/api/session'
import { SESSION_STATUS_MAP } from '@/utils/constants'
import { formatTokens, formatCost, formatDateTime } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { SessionSummary } from '@/types/session'

const route = useRoute()
const router = useRouter()
const agentId = computed(() => Number(route.params.id))
const sessions = ref<SessionSummary[]>([])
const loading = ref(false)

async function loadSessions() {
  loading.value = true
  try {
    const res = await getSessionList(agentId.value, { page: 1, pageSize: 50 })
    if (res.code === 0 && res.data) {
      sessions.value = res.data.list || []
    }
  } catch {
    ElMessage.error('加载会话历史失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadSessions)
onActivated(loadSessions)

function statusIcon(status: string) {
  switch (status) { case 'completed': return 'CircleCheckFilled'; case 'active': return 'Loading'; case 'error': return 'CircleCloseFilled'; default: return 'VideoPause' }
}
function statusColor(status: string) {
  return status === 'completed' ? '#67c23a' : status === 'active' ? '#409eff' : status === 'error' ? '#f56c6c' : '#909399'
}

function openSession(sid: number) {
  router.push(`/agents/${agentId.value}/chat?sessionId=${sid}`)
}

async function handleDelete(sid: number) {
  try {
    await ElMessageBox.confirm('确定删除该会话？', '提示', { type: 'warning' })
    const res = await deleteSession(sid)
    if (res.code === 0) {
      sessions.value = sessions.value.filter(s => s.sessionId !== sid)
      ElMessage.success('已删除')
    }
  } catch { /* cancelled */ }
}
</script>

<template>
  <div class="session-history-page">
    <div class="page-header">
      <h2>会话历史</h2>
      <el-button type="primary" @click="router.push(`/agents/${agentId}/chat`)"><el-icon><Plus /></el-icon> 新会话</el-button>
    </div>
    <el-card>
      <el-table v-loading="loading" :data="sessions" @row-click="(row: SessionSummary) => openSession(row.sessionId)" style="cursor:pointer">
        <el-table-column width="40"><template #default="{ row }"><el-icon :style="{ color: statusColor(row.status) }" :class="{ 'is-loading': row.status === 'active' }"><component :is="statusIcon(row.status)" /></el-icon></template></el-table-column>
        <el-table-column prop="title" label="标题" />
        <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 'completed' ? 'success' : row.status === 'error' ? 'danger' : 'info'" size="small">{{ SESSION_STATUS_MAP[row.status] }}</el-tag></template></el-table-column>
        <el-table-column prop="messageCount" label="消息数" width="80" />
        <el-table-column label="Token" width="100"><template #default="{ row }">{{ formatTokens(row.totalTokens) }}</template></el-table-column>
        <el-table-column label="费用" width="80"><template #default="{ row }">{{ formatCost(row.totalCost) }}</template></el-table-column>
        <el-table-column label="时间" width="160"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="80"><template #default="{ row }"><el-button type="danger" text size="small" @click.stop="handleDelete(row.sessionId)">删除</el-button></template></el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>.session-history-page { max-width: 1200px; }</style>
