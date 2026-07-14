<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { TOOL_CATEGORY_MAP } from '@/utils/constants'
import { ElMessage } from 'element-plus'
import type { ToolParameter } from '@/types/tool'

const router = useRouter()
const loading = ref(false)

const form = reactive({
  name: '', displayName: '', description: '', category: 'search' as string, icon: '🔧',
  endpoint: { url: '', method: 'POST' as string, headers: {} as Record<string, string>, timeoutMs: 10000 },
  parameters: [] as ToolParameter[],
  responseMapping: '', credentialRef: '', retryOnFail: true, maxRetries: 2,
})

const headerKey = ref('')
const headerValue = ref('')

function addHeader() {
  if (headerKey.value) { form.endpoint.headers[headerKey.value] = headerValue.value; headerKey.value = ''; headerValue.value = '' }
}
function removeHeader(key: string) { delete form.endpoint.headers[key] }

function addParameter() {
  form.parameters.push({ name: '', type: 'string', required: false, description: '', defaultValue: '' })
}
function removeParameter(idx: number) { form.parameters.splice(idx, 1) }

const categories = Object.entries(TOOL_CATEGORY_MAP).map(([value, label]) => ({ value, label }))

async function handleSubmit() {
  if (!form.name || !form.displayName) { ElMessage.warning('请填写工具名称'); return }
  loading.value = true
  try {
    ElMessage.success('工具注册成功')
    router.push('/tools')
  } catch { ElMessage.error('注册失败') } finally { loading.value = false }
}
</script>

<template>
  <div class="tool-register-page">
    <div class="page-header"><h2>注册工具</h2><el-button @click="router.push('/tools')">返回列表</el-button></div>
    <el-card>
      <el-form label-width="120px" style="max-width:800px">
        <el-divider content-position="left">基础信息</el-divider>
        <el-form-item label="标识名" required><el-input v-model="form.name" placeholder="如 search_web" /></el-form-item>
        <el-form-item label="显示名称" required><el-input v-model="form.displayName" placeholder="如 网页搜索" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category"><el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" /></el-select>
        </el-form-item>
        <el-form-item label="图标"><el-input v-model="form.icon" style="width:80px" /></el-form-item>

        <el-divider content-position="left">Endpoint 配置</el-divider>
        <el-form-item label="URL"><el-input v-model="form.endpoint.url" placeholder="https://api.example.com/v1/search" /></el-form-item>
        <el-form-item label="Method">
          <el-select v-model="form.endpoint.method" style="width:120px">
            <el-option v-for="m in ['GET','POST','PUT','DELETE','PATCH']" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="Headers">
          <div style="display:flex;gap:8px;flex-wrap:wrap;margin-bottom:8px">
            <el-tag v-for="(val, key) in form.endpoint.headers" :key="key" closable @close="removeHeader(key as string)">{{ key }}: {{ val }}</el-tag>
          </div>
          <div style="display:flex;gap:8px">
            <el-input v-model="headerKey" size="small" placeholder="Key" style="width:150px" />
            <el-input v-model="headerValue" size="small" placeholder="Value" style="width:250px" />
            <el-button size="small" @click="addHeader">添加</el-button>
          </div>
        </el-form-item>
        <el-form-item label="超时(ms)"><el-input-number v-model="form.endpoint.timeoutMs" :min="1000" :max="60000" :step="1000" /></el-form-item>

        <el-divider content-position="left">参数定义</el-divider>
        <el-button size="small" @click="addParameter" style="margin-bottom:12px"><el-icon><Plus /></el-icon> 添加参数</el-button>
        <el-table v-if="form.parameters.length > 0" :data="form.parameters" border size="small">
          <el-table-column label="名称" width="150"><template #default="{ row }"><el-input v-model="row.name" size="small" /></template></el-table-column>
          <el-table-column label="类型" width="120">
            <template #default="{ row }">
              <el-select v-model="row.type" size="small">
                <el-option v-for="t in ['string','integer','number','boolean','object','array']" :key="t" :label="t" :value="t" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="必填" width="70"><template #default="{ row }"><el-checkbox v-model="row.required" /></template></el-table-column>
          <el-table-column label="描述"><template #default="{ row }"><el-input v-model="row.description" size="small" /></template></el-table-column>
          <el-table-column label="默认值" width="150"><template #default="{ row }"><el-input v-model="row.defaultValue" size="small" /></template></el-table-column>
          <el-table-column label="" width="60"><template #default="{ $index }"><el-button text type="danger" size="small" @click="removeParameter($index)">删除</el-button></template></el-table-column>
        </el-table>

        <el-divider content-position="left">响应映射</el-divider>
        <el-form-item label="映射模板"><el-input v-model="form.responseMapping" type="textarea" :rows="4" placeholder="用 {{field}} 语法映射响应字段" /></el-form-item>

        <el-divider content-position="left">高级</el-divider>
        <el-form-item label="密钥引用"><el-input v-model="form.credentialRef" placeholder="如 api_key" /></el-form-item>
        <el-form-item label="失败重试"><el-switch v-model="form.retryOnFail" /></el-form-item>
        <el-form-item v-if="form.retryOnFail" label="最大重试"><el-input-number v-model="form.maxRetries" :min="1" :max="5" /></el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">注册工具</el-button>
          <el-button @click="router.push('/tools')">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>.tool-register-page { max-width: 1000px; }</style>
