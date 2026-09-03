<script setup lang="ts">
import { useAppStore } from '@/stores/app'
import SidebarMenu from './SidebarMenu.vue'
import TopHeader from './TopHeader.vue'

const appStore = useAppStore()
</script>

<template>
  <el-container class="app-layout">
    <el-aside :width="appStore.sidebarCollapsed ? '64px' : '244px'" class="app-aside">
      <SidebarMenu />
    </el-aside>
    <el-container>
      <el-header class="app-header">
        <TopHeader />
      </el-header>
      <el-main class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.app-layout {
  height: 100vh;
}
.app-aside {
  background: var(--bg-surface);
  border-right: 1px solid var(--border-1);
  transition: width 0.25s ease-out;
  overflow: hidden;
}
.app-header {
  display: flex;
  align-items: center;
  border-bottom: 1px solid var(--border-1);
  background: var(--bg-surface);
  padding: 0 16px;
  height: 56px;
}
.app-main {
  background: var(--bg-page);
  padding: 24px 32px;
  overflow-y: auto;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
