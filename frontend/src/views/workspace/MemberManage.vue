<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { mockMembers } from '@/mock/workspace'
import { ElMessage, ElMessageBox } from 'element-plus'
import { formatDateTime } from '@/utils/format'
import type { Member } from '@/types/workspace'

const members = ref<Member[]>([])
const showInvite = ref(false)
const inviteForm = ref({ email: '', role: 'DEVELOPER' })

onMounted(() => { members.value = [...mockMembers] })

function handleRoleChange(member: Member, newRole: string) {
  member.role = newRole
  const labelMap: Record<string, string> = { ADMIN: '管理员', MANAGER: '管理者', DEVELOPER: '开发者', VIEWER: '只读' }
  member.roleLabel = labelMap[newRole] || newRole
  ElMessage.success(`已将 ${member.nickname} 的角色更改为 ${member.roleLabel}`)
}

function handleRemove(member: Member) {
  ElMessageBox.confirm(`确定移除 ${member.nickname} 吗？`, '提示', { type: 'warning' }).then(() => {
    members.value = members.value.filter(m => m.userId !== member.userId)
    ElMessage.success('已移除')
  }).catch(() => {})
}

function handleInvite() {
  if (!inviteForm.value.email) { ElMessage.warning('请输入邮箱'); return }
  ElMessage.success(`已发送邀请至 ${inviteForm.value.email}`)
  showInvite.value = false
  inviteForm.value.email = ''
}
</script>

<template>
  <div class="member-manage-page">
    <div class="page-header">
      <h2>成员管理</h2>
      <el-button type="primary" @click="showInvite = true"><el-icon><Plus /></el-icon> 邀请成员</el-button>
    </div>
    <el-card>
      <el-table :data="members">
        <el-table-column label="成员" min-width="180">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:10px">
              <el-avatar :size="32" icon="UserFilled" />
              <div><div style="font-weight:500">{{ row.nickname }}</div><div class="text-muted" style="font-size:12px">{{ row.email }}</div></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="140">
          <template #default="{ row }">
            <el-select :model-value="row.role" size="small" @change="(val: string) => handleRoleChange(row, val)">
              <el-option label="管理员" value="ADMIN" /><el-option label="管理者" value="MANAGER" /><el-option label="开发者" value="DEVELOPER" /><el-option label="只读" value="VIEWER" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="Agent 数" width="80"><template #default="{ row }">{{ row.agentCount }}</template></el-table-column>
        <el-table-column label="30天会话" width="90"><template #default="{ row }">{{ row.sessionCount30d }}</template></el-table-column>
        <el-table-column label="加入时间" width="160"><template #default="{ row }">{{ formatDateTime(row.joinedAt) }}</template></el-table-column>
        <el-table-column label="最后活跃" width="160"><template #default="{ row }">{{ formatDateTime(row.lastActiveAt) }}</template></el-table-column>
        <el-table-column label="" width="70"><template #default="{ row }"><el-button text type="danger" size="small" @click="handleRemove(row)">移除</el-button></template></el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showInvite" title="邀请成员" width="450px">
      <el-form label-width="80px">
        <el-form-item label="邮箱"><el-input v-model="inviteForm.email" placeholder="user@company.com" /></el-form-item>
        <el-form-item label="角色"><el-select v-model="inviteForm.role"><el-option label="开发者" value="DEVELOPER" /><el-option label="管理者" value="MANAGER" /><el-option label="只读" value="VIEWER" /></el-select></el-form-item>
      </el-form>
      <template #footer><el-button @click="showInvite = false">取消</el-button><el-button type="primary" @click="handleInvite">发送邀请</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>.member-manage-page { max-width: 1200px; }</style>
