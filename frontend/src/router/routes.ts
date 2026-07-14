import type { RouteRecordRaw } from 'vue-router'

export const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/components/layout/AppLayout.vue'),
    redirect: '/dashboard',
    children: [
      // ===== 模块一：工作台 =====
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/DashboardView.vue'),
        meta: { title: '工作台', icon: 'HomeFilled' },
      },
      // ===== 模块二：Agent 管理 =====
      {
        path: 'agents',
        name: 'AgentList',
        component: () => import('@/views/agent/AgentList.vue'),
        meta: { title: 'Agent 管理', icon: 'Cpu' },
      },
      {
        path: 'agents/create',
        name: 'AgentCreate',
        component: () => import('@/views/agent/AgentCreate.vue'),
        meta: { title: '创建 Agent', hidden: true },
      },
      {
        path: 'agents/:id',
        name: 'AgentDetail',
        component: () => import('@/views/agent/AgentDetail.vue'),
        meta: { title: 'Agent 详情', hidden: true },
        redirect: { name: 'AgentConfig' },
        children: [
          { path: 'config', name: 'AgentConfig', component: () => import('@/views/agent/AgentConfig.vue'), meta: { title: '基础配置' } },
          { path: 'prompt', name: 'PromptEditor', component: () => import('@/views/agent/PromptEditor.vue'), meta: { title: 'Prompt' } },
          { path: 'tools', name: 'ToolBinding', component: () => import('@/views/agent/ToolBinding.vue'), meta: { title: '工具绑定' } },
          { path: 'memory', name: 'MemoryConfig', component: () => import('@/views/agent/MemoryConfig.vue'), meta: { title: '记忆策略' } },
        ],
      },
      // ===== 模块三：工具市场 =====
      {
        path: 'tools',
        name: 'ToolMarket',
        component: () => import('@/views/tool/ToolMarket.vue'),
        meta: { title: '工具市场', icon: 'SetUp' },
      },
      { path: 'tools/register', name: 'ToolRegister', component: () => import('@/views/tool/ToolRegister.vue'), meta: { title: '注册工具', hidden: true } },
      { path: 'tools/:id', name: 'ToolDetail', component: () => import('@/views/tool/ToolDetail.vue'), meta: { title: '工具详情', hidden: true } },
      // ===== 模块四：编排 =====
      {
        path: 'orchestration',
        name: 'WorkflowList',
        component: () => import('@/views/orchestration/WorkflowList.vue'),
        meta: { title: 'Agent 编排', icon: 'Share' },
      },
      { path: 'orchestration/:id/edit', name: 'WorkflowEditor', component: () => import('@/views/orchestration/WorkflowEditor.vue'), meta: { title: '编排画布', hidden: true } },
      { path: 'orchestration/:id/run/:runId', name: 'WorkflowRun', component: () => import('@/views/orchestration/WorkflowRun.vue'), meta: { title: '运行记录', hidden: true } },
      // ===== 模块五：知识库 =====
      {
        path: 'knowledge',
        name: 'KnowledgeList',
        component: () => import('@/views/knowledge/KnowledgeList.vue'),
        meta: { title: '知识库', icon: 'Collection' },
      },
      { path: 'knowledge/:id', name: 'KnowledgeDetail', component: () => import('@/views/knowledge/KnowledgeDetail.vue'), meta: { title: '知识库详情', hidden: true } },
      // ===== 模块六：会话 =====
      { path: 'agents/:id/chat', name: 'SessionConsole', component: () => import('@/views/session/SessionConsole.vue'), meta: { title: '会话', hidden: true } },
      { path: 'agents/:id/sessions', name: 'SessionHistory', component: () => import('@/views/session/SessionHistory.vue'), meta: { title: '会话历史', hidden: true } },
      // ===== 模块七：监控 =====
      {
        path: 'monitor',
        name: 'MonitorDashboard',
        component: () => import('@/views/monitor/MonitorDashboard.vue'),
        meta: { title: '监控面板', icon: 'Monitor' },
      },
      { path: 'monitor/trace/:traceId', name: 'TraceDetail', component: () => import('@/views/monitor/TraceDetail.vue'), meta: { title: '链路追踪', hidden: true } },
      { path: 'monitor/alerts', name: 'AlertConfig', component: () => import('@/views/monitor/AlertConfig.vue'), meta: { title: '告警配置', hidden: true } },
      // ===== 模块八：成本 =====
      {
        path: 'cost',
        name: 'CostDashboard',
        component: () => import('@/views/cost/CostDashboard.vue'),
        meta: { title: '成本管理', icon: 'Money' },
      },
      { path: 'cost/budget', name: 'BudgetConfig', component: () => import('@/views/cost/BudgetConfig.vue'), meta: { title: '预算配置', hidden: true } },
      // ===== 模块九：安全 =====
      {
        path: 'security/audit',
        name: 'AuditLog',
        component: () => import('@/views/security/AuditLog.vue'),
        meta: { title: '审计日志', icon: 'Lock' },
      },
      { path: 'security/roles', name: 'RoleManage', component: () => import('@/views/security/RoleManage.vue'), meta: { title: '角色权限', hidden: true } },
      { path: 'security/approvals', name: 'ApprovalList', component: () => import('@/views/security/ApprovalList.vue'), meta: { title: '审批列表', hidden: true } },
      // ===== 模块十：工作空间 =====
      {
        path: 'workspace/settings',
        name: 'WorkspaceSettings',
        component: () => import('@/views/workspace/WorkspaceSettings.vue'),
        meta: { title: '空间设置', icon: 'Setting' },
      },
      { path: 'workspace/members', name: 'MemberManage', component: () => import('@/views/workspace/MemberManage.vue'), meta: { title: '成员管理', hidden: true } },
    ],
  },
  // ===== 独立页面 =====
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { title: '注册' },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/NotFound.vue'),
    meta: { title: '404' },
  },
]
