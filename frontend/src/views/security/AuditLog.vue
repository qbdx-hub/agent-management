<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { mockAuditLogs } from '@/mock/security'
import { AUDIT_ACTION_MAP } from '@/utils/constants'
import { formatDateTime } from '@/utils/format'
import type { AuditLogItem } from '@/types/security'

const logs = ref<AuditLogItem[]>([])
const keyword = ref('')
const actionFilter = ref('')

onMounted(() => { logs.value = [...mockAuditLogs] })

const filteredLogs = computed(() => {
  let list = logs.value
  if (keyword.value) list = list.filter(l => l.userName.includes(keyword.value) || l.detail.includes(keyword.value))
  if (actionFilter.value) list = list.filter(l => l.action === actionFilter.value)
  return list
})

const actionOptions = computed(() => {
  return Object.entries(AUDIT_ACTION_MAP).map(([value, label]) => ({ value, label }))
})

import { computed } from 'vue'
</script>

<template>
  <div class="audit-log-page">
    <div class="page-header"><h2>审计日志</h2></div>
    <el-card class="mb-16">
      <div class="filter-bar">
        <el-input v-model="keyword" placeholder="搜索用户/详情..." style="width:250px" clearable />
        <el-select v-model="actionFilter" placeholder="操作类型" style="width:180px" clearable>
          <el-option v-for="opt in actionOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </div>
    </el-card>
    <el-card>
      <el-table :data="filteredLogs" size="small">
        <el-table-column label="时间" width="160"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></el-table-column>
        <el-table-column prop="userName" label="用户" width="80" />
        <el-table-column label="操作" width="120"><template #default="{ row }">{{ AUDIT_ACTION_MAP[row.action] || row.action }}</template></el-table-column>
        <el-table-column prop="resource" label="资源" width="120" />
        <el-table-column prop="detail" label="详情" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column label="结果" width="70"><template #default="{ row }"><el-tag :type="row.result === 'success' ? 'success' : 'danger'" size="small">{{ row.result === 'success' ? '成功' : '失败' }}</el-tag></template></el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>.audit-log-page { max-width: 1400px; }</style>
