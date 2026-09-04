<script setup lang="ts">
import { computed } from 'vue'

/**
 * 内联 SVG 图标集（与 myagent-ui 原型一致的纯图形方案）。
 * 颜色默认 currentColor，个别原型固定色的图标（back/search）保持原样。
 */
const props = defineProps<{ name: string; size?: number }>()

const ICONS: Record<string, { vb: string; body: string }> = {
  plus: {
    vb: '0 0 18 18',
    body: '<path d="M9 3v12M3 9h12" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>',
  },
  upload: {
    vb: '0 0 18 18',
    body: '<path d="M9 13V4M5 8l4-4 4 4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>',
  },
  terminal: {
    vb: '0 0 18 18',
    body: '<path d="M4 5l4 4-4 4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/><path d="M9.5 13H14" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>',
  },
  dots: {
    vb: '0 0 18 18',
    body: '<circle cx="4" cy="9" r="1.5" fill="currentColor"/><circle cx="9" cy="9" r="1.5" fill="currentColor"/><circle cx="14" cy="9" r="1.5" fill="currentColor"/>',
  },
  back: {
    vb: '0 0 16 16',
    body: '<path d="M10 3L5 8l5 5" style="stroke: var(--text-2)" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>',
  },
  arrow: {
    vb: '0 0 14 14',
    body: '<path d="M5 2l5 5-5 5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>',
  },
  search: {
    vb: '0 0 16 16',
    body: '<circle cx="7" cy="7" r="5" style="stroke: var(--text-2)" stroke-width="1.5"/><path d="M11 11l3 3" style="stroke: var(--text-2)" stroke-width="1.5" stroke-linecap="round"/>',
  },
  send: {
    vb: '0 0 16 16',
    body: '<path d="M2 8L14 2L10.5 14L8 9L2 8Z" fill="currentColor"/>',
  },
  server: {
    vb: '0 0 18 18',
    body: '<rect x="2" y="3" width="14" height="5" rx="1.5" fill="currentColor"/><rect x="2" y="10" width="14" height="5" rx="1.5" fill="currentColor"/><circle cx="5" cy="5.5" r="1" style="fill: var(--bg)"/><circle cx="5" cy="12.5" r="1" style="fill: var(--bg)"/>',
  },
  key: {
    vb: '0 0 16 16',
    body: '<circle cx="5.5" cy="10.5" r="3.5" stroke="currentColor" stroke-width="1.5"/><path d="M8 8l6-6M11.5 2.5L14 5M10 4l2.5 2.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>',
  },
  sliders: {
    vb: '0 0 16 16',
    body: '<path d="M2 4h12M2 8h12M2 12h12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/><circle cx="10" cy="4" r="2" style="fill: var(--card)" stroke="currentColor" stroke-width="1.5"/><circle cx="5" cy="8" r="2" style="fill: var(--card)" stroke="currentColor" stroke-width="1.5"/><circle cx="11" cy="12" r="2" style="fill: var(--card)" stroke="currentColor" stroke-width="1.5"/>',
  },
  bell: {
    vb: '0 0 16 16',
    body: '<path d="M8 2a4.5 4.5 0 0 1 4.5 4.5c0 3 1 4 1.5 4.5H2c.5-.5 1.5-1.5 1.5-4.5A4.5 4.5 0 0 1 8 2z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/><path d="M6.5 13.5a1.5 1.5 0 0 0 3 0" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>',
  },
  moon: {
    vb: '0 0 16 16',
    body: '<path d="M13.5 9.5A6 6 0 0 1 6.5 2.5a6 6 0 1 0 7 7z" fill="currentColor"/>',
  },
  sun: {
    vb: '0 0 16 16',
    body: '<circle cx="8" cy="8" r="3.2" stroke="currentColor" stroke-width="1.5"/><path d="M8 1.2v1.7M8 13.1v1.7M1.2 8h1.7M13.1 8h1.7M3.3 3.3l1.2 1.2M11.5 11.5l1.2 1.2M12.7 3.3l-1.2 1.2M4.5 11.5l-1.2 1.2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>',
  },
  help: {
    vb: '0 0 16 16',
    body: '<circle cx="8" cy="8" r="6" stroke="currentColor" stroke-width="1.5"/><path d="M6.2 6.2a1.8 1.8 0 1 1 2.6 1.7c-.6.3-.8.6-.8 1.1" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/><circle cx="8" cy="11.2" r="0.9" fill="currentColor"/>',
  },
  doc: {
    vb: '0 0 16 16',
    body: '<path d="M4 1.5h5.5L13 5v9.5a.9.9 0 0 1-.9.9H4a.9.9 0 0 1-.9-.9V2.4a.9.9 0 0 1 .9-.9z" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round"/><path d="M9.2 1.8V5.3h3.5" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round"/><path d="M5.5 8.5h5M5.5 11h5" stroke="currentColor" stroke-width="1.3" stroke-linecap="round"/>',
  },
  trash: {
    vb: '0 0 16 16',
    body: '<path d="M2.5 4h11M6.5 4V2.8a.8.8 0 0 1 .8-.8h1.4a.8.8 0 0 1 .8.8V4M4 4l.6 9a1 1 0 0 0 1 .9h4.8a1 1 0 0 0 1-.9L12 4" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>',
  },
}

const icon = computed(() => ICONS[props.name] || { vb: '0 0 16 16', body: '' })
const w = computed(() => props.size ?? 16)
</script>

<template>
  <svg :width="w" :height="w" :viewBox="icon.vb" fill="none" v-html="icon.body" />
</template>
