<script setup lang="ts">
import { useRoute } from 'vue-router'
import { computed } from 'vue'

const route = useRoute()

/** tab 图标 path（来自原型 tabIcons，第 3 个为知识库列表图形） */
const ICON_PATHS = [
  'M3 8l5-5 5 5v5a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V8z',
  'M2 3h12v7H6l-3 3v-3H2V3z',
  'M2 3h12v4H2V3zm0 6h12v4H2V9z',
  'M8 8a3 3 0 1 0 0-6 3 3 0 0 0 0 6zm-5 6c0-2.8 2.2-5 5-5s5 2.2 5 5H3z',
]

const TABS = [
  { name: 'home', label: '首页' },
  { name: 'chat-list', label: '对话' },
  { name: 'kb', label: '知识库' },
  { name: 'profile', label: '我的' },
]

const active = computed(() => String(route.name || ''))
</script>

<template>
  <div class="tabbar">
    <router-link v-for="(t, i) in TABS" :key="t.name" :to="{ name: t.name }" class="tab" :class="{ active: active === t.name }">
      <svg width="18" height="18" viewBox="0 0 16 16">
        <path :d="ICON_PATHS[i]" :fill="active === t.name ? '#fff' : 'none'" :stroke="active === t.name ? 'none' : '#8C8C8C'" stroke-width="1.4" stroke-linejoin="round" />
      </svg>
      <span>{{ t.label }}</span>
    </router-link>
  </div>
</template>
