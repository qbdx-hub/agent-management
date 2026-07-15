import axios from 'axios'
import type { ApiResponse } from '@/types/common'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL + '/api/v1',
  timeout: 30_000,
  headers: { 'Content-Type': 'application/json' },
})

// 请求拦截器
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  const workspaceId = localStorage.getItem('workspaceId')
  const userName = localStorage.getItem('userName') || ''
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  if (workspaceId) {
    config.headers['X-Workspace-Id'] = workspaceId
  }
  if (userName) {
    config.headers['X-User-Name'] = userName
  }
  return config
})

// 响应拦截器
http.interceptors.response.use(
  (res) => {
    const data = res.data as ApiResponse
    if (data.code !== 0) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(new Error(data.message))
    }
    return res
  },
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('workspaceId')
      window.location.href = '/login'
      return Promise.reject(err)
    }
    ElMessage.error(err.response?.data?.message || '网络错误')
    return Promise.reject(err)
  }
)

export default http
