/**
 * 应用图标注册表：Tabler 图标（unplugin-icons 编译期按需引入，离线打包）
 * 模板中统一用全局组件 <UiIcon name="xxx" :size="16" />
 */
import type { Component } from 'vue'

// ===== 界面图标 =====
import iDashboard from '~icons/tabler/layout-dashboard'
import iRobot from '~icons/tabler/robot'
import iPuzzle from '~icons/tabler/puzzle'
import iRoute from '~icons/tabler/route'
import iBooks from '~icons/tabler/books'
import iActivity from '~icons/tabler/activity'
import iMoney from '~icons/tabler/report-money'
import iShieldLock from '~icons/tabler/shield-lock'
import iSettings from '~icons/tabler/settings'
import iArrowLeft from '~icons/tabler/arrow-left'
import iChevronDown from '~icons/tabler/chevron-down'
import iChevronUp from '~icons/tabler/chevron-up'
import iPlus from '~icons/tabler/plus'
import iCheck from '~icons/tabler/check'
import iTrash from '~icons/tabler/trash'
import iList from '~icons/tabler/list'
import iMessage from '~icons/tabler/message-circle'
import iCpu from '~icons/tabler/cpu'
import iTool from '~icons/tabler/tool'
import iSplit from '~icons/tabler/arrows-split'
import iPlay from '~icons/tabler/player-play'
import iPause from '~icons/tabler/player-pause'
import iUser from '~icons/tabler/user'
import iCircleX from '~icons/tabler/circle-x'
import iCircleCheck from '~icons/tabler/circle-check'
import iWand from '~icons/tabler/wand'
import iChartLine from '~icons/tabler/chart-line'
import iCoins from '~icons/tabler/coins'
import iWallet from '~icons/tabler/wallet'
import iAlert from '~icons/tabler/alert-triangle'
import iSend from '~icons/tabler/send'
import iUpload from '~icons/tabler/upload'
import iLoader from '~icons/tabler/loader-2'
import iFileText from '~icons/tabler/file-text'
import iSearch from '~icons/tabler/search'
import iSidebar from '~icons/tabler/layout-sidebar'

// ===== Agent 头像 / 工具图标 =====
import iBulb from '~icons/tabler/bulb'
import iRocket from '~icons/tabler/rocket'
import iTrophy from '~icons/tabler/trophy'
import iMedal from '~icons/tabler/medal'
import iCoffee from '~icons/tabler/coffee'
import iMoon from '~icons/tabler/moon'
import iGift from '~icons/tabler/gift'
import iMusic from '~icons/tabler/music'
import iCamera from '~icons/tabler/camera'
import iBell from '~icons/tabler/bell'
import iWorld from '~icons/tabler/world'
import iTarget from '~icons/tabler/target'
import iClipboard from '~icons/tabler/clipboard-text'
import iBriefcase from '~icons/tabler/briefcase'
import iChartBar from '~icons/tabler/chart-bar'
import iLaptop from '~icons/tabler/device-laptop'
import iStar from '~icons/tabler/star'
import iHeart from '~icons/tabler/heart'
import iFlask from '~icons/tabler/flask'
import iPalette from '~icons/tabler/palette'
import iPlane from '~icons/tabler/plane'
import iPencil from '~icons/tabler/pencil'
import iNotebook from '~icons/tabler/notebook'
import iBookmark from '~icons/tabler/bookmark'
import iChecklist from '~icons/tabler/checklist'
import iCalendar from '~icons/tabler/calendar'
import iVideo from '~icons/tabler/video'
import iMail from '~icons/tabler/mail'
import iQrcode from '~icons/tabler/qrcode'
// 工具市场（V14 内置工具扩容）
import iCalculator from '~icons/tabler/calculator'
import iBinary from '~icons/tabler/binary'
import iDice from '~icons/tabler/dice'
import iClock from '~icons/tabler/clock'
import iRepeat from '~icons/tabler/repeat'
import iRegex from '~icons/tabler/regex'
import iCode from '~icons/tabler/code'
import iHash from '~icons/tabler/hash'
import iTable from '~icons/tabler/table'
import iGitCompare from '~icons/tabler/git-compare'
import iFolderPlus from '~icons/tabler/folder-plus'
import iArrowsLeftRight from '~icons/tabler/arrows-left-right'
import iCopy from '~icons/tabler/copy'
import iPackage from '~icons/tabler/package'
import iApi from '~icons/tabler/api'
import iCloud from '~icons/tabler/cloud'
import iServer from '~icons/tabler/server'
import iLink from '~icons/tabler/link'

export const I: Record<string, Component> = {
  // 界面
  dashboard: iDashboard, robot: iRobot, puzzle: iPuzzle, route: iRoute, books: iBooks,
  activity: iActivity, money: iMoney, 'shield-lock': iShieldLock, settings: iSettings,
  'arrow-left': iArrowLeft, 'chevron-down': iChevronDown, 'chevron-up': iChevronUp,
  plus: iPlus, check: iCheck,
  trash: iTrash, list: iList, message: iMessage, cpu: iCpu, tool: iTool, split: iSplit,
  play: iPlay, pause: iPause, user: iUser, 'circle-x': iCircleX, 'circle-check': iCircleCheck,
  wand: iWand, 'chart-line': iChartLine, coins: iCoins, wallet: iWallet, alert: iAlert,
  send: iSend, upload: iUpload, loader: iLoader, 'file-text': iFileText, search: iSearch,
  sidebar: iSidebar,
  // 头像 / 工具
  bulb: iBulb, rocket: iRocket, trophy: iTrophy, medal: iMedal, coffee: iCoffee,
  moon: iMoon, gift: iGift, music: iMusic, camera: iCamera, bell: iBell, world: iWorld,
  target: iTarget, clipboard: iClipboard, briefcase: iBriefcase, 'chart-bar': iChartBar,
  laptop: iLaptop, star: iStar, heart: iHeart, flask: iFlask, palette: iPalette,
  plane: iPlane, pencil: iPencil, notebook: iNotebook, bookmark: iBookmark,
  checklist: iChecklist, calendar: iCalendar, video: iVideo, mail: iMail, qrcode: iQrcode,
  // 工具市场（V14 内置工具扩容）
  calculator: iCalculator, binary: iBinary, dice: iDice, clock: iClock, repeat: iRepeat,
  regex: iRegex, code: iCode, hash: iHash, table: iTable, 'git-compare': iGitCompare,
  'folder-plus': iFolderPlus, 'arrows-left-right': iArrowsLeftRight, copy: iCopy,
  package: iPackage, api: iApi, cloud: iCloud, server: iServer, link: iLink,
}

/** 兜底图标：未知值渲染为 robot */
export const FALLBACK_ICON = 'robot'

/** 旧头像/工具 PNG stem → Tabler 图标（数据库存量值兼容） */
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

/** 旧 emoji 值 → Tabler 图标（更早的存量数据兼容） */
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
 * 存量值解析：优先精确映射（PNG stem / emoji），再去数字前缀尝试，
 * 都不中则兜底 robot
 */
export function resolveLegacyIcon(value?: string): string {
  if (!value) return FALLBACK_ICON
  if (I[value]) return value // 已是新方案图标名
  if (LEGACY_STEMS[value]) return LEGACY_STEMS[value]
  if (LEGACY_EMOJI[value]) return LEGACY_EMOJI[value]
  const stripped = value.replace(/^\d+-/, '')
  if (I[stripped]) return stripped
  return FALLBACK_ICON
}

/** 头像底色板：柔和底 + 同族深前景，与 V4 语义色一致 */
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
