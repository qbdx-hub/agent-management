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
    <!-- 头部：直接落在页面上，不再包卡片 -->
    <div v-if="agent" class="detail-header">
      <div class="detail-info">
        <AgentAvatar :avatar="agent.avatar" :size="56" />
        <div class="detail-text">
          <div class="detail-name">
            {{ agent.name }}
            <el-tag :type="(AGENT_STATUS_COLORS[agent.status] as any)" size="small" round>{{ AGENT_STATUS_MAP[agent.status] }}</el-tag>
          </div>
          <div v-if="agent.description" class="detail-desc">{{ agent.description }}</div>
        </div>
      </div>
      <div class="detail-actions">
        <el-button @click="goToSessions"><UiIcon name="list" />会话历史</el-button>
        <el-button type="primary" @click="goToChat"><UiIcon name="message" />进入对话</el-button>
      </div>
    </div>
    <!-- 标签页：同样直接落在页面上，避免 卡片套卡片 -->
    <el-tabs v-if="agent" class="detail-tabs" :model-value="route.name as string" @tab-change="(name: string) => router.push({ name })">
      <el-tab-pane label="基础配置" name="AgentConfig" />
      <el-tab-pane label="Prompt" name="PromptEditor" />
      <el-tab-pane label="工具绑定" name="ToolBinding" />
      <el-tab-pane label="记忆策略" name="MemoryConfig" />
    </el-tabs>
    <router-view />
  </div>
</template>

<style scoped>
.agent-detail-page { max-width: 1200px; margin: 0 auto; }
.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.detail-info { display: flex; align-items: center; gap: 14px; min-width: 0; }
.detail-text { min-width: 0; }
.detail-name {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.2px;
  color: var(--text-1);
  display: flex;
  align-items: center;
  gap: 10px;
}
.detail-desc {
  font-size: 13px;
  color: var(--text-3);
  margin-top: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.detail-actions { display: flex; gap: 8px; flex-shrink: 0; }
.detail-tabs { margin-top: 20px; }
</style>
