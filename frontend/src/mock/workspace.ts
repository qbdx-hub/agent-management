import type { WorkspaceItem, WorkspaceSettings, Member, ActivityLog } from '@/types/workspace'

export const mockWorkspaces: WorkspaceItem[] = [
  { id: 1, name: 'Agent 开发团队', description: '负责 Agent 管理平台研发', role: 'ADMIN', memberCount: 5, agentCount: 12, createdAt: '2026-07-01T09:00:00+08:00' },
  { id: 2, name: 'AI 创新实验室', description: '探索 AI Agent 新应用场景', role: 'DEVELOPER', memberCount: 3, agentCount: 4, createdAt: '2026-07-05T10:00:00+08:00' },
]

export const mockWorkspaceSettings: WorkspaceSettings = {
  defaultModelProvider: 'openai',
  sessionRetentionDays: 90,
  autoArchiveDays: 30,
  maxTokensPerTask: 100000,
  language: 'zh-CN',
}

export const mockMembers: Member[] = [
  { userId: 1, username: 'zhangsan', nickname: '张三', avatar: '', email: 'zhangsan@company.com', role: 'ADMIN', roleLabel: '管理员', joinedAt: '2026-07-01T09:00:00+08:00', lastActiveAt: '2026-07-14T14:30:00+08:00', agentCount: 5, sessionCount30d: 230 },
  { userId: 2, username: 'lisi', nickname: '李四', avatar: '', email: 'lisi@company.com', role: 'MANAGER', roleLabel: '管理者', joinedAt: '2026-07-01T09:30:00+08:00', lastActiveAt: '2026-07-14T14:15:00+08:00', agentCount: 3, sessionCount30d: 180 },
  { userId: 3, username: 'wangwu', nickname: '王五', avatar: '', email: 'wangwu@company.com', role: 'DEVELOPER', roleLabel: '开发者', joinedAt: '2026-07-02T10:00:00+08:00', lastActiveAt: '2026-07-14T11:00:00+08:00', agentCount: 2, sessionCount30d: 85 },
  { userId: 4, username: 'zhaoliu', nickname: '赵六', avatar: '', email: 'zhaoliu@company.com', role: 'DEVELOPER', roleLabel: '开发者', joinedAt: '2026-07-05T09:00:00+08:00', lastActiveAt: '2026-07-13T17:00:00+08:00', agentCount: 1, sessionCount30d: 42 },
  { userId: 5, username: 'qianqi', nickname: '钱七', avatar: '', email: 'qianqi@company.com', role: 'VIEWER', roleLabel: '只读用户', joinedAt: '2026-07-10T14:00:00+08:00', lastActiveAt: '2026-07-13T10:00:00+08:00', agentCount: 0, sessionCount30d: 5 },
]

export const mockActivities: ActivityLog[] = [
  { type: 'agent.created', userId: 3, userName: '王五', description: '创建了 Agent「PR审查机器人」', relatedId: 4, relatedType: 'agent', createdAt: '2026-07-14T10:30:00+08:00' },
  { type: 'agent.status_change', userId: 1, userName: '张三', description: '将「代码审查助手」更新为 published', relatedId: 1, relatedType: 'agent', createdAt: '2026-07-14T09:00:00+08:00' },
  { type: 'tool.create', userId: 2, userName: '李四', description: '注册了工具「邮件发送」', relatedId: 5, relatedType: 'tool', createdAt: '2026-07-13T15:00:00+08:00' },
  { type: 'member.invite', userId: 1, userName: '张三', description: '邀请了钱七加入工作空间', relatedId: 5, relatedType: 'member', createdAt: '2026-07-10T14:00:00+08:00' },
  { type: 'agent.updated', userId: 2, userName: '李四', description: '更新了「文档生成器」的 System Prompt', relatedId: 2, relatedType: 'agent', createdAt: '2026-07-12T16:00:00+08:00' },
]
