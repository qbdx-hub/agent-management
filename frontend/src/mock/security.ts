import type { Role, AuditLogItem, ApprovalRule, ApprovalItem, ApiKey } from '@/types/security'

export const mockRoles: Role[] = [
  { id: 1, name: 'ADMIN', label: '管理员', description: '拥有所有权限', isSystem: true, memberCount: 2, permissions: ['agent:*', 'tool:*', 'workspace:*', 'security:*', 'cost:*', 'monitor:*'] },
  { id: 2, name: 'MANAGER', label: '管理者', description: '可管理Agent和工具，查看所有报表', isSystem: true, memberCount: 3, permissions: ['agent:*', 'tool:read', 'tool:register', 'monitor:*', 'cost:*', 'workspace:member:invite'] },
  { id: 3, name: 'DEVELOPER', label: '开发者', description: '可创建和使用Agent', isSystem: true, memberCount: 10, permissions: ['agent:read', 'agent:create', 'agent:update:own', 'tool:read', 'session:*', 'cost:read:own'] },
  { id: 4, name: 'VIEWER', label: '只读用户', description: '只能查看，不能修改', isSystem: true, memberCount: 2, permissions: ['agent:read', 'tool:read', 'monitor:read', 'cost:read'] },
]

export const mockAuditLogs: AuditLogItem[] = [
  { logId: 1, userId: 1, userName: '张三', action: 'agent.create', actionLabel: '创建Agent', resource: 'Agent/1', detail: '创建了Agent：代码审查助手', ip: '192.168.1.100', userAgent: 'Mozilla/5.0...', result: 'success', createdAt: '2026-07-14T09:00:00+08:00' },
  { logId: 2, userId: 2, userName: '李四', action: 'agent.update', actionLabel: '更新Agent', resource: 'Agent/2', detail: '修改了文档生成器的 System Prompt', ip: '192.168.1.101', userAgent: 'Mozilla/5.0...', result: 'success', createdAt: '2026-07-14T09:30:00+08:00' },
  { logId: 3, userId: 3, userName: '王五', action: 'tool.create', actionLabel: '注册工具', resource: 'Tool/5', detail: '注册了邮件发送工具', ip: '192.168.1.102', userAgent: 'Mozilla/5.0...', result: 'success', createdAt: '2026-07-14T10:00:00+08:00' },
  { logId: 4, userId: 1, userName: '张三', action: 'agent.status_change', actionLabel: '状态变更', resource: 'Agent/5', detail: '将部署检查Agent 从 published 改为 paused', ip: '192.168.1.100', userAgent: 'Mozilla/5.0...', result: 'success', createdAt: '2026-07-14T10:30:00+08:00' },
  { logId: 5, userId: 2, userName: '李四', action: 'session.start', actionLabel: '开始会话', resource: 'Session/1050', detail: '在文档生成器中启动了新会话', ip: '192.168.1.101', userAgent: 'Mozilla/5.0...', result: 'success', createdAt: '2026-07-14T14:15:00+08:00' },
  { logId: 6, userId: 1, userName: '张三', action: 'member.invite', actionLabel: '邀请成员', resource: 'Workspace/1', detail: '邀请了 user4@company.com 加入工作空间', ip: '192.168.1.100', userAgent: 'Mozilla/5.0...', result: 'success', createdAt: '2026-07-13T16:00:00+08:00' },
]

export const mockApprovalRules: ApprovalRule[] = [
  { id: 1, name: 'Agent 发布审批', triggerAction: 'agent.status_change', triggerCondition: '{"to":"published"}', approverRole: 'MANAGER', requiredApprovals: 1, enabled: true },
  { id: 2, name: '绑定敏感工具审批', triggerAction: 'agent.tools.bind', triggerCondition: '{"toolCategory":"operate"}', approverRole: 'ADMIN', requiredApprovals: 1, enabled: true },
]

export const mockApprovals: ApprovalItem[] = [
  { approvalId: 1, ruleName: 'Agent 发布审批', applicantId: 3, applicantName: '王五', action: 'agent.status_change', detail: '将Agent「PR审查机器人」从 testing 改为 published', status: 'pending', createdAt: '2026-07-14T10:00:00+08:00' },
  { approvalId: 2, ruleName: '绑定敏感工具审批', applicantId: 3, applicantName: '王五', action: 'agent.tools.bind', detail: '为Agent「部署检查Agent」绑定SSH操作工具', status: 'pending', createdAt: '2026-07-14T11:00:00+08:00' },
  { approvalId: 3, ruleName: 'Agent 发布审批', applicantId: 1, applicantName: '张三', action: 'agent.status_change', detail: '将Agent「客服助手」从 draft 改为 published', status: 'approved', createdAt: '2026-07-13T15:00:00+08:00' },
]

export const mockApiKeys: ApiKey[] = [
  { id: 1, provider: 'openai', keyName: '公司 OpenAI 账号', apiKeyMasked: 'sk-***...***abcd', isDefault: true, lastUsedAt: '2026-07-14T14:30:00+08:00', createdBy: 1, createdAt: '2026-07-01T09:00:00+08:00' },
  { id: 2, provider: 'anthropic', keyName: 'Anthropic 企业版', apiKeyMasked: 'sk-ant-***...***efgh', isDefault: false, lastUsedAt: '2026-07-14T13:00:00+08:00', createdBy: 1, createdAt: '2026-07-02T10:00:00+08:00' },
  { id: 3, provider: 'deepseek', keyName: 'DeepSeek 测试账号', apiKeyMasked: 'sk-***...***ijkl', isDefault: false, lastUsedAt: '2026-07-14T12:00:00+08:00', createdBy: 2, createdAt: '2026-07-05T09:00:00+08:00' },
]
