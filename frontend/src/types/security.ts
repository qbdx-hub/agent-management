// ==================== 角色权限 ====================

export interface Role {
  id: number
  name: string
  label: string
  description: string
  isSystem: boolean
  memberCount: number
  permissions: string[]
}

// ==================== 审批 ====================
// 审计日志类型见 api/audit.ts（audit_log 表结构）。

export interface ApprovalRule {
  id: number
  name: string
  triggerAction: string
  triggerCondition: string
  approverRole: string
  requiredApprovals: number
  enabled: boolean
}

export interface ApprovalItem {
  approvalId: number
  ruleName?: string
  applicantId?: number
  applicantName?: string
  approverName?: string
  resourceType?: string
  resourceId?: number
  resourceName?: string
  action: string
  detail: string
  status: 'pending' | 'approved' | 'rejected'
  reason?: string
  createdAt: string
  resolvedAt?: string | null
}
