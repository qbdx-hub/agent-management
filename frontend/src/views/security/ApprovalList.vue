<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getApprovals, approveApproval, rejectApproval } from '@/api/security'
import { APPROVAL_STATUS_MAP } from '@/utils/constants'
import { formatDateTime } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ApprovalItem } from '@/types/security'

const router = useRouter()
const approvals = ref<ApprovalItem[]>([])
const activeTab = ref('pending')
const loading = ref(false)
const pagination = ref({ page: 1, pageSize: 20, total: 0 })

async function fetchList() {
  loading.value = true
  try {
    const res = await getApprovals({
      status: activeTab.value === 'all' ? undefined : activeTab.value,
      page: pagination.value.page,
      pageSize: pagination.value.pageSize,
    })
    if (res.code === 0) {
      approvals.value = res.data.list
      pagination.value.total = res.data.total
    }
  } finally {
    loading.value = false
  }
}

function handleTabChange() {
  pagination.value.page = 1
  fetchList()
}

/** resourceType=workflow_run 的审批可跳转到运行页查看进度 */
function isWorkflowRun(item: ApprovalItem) {
  return item.resourceType === 'workflow_run' && item.resourceId != null
}
function goRun(item: ApprovalItem) {
  router.push(`/orchestration/run/${item.resourceId}`)
}

async function handleApprove(item: ApprovalItem) {
  try {
    const { value } = await ElMessageBox.prompt('请输入审批意见（可选）', '通过审批', {
      confirmButtonText: '通过', cancelButtonText: '取消', inputPlaceholder: '审批意见', inputValue: '',
    })
    const res = await approveApproval(item.approvalId, value || undefined)
    if (res.code === 0) {
      ElMessage.success('已通过' + (isWorkflowRun(item) ? '，工作流将从审批节点继续执行' : ''))
      fetchList()
    }
  } catch {
    // 用户取消
  }
}

async function handleReject(item: ApprovalItem) {
  try {
    const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝审批', {
      confirmButtonText: '拒绝', cancelButtonText: '取消', inputPlaceholder: '拒绝原因', inputValue: '',
    })
    const res = await rejectApproval(item.approvalId, value || undefined)
    if (res.code === 0) {
      ElMessage.success('已拒绝' + (isWorkflowRun(item) ? '，对应运行将置为失败' : ''))
      fetchList()
    }
  } catch {
    // 用户取消
  }
}

const actionLabel: Record<string, string> = {
  approve: '工作流审批', publish: '发布', register: '注册', delete: '删除',
}

onMounted(fetchList)
</script>

<template>
  <div class="approval-list-page">
    <div class="page-header"><h2>审批列表</h2></div>
    <el-card v-loading="loading" shadow="never" class="table-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="待审批" name="pending" />
        <el-tab-pane label="已通过" name="approved" />
        <el-tab-pane label="已拒绝" name="rejected" />
        <el-tab-pane label="全部" name="all" />
      </el-tabs>
      <div v-for="item in approvals" :key="item.approvalId" class="approval-item">
        <div class="approval-info">
          <div class="approval-title">
            <el-tag v-if="actionLabel[item.action]" size="small" type="info" round style="margin-right:6px">{{ actionLabel[item.action] }}</el-tag>
            {{ item.detail || item.resourceName || `审批 #${item.approvalId}` }}
            <el-button v-if="isWorkflowRun(item)" text type="primary" size="small" @click="goRun(item)">查看运行</el-button>
          </div>
          <div class="approval-meta">
            {{ item.ruleName ? item.ruleName + ' · ' : '' }}{{ item.applicantName || '—' }} 申请 · {{ formatDateTime(item.createdAt) }}
            <template v-if="item.resolvedAt"> · {{ item.approverName || '—' }} 处理于 {{ formatDateTime(item.resolvedAt) }}</template>
          </div>
          <div v-if="item.reason" class="approval-meta" style="margin-top:4px">意见：{{ item.reason }}</div>
        </div>
        <div v-if="item.status === 'pending'" class="approval-actions">
          <el-button type="success" size="small" @click="handleApprove(item)">通过</el-button>
          <el-button type="danger" size="small" @click="handleReject(item)">拒绝</el-button>
        </div>
        <el-tag v-else :type="item.status === 'approved' ? 'success' : 'danger'" size="small" round>
          {{ APPROVAL_STATUS_MAP[item.status] }}
        </el-tag>
      </div>
      <el-empty v-if="!loading && approvals.length === 0" description="暂无数据：工作流执行到「人工审批」节点时会在这里出现待办" />
      <div v-if="pagination.total > pagination.pageSize" style="margin-top:12px;display:flex;justify-content:flex-end">
        <el-pagination
          layout="prev, pager, next"
          :total="pagination.total"
          :page-size="pagination.pageSize"
          :current-page="pagination.page"
          @current-change="(p: number) => { pagination.page = p; fetchList() }"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.approval-list-page { max-width: 1200px; margin: 0 auto; }
.page-header h2 {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.2px;
}
.table-card :deep(.el-card__body) { padding: 20px; }
.approval-item { display: flex; align-items: center; justify-content: space-between; padding: 16px 0; border-bottom: 1px solid var(--border-1); }
.approval-item:last-child { border-bottom: none; }
.approval-info { flex: 1; }
.approval-title { font-size: 14px; font-weight: 600; color: var(--text-1); margin-bottom: 4px; }
.approval-meta { font-size: 12px; color: var(--text-3); }
.approval-actions { display: flex; gap: 8px; }
</style>
