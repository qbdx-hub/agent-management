<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useWorkspaceStore } from '@/stores/workspace'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const wsStore = useWorkspaceStore()

const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    // 登录响应已返回 workspaces，直接选第一个为默认空间；
    // 不调 fetchMyWorkspaces（后端尚未实现该接口，避免 404 阻塞跳转）
    if (!wsStore.currentId && userStore.workspaces.length > 0) {
      wsStore.switchWorkspace(userStore.workspaces[0].id)
    }
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch {
    ElMessage.error('登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <!-- 背景图（AI 家族蜡笔画）+ 中心柔光，保证卡片可读 -->
    <div class="bg-image"></div>
    <div class="bg-overlay"></div>

    <!-- 居中亮色登录卡片 -->
    <div class="glass-card">
      <!-- 蜡笔涂鸦装饰（取插画吉祥物色） -->
      <svg class="doodle d-star" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M12 3.2l2.5 5.1 5.6.8-4 4 1 5.7-5.1-2.7-5.1 2.7 1-5.7-4-4 5.6-.8z" stroke="#e8c34a" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <svg class="doodle d-heart" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M12 20.5C7.3 16.3 3.5 13 3.5 9 3.5 6.4 5.5 4.5 7.8 4.5c1.7 0 3.2 1 4.2 2.5 1-1.5 2.5-2.5 4.2-2.5 2.3 0 4.3 1.9 4.3 4.5 0 4-3.8 7.3-8.5 11.5z" stroke="#e87a90" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <svg class="doodle d-spark" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M12 3.5c.5 4.6 3.9 8 8.5 8.5-4.6.5-8 3.9-8.5 8.5-.5-4.6-3.9-8-8.5-8.5 4.6-.5 8-3.9 8.5-8.5z" stroke="#e8973f" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
      <svg class="doodle d-note" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M9.5 17.5V6l9-1.8v11.3" stroke="#9b6bc3" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        <circle cx="7" cy="17.5" r="2.4" stroke="#9b6bc3" stroke-width="2"/>
        <circle cx="16" cy="15.5" r="2.4" stroke="#9b6bc3" stroke-width="2"/>
      </svg>
      <svg class="doodle d-wave" viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M3 13c1.8-3.2 3.7-3.2 5.5 0s3.7 3.2 5.5 0 3.7-3.2 5.5 0" stroke="#4d8fd0" stroke-width="2" stroke-linecap="round"/>
      </svg>

      <div class="logo">
        <UiIcon name="robot" :size="30" />
      </div>
      <h1 class="title">Agent 管理系统</h1>

      <el-form @submit.prevent="handleLogin" class="login-form">
        <el-form-item>
          <el-input
            v-model="form.username"
            placeholder="用户名 / 邮箱"
            size="large"
            clearable
          >
            <template #prefix>
              <svg class="input-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M10 10a4 4 0 100-8 4 4 0 000 8zm-7 8a7 7 0 0114 0H3z"/></svg>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          >
            <template #prefix>
              <svg class="input-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clip-rule="evenodd"/></svg>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            <span v-if="!loading">登 录</span>
            <span v-else>登录中...</span>
          </el-button>
        </el-form-item>
      </el-form>

      <div class="footer">
        <span>还没有账号？</span>
        <router-link to="/register" class="register-link">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  position: relative;
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ffffff;
}

/* 背景图全屏铺满（白底蜡笔画，cover 裁切白边无痕） */
.bg-image {
  position: absolute;
  inset: 0;
  background: url('/login-bg-ai.jpg') center center / cover no-repeat;
  transform: scale(1.02);
}

/* 中心柔光：卡片区域留白更干净，插画在四周保持可见 */
.bg-overlay {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.78) 0%, rgba(255, 255, 255, 0.35) 42%, rgba(255, 255, 255, 0) 72%);
}

/* 亮色卡片：白底 + 描边 + 抬升阴影（V4 设计语言） */
.glass-card {
  position: relative;
  z-index: 2;
  width: 400px;
  max-width: 90vw;
  padding: 44px 40px 40px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  border: 1px solid var(--border-1);
  border-radius: 24px;
  box-shadow: var(--shadow-lift);
  animation: cardIn 0.6s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes cardIn {
  from {
    opacity: 0;
    transform: translateY(20px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@media (prefers-reduced-motion: reduce) {
  .glass-card {
    animation: none;
  }
}

/* 品牌块：与侧边栏 logo-mark 同款（accent-soft 底 + 机器人） */
.logo {
  width: 56px;
  height: 56px;
  margin: 0 auto 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--accent-soft);
  border-radius: 16px;
  color: var(--accent);
}

/* 蜡笔涂鸦：手绘线条小图，点缀卡片四角 */
.doodle {
  position: absolute;
  pointer-events: none;
  opacity: 0.8;
}

.d-star {
  width: 35px;
  top: 16px;
  right: 240px;
  transform: rotate(-14deg);
}

.d-heart {
  width: 28px;
  top: 58px;
  right: 14px;
  transform: rotate(16deg);
}

.d-spark {
  width: 27px;
  bottom: 26px;
  left: 14px;
  transform: rotate(10deg);
}

.d-note {
  width: 32px;
  bottom: 16px;
  right: 26px;
  transform: rotate(12deg);
}

.d-wave {
  width: 40px;
  top: 104px;
  left: 12px;
  transform: rotate(-6deg);
}

.title {
  text-align: center;
  color: var(--text-1);
  font-size: 24px;
  font-weight: 800;
  margin: 0 0 32px;
  letter-spacing: 0.5px;
}

.input-icon {
  width: 16px;
  height: 16px;
  color: var(--text-3);
}

/* 登录按钮（tokens.css 已把 EP primary 改绑为品牌色） */
.login-btn {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  margin-top: 4px;
}

.login-btn:hover {
  transform: translateY(-1px);
}

.login-btn:active {
  transform: translateY(0);
}

/* 底部注册入口 */
.footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: var(--text-2);
}

.register-link {
  color: var(--accent);
  text-decoration: none;
  font-weight: 600;
  margin-left: 4px;
}

.register-link:hover {
  text-decoration: underline;
}
</style>
