<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAgentStore } from '@/stores/agent'
import { mockModelProviders } from '@/mock/agents'
import { ElMessage } from 'element-plus'

const router = useRouter()
const agentStore = useAgentStore()
const currentStep = ref(0)
const loading = ref(false)

const form = reactive({
  name: '', description: '', avatar: '23-ai-robot', tags: [] as string[],
  modelProvider: '', modelName: '', temperature: 0.7, maxTokens: 4096,
})

const tagInput = ref('')
const avatarOptions = ['23-ai-robot', '07-lightbulb', '02-rocket', '24-trophy', '25-medal', '16-tea', '21-moon', '27-gift', '26-guitar', '19-camera', '03-bell', '14-globe']
const selectedProvider = ref(mockModelProviders[0])

function handleProviderChange(val: string) {
  selectedProvider.value = mockModelProviders.find(p => p.key === val) || mockModelProviders[0]
  form.modelName = ''
}

function addTag() {
  if (tagInput.value && !form.tags.includes(tagInput.value)) { form.tags.push(tagInput.value); tagInput.value = '' }
}
function removeTag(tag: string) { form.tags = form.tags.filter(t => t !== tag) }
function nextStep() {
  if (currentStep.value === 0 && !form.name.trim()) { ElMessage.warning('请输入 Agent 名称'); return }
  if (currentStep.value < 2) currentStep.value++
}
function prevStep() { if (currentStep.value > 0) currentStep.value-- }

async function handleFinish() {
  loading.value = true
  try {
    const id = await agentStore.createAgent({
      name: form.name, description: form.description, avatar: form.avatar, tags: form.tags, status: 'draft',
      modelProvider: form.modelProvider, modelName: form.modelName,
      temperature: form.temperature, maxTokens: form.maxTokens,
    })
    ElMessage.success('创建成功')
    router.push(`/agents/${id}`)
  } catch { ElMessage.error('创建失败') } finally { loading.value = false }
}
</script>

<template>
  <div class="agent-create-page">
    <div class="page-header"><h2>创建 Agent</h2><el-button @click="router.push('/agents')">返回列表</el-button></div>
    <el-card>
      <el-steps :active="currentStep" finish-status="success" style="margin-bottom:32px">
        <el-step title="基础信息" /><el-step title="模型配置" /><el-step title="确认创建" />
      </el-steps>

      <div v-show="currentStep === 0">
        <el-form label-width="100px" style="max-width:600px">
          <el-form-item label="头像">
            <div class="avatar-grid">
              <span v-for="a in avatarOptions" :key="a" class="avatar-option" :class="{ active: form.avatar === a }" @click="form.avatar = a"><AgentAvatar :avatar="a" :size="32" /></span>
            </div>
          </el-form-item>
          <el-form-item label="名称" required><el-input v-model="form.name" placeholder="例如：代码审查助手" maxlength="50" /></el-form-item>
          <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" placeholder="描述这个 Agent 的用途..." maxlength="200" /></el-form-item>
          <el-form-item label="标签">
            <div style="display:flex;gap:8px;flex-wrap:wrap;margin-bottom:8px"><el-tag v-for="tag in form.tags" :key="tag" closable @close="removeTag(tag)">{{ tag }}</el-tag></div>
            <el-input v-model="tagInput" size="small" placeholder="输入标签后回车" @keyup.enter="addTag" />
          </el-form-item>
        </el-form>
      </div>

      <div v-show="currentStep === 1">
        <el-form label-width="100px" style="max-width:600px">
          <el-form-item label="供应商"><el-select v-model="form.modelProvider" placeholder="选择供应商" @change="handleProviderChange"><el-option v-for="p in mockModelProviders" :key="p.key" :label="p.name" :value="p.key" /></el-select></el-form-item>
          <el-form-item label="模型"><el-select v-model="form.modelName" placeholder="选择模型"><el-option v-for="m in selectedProvider.models" :key="m.name" :label="m.displayName" :value="m.name" /></el-select></el-form-item>
          <el-form-item label="Temperature"><el-slider v-model="form.temperature" :min="0" :max="2" :step="0.1" show-input /></el-form-item>
          <el-form-item label="Max Tokens"><el-input-number v-model="form.maxTokens" :min="256" :max="128000" :step="256" /></el-form-item>
        </el-form>
      </div>

      <div v-show="currentStep === 2">
        <el-descriptions title="确认信息" :column="1" border>
          <el-descriptions-item label="头像"><AgentAvatar :avatar="form.avatar" :size="32" /></el-descriptions-item>
          <el-descriptions-item label="名称">{{ form.name }}</el-descriptions-item>
          <el-descriptions-item label="描述">{{ form.description || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="标签"><el-tag v-for="tag in form.tags" :key="tag" style="margin-right:4px">{{ tag }}</el-tag><span v-if="!form.tags.length" class="text-muted">无</span></el-descriptions-item>
          <el-descriptions-item label="模型">{{ selectedProvider.name }} / {{ form.modelName }}</el-descriptions-item>
          <el-descriptions-item label="Temperature">{{ form.temperature }}</el-descriptions-item>
          <el-descriptions-item label="Max Tokens">{{ form.maxTokens }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div style="margin-top:32px;display:flex;gap:12px">
        <el-button v-if="currentStep > 0" @click="prevStep">上一步</el-button>
        <el-button v-if="currentStep < 2" type="primary" @click="nextStep">下一步</el-button>
        <el-button v-if="currentStep === 2" type="primary" :loading="loading" @click="handleFinish">创建 Agent</el-button>
        <el-button @click="router.push('/agents')">取消</el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.agent-create-page { max-width: 900px; }
.avatar-grid { display: flex; gap: 8px; flex-wrap: wrap; }
.avatar-option { font-size: 24px; width: 40px; height: 40px; display: flex; align-items: center; justify-content: center; border: 2px solid transparent; border-radius: 8px; cursor: pointer; transition: all 0.2s; }
.avatar-option:hover { background: var(--el-fill-color-light); }
.avatar-option.active { border-color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
</style>
