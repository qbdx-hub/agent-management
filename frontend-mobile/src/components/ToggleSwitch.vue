<script setup lang="ts">
defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void }>()
</script>

<template>
  <div class="switch" :class="{ on: modelValue }" @click="emit('update:modelValue', !modelValue)" />
</template>
