<script setup lang="ts">
import { computed } from 'vue'
import { AGENT_ICONS, resolveAgentIcon, hueFor } from '@/components/agentIcons'

/**
 * Agent 图标头像：按存量 avatar 值解析 Tabler 图标（与 PC 端同源），
 * 底色由 key 稳定散列取柔和色板，同一 Agent 每次渲染同色。
 */
const props = defineProps<{ name?: string; avatar?: string; size?: number }>()

const iconName = computed(() => resolveAgentIcon(props.avatar))
const hue = computed(() => hueFor(props.avatar || props.name))
const px = computed(() => props.size ?? 42)
const iconSize = computed(() => Math.round(px.value * 0.52))
</script>

<template>
  <div
    class="avatar-ico"
    :style="{ background: hue.bg, color: hue.fg, width: `${px}px`, height: `${px}px` }"
  >
    <svg :width="iconSize" :height="iconSize" viewBox="0 0 24 24" fill="none" v-html="AGENT_ICONS[iconName] || ''" />
  </div>
</template>
