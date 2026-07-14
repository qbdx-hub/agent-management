<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { mockMessages } from '@/mock/sessions'
import { formatLatency } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const traceId = ref(route.params.traceId as string)
const steps = ref(mockMessages[1]?.steps || [])
</script>

<template>
  <div class="trace-detail-page">
    <div class="page-header">
      <div style="display:flex;align-items:center;gap:8px">
        <el-button text @click="router.push('/monitor')"><el-icon><ArrowLeft /></el-icon></el-button>
        <h2>链路追踪</h2>
      </div>
      <span class="text-muted">Trace ID: {{ traceId }}</span>
    </div>
    <el-card>
      <el-timeline>
        <el-timeline-item v-for="step in steps" :key="step.stepId" :type="step.status === 'success' ? 'success' : step.status === 'error' ? 'danger' : 'primary'" :timestamp="step.startedAt">
          <el-card shadow="never">
            <div style="display:flex;align-items:center;gap:8px;margin-bottom:8px">
              <span v-if="step.type === 'thinking'">🧠 思考</span>
              <span v-else>🔧 {{ step.toolName }}</span>
              <el-tag :type="step.status === 'success' ? 'success' : 'danger'" size="small">{{ step.status }}</el-tag>
              <span class="text-muted" style="font-size:12px">{{ formatLatency(step.durationMs) }}</span>
            </div>
            <div v-if="step.content" style="font-size:13px;color:#606266">{{ step.content }}</div>
            <div v-if="step.request" style="margin-top:8px"><pre class="code-block">{{ JSON.stringify(step.request, null, 2) }}</pre></div>
            <div v-if="step.response" style="margin-top:8px"><pre class="code-block">{{ JSON.stringify(step.response, null, 2) }}</pre></div>
            <div v-if="step.errorMessage" style="color:var(--el-color-danger);font-size:13px;margin-top:4px">❌ {{ step.errorMessage }}</div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<style scoped>
.trace-detail-page { max-width: 1000px; }
.code-block { background: #f5f7fa; padding: 8px; border-radius: 4px; font-size: 12px; overflow-x: auto; margin: 0; }
</style>
