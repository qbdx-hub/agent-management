import http from './index'
import type { ApiResponse, PaginatedData } from '@/types/common'
import type { Role, AuditLogItem, ApprovalRule, ApprovalItem, ApiKey } from '@/types/security'
import { mockRoles, mockAuditLogs, mockApprovalRules, mockApprovals, mockApiKeys } from '@/mock/security'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

export async function getRoles(): Promise<ApiResponse<Role[]>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: mockRoles }
  const res = await http.get<ApiResponse<Role[]>>('/security/roles')
  return res.data
}

export async function createRole(data: Omit<Role, 'id' | 'isSystem' | 'memberCount'>): Promise<ApiResponse<Role>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: { ...data, id: Date.now(), isSystem: false, memberCount: 0 } }
  const res = await http.post<ApiResponse<Role>>('/security/roles', data)
  return res.data
}

export async function deleteRole(roleId: number): Promise<ApiResponse<null>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: null }
  const res = await http.delete<ApiResponse<null>>(`/security/roles/${roleId}`)
  return res.data
}

export async function getAuditLogs(params: any): Promise<ApiResponse<PaginatedData<AuditLogItem>>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: { list: mockAuditLogs, total: mockAuditLogs.length, page: 1, pageSize: 20 } }
  const res = await http.get<ApiResponse<PaginatedData<AuditLogItem>>>('/security/audit-logs', { params })
  return res.data
}

export async function getApprovalRules(): Promise<ApiResponse<ApprovalRule[]>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: mockApprovalRules }
  const res = await http.get<ApiResponse<ApprovalRule[]>>('/security/approvals/rules')
  return res.data
}

export async function getPendingApprovals(): Promise<ApiResponse<PaginatedData<ApprovalItem>>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: { list: mockApprovals.filter(a => a.status === 'pending'), total: mockApprovals.length, page: 1, pageSize: 20 } }
  const res = await http.get<ApiResponse<PaginatedData<ApprovalItem>>>('/security/approvals/pending')
  return res.data
}

export async function approveApproval(approvalId: number, comment?: string): Promise<ApiResponse<null>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: null }
  const res = await http.post<ApiResponse<null>>(`/security/approvals/${approvalId}/approve`, { comment })
  return res.data
}

export async function rejectApproval(approvalId: number, comment?: string): Promise<ApiResponse<null>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: null }
  const res = await http.post<ApiResponse<null>>(`/security/approvals/${approvalId}/reject`, { comment })
  return res.data
}

export async function getApiKeys(): Promise<ApiResponse<ApiKey[]>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: mockApiKeys }
  const res = await http.get<ApiResponse<ApiKey[]>>('/security/api-keys')
  return res.data
}

export async function createApiKey(data: { provider: string; keyName: string; apiKey: string }): Promise<ApiResponse<ApiKey>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: { id: Date.now(), ...data, apiKeyMasked: data.apiKey.slice(0, 3) + '***' + data.apiKey.slice(-4), isDefault: false, lastUsedAt: null, createdBy: 1, createdAt: new Date().toISOString() } }
  const res = await http.post<ApiResponse<ApiKey>>('/security/api-keys', data)
  return res.data
}

export async function deleteApiKey(keyId: number): Promise<ApiResponse<null>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: null }
  const res = await http.delete<ApiResponse<null>>(`/security/api-keys/${keyId}`)
  return res.data
}
