<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getTokenTrend } from '@/api/monitor'
import { getCostOverview } from '@/api/cost'
import { formatTokens } from '@/utils/format'
import AppIcon from '@/components/AppIcon.vue'
import ToggleSwitch from '@/components/ToggleSwitch.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import { useThemeStore } from '@/stores/theme'

const router = useRouter()
const auth = useAuthStore()
const theme = useThemeStore()
const showLogout = ref(false)

const monthTokens = ref<number | null>(null)
const budgetPercent = ref<number | null>(null)

/** 有预算才显示用量进度条（无预算概念时不造假数据） */
const showBar = computed(() => budgetPercent.value !== null)

onMounted(async () => {
  try {
    const [trend, cost] = await Promise.all([
      getTokenTrend('30d', 'day'),
      getCostOverview('this_month'),
    ])
    monthTokens.value = trend.data.summary.totalInput + trend.data.summary.totalOutput
    if (cost.data.budgetLimit > 0) budgetPercent.value = Math.min(100, Math.round(cost.data.budgetPercent))
  } catch {
    /* 拦截器已 toast */
  }
})

function logout() {
  showLogout.value = false
  auth.logout()
  router.replace({ name: 'login' })
}
</script>

<template>
  <div class="page">
    <div class="scroll">
      <div class="pad" style="padding-top: calc(var(--safe-top) + 8px)">
        <div class="page-title" style="margin-bottom: 14px">我的</div>

        <div class="profile-card" style="cursor: pointer" @click="router.push({ name: 'profile-edit' })">
          <UserAvatar />
          <div style="flex: 1">
            <div style="font-size: 17px; font-weight: 700">{{ auth.displayName }}</div>
            <div style="font-size: 12px; color: var(--text-2); margin-top: 3px">
              {{ auth.user?.role === 'ADMIN' ? '管理员' : '成员' }} · {{ auth.user?.workspaces?.[0]?.name || auth.user?.username }}
            </div>
          </div>
          <div class="arrow"><AppIcon name="arrow" :size="13" /></div>
        </div>

        <!-- 本月 Token 用量（对齐设计稿） -->
        <div class="card" style="margin-top: 12px">
          <div style="display: flex; justify-content: space-between; align-items: center">
            <span style="font-size: 13px; color: var(--text-2)">本月 Token 用量</span>
            <span class="tag tag-reco">{{ auth.user?.role === 'ADMIN' ? '管理员' : '专业版' }}</span>
          </div>
          <div style="font-size: 22px; font-weight: 700; margin-top: 8px">
            {{ monthTokens !== null ? formatTokens(monthTokens) : '—' }}
            <span v-if="showBar" style="font-size: 13px; color: var(--text-2); font-weight: 400"> / 已用 {{ budgetPercent }}%</span>
          </div>
          <div v-if="showBar" class="bar"><i :style="{ width: budgetPercent + '%' }" /></div>
          <div style="font-size: 11px; color: var(--text-2); margin-top: 8px">统计范围：近 30 天所有会话的输入与输出 Token</div>
        </div>

        <div class="menu-group" style="margin-top: 12px">
          <div class="menu-row" @click="router.push({ name: 'keys' })">
            <div class="menu-ico"><AppIcon name="key" :size="14" /></div>
            <span class="label">API 密钥管理</span>
            <div class="arrow"><AppIcon name="arrow" :size="13" /></div>
          </div>
          <div class="menu-row" @click="router.push({ name: 'models' })">
            <div class="menu-ico"><AppIcon name="sliders" :size="14" /></div>
            <span class="label">模型偏好</span>
            <div class="arrow"><AppIcon name="arrow" :size="13" /></div>
          </div>
          <div class="menu-row" @click="router.push({ name: 'notify' })">
            <div class="menu-ico"><AppIcon name="bell" :size="14" /></div>
            <span class="label">通知设置</span>
            <div class="arrow"><AppIcon name="arrow" :size="13" /></div>
          </div>
          <div class="menu-row" @click="router.push({ name: 'help' })">
            <div class="menu-ico"><AppIcon name="help" :size="14" /></div>
            <span class="label">帮助与反馈</span>
            <div class="arrow"><AppIcon name="arrow" :size="13" /></div>
          </div>
          <div class="menu-row" @click="theme.toggle()">
            <div class="menu-ico"><AppIcon :name="theme.isDark ? 'sun' : 'moon'" :size="14" /></div>
            <span class="label">深色模式</span>
            <span @click.stop>
              <ToggleSwitch :model-value="theme.isDark" @update:model-value="theme.toggle()" />
            </span>
          </div>
        </div>

        <div class="menu-group" style="margin-top: 12px">
          <div class="menu-row" @click="showLogout = true">
            <div class="menu-ico" style="background: rgba(227,77,89,.15); color: var(--danger)"><AppIcon name="arrow" :size="14" style="transform: rotate(180deg)" /></div>
            <span class="label" style="color: var(--danger)">退出登录</span>
          </div>
        </div>

        <div class="version">MyAgent v1.0 · 移动端</div>
      </div>
    </div>

    <div v-if="showLogout" class="modal-mask" @click.self="showLogout = false">
      <div class="modal">
        <h4>退出登录</h4>
        <div style="font-size: 13px; color: var(--text-2)">确定退出当前账号？</div>
        <div class="row">
          <button class="btn-plain" @click="showLogout = false">取消</button>
          <button class="btn-main" @click="logout">退出</button>
        </div>
      </div>
    </div>
  </div>
</template>
