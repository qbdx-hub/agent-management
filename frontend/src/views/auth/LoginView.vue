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
    <div class="login-card">
      <div class="login-header">
        <div class="login-logo">🤖</div>
        <h1>Agent 管理系统</h1>
        <p>AI Agent 全生命周期管理平台</p>
      </div>
      <el-form @submit.prevent="handleLogin">
        <el-form-item>
          <el-input
            v-model="form.username"
            placeholder="用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            style="width: 100%"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <p class="login-tip">开发模式：任意账号密码即可登录</p>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}
.login-header {
  text-align: center;
  margin-bottom: 32px;
}
.login-logo {
  font-size: 48px;
  margin-bottom: 12px;
}
.login-header h1 {
  font-size: 24px;
  margin: 0 0 8px;
  color: #333;
}
.login-header p {
  color: #999;
  margin: 0;
  font-size: 14px;
}
.login-tip {
  text-align: center;
  color: #bbb;
  font-size: 12px;
  margin: 0;
}
</style>
