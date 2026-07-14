<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { routes } from '@/router/routes'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()

// 提取 AppLayout 下的子路由作为菜单项
const menuItems = computed(() => {
  const layoutRoute = routes.find(r => r.path === '/')
  if (!layoutRoute || !layoutRoute.children) return []
  return layoutRoute.children
    .filter(r => !r.meta?.hidden && r.path !== '')
    .map(r => ({
      path: r.path,
      title: r.meta?.title as string || r.path,
      icon: r.meta?.icon as string,
    }))
})

const activeMenu = computed(() => {
  // 匹配当前路由对应的一级菜单路径
  const matched = route.matched.filter(m => m.path !== '/')
  if (matched.length > 0) {
    // 找到第一个匹配且不是隐藏的路由
    for (const m of matched) {
      const item = menuItems.value.find(i => i.path === m.path)
      if (item) return m.path
    }
    // 找不到则取上一级
    for (const m of matched) {
      const parent = menuItems.value.find(i => m.path.startsWith(i.path))
      if (parent) return parent.path
    }
  }
  return route.path.replace('/', '') || 'dashboard'
})

function onSelect(path: string) {
  router.push('/' + path)
}
</script>

<template>
  <div class="sidebar">
    <div class="sidebar-logo">
      <span v-if="!appStore.sidebarCollapsed" class="logo-text">Agent 管理</span>
      <span v-else class="logo-icon">🤖</span>
    </div>
    <el-menu
      :default-active="activeMenu"
      :collapse="appStore.sidebarCollapsed"
      :router="false"
      background-color="transparent"
      @select="onSelect"
    >
      <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
        <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
        <template #title>{{ item.title }}</template>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<style scoped>
.sidebar {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.sidebar-logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid var(--el-border-color-light);
  font-size: 18px;
  font-weight: 600;
  color: var(--el-color-primary);
}
.logo-text {
  white-space: nowrap;
}
.logo-icon {
  font-size: 24px;
}
.el-menu {
  border-right: none;
  flex: 1;
}
</style>
