<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { formatDateTime } from '@/utils/format'

const router = useRouter()
const knowledgeBases = ref([
  { id: 1, name: 'Agent 业务知识库', description: 'Agent 相关业务文档和规范', documentCount: 12, vectorCount: 350, boundAgentCount: 2, status: 'active', updatedAt: '2026-07-12T10:00:00+08:00' },
  { id: 2, name: '技术架构文档', description: '系统架构设计和技术选型文档', documentCount: 8, vectorCount: 210, boundAgentCount: 1, status: 'active', updatedAt: '2026-07-10T15:00:00+08:00' },
  { id: 3, name: 'API 接口规范', description: 'RESTful API 设计规范和示例', documentCount: 15, vectorCount: 480, boundAgentCount: 3, status: 'active', updatedAt: '2026-07-11T09:00:00+08:00' },
])

const showCreate = ref(false)
const newKB = ref({ name: '', description: '' })

function handleCreate() {
  if (!newKB.value.name) return
  knowledgeBases.value.push({ id: Date.now(), ...newKB.value, documentCount: 0, vectorCount: 0, boundAgentCount: 0, status: 'active', updatedAt: new Date().toISOString() })
  showCreate.value = false
  newKB.value = { name: '', description: '' }
}
</script>

<template>
  <div class="knowledge-list-page">
    <div class="page-header">
      <h2>知识库</h2>
      <el-button type="primary" @click="showCreate = true"><el-icon><Plus /></el-icon> 创建知识库</el-button>
    </div>
    <div class="card-grid">
      <el-card v-for="kb in knowledgeBases" :key="kb.id" shadow="hover" class="kb-card" @click="router.push(`/knowledge/${kb.id}`)">
        <div class="kb-header">
          <img src="/icons/04-notebook.png" class="kb-icon" alt="" />
          <div><div class="kb-name">{{ kb.name }}</div><div class="text-muted" style="font-size:12px">{{ kb.description }}</div></div>
        </div>
        <div class="kb-meta">
          <span><el-icon class="ii"><Document /></el-icon>{{ kb.documentCount }} 文档</span>
          <span><el-icon class="ii"><Histogram /></el-icon>{{ kb.vectorCount }} 向量</span>
          <span><el-icon class="ii"><Cpu /></el-icon>{{ kb.boundAgentCount }} Agent</span>
        </div>
        <div class="text-muted" style="font-size:12px;margin-top:8px">更新于 {{ formatDateTime(kb.updatedAt) }}</div>
      </el-card>
    </div>

    <el-dialog v-model="showCreate" title="创建知识库" width="450px">
      <el-form label-width="80px">
        <el-form-item label="名称"><el-input v-model="newKB.name" placeholder="例如：业务知识库" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="newKB.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showCreate = false">取消</el-button><el-button type="primary" @click="handleCreate">创建</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.knowledge-list-page { max-width: 1200px; }
.kb-card { cursor: pointer; transition: all 0.2s; }
.kb-card:hover { transform: translateY(-2px); }
.kb-header { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.kb-icon { width: 30px; height: 30px; object-fit: contain; }
.kb-name { font-weight: 600; font-size: 15px; }
.kb-meta { display: flex; gap: 16px; font-size: 12px; color: #909399; }
</style>
