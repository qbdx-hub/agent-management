import http from './index'
import type { ApiResponse, PaginatedData } from '@/types/common'
import type { WorkspaceItem, WorkspaceSettings, Member, ActivityLog } from '@/types/workspace'
import { mockWorkspaces, mockWorkspaceSettings, mockMembers, mockActivities } from '@/mock/workspace'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

export async function getMyWorkspaces(): Promise<ApiResponse<WorkspaceItem[]>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: mockWorkspaces }
  const res = await http.get<ApiResponse<WorkspaceItem[]>>('/workspaces')
  return res.data
}

export async function createWorkspace(data: { name: string; description: string }): Promise<ApiResponse<WorkspaceItem>> {
  if (USE_MOCK) {
    const item: WorkspaceItem = { id: Date.now(), ...data, role: 'ADMIN', memberCount: 1, agentCount: 0, createdAt: new Date().toISOString() }
    return { code: 0, message: 'ok', data: item }
  }
  const res = await http.post<ApiResponse<WorkspaceItem>>('/workspaces', data)
  return res.data
}

export async function getWorkspaceSettings(): Promise<ApiResponse<WorkspaceSettings>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: mockWorkspaceSettings }
  const res = await http.get<ApiResponse<WorkspaceSettings>>(`/workspaces/${localStorage.getItem('workspaceId')}/settings`)
  return res.data
}

export async function updateWorkspaceSettings(settings: Partial<WorkspaceSettings>): Promise<ApiResponse<null>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: null }
  const res = await http.put<ApiResponse<null>>(`/workspaces/${localStorage.getItem('workspaceId')}/settings`, settings)
  return res.data
}

export async function getMembers(): Promise<ApiResponse<Member[]>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: mockMembers }
  const res = await http.get<ApiResponse<Member[]>>(`/workspaces/${localStorage.getItem('workspaceId')}/members`)
  return res.data
}

export async function inviteMember(email: string, role: string): Promise<ApiResponse<null>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: null }
  const res = await http.post<ApiResponse<null>>(`/workspaces/${localStorage.getItem('workspaceId')}/members`, { email, role })
  return res.data
}

export async function updateMemberRole(userId: number, role: string): Promise<ApiResponse<null>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: null }
  const res = await http.put<ApiResponse<null>>(`/workspaces/${localStorage.getItem('workspaceId')}/members/${userId}`, { role })
  return res.data
}

export async function removeMember(userId: number): Promise<ApiResponse<null>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: null }
  const res = await http.delete<ApiResponse<null>>(`/workspaces/${localStorage.getItem('workspaceId')}/members/${userId}`)
  return res.data
}

export async function getActivities(): Promise<ApiResponse<PaginatedData<ActivityLog>>> {
  if (USE_MOCK) return { code: 0, message: 'ok', data: { list: mockActivities, total: mockActivities.length, page: 1, pageSize: 20 } }
  const res = await http.get<ApiResponse<PaginatedData<ActivityLog>>>(`/workspaces/${localStorage.getItem('workspaceId')}/activities`)
  return res.data
}
