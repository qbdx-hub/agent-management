import http from './index'
import type { ApiResponse, PaginatedData } from '@/types/common'
import type { Role, ApprovalItem } from '@/types/security'

// ==================== 角色 ====================

/** GET /security/roles —— 角色列表（本空间自定义 + 系统角色） */
export async function getRoles(): Promise<ApiResponse<Role[]>> {
  const res = await http.get<ApiResponse<Role[]>>('/security/roles')
  return res.data
}

/** POST /security/roles —— 创建自定义角色 */
export async function createRole(data: Omit<Role, 'id' | 'isSystem' | 'memberCount'>): Promise<ApiResponse<Role>> {
  const res = await http.post<ApiResponse<Role>>('/security/roles', data)
  return res.data
}

/** DELETE /security/roles/{id} —— 删除自定义角色（系统角色后端会拒绝） */
export async function deleteRole(roleId: number): Promise<ApiResponse<null>> {
  const res = await http.delete<ApiResponse<null>>(`/security/roles/${roleId}`)
  return res.data
}

// ==================== 审批 ====================
// 审批规则（GET /security/approvals/rules）后端已实现，由系统自动生成，前端暂无管理界面。

/** GET /security/approvals —— 审批记录分页（status=pending/approved/rejected，可省略） */
export async function getApprovals(params?: { status?: string; page?: number; pageSize?: number }): Promise<ApiResponse<PaginatedData<ApprovalItem>>> {
  const res = await http.get<ApiResponse<PaginatedData<ApprovalItem>>>('/security/approvals', { params })
  return res.data
}

/** POST /security/approvals/{id}/approve —— 通过（workflow_run 类型会联动恢复运行） */
export async function approveApproval(approvalId: number, comment?: string): Promise<ApiResponse<null>> {
  const res = await http.post<ApiResponse<null>>(`/security/approvals/${approvalId}/approve`, { comment })
  return res.data
}

/** POST /security/approvals/{id}/reject —— 拒绝（workflow_run 类型会联动终止运行） */
export async function rejectApproval(approvalId: number, comment?: string): Promise<ApiResponse<null>> {
  const res = await http.post<ApiResponse<null>>(`/security/approvals/${approvalId}/reject`, { comment })
  return res.data
}
