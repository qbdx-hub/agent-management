<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

/** 当前登录用户头像：已上传显示图片，否则显示昵称首字符（复用全局 .avatar 样式） */
const props = defineProps<{ size?: 'md' | 'sm' }>()

const auth = useAuthStore()
const avatarUrl = computed(() => auth.user?.avatar || '')
</script>

<template>
  <div class="avatar" :class="{ sm: size === 'sm' }" style="overflow: hidden">
    <img v-if="avatarUrl" :src="avatarUrl" alt="头像" style="width: 100%; height: 100%; object-fit: cover" />
    <template v-else>{{ auth.displayName.slice(0, 1) }}</template>
  </div>
</template>
