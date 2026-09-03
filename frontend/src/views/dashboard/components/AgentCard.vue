<script setup lang="ts">
import type { AgentSummary } from '@/types/agent'
import { AGENT_STATUS_MAP, AGENT_STATUS_COLORS } from '@/utils/constants'
import { formatPercent } from '@/utils/format'

defineProps<{ agent: AgentSummary }>()
defineEmits<{ click: [] }>()
</script>

<template>
  <el-card shadow="never" class="agent-card" @click="$emit('click')">
    <div class="agent-top">
      <AgentAvatar :avatar="agent.avatar" :size="42" />
      <div class="agent-info">
        <div class="agent-name">{{ agent.name }}</div>
        <div class="agent-desc">{{ agent.description || '暂无描述' }}</div>
      </div>
      <el-tag :type="(AGENT_STATUS_COLORS[agent.status] as any)" size="small" round>
        {{ AGENT_STATUS_MAP[agent.status] }}
      </el-tag>
    </div>
    <div class="agent-stats">
      <div class="stat">
        <div class="k">工具</div>
        <div class="v num">{{ agent.toolCount }}</div>
      </div>
      <div class="stat">
        <div class="k">会话</div>
        <div class="v num">{{ agent.totalSessions }}</div>
      </div>
      <div class="stat">
        <div class="k">成功率</div>
        <div class="v num" :class="{ muted: agent.successRate == null }">{{ formatPercent(agent.successRate) }}</div>
      </div>
    </div>
    <span class="card-action">进入会话 →</span>
  </el-card>
</template>

<style scoped>
.agent-card {
  cursor: pointer;
  position: relative;
  transition: transform 0.15s ease-out, box-shadow 0.15s ease-out, border-color 0.15s ease-out;
}
.agent-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lift);
  border-color: var(--border-1);
}
.agent-top {
  display: flex;
  align-items: center;
  gap: 12px;
}
.agent-info {
  flex: 1;
  min-width: 0;
}
.agent-name {
  font-weight: 700;
  font-size: 14.5px;
  color: var(--text-1);
}
.agent-desc {
  color: var(--text-2);
  font-size: 12.5px;
  margin-top: 1px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.agent-stats {
  display: flex;
  gap: 26px;
  margin-top: 18px;
}
.stat .k {
  font-size: 12px;
  color: var(--text-2);
}
.stat .v {
  font-size: 15px;
  font-weight: 700;
  margin-top: 1px;
  color: var(--text-1);
}
.stat .v.muted {
  color: var(--text-3);
  font-weight: 500;
}
.card-action {
  position: absolute;
  right: 20px;
  bottom: 18px;
  font-size: 12.5px;
  color: var(--accent);
  font-weight: 600;
  opacity: 0;
  transition: opacity 0.15s ease-out;
}
.agent-card:hover .card-action {
  opacity: 1;
}
@media (prefers-reduced-motion: reduce) {
  .agent-card,
  .card-action {
    transition: none;
  }
  .agent-card:hover {
    transform: none;
  }
}
</style>
