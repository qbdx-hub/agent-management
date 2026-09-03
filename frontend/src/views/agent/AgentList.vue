<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAgentStore } from '@/stores/agent'
import { AGENT_STATUS_MAP, AGENT_STATUS_COLORS } from '@/utils/constants'
import { formatPercent, timeAgo } from '@/utils/format'

const router = useRouter()
const agentStore = useAgentStore()

onMounted(() => {
  agentStore.fetchAgentList()
})

function handleSearch() {
  agentStore.pagination.page = 1
  agentStore.fetchAgentList()
}

function handlePageChange(page: number) {
  agentStore.pagination.page = page
  agentStore.fetchAgentList()
}

function handleSizeChange(size: number) {
  agentStore.pagination.pageSize = size
  agentStore.pagination.page = 1
  agentStore.fetchAgentList()
}

async function handleDelete(id: number, name: string) {
  try {
    await agentStore.deleteAgent(id)
  } catch {}
}

function goToChat(id: number) {
  router.push(`/agents/${id}/chat`)
}
</script>

<template>
  <div class="agent-list-page">
    <div class="page-header">
      <h2>Agent 管理</h2>
      <el-button type="primary" @click="router.push('/agents/create')">
        <UiIcon name="plus" />新建 Agent
      </el-button>
    </div>

    <!-- 搜索/筛选与表格合并为一张卡 -->
    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-input
          v-model="agentStore.filter.keyword"
          placeholder="搜索 Agent 名称..."
          style="width: 250px"
          clearable
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select
          v-model="agentStore.filter.status"
          placeholder="状态筛选"
          style="width: 140px"
          clearable
          @change="handleSearch"
        >
          <el-option v-for="(label, key) in AGENT_STATUS_MAP" :key="key" :label="label" :value="key" />
        </el-select>
        <el-button @click="handleSearch">搜索</el-button>
      </div>

      <el-table :data="agentStore.list" v-loading="agentStore.loading" style="width: 100%">
        <el-table-column label="Agent" min-width="220">
          <template #default="{ row }">
            <div class="agent-cell" @click="router.push(`/agents/${row.id}`)">
              <AgentAvatar :avatar="row.avatar" :size="36" />
              <div>
                <div class="agent-name">{{ row.name }}</div>
                <div class="agent-desc">{{ row.description }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <el-tag :type="(AGENT_STATUS_COLORS[row.status] as any)" size="small" round>
              {{ AGENT_STATUS_MAP[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="模型" width="160">
          <template #default="{ row }">
            <span class="num model-name">{{ row.modelName || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="成功率" width="100">
          <template #default="{ row }">
            <span class="num" :class="{ muted: row.successRate == null }">{{ formatPercent(row.successRate) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="会话数" width="90">
          <template #default="{ row }"><span class="num">{{ row.totalSessions }}</span></template>
        </el-table-column>
        <el-table-column label="更新时间" width="130">
          <template #default="{ row }">{{ timeAgo(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="router.push(`/agents/${row.id}`)">配置</el-button>
            <el-button text type="primary" size="small" @click="goToChat(row.id)">对话</el-button>
            <el-popconfirm title="确定删除该 Agent？" @confirm="handleDelete(row.id, row.name)">
              <template #reference>
                <el-button text type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="agentStore.pagination.page"
          v-model:page-size="agentStore.pagination.pageSize"
          :total="agentStore.pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.agent-list-page { max-width: 1400px; margin: 0 auto; }
.page-header h2 {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.2px;
}
.table-card :deep(.el-card__body) { padding: 20px; }
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.agent-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}
.agent-cell:hover .agent-name { color: var(--accent); }
.agent-name { font-weight: 600; font-size: 13.5px; color: var(--text-1); }
.agent-desc {
  font-size: 12px;
  color: var(--text-3);
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.model-name { font-size: 12.5px; color: var(--text-2); }
.muted { color: var(--text-3); font-weight: 500; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
