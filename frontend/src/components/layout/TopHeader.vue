<script setup lang="ts">
import { ref } from 'vue'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { useWorkspaceStore } from '@/stores/workspace'
import { updateProfile } from '@/api/auth'
import { ElMessageBox, ElMessage } from 'element-plus'

const appStore = useAppStore()
const userStore = useUserStore()
const wsStore = useWorkspaceStore()

const showProfile = ref(false)
const profileForm = ref({
  username: '',
  nickname: '',
  email: '',
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const profileLoading = ref(false)

function handleCommand(cmd: string) {
  if (cmd === 'profile') openProfile()
  else if (cmd === 'logout') handleLogout()
}

function handleLogout() {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' }).then(() => {
    userStore.logout()
  }).catch(() => {})
}

function handleSwitchWorkspace(id: number) {
  wsStore.switchWorkspace(id)
  window.location.reload()
}

function openProfile() {
  const user = userStore.user
  if (!user) {
    ElMessage.warning('用户信息未加载，请刷新页面后重试')
    return
  }
  profileForm.value = {
    username: user.username || '',
    nickname: user.nickname || '',
    email: user.email || '',
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  }
  showProfile.value = true
}

async function submitProfile() {
  if (!profileForm.value.username || !profileForm.value.nickname) {
    ElMessage.warning('用户名和昵称不能为空')
    return
  }
  if (profileForm.value.newPassword || profileForm.value.confirmPassword) {
    if (profileForm.value.newPassword !== profileForm.value.confirmPassword) {
      ElMessage.warning('两次输入的新密码不一致')
      return
    }
    if (!profileForm.value.oldPassword) {
      ElMessage.warning('修改密码需填写旧密码')
      return
    }
  }

  profileLoading.value = true
  try {
    const payload = {
      username: profileForm.value.username,
      nickname: profileForm.value.nickname,
      email: profileForm.value.email || undefined,
      oldPassword: profileForm.value.oldPassword || undefined,
      newPassword: profileForm.value.newPassword || undefined
    }
    const res = await updateProfile(payload)
    if (res.code === 0) {
      ElMessage.success('个人信息修改成功')
      showProfile.value = false
      // 刷新本地用户信息
      await userStore.fetchUserInfo()
    } else {
      ElMessage.warning(res.message || '修改失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '修改失败')
  } finally {
    profileLoading.value = false
  }
}
</script>

<template>
  <div class="top-header">
    <div class="top-header-left">
      <el-button
        :icon="appStore.sidebarCollapsed ? 'Expand' : 'Fold'"
        text
        @click="appStore.toggleSidebar()"
      />
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-if="$route.meta.title" :to="$route.path">
          {{ $route.meta.title }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    <div class="top-header-right">
      <!-- 工作空间切换 -->
      <el-select
        v-if="wsStore.list.length > 0"
        :model-value="wsStore.currentId"
        style="width: 160px; margin-right: 12px"
        size="small"
        @change="handleSwitchWorkspace"
      >
        <el-option
          v-for="ws in wsStore.list"
          :key="ws.id"
          :label="ws.name"
          :value="ws.id"
        />
      </el-select>

      <!-- 用户信息 -->
      <el-dropdown trigger="click" @command="handleCommand">
        <span class="user-info">
          <el-avatar :size="28" icon="UserFilled" />
          <span class="user-name">{{ userStore.user?.nickname || userStore.user?.username || '用户' }}</span>
          <el-icon><ArrowDown /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item disabled>
              {{ userStore.user?.role === 'ADMIN' ? '管理员' : '成员' }}
            </el-dropdown-item>
            <el-dropdown-item command="profile">修改个人信息</el-dropdown-item>
            <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>

  <!-- 修改个人信息弹窗 -->
  <el-dialog v-model="showProfile" title="修改个人信息" width="450px">
    <el-form :model="profileForm" label-width="90px">
      <el-form-item label="用户名">
        <el-input v-model="profileForm.username" placeholder="登录用户名" />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="profileForm.nickname" placeholder="显示昵称" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="profileForm.email" placeholder="可选" />
      </el-form-item>
      <el-divider />
      <el-form-item label="旧密码">
        <el-input v-model="profileForm.oldPassword" type="password" show-password placeholder="修改密码时必填" />
      </el-form-item>
      <el-form-item label="新密码">
        <el-input v-model="profileForm.newPassword" type="password" show-password placeholder="不修改请留空" />
      </el-form-item>
      <el-form-item label="确认密码">
        <el-input v-model="profileForm.confirmPassword" type="password" show-password placeholder="不修改请留空" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="showProfile = false">取消</el-button>
      <el-button type="primary" :loading="profileLoading" @click="submitProfile">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.top-header {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.top-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.top-header-right {
  display: flex;
  align-items: center;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 14px;
}
.user-name {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
