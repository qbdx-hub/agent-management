<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{ icon?: string; size?: number }>(), { size: 28 })

// 图标值若为 PNG stem（数字开头，如 '09-search'）渲染为图片；否则当 emoji 文本，兼容旧数据
const isImg = computed(() => !!props.icon && /^\d+-/.test(props.icon as string))
const src = computed(() => `/icons/${props.icon}.png`)
</script>

<template>
  <img v-if="isImg" :src="src" class="tool-icon-img" :style="{ width: size + 'px', height: size + 'px' }" alt="" />
  <span v-else class="tool-icon-emoji" :style="{ fontSize: size + 'px', lineHeight: 1 }">{{ icon }}</span>
</template>

<style scoped>
.tool-icon-img { object-fit: contain; display: inline-block; vertical-align: middle; }
.tool-icon-emoji { display: inline-block; vertical-align: middle; }
</style>
