import type { Router } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useWorkspaceStore } from '@/stores/workspace'

export function setupGuards(router: Router) {
  router.beforeEach(async (to, _from, next) => {
    const userStore = useUserStore()
    const wsStore = useWorkspaceStore()

    // 1. 登录检查（登录和注册页面不需要认证）
    const publicPaths = ['/login', '/register']
    if (!publicPaths.includes(to.path) && !userStore.isLoggedIn) {
      return next('/login')
    }

    // 2. 工作空间检查（仅已登录用户需要；dashboard/空间设置页允许无空间进入）。
    //    关键：未登录用户（含访问 /register）不进此分支，避免触发鉴权请求 -> 401 -> 被拦截器弹回 /login
    const wsExemptPaths = ['/workspace/settings', '/dashboard']
    if (userStore.isLoggedIn && !wsExemptPaths.includes(to.path) && !wsStore.isReady) {
      // 优先从 localStorage 恢复
      const savedWsId = localStorage.getItem('workspaceId')
      if (savedWsId) {
        wsStore.switchWorkspace(Number(savedWsId))
      } else if (userStore.workspaces.length > 0) {
        // 登录响应已带回工作空间，直接选第一个兜底（避免依赖尚未实现的 /workspaces 接口）
        wsStore.switchWorkspace(userStore.workspaces[0].id)
      } else {
        // 工作空间列表接口未就绪会 404，try/catch 防止 reject 中断导航
        try {
          await wsStore.fetchMyWorkspaces()
        } catch {
          /* 接口未实现，忽略，交由 isReady 判断 */
        }
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
