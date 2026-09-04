import http from './http'
import type { ApiResponse, LoginVO, UserVO, Preferences, UserProfileForm } from '@/types'

export async function login(username: string, password: string): Promise<ApiResponse<LoginVO>> {
  const res = await http.post<ApiResponse<LoginVO>>('/auth/login', { username, password })
  return res.data
}

export async function register(form: { username: string; nickname: string; email: string; password: string }): Promise<ApiResponse<null>> {
  const res = await http.post<ApiResponse<null>>('/auth/register', form)
  return res.data
}

export async function getCurrentUser(): Promise<ApiResponse<UserVO>> {
  const res = await http.get<ApiResponse<UserVO>>('/auth/me')
  return res.data
}

export async function updateProfile(form: UserProfileForm): Promise<ApiResponse<null>> {
  const res = await http.put<ApiResponse<null>>('/auth/profile', form)
  return res.data
}

export async function uploadAvatar(file: File): Promise<ApiResponse<string>> {
  const fd = new FormData()
  fd.append('file', file)
  const res = await http.post<ApiResponse<string>>('/auth/avatar', fd)
  return res.data
}

export async function getPreferences(): Promise<ApiResponse<Preferences>> {
  const res = await http.get<ApiResponse<Preferences>>('/auth/preferences')
  return res.data
}

export async function savePreferences(prefs: Preferences): Promise<ApiResponse<null>> {
  const res = await http.put<ApiResponse<null>>('/auth/preferences', { preferences: prefs })
  return res.data
}
