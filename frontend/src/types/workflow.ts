// ==================== 工作流枚举 ====================

export type WorkflowStatus = 'draft' | 'active' | 'archived'

// ==================== 工作流列表 ====================

export interface WorkflowSummary {
  id: number
  name: string
  description: string
  status: WorkflowStatus
  nodeCount: number
  createdBy: number
  creatorName: string
  updatedAt: string
}

// ==================== 画布节点 / 边 ====================

/** 画布节点（对应 workflow_node 表，前端 Vue Flow 节点的业务数据） */
export interface WorkflowNode {
  id?: number
  /** 节点业务标识（前端生成，Vue Flow node.id） */
  nodeId: string
  /** 类型：start/agent/tool/condition/end/approval */
  type: string
  label: string
  agentId?: number
  toolId?: number
  config?: Record<string, any>
  positionX: number
  positionY: number
}

/** 画布连线（对应 workflow_edge 表） */
export interface WorkflowEdge {
  id?: number
  edgeId: string
  sourceNodeId: string
  targetNodeId: string
  label?: string
  condition?: Record<string, any>
}

// ==================== 工作流详情 ====================

export interface WorkflowDetail {
  id: number
  name: string
  description: string
  status: WorkflowStatus
  triggerType?: string
  triggerConfig?: Record<string, any>
  createdBy: number
  creatorName: string
  createdAt: string
  updatedAt: string
  nodes: WorkflowNode[]
  edges: WorkflowEdge[]
}

// ==================== 创建 / 保存 ====================

export interface WorkflowCreateDTO {
  name: string
  description?: string
}

export interface WorkflowSaveDTO {
  name: string
  description: string
  status: WorkflowStatus
  nodes: WorkflowNode[]
  edges: WorkflowEdge[]
}

// ==================== 运行记录 ====================

/** 运行状态：running=执行中 waiting_approval=等待审批 completed=完成 failed=失败 */
export type WorkflowRunStatus = 'running' | 'waiting_approval' | 'completed' | 'failed'

/** 单节点执行结果（后端 nodeResults JSON 数组项） */
export interface WorkflowNodeResult {
  nodeId: string
  label?: string
  type?: string
  status: 'success' | 'error' | 'waiting' | 'rejected' | 'skipped'
  output?: string
  error?: string
  durationMs?: number
  sequence?: number
  tokens?: number
}

/** 工作流运行记录（对应 workflow_run 表） */
export interface WorkflowRun {
  id: number
  workflowId: number
  status: WorkflowRunStatus
  input?: Record<string, any>
  output?: Record<string, any>
  nodeResults: WorkflowNodeResult[]
  error?: string
  totalTokens?: number
  totalCost?: number
  duration?: number
  startedAt?: string
  endedAt?: string
  triggeredBy?: number
  triggeredByName?: string
  createdAt?: string
}
