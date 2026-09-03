<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getWorkspaceSettings, updateWorkspaceSettings } from '@/api/workspace'
import { useWorkspaceStore } from '@/stores/workspace'
import { ElMessage } from 'element-plus'
import type { WorkspaceSettings } from '@/types/workspace'

const router = useRouter()
const wsStore = useWorkspaceStore()

const DEFAULTS: WorkspaceSettings = {
  defaultModelProvider: 'openai',
  sessionRetentionDays: 90,
  autoArchiveDays: 30,
  maxTokensPerTask: 100000,
  language: 'zh-CN',
}

const settings = reactive<WorkspaceSettings>({ ...DEFAULTS })
const form = reactive({ name: '', description: '' })
const loading = ref(false)
const saving = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await getWorkspaceSettings()
    if (res.code === 0) Object.assign(settings, res.data)
    form.name = wsStore.current?.name || ''
    form.description = wsStore.current?.description || ''
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!form.name.trim()) {
    ElMessage.warning('空间名称不能为空')
    return
  }
  saving.value = true
  try {
    const res = await updateWorkspaceSettings({
      name: form.name.trim(),
      description: form.description,
      defaultModelProvider: settings.defaultModelProvider,
      language: settings.language,
      sessionRetentionDays: settings.sessionRetentionDays,
      autoArchiveDays: settings.autoArchiveDays,
      maxTokensPerTask: settings.maxTokensPerTask,
    })
    if (res.code === 0) {
      ElMessage.success('设置已保存')
      // 名称/描述可能已变更，刷新空间列表缓存
      wsStore.fetchMyWorkspaces().catch(() => {})
    }
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="workspace-settings-page">
    <div class="page-header">
      <h2>空间设置</h2>
      <div style="display:flex;gap:8px">
        <el-button @click="router.push('/workspace/members')">成员管理</el-button>
        <el-button @click="router.push('/workspace/activity')">空间动态</el-button>
      </div>
    </div>
    <el-card v-loading="loading">
      <el-form label-width="140px" style="max-width:600px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-form-item label="空间名称"><el-input v-model="form.name" placeholder="空间名称" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" placeholder="空间描述" /></el-form-item>

        <el-divider content-position="left">运行配置</el-divider>
        <el-form-item label="默认模型供应商">
          <el-select v-model="settings.defaultModelProvider"><el-option label="OpenAI" value="openai" /><el-option label="Anthropic" value="anthropic" /><el-option label="DeepSeek" value="deepseek" /></el-select>
        </el-form-item>
        <el-form-item label="语言"><el-select v-model="settings.language"><el-option label="中文" value="zh-CN" /><el-option label="English" value="en" /></el-select></el-form-item>
        <el-form-item label="会话保留天数"><el-input-number v-model="settings.sessionRetentionDays" :min="7" :max="365" /></el-form-item>
        <el-form-item label="自动归档天数"><el-input-number v-model="settings.autoArchiveDays" :min="7" :max="180" /></el-form-item>
        <el-form-item label="单任务最大Token"><el-input-number v-model="settings.maxTokensPerTask" :min="10000" :max="1000000" :step="10000" /></el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存设置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>.workspace-settings-page { max-width: 900px; margin: 0 auto; }</style>
