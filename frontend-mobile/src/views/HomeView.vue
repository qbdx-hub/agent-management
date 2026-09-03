<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getAgentList } from '@/api/agent'
import { getMonitorOverview } from '@/api/monitor'
import { useAuthStore } from '@/stores/auth'
import type { AgentSummary, MonitorOverview } from '@/types'
import { formatTokens, timeAgo } from '@/utils/format'
import { toast } from '@/utils/toast'
import AppIcon from '@/components/AppIcon.vue'

const router = useRouter()
const auth = useAuthStore()

const overview = ref<MonitorOverview | null>(null)
const agents = ref<AgentSummary[]>([])
const totalAgents = ref(0)
const loading = ref(true)

const hour = new Date().getHours()
const greeting = hour < 6 ? '夜深了' : hour < 12 ? '早上好' : hour < 18 ? '下午好' : '晚上好'

/** 副标题：{n} 个 Agent 正在运行（对齐设计稿首页） */
const runningCount = computed(() => overview.value?.activeAgentCount ?? 0)
const subtitle = computed(() => `${runningCount.value} 个 Agent 正在运行`)

const QUICK_ACTIONS = [
  { label: '新建 Agent', icon: 'plus', act: () => router.push({ name: 'agent-create' }) },
  { label: '上传文件', icon: 'upload', act: () => router.push({ name: 'kb' }) },
  { label: '终端', icon: 'terminal', act: () => toast('移动端暂未开放终端') },
  { label: '更多', icon: 'dots', act: () => router.push({ name: 'profile' }) },
] as const

function statusOf(a: AgentSummary): { cls: string; label: string } {
  if (a.status === 'published') return { cls: 'tag-running', label: '运行中' }
  if (a.status === 'paused') return { cls: 'tag-paused', label: '已暂停' }
  return { cls: 'tag-paused', label: '草稿' }
}

onMounted(async () => {
  try {
    const [ov, ag] = await Promise.all([
      getMonitorOverview('today'),
      getAgentList({ page: 1, pageSize: 5 }),
    ])
    overview.value = ov.data
    agents.value = ag.data.list || []
    totalAgents.value = ag.data.total ?? agents.value.length
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
      <div class="pad" style="padding-top: calc(var(--safe-top) + 8px)">
        <!-- 顶部：问候 + 铃铛/头像（对齐设计稿） -->
        <div style="display: flex; align-items: center; gap: 12px">
          <div style="flex: 1">
            <div style="font-size: 22px; font-weight: 700">{{ greeting }}，{{ auth.displayName }}</div>
            <div style="font-size: 12px; color: var(--text-2); margin-top: 4px">{{ subtitle }}</div>
          </div>
          <div
            style="width: 36px; height: 36px; border-radius: 50%; background: var(--card); display: flex; align-items: center; justify-content: center; color: var(--text-1)"
            @click="router.push({ name: 'notify' })"
          >
            <AppIcon name="bell" :size="16" />
          </div>
          <div class="avatar sm">{{ auth.displayName.slice(0, 1) }}</div>
        </div>

        <!-- 蓝色渐变状态卡（对齐设计稿：大数字 + 分列统计） -->
        <div class="hero-card" style="margin-top: 16px">
          <div style="display: flex; justify-content: space-between; align-items: center">
            <span style="font-size: 14px; font-weight: 600">Agent 运行状态</span>
            <span class="live-badge"><i class="dot" />实时</span>
          </div>
          <div class="hero-big">
            <div class="num">{{ runningCount }}</div>
            <div class="side">运行中 · 共 {{ totalAgents }} 个 Agent</div>
          </div>
          <div class="hero-divider">
            <div>
              <div class="k">今日 Token 消耗</div>
              <div class="v">{{ overview ? formatTokens(overview.totalTokensToday) : '—' }}</div>
            </div>
            <div>
              <div class="k">今日调用</div>
              <div class="v">{{ overview?.todayCallCount ?? '—' }} 次</div>
            </div>
          </div>
        </div>

        <!-- 快捷操作四宫格（对齐设计稿） -->
        <div class="quick-row">
          <div v-for="q in QUICK_ACTIONS" :key="q.label" class="quick" @click="q.act()">
            <div class="ico"><AppIcon :name="q.icon" :size="17" /></div>
            <div>{{ q.label }}</div>
          </div>
        </div>

        <!-- 我的 Agent -->
        <div class="section-head">
          <span class="t">我的 Agent</span>
          <span class="more" @click="router.push({ name: 'chat-list' })">查看全部</span>
        </div>
        <div v-for="a in agents" :key="a.id" class="agent-card" @click="router.push(`/chat/${a.id}`)">
          <div class="avatar-ico" style="color: var(--brand); font-weight: 600">{{ a.name.slice(0, 1) }}</div>
          <div class="info">
            <div class="name">{{ a.name }}</div>
            <div class="meta">{{ a.config?.modelName || '未配置模型' }} · 活跃于 {{ timeAgo(a.updatedAt) }}</div>
          </div>
          <span class="tag" :class="statusOf(a).cls">{{ statusOf(a).label }}</span>
        </div>
        <div v-if="!loading && !agents.length" class="empty">还没有 Agent，去 PC 端创建一个吧</div>
      </div>
    </div>
  </div>
</template>
