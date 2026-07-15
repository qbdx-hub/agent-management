<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { formatDateTime } from '@/utils/format'
import { ElMessage } from 'element-plus'
import { getKnowledgeBase, listDocuments, uploadDocument, deleteDocument } from '@/api/knowledge'
import type { KnowledgeBase, Document } from '@/api/knowledge'

const route = useRoute()
const router = useRouter()
const kbId = Number(route.params.id)
const activeTab = ref('documents')
const loading = ref(false)

const kb = ref<KnowledgeBase | null>(null)
const documents = ref<Document[]>([])

async function loadDetail() {
  loading.value = true
  try {
    const [kbRes, docRes] = await Promise.all([
      getKnowledgeBase(kbId),
      listDocuments(kbId),
    ])
    if (kbRes.code === 0) kb.value = kbRes.data
    if (docRes.code === 0) documents.value = docRes.data || []
  } catch {
    // 错误已由 http 拦截器统一提示
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDetail()
})

// 文档上传
async function handleUpload(options: any) {
  try {
    const res = await uploadDocument(kbId, options.file)
    if (res.code === 0) {
      ElMessage.success('文档上传成功')
      documents.value.unshift(res.data)
    }
  } catch {
    // 错误已由 http 拦截器统一提示
  }
}

async function handleDeleteDoc(doc: Document) {
  try {
    const res = await deleteDocument(doc.id)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      documents.value = documents.value.filter(d => d.id !== doc.id)
    }
  } catch {
    // 错误已由 http 拦截器统一提示
  }
}

// 检索测试
const searchQuery = ref('')
const searchResults = ref<any[]>([])
const searching = ref(false)

async function handleSearch() {
  if (!searchQuery.value) return
  searching.value = true
  // TODO: 接入后端向量化搜索接口
  searchResults.value = [
    { chunkIndex: 3, documentTitle: '示例文档.pdf', content: '检索接口待后端实现...', score: 0.92 },
  ]
  searching.value = false
}

// 设置
const settingsForm = ref({
  embeddingModel: '',
  chunkSize: 500,
  chunkOverlap: 50,
})

function initSettings() {
  if (kb.value) {
    settingsForm.value.embeddingModel = kb.value.embeddingModel || ''
    settingsForm.value.chunkSize = kb.value.config?.chunk_size || 500
    settingsForm.value.chunkOverlap = kb.value.config?.chunk_overlap || 50
  }
}

function handleSaveSettings() {
  // TODO: 接入后端更新接口
  ElMessage.success('设置保存成功（接口待实现）')
}

// 状态标签
function statusLabel(status: string) {
  const map: Record<string, { text: string; type: string }> = {
    pending: { text: '待处理', type: 'info' },
    processing: { text: '处理中', type: 'warning' },
    completed: { text: '已完成', type: 'success' },
    failed: { text: '失败', type: 'danger' },
  }
  return map[status] || { text: status, type: 'info' }
}
</script>

<template>
  <div class="knowledge-detail-page" v-loading="loading">
    <div class="page-header">
      <div style="display:flex;align-items:center;gap:8px">
        <el-button text @click="router.push('/knowledge')"><el-icon><ArrowLeft /></el-icon></el-button>
        <h2>{{ kb?.name || '知识库详情' }}</h2>
      </div>
    </div>
    <el-card>
      <el-tabs v-model="activeTab" @tab-change="tab => { if (tab === 'settings') initSettings() }">
        <el-tab-pane label="文档" name="documents">
          <div style="margin-bottom:16px">
            <el-upload drag action="#" :auto-upload="false" accept=".pdf,.md,.txt,.json,.js,.ts,.py,.java" :http-request="handleUpload">
              <el-icon style="font-size:40px;color:#909399"><UploadFilled /></el-icon>
              <div>拖拽文件到此处，或 <em>点击上传</em></div>
              <div class="text-muted" style="font-size:12px">支持 PDF / Markdown / TXT / 代码文件</div>
            </el-upload>
          </div>
          <el-table :data="documents">
            <el-table-column prop="name" label="文件名" />
            <el-table-column prop="chunkCount" label="分块数" width="80" />
            <el-table-column label="Token" width="80"><template #default="{ row }">{{ row.totalTokens }}</template></el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusLabel(row.status).type as any" size="small">{{ statusLabel(row.status).text }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="上传时间" width="160"><template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template></el-table-column>
            <el-table-column label="" width="70">
              <template #default="{ row }">
                <el-button text type="danger" size="small" @click="handleDeleteDoc(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="documents.length === 0" description="暂无文档，请上传文件" />
        </el-tab-pane>

        <el-tab-pane label="检索测试" name="search">
          <div style="display:flex;gap:8px;margin-bottom:16px">
            <el-input v-model="searchQuery" placeholder="输入检索问题..." @keyup.enter="handleSearch" />
            <el-button type="primary" :loading="searching" @click="handleSearch">检索</el-button>
          </div>
          <div v-for="(r, idx) in searchResults" :key="idx" class="search-result">
            <div class="result-header">
              <span>{{ r.documentTitle }} · Chunk #{{ r.chunkIndex }}</span>
              <el-tag size="small">{{ (r.score * 100).toFixed(0) }}%</el-tag>
            </div>
            <div class="result-content text-muted">{{ r.content }}</div>
          </div>
          <el-empty v-if="searchResults.length === 0 && searchQuery" description="没有找到相关内容" />
        </el-tab-pane>

        <el-tab-pane label="设置" name="settings">
          <el-form label-width="120px" style="max-width:500px">
            <el-form-item label="Embedding 模型">
              <el-input v-model="settingsForm.embeddingModel" disabled />
            </el-form-item>
            <el-form-item label="分块大小">
              <el-input-number v-model="settingsForm.chunkSize" :min="100" :max="2000" :step="100" />
            </el-form-item>
            <el-form-item label="分块重叠">
              <el-input-number v-model="settingsForm.chunkOverlap" :min="0" :max="200" :step="10" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSaveSettings">保存设置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.knowledge-detail-page { max-width: 1200px; }
.search-result { padding: 12px; border: 1px solid var(--el-border-color-lighter); border-radius: 6px; margin-bottom: 12px; }
.result-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; font-weight: 500; }
.result-content { font-size: 13px; line-height: 1.6; }
</style>
