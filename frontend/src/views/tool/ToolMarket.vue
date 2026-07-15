<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getToolList } from '@/api/tool'
import { TOOL_CATEGORY_MAP } from '@/utils/constants'
import { formatPercent, formatLatency } from '@/utils/format'
import type { ToolSummary } from '@/types/tool'
import ToolIcon from '@/components/ToolIcon.vue'

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
      <el-button type="primary" @click="router.push('/tools/register')"><el-icon><Plus /></el-icon> 注册工具</el-button>
    </div>

    <el-card class="mb-16">
      <div class="filter-bar">
        <el-input v-model="keyword" placeholder="搜索工具..." style="width:250px" clearable prefix-icon="Search" />
        <el-radio-group v-model="activeCategory" size="small">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button v-for="cat in categories" :key="cat" :value="cat">{{ TOOL_CATEGORY_MAP[cat] }}</el-radio-button>
        </el-radio-group>
      </div>
    </el-card>

    <div class="card-grid">
      <el-card v-for="tool in filteredTools" :key="tool.id" shadow="hover" class="tool-card" @click="router.push(`/tools/${tool.id}`)">
        <div class="tool-card-header">
          <ToolIcon :icon="tool.icon" :size="28" />
          <div class="tool-info">
            <div class="tool-name">{{ tool.displayName }}</div>
            <el-tag size="small" type="info">{{ tool.categoryLabel }}</el-tag>
            <el-tag v-if="tool.type === 'mcp'" size="small" type="warning" style="margin-left:4px">MCP</el-tag>
          </div>
        </div>
        <div class="tool-desc text-muted">{{ tool.description }}</div>
        <div class="tool-meta">
          <span><el-icon class="ii"><Cpu /></el-icon>{{ tool.bindAgentCount }} Agent</span>
          <span><el-icon class="ii"><Phone /></el-icon>{{ tool.totalCalls }} 次调用</span>
          <span><el-icon class="ii"><CircleCheck /></el-icon>{{ formatPercent(tool.successRate) }}</span>
          <span><el-icon class="ii"><Timer /></el-icon>{{ formatLatency(tool.avgLatencyMs) }}</span>
        </div>
      </el-card>
    </div>
    <el-empty v-if="filteredTools.length === 0" description="没有找到匹配的工具" />
  </div>
</template>

<style scoped>
.tool-market-page { max-width: 1400px; }
.tool-card { cursor: pointer; transition: all 0.2s; }
.tool-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.tool-card-header { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.tool-icon { font-size: 28px; }
.tool-name { font-weight: 600; font-size: 15px; }
.tool-desc { font-size: 13px; margin-bottom: 12px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.tool-meta { display: flex; gap: 12px; font-size: 12px; color: #909399; flex-wrap: wrap; }
</style>
