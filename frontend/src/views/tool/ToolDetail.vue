<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { mockToolDetail, mockToolTestSuccess, mockToolStats } from '@/mock/tools'
import { TOOL_CATEGORY_MAP } from '@/utils/constants'
import { formatPercent, formatLatency, formatDateTime } from '@/utils/format'
import type { ToolDetail as ToolDetailType, ToolTestResult, ToolStats } from '@/types/tool'

const route = useRoute()
const router = useRouter()
const tool = ref<ToolDetailType | null>(null)
const testResult = ref<ToolTestResult | null>(null)
const stats = ref<ToolStats | null>(null)
const activeTab = ref('info')
const testParams = ref<Record<string, any>>({})
const testing = ref(false)

onMounted(() => {
  tool.value = { ...mockToolDetail, id: Number(route.params.id) }
  stats.value = { ...mockToolStats }
  tool.value.parameters.forEach(p => { testParams.value[p.name] = p.defaultValue || '' })
})

async function runTest() {
  testing.value = true
  setTimeout(() => {
    testResult.value = { ...mockToolTestSuccess }
    testing.value = false
  }, 1500)
}
</script>

<template>
  <div class="tool-detail-page" v-if="tool">
    <div class="page-header">
      <div style="display:flex;align-items:center;gap:12px">
        <span style="font-size:32px">{{ tool.icon }}</span>
        <div>
          <h2 style="margin:0">{{ tool.displayName }}</h2>
          <div class="text-muted">{{ tool.name }} · {{ tool.categoryLabel }} · {{ tool.type.toUpperCase() }}</div>
        </div>
      </div>
      <el-button @click="router.push('/tools')">返回列表</el-button>
    </div>

    <el-card>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="信息" name="info">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="名称">{{ tool.displayName }}</el-descriptions-item>
            <el-descriptions-item label="标识">{{ tool.name }}</el-descriptions-item>
            <el-descriptions-item label="分类">{{ tool.categoryLabel }}</el-descriptions-item>
            <el-descriptions-item label="类型">{{ tool.type }}</el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ tool.description }}</el-descriptions-item>
            <el-descriptions-item label="URL" :span="2"><code>{{ tool.endpoint?.url }}</code></el-descriptions-item>
            <el-descriptions-item label="Method">{{ tool.endpoint?.method }}</el-descriptions-item>
            <el-descriptions-item label="超时">{{ tool.endpoint?.timeoutMs }}ms</el-descriptions-item>
            <el-descriptions-item label="绑定 Agent">{{ tool.bindAgentCount }}</el-descriptions-item>
            <el-descriptions-item label="总调用">{{ tool.totalCalls }}</el-descriptions-item>
          </el-descriptions>

          <h4 style="margin:20px 0 12px">参数定义</h4>
          <el-table :data="tool.parameters" border size="small">
            <el-table-column prop="name" label="名称" width="150" />
            <el-table-column prop="type" label="类型" width="100" />
            <el-table-column label="必填" width="70"><template #default="{ row }">{{ row.required ? '是' : '否' }}</template></el-table-column>
            <el-table-column prop="description" label="描述" />
            <el-table-column prop="defaultValue" label="默认值" width="120" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="测试" name="test">
          <el-row :gutter="20">
            <el-col :span="12">
              <h4>参数</h4>
              <el-form label-width="100px">
                <el-form-item v-for="p in tool.parameters" :key="p.name" :label="p.name">
                  <el-input v-model="testParams[p.name]" :placeholder="p.description" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" :loading="testing" @click="runTest">执行测试</el-button>
                </el-form-item>
              </el-form>
            </el-col>
            <el-col :span="12">
              <h4>结果</h4>
              <div v-if="testResult">
                <el-descriptions :column="1" border size="small">
                  <el-descriptions-item label="状态"><el-tag :type="testResult.success ? 'success' : 'danger'" size="small">{{ testResult.success ? '成功' : '失败' }}</el-tag></el-descriptions-item>
                  <el-descriptions-item label="延迟">{{ formatLatency(testResult.latencyMs) }}</el-descriptions-item>
                  <el-descriptions-item label="HTTP">{{ testResult.responseStatus }}</el-descriptions-item>
                </el-descriptions>
                <h4 style="margin:12px 0 8px">响应体</h4>
                <pre class="response-block">{{ testResult.responseBody }}</pre>
                <h4 style="margin:12px 0 8px">映射输出</h4>
                <pre class="response-block">{{ testResult.mappedOutput }}</pre>
              </div>
              <el-empty v-else description="点击执行测试查看结果" />
            </el-col>
          </el-row>
        </el-tab-pane>

        <el-tab-pane label="统计" name="stats">
          <div v-if="stats" class="stats-row">
            <el-card shadow="hover"><div class="stat-val">{{ stats.totalCalls }}</div><div class="stat-lbl">总调用</div></el-card>
            <el-card shadow="hover"><div class="stat-val">{{ formatPercent(stats.successRate) }}</div><div class="stat-lbl">成功率</div></el-card>
            <el-card shadow="hover"><div class="stat-val">{{ formatLatency(stats.avgLatencyMs) }}</div><div class="stat-lbl">平均延迟</div></el-card>
            <el-card shadow="hover"><div class="stat-val">{{ formatLatency(stats.p99LatencyMs) }}</div><div class="stat-lbl">P99 延迟</div></el-card>
          </div>
          <h4 style="margin:16px 0 12px">绑定的 Agent</h4>
          <el-table v-if="stats" :data="stats.topAgents" border size="small">
            <el-table-column prop="agentName" label="Agent" />
            <el-table-column prop="callCount" label="调用次数" width="120" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="最近调用" name="recent">
          <el-table :data="tool.recentCalls" border size="small">
            <el-table-column label="时间" width="160"><template #default="{ row }">{{ formatDateTime(row.time) }}</template></el-table-column>
            <el-table-column label="参数"><template #default="{ row }"><code>{{ JSON.stringify(row.params) }}</code></template></el-table-column>
            <el-table-column prop="resultSummary" label="结果" width="150" />
            <el-table-column label="状态" width="80"><template #default="{ row }"><el-tag :type="row.success ? 'success' : 'danger'" size="small">{{ row.success ? '成功' : '失败' }}</el-tag></template></el-table-column>
            <el-table-column label="延迟" width="80"><template #default="{ row }">{{ formatLatency(row.latencyMs) }}</template></el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.tool-detail-page { max-width: 1200px; }
.response-block { background: #f5f7fa; padding: 12px; border-radius: 6px; font-size: 13px; overflow-x: auto; white-space: pre-wrap; word-break: break-all; }
.stat-val { font-size: 28px; font-weight: 700; color: var(--el-color-primary); text-align: center; }
.stat-lbl { text-align: center; color: #909399; font-size: 13px; margin-top: 4px; }
</style>
