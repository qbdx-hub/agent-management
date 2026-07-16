<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBudgets, createBudget, toggleBudget, deleteBudget } from '@/api/cost'
import { NOTIFY_CHANNEL_MAP } from '@/utils/constants'
import { formatCost, formatDateTime } from '@/utils/format'
import type { BudgetConfig, BudgetScope, BudgetPeriod } from '@/types/cost'

const router = useRouter()

// ==================== 状态 ====================
const loading = ref(false)
const budgets = ref<BudgetConfig[]>([])
const showCreate = ref(false)
const creating = ref(false)

const SCOPE_LABEL: Record<BudgetScope, string> = {
  global: '全局',
  workspace: '工作空间',
  user: '个人',
  agent: 'Agent',
}
const PERIOD_LABEL: Record<BudgetPeriod, string> = {
  daily: '日预算',
  monthly: '月预算',
}

// 新建表单
const newBudget = ref({
  name: '',
  scope: 'global' as BudgetScope,
  scopeId: null as number | null,
  period: 'monthly' as BudgetPeriod,
  limit: 2000,
  warnPercent: 80,
  meltdownEnabled: true,
  notifyChannels: ['feishu'] as string[],
  enabled: true,
})

// ==================== 方法 ====================
async function loadBudgets() {
  loading.value = true
  try {
    const res = await getBudgets()
    budgets.value = res.data ?? []
  } catch (e: any) {
    ElMessage.error(e.message || '加载预算列表失败')
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!newBudget.value.name) {
    ElMessage.warning('请输入预算名称')
    return
  }
  creating.value = true
  try {
    const res = await createBudget({
      name: newBudget.value.name,
      scope: newBudget.value.scope,
      scopeId: newBudget.value.scopeId,
      period: newBudget.value.period,
      limit: newBudget.value.limit,
      warnPercent: newBudget.value.warnPercent,
      meltdownEnabled: newBudget.value.meltdownEnabled,
      notifyChannels: newBudget.value.notifyChannels,
      enabled: newBudget.value.enabled,
    })
    ElMessage.success('预算创建成功')
    showCreate.value = false
    budgets.value.push(res.data!)
    resetForm()
  } catch (e: any) {
    ElMessage.error(e.message || '创建失败')
  } finally {
    creating.value = false
  }
}

async function handleToggle(row: BudgetConfig, val: boolean) {
  try {
    const res = await toggleBudget(row.id, val)
    // 更新本地列表中的对应条目
    const idx = budgets.value.findIndex(b => b.id === row.id)
    if (idx !== -1 && res.data) {
      budgets.value[idx] = res.data
    }
    ElMessage.success(val ? '预算已启用' : '预算已禁用')
  } catch (e: any) {
    // 操作失败时恢复开关状态
    row.enabled = !val
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleDelete(row: BudgetConfig) {
  try {
    await ElMessageBox.confirm(
      `确定要删除预算「${row.name}」吗？此操作不可撤销。`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return // 用户取消
  }
  try {
    await deleteBudget(row.id)
    budgets.value = budgets.value.filter(b => b.id !== row.id)
    ElMessage.success('预算已删除')
  } catch (e: any) {
    ElMessage.error(e.message || '删除失败')
  }
}

function resetForm() {
  newBudget.value = {
    name: '',
    scope: 'global',
    scopeId: null,
    period: 'monthly',
    limit: 2000,
    warnPercent: 80,
    meltdownEnabled: true,
    notifyChannels: ['feishu'],
    enabled: true,
  }
}

onMounted(loadBudgets)
</script>

<template>
  <div class="budget-config-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div style="display:flex;align-items:center;gap:8px">
        <el-button text @click="router.push('/cost')">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <h2>预算配置</h2>
      </div>
      <el-button type="primary" @click="showCreate = true">
        <el-icon><Plus /></el-icon> 添加预算
      </el-button>
    </div>

    <!-- 预算列表 -->
    <el-card>
      <el-table :data="budgets" v-loading="loading" stripe empty-text="暂无预算配置">
        <el-table-column prop="name" label="名称" min-width="140" />

        <el-table-column label="范围" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.scope === 'global' ? '' : row.scope === 'agent' ? 'warning' : 'info'">
              {{ SCOPE_LABEL[row.scope] || row.scope }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="周期" width="90">
          <template #default="{ row }">
            {{ PERIOD_LABEL[row.period] || row.period }}
          </template>
        </el-table-column>

        <el-table-column label="限额" width="110" align="right">
          <template #default="{ row }">
            <span style="font-weight:600">{{ formatCost(row.limit) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="已用" width="110" align="right">
          <template #default="{ row }">
            <span style="color:#e6a23c">{{ formatCost(row.currentAmount ?? 0) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="告警阈值" width="80" align="center">
          <template #default="{ row }">
            {{ row.warnPercent }}%
          </template>
        </el-table-column>

        <el-table-column label="熔断" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.meltdownEnabled ? 'danger' : 'info'" size="small">
              {{ row.meltdownEnabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="通知渠道" width="120">
          <template #default="{ row }">
            <span v-for="(ch, i) in row.notifyChannels" :key="ch">
              {{ NOTIFY_CHANNEL_MAP[ch] || ch }}{{ i < row.notifyChannels.length - 1 ? '、' : '' }}
            </span>
          </template>
        </el-table-column>

        <!-- 启用/禁用开关 -->
        <el-table-column label="启用状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.enabled"
              active-text="启用"
              inactive-text="禁用"
              inline-prompt
              @change="(val: boolean) => handleToggle(row, val)"
            />
          </template>
        </el-table-column>

        <!-- 创建时间 -->
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>

        <!-- 操作 -->
        <el-table-column label="操作" width="80" align="center" fixed="right">
          <template #default="{ row }">
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建预算对话框 -->
    <el-dialog v-model="showCreate" title="添加预算" width="520px" @close="resetForm">
      <el-form label-width="110px">
        <el-form-item label="名称" required>
          <el-input v-model="newBudget.name" placeholder="例如：全局月度预算" />
        </el-form-item>

        <el-form-item label="范围">
          <el-select v-model="newBudget.scope" style="width:100%">
            <el-option label="全局" value="global" />
            <el-option label="工作空间" value="workspace" />
            <el-option label="个人" value="user" />
            <el-option label="Agent" value="agent" />
          </el-select>
        </el-form-item>

        <el-form-item label="周期">
          <el-select v-model="newBudget.period" style="width:100%">
            <el-option label="日预算" value="daily" />
            <el-option label="月预算" value="monthly" />
          </el-select>
        </el-form-item>

        <el-form-item label="限额 (USD)" required>
          <el-input-number v-model="newBudget.limit" :min="1" :step="100" style="width:100%" />
        </el-form-item>

        <el-form-item label="告警阈值">
          <el-slider v-model="newBudget.warnPercent" :min="50" :max="100" :step="5" show-stops :marks="{ 50: '50%', 80: '80%', 100: '100%' }" />
        </el-form-item>

        <el-form-item label="自动熔断">
          <el-switch v-model="newBudget.meltdownEnabled" active-text="超支时熔断" inactive-text="不熔断" />
        </el-form-item>

        <el-form-item label="通知渠道">
          <el-checkbox-group v-model="newBudget.notifyChannels">
            <el-checkbox label="feishu">飞书</el-checkbox>
            <el-checkbox label="wecom">企业微信</el-checkbox>
            <el-checkbox label="email">邮件</el-checkbox>
            <el-checkbox label="webhook">Webhook</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="立即启用">
          <el-switch v-model="newBudget.enabled" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.budget-config-page {
  max-width: 1200px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
</style>
