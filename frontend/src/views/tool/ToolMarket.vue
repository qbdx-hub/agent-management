<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getToolList } from '@/api/tool'
import { TOOL_CATEGORY_MAP } from '@/utils/constants'
import { formatPercent, formatLatency, formatNumber } from '@/utils/format'
import type { ToolSummary } from '@/types/tool'
import ToolIcon from '@/components/ToolIcon.vue'
import { I as Ic } from '@/components/icons'

const router = useRouter()
const tools = ref<ToolSummary[]>([])
const activeCategory = ref('all')
const keyword = ref('')

onMounted(async () => {
  const res = await getToolList({ page: 1, pageSize: 100 })
  if (res.code === 0) tools.value = res.data.list
})

const filteredTools = computed(() => {
  let list = tools.value
  if (activeCategory.value !== 'all') list = list.filter(t => t.category === activeCategory.value)
  if (keyword.value) list = list.filter(t => t.displayName.includes(keyword.value) || t.description.includes(keyword.value))
  return list
})

const categories = computed(() => {
  return Object.keys(TOOL_CATEGORY_MAP) as (keyof typeof TOOL_CATEGORY_MAP)[]
})
</script>

<template>
  <div class="tool-market-page">
    <div class="page-header">
      <h2>工具市场</h2>
      <el-button type="primary" @click="router.push('/tools/register')"><UiIcon name="plus" />注册工具</el-button>
    </div>

    <!-- 搜索 + 分类：直接落在页面上，不再包卡片 -->
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索工具..." style="width: 250px" clearable :prefix-icon="Ic.search" />
      <el-radio-group v-model="activeCategory" size="small">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button v-for="cat in categories" :key="cat" :value="cat">{{ TOOL_CATEGORY_MAP[cat] }}</el-radio-button>
      </el-radio-group>
    </div>

    <div class="card-grid">
      <el-card v-for="tool in filteredTools" :key="tool.id" shadow="never" class="tool-card" @click="router.push(`/tools/${tool.id}`)">
        <div class="tool-card-header">
          <ToolIcon :icon="tool.icon" :size="28" />
          <div class="tool-info">
            <div class="tool-name">{{ tool.displayName }}</div>
            <div class="tool-tags">
              <el-tag size="small" type="info" round>{{ tool.categoryLabel }}</el-tag>
              <el-tag v-if="tool.type === 'mcp'" size="small" type="warning" round>MCP</el-tag>
              <el-tag v-if="tool.type === 'builtin'" size="small" type="success" round>内置</el-tag>
            </div>
          </div>
        </div>
        <div class="tool-desc">{{ tool.description }}</div>
        <div class="tool-meta">
          <span><span class="num">{{ tool.bindAgentCount }}</span> Agent</span>
          <span><span class="num">{{ formatNumber(tool.totalCalls) }}</span> 次调用</span>
          <span class="num" :class="{ muted: tool.successRate == null }">{{ formatPercent(tool.successRate) }}</span>
          <span class="num">{{ formatLatency(tool.avgLatencyMs) }}</span>
        </div>
      </el-card>
    </div>
    <el-empty v-if="filteredTools.length === 0" description="没有找到匹配的工具" />
  </div>
</template>

<style scoped>
.tool-market-page { max-width: 1400px; margin: 0 auto; }
.page-header h2 {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.2px;
}
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}
.tool-card {
  cursor: pointer;
  transition: transform 0.15s ease-out, box-shadow 0.15s ease-out, border-color 0.15s ease-out;
}
.tool-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lift);
}
.tool-card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.tool-info { flex: 1; min-width: 0; }
.tool-name { font-weight: 700; font-size: 14.5px; color: var(--text-1); }
.tool-tags { display: flex; gap: 4px; margin-top: 3px; }
.tool-desc {
  font-size: 12.5px;
  color: var(--text-2);
  margin-bottom: 14px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 38px;
}
.tool-meta {
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: var(--text-2);
  flex-wrap: wrap;
}
.tool-meta .num { font-size: 12px; }
.muted { color: var(--text-3); font-weight: 500; }
@media (prefers-reduced-motion: reduce) {
  .tool-card { transition: none; }
  .tool-card:hover { transform: none; }
}
</style>
