// ==================== API 通用类型 ====================

/** 后端统一响应体 */
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

/** 分页请求参数 */
export interface PaginationQuery {
  page: number
  pageSize: number
  keyword?: string
  sortBy?: string
  sortDir?: 'asc' | 'desc'
}

/** 分页响应 */
export interface PaginatedData<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

// ==================== 通用业务类型 ====================

/** 选项 */
export interface SelectOption {
  label: string
  value: string | number
}

/** 面包屑 */
export interface BreadcrumbItem {
  title: string
  path?: string
}

/** 状态标签 */
export type StatusType = 'success' | 'warning' | 'danger' | 'info' | ''
