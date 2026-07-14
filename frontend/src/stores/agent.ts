import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'
import * as agentApi from '@/api/agent'
import type { AgentSummary, AgentDetail, CreateAgentDTO, UpdateAgentDTO, AgentStatus } from '@/types/agent'

export const useAgentStore = defineStore('agent', () => {
  const list = ref<AgentSummary[]>([])
  const current = ref<AgentDetail | null>(null)
  const loading = ref(false)
  const filter = reactive({ keyword: '', status: '' as AgentStatus | '', tag: '' })
  const pagination = reactive({ page: 1, pageSize: 20, total: 0 })

  async function fetchAgentList() {
    loading.value = true
    try {
      const res = await agentApi.getAgentList({ ...pagination, ...filter })
      if (res.code === 0) {
        list.value = res.data.list
        pagination.total = res.data.total
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchAgentDetail(id: number) {
    loading.value = true
    try {
      const res = await agentApi.getAgentDetail(id)
      if (res.code === 0) {
        current.value = res.data
      }
    } finally {
      loading.value = false
    }
  }

  async function createAgent(data: CreateAgentDTO): Promise<number> {
    const res = await agentApi.createAgent(data)
    if (res.code === 0) {
      await fetchAgentList()
      return res.data.id
    }
    throw new Error('创建失败')
  }

  async function updateAgent(id: number, data: UpdateAgentDTO) {
    const res = await agentApi.updateAgent(id, data)
    if (res.code === 0) {
      if (current.value?.id === id) {
        await fetchAgentDetail(id)
      }
      await fetchAgentList()
    }
  }

  async function deleteAgent(id: number) {
    const res = await agentApi.deleteAgent(id)
    if (res.code === 0) {
      await fetchAgentList()
    }
  }

  async function updateAgentStatus(id: number, status: AgentStatus) {
    const res = await agentApi.updateAgentStatus(id, status)
    if (res.code === 0) {
      if (current.value?.id === id) {
        current.value.status = status
      }
      await fetchAgentList()
    }
  }

  return { list, current, loading, filter, pagination, fetchAgentList, fetchAgentDetail, createAgent, updateAgent, deleteAgent, updateAgentStatus }
})
