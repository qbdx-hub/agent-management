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
  /** 空间名称/描述（保存时随设置一起提交；读取走 workspace store） */
  name?: string
  description?: string
  /** 共享工作目录：会话间共享文件区（否则每会话独立沙箱） */
  sharedWorkdir: boolean
  /** 允许沙箱外运行总闸 */
  allowOutsideSandbox: boolean
  /** 空间级禁用的内置工具名 */
  disabledTools: string[]
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
