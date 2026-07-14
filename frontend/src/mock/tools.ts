import type { ToolSummary, ToolDetail, ToolTestResult, ToolStats } from '@/types/tool'

export const mockTools: ToolSummary[] = [
  {
    id: 1, name: 'search_web', displayName: '网页搜索', description: '搜索互联网获取最新信息',
    category: 'search', categoryLabel: '查询类', icon: '🔍', type: 'api', status: 'active',
    bindAgentCount: 3, totalCalls: 1500, successRate: 0.995, avgLatencyMs: 320,
    createdAt: '2026-07-10T10:00:00+08:00',
  },
  {
    id: 2, name: 'code_executor', displayName: '代码执行', description: '在沙箱中执行 Python/JS 代码',
    category: 'compute', categoryLabel: '计算类', icon: '⚡', type: 'api', status: 'active',
    bindAgentCount: 2, totalCalls: 680, successRate: 0.972, avgLatencyMs: 1500,
    createdAt: '2026-07-09T14:00:00+08:00',
  },
  {
    id: 3, name: 'file_reader', displayName: '文件读取', description: '读取本地或远程文件内容',
    category: 'operate', categoryLabel: '操作类', icon: '📁', type: 'builtin', status: 'active',
    bindAgentCount: 5, totalCalls: 3200, successRate: 0.998, avgLatencyMs: 50,
    createdAt: '2026-07-01T09:00:00+08:00',
  },
  {
    id: 4, name: 'github_api', displayName: 'GitHub API', description: '调用 GitHub REST API，管理 PR/Issue/仓库',
    category: 'operate', categoryLabel: '操作类', icon: '🐙', type: 'mcp', status: 'active',
    bindAgentCount: 2, totalCalls: 450, successRate: 0.988, avgLatencyMs: 600,
    createdAt: '2026-07-05T10:00:00+08:00',
  },
  {
    id: 5, name: 'email_sender', displayName: '邮件发送', description: '通过 SMTP 发送邮件通知',
    category: 'notify', categoryLabel: '通知类', icon: '📧', type: 'api', status: 'active',
    bindAgentCount: 1, totalCalls: 120, successRate: 0.95, avgLatencyMs: 800,
    createdAt: '2026-07-08T11:00:00+08:00',
  },
  {
    id: 6, name: 'image_analyzer', displayName: '图片分析', description: '分析图片内容，识别物体和文字',
    category: 'perceive', categoryLabel: '感知类', icon: '🖼️', type: 'api', status: 'inactive',
    bindAgentCount: 0, totalCalls: 30, successRate: 0.867, avgLatencyMs: 2500,
    createdAt: '2026-07-11T09:00:00+08:00',
  },
  {
    id: 7, name: 'db_query', displayName: '数据库查询', description: '连接 MySQL/PostgreSQL 执行查询',
    category: 'search', categoryLabel: '查询类', icon: '🗄️', type: 'api', status: 'active',
    bindAgentCount: 1, totalCalls: 800, successRate: 0.976, avgLatencyMs: 200,
    createdAt: '2026-07-03T15:00:00+08:00',
  },
  {
    id: 8, name: 'slack_notifier', displayName: 'Slack 通知', description: '发送消息到 Slack 频道',
    category: 'notify', categoryLabel: '通知类', icon: '💬', type: 'api', status: 'active',
    bindAgentCount: 2, totalCalls: 300, successRate: 0.993, avgLatencyMs: 400,
    createdAt: '2026-07-06T08:00:00+08:00',
  },
]

export const mockToolDetail: ToolDetail = {
  id: 4, name: 'github_api', displayName: 'GitHub API', description: '调用 GitHub REST API，管理 PR/Issue/仓库',
  category: 'operate', categoryLabel: '操作类', icon: '🐙', type: 'mcp', status: 'active',
  endpoint: { url: 'https://api.github.com', method: 'GET', headers: { Authorization: 'Bearer {{github_token}}', 'Content-Type': 'application/json' }, timeoutMs: 15000 },
  parameters: [
    { name: 'owner', type: 'string', required: true, description: '仓库所有者' },
    { name: 'repo', type: 'string', required: true, description: '仓库名' },
    { name: 'pr_number', type: 'integer', required: false, description: 'PR 编号' },
  ],
  responseMapping: '仓库 {{owner}}/{{repo}} 数据获取成功',
  credentialRef: 'github_token',
  retryOnFail: true, maxRetries: 2,
  bindAgentCount: 2, totalCalls: 450, successRate: 0.988, avgLatencyMs: 600,
  recentCalls: [
    { time: '2026-07-14T14:00:00+08:00', params: { owner: 'myteam', repo: 'agent-mgmt' }, resultSummary: '获取成功', success: true, latencyMs: 520 },
    { time: '2026-07-14T13:30:00+08:00', params: { owner: 'myteam', repo: 'agent-mgmt', pr_number: 42 }, resultSummary: 'PR 数据已返回', success: true, latencyMs: 680 },
  ],
  createdAt: '2026-07-05T10:00:00+08:00',
}

export const mockToolTestSuccess: ToolTestResult = {
  success: true, latencyMs: 520,
  requestUrl: 'https://api.github.com/repos/myteam/agent-mgmt',
  requestBody: '{}',
  responseStatus: 200,
  responseBody: '{"full_name":"myteam/agent-mgmt","stars":42,"forks":12}',
  mappedOutput: '仓库 myteam/agent-mgmt 数据获取成功',
}

export const mockToolStats: ToolStats = {
  totalCalls: 450, successRate: 0.988, avgLatencyMs: 600, p99LatencyMs: 1200,
  dailyCallCount: [
    { date: '2026-07-10', count: 35, failCount: 0 },
    { date: '2026-07-11', count: 42, failCount: 1 },
    { date: '2026-07-12', count: 28, failCount: 0 },
    { date: '2026-07-13', count: 55, failCount: 1 },
    { date: '2026-07-14', count: 20, failCount: 0 },
  ],
  topAgents: [
    { agentId: 1, agentName: '代码审查助手', callCount: 280 },
    { agentId: 4, agentName: 'PR 审查机器人', callCount: 170 },
  ],
}
