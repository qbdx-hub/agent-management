import { defineStore } from 'pinia'

export type ThemeMode = 'light' | 'dark'

const STORAGE_KEY = 'myagent-theme'

function readStored(): ThemeMode {
  try {
    return localStorage.getItem(STORAGE_KEY) === 'dark' ? 'dark' : 'light'
  } catch {
    return 'light'
  }
}

/** 明暗主题：默认明亮（对齐 PC 端 V4），本地持久化，切换 html 根类驱动 tokens.css */
export const useThemeStore = defineStore('theme', {
  state: () => ({
    mode: 'light' as ThemeMode,
  }),
  getters: {
    isDark: (s) => s.mode === 'dark',
  },
  actions: {
    /** 应用启动时调用：恢复上次选择并挂到 <html> 上 */
    init() {
      this.mode = readStored()
      this.apply()
    },
    toggle() {
      this.mode = this.mode === 'dark' ? 'light' : 'dark'
      try {
        localStorage.setItem(STORAGE_KEY, this.mode)
      } catch {
        /* 隐私模式等场景下持久化失败不影响切换 */
      }
      this.apply()
    },
    apply() {
      document.documentElement.classList.toggle('dark', this.mode === 'dark')
    },
  },
})
