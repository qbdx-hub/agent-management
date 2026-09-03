<script setup lang="ts">
import { computed } from 'vue'
import { I, FALLBACK_ICON } from './icons'

const props = withDefaults(defineProps<{ name?: string; size?: number }>(), { size: 16 })

const comp = computed(() => I[props.name || ''] || I[FALLBACK_ICON])
</script>

<template>
  <component :is="comp" :width="size" :height="size" class="ui-icon" />
</template>

<style scoped>
.ui-icon {
  flex-shrink: 0;
  display: inline-block;
  vertical-align: middle;
}
</style>
