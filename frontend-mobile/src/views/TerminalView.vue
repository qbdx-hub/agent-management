<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { execTerminal, getTerminalInfo } from '@/api/terminal'
import AppIcon from '@/components/AppIcon.vue'

/**
 * 移动端命令终端：命令在服务器的工作空间沙箱目录（agent.sandbox.root/ws-{id}/）内执行。
 * cwd 状态由前端持有、每次请求回传（服务端无状态）；owner/admin 直通，
 * member 受空间「成员终端开关」（PC 端空间设置）控制，执行全量审计。
 * 终端固定深色（GitHub Dark 色板），不随应用主题切换。
 */

type LineKind = 'input' | 'output' | 'sys' | 'err'
interface TermLine {
  id: number
  kind: LineKind
  text: string
}

const router = useRouter()

const lines = ref<TermLine[]>([])
const cwd = ref('') // 相对沙箱根，'' 即根目录
const input = ref('')
const running = ref(false)
const os = ref('')
const role = ref('')
const sandboxPath = ref('')
const memberTerminalEnabled = ref(true)
const showBanner = ref(!sessionStorage.getItem('term_banner'))
const scrollEl = ref<HTMLElement>()

// 命令历史：localStorage 持久化（上限 50），↑↓键与头部按钮共用游标
const HIST_KEY = 'term_hist'
const hist = ref<string[]>(JSON.parse(localStorage.getItem(HIST_KEY) || '[]'))
const histIdx = ref(-1)

let seq = 0

const isWin = computed(() => /win/i.test(os.value))
const chips = computed(() =>
  isWin.value
    ? ['dir', 'cd', 'type README.md', 'ipconfig', 'java -version', 'echo %PATH%']
    : ['ls -la', 'pwd', 'cat /etc/os-release', 'ifconfig', 'java -version', 'df -h'],
)
const shortCwd = computed(() => (cwd.value ? `~/${cwd.value}` : `~/${sandboxPath.value || 'sandbox'}`))

/** 横幅里的可用性说明：随角色与空间「成员终端开关」变化 */
const accessNote = computed(() => {
  if (role.value === 'owner') return '；你以空间所有者身份使用'
  if (role.value === 'admin') return '；你以空间管理员身份使用'
  if (role.value === 'member') {
    return memberTerminalEnabled.value ? '；本空间已对成员开放终端' : '；空间管理员未开放成员终端'
  }
  return ''
})

function push(kind: LineKind, text: string) {
  lines.value.push({ id: ++seq, kind, text })
}

function persistHist(cmd: string) {
  const next = hist.value.filter((h) => h !== cmd)
  next.push(cmd)
  hist.value = next.slice(-50)
  localStorage.setItem(HIST_KEY, JSON.stringify(hist.value))
  histIdx.value = -1
}

function cycleHist(dir: 1 | -1) {
  if (!hist.value.length) return
  if (histIdx.value === -1 && dir === -1) {
    histIdx.value = hist.value.length - 1
  } else {
    histIdx.value += dir
  }
  if (histIdx.value < -1) histIdx.value = -1
  if (histIdx.value >= hist.value.length) histIdx.value = -1
  input.value = histIdx.value === -1 ? '' : hist.value[histIdx.value]
}

async function run(raw?: string) {
  const cmd = (raw ?? input.value).trim()
  if (!cmd || running.value) return
  push('input', cmd)
  input.value = ''
  persistHist(cmd)
  running.value = true
  try {
    const res = await execTerminal(cmd, cwd.value)
    if (res.code !== 0) {
      push('err', res.message || '执行失败')
    } else if (res.data) {
      const d = res.data
      cwd.value = d.cwd
      for (const l of d.output.split(/\r?\n/)) push('output', l)
      if (d.timedOut) push('err', '命令超过 30s 已被强制终止（以上为已捕获输出）')
      const notes: string[] = []
      if (d.exitCode !== 0 && !d.timedOut) notes.push(`exit ${d.exitCode}`)
      if (d.truncated) notes.push('输出过长已截断')
      notes.push(`${d.durationMs}ms`)
      push('sys', `↳ ${notes.join(' · ')}`)
    }
  } catch (e) {
    // 拦截器已 toast 业务错误，这里只把详情落进终端流
    push('err', e instanceof Error ? e.message : '网络错误')
  } finally {
    running.value = false
  }
}

function clearScreen() {
  lines.value = []
}

function dismissBanner() {
  showBanner.value = false
  sessionStorage.setItem('term_banner', '1')
}

watch(
  () => lines.value.length,
  () => nextTick(() => {
    if (scrollEl.value) scrollEl.value.scrollTop = scrollEl.value.scrollHeight
  }),
)

onMounted(async () => {
  try {
    const res = await getTerminalInfo()
    if (res.code === 0 && res.data) {
      os.value = res.data.os
      role.value = res.data.role
      sandboxPath.value = res.data.sandboxPath
      memberTerminalEnabled.value = res.data.memberTerminalEnabled !== false
      push('sys', `已连接 ${res.data.sandboxPath} · ${res.data.os} · ${res.data.role}`)
    }
  } catch {
    push('err', '终端连接失败，请稍后重试')
  }
})
</script>

<template>
  <div class="page term-page">
    <div class="navbar term-navbar">
      <div class="back-btn" @click="router.back()"><AppIcon name="back" :size="16" /></div>
      <span class="title">终端<span class="cwd">{{ shortCwd }}</span></span>
      <div class="icon-btn" title="上一条命令" @click="cycleHist(-1)"><AppIcon name="arrow" :size="14" /></div>
      <div class="icon-btn" title="清屏" @click="clearScreen"><AppIcon name="trash" :size="15" /></div>
    </div>

    <div class="scroll term-scroll" ref="scrollEl">
      <div v-if="showBanner" class="term-banner">
        <div class="b-title">服务器沙箱终端</div>
        <div class="b-body">
          命令在服务器 <b>~/{{ sandboxPath }}</b> 目录内执行，30s 超时，全程审计{{ accessNote }}。
        </div>
        <div class="b-close" @click="dismissBanner">知道了</div>
      </div>

      <div v-for="l in lines" :key="l.id" :class="['term-line', l.kind]">
        <span v-if="l.kind === 'input'" class="prompt">$&nbsp;</span>{{ l.text }}
      </div>
      <div v-if="running" class="term-line sys">… 执行中 …</div>
    </div>

    <div class="term-chips">
      <button v-for="c in chips" :key="c" :disabled="running" @click="run(c)">{{ c }}</button>
    </div>

    <div class="term-inputbar">
      <span class="prompt">$</span>
      <input
        v-model="input"
        :disabled="running"
        spellcheck="false"
        autocapitalize="off"
        autocomplete="off"
        placeholder="输入命令…"
        @keydown.enter="run()"
        @keydown.up.prevent="cycleHist(-1)"
        @keydown.down.prevent="cycleHist(1)"
      />
      <button class="send-btn" :disabled="running || !input.trim()" @click="run()">
        <AppIcon name="send" :size="15" />
      </button>
    </div>
  </div>
</template>

<style scoped>
/* 固定深色终端风，不使用主题变量（浅色主题下仍是深色）；唯一例外 --safe-bottom 安全区 */
.term-page {
  background: #0d1117;
}
.term-navbar {
  background: #0d1117;
  border-bottom: 1px solid #21262d;
}
.term-navbar .back-btn,
.term-navbar .icon-btn {
  background: #161b22;
  border: 1px solid #2d333b;
  color: #c9d1d9;
}
.term-navbar .title {
  color: #e6edf3;
}
.cwd {
  font-family: ui-monospace, SFMono-Regular, Consolas, 'Courier New', monospace;
  font-size: 12px;
  color: #8b949e;
  margin-left: 10px;
}
.icon-btn {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.term-scroll {
  font-family: ui-monospace, SFMono-Regular, Consolas, 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.7;
  padding: 12px 16px 16px;
  white-space: pre-wrap;
  word-break: break-all;
}
.term-line {
  color: #c9d1d9;
  min-height: 1.7em;
}
.term-line.input {
  color: #e6edf3;
}
.term-line .prompt,
.term-line.input .prompt {
  color: #7ee787;
}
.term-line.sys {
  color: #8b949e;
  font-style: italic;
}
.term-line.err {
  color: #f85149;
}

.term-banner {
  background: #161b22;
  border: 1px solid #2d333b;
  border-radius: 10px;
  padding: 10px 12px;
  margin-bottom: 12px;
}
.b-title {
  color: #e6edf3;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 4px;
}
.b-body {
  color: #8b949e;
  font-size: 11px;
  line-height: 1.6;
}
.b-body b {
  color: #7ee787;
  font-weight: 500;
}
.b-close {
  color: #7ee787;
  font-size: 11px;
  margin-top: 6px;
}

.term-chips {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  white-space: nowrap;
  padding: 8px 12px 0;
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.term-chips::-webkit-scrollbar {
  display: none;
}
.term-chips button {
  flex: 0 0 auto;
  background: #161b22;
  border: 1px solid #2d333b;
  color: #7ee787;
  border-radius: 999px;
  padding: 5px 12px;
  font-family: ui-monospace, SFMono-Regular, Consolas, 'Courier New', monospace;
  font-size: 11px;
}
.term-chips button:disabled {
  opacity: 0.4;
}

.term-inputbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 8px 12px calc(var(--safe-bottom) + 12px);
  height: 44px;
  padding: 0 6px 0 14px;
  border-radius: 22px;
  background: #161b22;
  border: 1px solid #2d333b;
}
.term-inputbar .prompt {
  color: #7ee787;
  font-family: ui-monospace, SFMono-Regular, Consolas, 'Courier New', monospace;
  font-size: 13px;
}
.term-inputbar input {
  flex: 1;
  min-width: 0;
  height: 100%;
  background: none;
  border: none;
  outline: none;
  color: #e6edf3;
  font-family: ui-monospace, SFMono-Regular, Consolas, 'Courier New', monospace;
  font-size: 13px;
  caret-color: #7ee787;
}
.term-inputbar input::placeholder {
  color: #484f58;
}
.term-inputbar input:disabled {
  opacity: 0.5;
}
.send-btn {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #238636;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}
.send-btn:disabled {
  opacity: 0.4;
}
</style>
