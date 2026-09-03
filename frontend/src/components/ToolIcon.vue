<script setup lang="ts">
import { computed } from 'vue'
import { resolveLegacyIcon, hueFor } from './icons'

const props = withDefaults(defineProps<{ icon?: string; size?: number }>(), { size: 28 })

// 存量值（PNG stem / emoji）与新版图标名统一解析为 Tabler 图标
const iconName = computed(() => resolveLegacyIcon(props.icon))
const hue = computed(() => hueFor(props.icon))
const iconSize = computed(() => Math.round(props.size * 0.54))
</script>

<template>
  <span
    class="tool-icon-wrap"
    :style="{ width: size + 'px', height: size + 'px', background: hue.bg, color: hue.fg }"
  >
    <UiIcon :name="iconName" :size="iconSize" />
  </span>
</template>

<style scoped>
.tool-icon-wrap {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  flex-shrink: 0;
  vertical-align: middle;
}
</style>
