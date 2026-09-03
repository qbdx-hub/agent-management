import { defineStore } from 'pinia'
import type { UserVO } from '@/types'

const TOKEN_KEY = 'm_token'
const USER_KEY = 'm_user'

function loadUser(): UserVO | null {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? (JSON.parse(raw) as UserVO) : null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: loadUser(),
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
    /** 后端要求工作空间上下文：取首个工作空间 */
    workspaceId: (s) => s.user?.workspaces?.[0]?.id ?? null,
    displayName: (s) => s.user?.nickname || s.user?.username || '',
  },
  actions: {
    setSession(token: string, user: UserVO) {
      this.token = token
      this.user = user
      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(USER_KEY, JSON.stringify(user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    },
  },
})
