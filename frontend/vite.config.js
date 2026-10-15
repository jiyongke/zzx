import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 前端开发服务器配置：代理 /api 到 Spring Boot 后端
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
