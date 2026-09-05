<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { updateProfile, uploadAvatar } from '@/api/auth'
import { resolveFileUrl } from '@/utils/file'
import { useAuthStore } from '@/stores/auth'
import { toast } from '@/utils/toast'
import AppIcon from '@/components/AppIcon.vue'

const router = useRouter()
const auth = useAuthStore()

const form = reactive({
  username: auth.user?.username || '',
  nickname: auth.user?.nickname || '',
  email: auth.user?.email || '',
  oldPassword: '',
  newPassword: '',
})

const avatarUrl = ref(resolveFileUrl(auth.user?.avatar))
const uploading = ref(false)
const saving = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

function pickAvatar() {
  fileInput.value?.click()
}

async function onFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = '' // 允许重复选择同一文件
  if (!file) return
  if (!file.type.startsWith('image/')) return toast('请选择图片文件', 'error')
  if (file.size > 10 * 1024 * 1024) return toast('头像不能超过 10MB', 'error')

  uploading.value = true
  try {
    const res = await uploadAvatar(file)
    // 服务端返回带版本参数的相对 URL，天然防缓存；补全 API 源供 <img> 直接加载
    avatarUrl.value = resolveFileUrl(res.data)
    await auth.refreshUser()
    toast('头像已更新')
  } catch {
    /* 拦截器已 toast */
  } finally {
    uploading.value = false
  }
}

/** 与后端 UserProfileForm 校验规则一致 */
function validate(): string | null {
  if (!/^[a-zA-Z0-9_-]{3,50}$/.test(form.username.trim())) {
    return '用户名只能包含字母、数字、下划线、短横线，长度 3-50'
  }
  if (!form.nickname.trim() || form.nickname.trim().length > 50) {
    return '昵称长度 1-50'
  }
  const email = form.email.trim()
  if (email && (email.length > 100 || !/^\S+@\S+\.\S+$/.test(email))) {
    return '邮箱格式不正确'
  }
  if (form.newPassword) {
    if (!/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z0-9!@#$%^&*._-]{8,50}$/.test(form.newPassword)) {
      return '新密码需 8-50 位且包含字母和数字'
    }
    if (!form.oldPassword) {
      return '请输入旧密码'
    }
  }
  return null
}

async function submit() {
  const err = validate()
  if (err) return toast(err, 'error')
  saving.value = true
  try {
    await updateProfile({
      username: form.username.trim(),
      nickname: form.nickname.trim(),
      email: form.email.trim() || undefined,
      oldPassword: form.newPassword ? form.oldPassword : undefined,
      newPassword: form.newPassword || undefined,
    })
    await auth.refreshUser()
    toast('已保存')
    router.back()
  } catch {
    /* 拦截器已 toast（用户名/邮箱重复、旧密码错误等） */
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="page">
    <div class="navbar">
      <div class="back-btn" @click="router.back()">
        <AppIcon name="back" :size="16" />
      </div>
      <span class="title">个人资料</span>
    </div>

    <div class="scroll">
      <div class="pad" style="padding-top: 4px">
        <!-- 头像：点击上传 -->
        <div class="card" style="display: flex; flex-direction: column; align-items: center; padding: 20px 16px">
          <div
            style="position: relative; cursor: pointer"
            :style="{ opacity: uploading ? 0.6 : 1 }"
            @click="pickAvatar"
          >
            <div class="avatar" style="overflow: hidden">
              <img v-if="avatarUrl" :src="avatarUrl" alt="头像" style="width: 100%; height: 100%; object-fit: cover" />
              <template v-else>{{ auth.displayName.slice(0, 1) }}</template>
            </div>
            <div class="avatar-edit-badge">
              <AppIcon name="upload" :size="10" />
            </div>
          </div>
          <div style="font-size: 12px; color: var(--text-2); margin-top: 8px">
            {{ uploading ? '上传中…' : '点击更换头像 · jpg/png ≤ 10MB' }}
          </div>
          <input ref="fileInput" type="file" accept="image/jpeg,image/png,image/webp,image/gif" style="display: none" @change="onFileChange" />
        </div>

        <div class="group-label">基本信息</div>
        <div class="card" style="display: flex; flex-direction: column; gap: 12px">
          <div>
            <div class="field-label">用户名</div>
            <input v-model="form.username" class="field" placeholder="登录账号" maxlength="50"
              autocomplete="username" />
          </div>
          <div>
            <div class="field-label">昵称</div>
            <input v-model="form.nickname" class="field" placeholder="展示名称" maxlength="50" />
          </div>
          <div>
            <div class="field-label">邮箱</div>
            <input v-model="form.email" class="field" type="email" placeholder="选填" maxlength="100"
              autocomplete="email" />
          </div>
        </div>

        <div class="group-label">修改密码</div>
        <div class="card" style="display: flex; flex-direction: column; gap: 12px">
          <div>
            <div class="field-label">旧密码</div>
            <input v-model="form.oldPassword" class="field" type="password" placeholder="不修改密码请留空"
              autocomplete="current-password" />
          </div>
          <div>
            <div class="field-label">新密码</div>
            <input v-model="form.newPassword" class="field" type="password" placeholder="8-50 位，含字母和数字"
              autocomplete="new-password" />
          </div>
        </div>

        <button class="primary-btn" :disabled="saving" @click="submit">
          {{ saving ? '保存中…' : '保存' }}
        </button>
        <div style="height: 8px" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.field-label {
  font-size: 12px;
  color: var(--text-2);
  margin-bottom: 6px;
}
.field {
  width: 100%;
  background: var(--comp);
  border: none;
  outline: none;
  border-radius: 10px;
  padding: 12px 14px;
  color: var(--text-1);
  font-size: 14px;
}
.field::placeholder {
  color: var(--text-3);
}
.avatar-edit-badge {
  position: absolute;
  right: -2px;
  bottom: -2px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--brand);
  color: #fff;
  border: 2px solid var(--card);
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
