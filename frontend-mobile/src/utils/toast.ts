// 轻量 toast（不引入组件库，样式见 tokens.css .toast）
let container: HTMLElement | null = null
let timer: ReturnType<typeof setTimeout> | null = null

function ensureContainer(): HTMLElement {
  if (!container) {
    container = document.createElement('div')
    // 挂进机身（app-shell 有 transform，fixed 相对机身定位），桌面端不会飘到窗口外
    ;(document.querySelector('.app-shell') || document.body).appendChild(container)
  }
  return container
}

export function toast(message: string, type: 'info' | 'error' = 'info', duration = 2000) {
  const el = ensureContainer()
  el.className = 'toast' + (type === 'error' ? ' toast-err' : '')
  el.textContent = message
  el.style.display = 'block'
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => {
    el.style.display = 'none'
  }, duration)
}
