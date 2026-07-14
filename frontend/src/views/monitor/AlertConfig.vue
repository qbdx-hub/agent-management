<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { mockAlertRules } from '@/mock/monitor'
import { ALERT_SEVERITY_MAP, NOTIFY_CHANNEL_MAP } from '@/utils/constants'
import type { AlertRule } from '@/types/monitor'

const router = useRouter()
const rules = ref<AlertRule[]>([])
const showCreate = ref(false)
const newRule = ref<Partial<AlertRule>>({ name: '', metric: 'success_rate', condition: 'lt', threshold: 0.85, duration: '5m', severity: 'warning', enabled: true, notifyChannels: ['feishu'] })

onMounted(() => { rules.value = [...mockAlertRules] })

function handleCreate() {
  rules.value.push({ ...newRule.value, id: Date.now(), targetType: 'agent', targetId: null } as AlertRule)
  showCreate.value = false
}

function handleDelete(idx: number) { rules.value.splice(idx, 1) }
</script>

<template>
  <div class="alert-config-page">
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
        <el-table-column label="严重度" width="80"><template #default="{ row }"><el-tag :type="row.severity === 'critical' ? 'danger' : 'warning'" size="small">{{ ALERT_SEVERITY_MAP[row.severity] }}</el-tag></template></el-table-column>
        <el-table-column label="通知" width="150"><template #default="{ row }"><el-tag v-for="ch in row.notifyChannels" :key="ch" size="small" style="margin-right:4px">{{ NOTIFY_CHANNEL_MAP[ch] || ch }}</el-tag></template></el-table-column>
        <el-table-column label="启用" width="70"><template #default="{ row }"><el-switch v-model="row.enabled" /></template></el-table-column>
        <el-table-column label="" width="70"><template #default="{ $index }"><el-button text type="danger" size="small" @click="handleDelete($index)">删除</el-button></template></el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showCreate" title="添加告警规则" width="500px">
      <el-form label-width="100px">
        <el-form-item label="规则名称"><el-input v-model="newRule.name" /></el-form-item>
        <el-form-item label="指标"><el-select v-model="newRule.metric"><el-option label="成功率" value="success_rate" /><el-option label="P99延迟" value="p99_latency" /><el-option label="日Token" value="daily_tokens" /></el-select></el-form-item>
        <el-form-item label="条件"><el-select v-model="newRule.condition" style="width:100px"><el-option label="低于" value="lt" /><el-option label="高于" value="gt" /></el-select></el-form-item>
        <el-form-item label="阈值"><el-input-number v-model="newRule.threshold" :step="0.01" /></el-form-item>
        <el-form-item label="严重度"><el-select v-model="newRule.severity"><el-option v-for="(label, key) in ALERT_SEVERITY_MAP" :key="key" :label="label" :value="key" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="showCreate = false">取消</el-button><el-button type="primary" @click="handleCreate">创建</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>.alert-config-page { max-width: 1200px; }</style>
