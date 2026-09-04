<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getAgentList } from '@/api/agent'
import type { AgentSummary } from '@/types'
import AppIcon from '@/components/AppIcon.vue'
import AgentAvatar from '@/components/AgentAvatar.vue'

const router = useRouter()
const agents = ref<AgentSummary[]>([])
const keyword = ref('')
const loading = ref(true)

const filtered = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  if (!k) return agents.value
  return agents.value.filter((a) => a.name.toLowerCase().includes(k) || (a.description || '').toLowerCase().includes(k))
})

onMounted(async () => {
  try {
    const res = await getAgentList({ page: 1, pageSize: 50 })
    agents.value = res.data.list || []
  } catch {
    /* 拦截器已 toast */
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page">
    <div class="scroll">
      <div class="pad" style="padding-top: 12px">
        <div class="page-title">对话</div>
        <div class="search-box" style="margin-top: 14px">
          <AppIcon name="search" :size="15" />
          <input v-model="keyword" placeholder="搜索 Agent" />
        </div>

        <div style="margin-top: 16px; display: flex; flex-direction: column; gap: 12px">
          <div v-for="a in filtered" :key="a.id" class="agent-card" @click="router.push(`/chat/${a.id}`)">
            <AgentAvatar :name="a.name" :avatar="a.avatar" />
            <div class="info">
              <div class="name">{{ a.name }}</div>
              <div class="meta">{{ a.description || (a.config?.modelName || '开始新对话') }}</div>
            </div>
            <AppIcon name="arrow" :size="13" />
          </div>
        </div>
        <div v-if="!loading && !filtered.length" class="empty">{{ keyword ? '没有匹配的 Agent' : '暂无 Agent' }}</div>
        <div style="height: 24px" />
      </div>
    </div>
  </div>
</template>
