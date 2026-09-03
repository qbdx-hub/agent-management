<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { createKnowledgeBase, listKnowledgeBases } from '@/api/kb'
import type { KnowledgeBase } from '@/types'
import { formatTokens, timeAgo } from '@/utils/format'
import { toast } from '@/utils/toast'
import AppIcon from '@/components/AppIcon.vue'

const router = useRouter()
const kbs = ref<KnowledgeBase[]>([])
const loading = ref(true)
const keyword = ref('')
const showCreate = ref(false)
const form = ref({ name: '', description: '' })
const creating = ref(false)

const STATUS_MAP: Record<string, { cls: string; label: string }> = {
  active: { cls: 'tag-running', label: '可用' },
  building: { cls: 'tag-warn', label: '构建中' },
  error: { cls: 'tag-stopped', label: '异常' },
}

const filtered = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  if (!k) return kbs.value
  return kbs.value.filter((b) => b.name.toLowerCase().includes(k) || (b.description || '').toLowerCase().includes(k))
})

/** hero 汇总（对齐工作站屏的绿色状态卡） */
const totalDocs = computed(() => kbs.value.reduce((s, b) => s + (b.documentCount || 0), 0))
const totalTokens = computed(() => kbs.value.reduce((s, b) => s + (b.totalTokens || 0), 0))

async function load() {
  loading.value = true
  try {
    const res = await listKnowledgeBases()
    kbs.value = res.data || []
  } catch {
    /* 拦截器已 toast */
  } finally {
    loading.value = false
  }
}

async function create() {
  if (!form.value.name.trim()) return toast('请输入知识库名称', 'error')
  creating.value = true
  try {
    await createKnowledgeBase({ name: form.value.name.trim(), description: form.value.description.trim() || undefined, type: 'vector' })
    toast('创建成功')
    showCreate.value = false
    form.value = { name: '', description: '' }
    await load()
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
    <div class="pad" style="padding-top: calc(var(--safe-top) + 8px)">
      <div class="page-title">知识库</div>
      <div class="search-box" style="margin-top: 12px">
        <AppIcon name="search" :size="15" />
        <input v-model="keyword" placeholder="搜索知识库" />
      </div>
    </div>

    <div class="scroll">
      <div class="pad">
        <!-- 绿色总览卡（对齐工作站屏） -->
        <div class="hero-card hero-green" style="margin-top: 14px">
          <div style="display: flex; justify-content: space-between; align-items: center">
            <span style="font-size: 14px; font-weight: 600">知识库总览</span>
            <span class="live-badge"><i class="dot" />实时</span>
          </div>
          <div style="font-size: 12px; opacity: .85; margin-top: 6px">
            {{ kbs.length }} 个知识库 · {{ totalDocs }} 篇文档已向量化
          </div>
          <div class="hero-divider">
            <div>
              <div class="k">知识库</div>
              <div class="v">{{ kbs.length }} 个</div>
            </div>
            <div>
              <div class="k">文档总数</div>
              <div class="v">{{ totalDocs }} 篇</div>
            </div>
            <div>
              <div class="k">知识总量</div>
              <div class="v">{{ formatTokens(totalTokens) }}</div>
            </div>
          </div>
        </div>

        <div class="section-head">
          <span class="t">我的知识库</span>
        </div>

        <div v-if="loading" class="empty">加载中…</div>
        <template v-else>
          <div v-for="kb in filtered" :key="kb.id" class="agent-card" @click="router.push(`/kb/${kb.id}`)">
            <div class="avatar-ico" style="color: var(--brand)"><AppIcon name="doc" :size="17" /></div>
            <div class="info">
              <div class="name">{{ kb.name }}</div>
              <div class="meta">{{ kb.documentCount }} 个文档 · {{ timeAgo(kb.updatedAt) }}</div>
            </div>
            <span class="tag" :class="STATUS_MAP[kb.status]?.cls || 'tag-paused'">{{ STATUS_MAP[kb.status]?.label || kb.status }}</span>
          </div>
          <div v-if="!filtered.length" class="empty">{{ keyword ? '没有匹配的知识库' : '还没有知识库，点击下方按钮创建' }}</div>
        </template>

        <button class="primary-btn" style="margin-top: 16px" @click="showCreate = true">＋ 新建知识库</button>
        <div style="height: 8px" />
      </div>
    </div>

    <div v-if="showCreate" class="modal-mask" @click.self="showCreate = false">
      <div class="modal">
        <h4>新建知识库</h4>
        <input v-model="form.name" placeholder="名称（必填）" />
        <textarea v-model="form.description" rows="3" placeholder="描述（选填）" style="margin-top: 12px" />
        <div class="row">
          <button class="btn-plain" @click="showCreate = false">取消</button>
          <button class="btn-main" :disabled="creating" @click="create">{{ creating ? '创建中…' : '创建' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>
