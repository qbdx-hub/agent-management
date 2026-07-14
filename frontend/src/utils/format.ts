/**
 * 格式化日期时间
 */
export function formatDateTime(dateStr: string): string {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day} ${h}:${min}`
}

/**
 * 格式化日期
 */
export function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/**
 * 格式化数字（添加千分位）
 */
export function formatNumber(n: number): string {
  if (n == null) return '0'
  return n.toLocaleString('zh-CN')
}

/**
 * 格式化 Token 数量
 */
export function formatTokens(tokens: number): string {
  if (tokens == null) return '0'
  if (tokens >= 1_000_000) {
    return (tokens / 1_000_000).toFixed(1) + 'M'
  }
  if (tokens >= 1_000) {
    return (tokens / 1_000).toFixed(1) + 'K'
  }
  return String(tokens)
}

/**
 * 格式化费用（美元）
 */
export function formatCost(cost: number): string {
  if (cost == null) return '$0.00'
  return '$' + cost.toFixed(2)
}

/**
 * 格式化百分比
 */
export function formatPercent(rate: number): string {
  if (rate == null) return '-'
  return (rate * 100).toFixed(1) + '%'
}

/**
 * 格式化毫秒延迟
 */
export function formatLatency(ms: number): string {
  if (ms == null) return '-'
  if (ms >= 1000) {
    return (ms / 1000).toFixed(1) + 's'
  }
  return ms + 'ms'
}

/**
 * 计算相对时间描述
 */
export function timeAgo(dateStr: string): string {
  if (!dateStr) return '-'
  const now = Date.now()
  const then = new Date(dateStr).getTime()
  const diff = now - then
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes} 分钟前`
  if (hours < 24) return `${hours} 小时前`
  if (days < 30) return `${days} 天前`
  return formatDate(dateStr)
}
