// ==================== 成本 ====================

export type CostPeriod = 'today' | 'this_month' | 'last_month'
export type CostDimension = 'model' | 'agent' | 'member'
export type BudgetPeriod = 'daily' | 'monthly'
export type BudgetScope = 'workspace' | 'user' | 'agent'

export interface CostOverview {
  totalCost: number
  budgetLimit: number
  budgetRemaining: number
  budgetPercent: number
  todayCost: number
  yesterdayCost: number
  projectedMonthCost: number
  meltdownStatus: 'normal' | 'warning' | 'meltdown'
}

export interface CostBreakdownItem {
  label: string
  cost: number
  percent: number
  tokenInput: number
  tokenOutput: number
}

export interface CostTrendPoint {
  date: string
  cost: number
  tokens: number
}

export interface BudgetConfig {
  id: number
  name: string
  scope: BudgetScope
  scopeId: number | null
  period: BudgetPeriod
  limit: number
  warnPercent: number
  meltdownEnabled: boolean
  notifyChannels: string[]
}

export interface CostRecord {
  recordId: number
  agentId: number
  agentName: string
  sessionId: number
  modelProvider: string
  modelName: string
  tokenInput: number
  tokenOutput: number
  cost: number
  userId: number
  userName: string
  createdAt: string
}
