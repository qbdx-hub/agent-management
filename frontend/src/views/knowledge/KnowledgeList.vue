<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { formatDateTime } from '@/utils/format'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listKnowledgeBases, deleteKnowledgeBase } from '@/api/knowledge'
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
  } catch (err: any) {
    ElMessage.error(err?.message || '加载知识库列表失败')
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

async function handleDelete(kb: KnowledgeBase, event: Event) {
  // 阻止卡片点击事件
  event.stopPropagation()

  try {
    await ElMessageBox.confirm(
      `确定删除知识库"${kb.name}"吗？删除后将同时清除所有关联文档，且不可恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    )
  } catch {
    return // 用户取消
  }

  try {
    const res = await deleteKnowledgeBase(kb.id)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      knowledgeBases.value = knowledgeBases.value.filter(item => item.id !== kb.id)
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '删除失败')
  }
}
</script>

<template>
  <div class="knowledge-list-page" v-loading="loading">
    <div class="page-header">
      <h2>知识库</h2>
      <el-button type="primary" @click="handleCreate"><UiIcon name="plus" /> 创建知识库</el-button>
    </div>
    <div class="card-grid">
      <el-card
        v-for="kb in knowledgeBases"
        :key="kb.id"
        shadow="never"
        class="kb-card"
        @click="goToDetail(kb.id)"
      >
        <div class="kb-header">
          <span class="kb-icon"><UiIcon name="notebook" :size="20" /></span>
          <div style="flex:1;min-width:0">
            <div class="kb-name">{{ kb.name }}</div>
            <div class="kb-desc">{{ kb.description || '暂无描述' }}</div>
          </div>
          <el-button
            type="danger"
            text
            size="small"
            @click="handleDelete(kb, $event)"
          >
            <UiIcon name="trash" />
          </el-button>
        </div>
        <div class="kb-meta">
          <span><UiIcon name="file-text" class="ii" /><span class="num">{{ kb.documentCount }}</span> 文档</span>
          <span><UiIcon name="coins" class="ii" /><span class="num">{{ kb.totalTokens }}</span> Token</span>
          <el-tag :type="kb.status === 'active' ? 'success' : kb.status === 'building' ? 'warning' : 'danger'" size="small" round>
            {{ kb.status === 'active' ? '正常' : kb.status === 'building' ? '构建中' : '异常' }}
          </el-tag>
        </div>
        <div class="kb-updated">更新于 {{ formatDateTime(kb.updatedAt) }}</div>
      </el-card>
    </div>
    <el-empty v-if="!loading && knowledgeBases.length === 0" description="暂无知识库，点击上方按钮创建" />
  </div>
</template>

<style scoped>
.knowledge-list-page { max-width: 1200px; margin: 0 auto; }
.page-header h2 {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.2px;
}
.kb-card {
  cursor: pointer;
  transition: transform 0.15s ease-out, box-shadow 0.15s ease-out;
}
.kb-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lift);
}
.kb-header { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.kb-icon {
  width: 40px; height: 40px; border-radius: 10px;
  background: var(--accent-soft); color: var(--accent);
  display: inline-flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.kb-name { font-weight: 700; font-size: 14.5px; color: var(--text-1); }
.kb-desc {
  font-size: 12px;
  color: var(--text-3);
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.kb-meta { display: flex; gap: 14px; font-size: 12px; color: var(--text-2); align-items: center; }
.kb-updated { font-size: 12px; color: var(--text-3); margin-top: 10px; }
@media (prefers-reduced-motion: reduce) {
  .kb-card { transition: none; }
  .kb-card:hover { transform: none; }
}
</style>
