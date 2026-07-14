<script setup lang="ts">
import { reactive } from 'vue'
import { useAgentStore } from '@/stores/agent'
import { ElMessage } from 'element-plus'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; created: [id: number] }>()

const agentStore = useAgentStore()

const form = reactive({
  name: '',
  description: '',
  avatar: '🤖',
  tags: [] as string[],
  status: 'draft' as const,
})

const tagInput = ref('')
const loading = ref(false)

const avatarOptions = ['🤖', '🔍', '📄', '📊', '💬', '🚀', '🛠️', '🎨', '📝', '🔬', '💡', '🎯']

function addTag() {
  if (tagInput.value && !form.tags.includes(tagInput.value)) {
    form.tags.push(tagInput.value)
    tagInput.value = ''
  }
}

function removeTag(tag: string) {
  form.tags = form.tags.filter(t => t !== tag)
}

async function handleSubmit() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入 Agent 名称')
    return
  }
  loading.value = true
  try {
    const id = await agentStore.createAgent({ ...form })
    ElMessage.success('创建成功')
    emit('created', id)
    // 重置
    form.name = ''
    form.description = ''
    form.avatar = '🤖'
    form.tags = []
  } catch {
    ElMessage.error('创建失败')
  } finally {
    loading.value = false
  }
}

function handleClose() {
  emit('update:modelValue', false)
}

import { ref } from 'vue'
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="快速创建 Agent"
    width="500px"
    @close="handleClose"
  >
    <el-form label-position="top">
      <el-form-item label="头像">
        <div class="avatar-grid">
          <span
            v-for="a in avatarOptions"
            :key="a"
            class="avatar-option"
            :class="{ active: form.avatar === a }"
            @click="form.avatar = a"
          >
            {{ a }}
          </span>
        </div>
      </el-form-item>

      <el-form-item label="名称" required>
        <el-input v-model="form.name" placeholder="例如：代码审查助手" maxlength="50" />
      </el-form-item>

      <el-form-item label="描述">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          placeholder="描述这个 Agent 的用途..."
          maxlength="200"
        />
      </el-form-item>

      <el-form-item label="标签">
        <div style="display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 8px">
          <el-tag v-for="tag in form.tags" :key="tag" closable @close="removeTag(tag)">
            {{ tag }}
          </el-tag>
        </div>
        <el-input
          v-model="tagInput"
          size="small"
          placeholder="输入标签后回车"
          @keyup.enter="addTag"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">
        创建
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.avatar-grid {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.avatar-option {
  font-size: 24px;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid transparent;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.avatar-option:hover {
  background: var(--el-fill-color-light);
}
.avatar-option.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
</style>
