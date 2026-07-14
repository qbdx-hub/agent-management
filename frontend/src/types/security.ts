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

// ==================== 审计日志 ====================

export interface AuditLogItem {
  logId: number
  userId: number
  userName: string
  action: string
  actionLabel: string
  resource: string
  detail: string
  ip: string
  userAgent: string
  result: 'success' | 'failure'
  createdAt: string
}

// ==================== 审批 ====================

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
  ruleName: string
  applicantId: number
  applicantName: string
  action: string
  detail: string
  status: 'pending' | 'approved' | 'rejected'
  createdAt: string
}

// ==================== API Key ====================

export interface ApiKey {
  id: number
  provider: string
  keyName: string
  apiKeyMasked: string
  isDefault: boolean
  lastUsedAt: string | null
  createdBy: number
  createdAt: string
}
