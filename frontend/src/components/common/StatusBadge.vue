<script setup lang="ts">
import type { StatusType } from '@/types/common'

defineProps<{
  type: StatusType
  label: string
  size?: 'large' | 'default' | 'small'
}>()
</script>

<template>
  <el-tag :type="type || 'info'" :size="size || 'default'">
    {{ label }}
  </el-tag>
</template>
