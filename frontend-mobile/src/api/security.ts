import http from './http'
import type { ApiResponse, ApiKeyItem } from '@/types'

export async function listApiKeys(): Promise<ApiResponse<ApiKeyItem[]>> {
  const res = await http.get<ApiResponse<ApiKeyItem[]>>('/security/api-keys')
  return res.data
}

/** 创建密钥：明文只在本次响应中返回（data.key） */
export async function createApiKey(name: string): Promise<ApiResponse<ApiKeyItem>> {
  const res = await http.post<ApiResponse<ApiKeyItem>>('/security/api-keys', { name })
  return res.data
}

export async function updateApiKeyStatus(id: number, enabled: boolean): Promise<ApiResponse<null>> {
  const res = await http.put<ApiResponse<null>>(`/security/api-keys/${id}/status`, { enabled })
  return res.data
}

export async function deleteApiKey(id: number): Promise<ApiResponse<null>> {
  const res = await http.delete<ApiResponse<null>>(`/security/api-keys/${id}`)
  return res.data
}
