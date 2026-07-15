<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { formatDateTime } from '@/utils/format'
import { listKnowledgeBases } from '@/api/knowledge'
import type { KnowledgeBase } from '@/api/knowledge'

const router = useRouter()
const knowledgeBases = ref<KnowledgeBase[]>([])
const loading = ref(false)

async function loadList() {
  loading.value = true
  try {
    const res = await listKnowledgeBases()
    if (res.code === 0) {
      knowledgeBases.value = res.data || []
    }
  } catch {
    // 错误已由 http 拦截器统一提示
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadList()
})

function handleCreate() {
  router.push('/knowledge/create')
}

function goToDetail(id: number) {
  router.push(`/knowledge/${id}`)
}
</script>

<template>
  <div class="knowledge-list-page" v-loading="loading">
    <div class="page-header">
      <h2>知识库</h2>
      <el-button type="primary" @click="handleCreate"><el-icon><Plus /></el-icon> 创建知识库</el-button>
    </div>
    <div class="card-grid">
      <el-card
        v-for="kb in knowledgeBases"
        :key="kb.id"
        shadow="hover"
        class="kb-card"
        @click="goToDetail(kb.id)"
      >
        <div class="kb-header">
          <span class="kb-icon">📚</span>
          <div>
            <div class="kb-name">{{ kb.name }}</div>
            <div class="text-muted" style="font-size:12px">{{ kb.description || '暂无描述' }}</div>
          </div>
        </div>
        <div class="kb-meta">
          <span>📄 {{ kb.documentCount }} 文档</span>
          <span>🔢 {{ kb.totalTokens }} Token</span>
          <el-tag :type="kb.status === 'active' ? 'success' : kb.status === 'building' ? 'warning' : 'danger'" size="small">
            {{ kb.status === 'active' ? '正常' : kb.status === 'building' ? '构建中' : '异常' }}
          </el-tag>
        </div>
        <div class="text-muted" style="font-size:12px;margin-top:8px">更新于 {{ formatDateTime(kb.updatedAt) }}</div>
      </el-card>
    </div>
    <el-empty v-if="!loading && knowledgeBases.length === 0" description="暂无知识库，点击上方按钮创建" />
  </div>
</template>

<style scoped>
.knowledge-list-page { max-width: 1200px; }
.kb-card { cursor: pointer; transition: all 0.2s; }
.kb-card:hover { transform: translateY(-2px); }
.kb-header { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.kb-icon { font-size: 28px; }
.kb-name { font-weight: 600; font-size: 15px; }
.kb-meta { display: flex; gap: 16px; font-size: 12px; color: #909399; align-items: center; }
</style>
