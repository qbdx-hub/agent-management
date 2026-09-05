import http from './http'
import type { ApiResponse, TerminalExecResult, TerminalInfo } from '@/types'

/** 终端环境信息（os → 快捷命令 dir/ls 组、角色、沙箱目录名） */
export async function getTerminalInfo(): Promise<ApiResponse<TerminalInfo>> {
  const res = await http.get<ApiResponse<TerminalInfo>>('/terminal/info')
  return res.data
}

/**
 * 执行命令。cwd 状态由前端持有（服务端无状态），每次回传当前相对路径。
 * 后端硬超时 30s，这里按请求覆盖实例默认 30s 超时为 35s，避免后端尚未返回先被前端掐断。
 */
export async function execTerminal(command: string, cwd: string): Promise<ApiResponse<TerminalExecResult>> {
  const res = await http.post<ApiResponse<TerminalExecResult>>(
    '/terminal/exec',
    { command, cwd },
    { timeout: 35_000 },
  )
  return res.data
}
