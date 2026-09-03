import { defineStore } from 'pinia'
import type { Preferences } from '@/types'
import { getPreferences, savePreferences } from '@/api/auth'

/** 与后端 defaultPreferences() 一致的兜底结构 */
export function defaultPreferences(): Preferences {
  return {
    defaultModel: 'deepseek-chat', // 平台真实接入的模型（model_pricing 表）
    temperature: 0.7,
    maxTokens: 4096,
    replyStyle: '均衡',
    notifications: {
      agentFinished: true,
      taskFailed: true,
      instanceAlert: true,
      tokenUsage80: false,
      quietHours: { enabled: true, from: '23:00', to: '08:00' },
      channels: ['app', 'email'],
    },
  }
}

/** 后端对 preferences JSON 原样存取，本地做字段级合并防止旧数据缺字段 */
function mergeDefaults(raw: Partial<Preferences> | null | undefined): Preferences {
  const d = defaultPreferences()
  if (!raw) return d
  return {
    ...d,
    ...raw,
    notifications: { ...d.notifications, ...(raw.notifications || {}) },
  }
}

export const usePrefsStore = defineStore('prefs', {
  state: () => ({
    prefs: defaultPreferences(),
    loaded: false,
    saving: false,
  }),
  actions: {
    async load() {
      const res = await getPreferences()
      this.prefs = mergeDefaults(res.data as Partial<Preferences>)
      this.loaded = true
    },
    async save(patch?: Partial<Preferences>) {
      if (patch) this.prefs = mergeDefaults({ ...this.prefs, ...patch })
      this.saving = true
      try {
        await savePreferences(this.prefs)
      } finally {
        this.saving = false
      }
    },
  },
})
