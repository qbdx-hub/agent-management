<script setup lang="ts">
import { computed } from 'vue'
import { resolveLegacyIcon, hueFor } from './icons'

const props = withDefaults(defineProps<{ avatar?: string; size?: number }>(), { size: 40 })

// 存量值（PNG stem / emoji）与新版图标名统一解析为 Tabler 图标
const iconName = computed(() => resolveLegacyIcon(props.avatar))
const hue = computed(() => hueFor(props.avatar))
const iconSize = computed(() => Math.round(props.size * 0.52))
</script>

<template>
  <span
    class="agent-avatar-wrap"
    :style="{ width: size + 'px', height: size + 'px', background: hue.bg, color: hue.fg }"
  >
    <UiIcon :name="iconName" :size="iconSize" />
  </span>
</template>

<style scoped>
.agent-avatar-wrap {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
