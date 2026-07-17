<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCostOverview, getCostBreakdown, getCostTrend, getCostRecords } from '@/api/cost'
import { formatCost, formatPercent, formatTokens, formatDateTime } from '@/utils/format'
import type { CostOverview, CostBreakdownItem, CostTrendPoint, CostRecord } from '@/types/cost'

const router = useRouter()

const overview = ref<CostOverview>({
  totalCost: 0, budgetLimit: 0, budgetRemaining: 0,
  budgetPercent: 0, todayCost: 0, yesterdayCost: 0,
  projectedMonthCost: 0, meltdownStatus: 'normal',
})
const byModel = ref<CostBreakdownItem[]>([])
const byAgent = ref<CostBreakdownItem[]>([])
const trend = ref<CostTrendPoint[]>([])
const records = ref<CostRecord[]>([])
const loading = ref(false)

function barColor(percent: number) {
  if (percent > 90) return '#f56c6c'
  if (percent > 70) return '#e6a23c'
  return '#67c23a'
}

// 计算趋势柱状图高度
const maxCost = computed(() => {
  if (trend.value.length === 0) return 0
  return Math.max(...trend.value.map(p => p.cost))
})

function getBarHeight(cost: number): string {
  const minHeight = 8  // 最小高度，确保可见
  const maxHeight = 100 // 最大高度（百分比）
  const chartHeight = 120 // 图表总高度 px
  const minPx = minHeight
  const maxPx = chartHeight - 20 // 留出标签空间

  if (cost <= 0) return minPx + 'px'
  if (maxCost.value <= 0) return minPx + 'px'

  // 使用对数缩放，让小值也能有明显差异
  const ratio = cost / maxCost.value
  // 线性映射到 minPx-maxPx 范围
  const height = Math.max(minPx, Math.min(maxPx, ratio * maxPx))

  return Math.round(height) + 'px'
}

async function loadData() {
  loading.value = true
  try {
    const [overviewRes, modelRes, agentRes, trendRes, recordsRes] = await Promise.all([
      getCostOverview('this_month'),
      getCostBreakdown('model', 'this_month'),
      getCostBreakdown('agent', 'this_month'),
      getCostTrend('30d', 'day'),
      getCostRecords({ page: 1, pageSize: 20 }),
    ])
    overview.value = overviewRes.data ?? overview.value
    byModel.value = modelRes.data ?? []
    byAgent.value = agentRes.data ?? []
    trend.value = trendRes.data ?? []
    records.value = recordsRes.data?.list ?? []
  } catch (e: any) {
    ElMessage.error(e.message || '加载成本数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="cost-page">
    <div class="page-header">
      <h2>成本管理</h2>
      <el-button @click="router.push('/cost/budget')">预算配置</el-button>
    </div>

    <!-- 概览卡片 -->
    <div class="stats-row" v-loading="loading">
      <el-card shadow="hover" class="cost-card"><div class="cost-val">{{ formatCost(overview.totalCost) }}</div><div class="cost-lbl">本月总花费</div></el-card>
      <el-card shadow="hover" class="cost-card"><div class="cost-val">{{ formatCost(overview.budgetRemaining) }}</div><div class="cost-lbl">预算剩余</div></el-card>
      <el-card shadow="hover" class="cost-card"><div class="cost-val">{{ formatCost(overview.todayCost) }}</div><div class="cost-lbl">今日花费</div></el-card>
      <el-card shadow="hover" class="cost-card"><div class="cost-val">{{ formatCost(overview.projectedMonthCost) }}</div><div class="cost-lbl">预计月底</div></el-card>
    </div>

    <!-- 预算进度 -->
    <el-card class="mb-24">
      <div style="display:flex;align-items:center;gap:16px">
        <span style="white-space:nowrap">预算使用</span>
        <el-progress :percentage="Number(overview.budgetPercent)" :color="barColor(Number(overview.budgetPercent))" style="flex:1" />
        <span class="text-muted">{{ formatCost(overview.totalCost) }} / {{ formatCost(overview.budgetLimit) }}</span>
      </div>
    </el-card>

    <el-row :gutter="20">
      <!-- 按模型 -->
      <el-col :span="8">
        <el-card class="mb-24">
          <template #header><span>按模型拆分</span></template>
          <el-empty v-if="byModel.length === 0" description="暂无数据" :image-size="48" />
          <div v-for="item in byModel" :key="item.label" class="breakdown-item">
            <div class="breakdown-header"><span>{{ item.label }}</span><span>{{ formatCost(item.cost) }}</span></div>
            <el-progress :percentage="Number(item.percent)" :show-text="false" :stroke-width="8" />
            <div class="text-muted" style="font-size:12px">{{ formatPercent(Number(item.percent) / 100) }} · {{ formatTokens(item.tokenInput + item.tokenOutput) }} tokens</div>
          </div>
        </el-card>
      </el-col>

      <!-- 按 Agent -->
      <el-col :span="8">
        <el-card class="mb-24">
          <template #header><span>按 Agent 拆分</span></template>
          <el-empty v-if="byAgent.length === 0" description="暂无数据" :image-size="48" />
          <div v-for="item in byAgent" :key="item.label" class="breakdown-item">
            <div class="breakdown-header"><span>{{ item.label }}</span><span>{{ formatCost(item.cost) }}</span></div>
            <el-progress :percentage="Number(item.percent)" :show-text="false" :stroke-width="8" color="#409eff" />
            <div class="text-muted" style="font-size:12px">{{ formatPercent(Number(item.percent) / 100) }}</div>
          </div>
        </el-card>
      </el-col>

      <!-- 趋势 -->
      <el-col :span="8">
        <el-card class="mb-24">
          <template #header><span>30天趋势</span></template>
          <el-empty v-if="trend.length === 0" description="暂无数据" :image-size="48" />
          <div v-else class="trend-chart">
            <div v-for="(point, idx) in trend.slice(-14)" :key="idx" class="trend-bar-col">
              <div
                class="trend-bar"
                :class="{ 'trend-bar--has-value': point.cost > 0 }"
                :style="{ height: getBarHeight(point.cost) }"
                :title="formatCost(point.cost)"
              ></div>
              <div class="trend-label">{{ point.date.split('-')[2] }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 消费明细 -->
    <el-card>
      <template #header><span>消费明细</span></template>
      <el-table :data="records" size="small" empty-text="暂无消费记录">
        <el-table-column prop="agentName" label="Agent" width="140" />
        <el-table-column prop="modelName" label="模型" width="150" />
        <el-table-column label="Input" width="100"><template #default="{ row }">{{ formatTokens(row.tokenInput) }}</template></el-table-column>
        <el-table-column label="Output" width="100"><template #default="{ row }">{{ formatTokens(row.tokenOutput) }}</template></el-table-column>
        <el-table-column label="费用" width="80"><template #default="{ row }">{{ formatCost(row.cost) }}</template></el-table-column>
        <el-table-column prop="userName" label="用户" width="80" />
        <el-table-column label="时间" width="160"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.cost-page { max-width: 1400px; }
.cost-card { text-align: center; }
.cost-val { font-size: 28px; font-weight: 700; color: var(--el-color-primary); }
.cost-lbl { color: #909399; font-size: 13px; margin-top: 4px; }
.breakdown-item { padding: 10px 0; border-bottom: 1px solid var(--el-border-color-lighter); }
.breakdown-item:last-child { border-bottom: none; }
.breakdown-header { display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 4px; }
.trend-chart { display: flex; align-items: flex-end; gap: 4px; height: 120px; padding: 4px 0; }
.trend-bar-col { display: flex; flex-direction: column; align-items: center; gap: 2px; flex: 1; }
.trend-bar {
  width: 100%;
  background: var(--el-border-color-lighter);
  border-radius: 3px 3px 0 0;
  min-height: 8px;
  transition: all 0.3s ease;
}
.trend-bar--has-value {
  background: linear-gradient(180deg, var(--el-color-primary) 0%, var(--el-color-primary-light-3) 100%);
  box-shadow: 0 -2px 4px rgba(64, 158, 255, 0.2);
}
.trend-bar:hover {
  opacity: 0.85;
  transform: scaleY(1.05);
  transform-origin: bottom;
}
.trend-label { font-size: 10px; color: #909399; }
</style>
