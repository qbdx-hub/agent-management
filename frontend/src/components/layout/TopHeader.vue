<script setup lang="ts">
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { useWorkspaceStore } from '@/stores/workspace'
import { ElMessageBox } from 'element-plus'

const appStore = useAppStore()
const userStore = useUserStore()
const wsStore = useWorkspaceStore()

function handleLogout() {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' }).then(() => {
    userStore.logout()
  }).catch(() => {})
}

function handleSwitchWorkspace(id: number) {
  wsStore.switchWorkspace(id)
  window.location.reload()
}
</script>

<template>
  <div class="top-header">
    <div class="top-header-left">
      <el-button
        :icon="appStore.sidebarCollapsed ? 'Expand' : 'Fold'"
        text
        @click="appStore.toggleSidebar()"
      />
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-if="$route.meta.title" :to="$route.path">
          {{ $route.meta.title }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div class="top-header-right">
      <!-- 工作空间切换 -->
      <el-select
        v-if="wsStore.list.length > 0"
        :model-value="wsStore.currentId"
        style="width: 160px; margin-right: 12px"
        size="small"
        @change="handleSwitchWorkspace"
      >
        <el-option
          v-for="ws in wsStore.list"
          :key="ws.id"
          :label="ws.name"
          :value="ws.id"
        />
      </el-select>

      <!-- 用户信息 -->
      <el-dropdown trigger="click">
        <span class="user-info">
          <el-avatar :size="28" icon="UserFilled" />
          <span class="user-name">{{ userStore.user?.nickname || userStore.user?.username || '用户' }}</span>
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item disabled>
              {{ userStore.user?.role === 'ADMIN' ? '管理员' : '成员' }}
            </el-dropdown-item>
            <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<style scoped>
.top-header {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.top-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.top-header-right {
  display: flex;
  align-items: center;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 14px;
}
.user-name {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
