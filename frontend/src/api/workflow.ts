import http from './index'
import type { ApiResponse, PaginatedData, PaginationQuery } from '@/types/common'
import type { WorkflowSummary, WorkflowDetail, WorkflowCreateDTO, WorkflowSaveDTO, WorkflowRun } from '@/types/workflow'

/** GET /workflows —— 工作流分页列表 */
export async function getWorkflowList(params: PaginationQuery & { status?: string }): Promise<ApiResponse<PaginatedData<WorkflowSummary>>> {
  const res = await http.get<ApiResponse<PaginatedData<WorkflowSummary>>>('/workflows', { params })
  return res.data
}

/** GET /workflows/{id} —— 工作流详情（含画布 nodes/edges） */
export async function getWorkflow(id: number): Promise<ApiResponse<WorkflowDetail>> {
  const res = await http.get<ApiResponse<WorkflowDetail>>(`/workflows/${id}`)
  return res.data
}

/** POST /workflows —— 创建空工作流 */
export async function createWorkflow(data: WorkflowCreateDTO): Promise<ApiResponse<WorkflowDetail>> {
  const res = await http.post<ApiResponse<WorkflowDetail>>('/workflows', data)
  return res.data
}

/** PUT /workflows/{id} —— 保存画布（全量替换 nodes/edges，落 position） */
export async function saveWorkflow(id: number, data: WorkflowSaveDTO): Promise<ApiResponse<WorkflowDetail>> {
  const res = await http.put<ApiResponse<WorkflowDetail>>(`/workflows/${id}`, data)
  return res.data
}

/** DELETE /workflows/{id} —— 删除工作流 */
export async function deleteWorkflow(id: number): Promise<ApiResponse<null>> {
  const res = await http.delete<ApiResponse<null>>(`/workflows/${id}`)
  return res.data
}

/** POST /workflows/{id}/run —— 启动一次运行（异步执行），返回 runId */
export async function runWorkflow(id: number, input?: Record<string, any>): Promise<ApiResponse<{ runId: number }>> {
  const res = await http.post<ApiResponse<{ runId: number }>>(`/workflows/${id}/run`, { input: input ?? {} })
  return res.data
}

/** GET /workflows/runs/{runId} —— 运行详情（运行中可轮询看进度） */
export async function getWorkflowRun(runId: number | string): Promise<ApiResponse<WorkflowRun>> {
  const res = await http.get<ApiResponse<WorkflowRun>>(`/workflows/runs/${runId}`)
  return res.data
}

/** GET /workflows/{id}/runs —— 某工作流的运行历史（分页） */
export async function getWorkflowRuns(id: number, params?: PaginationQuery): Promise<ApiResponse<PaginatedData<WorkflowRun>>> {
  const res = await http.get<ApiResponse<PaginatedData<WorkflowRun>>>(`/workflows/${id}/runs`, { params })
  return res.data
}

/** POST /workflows/runs/{runId}/approve —— 审批处理（通过=继续执行，拒绝=运行失败） */
export async function approveWorkflowRun(runId: number | string, approved: boolean, reason?: string): Promise<ApiResponse<null>> {
  const res = await http.post<ApiResponse<null>>(`/workflows/runs/${runId}/approve`, { approved, reason })
  return res.data
}
