<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { formatDateTime } from '@/utils/format'
import { ElMessage } from 'element-plus'
import type { UploadFile, UploadRawFile } from 'element-plus'
import {
  getKnowledgeBase,
  listDocuments,
  uploadDocument,
  uploadDocumentByChunk,
  deleteDocument,
  validateFile,
  getFileExtension,
  MAX_FILE_SIZE,
  CHUNK_SIZE,
} from '@/api/knowledge'
import type { KnowledgeBase, Document } from '@/api/knowledge'

const route = useRoute()
const router = useRouter()
const kbId = Number(route.params.id)
const activeTab = ref('documents')
const loading = ref(false)

const kb = ref<KnowledgeBase | null>(null)
const documents = ref<Document[]>([])

// ==================== 数据加载 ====================

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
    // 忽略网络错误
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDetail()
})

// ==================== 文件上传逻辑 ====================

/** 上传状态 */
const uploading = ref(false)
/** 上传进度（0-100） */
const uploadProgress = ref(0)
/** 当前上传文件名（用于进度展示） */
const uploadingFileName = ref('')

/**
 * 文件选择/拖拽前的校验（el-upload :before-upload 钩子）
 * 校验文件格式和大小，返回 false 阻止自动上传
 */
function beforeUpload(file: File): boolean {
  const result = validateFile(file)
  if (!result.valid) {
    ElMessage.warning(result.message!)
    return false
  }
  return true
}

/**
 * 覆盖默认上传行为（el-upload :http-request 钩子）
 * 根据文件大小自动选择：小文件直接上传，大文件分片上传
 */
async function handleUpload(options: any): Promise<void> {
  const file = options.file as File

  // 校验
  const validation = validateFile(file)
  if (!validation.valid) {
    ElMessage.warning(validation.message!)
    return
  }

  uploading.value = true
  uploadProgress.value = 0
  uploadingFileName.value = file.name

  try {
    let res
    // 大于分片阈值时使用分片上传
    if (file.size > CHUNK_SIZE) {
      res = await uploadDocumentByChunk(kbId, file, (percent) => {
        uploadProgress.value = percent
      })
    } else {
      // 小文件直接上传
      res = await uploadDocument(kbId, file, (percent) => {
        uploadProgress.value = percent
      })
    }

    if (res.code === 0) {
      ElMessage.success(`"${file.name}" 上传成功`)
      // 上传完成后刷新文档列表
      await refreshDocuments()
    }
  } catch {
    // 忽略网络错误
  } finally {
    uploading.value = false
    uploadProgress.value = 0
    uploadingFileName.value = ''
  }
}

/**
 * 刷新文档列表（上传成功/删除后调用）
 */
async function refreshDocuments() {
  try {
    const res = await listDocuments(kbId)
    if (res.code === 0) {
      documents.value = res.data || []
    }
  } catch {
    // 忽略网络错误
  }
}

/**
 * 删除文档
 */
async function handleDeleteDoc(doc: Document) {
  try {
    const res = await deleteDocument(doc.id, kbId)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      documents.value = documents.value.filter(d => d.id !== doc.id)
    }
  } catch {
    // 忽略网络错误
  }
}

/** 已上传文件大小格式化 */
function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

// ==================== 检索测试 ====================

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

// ==================== 设置 ====================

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

// ==================== 工具函数 ====================

/** 状态标签映射 */
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
          <!-- 上传区域 -->
          <div style="margin-bottom:16px">
            <el-upload
              drag
              action="#"
              :auto-upload="true"
              :show-file-list="false"
              :before-upload="beforeUpload"
              :http-request="handleUpload"
              :disabled="uploading"
              accept=".pdf,.md,.txt,.json,.js,.ts,.jsx,.tsx,.py,.java,.go,.rs,.c,.cpp,.h,.hpp,.css,.scss,.less,.html,.xml,.yaml,.yml,.sh,.bat,.sql"
              multiple
            >
              <el-icon style="font-size:40px;color:#909399"><UploadFilled /></el-icon>
              <div>拖拽文件到此处，或 <em>点击上传</em></div>
              <div class="text-muted" style="font-size:12px">
                支持 PDF / Markdown / TXT / 代码文件，单文件最大 {{ MAX_FILE_SIZE / 1024 / 1024 }}MB
              </div>
            </el-upload>

            <!-- 上传进度条 -->
            <div v-if="uploading" class="upload-progress">
              <div class="progress-info">
                <el-icon class="is-loading"><Loading /></el-icon>
                <span>正在上传: {{ uploadingFileName }}</span>
                <span class="progress-percent">{{ uploadProgress }}%</span>
              </div>
              <el-progress :percentage="uploadProgress" :show-text="false" :stroke-width="6" />
            </div>
          </div>

          <!-- 文档表格 -->
          <el-table :data="documents">
            <el-table-column prop="name" label="文件名" min-width="200" show-overflow-tooltip />
            <el-table-column prop="chunkCount" label="分块数" width="80" align="center" />
            <el-table-column label="Token" width="90" align="center">
              <template #default="{ row }">{{ row.totalTokens }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="statusLabel(row.status).type as any" size="small">
                  {{ statusLabel(row.status).text }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="上传时间" width="160">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column label="" width="70" align="center">
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

/* 上传进度区域 */
.upload-progress {
  margin-top: 12px;
  padding: 12px 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
}
.progress-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--el-text-color-regular);
}
.progress-percent {
  margin-left: auto;
  font-weight: 500;
  color: var(--el-color-primary);
}
</style>
