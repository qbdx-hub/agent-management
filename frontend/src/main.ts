import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/tokens.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import AgentAvatar from '@/components/AgentAvatar.vue'
import UiIcon from '@/components/UiIcon.vue'
import App from './App.vue'
import router from './router'
import './style.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// 全局注册：Agent 头像（Tabler 图标 + 色板，兼容旧 PNG stem / emoji 数据）与通用图标
app.component('AgentAvatar', AgentAvatar)
app.component('UiIcon', UiIcon)

app.mount('#app')
