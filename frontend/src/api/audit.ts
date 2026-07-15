import http from './index'
import type { ApiResponse } from '@/types/common'

// ==================== 类型定义 ====================

/** 审计日志实体（与后端 AuditLog 一一对应） */
export interface AuditLog {
  id: number
  workspaceId: number
  userId: number
  userName: string
  action: string
  actionLabel: string
  resourceType: string
  resourceId: number
  resourceName: string
  detail: string
  result: 'success' | 'failure'
  ipAddress: string
  userAgent: string
  createdAt: string
}

// ==================== 接口 ====================

/**
 * GET /audit-logs —— 查询审计日志列表
 * @param limit 返回条数，默认 50
 */
export async function listAuditLogs(limit: number = 50): Promise<ApiResponse<AuditLog[]>> {
  const res = await http.get<ApiResponse<AuditLog[]>>('/audit-logs', {
    params: { limit },
  })
  return res.data
}
