import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  // 添加以下配置解决 flv.js 的 worker 问题
  optimizeDeps: {
    include: ['flv.js']
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  },
  // 解决 worker 加载问题
  worker: {
    format: 'es'
  }
})
