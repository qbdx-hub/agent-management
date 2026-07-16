<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useAgentStore } from '@/stores/agent'
import { updatePromptConfig } from '@/api/agent'
import { ElMessage, ElMessageBox } from 'element-plus'

const agentStore = useAgentStore()
const agent = computed(() => agentStore.current)
const prompt = ref('')
const variables = ref<any[]>([])
const versions = ref<any[]>([])
const showVersionDrawer = ref(false)
const loading = ref(false)

const L = '{{'
const R = '}}'
function varName(name: string) { return L + name + R }
const tipExample1 = '{{variableName}}'
const tipExample2 = '{{language}}'

onMounted(() => { initPrompt() })
watch(agent, () => { initPrompt() }, { deep: true })

function initPrompt() {
  if (!agent.value) return
  prompt.value = agent.value.config.systemPrompt
  variables.value = [...agent.value.config.promptVariables]
}

async function handleSave() {
  if (!agent.value) return
  loading.value = true
  try {
    const res = await updatePromptConfig(agent.value.id, {
      systemPrompt: prompt.value,
      promptVariables: variables.value,
    })
    if (res.code === 0) {
      await agentStore.fetchAgentDetail(agent.value.id)
      ElMessage.success('Prompt 已保存')
    }
  } catch { ElMessage.error('保存失败') } finally { loading.value = false }
}

function insertVariable(name: string) {
  const textarea = document.querySelector('.prompt-textarea textarea') as HTMLTextAreaElement
  if (textarea) {
    const start = textarea.selectionStart
    const end = textarea.selectionEnd
    prompt.value = prompt.value.slice(0, start) + `{{${name}}}` + prompt.value.slice(end)
  } else {
    prompt.value += `{{${name}}}`
  }
}

function handleVersionClick(version: any) {
  ElMessageBox.confirm(`确定回滚到 ${version.versionNumber} 吗？`, '回滚确认').then(() => {
    prompt.value = version.systemPrompt
    showVersionDrawer.value = false
    ElMessage.success(`已回滚到 ${version.versionNumber}`)
  }).catch(() => {})
}
</script>

<template>
  <div class="prompt-editor" v-if="agent">
    <el-row :gutter="20">
      <el-col :span="16">
        <div class="editor-header">
          <span>System Prompt</span>
          <div class="editor-actions">
            <el-button size="small" @click="showVersionDrawer = true">版本历史</el-button>
            <el-button type="primary" size="small" :loading="loading" @click="handleSave">保存</el-button>
          </div>
        </div>
        <el-input
          v-model="prompt"
          type="textarea"
          :rows="18"
          class="prompt-textarea"
          placeholder="输入 System Prompt..."
          style="font-family: 'Cascadia Code', 'Fira Code', Consolas, monospace"
        />
        <div class="prompt-tip text-muted">支持变量语法：<code v-text="tipExample1"></code>，例如 <code v-text="tipExample2"></code></div>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header><span>变量列表</span></template>
          <div v-if="variables.length === 0" class="text-muted" style="padding:12px 0">暂无变量</div>
          <div v-for="v in variables" :key="v.name" class="var-item" @click="insertVariable(v.name)">
            <div class="var-name">{{ varName(v.name) }}</div>
            <div class="var-meta text-muted">{{ v.label }} · {{ v.type }} {{ v.required ? '(必填)' : '' }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-drawer v-model="showVersionDrawer" title="Prompt 版本历史" size="450px">
      <div v-for="v in versions" :key="v.versionId" class="version-item" @click="handleVersionClick(v)">
        <div class="version-header">
          <strong>{{ v.versionNumber }}</strong>
          <span class="text-muted">{{ v.changedAt.split('T')[0] }} · {{ v.changerName }}</span>
        </div>
        <div class="version-note text-muted">{{ v.changeNote }}</div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.prompt-editor { padding: 20px 0; }
.editor-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; font-weight: 600; }
.editor-actions { display: flex; gap: 8px; }
.prompt-textarea :deep(textarea) { font-size: 14px; line-height: 1.6; }
.prompt-tip { font-size: 12px; margin-top: 8px; }
.var-item { padding: 10px; border-bottom: 1px solid var(--el-border-color-lighter); cursor: pointer; transition: background 0.2s; }
.var-item:hover { background: var(--el-fill-color-light); }
.var-name { font-family: monospace; font-size: 13px; color: var(--el-color-primary); }
.var-meta { font-size: 12px; }
.version-item { padding: 12px; border-bottom: 1px solid var(--el-border-color-lighter); cursor: pointer; transition: background 0.2s; }
.version-item:hover { background: var(--el-fill-color-light); }
.version-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 4px; }
.version-note { font-size: 12px; }
</style>
