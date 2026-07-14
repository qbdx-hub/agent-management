import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const globalLoading = ref(false)

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setLoading(v: boolean) {
    globalLoading.value = v
  }

  return { sidebarCollapsed, globalLoading, toggleSidebar, setLoading }
})
