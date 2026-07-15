<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const form = reactive({
  username: '',
  nickname: '',
  email: '',
  password: '',
  confirmPassword: '',
})
const loading = ref(false)
const step = ref(1)
const passwordStrength = ref(0)

function checkPasswordStrength() {
  const p = form.password
  let score = 0
  if (p.length >= 6) score++
  if (p.length >= 10) score++
  if (/[a-z]/.test(p) && /[A-Z]/.test(p)) score++
  if (/\d/.test(p)) score++
  if (/[^a-zA-Z0-9]/.test(p)) score++
  passwordStrength.value = Math.min(score, 4)
}

function getStrengthLabel(s: number) {
  return ['', '弱', '一般', '较强', '强'][s] || ''
}

function getStrengthColor(s: number) {
  return ['', '#ef4444', '#f59e0b', '#3b82f6', '#10b981'][s] || ''
}

async function handleRegister() {
  if (!form.username || !form.nickname || !form.email || !form.password) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (!/^[a-zA-Z0-9_-]{3,50}$/.test(form.username)) {
    ElMessage.warning('用户名只能包含字母、数字、下划线、短横线，长度3-50')
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    ElMessage.warning('邮箱格式不正确')
    return
  }
  if (form.password !== form.confirmPassword) {
    ElMessage.warning('两次密码不一致')
    return
  }
  if (!/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d!@#$%^&*._-]{8,50}$/.test(form.password)) {
    ElMessage.warning('密码需8-50位且包含字母和数字')
    return
  }

  loading.value = true
  try {
    await userStore.register({
      username: form.username,
      nickname: form.nickname,
      email: form.email,
      password: form.password,
    })
    step.value = 2
    ElMessage.success('注册成功')
  } catch {
    // 响应拦截器已弹出具体错误（如"用户名已被注册"），此处无需重复提示
  } finally {
    loading.value = false
  }
}

function goToLogin() {
  router.push('/login')
}
</script>

<template>
  <div class="register-page">
    <!-- 背景图 + 暗化遮罩 -->
    <div class="bg-image"></div>
    <div class="bg-overlay"></div>

    <!-- 居中毛玻璃注册卡片 -->
    <div class="glass-card">
      <!-- 注册表单 -->
      <div v-if="step === 1">
        <div class="logo">
          <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="48" height="48" rx="12" fill="rgba(255,255,255,0.18)"/>
            <path d="M24 14V34M14 24H34" stroke="#fff" stroke-width="3" stroke-linecap="round"/>
          </svg>
        </div>
        <h1 class="title">创建账号</h1>

        <el-form @submit.prevent="handleRegister" class="register-form">
          <el-form-item>
            <el-input v-model="form.username" placeholder="用户名" size="large" clearable>
              <template #prefix>
                <svg class="input-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M10 10a4 4 0 100-8 4 4 0 000 8zm-7 8a7 7 0 0114 0H3z"/></svg>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item>
            <el-input v-model="form.nickname" placeholder="昵称" size="large" clearable>
              <template #prefix>
                <svg class="input-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M17 10c0 1.657-3.134 3-7 3s-7-1.343-7-3m14 0c0-1.657-3.134-3-7-3S3 8.343 3 10m14 0v4c0 1.657-3.134 3-7 3s-7-1.343-7-3v-4"/></svg>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item>
            <el-input v-model="form.email" placeholder="邮箱地址" size="large" clearable>
              <template #prefix>
                <svg class="input-icon" viewBox="0 0 20 20" fill="currentColor"><path d="M2.003 5.884L10 9.882l7.997-3.998A2 2 0 0016 4H4a2 2 0 00-1.997 1.884z"/><path d="M18 8.118l-8 4-8-4V14a2 2 0 002 2h12a2 2 0 002-2V8.118z"/></svg>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item>
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码（至少8位，含字母和数字）"
              size="large"
              show-password
              @input="checkPasswordStrength"
            >
              <template #prefix>
                <svg class="input-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clip-rule="evenodd"/></svg>
              </template>
            </el-input>
            <div v-if="form.password" class="password-strength">
              <div class="strength-bars">
                <div
                  v-for="i in 4"
                  :key="i"
                  class="strength-bar"
                  :style="{ background: i <= passwordStrength ? getStrengthColor(passwordStrength) : '' }"
                ></div>
              </div>
              <span class="strength-label" :style="{ color: getStrengthColor(passwordStrength) }">
                {{ getStrengthLabel(passwordStrength) }}
              </span>
            </div>
          </el-form-item>

          <el-form-item>
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="确认密码"
              size="large"
              show-password
              @keyup.enter="handleRegister"
            >
              <template #prefix>
                <svg class="input-icon" viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M2.166 4.999A11.954 11.954 0 0010 1.944 11.954 11.954 0 0017.834 5c.11.65.166 1.32.166 2.001 0 5.225-3.34 9.67-8 11.317C5.34 16.67 2 12.225 2 7c0-.682.057-1.35.166-2.001zm11.541 3.708a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/></svg>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" size="large" :loading="loading" class="register-btn" @click="handleRegister">
              <span v-if="!loading">注 册</span>
              <span v-else>注册中...</span>
            </el-button>
          </el-form-item>
        </el-form>

        <div class="footer">
          <span>已有账号？</span>
          <router-link to="/login" class="login-link">返回登录</router-link>
        </div>
      </div>

      <!-- 注册成功 -->
      <div v-else class="success-box">
        <div class="success-icon">
          <svg viewBox="0 0 64 64" fill="none">
            <circle cx="32" cy="32" r="32" fill="rgba(255,255,255,0.22)"/>
            <path d="M20 32L28 40L44 24" stroke="#fff" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <h2>注册成功</h2>
        <p class="success-desc">账号已创建，现在可以登录了</p>
        <el-button type="primary" size="large" class="go-login-btn" @click="goToLogin">前往登录</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.register-page {
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

.bg-overlay {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.35);
}

/* 透明背景的玻璃卡片（保留模糊/边框/阴影，去掉白底色） */
.glass-card {
  position: relative;
  z-index: 2;
  width: 420px;
  max-width: 90vw;
  padding: 44px 40px;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.25);
  border-radius: 24px;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.3);
  animation: cardIn 0.6s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes cardIn {
  from { opacity: 0; transform: translateY(20px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
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
  margin: 0 0 32px;
  letter-spacing: 1px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
}

/* 表单适配毛玻璃 */
.register-form :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 12px;
  box-shadow: none;
  padding: 4px 16px;
  transition: all 0.25s;
}

.register-form :deep(.el-input__wrapper:hover) {
  background: rgba(255, 255, 255, 0.24);
  border-color: rgba(255, 255, 255, 0.5);
}

.register-form :deep(.el-input__wrapper.is-focus) {
  background: rgba(255, 255, 255, 0.28);
  border-color: #ffffff;
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.2);
}

.register-form :deep(.el-input__inner) {
  color: #ffffff;
  font-size: 15px;
}

.register-form :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.7);
}

.register-form :deep(.el-input__prefix) {
  color: rgba(255, 255, 255, 0.85);
}

.input-icon {
  width: 16px;
  height: 16px;
}

/* 密码强度 */
.password-strength {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
  width: 100%;
}

.strength-bars {
  display: flex;
  gap: 4px;
  flex: 1;
}

.strength-bar {
  height: 4px;
  flex: 1;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.25);
  transition: all 0.3s;
}

.strength-label {
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.4);
}

.register-btn {
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

.register-btn:hover {
  background: #ffffff;
  transform: translateY(-1px);
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.25);
}

.footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.85);
}

.login-link {
  color: #ffffff;
  text-decoration: none;
  font-weight: 600;
  margin-left: 4px;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.3);
}

.login-link:hover {
  text-decoration: underline;
}

/* 成功页 */
.success-box {
  text-align: center;
  padding: 20px 0;
}

.success-icon {
  width: 72px;
  height: 72px;
  margin: 0 auto 24px;
  animation: scaleIn 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.success-icon svg {
  width: 100%;
  height: 100%;
}

.success-box h2 {
  font-size: 24px;
  color: #ffffff;
  margin: 0 0 12px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.25);
}

.success-desc {
  color: rgba(255, 255, 255, 0.85);
  font-size: 14px;
  margin: 0 0 32px;
}

.go-login-btn {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.95);
  border: none;
  color: #1e293b;
}

.go-login-btn:hover {
  background: #ffffff;
}

@keyframes scaleIn {
  from { opacity: 0; transform: scale(0.5); }
  to { opacity: 1; transform: scale(1); }
}
</style>
