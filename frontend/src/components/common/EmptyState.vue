<script setup lang="ts">
defineProps<{
  icon?: string
  title: string
  description?: string
  actionText?: string
}>()

defineEmits<{
  action: []
}>()
</script>

<template>
  <div class="empty-state">
    <el-empty :description="description || ''">
      <template v-if="title" #title>
        <p class="empty-title">{{ title }}</p>
      </template>
      <template v-if="actionText" #default>
        <el-button type="primary" @click="$emit('action')">{{ actionText }}</el-button>
      </template>
    </el-empty>
  </div>
</template>

<style scoped>
.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
}
.empty-title {
  font-size: 16px;
  color: var(--el-text-color-regular);
  margin-bottom: 8px;
}
</style>
