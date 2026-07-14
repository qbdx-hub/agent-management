<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { mockRoles } from '@/mock/security'
import type { Role } from '@/types/security'

const roles = ref<Role[]>([])
onMounted(() => { roles.value = [...mockRoles] })

function permissionLabel(perm: string) {
  const map: Record<string, string> = { 'agent:*': 'Agent 全部', 'agent:read': 'Agent 查看', 'agent:create': '创建 Agent', 'agent:update:own': '更新自己的 Agent', 'tool:*': '工具全部', 'tool:read': '工具查看', 'tool:register': '注册工具', 'session:*': '会话全部', 'monitor:*': '监控全部', 'monitor:read': '监控查看', 'cost:*': '成本全部', 'cost:read:own': '查看自己的成本', 'cost:read': '成本查看', 'security:*': '安全全部', 'workspace:*': '空间全部', 'workspace:member:invite': '邀请成员' }
  return map[perm] || perm
}
</script>

<template>
  <div class="role-manage-page">
    <div class="page-header"><h2>角色权限</h2></div>
    <el-row :gutter="20">
      <el-col v-for="role in roles" :key="role.id" :span="12">
        <el-card class="role-card mb-16">
          <div class="role-header">
            <div>
              <strong>{{ role.label }}</strong>
              <el-tag v-if="role.isSystem" size="small" type="info" style="margin-left:8px">系统</el-tag>
              <el-tag v-else size="small" style="margin-left:8px">自定义</el-tag>
            </div>
            <span class="text-muted">{{ role.memberCount }} 人</span>
          </div>
          <p class="text-muted" style="font-size:13px;margin:8px 0">{{ role.description }}</p>
          <div class="perm-list">
            <el-tag v-for="perm in role.permissions" :key="perm" size="small" style="margin:2px">{{ permissionLabel(perm) }}</el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.role-manage-page { max-width: 1200px; }
.role-card { height: 100%; }
.role-header { display: flex; align-items: center; justify-content: space-between; }
.perm-list { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 8px; }
</style>
