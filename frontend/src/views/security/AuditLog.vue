<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { formatDateTime } from '@/utils/format'
import { ElMessage } from 'element-plus'
import { listAuditLogs } from '@/api/audit'
import type { AuditLog } from '@/api/audit'

const logs = ref<AuditLog[]>([])
const loading = ref(false)
const keyword = ref('')
const actionFilter = ref('')

async function loadLogs() {
  loading.value = true
  try {
    const res = await listAuditLogs(100)
    if (res.code === 0) {
      logs.value = res.data || []
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '加载审计日志失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadLogs()
})

const filteredLogs = computed(() => {
  let list = logs.value
  if (keyword.value) {
    const kw = keyword.value.toLowerCase()
    list = list.filter(l =>
      (l.userName && l.userName.toLowerCase().includes(kw)) ||
      (l.detail && l.detail.toLowerCase().includes(kw)) ||
      (l.resourceName && l.resourceName.toLowerCase().includes(kw))
    )
  }
  if (actionFilter.value) {
    list = list.filter(l => l.action === actionFilter.value)
  }
  return list
})

// 操作类型选项
const actionOptions = [
  { value: 'knowledge.create', label: '创建知识库' },
  { value: 'knowledge.delete', label: '删除知识库' },
  { value: 'document.upload', label: '上传文档' },
  { value: 'document.delete', label: '删除文档' },
]

/** 操作类型显示名映射 */
function getActionLabel(action: string): string {
  const found = actionOptions.find(opt => opt.value === action)
  return found ? found.label : action
}

/** 资源类型显示名映射 */
function getResourceTypeLabel(type: string): string {
  const map: Record<string, string> = {
    knowledge_base: '知识库',
    document: '文档',
    agent: 'Agent',
    tool: '工具',
    user: '用户',
    workspace: '工作空间',
  }
  return map[type] || type
}
</script>

<template>
  <div class="audit-log-page" v-loading="loading">
    <div class="page-header">
      <h2>审计日志</h2>
    </div>
    <el-card class="mb-16">
      <div class="filter-bar">
        <el-input v-model="keyword" placeholder="搜索用户/详情/资源..." style="width:280px" clearable />
        <el-select v-model="actionFilter" placeholder="操作类型" style="width:180px" clearable>
          <el-option v-for="opt in actionOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </div>
    </el-card>
    <el-card>
      <el-table :data="filteredLogs" size="small">
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column prop="userName" label="用户" width="90" />
        <el-table-column label="操作" width="110">
          <template #default="{ row }">{{ getActionLabel(row.action) }}</template>
        </el-table-column>
        <el-table-column label="资源类型" width="90">
          <template #default="{ row }">{{ getResourceTypeLabel(row.resourceType) }}</template>
        </el-table-column>
        <el-table-column prop="resourceName" label="资源名称" width="150" show-overflow-tooltip />
        <el-table-column prop="detail" label="详情" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP" width="130" />
        <el-table-column label="结果" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.result === 'success' ? 'success' : 'danger'" size="small">
              {{ row.result === 'success' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && filteredLogs.length === 0" description="暂无审计日志" />
    </el-card>
  </div>
</template>

<style scoped>
.audit-log-page { max-width: 1400px; }
.filter-bar { display: flex; gap: 12px; }
.mb-16 { margin-bottom: 16px; }
</style>
