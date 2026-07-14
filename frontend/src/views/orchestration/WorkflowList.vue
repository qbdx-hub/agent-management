<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { formatDateTime } from '@/utils/format'

const router = useRouter()
const workflows = ref([
  { id: 1, name: '代码审查流水线', description: '代码编写→审查→测试的自动化流水线', nodeCount: 4, lastRunStatus: 'success', updatedAt: '2026-07-14T10:00:00+08:00' },
  { id: 2, name: '文档自动生成', description: '根据代码变更自动生成 API 文档', nodeCount: 3, lastRunStatus: 'success', updatedAt: '2026-07-13T15:00:00+08:00' },
  { id: 3, name: '部署前检查', description: '部署前自动检查配置、依赖和安全', nodeCount: 5, lastRunStatus: 'error', updatedAt: '2026-07-12T09:00:00+08:00' },
])

function statusType(s: string) { return s === 'success' ? 'success' : s === 'error' ? 'danger' : 'info' }
function statusLabel(s: string) { return s === 'success' ? '成功' : s === 'error' ? '失败' : '运行中' }
</script>

<template>
  <div class="workflow-list-page">
    <div class="page-header">
      <h2>Agent 编排</h2>
      <el-button type="primary" @click="router.push('/orchestration/0/edit')"><el-icon><Plus /></el-icon> 创建工作流</el-button>
    </div>
    <el-card>
      <el-table :data="workflows">
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="nodeCount" label="节点数" width="80" />
        <el-table-column label="最近运行" width="80"><template #default="{ row }"><el-tag :type="statusType(row.lastRunStatus)" size="small">{{ statusLabel(row.lastRunStatus) }}</el-tag></template></el-table-column>
        <el-table-column label="更新时间" width="160"><template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template></el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="router.push(`/orchestration/${row.id}/edit`)">编辑</el-button>
            <el-button text type="primary" size="small">运行</el-button>
            <el-button text type="danger" size="small">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>.workflow-list-page { max-width: 1200px; }</style>
