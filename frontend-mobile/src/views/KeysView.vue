<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createApiKey, deleteApiKey, listApiKeys, updateApiKeyStatus } from '@/api/security'
import type { ApiKeyItem } from '@/types'
import { formatDate } from '@/utils/format'
import { toast } from '@/utils/toast'
import AppIcon from '@/components/AppIcon.vue'
import ToggleSwitch from '@/components/ToggleSwitch.vue'

const router = useRouter()
const keys = ref<ApiKeyItem[]>([])
const loading = ref(true)
const showCreate = ref(false)
const name = ref('')
const creating = ref(false)
const created = ref<ApiKeyItem | null>(null) // 明文只展示一次

async function load() {
  loading.value = true
  try {
    const res = await listApiKeys()
    keys.value = res.data || []
  } catch {
    /* 拦截器已 toast */
  } finally {
    loading.value = false
  }
}

async function create() {
  if (!name.value.trim()) return toast('请输入密钥名称', 'error')
  creating.value = true
  try {
    const res = await createApiKey(name.value.trim())
    created.value = res.data
    showCreate.value = false
    name.value = ''
    await load()
  } catch {
    /* 拦截器已 toast */
  } finally {
    creating.value = false
  }
}

async function copyKey() {
  if (!created.value?.key) return
  try {
    await navigator.clipboard.writeText(created.value.key)
    toast('密钥已复制到剪贴板')
  } catch {
    toast('复制失败，请长按手动复制', 'error')
  }
}

async function toggle(item: ApiKeyItem, enabled: boolean) {
  try {
    await updateApiKeyStatus(item.id, enabled)
    item.status = enabled ? 'active' : 'disabled'
    toast(enabled ? '已启用' : '已停用')
  } catch {
    /* 拦截器已 toast */
  }
}

async function remove(item: ApiKeyItem) {
  try {
    await deleteApiKey(item.id)
    toast('已删除')
    await load()
  } catch {
    /* 拦截器已 toast */
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <div class="navbar">
      <div class="back-btn" @click="router.back()">
        <AppIcon name="back" :size="16" />
      </div>
      <span class="title">API 密钥管理</span>
    </div>

    <div class="scroll">
      <div class="pad" style="padding-top: 4px">
        <div class="tip-card">
          密钥用于调用灵枢agent OpenAPI，请妥善保管，切勿泄露。<br />
          最多可创建 5 个密钥，泄露后请立即删除重建。
        </div>

        <div v-if="loading" class="empty">加载中…</div>

        <div v-for="k in keys" :key="k.id" class="card" style="margin-top: 14px">
          <div style="display: flex; justify-content: space-between; align-items: center">
            <span style="font-size: 14px; font-weight: 600">{{ k.name }}</span>
            <span class="tag" :class="k.status === 'active' ? 'tag-running' : 'tag-stopped'">
              {{ k.status === 'active' ? '使用中' : '已停用' }}
            </span>
          </div>
          <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 10px">
            <span class="key-mask">{{ k.mask }}</span>
            <ToggleSwitch :model-value="k.status === 'active'" @update:model-value="(v) => toggle(k, v)" />
          </div>
          <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 8px">
            <span style="font-size: 11px; color: var(--text-2)">创建于 {{ formatDate(k.createdAt) }}</span>
            <span style="font-size: 12px; color: var(--danger)" @click="remove(k)">删除</span>
          </div>
        </div>

        <div v-if="!loading && !keys.length" class="empty">还没有密钥</div>

        <button class="ghost-btn" style="margin-top: 20px" @click="showCreate = true">＋ 新建密钥</button>
      </div>
    </div>

    <!-- 创建弹窗 -->
    <div v-if="showCreate" class="modal-mask" @click.self="showCreate = false">
      <div class="modal">
        <h4>新建密钥</h4>
        <input v-model="name" placeholder="密钥名称（如：测试环境）" />
        <div class="row">
          <button class="btn-plain" @click="showCreate = false">取消</button>
          <button class="btn-main" :disabled="creating" @click="create">{{ creating ? '创建中…' : '创建' }}</button>
        </div>
      </div>
    </div>

    <!-- 明文展示弹窗（仅一次） -->
    <div v-if="created" class="modal-mask">
      <div class="modal">
        <h4>请立即保存密钥</h4>
        <div style="font-size: 12px; color: var(--text-2)">关闭后将无法再次查看明文。</div>
        <div class="key-mask" style="background: var(--comp); border-radius: 10px; padding: 12px; margin-top: 12px; word-break: break-all">
          {{ created.key }}
        </div>
        <div class="row">
          <button class="btn-plain" @click="copyKey">复制</button>
          <button class="btn-main" @click="created = null">我已保存</button>
        </div>
      </div>
    </div>
  </div>
</template>
