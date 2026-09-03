<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { toast } from '@/utils/toast'
import AppIcon from '@/components/AppIcon.vue'

const router = useRouter()
const keyword = ref('')
const open = ref<number | null>(0)

/** 与原型 faqs 对应，答案按当前平台实际功能撰写（GPU/云工作站已替换为知识库） */
const FAQS = [
  {
    q: '如何创建一个新的 Agent？',
    a: '在 PC 端「Agent 管理」页点击新建，配置模型、系统提示词与绑定工具后发布。移动端可查看 Agent 列表并直接发起对话。',
  },
  {
    q: 'API 密钥如何配置和使用？',
    a: '在「我的 → API 密钥管理」中创建密钥（明文仅展示一次），随后在第三方系统中以 Bearer Token 方式调用平台 OpenAI 兼容接口。密钥可随时停用或删除。',
  },
  {
    q: '知识库支持哪些文件格式？',
    a: '支持 PDF、Markdown、纯文本、JSON 及主流代码文件（单文件 ≤ 50MB）。上传后系统自动分块并向量化，可在知识库详情页检索验证。',
  },
  {
    q: '对话记录会保存吗？',
    a: '会。每个 Agent 的会话历史保存在服务端，再次进入对话时自动恢复最近一次会话，可继续追问。',
  },
]

const filtered = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  if (!k) return FAQS
  return FAQS.filter((f) => f.q.toLowerCase().includes(k) || f.a.toLowerCase().includes(k))
})

function feedback() {
  toast('感谢你的反馈，我们会尽快处理')
}
</script>

<template>
  <div class="page">
    <div class="navbar">
      <div class="back-btn" @click="router.back()">
        <AppIcon name="back" :size="16" />
      </div>
      <span class="title">帮助与反馈</span>
    </div>

    <div class="scroll">
      <div class="pad" style="padding-top: 4px">
        <div class="search-box" style="margin-top: 4px">
          <AppIcon name="search" :size="15" />
          <input v-model="keyword" placeholder="搜索常见问题" />
        </div>

        <div class="group-label">常见问题</div>
        <div v-for="(f, i) in filtered" :key="i" class="faq-row" style="flex-direction: column; align-items: stretch">
          <div style="display: flex; align-items: center; gap: 12px" @click="open = open === i ? null : i">
            <span style="flex: 1; font-size: 14px">{{ f.q }}</span>
            <div class="arrow" :style="{ transform: open === i ? 'rotate(90deg)' : 'none', transition: 'transform .2s' }">
              <AppIcon name="arrow" :size="12" />
            </div>
          </div>
          <div v-if="open === i" style="font-size: 13px; color: var(--text-2); line-height: 1.6; margin-top: 8px">
            {{ f.a }}
          </div>
        </div>
        <div v-if="!filtered.length" class="empty">没有匹配的问题</div>

        <button class="primary-btn" style="margin-top: 16px" @click="feedback">提交意见反馈</button>
        <div class="version">MyAgent v1.0 (Build 20260902)</div>
      </div>
    </div>
  </div>
</template>
