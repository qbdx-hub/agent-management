// ==================== Agent 状态 ====================

export const AGENT_STATUS_MAP: Record<string, string> = {
  draft: '草稿',
  testing: '调试中',
  published: '已发布',
  paused: '已暂停',
  archived: '已归档',
}

export const AGENT_STATUS_COLORS: Record<string, string> = {
  draft: 'info',
  testing: 'warning',
  published: 'success',
  paused: '',
  archived: 'danger',
}

// ==================== 会话状态 ====================

export const SESSION_STATUS_MAP: Record<string, string> = {
  active: '进行中',
  completed: '已完成',
  stopped: '已停止',
  error: '错误',
}

// ==================== 步骤类型 ====================

export const STEP_TYPE_MAP: Record<string, string> = {
  thinking: '思考',
  tool_call: '工具调用',
  tool_result: '工具返回',
}

// ==================== 执行模式 ====================

export const EXECUTION_MODE_MAP: Record<string, string> = {
  auto: '自动执行',
  step_by_step: '逐步确认',
  plan_only: '只计划不执行',
}

// ==================== 工具分类 ====================

export const TOOL_CATEGORY_MAP: Record<string, string> = {
  search: '查询类',
  compute: '计算类',
  operate: '操作类',
  perceive: '感知类',
  notify: '通知类',
  custom: '自定义',
}

// ==================== 记忆策略 ====================

export const MEMORY_STRATEGY_MAP: Record<string, string> = {
  sliding_window: '滑动窗口',
  summary: '摘要压缩',
  full: '全量保留',
}

// ==================== 审批状态 ====================

export const APPROVAL_STATUS_MAP: Record<string, string> = {
  pending: '待审批',
  approved: '已通过',
  rejected: '已拒绝',
}

// ==================== 告警严重度 ====================

export const ALERT_SEVERITY_MAP: Record<string, string> = {
  info: '信息',
  warning: '警告',
  critical: '严重',
}

// ==================== 通知渠道 ====================

export const NOTIFY_CHANNEL_MAP: Record<string, string> = {
  feishu: '飞书',
  wecom: '企业微信',
  email: '邮件',
  webhook: 'Webhook',
}

// ==================== 操作审计 ====================

export const AUDIT_ACTION_MAP: Record<string, string> = {
  'agent.create': '创建 Agent',
  'agent.update': '更新 Agent',
  'agent.delete': '删除 Agent',
  'agent.status_change': '状态变更',
  'tool.create': '注册工具',
  'tool.delete': '删除工具',
  'session.start': '开始会话',
  'session.message': '发送消息',
  'api_key.create': '添加 API Key',
  'api_key.delete': '删除 API Key',
  'member.invite': '邀请成员',
  'member.remove': '移除成员',
  'role.change': '角色变更',
  'budget.update': '预算修改',
  'security.policy': '安全策略变更',
}
