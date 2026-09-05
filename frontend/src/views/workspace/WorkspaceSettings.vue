<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getWorkspaceSettings, updateWorkspaceSettings } from '@/api/workspace'
import { useWorkspaceStore } from '@/stores/workspace'
import { ElMessage } from 'element-plus'
import type { WorkspaceSettings } from '@/types/workspace'

const router = useRouter()
const wsStore = useWorkspaceStore()

const settings = reactive<WorkspaceSettings>({
  sharedWorkdir: false,
  allowOutsideSandbox: false,
  disabledTools: [],
  memberTerminalEnabled: true,
})
const form = reactive({ name: '', description: '' })
const loading = ref(false)
const saving = ref(false)

/** 内置工具展示信息（key 与后端工具名一致，顺序按类别分组） */
const BUILTIN_TOOLS = [
  // 文件与命令
  { key: 'read_file', icon: 'file-text', label: '读取文件', desc: '查看文本文件内容' },
  { key: 'write_file', icon: 'pencil', label: '写入文件', desc: '创建/覆盖文件' },
  { key: 'edit_file', icon: 'wand', label: '编辑文件', desc: '精确替换文件片段' },
  { key: 'list_files', icon: 'list', label: '列举文件', desc: '按模式列目录' },
  { key: 'search_files', icon: 'search', label: '搜索文件', desc: '按内容 Grep 检索' },
  { key: 'run_command', icon: 'cpu', label: '执行命令', desc: '运行 shell 命令' },
  { key: 'create_dir', icon: 'folder-plus', label: '创建目录', desc: '递归建目录' },
  { key: 'delete_path', icon: 'trash', label: '删除路径', desc: '删文件/目录（拒删根）' },
  { key: 'move_path', icon: 'arrows-left-right', label: '移动/重命名', desc: '移动或改名' },
  { key: 'copy_path', icon: 'copy', label: '复制路径', desc: '递归复制文件' },
  { key: 'file_info', icon: 'clipboard', label: '文件信息', desc: '大小/行数/图片尺寸' },
  { key: 'zip_pack', icon: 'package', label: '打包 zip', desc: '目录打包压缩' },
  { key: 'zip_unpack', icon: 'package', label: '解压 zip', desc: '防 zip-slip 解压' },
  { key: 'qr_generate', icon: 'qrcode', label: '二维码生成', desc: '文本/链接转二维码 SVG' },
  // 联网
  { key: 'web_search', icon: 'world', label: '网页搜索', desc: '必应联网搜索' },
  { key: 'web_fetch', icon: 'send', label: '网页抓取', desc: '读取 URL 内容' },
  { key: 'http_request', icon: 'api', label: 'HTTP 请求', desc: '通用 REST 调用' },
  { key: 'weather_forecast', icon: 'cloud', label: '天气预报', desc: '全球城市未来 7 天' },
  { key: 'ip_lookup', icon: 'world', label: 'IP 归属地', desc: 'IP 定位与运营商' },
  { key: 'dns_lookup', icon: 'server', label: '域名解析', desc: '查域名全部 IP' },
  { key: 'url_metadata', icon: 'link', label: '网页元信息', desc: 'title/description/og' },
  { key: 'pdf_extract_text', icon: 'file-text', label: 'PDF 提取', desc: '按页提取 PDF 文本' },
  { key: 'webhook_notify', icon: 'bell', label: 'Webhook 推送', desc: '企微/钉钉/飞书机器人' },
  // 计算与转换
  { key: 'calculator', icon: 'calculator', label: '计算器', desc: '数学表达式求值' },
  { key: 'unit_convert', icon: 'split', label: '单位换算', desc: '长度/重量/温度等' },
  { key: 'number_base_convert', icon: 'binary', label: '进制转换', desc: '2-36 进制互转' },
  { key: 'random_generator', icon: 'dice', label: '随机生成', desc: '随机数/UUID/密码' },
  { key: 'loan_calc', icon: 'coins', label: '房贷计算', desc: '等额本息/本金' },
  { key: 'color_convert', icon: 'palette', label: '颜色转换', desc: 'HEX/RGB/HSL 互转' },
  { key: 'current_time', icon: 'clock', label: '当前时间', desc: '指定时区现时刻' },
  { key: 'date_calculator', icon: 'calendar', label: '日期计算', desc: '加减天数/工作日' },
  { key: 'timestamp_convert', icon: 'clock', label: '时间戳转换', desc: '秒/毫秒互转' },
  { key: 'cron_next', icon: 'repeat', label: 'Cron 解析', desc: '未来执行时间点' },
  { key: 'text_stats', icon: 'chart-bar', label: '字数统计', desc: '字符/单词/字节数' },
  { key: 'text_transform', icon: 'wand', label: '文本变换', desc: '大小写/去重/排序' },
  { key: 'regex_tool', icon: 'regex', label: '正则工具', desc: '匹配/校验/替换' },
  { key: 'base64_codec', icon: 'code', label: 'Base64', desc: '编解码（URL-safe）' },
  { key: 'hash_calculator', icon: 'hash', label: '哈希计算', desc: 'MD5/SHA 系列摘要' },
  { key: 'url_codec', icon: 'world', label: 'URL 编解码', desc: '百分号编解码' },
  { key: 'json_tool', icon: 'code', label: 'JSON 工具', desc: '校验/格式化/取值' },
  { key: 'csv_json_convert', icon: 'table', label: 'CSV↔JSON', desc: '表格与 JSON 互转' },
  { key: 'text_diff', icon: 'git-compare', label: '文本对比', desc: '行级差异对比' },
]

function isDisabled(key: string) {
  return settings.disabledTools.includes(key)
}

function toggleTool(key: string) {
  if (isDisabled(key)) {
    settings.disabledTools = settings.disabledTools.filter(t => t !== key)
  } else {
    settings.disabledTools = [...settings.disabledTools, key]
  }
}

/** 当前生效的沙箱根目录展示（与后端 agent.sandbox.root 布局一致） */
const sandboxRootLabel = computed(() =>
  `data/agent-workspaces/ws-${wsStore.currentId ?? '?'}${settings.sharedWorkdir ? '' : '/session-{会话ID}'}`)

async function load() {
  loading.value = true
  try {
    const res = await getWorkspaceSettings()
    if (res.code === 0 && res.data) {
      Object.assign(settings, {
        sharedWorkdir: !!res.data.sharedWorkdir,
        allowOutsideSandbox: !!res.data.allowOutsideSandbox,
        disabledTools: res.data.disabledTools || [],
        memberTerminalEnabled: res.data.memberTerminalEnabled !== false,
      })
    }
    form.name = wsStore.current?.name || ''
    form.description = wsStore.current?.description || ''
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!form.name.trim()) {
    ElMessage.warning('空间名称不能为空')
    return
  }
  saving.value = true
  try {
    const res = await updateWorkspaceSettings({
      name: form.name.trim(),
      description: form.description,
      sharedWorkdir: settings.sharedWorkdir,
      allowOutsideSandbox: settings.allowOutsideSandbox,
      disabledTools: settings.disabledTools,
      memberTerminalEnabled: settings.memberTerminalEnabled,
    })
    if (res.code === 0) {
      ElMessage.success('设置已保存')
      // 名称/描述可能已变更，刷新空间列表缓存
      wsStore.fetchMyWorkspaces().catch(() => {})
    }
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="workspace-settings-page">
    <div class="page-header">
      <h2>空间设置</h2>
      <div style="display:flex;gap:8px">
        <el-button @click="router.push('/workspace/members')">成员管理</el-button>
        <el-button @click="router.push('/workspace/activity')">空间动态</el-button>
      </div>
    </div>

    <div v-loading="loading">
      <!-- 基本信息 -->
      <el-card shadow="never" class="mb-24">
        <template #header><span>基本信息</span></template>
        <el-form label-width="140px" style="max-width:600px">
          <el-form-item label="空间名称"><el-input v-model="form.name" placeholder="空间名称" /></el-form-item>
          <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" placeholder="空间描述" /></el-form-item>
        </el-form>
      </el-card>

      <!-- 成员权限 -->
      <el-card shadow="never" class="mb-24">
        <template #header><span>成员权限</span></template>
        <el-form label-width="140px" style="max-width:820px">
          <el-form-item label="允许成员用终端">
            <el-switch v-model="settings.memberTerminalEnabled" />
            <span class="switch-label">{{ settings.memberTerminalEnabled ? '已开放' : '仅管理员' }}</span>
            <div class="field-hint" :class="{ 'hint-danger': !settings.memberTerminalEnabled }">
              开启后空间成员可在移动端「终端」执行命令（沙箱内、30s 超时、全程审计）；关闭则仅空间所有者/管理员可用
            </div>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- Agent 执行环境 -->
      <el-card shadow="never">
        <template #header><span>Agent 执行环境</span></template>
        <el-form label-width="140px" style="max-width:820px">
          <el-form-item label="沙箱根目录">
            <div class="sandbox-root">
              <UiIcon name="shield-lock" :size="14" />
              <code>{{ sandboxRootLabel }}</code>
            </div>
            <div class="field-hint">本空间 Agent 的文件操作被限制在该目录内，其他空间不可见</div>
          </el-form-item>

          <el-form-item label="共享工作目录">
            <el-switch v-model="settings.sharedWorkdir" />
            <span class="switch-label">{{ settings.sharedWorkdir ? '已开启' : '已关闭' }}</span>
            <div class="field-hint">
              开启后本空间所有会话共用同一文件区，Agent 产物跨会话可见（类似项目目录）；关闭则每个会话独立沙箱、互不干扰
            </div>
          </el-form-item>

          <el-form-item label="允许沙箱外运行">
            <el-switch v-model="settings.allowOutsideSandbox" class="danger-switch" />
            <span class="switch-label">{{ settings.allowOutsideSandbox ? '允许' : '禁止' }}</span>
            <div class="field-hint" :class="{ 'hint-danger': settings.allowOutsideSandbox }">
              总闸开关：开启后成员才能在会话中使用「沙箱外运行」，Agent 将直接操作服务器真实文件系统，请谨慎放开
            </div>
          </el-form-item>

          <el-form-item label="内置工具">
            <div class="tool-grid">
              <div
                v-for="t in BUILTIN_TOOLS"
                :key="t.key"
                class="tool-card"
                :class="{ disabled: isDisabled(t.key) }"
                @click="toggleTool(t.key)"
              >
                <div class="tool-icon"><UiIcon :name="t.icon" :size="18" /></div>
                <div class="tool-info">
                  <div class="tool-label">{{ t.label }}</div>
                  <div class="tool-desc">{{ t.desc }}</div>
                </div>
                <el-tag :type="isDisabled(t.key) ? 'danger' : 'success'" size="small" effect="light">
                  {{ isDisabled(t.key) ? '已禁用' : '允许' }}
                </el-tag>
              </div>
            </div>
            <div class="field-hint">点击切换：禁用后本空间内所有 Agent 调用该工具都会被拒绝</div>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="saving" @click="handleSave">保存设置</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.workspace-settings-page { max-width: 900px; margin: 0 auto; }
.sandbox-root {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 6px 12px; background: rgba(22, 22, 29, 0.04);
  border-radius: 8px; color: var(--text-2);
}
.sandbox-root code { font-size: 13px; color: var(--text-1); }
.switch-label { margin-left: 10px; font-size: 13px; color: var(--text-2); }
.field-hint { font-size: 12px; color: var(--text-3); line-height: 1.6; margin-top: 4px; width: 100%; }
.field-hint.hint-danger { color: var(--st-danger, #cf3f4f); }
/* 内置工具开关网格 */
.tool-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; width: 100%; }
@media (max-width: 820px) { .tool-grid { grid-template-columns: 1fr; } }
.tool-card {
  display: flex; align-items: center; gap: 10px; padding: 10px 12px;
  border: 1px solid var(--border-1, #e8e9ee); border-radius: 10px;
  cursor: pointer; transition: border-color 0.15s, background 0.15s;
}
.tool-card:hover { border-color: var(--accent, #5a54e8); }
.tool-card.disabled { background: rgba(207, 63, 79, 0.04); }
.tool-icon {
  width: 34px; height: 34px; border-radius: 8px; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  background: var(--accent-soft, #efedfd); color: var(--accent, #5a54e8);
}
.tool-card.disabled .tool-icon { background: rgba(207, 63, 79, 0.08); color: var(--st-danger, #cf3f4f); }
.tool-info { flex: 1; min-width: 0; }
.tool-label { font-size: 13px; font-weight: 600; color: var(--text-1); }
.tool-desc { font-size: 12px; color: var(--text-3); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
