<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { usePrefsStore } from '@/stores/prefs'
import { toast } from '@/utils/toast'
import AppIcon from '@/components/AppIcon.vue'
import ToggleSwitch from '@/components/ToggleSwitch.vue'

const router = useRouter()
const prefs = usePrefsStore()
const saving = ref(false)

const CHANNELS = [
  { id: 'app', label: 'App 推送' },
  { id: 'email', label: '邮件' },
]

function toggleChannel(id: string) {
  const list = prefs.prefs.notifications.channels
  const i = list.indexOf(id)
  if (i >= 0) list.splice(i, 1)
  else list.push(id)
}

async function save() {
  saving.value = true
  try {
    await prefs.save()
    toast('已保存')
  } catch {
    /* 拦截器已 toast */
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  if (!prefs.loaded) prefs.load().catch(() => {})
})
</script>

<template>
  <div class="page">
    <div class="navbar">
      <div class="back-btn" @click="router.back()">
        <AppIcon name="back" :size="16" />
      </div>
      <span class="title">通知设置</span>
    </div>

    <div class="scroll" style="padding: 16px">
      <div class="group-label" style="margin-top: 0">Agent 与任务</div>
      <div class="menu-group">
        <div class="menu-row">
          <div style="flex: 1">
            <div class="label">Agent 运行完成</div>
            <div style="font-size: 11px; color: var(--text-2); margin-top: 2px">任务结束后推送结果通知</div>
          </div>
          <ToggleSwitch v-model="prefs.prefs.notifications.agentFinished" />
        </div>
        <div class="menu-row">
          <div style="flex: 1">
            <div class="label">任务失败告警</div>
            <div style="font-size: 11px; color: var(--text-2); margin-top: 2px">执行出错时立即提醒</div>
          </div>
          <ToggleSwitch v-model="prefs.prefs.notifications.taskFailed" />
        </div>
        <div class="menu-row">
          <div style="flex: 1">
            <div class="label">实例告警</div>
            <div style="font-size: 11px; color: var(--text-2); margin-top: 2px">资源异常时提醒</div>
          </div>
          <ToggleSwitch v-model="prefs.prefs.notifications.instanceAlert" />
        </div>
        <div class="menu-row">
          <div style="flex: 1">
            <div class="label">Token 用量提醒</div>
            <div style="font-size: 11px; color: var(--text-2); margin-top: 2px">用量达到 80% 时提醒</div>
          </div>
          <ToggleSwitch v-model="prefs.prefs.notifications.tokenUsage80" />
        </div>
      </div>

      <div class="group-label">免打扰时段</div>
      <div class="menu-group">
        <div class="menu-row">
          <div style="flex: 1">
            <div class="label">启用免打扰</div>
            <div style="font-size: 11px; color: var(--text-2); margin-top: 2px">时段内仅接收失败告警</div>
          </div>
          <ToggleSwitch v-model="prefs.prefs.notifications.quietHours.enabled" />
        </div>
        <div v-if="prefs.prefs.notifications.quietHours.enabled" class="menu-row">
          <div style="flex: 1; display: flex; align-items: center; gap: 8px">
            <input v-model="prefs.prefs.notifications.quietHours.from" type="time" class="time-input" />
            <span style="color: var(--text-2)">至</span>
            <input v-model="prefs.prefs.notifications.quietHours.to" type="time" class="time-input" />
          </div>
        </div>
      </div>

      <div class="group-label">推送渠道</div>
      <div class="menu-group">
        <div v-for="c in CHANNELS" :key="c.id" class="menu-row">
          <span class="label">{{ c.label }}</span>
          <ToggleSwitch
            :model-value="prefs.prefs.notifications.channels.includes(c.id)"
            @update:model-value="() => toggleChannel(c.id)"
          />
        </div>
      </div>

      <button class="primary-btn" style="width: 100%; margin-top: 24px" :disabled="saving" @click="save">
        {{ saving ? '保存中…' : '保存设置' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.time-input {
  background: var(--comp);
  border: none;
  border-radius: 8px;
  color: var(--text-1);
  padding: 6px 8px;
  font-size: 13px;
  outline: none;
}
</style>
