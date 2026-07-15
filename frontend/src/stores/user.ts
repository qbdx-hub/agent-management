import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, getCurrentUser, register as registerApi } from '@/api/auth'
import type { UserInfo } from '@/types/workspace'
import { ElMessage } from 'element-plus'

export interface WorkspaceBrief {
  id: number
  name: string
  role: string
}

interface UserState {
  user: (UserInfo & { permissions: string[]; workspaces: WorkspaceBrief[] }) | null
  token: string
}

export const useUserStore = defineStore('user', () => {
  const user = ref<UserState['user']>(null)
  const token = ref(localStorage.getItem('token') || '')

  const isLoggedIn = computed(() => !!token.value)
  const permissions = computed(() => user.value?.permissions || [])
  const workspaces = computed(() => user.value?.workspaces || [])

  async function login(username: string, password: string) {
    const res = await loginApi(username, password)
    if (res.code === 0) {
      token.value = res.data.token
      user.value = res.data.user
      localStorage.setItem('token', res.data.token)
    }
  }

  /** 注册：成功返回 true；失败时 http 拦截器已 reject 并弹错，调用方 catch 即可 */
  async function register(payload: { username: string; nickname: string; email: string; password: string }) {
    const res = await registerApi(payload)
    return res.code === 0
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('workspaceId')
    window.location.href = '/login'
  }

  async function fetchUserInfo() {
    const res = await getCurrentUser()
    if (res.code === 0) {
      user.value = res.data as UserState['user']
    }
  }

  function hasPermission(code: string): boolean {
    if (!user.value) return false
    if (user.value.role === 'ADMIN') return true
    return permissions.value.some(p => {
      if (p === code) return true
      if (p.endsWith(':*')) {
        const prefix = p.replace(':*', '')
        return code.startsWith(prefix)
      }
      return false
    })
  }

  return { user, token, isLoggedIn, permissions, workspaces, login, register, logout, fetchUserInfo, hasPermission }
})
