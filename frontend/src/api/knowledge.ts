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

/** 上传进度回调 */
export interface UploadProgressCallback {
  (percent: number): void
}

// ==================== 文件校验常量 ====================

/** 允许的文件扩展名 */
export const ALLOWED_FILE_EXTENSIONS = [
  '.pdf', '.md', '.txt', '.json',
  '.js', '.ts', '.jsx', '.tsx',
  '.py', '.java', '.go', '.rs',
  '.c', '.cpp', '.h', '.hpp',
  '.css', '.scss', '.less',
  '.html', '.xml', '.yaml', '.yml',
  '.sh', '.bat', '.sql',
]

/** 允许的 MIME 类型（兜底，部分浏览器可能不准确） */
export const ALLOWED_MIME_TYPES = [
  'application/pdf',
  'text/plain',
  'text/markdown',
  'text/x-markdown',
  'application/json',
  'text/javascript',
  'application/javascript',
  'text/typescript',
  'application/typescript',
  'text/x-python',
  'text/x-java-source',
  'text/html',
  'text/css',
  'text/xml',
  'application/xml',
  'text/x-sql',
  'application/x-yaml',
  'text/yaml',
]

/** 单文件大小限制（50MB，与后端 spring.servlet.multipart.max-file-size 一致） */
export const MAX_FILE_SIZE = 50 * 1024 * 1024

// ==================== 文件校验工具 ====================

/**
 * 获取文件扩展名（小写，含点号）
 */
export function getFileExtension(filename: string): string {
  const lastDot = filename.lastIndexOf('.')
  if (lastDot === -1) return ''
  return filename.slice(lastDot).toLowerCase()
}

/**
 * 校验文件格式是否合法
 * @returns { valid: boolean, message?: string }
 */
export function validateFileType(file: File): { valid: boolean; message?: string } {
  const ext = getFileExtension(file.name)
  if (!ALLOWED_FILE_EXTENSIONS.includes(ext)) {
    return {
      valid: false,
      message: `不支持的文件格式: ${ext}，支持: ${ALLOWED_FILE_EXTENSIONS.join(' ')}`,
    }
  }
  return { valid: true }
}

/**
 * 校验文件大小是否超限
 * @returns { valid: boolean, message?: string }
 */
export function validateFileSize(file: File): { valid: boolean; message?: string } {
  if (file.size > MAX_FILE_SIZE) {
    const maxMB = MAX_FILE_SIZE / 1024 / 1024
    return {
      valid: false,
      message: `文件大小 ${(file.size / 1024 / 1024).toFixed(1)}MB 超出限制，最大 ${maxMB}MB`,
    }
  }
  return { valid: true }
}

/**
 * 综合校验文件（格式 + 大小）
 */
export function validateFile(file: File): { valid: boolean; message?: string } {
  const typeResult = validateFileType(file)
  if (!typeResult.valid) return typeResult

  const sizeResult = validateFileSize(file)
  if (!sizeResult.valid) return sizeResult

  return { valid: true }
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

/**
 * DELETE /knowledge-bases/:id —— 删除知识库（级联删除文档）
 */
export async function deleteKnowledgeBase(id: number): Promise<ApiResponse<null>> {
  const res = await http.delete<ApiResponse<null>>(`/knowledge-bases/${id}`)
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
 * 单文件上传（小文件，直接上传）
 * POST /knowledge-bases/:id/documents
 * @param kbId      知识库 ID
 * @param file      文件对象
 * @param onProgress 上传进度回调（0-100）
 */
export async function uploadDocument(
  kbId: number,
  file: File,
  onProgress?: UploadProgressCallback,
): Promise<ApiResponse<Document>> {
  const formData = new FormData()
  formData.append('file', file)

  const res = await http.post<ApiResponse<Document>>(
    `/knowledge-bases/${kbId}/documents`,
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(percent)
        }
      },
    },
  )
  return res.data
}

/**
 * DELETE /knowledge-bases/:kbId/documents/:docId —— 删除文档
 * @param docId 文档 ID
 * @param kbId  知识库 ID
 */
export async function deleteDocument(docId: number, kbId: number): Promise<ApiResponse<null>> {
  const res = await http.delete<ApiResponse<null>>(`/knowledge-bases/${kbId}/documents/${docId}`)
  return res.data
}

/** 知识检索（向量搜索） */
export async function searchKnowledge(kbId: number, query: string, topK = 5): Promise<ApiResponse<any[]>> {
  const res = await http.get<ApiResponse<any[]>>(`/knowledge-bases/${kbId}/search`, { params: { q: query, topK } })
  return res.data
}

/** 触发文档处理（分块+向量化） */
export async function processDocument(kbId: number, docId: number): Promise<ApiResponse<null>> {
  const res = await http.post<ApiResponse<null>>(`/knowledge-bases/${kbId}/documents/${docId}/process`)
  return res.data
}
