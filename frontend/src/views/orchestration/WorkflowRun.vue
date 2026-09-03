<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getWorkflowRun, approveWorkflowRun } from '@/api/workflow'
import { formatDateTime, formatLatency, formatTokens, formatCost } from '@/utils/format'
import type { WorkflowRun, WorkflowNodeResult } from '@/types/workflow'

const route = useRoute()
const router = useRouter()

const run = ref<WorkflowRun | null>(null)
const loading = ref(false)
const approving = ref(false)
let pollTimer: number | undefined

const runId = computed(() => route.params.runId as string)

/** 运行中/等待审批时持续轮询（等待审批期间他人处理也能自动刷新） */
const activeStatus = computed(() => run.value?.status === 'running' || run.value?.status === 'waiting_approval')

const statusMeta: Record<string, { label: string; type: 'primary' | 'warning' | 'success' | 'danger' }> = {
  running: { label: '执行中', type: 'primary' },
  waiting_approval: { label: '等待审批', type: 'warning' },
  completed: { label: '已完成', type: 'success' },
  failed: { label: '已失败', type: 'danger' },
}

const NODE_TYPE_LABEL: Record<string, string> = {
  start: '开始', agent: 'Agent', tool: '工具', condition: '条件判断', approval: '人工审批', end: '结束',
}

const nodeStatusMeta: Record<string, { label: string; type: 'primary' | 'success' | 'danger' | 'warning' | 'info' }> = {
  success: { label: '完成', type: 'success' },
  error: { label: '失败', type: 'danger' },
  waiting: { label: '等待审批', type: 'warning' },
  rejected: { label: '已拒绝', type: 'info' },
  skipped: { label: '已跳过', type: 'info' },
}

function nodeColor(status: string) {
  return status === 'success' ? '#67c23a'
    : status === 'error' ? '#f56c6c'
    : status === 'waiting' ? '#e6a23c'
    : '#c0c4cc'
}

async function fetchRun() {
  const res = await getWorkflowRun(runId.value)
  if (res.code === 0) {
    run.value = res.data
  }
}

async function pollLoop() {
  if (!activeStatus.value) return
  try {
    await fetchRun()
  } catch {
    // 轮询失败静默，下个周期重试
  }
  pollTimer = window.setTimeout(pollLoop, 2000)
}

function startPolling() {
  stopPolling()
  pollTimer = window.setTimeout(pollLoop, 2000)
}
function stopPolling() {
  if (pollTimer) {
    window.clearTimeout(pollTimer)
    pollTimer = undefined
  }
}

async function handleApprove(approved: boolean) {
  try {
    const { value } = await ElMessageBox.prompt(
      approved ? '请输入审批意见（可选）' : '拒绝后本次运行将置为失败，请输入拒绝原因',
      approved ? '通过审批' : '拒绝审批',
      { confirmButtonText: approved ? '通过' : '拒绝', cancelButtonText: '取消', inputPlaceholder: '审批意见', inputValue: '' },
    )
    approving.value = true
    const res = await approveWorkflowRun(runId.value, approved, value || undefined)
    if (res.code === 0) {
      ElMessage.success(approved ? '已通过，流程继续执行' : '已拒绝，运行结束')
      await fetchRun()
      startPolling()
    }
  } catch {
    // 用户取消
  } finally {
    approving.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    await fetchRun()
  } finally {
    loading.value = false
  }
  startPolling()
})

onBeforeUnmount(stopPolling)
</script>

<template>
  <div class="workflow-run-page" v-loading="loading">
    <div class="page-header">
      <div style="display:flex;align-items:center;gap:8px">
        <el-button text @click="router.push('/orchestration')"><el-icon><ArrowLeft /></el-icon></el-button>
        <h2>运行记录 #{{ runId }}</h2>
      </div>
      <div v-if="run" style="display:flex;align-items:center;gap:10px">
        <el-tag v-if="run.status" :type="statusMeta[run.status]?.type" effect="dark" size="small">
          {{ statusMeta[run.status]?.label || run.status }}
        </el-tag>
        <el-button v-if="run.status === 'waiting_approval'" type="success" size="small" :loading="approving" @click="handleApprove(true)">通过</el-button>
        <el-button v-if="run.status === 'waiting_approval'" type="danger" size="small" :loading="approving" @click="handleApprove(false)">拒绝</el-button>
      </div>
    </div>

    <template v-if="run">
      <el-card style="margin-bottom:16px">
        <el-descriptions :column="4" size="small">
          <el-descriptions-item label="触发者">{{ run.triggeredByName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ run.startedAt ? formatDateTime(run.startedAt) : '-' }}</el-descriptions-item>
          <el-descriptions-item label="耗时">{{ run.duration != null ? formatLatency(run.duration) : '进行中' }}</el-descriptions-item>
          <el-descriptions-item label="Token / 费用">
            {{ formatTokens(run.totalTokens || 0) }} / {{ formatCost(run.totalCost || 0) }}
          </el-descriptions-item>
        </el-descriptions>
        <el-descriptions v-if="run.input && Object.keys(run.input).length" :column="1" size="small" style="margin-top:8px">
          <el-descriptions-item label="运行输入">
            <span class="mono">{{ JSON.stringify(run.input) }}</span>
          </el-descriptions-item>
        </el-descriptions>
        <el-alert v-if="run.error" :title="run.error" type="error" :closable="false" style="margin-top:8px" />
      </el-card>

      <el-card>
        <template #header><span>节点执行明细</span></template>
        <el-empty v-if="!run.nodeResults?.length" description="等待节点开始执行..." />
        <el-timeline v-else>
          <el-timeline-item
            v-for="node in run.nodeResults"
            :key="node.nodeId + '-' + node.sequence"
            :color="nodeColor(node.status)"
          >
            <el-card shadow="never">
              <div class="node-head">
                <strong>{{ node.label || node.nodeId }}</strong>
                <el-tag size="small" type="info">{{ NODE_TYPE_LABEL[node.type || ''] || node.type }}</el-tag>
                <el-tag size="small" :type="nodeStatusMeta[node.status]?.type">
                  {{ nodeStatusMeta[node.status]?.label || node.status }}
                </el-tag>
                <span v-if="node.durationMs" class="text-muted" style="font-size:12px">{{ formatLatency(node.durationMs) }}</span>
                <span v-if="node.tokens" class="text-muted" style="font-size:12px">{{ formatTokens(node.tokens) }}</span>
              </div>
              <div v-if="node.error" class="node-error">{{ node.error }}</div>
              <pre v-else-if="node.output" class="node-output">{{ node.output }}</pre>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </el-card>

      <el-card v-if="run.status === 'completed' && run.output?.result" style="margin-top:16px">
        <template #header><span>最终输出</span></template>
        <pre class="node-output">{{ run.output.result }}</pre>
      </el-card>
    </template>
    <el-empty v-else-if="!loading" description="运行记录不存在" />
  </div>
</template>

<style scoped>
.workflow-run-page { max-width: 900px; }
.node-head { display: flex; align-items: center; gap: 8px; }
.node-output {
  margin: 8px 0 0;
  padding: 8px 12px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 300px;
  overflow-y: auto;
}
.node-error {
  margin-top: 8px;
  padding: 8px 12px;
  background: var(--el-color-danger-light-9);
  color: var(--el-color-danger);
  border-radius: 4px;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-word;
}
.mono { font-size: 12px; word-break: break-all; }
</style>
