import type { AgentSummary, AgentDetail } from '@/types/agent'

export const mockAgents: AgentSummary[] = [
  {
    id: 1, name: '代码审查助手', description: '自动审查 PR 代码质量，输出改进建议',
    avatar: '09-search', status: 'published', modelName: 'Claude Opus 4.8',
    tags: ['code', 'review'], toolCount: 3, totalSessions: 230,
    successRate: 0.982, avgLatencyMs: 1200, createdBy: 1, creatorName: '张三',
    updatedAt: '2026-07-14T09:00:00+08:00',
  },
  {
    id: 2, name: '文档生成器', description: '根据代码注释自动生成 API 文档',
    avatar: '05-document', status: 'published', modelName: 'GPT-4o',
    tags: ['docs', 'api'], toolCount: 2, totalSessions: 89,
    successRate: 0.965, avgLatencyMs: 800, createdBy: 2, creatorName: '李四',
    updatedAt: '2026-07-13T16:30:00+08:00',
  },
  {
    id: 3, name: '数据分析 Agent', description: '连接数据库，自然语言查询生成图表',
    avatar: '11-growth-chart', status: 'published', modelName: 'DeepSeek V4',
    tags: ['data', 'sql'], toolCount: 4, totalSessions: 156,
    successRate: 0.942, avgLatencyMs: 2100, createdBy: 1, creatorName: '张三',
    updatedAt: '2026-07-12T11:00:00+08:00',
  },
  {
    id: 4, name: 'PR 审查机器人', description: '自动化 PR 评审，检查代码规范和安全漏洞',
    avatar: '23-ai-robot', status: 'testing', modelName: 'Claude Sonnet 4.6',
    tags: ['review', 'security'], toolCount: 3, totalSessions: 42,
    successRate: 0.905, avgLatencyMs: 1500, createdBy: 3, creatorName: '王五',
    updatedAt: '2026-07-14T08:15:00+08:00',
  },
  {
    id: 5, name: '部署检查 Agent', description: '部署前自动检查配置、依赖和兼容性',
    avatar: '02-rocket', status: 'paused', modelName: 'Claude Haiku 4.5',
    tags: ['devops', 'deploy'], toolCount: 5, totalSessions: 67,
    successRate: 0.821, avgLatencyMs: 3500, createdBy: 1, creatorName: '张三',
    updatedAt: '2026-07-10T14:00:00+08:00',
  },
  {
    id: 6, name: '客服助手', description: '智能客服，解答用户常见问题并自动建工单',
    avatar: '01-chat', status: 'draft', modelName: 'Gemini 2.5 Flash',
    tags: ['support'], toolCount: 1, totalSessions: 0,
    successRate: 0, avgLatencyMs: 0, createdBy: 2, creatorName: '李四',
    updatedAt: '2026-07-13T09:00:00+08:00',
  },
]

export const mockAgentDetail: AgentDetail = {
  id: 1,
  name: '代码审查助手',
  description: '自动审查 PR 代码质量，输出改进建议',
  avatar: '09-search',
  status: 'published',
  tags: ['code', 'review'],
  createdBy: 1,
  creatorName: '张三',
  createdAt: '2026-07-01T09:00:00+08:00',
  updatedAt: '2026-07-14T10:00:00+08:00',
  config: {
    modelProvider: 'anthropic',
    modelName: 'claude-opus-4-8',
    temperature: 0.7,
    maxTokens: 4096,
    topP: 0.95,
    systemPrompt: '你是一个资深代码审查专家。\n当用户提交代码时，你需要：\n1. 检查 {{aspect}} 方面的问题\n2. 按严重程度分类\n3. 给出具体的改进建议',
    promptVariables: [
      { name: 'language', label: '语言', type: 'string', defaultValue: 'Java', required: true },
      { name: 'aspect', label: '审查维度', type: 'select', options: ['安全性', '性能', '可读性', '全面'], defaultValue: '全面', required: true },
    ],
    boundTools: [
      { toolId: 1, toolName: '搜索网页', toolIcon: '09-search', enabled: true },
      { toolId: 3, toolName: '读取文件', toolIcon: '05-document', enabled: true },
      { toolId: 4, toolName: 'GitHub API', toolIcon: '14-globe', enabled: false },
    ],
    memory: {
      workingWindow: 20,
      shortTermStrategy: 'summary',
      longTermEnabled: true,
      knowledgeBaseIds: [1, 2],
    },
    execution: {
      maxSteps: 20,
      timeoutSeconds: 300,
      reflectionEnabled: true,
      reflectionDepth: 2,
    },
  },
  stats: {
    totalSessions: 230, totalMessages: 1840, totalTokens: 5200000,
    totalCost: 123.50, successRate: 0.982, avgLatencyMs: 1200, avgStepsPerSession: 5.3,
  },
}

