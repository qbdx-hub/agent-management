<script setup lang="ts">
import { ref, computed } from 'vue'
import { useAgentStore } from '@/stores/agent'
import { MEMORY_STRATEGY_MAP } from '@/utils/constants'
import { ElMessage } from 'element-plus'

const agentStore = useAgentStore()
const agent = computed(() => agentStore.current)
const loading = ref(false)

const config = ref({
  workingWindow: agent.value?.config.memory.workingWindow ?? 20,
  shortTermStrategy: agent.value?.config.memory.shortTermStrategy ?? 'summary',
  longTermEnabled: agent.value?.config.memory.longTermEnabled ?? true,
  knowledgeBaseIds: agent.value?.config.memory.knowledgeBaseIds ?? [],
})

const mockKnowledgeBases = [
  { id: 1, name: 'Agent 业务知识库' },
  { id: 2, name: '技术架构文档' },
  { id: 3, name: 'API 接口规范' },
]

async function handleSave() {
  loading.value = true
  try {
    ElMessage.success('记忆配置已保存')
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
          <el-option v-for="kb in mockKnowledgeBases" :key="kb.id" :label="kb.name" :value="kb.id" />
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleSave">保存配置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped>.memory-config { padding: 20px 0; }</style>
