<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { mockApprovals } from '@/mock/security'
import { APPROVAL_STATUS_MAP } from '@/utils/constants'
import { formatDateTime } from '@/utils/format'
import { ElMessage } from 'element-plus'
import type { ApprovalItem } from '@/types/security'

const approvals = ref<ApprovalItem[]>([])
const activeTab = ref('pending')

onMounted(() => { approvals.value = [...mockApprovals] })

const filteredApprovals = computed(() => {
  if (activeTab.value === 'pending') return approvals.value.filter(a => a.status === 'pending')
  if (activeTab.value === 'approved') return approvals.value.filter(a => a.status === 'approved')
  if (activeTab.value === 'rejected') return approvals.value.filter(a => a.status === 'rejected')
  return approvals.value
})

function handleApprove(id: number) {
  const item = approvals.value.find(a => a.approvalId === id)
  if (item) { item.status = 'approved'; ElMessage.success('已通过') }
}
function handleReject(id: number) {
  const item = approvals.value.find(a => a.approvalId === id)
  if (item) { item.status = 'rejected'; ElMessage.success('已拒绝') }
}

import { computed } from 'vue'
</script>

<template>
  <div class="approval-list-page">
    <div class="page-header"><h2>审批列表</h2></div>
    <el-card>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="待审批" name="pending" />
        <el-tab-pane label="已通过" name="approved" />
        <el-tab-pane label="已拒绝" name="rejected" />
      </el-tabs>
      <div v-for="item in filteredApprovals" :key="item.approvalId" class="approval-item">
        <div class="approval-info">
          <div class="approval-title">{{ item.detail }}</div>
          <div class="text-muted" style="font-size:12px">
            {{ item.ruleName }} · {{ item.applicantName }} · {{ formatDateTime(item.createdAt) }}
          </div>
        </div>
        <div v-if="item.status === 'pending'" class="approval-actions">
          <el-button type="success" size="small" @click="handleApprove(item.approvalId)">通过</el-button>
          <el-button type="danger" size="small" @click="handleReject(item.approvalId)">拒绝</el-button>
        </div>
        <el-tag v-else :type="item.status === 'approved' ? 'success' : 'danger'" size="small">
          {{ APPROVAL_STATUS_MAP[item.status] }}
        </el-tag>
      </div>
      <el-empty v-if="filteredApprovals.length === 0" description="暂无数据" />
    </el-card>
  </div>
</template>

<style scoped>
.approval-list-page { max-width: 1200px; }
.approval-item { display: flex; align-items: center; justify-content: space-between; padding: 16px 0; border-bottom: 1px solid var(--el-border-color-lighter); }
.approval-item:last-child { border-bottom: none; }
.approval-info { flex: 1; }
.approval-title { font-size: 14px; margin-bottom: 4px; }
.approval-actions { display: flex; gap: 8px; }
</style>
