<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMembers, inviteMember, updateMemberRole, removeMember } from '@/api/workspace'
import { formatDateTime } from '@/utils/format'
import type { Member } from '@/types/workspace'

const members = ref<Member[]>([])
const loading = ref(false)
const showInvite = ref(false)
const inviteForm = ref({ email: '', role: 'DEVELOPER' })
const inviting = ref(false)

async function fetchMembers() {
  loading.value = true
  try {
    const res = await getMembers()
    if (res.code === 0) members.value = res.data
  } finally {
    loading.value = false
  }
}

async function handleRoleChange(member: Member, newRole: string) {
  const res = await updateMemberRole(member.userId, newRole)
  if (res.code === 0) {
    member.role = newRole
    const labelMap: Record<string, string> = { ADMIN: '管理员', MANAGER: '管理者', DEVELOPER: '开发者', VIEWER: '只读' }
    member.roleLabel = labelMap[newRole] || newRole
    ElMessage.success(`已将 ${member.nickname} 的角色更改为 ${member.roleLabel}`)
  } else {
    // 失败回滚选择框显示
    fetchMembers()
  }
}

async function handleRemove(member: Member) {
  try {
    await ElMessageBox.confirm(`确定移除 ${member.nickname} 吗？`, '提示', { type: 'warning' })
    const res = await removeMember(member.userId)
    if (res.code === 0) {
      members.value = members.value.filter(m => m.userId !== member.userId)
      ElMessage.success('已移除')
    }
  } catch {
    // 用户取消
  }
}

async function handleInvite() {
  if (!inviteForm.value.email) { ElMessage.warning('请输入邮箱'); return }
  inviting.value = true
  try {
    const res = await inviteMember(inviteForm.value.email, inviteForm.value.role)
    if (res.code === 0) {
      ElMessage.success(`已将 ${inviteForm.value.email} 加入空间`)
      showInvite.value = false
      inviteForm.value.email = ''
      fetchMembers()
    }
  } finally {
    inviting.value = false
  }
}

onMounted(fetchMembers)
</script>

<template>
  <div class="member-manage-page">
    <div class="page-header">
      <h2>成员管理</h2>
      <el-button type="primary" @click="showInvite = true"><UiIcon name="plus" /> 邀请成员</el-button>
    </div>
    <el-card v-loading="loading" shadow="never" class="table-card">
      <el-table :data="members">
        <el-table-column label="成员" min-width="180">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:10px">
              <el-avatar :size="32" :src="row.avatar || undefined"><UiIcon name="user" :size="16" /></el-avatar>
              <div><div class="member-name">{{ row.nickname }}</div><div class="member-email">{{ row.email }}</div></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="140">
          <template #default="{ row }">
            <el-select :model-value="row.role" size="small" :disabled="row.role === 'owner'" @change="(val: string) => handleRoleChange(row, val)">
              <el-option label="管理员" value="ADMIN" /><el-option label="管理者" value="MANAGER" /><el-option label="开发者" value="DEVELOPER" /><el-option label="只读" value="VIEWER" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="Agent 数" width="90"><template #default="{ row }"><span class="num">{{ row.agentCount }}</span></template></el-table-column>
        <el-table-column label="30天会话" width="100"><template #default="{ row }"><span class="num">{{ row.sessionCount30d }}</span></template></el-table-column>
        <el-table-column label="加入时间" width="160"><template #default="{ row }">{{ row.joinedAt ? formatDateTime(row.joinedAt) : '—' }}</template></el-table-column>
        <el-table-column label="最后活跃" width="160"><template #default="{ row }">{{ row.lastActiveAt ? formatDateTime(row.lastActiveAt) : '—' }}</template></el-table-column>
        <el-table-column label="" width="70">
          <template #default="{ row }">
            <el-button v-if="row.role !== 'owner'" text type="danger" size="small" @click="handleRemove(row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showInvite" title="邀请成员" width="450px">
      <el-alert title="邀请仅对系统内已注册用户生效：按邮箱匹配用户并直接加入本空间" type="info" :closable="false" style="margin-bottom:12px" />
      <el-form label-width="80px">
        <el-form-item label="邮箱"><el-input v-model="inviteForm.email" placeholder="user@company.com" /></el-form-item>
        <el-form-item label="角色"><el-select v-model="inviteForm.role"><el-option label="管理者" value="MANAGER" /><el-option label="开发者" value="DEVELOPER" /><el-option label="只读" value="VIEWER" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="showInvite = false">取消</el-button><el-button type="primary" :loading="inviting" @click="handleInvite">加入空间</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.member-manage-page { max-width: 1200px; margin: 0 auto; }
.page-header h2 {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.2px;
}
.table-card :deep(.el-card__body) { padding: 20px; }
.member-name { font-weight: 600; font-size: 13.5px; color: var(--text-1); }
.member-email { font-size: 12px; color: var(--text-3); }
</style>
