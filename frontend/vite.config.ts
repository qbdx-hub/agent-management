import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Icons from 'unplugin-icons/vite'
import { resolve } from 'node:path'

export default defineConfig({
  plugins: [
    vue(),
    // 图标编译期按需引入（Tabler 集合，离线打包，不依赖运行时 CDN）
    Icons({ compiler: 'vue3' }),
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
