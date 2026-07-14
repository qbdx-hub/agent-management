import type { CostOverview, CostBreakdownItem, CostTrendPoint, BudgetConfig, CostRecord } from '@/types/cost'

export const mockCostOverview: CostOverview = {
  totalCost: 1247.30, budgetLimit: 2000.00, budgetRemaining: 752.70,
  budgetPercent: 62.4, todayCost: 45.20, yesterdayCost: 52.10,
  projectedMonthCost: 1450.00, meltdownStatus: 'normal',
}

export const mockCostByModel: CostBreakdownItem[] = [
  { label: 'GPT-4o', cost: 680.00, percent: 54.5, tokenInput: 2000000, tokenOutput: 500000 },
  { label: 'Claude Opus 4.8', cost: 320.00, percent: 25.7, tokenInput: 800000, tokenOutput: 200000 },
  { label: 'DeepSeek V4', cost: 180.00, percent: 14.4, tokenInput: 1500000, tokenOutput: 600000 },
  { label: 'Gemini 2.5', cost: 67.30, percent: 5.4, tokenInput: 500000, tokenOutput: 150000 },
]

export const mockCostByAgent: CostBreakdownItem[] = [
  { label: '代码审查助手', cost: 320.00, percent: 25.7, tokenInput: 600000, tokenOutput: 180000 },
  { label: '文档生成器', cost: 280.00, percent: 22.4, tokenInput: 500000, tokenOutput: 150000 },
  { label: '数据分析 Agent', cost: 200.00, percent: 16.0, tokenInput: 400000, tokenOutput: 200000 },
  { label: 'PR 审查机器人', cost: 185.00, percent: 14.8, tokenInput: 350000, tokenOutput: 120000 },
  { label: '部署检查 Agent', cost: 152.00, percent: 12.2, tokenInput: 200000, tokenOutput: 50000 },
]

export const mockCostTrend: CostTrendPoint[] = []
for (let i = 30; i >= 1; i--) {
  const d = new Date(2026, 6, 14 - i + 30)
  if (d.getDate() <= 14) {
    mockCostTrend.push({
      date: `2026-07-${String(d.getDate()).padStart(2, '0')}`,
      cost: 30 + Math.random() * 20,
      tokens: 100000 + Math.random() * 80000,
    })
  }
}

export const mockBudgets: BudgetConfig[] = [
  { id: 1, name: '个人日预算', scope: 'user', scopeId: null, period: 'daily', limit: 100, warnPercent: 80, meltdownEnabled: true, notifyChannels: ['feishu'] },
  { id: 2, name: '工作空间月预算', scope: 'workspace', scopeId: null, period: 'monthly', limit: 2000, warnPercent: 80, meltdownEnabled: true, notifyChannels: ['feishu', 'email'] },
]

export const mockCostRecords: CostRecord[] = [
  { recordId: 1, agentId: 1, agentName: '代码审查助手', sessionId: 1001, modelProvider: 'anthropic', modelName: 'claude-opus-4-8', tokenInput: 2500, tokenOutput: 800, cost: 0.04, userId: 1, userName: '张三', createdAt: '2026-07-14T14:30:20+08:00' },
  { recordId: 2, agentId: 2, agentName: '文档生成器', sessionId: 1050, modelProvider: 'openai', modelName: 'gpt-4o', tokenInput: 5000, tokenOutput: 2000, cost: 0.03, userId: 2, userName: '李四', createdAt: '2026-07-14T14:15:00+08:00' },
  { recordId: 3, agentId: 3, agentName: '数据分析 Agent', sessionId: 1892, modelProvider: 'deepseek', modelName: 'deepseek-v4', tokenInput: 3000, tokenOutput: 1000, cost: 0.001, userId: 1, userName: '张三', createdAt: '2026-07-14T14:00:00+08:00' },
  { recordId: 4, agentId: 1, agentName: '代码审查助手', sessionId: 1003, modelProvider: 'anthropic', modelName: 'claude-opus-4-8', tokenInput: 1800, tokenOutput: 600, cost: 0.03, userId: 1, userName: '张三', createdAt: '2026-07-14T13:45:00+08:00' },
  { recordId: 5, agentId: 5, agentName: '部署检查 Agent', sessionId: 2010, modelProvider: 'anthropic', modelName: 'claude-haiku-4-5', tokenInput: 1000, tokenOutput: 300, cost: 0.001, userId: 3, userName: '王五', createdAt: '2026-07-14T11:00:00+08:00' },
]
