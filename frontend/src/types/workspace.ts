export interface WorkspaceItem {
  id: number
  name: string
  description: string
  role: string
  memberCount: number
  agentCount: number
  createdAt: string
}

export interface WorkspaceSettings {
  defaultModelProvider: string
  sessionRetentionDays: number
  autoArchiveDays: number
  maxTokensPerTask: number
  language: string
}

export interface Member {
  userId: number
  username: string
  nickname: string
  avatar: string
  email: string
  role: string
  roleLabel: string
  joinedAt: string
  lastActiveAt: string
  agentCount: number
  sessionCount30d: number
}

export interface ActivityLog {
  type: string
  userId: number
  userName: string
  description: string
  relatedId: number
  relatedType: string
  createdAt: string
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar: string
  email: string
  role: string
}
