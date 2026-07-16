import http from './index'
import type { ApiResponse } from '@/types/common'
import type { UserInfo } from '@/types/workspace'

export interface LoginResponse {
  token: string
  expiresAt: string
  user: UserInfo & { permissions: string[]; workspaces: { id: number; name: string; role: string }[] }
}

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'

export async function login(username: string, password: string): Promise<ApiResponse<LoginResponse>> {
  if (USE_MOCK) {
    return {
      code: 0, message: 'ok',
      data: {
        token: 'mock-token-xxx',
        expiresAt: new Date(Date.now() + 7 * 24 * 3600 * 1000).toISOString(),
        user: {
          id: 1, username, nickname: '张三', avatar: '', email: 'zhangsan@company.com', role: 'ADMIN',
          permissions: ['agent:*', 'tool:*', 'workspace:*', 'security:*', 'cost:*', 'monitor:*'],
          workspaces: [{ id: 1, name: 'Agent 开发团队', role: 'ADMIN' }, { id: 2, name: 'AI 创新实验室', role: 'DEVELOPER' }],
        },
      },
    }
  }
  const res = await http.post<ApiResponse<LoginResponse>>('/auth/login', { username, password })
  return res.data
}

export interface RegisterPayload {
  username: string
  nickname: string
  email: string
  password: string
}

/** POST /auth/register —— 注册成功无返回体（后端 data:null） */
export async function register(payload: RegisterPayload): Promise<ApiResponse<null>> {
  if (USE_MOCK) {
    return { code: 0, message: 'ok', data: null }
  }
  const res = await http.post<ApiResponse<null>>('/auth/register', payload)
  return res.data
}

export async function getCurrentUser(): Promise<ApiResponse<LoginResponse['user']>> {
  if (USE_MOCK) {
    const { data } = await login('', '')
    return { code: 0, message: 'ok', data: data.user }
  }
  const res = await http.get<ApiResponse<LoginResponse['user']>>('/auth/me')
  return res.data
}

export interface ProfilePayload {
  username: string
  nickname: string
  email?: string
  oldPassword?: string
  newPassword?: string
}

/** PUT /auth/profile —— 修改当前用户个人信息（用户名、昵称、邮箱、密码） */
export async function updateProfile(payload: ProfilePayload): Promise<ApiResponse<null>> {
  if (USE_MOCK) {
    return { code: 0, message: '修改成功', data: null }
  }
  const res = await http.put<ApiResponse<null>>('/auth/profile', payload)
  return res.data
}
