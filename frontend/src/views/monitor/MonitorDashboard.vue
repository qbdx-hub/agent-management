<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getMonitorOverview, getTokenTrend, getAgentHealth, getErrorLogs, getAlertRecords } from '@/api/monitor'
import { formatPercent, formatLatency, formatTokens, formatNumber } from '@/utils/format'
import { ALERT_SEVERITY_MAP } from '@/utils/constants'
import type { MonitorOverview, TokenTrendPoint, AgentHealthItem, ErrorLogItem, AlertRecord } from '@/types/monitor'

const router = useRouter()
const loading = ref(false)
const overview = ref<MonitorOverview>({
  activeAgentCount: 0, runningTaskCount: 0, todayCallCount: 0,
  successRate: 0, avgLatencyMs: 0, p99LatencyMs: 0, totalTokensToday: 0,
  trends: { callCountChange: 0, successRateChange: 0, latencyChange: 0 }
})
const tokenTrend = ref<TokenTrendPoint[]>([])
const agentHealth = ref<AgentHealthItem[]>([])
const errors = ref<ErrorLogItem[]>([])
const alerts = ref<AlertRecord[]>([])
const period = ref('today')

const maxToken = computed(() => {
  const all = tokenTrend.value.flatMap(p => [p.input, p.output])
  return Math.max(...all, 1)
})

function healthColor(status: string) {
  return status === 'healthy' ? '#67c23a' : status === 'warning' ? '#e6a23c' : '#f56c6c'
}

async function loadData() {
  loading.value = true
  try {
    // 加载概览
    const overviewRes = await getMonitorOverview(period.value)
    if (overviewRes.code === 0 && overviewRes.data) {
      overview.value = overviewRes.data
    }

    // 加载 Token 趋势
    const granularity = period.value === 'today' ? 'hour' : 'day'
    const trendRes = await getTokenTrend(period.value === 'today' ? '7d' : period.value, granularity)
    if (trendRes.code === 0 && trendRes.data) {
      tokenTrend.value = trendRes.data.series || []
    }

    // 加载 Agent 健康排行
    const healthRes = await getAgentHealth()
    if (healthRes.code === 0 && healthRes.data) {
      agentHealth.value = healthRes.data
    }

    // 加载错误日志
    const errorRes = await getErrorLogs({ page: 1, pageSize: 10 })
    if (errorRes.code === 0 && errorRes.data) {
      errors.value = ((errorRes.data as any).list || errorRes.data) as ErrorLogItem[]
    }

    // 加载告警记录
    const alertRes = await getAlertRecords({ page: 1, pageSize: 5 })
    if (alertRes.code === 0 && alertRes.data) {
      alerts.value = ((alertRes.data as any).list || alertRes.data) as AlertRecord[]
    }
  } catch (e) {
    console.error('加载监控数据失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
watch(period, loadData)
</script>

<template>
  <div class="monitor-page" v-loading="loading">
    <div class="page-header">
      <h2>监控面板</h2>
      <div style="display:flex;gap:8px">
        <el-radio-group v-model="period" size="small">
          <el-radio-button value="today">今天</el-radio-button>
          <el-radio-button value="7d">7天</el-radio-button>
          <el-radio-button value="30d">30天</el-radio-button>
        </el-radio-group>
        <el-button size="small" @click="router.push('/monitor/alerts')">告警配置</el-button>
      </div>
    </div>

    <!-- 指标卡片 -->
    <div class="stats-row">
      <el-card shadow="hover" class="metric-card">
        <div class="metric-val">{{ overview.activeAgentCount }}</div>
        <div class="metric-lbl">活跃 Agent</div>
      </el-card>
      <el-card shadow="hover" class="metric-card">
        <div class="metric-val">{{ overview.runningTaskCount }}</div>
        <div class="metric-lbl">执行中任务</div>
      </el-card>
      <el-card shadow="hover" class="metric-card">
        <div class="metric-val">{{ formatNumber(overview.todayCallCount) }}</div>
        <div class="metric-lbl">今日调用</div>
        <div class="metric-trend" :class="overview.trends.callCountChange > 0 ? 'up' : 'down'">
          <el-icon v-if="overview.trends.callCountChange > 0"><ArrowUp /></el-icon><el-icon v-else><ArrowDown /></el-icon> {{ Math.abs(overview.trends.callCountChange * 100).toFixed(1) }}%
        </div>
      </el-card>
      <el-card shadow="hover" class="metric-card">
        <div class="metric-val">{{ formatPercent(overview.successRate) }}</div>
        <div class="metric-lbl">成功率</div>
      </el-card>
      <el-card shadow="hover" class="metric-card">
        <div class="metric-val">{{ formatLatency(overview.avgLatencyMs) }}</div>
        <div class="metric-lbl">平均延迟</div>
      </el-card>
      <el-card shadow="hover" class="metric-card">
        <div class="metric-val">{{ formatLatency(overview.p99LatencyMs) }}</div>
        <div class="metric-lbl">P99 延迟</div>
      </el-card>
    </div>

    <el-row :gutter="20">
      <!-- Token 趋势 -->
      <el-col :span="16">
        <el-card class="mb-24">
          <template #header><span>Token 用量趋势</span></template>
          <div class="chart-placeholder">
            <div class="bar-chart">
              <div v-for="(point, idx) in tokenTrend" :key="idx" class="bar-col">
                <div class="bar-input" :style="{ height: (point.input / maxToken * 120) + 'px' }" title="Input"></div>
                <div class="bar-output" :style="{ height: (point.output / maxToken * 120) + 'px' }" title="Output"></div>
                <div class="bar-label">{{ point.time }}</div>
              </div>
            </div>
            <div class="chart-legend">
              <span class="legend-item"><span class="legend-dot input"></span> Input</span>
              <span class="legend-item"><span class="legend-dot output"></span> Output</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- Agent 健康 -->
      <el-col :span="8">
        <el-card class="mb-24">
          <template #header><span>Agent 健康排行</span></template>
          <div v-if="agentHealth.length === 0" class="text-muted" style="text-align:center;padding:20px">暂无数据</div>
          <div v-for="agent in agentHealth" :key="agent.agentId" class="health-item">
            <div class="health-header">
              <span class="health-name">{{ agent.agentName }}</span>
              <span class="health-rate" :style="{ color: healthColor(agent.status) }">{{ formatPercent(agent.successRate) }}</span>
            </div>
            <el-progress :percentage="agent.successRate * 100" :color="healthColor(agent.status)" :show-text="false" :stroke-width="6" />
            <div class="health-meta text-muted">{{ formatLatency(agent.avgLatencyMs) }} · {{ agent.callCount24h }} 次/24h</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 告警记录 + 错误日志 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header><span>最近告警</span></template>
          <div v-if="alerts.length === 0" class="text-muted" style="text-align:center;padding:20px">暂无告警</div>
          <div v-for="alert in alerts" :key="alert.recordId" class="alert-item">
            <el-tag :type="alert.severity === 'critical' ? 'danger' : alert.severity === 'warning' ? 'warning' : 'info'" size="small">{{ ALERT_SEVERITY_MAP[alert.severity] }}</el-tag>
            <span class="alert-msg">{{ alert.message }}</span>
            <span class="text-muted" style="font-size:12px;white-space:nowrap">{{ alert.triggeredAt?.split('T')[1]?.slice(0,5) }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>错误日志</span></template>
          <div v-if="errors.length === 0" class="text-muted" style="text-align:center;padding:20px">暂无错误</div>
          <el-table v-else :data="errors" size="small">
            <el-table-column prop="agentName" label="Agent" width="130" />
            <el-table-column prop="errorType" label="类型" width="100" />
            <el-table-column prop="errorMessage" label="错误信息" show-overflow-tooltip />
            <el-table-column label="时间" width="80"><template #default="{ row }">{{ row.occurredAt?.split('T')[1]?.slice(0,5) }}</template></el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.monitor-page { max-width: 1400px; }
.metric-card { text-align: center; }
.metric-val { font-size: 28px; font-weight: 700; color: var(--el-color-primary); }
.metric-lbl { color: #909399; font-size: 13px; margin-top: 2px; }
.metric-trend { font-size: 12px; margin-top: 4px; }
.metric-trend.up { color: #67c23a; }
.metric-trend.down { color: #f56c6c; }
.bar-chart { display: flex; align-items: flex-end; gap: 4px; height: 140px; padding: 0 8px; }
.bar-col { display: flex; flex-direction: column; align-items: center; gap: 2px; flex: 1; }
.bar-input { width: 100%; background: var(--el-color-primary); border-radius: 2px 2px 0 0; min-height: 2px; }
.bar-output { width: 100%; background: var(--el-color-primary-light-5); border-radius: 2px 2px 0 0; min-height: 2px; }
.bar-label { font-size: 10px; color: #909399; }
.chart-legend { display: flex; gap: 16px; justify-content: center; margin-top: 12px; font-size: 12px; }
.legend-item { display: flex; align-items: center; gap: 4px; }
.legend-dot { width: 10px; height: 10px; border-radius: 2px; }
.legend-dot.input { background: var(--el-color-primary); }
.legend-dot.output { background: var(--el-color-primary-light-5); }
.health-item { padding: 10px 0; border-bottom: 1px solid var(--el-border-color-lighter); }
.health-item:last-child { border-bottom: none; }
.health-header { display: flex; justify-content: space-between; margin-bottom: 4px; }
.health-name { font-size: 13px; font-weight: 500; }
.health-rate { font-size: 13px; font-weight: 600; }
.health-meta { font-size: 12px; margin-top: 4px; }
.alert-item { display: flex; align-items: center; gap: 8px; padding: 8px 0; border-bottom: 1px solid var(--el-border-color-lighter); }
.alert-item:last-child { border-bottom: none; }
.alert-msg { flex: 1; font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
