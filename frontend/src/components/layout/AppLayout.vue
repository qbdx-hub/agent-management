<script setup lang="ts">
import { useAppStore } from '@/stores/app'
import SidebarMenu from './SidebarMenu.vue'
import TopHeader from './TopHeader.vue'

const appStore = useAppStore()
</script>

<template>
  <el-container class="app-layout">
    <el-aside :width="appStore.sidebarCollapsed ? '64px' : '220px'" class="app-aside">
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
  background-color: var(--el-menu-bg-color);
  border-right: 1px solid var(--el-border-color-light);
  transition: width 0.3s;
  overflow: hidden;
}
.app-header {
  display: flex;
  align-items: center;
  border-bottom: 1px solid var(--el-border-color-light);
  background: #fff;
  padding: 0 16px;
  height: 56px;
}
.app-main {
  background-color: var(--el-bg-color-page);
  padding: 20px;
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
