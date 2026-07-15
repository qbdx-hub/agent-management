<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{ avatar?: string; size?: number }>(), { size: 40 })

// avatar 值为 PNG stem（数字开头，如 '23-ai-robot'）渲染图片；否则当 emoji 文本，兼容数据库里的旧值
const isImg = computed(() => !!props.avatar && /^\d+-/.test(props.avatar as string))
const src = computed(() => `/icons/${props.avatar}.png`)
</script>

<template>
  <span class="agent-avatar-wrap" :style="{ width: size + 'px', height: size + 'px' }">
    <img v-if="isImg" :src="src" class="agent-avatar-img" alt="" />
    <span v-else class="agent-avatar-emoji" :style="{ fontSize: Math.round(size * 0.6) + 'px' }">{{ avatar }}</span>
  </span>
</template>

<style scoped>
.agent-avatar-wrap { display: inline-flex; align-items: center; justify-content: center; border-radius: 50%; overflow: hidden; background: var(--el-fill-color-light); flex-shrink: 0; vertical-align: middle; }
.agent-avatar-img { width: 100%; height: 100%; object-fit: cover; }
.agent-avatar-emoji { line-height: 1; display: inline-flex; align-items: center; justify-content: center; }
</style>
