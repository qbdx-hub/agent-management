<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { deleteDocument, getKnowledgeBase, listDocuments, searchKnowledge, uploadDocument, validateFile } from '@/api/kb'
import type { KbDocument, KnowledgeBase, SearchChunk } from '@/types'
import { formatFileSize, timeAgo } from '@/utils/format'
import { toast } from '@/utils/toast'
import AppIcon from '@/components/AppIcon.vue'

const route = useRoute()
const router = useRouter()
const kbId = Number(route.params.id)

const kb = ref<KnowledgeBase | null>(null)
const docs = ref<KbDocument[]>([])
const loading = ref(true)
const uploading = ref(false)
const uploadPercent = ref(0)
const fileInput = ref<HTMLInputElement | null>(null)

// 检索
const query = ref('')
const searching = ref(false)
const chunks = ref<(SearchChunk & { documentName?: string })[]>([])
const searched = ref(false)

const DOC_STATUS: Record<string, { cls: string; label: string }> = {
  completed: { cls: 'tag-running', label: '已就绪' },
  processing: { cls: 'tag-warn', label: '处理中' },
  pending: { cls: 'tag-paused', label: '排队中' },
  failed: { cls: 'tag-stopped', label: '失败' },
}

const docNameMap = computed(() => Object.fromEntries(docs.value.map((d) => [d.id, d.name])))

async function load() {
  loading.value = true
  try {
    const [kd, dl] = await Promise.all([getKnowledgeBase(kbId), listDocuments(kbId)])
    kb.value = kd.data
    docs.value = dl.data || []
  } catch {
    /* 拦截器已 toast */
  } finally {
    loading.value = false
  }
}

function pickFile() {
  fileInput.value?.click()
}

async function onFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  const check = validateFile(file)
  if (!check.valid) return toast(check.message!, 'error')
  uploading.value = true
  uploadPercent.value = 0
  try {
    await uploadDocument(kbId, file, (p) => (uploadPercent.value = p))
    toast('上传成功，正在后台解析')
    await load()
  } catch {
    /* 拦截器已 toast */
  } finally {
    uploading.value = false
  }
}

async function removeDoc(doc: KbDocument) {
  try {
    await deleteDocument(kbId, doc.id)
    toast('已删除')
    await load()
  } catch {
    /* 拦截器已 toast */
  }
}

async function search() {
  const q = query.value.trim()
  if (!q || searching.value) return
  searching.value = true
  searched.value = true
  try {
    const res = await searchKnowledge(kbId, q, 5)
    chunks.value = (res.data || []).map((c) => ({ ...c, documentName: docNameMap.value[c.documentId] }))
  } catch {
    /* 拦截器已 toast */
  } finally {
    searching.value = false
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
      <span class="title">{{ kb?.name || '知识库' }}</span>
      <span class="tag" :class="kb?.status === 'active' ? 'tag-running' : 'tag-warn'" style="margin-right: 4px">
        {{ kb?.status === 'active' ? '可用' : kb?.status === 'building' ? '构建中' : '异常' }}
      </span>
    </div>

    <div class="scroll" style="padding: 16px">
      <!-- 检索 -->
      <div class="search-box">
        <AppIcon name="search" :size="15" />
        <input v-model="query" placeholder="在知识库中检索…" @keydown.enter="search" />
        <button v-if="!searching" style="background: none; border: none; color: var(--brand); font-size: 13px" @click="search">搜索</button>
      </div>

      <template v-if="searched">
        <div class="group-label">检索结果（Top 5）</div>
        <div v-if="!chunks.length && !searching" class="empty">没有命中的内容</div>
        <div v-for="c in chunks" :key="c.chunkId" class="card" style="margin-bottom: 12px">
          <div style="display: flex; justify-content: space-between; font-size: 12px; color: var(--text-2)">
            <span>{{ c.documentName || `文档 #${c.documentId}` }}</span>
            <span>相关度 {{ (c.score * 100).toFixed(0) }}%</span>
          </div>
          <div style="font-size: 13px; margin-top: 8px; white-space: pre-wrap; word-break: break-word">{{ c.content }}</div>
        </div>
      </template>

      <!-- 文档列表 -->
      <div class="section-head" style="margin-top: 20px">
        <span class="t">文档（{{ docs.length }}）</span>
        <span class="more" @click="pickFile">
          <AppIcon name="upload" :size="13" style="vertical-align: -2px" /> 上传
        </span>
      </div>

      <div v-if="uploading" class="card" style="margin-bottom: 12px">
        <div style="font-size: 13px">上传中… {{ uploadPercent }}%</div>
        <div class="bar"><i :style="{ width: uploadPercent + '%' }" /></div>
      </div>

      <div v-for="d in docs" :key="d.id" class="agent-card" style="padding: 12px">
        <div class="avatar-ico" style="background: var(--comp); color: var(--text-2)">{{ (d.fileType || 'txt').replace('.', '').toUpperCase().slice(0, 4) }}</div>
        <div class="info">
          <div class="name">{{ d.name }}</div>
          <div class="meta">{{ formatFileSize(d.fileSize) }} · {{ d.chunkCount }} 块 · {{ timeAgo(d.createdAt) }}</div>
        </div>
        <div style="display: flex; align-items: center; gap: 10px">
          <span class="tag" :class="DOC_STATUS[d.status]?.cls || 'tag-paused'">{{ DOC_STATUS[d.status]?.label || d.status }}</span>
          <span style="color: var(--text-2)" @click="removeDoc(d)"><AppIcon name="trash" :size="15" /></span>
        </div>
      </div>
      <div v-if="!loading && !docs.length && !uploading" class="empty">暂无文档，点右上角「上传」</div>

      <div style="height: 24px" />
    </div>

    <input ref="fileInput" type="file" hidden accept=".pdf,.md,.txt,.json,.js,.ts,.py,.java,.go,.rs,.c,.cpp,.h,.css,.html,.xml,.yaml,.yml,.sh,.bat,.sql" @change="onFileChange" />
  </div>
</template>
