<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { VueFlow, useVueFlow, Position, Handle, type Connection } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'
import '@vue-flow/minimap/dist/style.css'
import { getWorkflow, saveWorkflow } from '@/api/workflow'
import { getToolList } from '@/api/tool'
import { useAgentStore } from '@/stores/agent'
import type { WorkflowStatus } from '@/types/workflow'

const route = useRoute()
const router = useRouter()
const workflowId = Number(route.params.id)

const agentStore = useAgentStore()

const form = reactive({
  name: '',
  description: '',
  status: 'draft' as WorkflowStatus,
})

// Vue Flow 的 Node/Edge 泛型（GraphNode）递归过深会触发 TS2589，这里用 any[] 规避；
// 节点结构：{ id, type:'custom', position:{x,y}, data:{label,nodeType,agentId?,agentName?,toolId?,toolName?,config?} }
const nodes = ref<any[]>([])
const edges = ref<any[]>([])
const selectedNode = ref<any>(null)
const loading = ref(false)
const saving = ref(false)

// 可选资源：已有 Agent / 工具列表（用于 agent/tool 节点关联）
const agentList = computed<any[]>(() => agentStore.list as any[])
const toolList = ref<any[]>([])

// data 字段统一用 any 访问（Vue Flow 节点的业务数据：label/nodeType/agentId/toolId/config）
const selectedData = computed<any>(() => selectedNode.value?.data)

const { onConnect, addEdges, onNodeClick, onPaneClick } = useVueFlow()

// 从节点右侧 Handle 拉线到目标节点左侧 Handle -> 创建 edge
onConnect((connection: Connection) => {
  addEdges({
    ...connection,
    id: 'e_' + Date.now(),
    type: 'smoothstep',
    data: { condition: null },
  })
})

onNodeClick(({ node }) => {
  selectedNode.value = node
})

onPaneClick(() => {
  selectedNode.value = null
})

const NODE_ICONS: Record<string, string> = {
  start: 'CaretRight', agent: 'Cpu', tool: 'Tools', condition: 'Share', approval: 'Avatar', end: 'VideoPause',
}
const NODE_LABELS: Record<string, string> = {
  start: '开始', agent: 'Agent 节点', tool: '工具节点', condition: '条件判断', approval: '人工审批', end: '结束',
}
const NODE_TYPE_LABEL: Record<string, string> = {
  start: '开始', agent: 'Agent', tool: '工具', condition: '条件判断', approval: '人工审批', end: '结束',
}

function iconOf(t: string) {
  return NODE_ICONS[t] || 'Grid'
}

// 添加节点；agent/tool 类型需关联一个已有资源（ref 携带 id+name）
function addNode(nodeType: string, ref?: { id: number; name: string }) {
  const id = 'n_' + Date.now()
  const data: any = { label: NODE_LABELS[nodeType] || '节点', nodeType }
  if (ref) {
    data.label = ref.name
    if (nodeType === 'agent') {
      data.agentId = ref.id
      data.agentName = ref.name
    } else if (nodeType === 'tool') {
      data.toolId = ref.id
      data.toolName = ref.name
    }
  }
  nodes.value = [
    ...nodes.value,
    {
      id,
      type: 'custom',
      position: { x: 250 + Math.random() * 80, y: 120 + nodes.value.length * 24 },
      data,
    },
  ]
  selectedNode.value = nodes.value[nodes.value.length - 1]
}

// agent / tool 节点：先弹窗选择已有资源；其余类型直接添加
const pickType = ref<'agent' | 'tool' | null>(null)
const pickDialogVisible = ref(false)
const pickKeyword = ref('')

function onClickAddNode(nodeType: string) {
  if (nodeType === 'agent' || nodeType === 'tool') {
    pickType.value = nodeType
    pickKeyword.value = ''
    pickDialogVisible.value = true
  } else {
    addNode(nodeType)
  }
}

const pickOptions = computed(() => {
  const list = pickType.value === 'agent' ? agentList.value : toolList.value
  const kw = pickKeyword.value.trim().toLowerCase()
  if (!kw) return list
  return list.filter((x: any) => String(x.displayName || x.name || '').toLowerCase().includes(kw))
})

function confirmPick(item: any) {
  if (!pickType.value) return
  const name = item.displayName || item.name
  addNode(pickType.value, { id: item.id, name })
  pickDialogVisible.value = false
  pickType.value = null
}

// 属性面板：改选 agent / 工具时同步名称
function onAgentChange(id: any) {
  const a = agentList.value.find(x => x.id === id)
  if (a && selectedData.value) {
    selectedData.value.agentName = a.name
    selectedData.value.label = a.name
  }
}
function onToolChange(id: any) {
  const t = toolList.value.find(x => x.id === id)
  if (t && selectedData.value) {
    selectedData.value.toolName = t.displayName
    selectedData.value.label = t.displayName
  }
}

function removeSelectedNode() {
  if (!selectedNode.value) return
  const id = selectedNode.value.id
  nodes.value = nodes.value.filter(n => n.id !== id)
  edges.value = edges.value.filter(e => e.source !== id && e.target !== id)
  selectedNode.value = null
}

function handleRun() {
  ElMessage.info('运行功能待接入')
}

async function load() {
  loading.value = true
  try {
    const res = await getWorkflow(workflowId)
    if (res.code === 0) {
      const d = res.data
      form.name = d.name
      form.description = d.description || ''
      form.status = d.status
      nodes.value = d.nodes.map(n => ({
        id: n.nodeId,
        type: 'custom',
        position: { x: n.positionX, y: n.positionY },
        data: {
          label: n.label,
          nodeType: n.type,
          agentId: n.agentId,
          toolId: n.toolId,
          config: n.config,
        },
      }))
      edges.value = d.edges.map(e => ({
        id: e.edgeId,
        source: e.sourceNodeId,
        target: e.targetNodeId,
        label: e.label,
        type: 'smoothstep',
        data: { condition: e.condition },
      }))
    }
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写工作流名称')
    return
  }
  saving.value = true
  try {
    const res = await saveWorkflow(workflowId, {
      name: form.name,
      description: form.description,
      status: form.status,
      nodes: nodes.value.map(n => ({
        nodeId: n.id,
        type: (n.data as any)?.nodeType || 'agent',
        label: (n.data as any)?.label || '',
        agentId: (n.data as any)?.agentId,
        toolId: (n.data as any)?.toolId,
        config: (n.data as any)?.config,
        positionX: n.position.x,
        positionY: n.position.y,
      })),
      edges: edges.value.map(e => ({
        edgeId: e.id,
        sourceNodeId: e.source,
        targetNodeId: e.target,
        label: e.label as string | undefined,
        condition: (e.data as any)?.condition,
      })),
    })
    if (res.code === 0) ElMessage.success('画布已保存')
  } catch {
    // 错误已由 axios 响应拦截器统一提示
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  load()
  // 拉取已有 Agent / 工具列表，供 agent/tool 节点选择关联
  agentStore.fetchAgentList().catch(() => {})
  getToolList({ page: 1, pageSize: 200 })
    .then(res => { if (res.code === 0) toolList.value = res.data.list })
    .catch(() => {})
})
</script>

<template>
  <div class="workflow-editor-page" v-loading="loading">
    <div class="page-header">
      <div style="display:flex;align-items:center;gap:8px">
        <el-button text @click="router.push('/orchestration')"><el-icon><ArrowLeft /></el-icon></el-button>
        <el-input v-model="form.name" style="width:240px" placeholder="工作流名称" />
        <el-tag size="small">{{ form.status }}</el-tag>
      </div>
      <div style="display:flex;gap:8px">
        <el-button :loading="saving" @click="handleSave"><el-icon><Check /></el-icon> 保存</el-button>
        <el-button type="primary" @click="handleRun"><el-icon><VideoPlay /></el-icon> 运行</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <!-- 画布 -->
      <el-col :span="16">
        <el-card class="canvas-card">
          <VueFlow
            v-model:nodes="nodes"
            v-model:edges="edges"
            fit-view-on-init
            class="vue-flow-canvas"
          >
            <Background :gap="20" pattern-color="#dcdfe6" />
            <Controls />
            <MiniMap />

            <!-- 统一自定义节点：按 data.nodeType 区分图标 / 配色 -->
            <template #node-custom="props">
              <div
                class="wf-node"
                :class="props.data.nodeType"
                :style="{ outline: selectedNode?.id === props.id ? '2px solid var(--el-color-primary)' : 'none' }"
              >
                <Handle type="target" :position="Position.Left" />
                <el-icon class="wf-node-icon"><component :is="iconOf(props.data.nodeType)" /></el-icon>
                <div class="wf-node-label">{{ props.data.label }}</div>
                <Handle type="source" :position="Position.Right" />
              </div>
            </template>
          </VueFlow>
          <div class="canvas-tip text-muted">拖拽节点移动位置 · 从节点右侧拉线到目标节点左侧创建连线 · 滚轮缩放 · 拖拽空白平移画布</div>
        </el-card>
      </el-col>

      <!-- 右侧：属性 + 添加节点 -->
      <el-col :span="8">
        <el-card v-if="selectedNode">
          <template #header><span>节点属性</span></template>
          <el-form label-width="72px">
            <el-form-item label="类型"><el-tag>{{ NODE_TYPE_LABEL[selectedData?.nodeType] || selectedData?.nodeType }}</el-tag></el-form-item>
            <el-form-item label="名称">
              <el-input
                v-model="selectedData.label"
                :disabled="selectedData?.nodeType === 'agent' || selectedData?.nodeType === 'tool'"
              />
            </el-form-item>
            <el-form-item v-if="selectedData?.nodeType === 'agent'" label="Agent">
              <el-select
                v-model="selectedData.agentId"
                placeholder="选择已有 Agent"
                filterable
                style="width:100%"
                @change="onAgentChange"
              >
                <el-option v-for="a in agentList" :key="a.id" :label="a.name" :value="a.id" />
              </el-select>
            </el-form-item>
            <el-form-item v-if="selectedData?.nodeType === 'tool'" label="工具">
              <el-select
                v-model="selectedData.toolId"
                placeholder="选择已有工具"
                filterable
                style="width:100%"
                @change="onToolChange"
              >
                <el-option v-for="t in toolList" :key="t.id" :label="t.displayName" :value="t.id" />
              </el-select>
            </el-form-item>
          </el-form>
          <el-button type="danger" plain size="small" style="width:100%" @click="removeSelectedNode">删除节点</el-button>
        </el-card>
        <el-card v-else>
          <el-empty description="点击节点查看属性" :image-size="60" />
        </el-card>

        <el-card style="margin-top:16px">
          <template #header><span>添加节点</span></template>
          <div style="display:flex;flex-direction:column;gap:8px">
            <el-button @click="addNode('start')"><el-icon class="ii"><CaretRight /></el-icon>开始</el-button>
            <el-button @click="onClickAddNode('agent')"><el-icon class="ii"><Cpu /></el-icon>Agent 节点</el-button>
            <el-button @click="onClickAddNode('tool')"><el-icon class="ii"><Tools /></el-icon>工具节点</el-button>
            <el-button @click="addNode('condition')"><el-icon class="ii"><Share /></el-icon>条件节点</el-button>
            <el-button @click="addNode('approval')"><el-icon class="ii"><Avatar /></el-icon>审批节点</el-button>
            <el-button @click="addNode('end')"><el-icon class="ii"><VideoPause /></el-icon>结束</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 选择已有 Agent / 工具 对话框 -->
    <el-dialog v-model="pickDialogVisible" :title="pickType === 'agent' ? '选择 Agent' : '选择工具'" width="480px">
      <el-input v-model="pickKeyword" placeholder="搜索名称..." clearable prefix-icon="Search" style="margin-bottom:12px" />
      <div class="pick-list">
        <div v-for="item in pickOptions" :key="item.id" class="pick-item" @click="confirmPick(item)">
          <el-icon v-if="pickType === 'agent'" class="ii"><Cpu /></el-icon>
          <el-icon v-else class="ii"><Tools /></el-icon>
          <span>{{ item.displayName || item.name }}</span>
        </div>
        <el-empty v-if="pickOptions.length === 0" :description="pickType === 'agent' ? '没有可选的 Agent，请先创建 Agent' : '没有可选的工具，请先注册工具'" :image-size="60" />
      </div>
      <template #footer>
        <el-button @click="pickDialogVisible = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.workflow-editor-page { max-width: 1400px; }
.canvas-card { position: relative; }
.vue-flow-canvas { height: 560px; }
.wf-node {
  position: relative;
  min-width: 140px;
  padding: 10px 16px;
  background: #fff;
  border: 2px solid var(--el-border-color);
  border-radius: 8px;
  text-align: center;
  cursor: grab;
}
.wf-node:active { cursor: grabbing; }
.wf-node.agent { border-color: #409eff; }
.wf-node.tool { border-color: #909399; }
.wf-node.condition { border-color: #e6a23c; }
.wf-node.approval { border-color: #67c23a; }
.wf-node.start { border-color: #409eff; background: #ecf5ff; }
.wf-node.end { border-color: #f56c6c; background: #fef0f0; }
.wf-node-icon { font-size: 20px; margin-bottom: 4px; }
.wf-node-label { font-size: 13px; font-weight: 600; }
.canvas-tip { font-size: 12px; text-align: center; padding: 8px; }
.pick-list { max-height: 320px; overflow-y: auto; display: flex; flex-direction: column; gap: 6px; }
.pick-item { display: flex; align-items: center; gap: 8px; padding: 10px 12px; border: 1px solid var(--el-border-color-lighter); border-radius: 6px; cursor: pointer; transition: all 0.15s; }
.pick-item:hover { border-color: var(--el-color-primary); background: var(--el-color-primary-light-9); }
</style>
