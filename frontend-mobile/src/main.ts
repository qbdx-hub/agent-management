import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useThemeStore } from './stores/theme'
import './styles/tokens.css'

const app = createApp(App)
app.use(createPinia())

// 挂载前同步恢复主题，避免暗色用户刷新时闪现明亮主题
useThemeStore().init()

app.use(router)
app.mount('#app')
