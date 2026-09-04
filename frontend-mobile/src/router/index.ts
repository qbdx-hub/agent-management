import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  // ---- 四个主 Tab（带底部导航）----
  { path: '/home', name: 'home', component: () => import('@/views/HomeView.vue'), meta: { tabbar: true, title: '首页' } },
  { path: '/chat-list', name: 'chat-list', component: () => import('@/views/ChatListView.vue'), meta: { tabbar: true, title: '对话' } },
  { path: '/kb', name: 'kb', component: () => import('@/views/KbListView.vue'), meta: { tabbar: true, title: '知识库' } },
  { path: '/profile', name: 'profile', component: () => import('@/views/ProfileView.vue'), meta: { tabbar: true, title: '我的' } },
  { path: '/profile/edit', name: 'profile-edit', component: () => import('@/views/ProfileEditView.vue'), meta: { title: '个人资料' } },

  // ---- 子页面（全屏，无 tabbar）----
  { path: '/login', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { public: true, title: '登录' } },
  { path: '/chat/:agentId', name: 'chat', component: () => import('@/views/ChatView.vue'), meta: { title: '对话' } },
  { path: '/agent/create', name: 'agent-create', component: () => import('@/views/AgentCreateView.vue'), meta: { title: '新建 Agent' } },
  { path: '/kb/:id', name: 'kb-detail', component: () => import('@/views/KbDetailView.vue'), meta: { title: '知识库详情' } },
  { path: '/keys', name: 'keys', component: () => import('@/views/KeysView.vue'), meta: { title: 'API 密钥' } },
  { path: '/models', name: 'models', component: () => import('@/views/ModelsView.vue'), meta: { title: '模型与偏好' } },
  { path: '/notify', name: 'notify', component: () => import('@/views/NotifyView.vue'), meta: { title: '通知设置' } },
  { path: '/help', name: 'help', component: () => import('@/views/HelpView.vue'), meta: { title: '帮助' } },

  { path: '/', redirect: '/home' },
  { path: '/:pathMatch(.*)*', redirect: '/home' },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isLoggedIn) {
    return { name: 'login', query: to.fullPath !== '/' ? { redirect: to.fullPath } : {} }
  }
  if (to.name === 'login' && auth.isLoggedIn) {
    return { name: 'home' }
  }
  document.title = to.meta.title ? `${to.meta.title} · MyAgent` : 'MyAgent'
})

export default router
