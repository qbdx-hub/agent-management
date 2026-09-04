/**
 * 一次性脚本：从 PC 端 @iconify-json/tabler 提取 Agent 头像所需图标，
 * 生成 src/components/agentIcons.ts（构建期数据，运行时零依赖）。
 * 用法：node scripts/extract-icons.cjs
 */
const fs = require('node:fs')
const path = require('node:path')

const SRC = 'E:/实习/agent_management/agent_management/frontend/node_modules/@iconify-json/tabler/icons.json'
const OUT = path.join(__dirname, '../src/components/agentIcons.ts')

// 注册表名 → tabler(iconify) 图标名
const WANT = {
  robot: 'robot',
  bulb: 'bulb',
  rocket: 'rocket',
  trophy: 'trophy',
  medal: 'medal',
  coffee: 'coffee',
  moon: 'moon',
  gift: 'gift',
  music: 'music',
  camera: 'camera',
  bell: 'bell',
  world: 'world',
  target: 'target',
  flask: 'flask',
  briefcase: 'briefcase',
  palette: 'palette',
  message: 'message-circle',
  notebook: 'notebook',
  'file-text': 'file-text',
  bookmark: 'bookmark',
  settings: 'settings',
  search: 'search',
  checklist: 'checklist',
  'chart-bar': 'chart-bar',
  send: 'send',
  calendar: 'calendar',
  video: 'video',
  mail: 'mail',
  qrcode: 'qrcode',
  clipboard: 'clipboard-text',
  pencil: 'pencil',
  laptop: 'device-laptop',
  books: 'books',
  plane: 'plane',
  star: 'star',
  heart: 'heart',
  tool: 'tool',
  'chart-line': 'chart-line',
  'circle-check': 'circle-check',
}

const db = JSON.parse(fs.readFileSync(SRC, 'utf8'))
const out = []
for (const [name, tabler] of Object.entries(WANT)) {
  const icon = db.icons[tabler] || (db.aliases && db.aliases[tabler] && db.icons[db.aliases[tabler].parent])
  if (!icon) {
    console.error('MISSING:', name, '->', tabler)
    continue
  }
  out.push(`  '${name}': '${icon.body.replace(/\\/g, '\\\\').replace(/'/g, "\\'")}',`)
}

const ts = `/**
 * Agent 头像 Tabler 图标集（scripts/extract-icons.cjs 从 @iconify-json/tabler 提取，
 * 与 PC 端 unplugin-icons 同源，构建期数据、离线可用、运行时零依赖）。
 * body 为 24x24 viewBox 的 SVG 内部标记，stroke=currentColor。
 */
export const AGENT_ICONS: Record<string, string> = {
${out.join('\n')}
}

/** 兜底图标：未知值渲染为 robot（与 PC 端一致） */
export const FALLBACK_ICON = 'robot'

/** 旧头像/工具 PNG stem → 图标名（数据库存量值兼容，与 PC 端 resolveLegacyIcon 同步） */
const LEGACY_STEMS: Record<string, string> = {
  '01-chat': 'message', '02-rocket': 'rocket', '03-bell': 'bell', '04-notebook': 'notebook',
  '05-document': 'file-text', '06-bookmark': 'bookmark', '07-lightbulb': 'bulb',
  '08-settings': 'settings', '09-search': 'search', '10-checklist': 'checklist',
  '11-growth-chart': 'chart-bar', '12-paper-plane': 'send', '13-send': 'send',
  '14-globe': 'world', '15-todo': 'checklist', '16-tea': 'coffee', '17-calendar-25': 'calendar',
  '18-video': 'video', '19-camera': 'camera', '20-camera-alt': 'camera', '21-moon': 'moon',
  '22-mail': 'mail', '23-ai-robot': 'robot', '24-trophy': 'trophy', '25-medal': 'medal',
  '26-guitar': 'music', '27-gift': 'gift', '28-qrcode': 'qrcode',
}

/** 旧 emoji 值 → 图标名（更早的存量数据兼容） */
const LEGACY_EMOJI: Record<string, string> = {
  '🤖': 'robot', '💡': 'bulb', '🚀': 'rocket', '🏆': 'trophy', '🎖️': 'medal', '🎖': 'medal',
  '🍵': 'coffee', '☕': 'coffee', '🌙': 'moon', '🎁': 'gift', '🎸': 'music', '🎵': 'music',
  '📷': 'camera', '🔔': 'bell', '🌍': 'world', '🎯': 'target', '📋': 'clipboard',
  '📝': 'pencil', '✏️': 'pencil', '⚙️': 'settings', '⚙': 'settings', '💼': 'briefcase',
  '📊': 'chart-bar', '📈': 'chart-line', '🔍': 'search', '💻': 'laptop', '📚': 'books',
  '✈️': 'plane', '🌟': 'star', '⭐': 'star', '❤️': 'heart', '🔧': 'tool', '🧪': 'flask',
  '🎨': 'palette', '📅': 'calendar', '📧': 'mail', '📄': 'file-text', '✅': 'circle-check',
}

/**
 * 存量值解析：优先精确映射（图标名 / PNG stem / emoji），再去数字前缀尝试，
 * 都不中则兜底 robot
 */
export function resolveAgentIcon(value?: string): string {
  if (!value) return FALLBACK_ICON
  if (AGENT_ICONS[value]) return value
  if (LEGACY_STEMS[value]) return LEGACY_STEMS[value]
  if (LEGACY_EMOJI[value]) return LEGACY_EMOJI[value]
  const stripped = value.replace(/^\\d+-/, '')
  if (AGENT_ICONS[stripped]) return stripped
  return FALLBACK_ICON
}

/** 头像底色板：柔和底 + 同族深前景（与 PC 端 V4 语义色一致，明暗主题通用） */
export const AVATAR_HUES: Array<{ bg: string; fg: string }> = [
  { bg: '#efedfd', fg: '#5a54e8' }, // indigo（品牌色）
  { bg: '#e4f5ec', fg: '#178a5b' }, // green
  { bg: '#fbf1de', fg: '#b3730f' }, // amber
  { bg: '#faebee', fg: '#cf3f4f' }, // rose
  { bg: '#e8f0fc', fg: '#3b6fd4' }, // blue
  { bg: '#e3f4f2', fg: '#0f8a80' }, // teal
]

/** 由 key 稳定散列取色，同一 Agent 每次渲染同色 */
export function hueFor(key?: string): { bg: string; fg: string } {
  const s = key || ''
  let h = 0
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) >>> 0
  return AVATAR_HUES[h % AVATAR_HUES.length]
}

/** Agent 头像可选集（AgentCreate 选择器用） */
export const AVATAR_OPTIONS = [
  'robot', 'bulb', 'rocket', 'trophy', 'medal', 'coffee',
  'moon', 'gift', 'music', 'camera', 'bell', 'world',
  'target', 'flask', 'briefcase', 'palette',
]
`

fs.writeFileSync(OUT, ts)
console.log('OK ->', OUT, '| icons:', Object.keys(WANT).length)
