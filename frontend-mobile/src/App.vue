<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import TabBar from '@/components/TabBar.vue'

const route = useRoute()
const showTabbar = computed(() => !!route.meta.tabbar)
</script>

<template>
  <div class="app-shell">
    <router-view />
    <TabBar v-if="showTabbar" />
  </div>
</template>
