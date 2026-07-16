<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useAgentStore } from '@/stores/agent'
import { getToolList } from '@/api/tool'
import { updateToolBindings } from '@/api/agent'
import { ElMessage } from 'element-plus'
import ToolIcon from '@/components/ToolIcon.vue'
import type { ToolSummary } from '@/types/tool'

const agentStore = useAgentStore()
const agent = computed(() => agentStore.current)
const loading = ref(false)
const allTools = ref<ToolSummary[]>([])
const boundToolIds = ref<number[]>([])

onMounted(async () => {
  // 加载真实工具列表
  try {
    const res = await getToolList({ page: 1, pageSize: 100 })
    if (res.code === 0 && res.data) {
      allTools.value = res.data.list || []
    }
  } catch { /* ignore */ }
  initBound()
})

watch(agent, () => { initBound() }, { deep: true })

function initBound() {
  if (agent.value?.config?.boundTools) {
    boundToolIds.value = agent.value.config.boundTools.filter(t => t.enabled).map(t => t.toolId)
  }
}

const isBound = (id: number) => boundToolIds.value.includes(id)

function toggleTool(id: number) {
  if (isBound(id)) {
    boundToolIds.value = boundToolIds.value.filter(tid => tid !== id)
  } else {
    boundToolIds.value.push(id)
  }
}

async function handleSave() {
  if (!agent.value) return
  loading.value = true
  try {
    const tools = allTools.value.map(t => ({
      toolId: t.id,
      enabled: boundToolIds.value.includes(t.id),
    }))
    const res = await updateToolBindings(agent.value.id, tools)
    if (res.code === 0) {
      await agentStore.fetchAgentDetail(agent.value.id)
      ElMessage.success('工具绑定已保存')
    }
  } catch { ElMessage.error('保存失败') } finally { loading.value = false }
}
</script>

<template>
  <div class="tool-binding" v-if="agent">
    <div class="binding-header">
      <span>选择要绑定到此 Agent 的工具（已选 {{ boundToolIds.length }} 个）</span>
      <el-button type="primary" size="small" :loading="loading" @click="handleSave">保存绑定</el-button>
    </div>
    <el-row :gutter="16">
      <el-col v-for="tool in allTools" :key="tool.id" :span="8">
        <el-card shadow="hover" class="tool-bind-card" :class="{ bound: isBound(tool.id) }" @click="toggleTool(tool.id)">
          <div class="tool-bind-header">
            <ToolIcon :icon="tool.icon" :size="24" />
            <div>
              <div class="tool-name">{{ tool.displayName }}</div>
              <div class="text-muted" style="font-size:12px">{{ tool.categoryLabel }}</div>
            </div>
            <el-icon v-if="isBound(tool.id)" class="bind-check" color="#67c23a"><CircleCheckFilled /></el-icon>
          </div>
          <div class="tool-desc text-muted">{{ tool.description }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="allTools.length === 0" description="暂无可用工具" />
  </div>
</template>

<style scoped>
.tool-binding { padding: 20px 0; }
.binding-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.tool-bind-card { cursor: pointer; transition: all 0.2s; margin-bottom: 16px; }
.tool-bind-card.bound { border-color: var(--el-color-success); background: var(--el-color-success-light-9); }
.tool-bind-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.tool-name { font-weight: 600; font-size: 14px; }
.bind-check { margin-left: auto; font-size: 20px; }
.tool-desc { font-size: 12px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
</style>
