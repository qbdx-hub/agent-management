import axios from 'axios'
import type { ApiResponse } from '@/types'
import { toast } from '@/utils/toast'
import { useAuthStore } from '@/stores/auth'

const http = axios.create({
  baseURL: (import.meta.env.VITE_API_BASE_URL || '') + '/api/v1',
  timeout: 30_000,
  // 注意：不要设默认 Content-Type —— 实例默认 JSON 头会让 FormData 上传
  // 携带错误的请求头（无 multipart boundary）导致后端 500；
  // axios 对普通 JSON 对象会自动设置 application/json
})

// 请求拦截器：Bearer token + 工作空间上下文（后端要求 X-Workspace-Id）
http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  if (auth.workspaceId) {
    config.headers['X-Workspace-Id'] = String(auth.workspaceId)
  }
  return config
})

// 响应拦截器：code!==0 统一提示；401 清会话回登录页
http.interceptors.response.use(
  (res) => {
    const data = res.data as ApiResponse
    if (data.code !== 0) {
      toast(data.message || '请求失败', 'error')
      return Promise.reject(new Error(data.message))
    }
    return res
  },
  (err) => {
    if (err.response?.status === 401) {
      const auth = useAuthStore()
      auth.logout()
      window.location.hash = '#/login'
      toast('登录已过期，请重新登录', 'error')
      return Promise.reject(err)
    }
    toast(err.response?.data?.message || '网络错误', 'error')
    return Promise.reject(err)
  }
)

export default http
