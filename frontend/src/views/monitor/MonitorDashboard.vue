<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getMonitorOverview, getTokenTrend, getAgentHealth, getErrorLogs, getAlertRecords, getMonitorCharts } from '@/api/monitor'
import { formatPercent, formatLatency, formatTokens, formatNumber, formatCost } from '@/utils/format'
import { ALERT_SEVERITY_MAP } from '@/utils/constants'
import type { MonitorOverview, TokenTrendPoint, AgentHealthItem, ErrorLogItem, AlertRecord, MonitorCharts } from '@/types/monitor'

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
const charts = ref<MonitorCharts>({
  callTrend: [], costTrend: [], agentDistribution: [], errorTypeDistribution: []
})

// ====== 图表计算 ======
const maxCall = computed(() =>
  Math.max(...charts.value.callTrend.map(p => Math.max(p.calls, p.errors)), 1))
const costTotal = computed(() => charts.value.costTrend.reduce((s, p) => s + (p.cost || 0), 0))
const maxCost = computed(() => Math.max(...charts.value.costTrend.map(p => p.cost || 0), 0.000001))
const maxAgentCalls = computed(() =>
  Math.max(...charts.value.agentDistribution.map(a => a.calls), 1))
const errorTotal = computed(() =>
  charts.value.errorTypeDistribution.reduce((s, e) => s + e.count, 0))

// 横轴标签抽稀：桶多时隔 N 个显示一个，避免文字重叠
function labelEvery(len: number) {
  return len > 25 ? 5 : len > 20 ? 4 : 1
}

// 错误类型中文名（后端存英文枚举，未识别的展示原值）
const ERROR_TYPE_LABELS: Record<string, string> = {
  llm_error: '模型调用错误', tool_error: '工具执行错误', timeout: '超时',
  network: '网络异常', workflow: '工作流错误', unknown: '其他'
}
function errorTypeLabel(t: string) {
  return ERROR_TYPE_LABELS[t] || t
}

// 环图配色（与 V4 状态色板一致）
const DONUT_COLORS = ['#5a54e8', '#b3730f', '#cf3f4f', '#3b6fd4', '#0f8a80', '#9295a0']
function donutColor(i: number) {
  return DONUT_COLORS[i % DONUT_COLORS.length]
}
const donutStyle = computed(() => {
  const total = errorTotal.value
  if (!total) return { background: 'var(--border-1)' }
  let acc = 0
  const stops: string[] = []
  charts.value.errorTypeDistribution.forEach((e, i) => {
    const from = (acc / total) * 360
    acc += e.count
    stops.push(`${donutColor(i)} ${from}deg ${(acc / total) * 360}deg`)
  })
  return { background: `conic-gradient(${stops.join(', ')})` }
})

const maxToken = computed(() => {
  const all = tokenTrend.value.flatMap(p => [p.input, p.output])
  return Math.max(...all, 1)
})

function healthColor(status: string) {
  return status === 'healthy' ? '#178a5b' : status === 'warning' ? '#b3730f' : '#cf3f4f'
}

async function loadData() {
  loading.value = true
  try {
    // 加载概览
    const overviewRes = await getMonitorOverview(period.value)
    if (overviewRes.code === 0 && overviewRes.data) {
      overview.value = overviewRes.data
    }

    // 加载 Token 趋势（today=按小时，7d/30d=按天）
    const granularity = period.value === 'today' ? 'hour' : 'day'
    const trendRes = await getTokenTrend(period.value, granularity)
    if (trendRes.code === 0 && trendRes.data) {
      tokenTrend.value = trendRes.data.series || []
    }

    // 加载聚合图表（调用/错误趋势、费用、Agent 分布、错误类型）
    const chartsRes = await getMonitorCharts(period.value)
    if (chartsRes.code === 0 && chartsRes.data) {
      charts.value = chartsRes.data
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
          <UiIcon :name="overview.trends.callCountChange > 0 ? 'chevron-up' : 'chevron-down'" :size="13" /> {{ Math.abs(overview.trends.callCountChange * 100).toFixed(1) }}%
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
          <div v-else class="health-scroll">
            <div v-for="agent in agentHealth" :key="agent.agentId" class="health-item">
              <div class="health-header">
                <span class="health-name">{{ agent.agentName }}</span>
                <span class="health-rate" :style="{ color: healthColor(agent.status) }">{{ formatPercent(agent.successRate) }}</span>
              </div>
              <el-progress :percentage="agent.successRate != null ? Math.min(100, Math.max(0, agent.successRate)) : 0" :color="healthColor(agent.status)" :show-text="false" :stroke-width="6" />
              <div class="health-meta text-muted">{{ formatLatency(agent.avgLatencyMs) }} · {{ agent.callCount24h }} 次/24h</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区：调用/错误趋势 + 费用趋势 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="mb-24">
          <template #header><span>调用与错误趋势</span></template>
          <div class="dual-chart">
            <div v-for="(p, idx) in charts.callTrend" :key="p.time" class="dual-col">
              <div class="bar-pair">
                <div class="bar-seg seg-calls" :style="{ height: (p.calls / maxCall * 110) + 'px' }" :title="`${p.time} 成功 ${p.calls} 次`"></div>
                <div class="bar-seg seg-errors" :style="{ height: (p.errors / maxCall * 110) + 'px' }" :title="`${p.time} 失败 ${p.errors} 次`"></div>
              </div>
              <div class="bar-label" :class="{ hidden: idx % labelEvery(charts.callTrend.length) !== 0 }">{{ p.time }}</div>
            </div>
          </div>
          <div class="chart-legend">
            <span class="legend-item"><span class="legend-dot seg-calls"></span> 成功调用</span>
            <span class="legend-item"><span class="legend-dot seg-errors"></span> 失败</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="mb-24">
          <template #header>
            <div class="card-head"><span>费用趋势</span><span class="head-total">合计 {{ formatCost(costTotal) }}</span></div>
          </template>
          <div class="cost-chart">
            <div v-for="(p, idx) in charts.costTrend" :key="p.time" class="cost-col">
              <div class="cost-bar" :style="{ height: Math.max(p.cost / maxCost * 110, 2) + 'px' }" :title="`${p.time} ¥${(p.cost || 0).toFixed(4)}`"></div>
              <div class="bar-label" :class="{ hidden: idx % labelEvery(charts.costTrend.length) !== 0 }">{{ p.time }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区：Agent 调用分布 + 错误类型分布 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card class="mb-24">
          <template #header><span>Agent 调用分布 TOP5</span></template>
          <div v-if="charts.agentDistribution.length === 0" class="text-muted" style="text-align:center;padding:20px">暂无数据</div>
          <div v-for="a in charts.agentDistribution" :key="a.agentId" class="dist-item">
            <span class="dist-name" :title="a.agentName">{{ a.agentName }}</span>
            <div class="dist-track">
              <div class="dist-fill" :style="{ width: (a.calls / maxAgentCalls * 100) + '%' }"></div>
            </div>
            <span class="dist-val">{{ a.calls }} 次 · {{ formatTokens(a.tokens) }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="mb-24">
          <template #header><span>错误类型分布</span></template>
          <div v-if="errorTotal === 0" class="text-muted" style="text-align:center;padding:20px">窗口内暂无错误</div>
          <div v-else class="donut-wrap">
            <div class="donut" :style="donutStyle">
              <div class="donut-hole">
                <div class="donut-total">{{ errorTotal }}</div>
                <div class="donut-cap">错误总数</div>
              </div>
            </div>
            <div class="donut-legend">
              <div v-for="(e, i) in charts.errorTypeDistribution" :key="e.errorType" class="legend-row">
                <span class="legend-swatch" :style="{ background: donutColor(i) }"></span>
                <span class="legend-type">{{ errorTypeLabel(e.errorType) }}</span>
                <span class="legend-count">{{ e.count }} 次</span>
              </div>
            </div>
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
.monitor-page { max-width: 1200px; margin: 0 auto; }
/* 6 个指标卡固定一行，窄屏逐级降为 3/2 列，避免第 6 张孤行 */
.monitor-page .stats-row { grid-template-columns: repeat(6, 1fr); }
@media (max-width: 1200px) {
  .monitor-page .stats-row { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 820px) {
  .monitor-page .stats-row { grid-template-columns: repeat(2, 1fr); }
}
.metric-card { text-align: center; }
.metric-val { font-size: 28px; font-weight: 700; color: var(--el-color-primary); }
.metric-lbl { color: var(--text-3); font-size: 13px; margin-top: 2px; }
.metric-trend { font-size: 12px; margin-top: 4px; }
.metric-trend.up { color: var(--st-running); }
.metric-trend.down { color: var(--st-danger); }
.bar-chart { display: flex; align-items: flex-end; gap: 4px; height: 140px; padding: 0 8px; }
.bar-col { display: flex; flex-direction: column; align-items: center; gap: 2px; flex: 1; }
.bar-input { width: 100%; background: var(--el-color-primary); border-radius: 2px 2px 0 0; min-height: 2px; }
.bar-output { width: 100%; background: var(--el-color-primary-light-5); border-radius: 2px 2px 0 0; min-height: 2px; }
.bar-label { font-size: 10px; color: var(--text-3); }
.chart-legend { display: flex; gap: 16px; justify-content: center; margin-top: 12px; font-size: 12px; }
.legend-item { display: flex; align-items: center; gap: 4px; }
.legend-dot { width: 10px; height: 10px; border-radius: 2px; }
.legend-dot.input { background: var(--el-color-primary); }
.legend-dot.output { background: var(--el-color-primary-light-5); }
/* 健康排行固定高度，超出内部滚动，卡片不再被 Agent 数量拉长 */
.health-scroll { height: 238px; overflow-y: auto; padding-right: 6px; }
.health-scroll::-webkit-scrollbar { width: 6px; }
.health-scroll::-webkit-scrollbar-thumb { background: var(--border-1, #e8e9ee); border-radius: 999px; }
.health-scroll::-webkit-scrollbar-thumb:hover { background: var(--text-3, #9295a0); }
.health-scroll::-webkit-scrollbar-track { background: transparent; }
.health-item { padding: 10px 0; border-bottom: 1px solid var(--el-border-color-lighter); }
.health-item:last-child { border-bottom: none; }
.health-header { display: flex; justify-content: space-between; margin-bottom: 4px; }
.health-name { font-size: 13px; font-weight: 500; }
.health-rate { font-size: 13px; font-weight: 600; }
.health-meta { font-size: 12px; margin-top: 4px; }
.alert-item { display: flex; align-items: center; gap: 8px; padding: 8px 0; border-bottom: 1px solid var(--el-border-color-lighter); }
.alert-item:last-child { border-bottom: none; }
.alert-msg { flex: 1; font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
/* ===== 聚合图表 ===== */
.card-head { display: flex; justify-content: space-between; align-items: center; }
.head-total { font-size: 12px; font-weight: 600; color: var(--text-2); font-variant-numeric: tabular-nums; }
.bar-label { font-size: 10px; color: var(--text-3); white-space: nowrap; }
.bar-label.hidden { visibility: hidden; }
/* 调用/错误双柱 */
.dual-chart { display: flex; align-items: flex-end; gap: 6px; height: 134px; padding: 0 4px; }
.dual-col { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px; min-width: 0; }
.bar-pair { display: flex; align-items: flex-end; justify-content: center; gap: 3px; height: 112px; width: 100%; }
.bar-seg { flex: 0 1 10px; max-width: 10px; border-radius: 2px 2px 0 0; min-height: 2px; }
.seg-calls { background: var(--el-color-primary); }
.seg-errors { background: var(--st-danger, #cf3f4f); }
/* 费用单柱 */
.cost-chart { display: flex; align-items: flex-end; gap: 6px; height: 134px; padding: 0 4px; }
.cost-col { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px; min-width: 0; }
.cost-bar { width: 100%; max-width: 22px; background: #b3730f; opacity: 0.85; border-radius: 3px 3px 0 0; min-height: 2px; }
/* Agent 调用分布横条 */
.dist-item { display: flex; align-items: center; gap: 10px; padding: 8px 0; }
.dist-name { width: 110px; font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.dist-track { flex: 1; height: 10px; background: rgba(22, 22, 29, 0.06); border-radius: 999px; overflow: hidden; }
.dist-fill { height: 100%; border-radius: 999px; background: var(--el-color-primary); transition: width 0.4s ease; }
.dist-val { width: 128px; text-align: right; font-size: 12px; color: var(--text-2); font-variant-numeric: tabular-nums; white-space: nowrap; }
/* 错误类型环图 */
.donut-wrap { display: flex; align-items: center; gap: 32px; justify-content: center; padding: 6px 0; }
.donut { width: 132px; height: 132px; border-radius: 50%; position: relative; flex-shrink: 0; }
.donut-hole { position: absolute; left: 50%; top: 50%; transform: translate(-50%, -50%); width: 78px; height: 78px; background: #fff; border-radius: 50%; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.donut-total { font-size: 20px; font-weight: 700; color: var(--text-1); font-variant-numeric: tabular-nums; }
.donut-cap { font-size: 11px; color: var(--text-3); }
.donut-legend { display: flex; flex-direction: column; gap: 8px; }
.legend-row { display: flex; align-items: center; gap: 8px; font-size: 12.5px; }
.legend-swatch { width: 10px; height: 10px; border-radius: 3px; flex-shrink: 0; }
.legend-type { color: var(--text-2); }
.legend-count { color: var(--text-1); font-weight: 600; font-variant-numeric: tabular-nums; }
</style>
