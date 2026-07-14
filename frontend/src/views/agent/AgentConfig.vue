<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useAgentStore } from '@/stores/agent'
import { mockModelProviders } from '@/mock/agents'
import { ElMessage } from 'element-plus'

const route = useRoute()
const agentStore = useAgentStore()
const agent = computed(() => agentStore.current)
const loading = ref(false)

const form = ref({
  name: '', description: '', avatar: '', tags: [] as string[],
  modelProvider: '', modelName: '', temperature: 0.7, maxTokens: 4096, topP: 0.95,
})

const selectedProvider = ref(mockModelProviders[0])

import { computed } from 'vue'

onMounted(() => { initForm() })
watch(agent, () => { initForm() }, { deep: true })

function initForm() {
  if (!agent.value) return
  const cfg = agent.value.config
  form.value = {
    name: agent.value.name, description: agent.value.description, avatar: agent.value.avatar, tags: [...agent.value.tags],
    modelProvider: cfg.modelProvider, modelName: cfg.modelName,
    temperature: cfg.temperature, maxTokens: cfg.maxTokens, topP: cfg.topP,
  }
  selectedProvider.value = mockModelProviders.find(p => p.key === cfg.modelProvider) || mockModelProviders[0]
}

function handleProviderChange(val: string) {
  selectedProvider.value = mockModelProviders.find(p => p.key === val) || mockModelProviders[0]
  form.value.modelName = ''
}

async function handleSave() {
  loading.value = true
  try {
    await agentStore.updateAgent(agent.value!.id, { name: form.value.name, description: form.value.description, avatar: form.value.avatar, tags: form.value.tags })
    await agentStore.fetchAgentDetail(agent.value!.id)
    ElMessage.success('保存成功')
  } catch { ElMessage.error('保存失败') } finally { loading.value = false }
}
</script>

<template>
  <div class="agent-config" v-if="agent">
    <el-form label-width="120px" style="max-width:700px">
      <el-divider content-position="left">基础信息</el-divider>
      <el-form-item label="名称"><el-input v-model="form.name" maxlength="50" /></el-form-item>
      <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" maxlength="200" /></el-form-item>
      <el-form-item label="头像"><el-input v-model="form.avatar" style="width:80px" /></el-form-item>
      <el-form-item label="标签">
        <div style="display:flex;gap:8px;flex-wrap:wrap">
          <el-tag v-for="tag in form.tags" :key="tag" closable @close="form.tags = form.tags.filter(t => t !== tag)">{{ tag }}</el-tag>
        </div>
      </el-form-item>

      <el-divider content-position="left">模型配置</el-divider>
      <el-form-item label="供应商">
        <el-select v-model="form.modelProvider" @change="handleProviderChange">
          <el-option v-for="p in mockModelProviders" :key="p.key" :label="p.name" :value="p.key" />
        </el-select>
      </el-form-item>
      <el-form-item label="模型">
        <el-select v-model="form.modelName">
          <el-option v-for="m in selectedProvider.models" :key="m.name" :label="m.displayName" :value="m.name" />
        </el-select>
      </el-form-item>
      <el-form-item label="Temperature"><el-slider v-model="form.temperature" :min="0" :max="2" :step="0.1" show-input style="width:400px" /></el-form-item>
      <el-form-item label="Max Tokens"><el-input-number v-model="form.maxTokens" :min="256" :max="128000" :step="256" /></el-form-item>
      <el-form-item label="Top P"><el-slider v-model="form.topP" :min="0" :max="1" :step="0.05" show-input style="width:400px" /></el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleSave">保存配置</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped>.agent-config { padding: 20px 0; }</style>
