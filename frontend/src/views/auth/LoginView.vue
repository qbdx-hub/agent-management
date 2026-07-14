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
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    await wsStore.fetchMyWorkspaces()
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
    <!-- 背景图 + 暗化遮罩 -->
    <div class="bg-image"></div>
    <div class="bg-overlay"></div>

    <!-- 居中毛玻璃登录卡片 -->
    <div class="glass-card">
      <div class="logo">
        <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
          <rect width="48" height="48" rx="12" fill="rgba(255,255,255,0.18)"/>
          <path d="M14 24L20 30L34 16" stroke="#fff" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </div>
      <h1 class="title">Agent 管理系统</h1>

      <el-form @submit.prevent="handleLogin" class="login-form">
        <el-form-item>
          <el-input
            v-model="form.username"
            placeholder="用户名"
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
}

/* 背景图全屏铺满 */
.bg-image {
  position: absolute;
  inset: 0;
  background: url('/login-bg.png') center center / cover no-repeat;
  transform: scale(1.02);
}

/* 暗化遮罩，让玻璃卡片更突出 */
.bg-overlay {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.35);
}

/* 透明背景的玻璃卡片（保留模糊/边框/阴影，去掉白底色） */
.glass-card {
  position: relative;
  z-index: 2;
  width: 400px;
  max-width: 90vw;
  padding: 48px 40px;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 24px;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.3);
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

.logo {
  width: 56px;
  height: 56px;
  margin: 0 auto 20px;
}

.logo svg {
  width: 100%;
  height: 100%;
  filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.2));
}

.title {
  text-align: center;
  color: #ffffff;
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 36px;
  letter-spacing: 1px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
}

/* 表单元素适配毛玻璃 */
.login-form :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 12px;
  box-shadow: none;
  padding: 4px 16px;
  transition: all 0.25s;
}

.login-form :deep(.el-input__wrapper:hover) {
  background: rgba(255, 255, 255, 0.24);
  border-color: rgba(255, 255, 255, 0.5);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  background: rgba(255, 255, 255, 0.28);
  border-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.2);
}

.login-form :deep(.el-input__inner) {
  color: #ffffff;
  font-size: 15px;
}

.login-form :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.7);
}

.login-form :deep(.el-input__prefix) {
  color: rgba(255, 255, 255, 0.85);
}

.input-icon {
  width: 16px;
  height: 16px;
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  background: rgba(255, 255, 255, 0.95);
  border: none;
  color: #1e293b;
  transition: all 0.25s;
}

.login-btn:hover {
  background: #ffffff;
  transform: translateY(-1px);
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.25);
}

.login-btn:active {
  transform: translateY(0);
}

/* 底部注册入口 */
.footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
}

.register-link {
  color: #ffffff;
  text-decoration: none;
  font-weight: 600;
  margin-left: 4px;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.3);
}

.register-link:hover {
  text-decoration: underline;
}
</style>
