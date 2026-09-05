import type { CapacitorConfig } from '@capacitor/cli'

const config: CapacitorConfig = {
  appId: 'com.lingshu.agent',
  appName: '灵枢agent',
  webDir: 'dist',
  android: {
    // WebView 源是 https://localhost，允许加载 http://47.94.144.140:8080 的图片等混合内容
    allowMixedContent: true,
  },
  server: {
    androidScheme: 'https',
  },
}

export default config
