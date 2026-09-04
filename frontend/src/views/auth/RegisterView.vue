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
  return ['', '#cf3f4f', '#b3730f', '#5a54e8', '#178a5b'][s] || ''
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
    <!-- 背景图（AI 家族蜡笔画）+ 中心柔光 -->
    <div class="bg-image"></div>
    <div class="bg-overlay"></div>

    <!-- 居中亮色注册卡片 -->
    <div class="glass-card">
      <!-- 蜡笔涂鸦装饰（与登录页同款） -->
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

      <!-- 注册表单 -->
      <div v-if="step === 1">
        <div class="logo">
          <UiIcon name="robot" :size="30" />
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
            <circle cx="32" cy="32" r="32" fill="#e4f5ec"/>
            <path d="M20 32L28 40L44 24" stroke="#178a5b" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round"/>
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
  background: #ffffff;
}

/* 背景图全屏铺满（白底蜡笔画，cover 裁切白边无痕） */
.bg-image {
  position: absolute;
  inset: 0;
  background: url('/login-bg-ai.jpg') center center / cover no-repeat;
  transform: scale(1.02);
}

.bg-overlay {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 50% 50%, rgba(255, 255, 255, 0.78) 0%, rgba(255, 255, 255, 0.35) 42%, rgba(255, 255, 255, 0) 72%);
}

/* 亮色卡片：白底 + 描边 + 抬升阴影（V4 设计语言） */
.glass-card {
  position: relative;
  z-index: 2;
  width: 420px;
  max-width: 90vw;
  padding: 44px 40px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(10px);
  border: 1px solid var(--border-1);
  border-radius: 24px;
  box-shadow: var(--shadow-lift);
  animation: cardIn 0.6s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes cardIn {
  from { opacity: 0; transform: translateY(20px) scale(0.98); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

/* 品牌块：与登录页/侧边栏一致（accent-soft 底 + 机器人） */
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

/* 蜡笔涂鸦（与登录页同款定位） */
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
  background: rgba(22, 22, 29, 0.08);
  transition: all 0.3s;
}

.strength-label {
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  color: var(--text-2);
}

.register-btn {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  margin-top: 4px;
}

.register-btn:hover {
  transform: translateY(-1px);
}

.footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: var(--text-2);
}

.login-link {
  color: var(--accent);
  text-decoration: none;
  font-weight: 600;
  margin-left: 4px;
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
  color: var(--text-1);
  margin: 0 0 12px;
}

.success-desc {
  color: var(--text-2);
  font-size: 14px;
  margin: 0 0 32px;
}

.go-login-btn {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
}

@keyframes scaleIn {
  from { opacity: 0; transform: scale(0.5); }
  to { opacity: 1; transform: scale(1); }
}
</style>
