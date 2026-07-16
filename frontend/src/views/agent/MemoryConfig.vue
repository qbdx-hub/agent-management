<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useAgentStore } from '@/stores/agent'
import { updateMemoryConfig } from '@/api/agent'
import { listKnowledgeBases } from '@/api/knowledge'
import { MEMORY_STRATEGY_MAP } from '@/utils/constants'
import { ElMessage } from 'element-plus'

const agentStore = useAgentStore()
const agent = computed(() => agentStore.current)
const loading = ref(false)

const config = ref({
  workingWindow: 20,
  shortTermStrategy: 'summary',
  longTermEnabled: true,
  knowledgeBaseIds: [] as number[],
})

const knowledgeBases = ref<{ id: number; name: string }[]>([])

onMounted(async () => {
  // 加载知识库列表
  try {
    const res = await listKnowledgeBases()
    if (res.code === 0 && res.data) {
      knowledgeBases.value = (res.data || []).map((kb: any) => ({ id: kb.id, name: kb.name }))
    }
  } catch { /* ignore */ }
  initForm()
})

watch(agent, () => { initForm() }, { deep: true })

function initForm() {
  if (!agent.value) return
  const mem = agent.value.config.memory
  config.value = {
    workingWindow: mem.workingWindow ?? 20,
    shortTermStrategy: mem.shortTermStrategy ?? 'summary',
    longTermEnabled: mem.longTermEnabled ?? true,
    knowledgeBaseIds: mem.knowledgeBaseIds ?? [],
  }
}

async function handleSave() {
  if (!agent.value) return
  loading.value = true
  try {
    const res = await updateMemoryConfig(agent.value.id, {
      workingWindow: config.value.workingWindow,
      memoryStrategy: config.value.shortTermStrategy,
      longTermEnabled: config.value.longTermEnabled ? 1 : 0,
      knowledgeBaseIds: config.value.knowledgeBaseIds,
    })
    if (res.code === 0) {
      await agentStore.fetchAgentDetail(agent.value.id)
      ElMessage.success('记忆配置已保存')
    }
  } catch { ElMessage.error('保存失败') } finally { loading.value = false }
}
</script>

<template>
  <div class="memory-config" v-if="agent">
    <el-form label-width="140px" style="max-width:600px">
      <el-form-item label="工作记忆窗口">
        <el-slider v-model="config.workingWindow" :min="1" :max="50" show-input style="width:400px" />
        <div class="text-muted" style="font-size:12px">保留最近多少轮对话作为上下文</div>
      </el-form-item>

      <el-form-item label="短期记忆策略">
        <el-radio-group v-model="config.shortTermStrategy">
          <el-radio v-for="(label, key) in MEMORY_STRATEGY_MAP" :key="key" :value="key">{{ label }}</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="长期记忆">
        <el-switch v-model="config.longTermEnabled" />
        <span style="margin-left:12px;font-size:13px">{{ config.longTermEnabled ? '已启用' : '未启用' }}</span>
      </el-form-item>

      <el-form-item v-if="config.longTermEnabled" label="绑定知识库">
        <el-select v-model="config.knowledgeBaseIds" multiple placeholder="选择知识库" style="width:100%">
          <el-option v-for="kb in knowledgeBases" :key="kb.id" :label="kb.name" :value="kb.id" />
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleSave">保存配置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped>.memory-config { padding: 20px 0; }</style>
