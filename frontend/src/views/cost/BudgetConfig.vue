<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { mockBudgets } from '@/mock/cost'
import { NOTIFY_CHANNEL_MAP } from '@/utils/constants'
import { formatCost } from '@/utils/format'
import type { BudgetConfig } from '@/types/cost'

const router = useRouter()
const budgets = ref<BudgetConfig[]>([])
const showCreate = ref(false)
const newBudget = ref<Partial<BudgetConfig>>({ name: '', scope: 'workspace', period: 'monthly', limit: 2000, warnPercent: 80, meltdownEnabled: true, notifyChannels: ['feishu'] })

onMounted(() => { budgets.value = [...mockBudgets] })

function handleCreate() {
  budgets.value.push({ ...newBudget.value, id: Date.now(), scopeId: null } as BudgetConfig)
  showCreate.value = false
}
function handleDelete(idx: number) { budgets.value.splice(idx, 1) }
</script>

<template>
  <div class="budget-config-page">
    <div class="page-header">
      <div style="display:flex;align-items:center;gap:8px"><el-button text @click="router.push('/cost')"><el-icon><ArrowLeft /></el-icon></el-button><h2>预算配置</h2></div>
      <el-button type="primary" @click="showCreate = true"><el-icon><Plus /></el-icon> 添加预算</el-button>
    </div>
    <el-card>
      <el-table :data="budgets">
        <el-table-column prop="name" label="名称" />
        <el-table-column label="范围" width="100"><template #default="{ row }">{{ row.scope === 'workspace' ? '工作空间' : row.scope === 'user' ? '个人' : 'Agent' }}</template></el-table-column>
        <el-table-column label="周期" width="80"><template #default="{ row }">{{ row.period === 'daily' ? '日' : '月' }}</template></el-table-column>
        <el-table-column label="限额" width="120"><template #default="{ row }">{{ formatCost(row.limit) }}</template></el-table-column>
        <el-table-column label="告警%" width="80"><template #default="{ row }">{{ row.warnPercent }}%</template></el-table-column>
        <el-table-column label="熔断" width="70"><template #default="{ row }"><el-tag :type="row.meltdownEnabled ? 'danger' : 'info'" size="small">{{ row.meltdownEnabled ? '启用' : '禁用' }}</el-tag></template></el-table-column>
        <el-table-column label="" width="70"><template #default="{ $index }"><el-button text type="danger" size="small" @click="handleDelete($index)">删除</el-button></template></el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="showCreate" title="添加预算" width="500px">
      <el-form label-width="100px">
        <el-form-item label="名称"><el-input v-model="newBudget.name" /></el-form-item>
        <el-form-item label="范围"><el-select v-model="newBudget.scope"><el-option label="工作空间" value="workspace" /><el-option label="个人" value="user" /><el-option label="Agent" value="agent" /></el-select></el-form-item>
        <el-form-item label="周期"><el-select v-model="newBudget.period"><el-option label="日预算" value="daily" /><el-option label="月预算" value="monthly" /></el-select></el-form-item>
        <el-form-item label="限额"><el-input-number v-model="newBudget.limit" :min="1" :step="100" /></el-form-item>
        <el-form-item label="告警阈值"><el-slider v-model="newBudget.warnPercent" :min="50" :max="100" /></el-form-item>
        <el-form-item label="自动熔断"><el-switch v-model="newBudget.meltdownEnabled" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showCreate = false">取消</el-button><el-button type="primary" @click="handleCreate">创建</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>.budget-config-page { max-width: 1200px; }</style>
