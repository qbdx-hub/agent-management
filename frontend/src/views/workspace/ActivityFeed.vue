<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getActivities } from '@/api/workspace'
import { formatDateTime, timeAgo } from '@/utils/format'
import type { ActivityLog } from '@/types/workspace'

const router = useRouter()
const activities = ref<ActivityLog[]>([])
const loading = ref(false)
const pagination = reactive({ page: 1, pageSize: 20, total: 0 })

/** 常见操作类型的中文标签（未覆盖的类型原样展示） */
const ACTION_LABELS: Record<string, string> = {
  'agent.create': '创建 Agent', 'agent.update': '更新 Agent', 'agent.delete': '删除 Agent',
  'agent.test': '测试 Agent', 'tool.create': '注册工具', 'tool.update': '更新工具',
  'tool.delete': '删除工具', 'tool.test': '测试工具', 'workflow.create': '创建工作流',
  'workflow.save': '保存画布', 'workflow.run': '运行工作流', 'workflow.approve': '工作流审批',
  'workflow.delete': '删除工作流', 'knowledge.create': '新建知识库', 'knowledge.delete': '删除知识库',
  'document.upload': '上传文档', 'document.delete': '删除文档', 'member.invite': '邀请成员',
  'member.role_update': '修改成员角色', 'member.remove': '移除成员',
  'workspace.create': '创建空间', 'workspace.update': '更新空间设置',
  'budget.create': '创建预算', 'budget.update': '更新预算', 'budget.delete': '删除预算',
  'alert.create': '创建告警规则', 'alert.delete': '删除告警规则', 'role.create': '创建角色',
  'approval.approve': '处理审批', 'user.login': '登录',
}

function actionLabel(t: string) {
  return ACTION_LABELS[t] || t
}

async function fetchList() {
  loading.value = true
  try {
    const res = await getActivities(pagination.page, pagination.pageSize)
    if (res.code === 0) {
      activities.value = res.data.list
      pagination.total = res.data.total
    }
  } finally {
    loading.value = false
  }
}

onMounted(fetchList)
</script>

<template>
  <div class="activity-feed-page">
    <div class="page-header">
      <div style="display:flex;align-items:center;gap:8px">
        <el-button text @click="router.push('/workspace/settings')"><UiIcon name="arrow-left" /></el-button>
        <h2>空间动态</h2>
      </div>
    </div>
    <el-card v-loading="loading">
      <el-empty v-if="!loading && activities.length === 0" description="暂无动态：创建 Agent、注册工具、运行工作流等操作会自动记录到这里" />
      <el-timeline v-else>
        <el-timeline-item
          v-for="item in activities"
          :key="item.createdAt + item.type"
          :timestamp="`${formatDateTime(item.createdAt)} · ${timeAgo(item.createdAt)}`"
          placement="top"
        >
          <div class="activity-row">
            <el-tag size="small" type="info">{{ actionLabel(item.type) }}</el-tag>
            <span><strong>{{ item.userName || '系统' }}</strong> {{ item.description }}</span>
          </div>
        </el-timeline-item>
      </el-timeline>
      <div v-if="pagination.total > pagination.pageSize" style="margin-top:12px;display:flex;justify-content:flex-end">
        <el-pagination
          layout="prev, pager, next"
          :total="pagination.total"
          :page-size="pagination.pageSize"
          :current-page="pagination.page"
          @current-change="(p: number) => { pagination.page = p; fetchList() }"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.activity-feed-page { max-width: 800px; margin: 0 auto; }
.activity-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
</style>
