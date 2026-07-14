import type { Router } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useWorkspaceStore } from '@/stores/workspace'

export function setupGuards(router: Router) {
  router.beforeEach(async (to, _from, next) => {
    const userStore = useUserStore()
    const wsStore = useWorkspaceStore()

    // 1. 登录检查
    if (to.path !== '/login' && !userStore.isLoggedIn) {
      return next('/login')
    }

    // 2. 工作空间检查（登录后必须有活跃空间）
    if (to.path !== '/login' && to.path !== '/workspace/settings' && to.path !== '/dashboard' && !wsStore.isReady) {
      // 尝试从 localStorage 恢复
      const savedWsId = localStorage.getItem('workspaceId')
      if (savedWsId) {
        wsStore.switchWorkspace(Number(savedWsId))
      } else {
        // 尝试加载空间列表
        await wsStore.fetchMyWorkspaces()
      }
    }

    // 3. 权限检查
    if (to.meta.permission && !userStore.hasPermission(to.meta.permission as string)) {
      return next('/dashboard')
    }

    // 设置页面标题
    if (to.meta.title) {
      document.title = `${to.meta.title} - Agent 管理系统`
    }

    next()
  })
}
