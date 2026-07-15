import type { SessionSummary, Message } from '@/types/session'

export const mockSessions: SessionSummary[] = [
  {
    sessionId: 1001, title: '审查 PR #42',
    status: 'completed', messageCount: 8, totalTokens: 12000,
    totalCost: 0.15, lastMessageAt: '2026-07-14T14:35:00+08:00',
    createdAt: '2026-07-14T14:30:00+08:00',
  },
  {
    sessionId: 1002, title: '审查 PR #43',
    status: 'active', messageCount: 3, totalTokens: 4500,
    totalCost: 0.06, lastMessageAt: '2026-07-14T15:00:00+08:00',
    createdAt: '2026-07-14T14:55:00+08:00',
  },
  {
    sessionId: 1003, title: '检查安全漏洞',
    status: 'error', messageCount: 5, totalTokens: 8000,
    totalCost: 0.10, lastMessageAt: '2026-07-13T11:00:00+08:00',
    createdAt: '2026-07-13T10:50:00+08:00',
  },
  {
    sessionId: 1004, title: '代码风格检查',
    status: 'completed', messageCount: 4, totalTokens: 6000,
    totalCost: 0.08, lastMessageAt: '2026-07-12T16:30:00+08:00',
    createdAt: '2026-07-12T16:25:00+08:00',
  },
]

export const mockMessages: Message[] = [
  {
    messageId: 1, role: 'user',
    content: '帮我审查 https://github.com/myteam/agent-mgmt/pull/42',
    createdAt: '2026-07-14T14:30:01+08:00',
  },
  {
    messageId: 2, role: 'assistant',
    content: '## 审查报告\n\n### 严重问题 (2)\n1. **AuthService.java:45** - SQL 注入风险，未使用参数化查询\n2. **LoginController.java:23** - 未校验用户输入长度\n\n### 一般问题 (3)\n1. **AuthService.java:78** - 异常处理过于宽泛 (catch Exception)\n2. **LoginController.java:15** - 缺少请求日志\n3. **UserMapper.java:12** - N+1 查询问题\n\n### 建议\n- 使用 PreparedStatement 替代字符串拼接\n- 添加 @Valid 注解进行参数校验',
    steps: [
      {
        stepId: 1, sequence: 1, type: 'thinking', status: 'success',
        content: '用户要求审查 PR #42，我需要先获取 PR 内容，然后逐文件分析代码变更',
        startedAt: '2026-07-14T14:30:02+08:00', completedAt: '2026-07-14T14:30:03+08:00', durationMs: 1200,
      },
      {
        stepId: 2, sequence: 2, type: 'tool_call', status: 'success',
        toolName: 'GitHub API', toolIcon: '14-globe',
        request: { owner: 'myteam', repo: 'agent-mgmt', pr_number: 42 },
        response: { files: ['AuthService.java', 'LoginController.java'], additions: 45, deletions: 12 },
        startedAt: '2026-07-14T14:30:03+08:00', completedAt: '2026-07-14T14:30:04+08:00', durationMs: 800,
      },
      {
        stepId: 3, sequence: 3, type: 'thinking', status: 'success',
        content: '已获取 PR 内容，涉及 2 个文件。逐文件分析中：AuthService.java 在第45行存在 SQL 拼接...',
        startedAt: '2026-07-14T14:30:04+08:00', completedAt: '2026-07-14T14:30:07+08:00', durationMs: 3000,
      },
      {
        stepId: 4, sequence: 4, type: 'tool_call', status: 'error',
        toolName: '代码静态分析', toolIcon: '11-growth-chart',
        request: { file: 'AuthService.java' },
        response: undefined, errorMessage: '静态分析服务连接超时', retryCount: 2,
        startedAt: '2026-07-14T14:30:07+08:00', completedAt: '2026-07-14T14:30:12+08:00', durationMs: 5200,
      },
    ],
    tokenUsage: { input: 2500, output: 800, total: 3300, cost: 0.04 },
    createdAt: '2026-07-14T14:30:20+08:00',
  },
]
