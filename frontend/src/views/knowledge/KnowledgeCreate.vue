<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { createKnowledgeBase } from '@/api/knowledge'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)

const form = reactive({
  name: '',
  description: '',
  type: 'vector' as 'vector' | 'keyword' | 'hybrid',
  embeddingModel: 'text-embedding-3-small',
  chunkSize: 500,
  chunkOverlap: 50,
})

const rules: FormRules = {
  name: [
    { required: true, message: '知识库名称不能为空', trigger: 'blur' },
    { max: 100, message: '名称不能超过100个字符', trigger: 'blur' },
  ],
  description: [
    { max: 500, message: '描述不能超过500个字符', trigger: 'blur' },
  ],
  embeddingModel: [
    { required: true, message: '请选择嵌入模型', trigger: 'change' },
  ],
}

const typeOptions = [
  { label: '向量检索', value: 'vector' },
  { label: '关键词检索', value: 'keyword' },
  { label: '混合检索', value: 'hybrid' },
]

const modelOptions = [
  { label: 'text-embedding-3-small', value: 'text-embedding-3-small' },
  { label: 'text-embedding-3-large', value: 'text-embedding-3-large' },
  { label: 'text-embedding-ada-002', value: 'text-embedding-ada-002' },
]

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const res = await createKnowledgeBase({
      name: form.name,
      description: form.description || undefined,
      type: form.type,
      embeddingModel: form.embeddingModel,
      config: {
        chunk_size: form.chunkSize,
        chunk_overlap: form.chunkOverlap,
      },
    })
    if (res.code === 0) {
      ElMessage.success('知识库创建成功')
      router.push('/knowledge')
    }
  } catch {
    // 错误已由 http 拦截器统一提示
  } finally {
    submitting.value = false
  }
}

function handleCancel() {
  router.push('/knowledge')
}
</script>

<template>
  <div class="knowledge-create-page">
    <div class="page-header">
      <div style="display:flex;align-items:center;gap:8px">
        <el-button text @click="handleCancel"><el-icon><ArrowLeft /></el-icon></el-button>
        <h2>新建知识库</h2>
      </div>
    </div>

    <el-card style="max-width:640px">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        label-position="right"
      >
        <el-form-item label="知识库名称" prop="name">
          <el-input v-model="form.name" placeholder="例如：业务知识库" maxlength="100" show-word-limit />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="简要描述知识库的用途和内容"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="检索类型" prop="type">
          <el-select v-model="form.type" style="width:100%">
            <el-option
              v-for="item in typeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="嵌入模型" prop="embeddingModel">
          <el-select v-model="form.embeddingModel" style="width:100%">
            <el-option
              v-for="item in modelOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-divider content-position="left">分块配置</el-divider>

        <el-form-item label="分块大小">
          <el-input-number v-model="form.chunkSize" :min="100" :max="2000" :step="100" />
          <span class="form-tip">Token 数</span>
        </el-form-item>

        <el-form-item label="分块重叠">
          <el-input-number v-model="form.chunkOverlap" :min="0" :max="500" :step="10" />
          <span class="form-tip">Token 数</span>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">创建知识库</el-button>
          <el-button @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.knowledge-create-page {
  max-width: 900px;
}
.page-header {
  margin-bottom: 20px;
}
.page-header h2 {
  margin: 0;
  font-size: 18px;
}
.form-tip {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}
</style>
