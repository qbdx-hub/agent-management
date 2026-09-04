<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login, register } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { toast } from '@/utils/toast'
import AppIcon from '@/components/AppIcon.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const mode = ref<'login' | 'register'>('login')
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
  nickname: '',
  email: '',
})

async function submit() {
  if (mode.value === 'login') {
    if (!form.username.trim() || !form.password) return toast('请输入账号和密码', 'error')
    loading.value = true
    try {
      const res = await login(form.username.trim(), form.password)
      auth.setSession(res.data.token, res.data.user)
      router.replace(String(route.query.redirect || '/home'))
    } catch {
      /* 拦截器已 toast */
    } finally {
      loading.value = false
    }
  } else {
    if (!form.username.trim()) return toast('请输入用户名', 'error')
    if (!form.nickname.trim()) return toast('请输入昵称', 'error')
    if (!form.email.trim()) return toast('请输入邮箱', 'error')
    if (form.password.length < 6) return toast('密码至少 6 位', 'error')
    loading.value = true
    try {
      await register({
        username: form.username.trim(),
        nickname: form.nickname.trim(),
        email: form.email.trim(),
        password: form.password,
      })
      toast('注册成功，请登录')
      mode.value = 'login'
    } catch {
      /* 拦截器已 toast */
    } finally {
      loading.value = false
    }
  }
}
</script>

<template>
  <div class="page">
    <div class="scroll">
      <div class="pad" style="padding-top: calc(var(--safe-top) + 72px)">
        <!-- Logo -->
        <div class="logo-row">
          <div class="logo"><img src="@/assets/logo/mascot.png" alt="灵枢agent" /></div>
          <div>
            <div class="page-title">
              <img class="page-wordmark" src="@/assets/logo/wordmark.png" alt="灵枢agent" />
            </div>
            <div style="font-size: 12px; color: var(--text-2); margin-top: 2px">智能体管理平台 · 移动端</div>
          </div>
        </div>

        <!-- 登录 / 注册切换 -->
        <div class="segmented" style="margin-top: 24px">
          <div class="seg-item" :class="{ active: mode === 'login' }" @click="mode = 'login'">登录</div>
          <div class="seg-item" :class="{ active: mode === 'register' }" @click="mode = 'register'">注册</div>
        </div>

        <div class="card" style="margin-top: 14px; display: flex; flex-direction: column; gap: 12px">
          <input v-model="form.username" class="field" placeholder="用户名" autocomplete="username" />
          <input v-model="form.password" class="field" type="password" placeholder="密码" autocomplete="current-password" />
          <template v-if="mode === 'register'">
            <input v-model="form.nickname" class="field" placeholder="昵称" />
            <input v-model="form.email" class="field" type="email" placeholder="邮箱" />
          </template>
          <button class="primary-btn" style="width: 100%; margin-top: 4px" :disabled="loading" @click="submit">
            {{ loading ? '请稍候…' : mode === 'login' ? '登 录' : '注 册' }}
          </button>
        </div>

        <div class="switch-tip" @click="mode = mode === 'login' ? 'register' : 'login'">
          {{ mode === 'login' ? '没有账号？去注册' : '已有账号？去登录' }}
          <AppIcon name="arrow" :size="11" />
        </div>

        <div class="version">灵枢agent v1.0 (Build 20260902)</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.logo-row {
  display: flex;
  align-items: center;
  gap: 14px;
}
.logo {
  width: 52px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.logo img {
  width: 40px;
  height: auto;
  display: block;
}
.page-wordmark {
  height: 21px;
  width: auto;
  display: block;
}
.field {
  width: 100%;
  height: 46px;
  border-radius: 12px;
  background: var(--comp);
  border: none;
  outline: none;
  color: var(--text-1);
  font-size: 14px;
  padding: 0 14px;
}
.field::placeholder {
  color: var(--text-2);
}
.switch-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  margin-top: 18px;
  font-size: 13px;
  color: var(--brand);
  cursor: pointer;
}
</style>
