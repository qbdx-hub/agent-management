<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { mockWorkspaceSettings } from '@/mock/workspace'
import { useWorkspaceStore } from '@/stores/workspace'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { WorkspaceSettings } from '@/types/workspace'

const wsStore = useWorkspaceStore()
const settings = ref<WorkspaceSettings>({ ...mockWorkspaceSettings })
const loading = ref(false)

async function handleSave() {
  loading.value = true
  try { ElMessage.success('设置已保存') } catch { ElMessage.error('保存失败') } finally { loading.value = false }
}

function handleDelete() {
  ElMessageBox.confirm('确定要删除此工作空间吗？此操作不可恢复。', '危险操作', { type: 'error', confirmButtonText: '确定删除' })
    .then(() => { ElMessage.success('已删除') }).catch(() => {})
}
</script>

<template>
  <div class="workspace-settings-page">
    <div class="page-header"><h2>空间设置</h2></div>
    <el-card>
      <el-form label-width="140px" style="max-width:600px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-form-item label="空间名称"><el-input :model-value="wsStore.current?.name || 'Agent 开发团队'" /></el-form-item>
        <el-form-item label="描述"><el-input type="textarea" :rows="2" :model-value="wsStore.current?.description || ''" /></el-form-item>

        <el-divider content-position="left">运行配置</el-divider>
        <el-form-item label="默认模型供应商">
          <el-select v-model="settings.defaultModelProvider"><el-option label="OpenAI" value="openai" /><el-option label="Anthropic" value="anthropic" /><el-option label="DeepSeek" value="deepseek" /></el-select>
        </el-form-item>
        <el-form-item label="语言"><el-select v-model="settings.language"><el-option label="中文" value="zh-CN" /><el-option label="English" value="en" /></el-select></el-form-item>
        <el-form-item label="会话保留天数"><el-input-number v-model="settings.sessionRetentionDays" :min="7" :max="365" /></el-form-item>
        <el-form-item label="自动归档天数"><el-input-number v-model="settings.autoArchiveDays" :min="7" :max="180" /></el-form-item>
        <el-form-item label="单任务最大Token"><el-input-number v-model="settings.maxTokensPerTask" :min="10000" :max="1000000" :step="10000" /></el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSave">保存设置</el-button>
        </el-form-item>

        <el-divider content-position="left">危险区域</el-divider>
        <el-form-item>
          <el-button type="danger" @click="handleDelete">删除工作空间</el-button>
          <span class="text-muted" style="margin-left:12px">删除后所有数据将无法恢复</span>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>.workspace-settings-page { max-width: 900px; }</style>
