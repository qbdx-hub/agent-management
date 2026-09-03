<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getRoles, createRole, deleteRole } from '@/api/security'
import type { Role } from '@/types/security'

const roles = ref<Role[]>([])
const loading = ref(false)
const showCreate = ref(false)
const creating = ref(false)
const createForm = ref({ name: '', label: '', description: '', permissionText: 'agent:read\ntool:read' })

async function fetchRoles() {
  loading.value = true
  try {
    const res = await getRoles()
    if (res.code === 0) roles.value = res.data
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  if (!createForm.value.name.trim() || !createForm.value.label.trim()) {
    ElMessage.warning('请填写角色标识与显示名')
    return
  }
  creating.value = true
  try {
    const permissions = createForm.value.permissionText
      .split(/[\n,，]/)
      .map(s => s.trim())
      .filter(Boolean)
    const res = await createRole({
      name: createForm.value.name.trim(),
      label: createForm.value.label.trim(),
      description: createForm.value.description,
      permissions,
    })
    if (res.code === 0) {
      ElMessage.success('角色已创建')
      showCreate.value = false
      createForm.value = { name: '', label: '', description: '', permissionText: 'agent:read\ntool:read' }
      fetchRoles()
    }
  } finally {
    creating.value = false
  }
}

async function handleDelete(role: Role) {
  try {
    await ElMessageBox.confirm(`确认删除角色「${role.label}」？`, '提示', { type: 'warning' })
    const res = await deleteRole(role.id)
    if (res.code === 0) {
      ElMessage.success('已删除')
      fetchRoles()
    }
  } catch {
    // 用户取消
  }
}

function permissionLabel(perm: string) {
  const map: Record<string, string> = { 'agent:*': 'Agent 全部', 'agent:read': 'Agent 查看', 'agent:create': '创建 Agent', 'agent:update:own': '更新自己的 Agent', 'tool:*': '工具全部', 'tool:read': '工具查看', 'tool:register': '注册工具', 'session:*': '会话全部', 'monitor:*': '监控全部', 'monitor:read': '监控查看', 'cost:*': '成本全部', 'cost:read:own': '查看自己的成本', 'cost:read': '成本查看', 'security:*': '安全全部', 'workspace:*': '空间全部', 'workspace:member:invite': '邀请成员' }
  return map[perm] || perm
}

onMounted(fetchRoles)
</script>

<template>
  <div class="role-manage-page">
    <div class="page-header">
      <h2>角色权限</h2>
      <el-button type="primary" @click="showCreate = true"><el-icon><Plus /></el-icon> 新建角色</el-button>
    </div>
    <el-row v-loading="loading" :gutter="20">
      <el-col v-for="role in roles" :key="role.id" :span="12">
        <el-card class="role-card mb-16">
          <div class="role-header">
            <div>
              <strong>{{ role.label }}</strong>
              <el-tag v-if="role.isSystem" size="small" type="info" style="margin-left:8px">系统</el-tag>
              <el-tag v-else size="small" style="margin-left:8px">自定义</el-tag>
            </div>
            <div style="display:flex;align-items:center;gap:8px">
              <span class="text-muted">{{ role.memberCount }} 人</span>
              <el-button v-if="!role.isSystem" text type="danger" size="small" @click="handleDelete(role)">删除</el-button>
            </div>
          </div>
          <p class="text-muted" style="font-size:13px;margin:8px 0">{{ role.description }}</p>
          <div class="perm-list">
            <el-tag v-for="perm in role.permissions" :key="perm" size="small" style="margin:2px">{{ permissionLabel(perm) }}</el-tag>
            <span v-if="!role.permissions?.length" class="text-muted" style="font-size:12px">未配置权限</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="showCreate" title="新建角色" width="480px">
      <el-form label-width="80px">
        <el-form-item label="角色标识"><el-input v-model="createForm.name" placeholder="如 reviewer（英文标识）" /></el-form-item>
        <el-form-item label="显示名"><el-input v-model="createForm.label" placeholder="如 审核员" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="createForm.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="权限">
          <el-input v-model="createForm.permissionText" type="textarea" :rows="4" placeholder="每行一个权限标识，如 agent:read" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.role-manage-page { max-width: 1200px; }
.role-card { height: 100%; }
.role-header { display: flex; align-items: center; justify-content: space-between; }
.perm-list { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 8px; }
</style>
