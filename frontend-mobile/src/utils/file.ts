/**
 * 服务端文件 URL 解析。
 * 头像等文件接口返回的是相对路径（/api/v1/uploads/...），axios 请求会自动带上
 * VITE_API_BASE_URL，但 <img :src> 不走 axios——在 APK WebView（页面源是本地
 * localhost）或无 /api 反代的部署形态下会 404。渲染前统一用本函数补全。
 */
const API_ORIGIN = import.meta.env.VITE_API_BASE_URL || ''

export function resolveFileUrl(url?: string | null): string {
  if (!url) return ''
  // 绝对地址（http(s)://、协议相对 //、data:）原样返回
  if (/^(https?:)?\/\//i.test(url) || url.startsWith('data:')) return url
  return API_ORIGIN + url
}
