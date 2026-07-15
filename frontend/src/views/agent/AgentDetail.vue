<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAgentStore } from '@/stores/agent'
import { AGENT_STATUS_MAP, AGENT_STATUS_COLORS } from '@/utils/constants'

const route = useRoute()
const router = useRouter()
const agentStore = useAgentStore()

const agentId = computed(() => Number(route.params.id))
const agent = computed(() => agentStore.current)

onMounted(() => { agentStore.fetchAgentDetail(agentId.value) })
watch(agentId, (id) => { if (id) agentStore.fetchAgentDetail(id) })

function goToChat() { router.push(`/agents/${agentId.value}/chat`) }
function goToSessions() { router.push(`/agents/${agentId.value}/sessions`) }
</script>

<template>
  <div class="agent-detail-page" v-loading="agentStore.loading">
    <el-card v-if="agent" class="mb-16">
      <div class="detail-header">
        <div class="detail-info">
          <AgentAvatar :avatar="agent.avatar" :size="56" />
          <div>
            <div class="detail-name">{{ agent.name }}</div>
            <div class="text-muted">{{ agent.description }}</div>
          </div>
          <el-tag :type="(AGENT_STATUS_COLORS[agent.status] as any)" style="margin-left:12px">{{ AGENT_STATUS_MAP[agent.status] }}</el-tag>
        </div>
        <div class="detail-actions">
          <el-button type="primary" @click="goToChat"><el-icon><ChatDotRound /></el-icon> 对话</el-button>
          <el-button @click="goToSessions"><el-icon><List /></el-icon> 会话历史</el-button>
        </div>
      </div>
    </el-card>
    <el-card v-if="agent">
      <el-tabs :model-value="route.name as string" @tab-change="(name: string) => router.push({ name })">
        <el-tab-pane label="基础配置" name="AgentConfig" />
        <el-tab-pane label="Prompt" name="PromptEditor" />
        <el-tab-pane label="工具绑定" name="ToolBinding" />
        <el-tab-pane label="记忆策略" name="MemoryConfig" />
      </el-tabs>
      <router-view />
    </el-card>
  </div>
</template>

<style scoped>
.agent-detail-page { max-width: 1200px; }
.detail-header { display: flex; align-items: center; justify-content: space-between; }
.detail-info { display: flex; align-items: center; gap: 12px; }
.detail-avatar { font-size: 36px; }
.detail-name { font-size: 20px; font-weight: 600; }
.detail-actions { display: flex; gap: 8px; }
</style>
