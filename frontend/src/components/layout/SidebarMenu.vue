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
    <div class="sidebar-logo" :class="{ collapsed: appStore.sidebarCollapsed }">
      <span class="logo-mark"><UiIcon name="robot" :size="19" /></span>
      <span v-if="!appStore.sidebarCollapsed" class="logo-text">Agent 管理</span>
    </div>
    <el-menu
      :default-active="activeMenu"
      :collapse="appStore.sidebarCollapsed"
      :router="false"
      background-color="transparent"
      @select="onSelect"
    >
      <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
        <UiIcon v-if="item.icon" :name="item.icon" :size="19" class="menu-icon" />
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
  height: 64px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  border-bottom: 1px solid var(--border-1);
  font-size: 17px;
  font-weight: 800;
  letter-spacing: 0.2px;
  color: var(--text-1);
  flex-shrink: 0;
}
.logo-text {
  white-space: nowrap;
}
.logo-mark {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: var(--accent-soft);
  color: var(--accent);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.sidebar-logo.collapsed {
  padding: 0;
  justify-content: center;
}
.el-menu {
  border-right: none;
  flex: 1;
  padding: 12px;
  overflow-y: auto;
  --el-menu-base-level-padding: 12px;
  --el-menu-icon-width: 30px;
}
.el-menu :deep(.el-menu-item) {
  height: 48px;
  line-height: 48px;
  margin: 3px 0;
  border-radius: var(--r-control);
  font-size: 15px;
  transition: background 0.15s ease-out, color 0.15s ease-out;
}
.el-menu :deep(.el-menu-item .menu-icon) {
  color: var(--text-2);
  margin-right: 10px;
}
.el-menu :deep(.el-menu-item.is-active .menu-icon) {
  color: var(--accent);
}
.el-menu :deep(.el-menu-item:hover .menu-icon) {
  color: var(--text-1);
}
.el-menu :deep(.el-menu-item.is-active) {
  background: var(--accent-soft);
  font-weight: 600;
}
/* 折叠态：图标居中 */
.el-menu.el-menu--collapse {
  padding: 12px 10px;
}
.el-menu.el-menu--collapse :deep(.el-menu-item) {
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
