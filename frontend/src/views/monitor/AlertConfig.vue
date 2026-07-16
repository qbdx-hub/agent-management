<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAlertRules, createAlertRule, deleteAlertRule, toggleAlertRule } from '@/api/monitor'
import { ALERT_SEVERITY_MAP, NOTIFY_CHANNEL_MAP } from '@/utils/constants'
import type { AlertRule } from '@/types/monitor'

const router = useRouter()
const loading = ref(false)
const rules = ref<AlertRule[]>([])
const showCreate = ref(false)
const newRule = ref<Partial<AlertRule>>({
  name: '', metric: 'success_rate', condition: 'lt', threshold: 0.85,
  duration: '5m', severity: 'warning', enabled: true, notifyChannels: ['feishu'],
  targetType: 'agent', targetId: null
})

async function loadRules() {
  loading.value = true
  try {
    const res = await getAlertRules()
    if (res.code === 0 && res.data) {
      rules.value = res.data
    }
  } catch (e) {
    console.error('加载告警规则失败', e)
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!newRule.value.name) return
  try {
    const res = await createAlertRule(newRule.value as Omit<AlertRule, 'id'>)
    if (res.code === 0 && res.data) {
      rules.value.push(res.data)
    }
    showCreate.value = false
    // 重置表单
    newRule.value = {
      name: '', metric: 'success_rate', condition: 'lt', threshold: 0.85,
      duration: '5m', severity: 'warning', enabled: true, notifyChannels: ['feishu'],
      targetType: 'agent', targetId: null
    }
  } catch (e) {
    console.error('创建告警规则失败', e)
  }
}

async function handleToggle(rule: AlertRule) {
  try {
    const res = await toggleAlertRule(rule.id, rule.enabled)
    if (res.code === 0) {
      // 成功切换
    }
  } catch (e) {
    // 失败时回滚前端状态
    rule.enabled = !rule.enabled
    console.error('切换规则状态失败', e)
  }
}

async function handleDelete(ruleId: number) {
  try {
    const res = await deleteAlertRule(ruleId)
    if (res.code === 0) {
      rules.value = rules.value.filter(r => r.id !== ruleId)
    }
  } catch (e) {
    console.error('删除告警规则失败', e)
  }
}

onMounted(loadRules)
</script>

<template>
  <div class="alert-config-page" v-loading="loading">
    <div class="page-header">
      <div style="display:flex;align-items:center;gap:8px">
        <el-button text @click="router.push('/monitor')"><el-icon><ArrowLeft /></el-icon></el-button>
        <h2>告警配置</h2>
      </div>
      <el-button type="primary" @click="showCreate = true"><el-icon><Plus /></el-icon> 添加规则</el-button>
    </div>
    <el-card>
      <el-table :data="rules">
        <el-table-column prop="name" label="规则名称" />
        <el-table-column prop="metric" label="指标" width="120" />
        <el-table-column label="条件" width="150"><template #default="{ row }">{{ row.condition }} {{ row.threshold }}</template></el-table-column>
        <el-table-column label="严重度" width="80"><template #default="{ row }"><el-tag :type="row.severity === 'critical' ? 'danger' : row.severity === 'warning' ? 'warning' : 'info'" size="small">{{ ALERT_SEVERITY_MAP[row.severity] }}</el-tag></template></el-table-column>
        <el-table-column label="通知" width="150"><template #default="{ row }"><el-tag v-for="ch in row.notifyChannels" :key="ch" size="small" style="margin-right:4px">{{ NOTIFY_CHANNEL_MAP[ch] || ch }}</el-tag></template></el-table-column>
        <el-table-column label="启用" width="70"><template #default="{ row }"><el-switch v-model="row.enabled" @change="handleToggle(row)" /></template></el-table-column>
        <el-table-column label="" width="70"><template #default="{ row }"><el-popconfirm title="确认删除该规则？" @confirm="handleDelete(row.id)"><template #reference><el-button text type="danger" size="small">删除</el-button></template></el-popconfirm></template></el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showCreate" title="添加告警规则" width="500px">
      <el-form label-width="100px">
        <el-form-item label="规则名称"><el-input v-model="newRule.name" /></el-form-item>
        <el-form-item label="指标"><el-select v-model="newRule.metric"><el-option label="成功率" value="success_rate" /><el-option label="P99延迟" value="p99_latency" /><el-option label="日Token" value="daily_tokens" /><el-option label="错误率" value="error_rate" /></el-select></el-form-item>
        <el-form-item label="条件"><el-select v-model="newRule.condition" style="width:100px"><el-option label="低于" value="lt" /><el-option label="高于" value="gt" /><el-option label="≤" value="lte" /><el-option label="≥" value="gte" /></el-select></el-form-item>
        <el-form-item label="阈值"><el-input-number v-model="newRule.threshold" :step="0.01" /></el-form-item>
        <el-form-item label="持续时间"><el-select v-model="newRule.duration"><el-option label="立即" value="0m" /><el-option label="5分钟" value="5m" /><el-option label="1小时" value="1h" /><el-option label="1天" value="1d" /></el-select></el-form-item>
        <el-form-item label="严重度"><el-select v-model="newRule.severity"><el-option v-for="(label, key) in ALERT_SEVERITY_MAP" :key="key" :label="label" :value="key" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="showCreate = false">取消</el-button><el-button type="primary" @click="handleCreate">创建</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>.alert-config-page { max-width: 1200px; }</style>
