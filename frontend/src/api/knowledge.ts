import http from './index'
import type { ApiResponse, PaginatedData } from '@/types/common'

// ==================== 类型定义 ====================

/** 知识库配置（JSON 字段） */
export interface KnowledgeBaseConfig {
  chunk_size?: number
  chunk_overlap?: number
  [key: string]: any
}

/** 知识库实体（与后端 KnowledgeBase 一一对应） */
export interface KnowledgeBase {
  id: number
  workspaceId: number
  name: string
  description: string
  type: 'vector' | 'keyword' | 'hybrid'
  embeddingModel: string
  documentCount: number
  totalTokens: number
  status: 'active' | 'building' | 'error'
  config: KnowledgeBaseConfig | null
  createdBy: number
  createdAt: string
  updatedAt: string
}

/** 创建知识库请求体 */
export interface KnowledgeBaseCreatePayload {
  name: string
  description?: string
  type?: 'vector' | 'keyword' | 'hybrid'
  embeddingModel?: string
  config?: KnowledgeBaseConfig
}

/** 文档实体（与后端 Document 一一对应） */
export interface Document {
  id: number
  knowledgeBaseId: number
  name: string
  fileType: string
  fileSize: number
  fileUrl: string
  chunkCount: number
  totalTokens: number
  status: 'pending' | 'processing' | 'completed' | 'failed'
  error: string | null
  metadata: Record<string, any> | null
  uploadedBy: number
  createdAt: string
  updatedAt: string
}

// ==================== 知识库接口 ====================

/**
 * POST /knowledge-bases —— 创建知识库
 */
export async function createKnowledgeBase(payload: KnowledgeBaseCreatePayload): Promise<ApiResponse<KnowledgeBase>> {
  const res = await http.post<ApiResponse<KnowledgeBase>>('/knowledge-bases', payload)
  return res.data
}

/**
 * GET /knowledge-bases —— 查询当前工作空间下的知识库列表
 */
export async function listKnowledgeBases(): Promise<ApiResponse<KnowledgeBase[]>> {
  const res = await http.get<ApiResponse<KnowledgeBase[]>>('/knowledge-bases')
  return res.data
}

/**
 * GET /knowledge-bases/:id —— 查询单条知识库详情
 */
export async function getKnowledgeBase(id: number): Promise<ApiResponse<KnowledgeBase>> {
  const res = await http.get<ApiResponse<KnowledgeBase>>(`/knowledge-bases/${id}`)
  return res.data
}

// ==================== 文档接口 ====================

/**
 * GET /knowledge-bases/:id/documents —— 查询知识库下的文档列表
 */
export async function listDocuments(kbId: number): Promise<ApiResponse<Document[]>> {
  const res = await http.get<ApiResponse<Document[]>>(`/knowledge-bases/${kbId}/documents`)
  return res.data
}

/**
 * POST /knowledge-bases/:id/documents —— 上传文档
 */
export async function uploadDocument(kbId: number, file: File): Promise<ApiResponse<Document>> {
  const formData = new FormData()
  formData.append('file', file)
  const res = await http.post<ApiResponse<Document>>(`/knowledge-bases/${kbId}/documents`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return res.data
}

/**
 * DELETE /documents/:id —— 删除文档
 */
export async function deleteDocument(docId: number): Promise<ApiResponse<null>> {
  const res = await http.delete<ApiResponse<null>>(`/documents/${docId}`)
  return res.data
}
