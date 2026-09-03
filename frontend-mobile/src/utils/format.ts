/** token 数格式化：128500 → "128.5k"，1200000 → "1.2M" */
export function formatTokens(n: number | undefined | null): string {
  if (n == null) return '0'
  if (n >= 1_000_000) return (n / 1_000_000).toFixed(1) + 'M'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return String(n)
}

/** 金额格式化（人民币） */
export function formatCost(n: number | undefined | null): string {
  return '¥' + Number(n ?? 0).toFixed(2)
}

/** 文件大小格式化 */
export function formatFileSize(bytes: number): string {
  if (bytes >= 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB'
  if (bytes >= 1024) return (bytes / 1024).toFixed(0) + ' KB'
  return bytes + ' B'
}

/** 相对时间：几分钟前/几小时前/昨天/日期 */
export function timeAgo(iso: string | undefined | null): string {
  if (!iso) return '-'
  const t = new Date(iso).getTime()
  if (Number.isNaN(t)) return '-'
  const diff = Date.now() - t
  if (diff < 60_000) return '刚刚'
  if (diff < 3_600_000) return Math.floor(diff / 60_000) + ' 分钟前'
  if (diff < 86_400_000) return Math.floor(diff / 3_600_000) + ' 小时前'
  if (diff < 172_800_000) return '昨天'
  const d = new Date(iso)
  return `${d.getMonth() + 1}-${d.getDate()}`
}

/** 日期时间展示 */
export function formatDateTime(iso: string | undefined | null): string {
  if (!iso) return '-'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '-'
  const p = (x: number) => String(x).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

/** 仅日期展示 */
export function formatDate(iso: string | undefined | null): string {
  return formatDateTime(iso).split(' ')[0]
}
