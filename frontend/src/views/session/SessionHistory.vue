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
  switch (status) { case 'completed': return 'circle-check'; case 'active': return 'loader'; case 'error': return 'circle-x'; default: return 'pause' }
}
function statusColor(status: string) {
  return status === 'completed' ? '#178a5b' : status === 'active' ? '#5a54e8' : status === 'error' ? '#cf3f4f' : '#9295a0'
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
      <el-button type="primary" @click="router.push(`/agents/${agentId}/chat`)"><UiIcon name="plus" />新会话</el-button>
    </div>
    <el-card shadow="never">
      <el-table v-loading="loading" :data="sessions" @row-click="(row: SessionSummary) => openSession(row.sessionId)" style="cursor:pointer">
        <el-table-column width="40"><template #default="{ row }"><UiIcon :name="statusIcon(row.status)" :size="17" :style="{ color: statusColor(row.status) }" :class="{ 'is-loading': row.status === 'active' }" /></template></el-table-column>
        <el-table-column prop="title" label="标题" />
        <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 'completed' ? 'success' : row.status === 'error' ? 'danger' : 'info'" size="small" round>{{ SESSION_STATUS_MAP[row.status] }}</el-tag></template></el-table-column>
        <el-table-column label="消息数" width="90"><template #default="{ row }"><span class="num">{{ row.messageCount }}</span></template></el-table-column>
        <el-table-column label="Token" width="100"><template #default="{ row }"><span class="num">{{ formatTokens(row.totalTokens) }}</span></template></el-table-column>
        <el-table-column label="费用" width="90"><template #default="{ row }"><span class="num">{{ formatCost(row.totalCost) }}</span></template></el-table-column>
        <el-table-column label="时间" width="160"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="80"><template #default="{ row }"><el-button type="danger" text size="small" @click.stop="handleDelete(row.sessionId)">删除</el-button></template></el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.session-history-page { max-width: 1200px; margin: 0 auto; }
.page-header h2 {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.2px;
}
</style>
