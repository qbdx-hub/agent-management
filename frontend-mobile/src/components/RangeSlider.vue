<script setup lang="ts">
import { computed, ref } from 'vue'

const props = withDefaults(defineProps<{ modelValue: number; min?: number; max?: number; step?: number }>(), {
  min: 0,
  max: 1,
  step: 0.1,
})
const emit = defineEmits<{ (e: 'update:modelValue', v: number): void }>()

const el = ref<HTMLElement | null>(null)

const percent = computed(() => {
  const span = props.max - props.min || 1
  return Math.min(100, Math.max(0, ((props.modelValue - props.min) / span) * 100))
})

function pick(clientX: number) {
  const rect = el.value!.getBoundingClientRect()
  const ratio = Math.min(1, Math.max(0, (clientX - rect.left) / rect.width))
  let v = props.min + ratio * (props.max - props.min)
  v = Math.round(v / props.step) * props.step
  // 修正浮点误差，并对齐到 step 位数
  const digits = (String(props.step).split('.')[1] || '').length
  v = Number(v.toFixed(digits))
  emit('update:modelValue', Math.min(props.max, Math.max(props.min, v)))
}

function onDown(e: PointerEvent) {
  pick(e.clientX)
  const move = (ev: PointerEvent) => pick(ev.clientX)
  const up = () => {
    window.removeEventListener('pointermove', move)
    window.removeEventListener('pointerup', up)
  }
  window.addEventListener('pointermove', move)
  window.addEventListener('pointerup', up)
}
</script>

<template>
  <div ref="el" class="slider" @pointerdown="onDown">
    <div class="fill" :style="{ width: percent + '%' }" />
    <div class="thumb" :style="{ left: percent + '%' }" />
  </div>
</template>
