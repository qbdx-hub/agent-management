import http from './http'
import type { ApiResponse, KnowledgeBase, KbDocument, SearchChunk } from '@/types'

/** 单文件大小限制（50MB，与后端 spring.servlet.multipart.max-file-size 一致） */
export const MAX_FILE_SIZE = 50 * 1024 * 1024

const ALLOWED_EXTENSIONS = [
  '.pdf', '.md', '.txt', '.json',
  '.js', '.ts', '.jsx', '.tsx',
  '.py', '.java', '.go', '.rs',
  '.c', '.cpp', '.h', '.hpp',
  '.css', '.scss', '.less',
  '.html', '.xml', '.yaml', '.yml',
  '.sh', '.bat', '.sql',
]

export function validateFile(file: File): { valid: boolean; message?: string } {
  const lastDot = file.name.lastIndexOf('.')
  const ext = lastDot === -1 ? '' : file.name.slice(lastDot).toLowerCase()
  if (!ALLOWED_EXTENSIONS.includes(ext)) {
    return { valid: false, message: `不支持的文件格式: ${ext || '（无扩展名）'}` }
  }
  if (file.size > MAX_FILE_SIZE) {
    return { valid: false, message: `文件 ${(file.size / 1024 / 1024).toFixed(1)}MB 超出 ${MAX_FILE_SIZE / 1024 / 1024}MB 限制` }
  }
  return { valid: true }
}

export async function listKnowledgeBases(): Promise<ApiResponse<KnowledgeBase[]>> {
  const res = await http.get<ApiResponse<KnowledgeBase[]>>('/knowledge-bases')
  return res.data
}

export async function getKnowledgeBase(id: number): Promise<ApiResponse<KnowledgeBase>> {
  const res = await http.get<ApiResponse<KnowledgeBase>>(`/knowledge-bases/${id}`)
  return res.data
}

export async function createKnowledgeBase(data: { name: string; description?: string; type?: string }): Promise<ApiResponse<KnowledgeBase>> {
  const res = await http.post<ApiResponse<KnowledgeBase>>('/knowledge-bases', data)
  return res.data
}

export async function deleteKnowledgeBase(id: number): Promise<ApiResponse<null>> {
  const res = await http.delete<ApiResponse<null>>(`/knowledge-bases/${id}`)
  return res.data
}

export async function listDocuments(kbId: number): Promise<ApiResponse<KbDocument[]>> {
  const res = await http.get<ApiResponse<KbDocument[]>>(`/knowledge-bases/${kbId}/documents`)
  return res.data
}

/** 上传文档（multipart，带进度回调） */
export async function uploadDocument(
  kbId: number,
  file: File,
  onProgress?: (percent: number) => void
): Promise<ApiResponse<KbDocument>> {
  const formData = new FormData()
  formData.append('file', file)
  const res = await http.post<ApiResponse<KbDocument>>(`/knowledge-bases/${kbId}/documents`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (e) => {
      if (onProgress && e.total) onProgress(Math.round((e.loaded * 100) / e.total))
    },
  })
  return res.data
}

export async function deleteDocument(kbId: number, docId: number): Promise<ApiResponse<null>> {
  const res = await http.delete<ApiResponse<null>>(`/knowledge-bases/${kbId}/documents/${docId}`)
  return res.data
}

/** 知识检索（向量搜索，topK 默认 5） */
export async function searchKnowledge(kbId: number, q: string, topK = 5): Promise<ApiResponse<SearchChunk[]>> {
  const res = await http.get<ApiResponse<SearchChunk[]>>(`/knowledge-bases/${kbId}/search`, { params: { q, topK } })
  return res.data
}

/** 触发文档处理（分块+向量化） */
export async function processDocument(kbId: number, docId: number): Promise<ApiResponse<null>> {
  const res = await http.post<ApiResponse<null>>(`/knowledge-bases/${kbId}/documents/${docId}/process`)
  return res.data
}
