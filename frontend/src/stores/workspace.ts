import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as workspaceApi from '@/api/workspace'
import type { WorkspaceItem, Member } from '@/types/workspace'

export const useWorkspaceStore = defineStore('workspace', () => {
  const currentId = ref<number | null>(Number(localStorage.getItem('workspaceId')) || null)
  const current = ref<WorkspaceItem | null>(null)
  const list = ref<WorkspaceItem[]>([])
  const members = ref<Member[]>([])

  const isReady = computed(() => !!currentId.value)

  async function fetchMyWorkspaces() {
    const res = await workspaceApi.getMyWorkspaces()
    if (res.code === 0) {
      list.value = res.data
      // 如果没有选中空间，默认选第一个
      if (!currentId.value && list.value.length > 0) {
        switchWorkspace(list.value[0].id)
      }
    }
  }

  function switchWorkspace(id: number) {
    currentId.value = id
    localStorage.setItem('workspaceId', String(id))
    const found = list.value.find(w => w.id === id)
    if (found) current.value = found
  }

  async function fetchMembers() {
    if (!currentId.value) return
    const res = await workspaceApi.getMembers()
    if (res.code === 0) {
      members.value = res.data
    }
  }

  async function inviteMember(email: string, role: string) {
    const res = await workspaceApi.inviteMember(email, role)
    if (res.code === 0) {
      await fetchMembers()
    }
  }

  async function removeMember(userId: number) {
    const res = await workspaceApi.removeMember(userId)
    if (res.code === 0) {
      members.value = members.value.filter(m => m.userId !== userId)
    }
  }

  return { currentId, current, list, members, isReady, fetchMyWorkspaces, switchWorkspace, fetchMembers, inviteMember, removeMember }
})
