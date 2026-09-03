<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getWorkflowList, createWorkflow, deleteWorkflow, runWorkflow, getWorkflowRuns } from '@/api/workflow'
import { formatDateTime } from '@/utils/format'
import type { WorkflowSummary, WorkflowRun, WorkflowRunStatus } from '@/types/workflow'

const router = useRouter()
const workflows = ref<WorkflowSummary[]>([])
const loading = ref(false)
const pagination = reactive({ page: 1, pageSize: 20, total: 0 })

// 运行对话框状态
const showRun = ref(false)
const runTarget = ref<WorkflowSummary | null>(null)
const runInput = ref('')
const running = ref(false)

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

function openRun(row: WorkflowSummary) {
  runTarget.value = row
  runInput.value = ''
  showRun.value = true
}

// 运行历史对话框状态
const showRuns = ref(false)
const runsTarget = ref<WorkflowSummary | null>(null)
const runsList = ref<WorkflowRun[]>([])
const runsLoading = ref(false)

const RUN_STATUS_META: Record<WorkflowRunStatus, { label: string; type: 'success' | 'error' | 'warning' | 'info' | 'primary' }> = {
  running: { label: '运行中', type: 'primary' },
  waiting_approval: { label: '等待审批', type: 'warning' },
  completed: { label: '已完成', type: 'success' },
  failed: { label: '已失败', type: 'error' },
}

async function openRuns(row: WorkflowSummary) {
  runsTarget.value = row
  showRuns.value = true
  runsLoading.value = true
  runsList.value = []
  try {
    const res = await getWorkflowRuns(row.id, { page: 1, pageSize: 20 })
    if (res.code === 0) runsList.value = res.data?.list || []
  } finally {
    runsLoading.value = false
  }
}

function goRunDetail(run: WorkflowRun) {
  showRuns.value = false
  router.push(`/orchestration/${run.workflowId}/run/${run.id}`)
}

async function confirmRun() { if (!runTarget.value) return
  running.value = true
  try {
    const res = await runWorkflow(runTarget.value.id, runInput.value.trim() ? { question: runInput.value.trim() } : {})
    if (res.code === 0) {
      showRun.value = false
      ElMessage.success('已开始运行')
      router.push(`/orchestration/${runTarget.value.id}/run/${res.data.runId}`)
    }
  } finally {
    running.value = false
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
      <el-button type="primary" @click="handleCreate"><UiIcon name="plus" /> 创建工作流</el-button>
    </div>
    <el-card v-loading="loading" shadow="never" class="table-card">
      <el-table :data="workflows">
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="nodeCount" label="节点数" width="80">
          <template #default="{ row }"><span class="num">{{ row.nodeCount }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small" round>{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建者" width="110">
          <template #default="{ row }">{{ row.creatorName || '—' }}</template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="250">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="router.push(`/orchestration/${row.id}/edit`)">编辑</el-button>
            <el-button text type="primary" size="small" @click="openRun(row)">运行</el-button>
            <el-button text type="primary" size="small" @click="openRuns(row)">历史</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showRun" :title="`运行「${runTarget?.name}」`" width="480px">
      <el-form label-width="80px">
        <el-form-item label="任务输入">
          <el-input
            v-model="runInput"
            type="textarea"
            :rows="4"
            placeholder="输入本次运行的任务描述（将作为 question 传给工作流的 Agent 节点），可留空"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRun = false">取消</el-button>
        <el-button type="primary" :loading="running" @click="confirmRun">开始运行</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showRuns" :title="`运行历史 · ${runsTarget?.name || ''}`" width="720px">
      <el-table v-loading="runsLoading" :data="runsList" size="small">
        <el-table-column prop="id" label="运行ID" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="RUN_STATUS_META[row.status as WorkflowRunStatus]?.type || 'info'" size="small">
              {{ RUN_STATUS_META[row.status as WorkflowRunStatus]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开始时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.startedAt || row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="耗时" width="90">
          <template #default="{ row }"><span class="num">{{ row.duration != null ? row.duration + 's' : '—' }}</span></template>
        </el-table-column>
        <el-table-column label="费用" width="90">
          <template #default="{ row }"><span class="num">{{ row.totalCost != null ? '¥' + row.totalCost.toFixed(4) : '—' }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="goRunDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!runsLoading && runsList.length === 0" description="还没有运行记录，点击「运行」开始第一次执行" />
    </el-dialog>
  </div>
</template>

<style scoped>
.workflow-list-page { max-width: 1200px; margin: 0 auto; }
.page-header h2 {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.2px;
}
.table-card :deep(.el-card__body) { padding: 20px; }
</style>
