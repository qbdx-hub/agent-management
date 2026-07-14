<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { formatDateTime } from '@/utils/format'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const kbId = Number(route.params.id)
const activeTab = ref('documents')

const kb = ref({ id: kbId, name: 'Agent 业务知识库', description: 'Agent 相关业务文档和规范', embeddingModel: 'text-embedding-3-small', chunkSize: 500, chunkOverlap: 50 })

const documents = ref([
  { id: 1, title: 'Agent 配置指南.pdf', chunks: 28, tokenCount: 3200, status: 'indexed', uploadedAt: '2026-07-10T09:00:00+08:00' },
  { id: 2, title: 'API 接口文档.md', chunks: 45, tokenCount: 5800, status: 'indexed', uploadedAt: '2026-07-11T10:00:00+08:00' },
  { id: 3, title: '系统架构设计.md', chunks: 32, tokenCount: 4100, status: 'processing', uploadedAt: '2026-07-12T14:00:00+08:00' },
])

const searchQuery = ref('')
const searchResults = ref<any[]>([])

function handleSearch() {
  if (!searchQuery.value) return
  searchResults.value = [
    { chunkIndex: 3, documentTitle: 'Agent 配置指南.pdf', content: '配置 Agent 需要以下步骤：1. 选择模型 2. 编写 System Prompt 3. 绑定工具...', score: 0.92 },
    { chunkIndex: 7, documentTitle: 'API 接口文档.md', content: 'POST /agents 创建新的 Agent，请求体包含 name、description、modelProvider...', score: 0.85 },
  ]
}
</script>

<template>
  <div class="knowledge-detail-page">
    <div class="page-header">
      <div style="display:flex;align-items:center;gap:8px">
        <el-button text @click="router.push('/knowledge')"><el-icon><ArrowLeft /></el-icon></el-button>
        <h2>{{ kb.name }}</h2>
      </div>
    </div>
    <el-card>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="文档" name="documents">
          <div style="margin-bottom:16px">
            <el-upload drag action="#" :auto-upload="false" accept=".pdf,.md,.txt,.json,.js,.ts,.py,.java">
              <el-icon style="font-size:40px;color:#909399"><UploadFilled /></el-icon>
              <div>拖拽文件到此处，或 <em>点击上传</em></div>
              <div class="text-muted" style="font-size:12px">支持 PDF / Markdown / TXT / 代码文件</div>
            </el-upload>
          </div>
          <el-table :data="documents">
            <el-table-column prop="title" label="文件名" />
            <el-table-column prop="chunks" label="分块数" width="80" />
            <el-table-column label="Token" width="80"><template #default="{ row }">{{ row.tokenCount }}</template></el-table-column>
            <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="row.status === 'indexed' ? 'success' : 'warning'" size="small">{{ row.status === 'indexed' ? '已索引' : '处理中' }}</el-tag></template></el-table-column>
            <el-table-column label="上传时间" width="160"><template #default="{ row }">{{ formatDateTime(row.uploadedAt) }}</template></el-table-column>
            <el-table-column label="" width="70"><template #default><el-button text type="danger" size="small">删除</el-button></template></el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="检索测试" name="search">
          <div style="display:flex;gap:8px;margin-bottom:16px">
            <el-input v-model="searchQuery" placeholder="输入检索问题..." @keyup.enter="handleSearch" />
            <el-button type="primary" @click="handleSearch">检索</el-button>
          </div>
          <div v-for="(r, idx) in searchResults" :key="idx" class="search-result">
            <div class="result-header"><span>{{ r.documentTitle }} · Chunk #{{ r.chunkIndex }}</span><el-tag size="small">{{ (r.score * 100).toFixed(0) }}%</el-tag></div>
            <div class="result-content text-muted">{{ r.content }}</div>
          </div>
          <el-empty v-if="searchResults.length === 0 && searchQuery" description="没有找到相关内容" />
        </el-tab-pane>

        <el-tab-pane label="设置" name="settings">
          <el-form label-width="120px" style="max-width:500px">
            <el-form-item label="Embedding 模型"><el-input :model-value="kb.embeddingModel" disabled /></el-form-item>
            <el-form-item label="分块大小"><el-input-number :model-value="kb.chunkSize" :min="100" :max="2000" :step="100" /></el-form-item>
            <el-form-item label="分块重叠"><el-input-number :model-value="kb.chunkOverlap" :min="0" :max="200" :step="10" /></el-form-item>
            <el-form-item><el-button type="primary">保存设置</el-button></el-form-item>
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
