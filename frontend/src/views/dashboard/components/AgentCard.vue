<script setup lang="ts">
import type { AgentSummary } from '@/types/agent'
import { AGENT_STATUS_MAP, AGENT_STATUS_COLORS } from '@/utils/constants'
import { formatPercent } from '@/utils/format'

defineProps<{ agent: AgentSummary }>()
defineEmits<{ click: [] }>()
</script>

<template>
  <el-card shadow="hover" class="agent-card" @click="$emit('click')">
    <div class="agent-card-header">
      <AgentAvatar :avatar="agent.avatar" :size="44" />
      <div class="agent-info">
        <div class="agent-name">{{ agent.name }}</div>
        <el-tag :type="(AGENT_STATUS_COLORS[agent.status] as any)" size="small">
          {{ AGENT_STATUS_MAP[agent.status] }}
        </el-tag>
      </div>
    </div>
    <div class="agent-desc">{{ agent.description }}</div>
    <div class="agent-meta">
      <span><el-icon class="ii"><Tools /></el-icon>{{ agent.toolCount }} 工具</span>
      <span><el-icon class="ii"><TrendCharts /></el-icon>{{ formatPercent(agent.successRate) }} 成功率</span>
      <span><el-icon class="ii"><ChatDotRound /></el-icon>{{ agent.totalSessions }} 会话</span>
    </div>
  </el-card>
</template>

<style scoped>
.agent-card {
  cursor: pointer;
  transition: all 0.2s;
}
.agent-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
.agent-card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.agent-avatar {
  font-size: 28px;
}
.agent-name {
  font-weight: 600;
  font-size: 15px;
}
.agent-desc {
  color: #909399;
  font-size: 13px;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.agent-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
}
</style>
