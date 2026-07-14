<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const workflowId = Number(route.params.id)

// 节点数据
const nodes = ref([
  { id: 'n1', type: 'agent', label: '需求分析', agentName: '代码审查助手', x: 100, y: 100 },
  { id: 'n2', type: 'agent', label: '代码生成', agentName: '文档生成器', x: 100, y: 250 },
  { id: 'n3', type: 'agent', label: '代码审查', agentName: 'PR 审查机器人', x: 100, y: 400 },
  { id: 'n4', type: 'condition', label: '是否有问题?', x: 350, y: 250 },
  { id: 'n5', type: 'approval', label: '人工确认', x: 550, y: 400 },
])

const edges = ref([
  { from: 'n1', to: 'n2' },
  { from: 'n2', to: 'n3' },
  { from: 'n3', to: 'n4' },
  { from: 'n4', to: 'n5' },
])

const selectedNode = ref<any>(null)

function selectNode(node: any) { selectedNode.value = node }
function deselectNode() { selectedNode.value = null }

function handleSave() { ElMessage.success('工作流已保存') }
function handleRun() { ElMessage.success('工作流已启动') }
</script>

<template>
  <div class="workflow-editor-page">
    <div class="page-header">
      <div style="display:flex;align-items:center;gap:8px">
        <el-button text @click="router.push('/orchestration')"><el-icon><ArrowLeft /></el-icon></el-button>
        <h2>编排画布</h2>
      </div>
      <div style="display:flex;gap:8px">
        <el-button @click="handleSave"><el-icon><Check /></el-icon> 保存</el-button>
        <el-button type="primary" @click="handleRun"><el-icon><VideoPlay /></el-icon> 运行</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <!-- 画布 -->
      <el-col :span="16">
        <el-card class="canvas-card">
          <div class="canvas-area">
            <svg class="canvas-svg" width="100%" height="500">
              <!-- 连线 -->
              <line v-for="(edge, idx) in edges" :key="idx"
                :x1="nodes.find(n => n.id === edge.from)?.x! + 80"
                :y1="nodes.find(n => n.id === edge.from)?.y! + 25"
                :x2="nodes.find(n => n.id === edge.to)?.x! + 80"
                :y2="nodes.find(n => n.id === edge.to)?.y! + 25"
                stroke="#c0c4cc" stroke-width="2" marker-end="url(#arrow)"
              />
              <defs>
                <marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="6" markerHeight="6" orient="auto">
                  <path d="M 0 0 L 10 5 L 0 10 z" fill="#c0c4cc" />
                </marker>
              </defs>
            </svg>

            <!-- 节点 -->
            <div
              v-for="node in nodes" :key="node.id"
              class="canvas-node"
              :class="[node.type, { selected: selectedNode?.id === node.id }]"
              :style="{ left: node.x + 'px', top: node.y + 'px' }"
              @click="selectNode(node)"
            >
              <div class="node-icon">
                <span v-if="node.type === 'agent'">🤖</span>
                <span v-else-if="node.type === 'condition'">🔀</span>
                <span v-else>👤</span>
              </div>
              <div class="node-label">{{ node.label }}</div>
              <div v-if="node.agentName" class="node-agent text-muted">{{ node.agentName }}</div>
            </div>
          </div>
          <div class="canvas-tip text-muted">点击节点查看详情，拖拽可移动位置（完整拖拽功能需集成 Vue Flow）</div>
        </el-card>
      </el-col>

      <!-- 属性面板 -->
      <el-col :span="8">
        <el-card v-if="selectedNode">
          <template #header><span>节点属性</span></template>
          <el-form label-width="80px">
            <el-form-item label="类型"><el-tag>{{ selectedNode.type === 'agent' ? 'Agent' : selectedNode.type === 'condition' ? '条件' : '审批' }}</el-tag></el-form-item>
            <el-form-item label="名称"><el-input v-model="selectedNode.label" /></el-form-item>
            <el-form-item v-if="selectedNode.agentName" label="Agent"><el-input v-model="selectedNode.agentName" /></el-form-item>
          </el-form>
        </el-card>
        <el-card v-else>
          <el-empty description="点击节点查看属性" :image-size="60" />
        </el-card>

        <el-card style="margin-top:16px">
          <template #header><span>添加节点</span></template>
          <div style="display:flex;flex-direction:column;gap:8px">
            <el-button @click="nodes.push({ id: 'n' + Date.now(), type: 'agent', label: '新 Agent 节点', agentName: '', x: 200, y: 200 + nodes.length * 60 })"><el-icon><Plus /></el-icon> Agent 节点</el-button>
            <el-button @click="nodes.push({ id: 'n' + Date.now(), type: 'condition', label: '条件判断', x: 200, y: 200 + nodes.length * 60 })"><el-icon><Plus /></el-icon> 条件节点</el-button>
            <el-button @click="nodes.push({ id: 'n' + Date.now(), type: 'approval', label: '人工审批', x: 200, y: 200 + nodes.length * 60 })"><el-icon><Plus /></el-icon> 审批节点</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.workflow-editor-page { max-width: 1400px; }
.canvas-card { position: relative; }
.canvas-area { position: relative; height: 500px; background: #fafafa; border: 1px solid var(--el-border-color-lighter); border-radius: 6px; overflow: hidden; }
.canvas-svg { position: absolute; top: 0; left: 0; pointer-events: none; }
.canvas-node { position: absolute; width: 160px; padding: 12px; background: #fff; border: 2px solid var(--el-border-color); border-radius: 8px; cursor: pointer; text-align: center; transition: all 0.2s; z-index: 1; }
.canvas-node:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.canvas-node.selected { border-color: var(--el-color-primary); box-shadow: 0 0 0 3px var(--el-color-primary-light-8); }
.canvas-node.agent { border-color: #409eff; }
.canvas-node.condition { border-color: #e6a23c; }
.canvas-node.approval { border-color: #67c23a; }
.node-icon { font-size: 20px; margin-bottom: 4px; }
.node-label { font-size: 13px; font-weight: 600; }
.node-agent { font-size: 11px; }
.canvas-tip { font-size: 12px; text-align: center; padding: 8px; }
</style>
