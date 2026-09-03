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
        <el-button text @click="router.push('/monitor')"><UiIcon name="arrow-left" /></el-button>
        <h2>链路追踪</h2>
      </div>
      <span class="trace-id">Trace ID: {{ traceId }}</span>
    </div>

    <!-- 时间线直接落在页面上，步骤内容用边框面板替代嵌套卡片 -->
    <el-empty v-if="steps.length === 0" description="暂无链路数据" />
    <el-timeline v-else class="trace-timeline">
      <el-timeline-item
        v-for="step in steps"
        :key="step.stepId"
        :type="step.status === 'success' ? 'success' : step.status === 'error' ? 'danger' : 'primary'"
        :timestamp="step.startedAt"
      >
        <div class="step-panel">
          <div class="step-head">
            <span v-if="step.type === 'thinking'" class="step-title"><UiIcon name="wand" class="ii" />思考</span>
            <span v-else class="step-title"><UiIcon name="tool" class="ii" />{{ step.toolName }}</span>
            <el-tag :type="step.status === 'success' ? 'success' : 'danger'" size="small" round>{{ step.status }}</el-tag>
            <span class="step-latency num">{{ formatLatency(step.durationMs) }}</span>
          </div>
          <div v-if="step.content" class="step-content">{{ step.content }}</div>
          <div v-if="step.request" class="step-io"><pre class="code-block">{{ JSON.stringify(step.request, null, 2) }}</pre></div>
          <div v-if="step.response" class="step-io"><pre class="code-block">{{ JSON.stringify(step.response, null, 2) }}</pre></div>
          <div v-if="step.errorMessage" class="step-error"><UiIcon name="circle-x" class="ii" />{{ step.errorMessage }}</div>
        </div>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<style scoped>
.trace-detail-page { max-width: 1000px; margin: 0 auto; }
.trace-id { font-size: 12.5px; color: var(--text-3); }
.trace-timeline { padding: 8px 0 0 4px; }
.step-panel {
  border: 1px solid var(--border-1);
  border-radius: var(--r-control);
  background: var(--bg-surface);
  padding: 14px 16px;
}
.step-head { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.step-title { font-weight: 600; font-size: 13.5px; color: var(--text-1); }
.step-latency { font-size: 12px; color: var(--text-3); }
.step-content { font-size: 13px; color: var(--text-2); }
.step-io { margin-top: 8px; }
.step-error { color: var(--st-danger); font-size: 13px; margin-top: 6px; }
.code-block {
  background: var(--bg-hover);
  border: 1px solid var(--border-1);
  padding: 10px 12px;
  border-radius: 8px;
  font-family: var(--font-num);
  font-size: 12px;
  overflow-x: auto;
  margin: 0;
}
</style>
