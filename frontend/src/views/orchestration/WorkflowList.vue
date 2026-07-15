<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getWorkflowList, createWorkflow, deleteWorkflow } from '@/api/workflow'
import { formatDateTime } from '@/utils/format'
import type { WorkflowSummary } from '@/types/workflow'

const router = useRouter()
const workflows = ref<WorkflowSummary[]>([])
const loading = ref(false)
const pagination = reactive({ page: 1, pageSize: 20, total: 0 })

async function fetchList() {
  loading.value = true
  try {
    const res = await getWorkflowList({ ...pagination })
    if (res.code === 0) {
      workflows.value = res.data.list
      pagination.total = res.data.total
    }
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  const res = await createWorkflow({ name: '未命名工作流' })
  if (res.code === 0) {
    router.push(`/orchestration/${res.data.id}/edit`)
  }
}

async function handleDelete(row: WorkflowSummary) {
  try {
    await ElMessageBox.confirm(`确认删除工作流「${row.name}」？`, '提示', { type: 'warning' })
    const res = await deleteWorkflow(row.id)
    if (res.code === 0) {
      ElMessage.success('已删除')
      fetchList()
    }
  } catch {
    // 用户取消
  }
}

function statusType(s: string) {
  return s === 'active' ? 'success' : s === 'archived' ? 'info' : 'warning'
}
function statusLabel(s: string) {
  return s === 'active' ? '已启用' : s === 'archived' ? '已归档' : '草稿'
}

onMounted(fetchList)
</script>

<template>
  <div class="workflow-list-page">
    <div class="page-header">
      <h2>Agent 编排</h2>
      <el-button type="primary" @click="handleCreate"><el-icon><Plus /></el-icon> 创建工作流</el-button>
    </div>
    <el-card v-loading="loading">
      <el-table :data="workflows">
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="nodeCount" label="节点数" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建者" width="110">
          <template #default="{ row }">{{ row.creatorName || '-' }}</template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="router.push(`/orchestration/${row.id}/edit`)">编辑</el-button>
            <el-button text type="primary" size="small" @click="ElMessage.info('运行功能待接入')">运行</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>.workflow-list-page { max-width: 1200px; }</style>
