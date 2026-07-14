<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAgentStore } from '@/stores/agent'
import AgentCard from './components/AgentCard.vue'
import RecentTasks from './components/RecentTasks.vue'
import QuickCreate from './components/QuickCreate.vue'

const router = useRouter()
const agentStore = useAgentStore()
const showCreateDialog = ref(false)

onMounted(() => {
  agentStore.fetchAgentList()
})

function handleAgentCreated(id: number) {
  showCreateDialog.value = false
  router.push(`/agents/${id}`)
}
</script>

<template>
  <div class="dashboard-page">
    <div class="welcome-section">
      <h2>👋 欢迎回来</h2>
      <p class="text-muted">管理你的 AI Agent，查看运行状态</p>
    </div>

    <div class="stats-row">
      <el-card shadow="hover" class="stat-card">
        <div class="stat-value">{{ agentStore.list.length }}</div>
        <div class="stat-label">Agent 总数</div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-value text-success">{{ agentStore.list.filter(a => a.status === 'published').length }}</div>
        <div class="stat-label">已发布</div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-value text-warning">{{ agentStore.list.filter(a => a.status === 'testing').length }}</div>
        <div class="stat-label">调试中</div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-value text-muted">{{ agentStore.list.filter(a => a.status === 'draft').length }}</div>
        <div class="stat-label">草稿</div>
      </el-card>
    </div>

    <el-row :gutter="20">
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>我的 Agent</span>
              <el-button type="primary" @click="showCreateDialog = true"><el-icon><Plus /></el-icon> 新建 Agent</el-button>
            </div>
          </template>
          <div class="card-grid">
            <AgentCard v-for="agent in agentStore.list" :key="agent.id" :agent="agent" @click="router.push(`/agents/${agent.id}`)" />
          </div>
          <el-empty v-if="agentStore.list.length === 0" description="还没有 Agent，点击上方按钮创建" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="mb-24">
          <template #header><span>快速操作</span></template>
          <div class="quick-actions">
            <el-button @click="router.push('/agents/create')" style="width:100%"><el-icon><Plus /></el-icon> 创建 Agent</el-button>
            <el-button @click="router.push('/tools/register')" style="width:100%"><el-icon><SetUp /></el-icon> 注册工具</el-button>
            <el-button @click="router.push('/monitor')" style="width:100%"><el-icon><Monitor /></el-icon> 查看监控</el-button>
          </div>
        </el-card>
        <RecentTasks />
      </el-col>
    </el-row>
    <QuickCreate v-model="showCreateDialog" @created="handleAgentCreated" />
  </div>
</template>

<style scoped>
.dashboard-page { max-width: 1400px; }
.welcome-section { margin-bottom: 24px; }
.welcome-section h2 { font-size: 24px; margin-bottom: 4px; }
.stat-card { text-align: center; }
.stat-value { font-size: 32px; font-weight: 700; color: var(--el-color-primary); line-height: 1.2; }
.stat-label { color: #909399; font-size: 13px; margin-top: 4px; }
.card-header { display: flex; align-items: center; justify-content: space-between; }
.quick-actions { display: flex; flex-direction: column; gap: 10px; }
.quick-actions .el-button { margin-left: 0; }
</style>
