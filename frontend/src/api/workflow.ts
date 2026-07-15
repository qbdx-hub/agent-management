import http from './index'
import type { ApiResponse, PaginatedData, PaginationQuery } from '@/types/common'
import type { WorkflowSummary, WorkflowDetail, WorkflowCreateDTO, WorkflowSaveDTO } from '@/types/workflow'

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
