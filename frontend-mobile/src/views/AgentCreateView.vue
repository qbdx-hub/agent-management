<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createAgent } from '@/api/agent'
import { getModelCatalog } from '@/api/model'
import { usePrefsStore } from '@/stores/prefs'
import type { ModelCatalogItem } from '@/types'
import { toast } from '@/utils/toast'
import AppIcon from '@/components/AppIcon.vue'
import RangeSlider from '@/components/RangeSlider.vue'

const router = useRouter()
const prefs = usePrefsStore()

const name = ref('')
const description = ref('')
const catalog = ref<ModelCatalogItem[]>([])
const selectedModel = ref('')
const temperature = ref(0.7)
const maxTokens = ref(4096)
const loading = ref(true)
const creating = ref(false)

async function load() {
  loading.value = true
  try {
    if (!prefs.loaded) await prefs.load().catch(() => {})
    temperature.value = prefs.prefs.temperature
    maxTokens.value = prefs.prefs.maxTokens
    const res = await getModelCatalog()
    catalog.value = res.data || []
    // 默认选用户偏好模型；偏好不在目录中则选第一个
    selectedModel.value = catalog.value.some((m) => m.modelName === prefs.prefs.defaultModel)
      ? prefs.prefs.defaultModel
      : catalog.value[0]?.modelName || ''
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

async function submit() {
  if (!name.value.trim()) return toast('请输入 Agent 名称', 'error')
  const m = catalog.value.find((x) => x.modelName === selectedModel.value)
  creating.value = true
  try {
    const res = await createAgent({
      name: name.value.trim(),
      description: description.value.trim() || undefined,
      modelProvider: m?.provider,
      modelName: m?.modelName,
      temperature: temperature.value,
      maxTokens: maxTokens.value,
    })
    toast('创建成功')
    // 直接进入与该 Agent 的对话
    router.replace(`/chat/${res.data.id}`)
  } catch {
    /* 拦截器已 toast */
  } finally {
    creating.value = false
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
      <span class="title">新建 Agent</span>
    </div>

    <div class="scroll">
      <div class="pad" style="padding-top: 4px">
        <div class="group-label" style="margin-top: 4px">基本信息</div>
        <div class="card" style="display: flex; flex-direction: column; gap: 12px">
          <input v-model="name" class="field" placeholder="Agent 名称（必填）" maxlength="100" />
          <textarea v-model="description" class="field area" rows="3" placeholder="描述这个 Agent 的职责（选填）" maxlength="500" />
        </div>

        <div class="group-label">模型配置</div>
        <div v-if="loading" class="empty">加载中…</div>
        <div v-else class="menu-group">
          <div
            v-for="m in catalog"
            :key="m.id"
            class="menu-row"
            style="padding: 14px 16px"
            @click="selectedModel = m.modelName"
          >
            <div class="radio-outer" :class="{ sel: selectedModel === m.modelName }">
              <div v-if="selectedModel === m.modelName" class="radio-inner" />
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

        <div class="group-label">生成参数</div>
        <div class="card">
          <div style="display: flex; justify-content: space-between; font-size: 13px">
            <span>随机性 (Temperature)</span>
            <span style="color: var(--brand); font-weight: 600">{{ temperature.toFixed(1) }}</span>
          </div>
          <RangeSlider v-model="temperature" :min="0" :max="2" :step="0.1" />
          <div class="slider-ends"><span>严谨 0</span><span>创意 2</span></div>

          <div style="display: flex; justify-content: space-between; font-size: 13px; margin-top: 18px">
            <span>最大输出长度</span>
            <span style="color: var(--brand); font-weight: 600">{{ maxTokens }} tokens</span>
          </div>
          <RangeSlider v-model="maxTokens" :min="256" :max="8192" :step="256" />
          <div class="slider-ends"><span>256</span><span>8192</span></div>
        </div>

        <div style="font-size: 11px; color: var(--text-2); margin-top: 12px">
          创建后即可在移动端直接对话；系统提示词、AI 端点与工具绑定可在 PC 端继续完善。
        </div>

        <button class="primary-btn" style="margin-top: 16px" :disabled="creating" @click="submit">
          {{ creating ? '创建中…' : '创建 Agent' }}
        </button>
        <div style="height: 8px" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.field {
  width: 100%;
  background: var(--comp);
  border: none;
  outline: none;
  border-radius: 10px;
  padding: 12px 14px;
  color: var(--text-1);
  font-size: 14px;
  resize: none;
}
.field::placeholder {
  color: var(--text-2);
}
.field.area {
  line-height: 1.6;
}
</style>
