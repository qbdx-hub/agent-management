<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAgentStore } from '@/stores/agent'
import { useUserStore } from '@/stores/user'
import { getMonitorOverview } from '@/api/monitor'
import type { MonitorOverview } from '@/types/monitor'
import AgentCard from './components/AgentCard.vue'
import RecentTasks from './components/RecentTasks.vue'
import QuickCreate from './components/QuickCreate.vue'
import { formatTokens, formatPercent } from '@/utils/format'

const router = useRouter()
const agentStore = useAgentStore()
const userStore = useUserStore()
const showCreateDialog = ref(false)

const overview = ref<MonitorOverview | null>(null)
const overviewLoading = ref(true)

const totalCount = computed(() => agentStore.list.length)
const publishedCount = computed(() => agentStore.list.filter(a => a.status === 'published').length)
const testingCount = computed(() => agentStore.list.filter(a => a.status === 'testing').length)
const draftCount = computed(() => agentStore.list.filter(a => a.status === 'draft').length)

// 页头状态一句话来自真实统计
const statusLine = computed(() => {
  const running = overview.value?.runningTaskCount ?? 0
  return running > 0
    ? `${totalCount.value} 个 Agent，${running} 个会话进行中`
    : `${totalCount.value} 个 Agent`
})

const nickname = computed(() => userStore.user?.nickname || userStore.user?.username || '用户')

onMounted(async () => {
  agentStore.fetchAgentList()
  try {
    const res = await getMonitorOverview('today')
    if (res.code === 0 && res.data) {
      overview.value = res.data
    }
  } catch { /* 概览数据取不到时显示占位 */ } finally {
    overviewLoading.value = false
  }
})

function handleAgentCreated(id: number) {
  showCreateDialog.value = false
  router.push(`/agents/${id}`)
}
</script>

<template>
  <div class="dashboard-page">
    <div class="page-head">
      <h1>欢迎回来，{{ nickname }}</h1>
      <span class="status-line">{{ statusLine }}</span>
    </div>

    <!-- 概览：四张等宽指标卡，数值 tabular，加载用骨架 -->
    <div class="overview">
      <div class="metric">
        <div class="label">Agent 总数</div>
        <div class="value num">{{ totalCount }}</div>
        <div class="delta">{{ draftCount }} 个草稿</div>
      </div>
      <div class="metric">
        <div class="label">已发布</div>
        <div class="value num">{{ publishedCount }}</div>
        <div class="delta">{{ testingCount }} 个调试中</div>
      </div>
      <div class="metric">
        <div class="label">进行中会话</div>
        <div class="value num accent">
          <el-skeleton v-if="overviewLoading" :rows="1" animated style="width:48px" />
          <template v-else>{{ overview ? overview.runningTaskCount : '—' }}</template>
        </div>
        <div class="delta">
          <template v-if="overview">今日调用 {{ overview.todayCallCount }} 次</template>
          <template v-else>&nbsp;</template>
        </div>
      </div>
      <div class="metric">
        <div class="label">今日 Token</div>
        <div class="value num">
          <el-skeleton v-if="overviewLoading" :rows="1" animated style="width:64px" />
          <template v-else>{{ overview ? formatTokens(overview.totalTokensToday) : '—' }}</template>
        </div>
        <div class="delta">
          <template v-if="overview">今日成功率 {{ formatPercent(overview.successRate) }}</template>
          <template v-else>&nbsp;</template>
        </div>
      </div>
    </div>

    <div class="content-layout">
      <!-- 左：我的 Agent（去掉外层卡片，网格直接落在页面上） -->
      <section class="agents-section">
        <div class="section-head">
          <h2>我的 Agent<span class="sub">共 {{ totalCount }} 个</span></h2>
          <el-button type="primary" @click="showCreateDialog = true">
            <UiIcon name="plus" />新建 Agent
          </el-button>
        </div>
        <div v-if="agentStore.list.length > 0" class="agent-grid">
          <AgentCard
            v-for="agent in agentStore.list"
            :key="agent.id"
            :agent="agent"
            @click="router.push(`/agents/${agent.id}/chat`)"
          />
        </div>
        <el-empty v-else description="还没有 Agent，点击右上角按钮创建第一个" />
      </section>

      <!-- 右：最近任务 + 快捷入口（原「快速操作」卡降级为链接列表） -->
      <aside class="side-col">
        <RecentTasks />
        <div class="panel">
          <div class="panel-head">快捷入口</div>
          <div class="quick-links">
            <div class="ql" @click="router.push('/tools/register')"><span>注册工具</span><span class="arrow">→</span></div>
            <div class="ql" @click="router.push('/monitor')"><span>查看监控</span><span class="arrow">→</span></div>
            <div class="ql" @click="router.push('/cost')"><span>成本报表</span><span class="arrow">→</span></div>
          </div>
        </div>
      </aside>
    </div>

    <QuickCreate v-model="showCreateDialog" @created="handleAgentCreated" />
  </div>
</template>

<style scoped>
.dashboard-page {
  max-width: 1280px;
  margin: 0 auto;
}

/* 页头 */
.page-head { margin-bottom: 24px; }
.page-head h1 {
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -0.3px;
  line-height: 1.3;
}
.status-line { font-size: 13.5px; color: var(--text-2); margin-top: 2px; display: block; }

/* 概览四卡 */
.overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 32px;
}
.metric {
  background: var(--bg-surface);
  border: 1px solid var(--border-1);
  border-radius: var(--r-card);
  box-shadow: var(--shadow-card);
  padding: 18px 20px;
}
.metric .label { font-size: 12.5px; color: var(--text-2); margin-bottom: 6px; }
.metric .value {
  font-size: 30px;
  font-weight: 800;
  letter-spacing: -0.8px;
  line-height: 1.2;
  min-height: 36px;
  display: flex;
  align-items: center;
}
.metric .value.accent { color: var(--accent); }
.metric .delta { font-size: 12px; color: var(--text-3); margin-top: 3px; min-height: 18px; }

/* 双栏 */
.content-layout {
  display: grid;
  grid-template-columns: 1fr 316px;
  gap: 28px;
  align-items: start;
}

/* 我的 Agent */
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.section-head h2 { font-size: 17px; font-weight: 800; letter-spacing: -0.2px; }
.section-head .sub { font-size: 13px; color: var(--text-3); margin-left: 8px; font-weight: 400; }
.agent-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18px;
}

/* 右栏 */
.side-col { display: flex; flex-direction: column; gap: 18px; }
.panel {
  background: var(--bg-surface);
  border: 1px solid var(--border-1);
  border-radius: var(--r-card);
  box-shadow: var(--shadow-card);
}
.panel-head {
  padding: 15px 18px;
  border-bottom: 1px solid var(--border-1);
  font-size: 14px;
  font-weight: 700;
}
.quick-links { padding: 8px 18px 12px; }
.ql {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 11px 0;
  font-size: 13.5px;
  color: var(--text-2);
  cursor: pointer;
  transition: color 0.15s ease-out;
}
.ql:hover { color: var(--accent); }
.ql + .ql { border-top: 1px solid var(--border-1); }
.ql .arrow { color: var(--text-3); }
.ql:hover .arrow { color: var(--accent); }

@media (max-width: 1200px) {
  .content-layout { grid-template-columns: 1fr; }
}
@media (max-width: 820px) {
  .overview { grid-template-columns: repeat(2, 1fr); }
  .agent-grid { grid-template-columns: 1fr; }
}
</style>
