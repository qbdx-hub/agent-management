<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { usePrefsStore } from '@/stores/prefs'
import { getModelCatalog } from '@/api/model'
import type { ModelCatalogItem } from '@/types'
import { toast } from '@/utils/toast'
import AppIcon from '@/components/AppIcon.vue'
import RangeSlider from '@/components/RangeSlider.vue'

const router = useRouter()
const prefs = usePrefsStore()
const saving = ref(false)

/** 系统真实接入的模型（backend model_pricing，GET /models） */
const catalog = ref<ModelCatalogItem[]>([])
const loading = ref(true)
const STYLES = ['简洁', '均衡', '详细']

async function load() {
  loading.value = true
  try {
    const res = await getModelCatalog()
    catalog.value = res.data || []
    // 已存偏好不在目录中（如旧假数据）时回落到第一个可用模型
    if (catalog.value.length && !catalog.value.some((m) => m.modelName === prefs.prefs.defaultModel)) {
      prefs.prefs.defaultModel = catalog.value[0].modelName
    }
  } catch {
    /* 拦截器已 toast */
  } finally {
    loading.value = false
  }
}

function priceText(m: ModelCatalogItem): string {
  const fmt = (v: number) => (v >= 1 ? v.toFixed(2) : v.toFixed(4)).replace(/0+$/, '').replace(/\.$/, '')
  return `输入 $${fmt(m.inputPricePer1k)} / 输出 $${fmt(m.outputPricePer1k)} 每 1K token`
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
  load()
})
</script>

<template>
  <div class="page">
    <div class="navbar">
      <div class="back-btn" @click="router.back()">
        <AppIcon name="back" :size="16" />
      </div>
      <span class="title">模型偏好</span>
    </div>

    <div class="scroll">
      <div class="pad" style="padding-top: 4px">
        <div class="group-label" style="margin-top: 4px">默认模型</div>
        <div v-if="loading" class="empty">加载中…</div>
        <div v-else class="menu-group">
          <div
            v-for="m in catalog"
            :key="m.id"
            class="menu-row"
            style="padding: 14px 16px"
            @click="prefs.prefs.defaultModel = m.modelName"
          >
            <div class="radio-outer" :class="{ sel: prefs.prefs.defaultModel === m.modelName }">
              <div v-if="prefs.prefs.defaultModel === m.modelName" class="radio-inner" />
            </div>
            <div class="info">
              <div class="name" style="font-size: 14px">
                {{ m.displayName || m.modelName }}
                <span class="tag tag-reco" style="font-size: 10px">{{ m.provider }}</span>
              </div>
              <div class="meta" style="font-size: 11px">{{ m.modelName }} · {{ priceText(m) }}</div>
            </div>
          </div>
        </div>
        <div style="font-size: 11px; color: var(--text-2); margin-top: 10px">
          以上为平台已接入的模型（OpenAI 兼容协议）。此偏好将作为新建 Agent 的默认模型；已有 Agent 可在 PC 端单独调整。
        </div>

        <div class="group-label">生成参数</div>
        <div class="card">
          <div style="display: flex; justify-content: space-between; font-size: 13px">
            <span>随机性 (Temperature)</span>
            <span style="color: var(--brand); font-weight: 600">{{ prefs.prefs.temperature.toFixed(1) }}</span>
          </div>
          <RangeSlider v-model="prefs.prefs.temperature" :min="0" :max="2" :step="0.1" />
          <div class="slider-ends"><span>严谨 0</span><span>创意 2</span></div>

          <div style="display: flex; justify-content: space-between; font-size: 13px; margin-top: 18px">
            <span>最大输出长度</span>
            <span style="color: var(--brand); font-weight: 600">{{ prefs.prefs.maxTokens }} tokens</span>
          </div>
          <RangeSlider v-model="prefs.prefs.maxTokens" :min="256" :max="8192" :step="256" />
          <div class="slider-ends"><span>256</span><span>8192</span></div>

          <div style="font-size: 13px; margin: 18px 0 10px">回复风格</div>
          <div class="segmented">
            <div
              v-for="s in STYLES"
              :key="s"
              class="seg-item"
              :class="{ active: prefs.prefs.replyStyle === s }"
              @click="prefs.prefs.replyStyle = s"
            >
              {{ s }}
            </div>
          </div>
        </div>

        <button class="primary-btn" :disabled="saving" @click="save">
          {{ saving ? '保存中…' : '保存偏好' }}
        </button>
      </div>
    </div>
  </div>
</template>
