<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { formatDateTime, formatLatency } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const runNodes = ref([
  { id: 'n1', label: '需求分析', status: 'success', durationMs: 3200 },
  { id: 'n2', label: '代码生成', status: 'success', durationMs: 8500 },
  { id: 'n3', label: '代码审查', status: 'success', durationMs: 4200 },
  { id: 'n4', label: '条件判断', status: 'success', durationMs: 100 },
  { id: 'n5', label: '人工确认', status: 'running', durationMs: 0 },
])

function nodeColor(status: string) {
  return status === 'success' ? '#67c23a' : status === 'error' ? '#f56c6c' : status === 'running' ? '#409eff' : '#c0c4cc'
}
</script>

<template>
  <div class="workflow-run-page">
    <div class="page-header">
      <div style="display:flex;align-items:center;gap:8px">
        <el-button text @click="router.push('/orchestration')"><el-icon><ArrowLeft /></el-icon></el-button>
        <h2>运行记录</h2>
      </div>
      <span class="text-muted">Run ID: {{ route.params.runId }}</span>
    </div>
    <el-card>
      <el-timeline>
        <el-timeline-item v-for="node in runNodes" :key="node.id" :color="nodeColor(node.status)" :timestamp="node.status === 'running' ? '执行中...' : formatLatency(node.durationMs)">
          <el-card shadow="never">
            <div style="display:flex;align-items:center;gap:8px">
              <strong>{{ node.label }}</strong>
              <el-tag :type="node.status === 'success' ? 'success' : node.status === 'error' ? 'danger' : 'primary'" size="small">
                {{ node.status === 'success' ? '完成' : node.status === 'error' ? '失败' : '执行中' }}
              </el-tag>
              <span v-if="node.durationMs" class="text-muted" style="font-size:12px">{{ formatLatency(node.durationMs) }}</span>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<style scoped>.workflow-run-page { max-width: 800px; }</style>
