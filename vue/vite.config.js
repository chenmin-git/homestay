import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        target: process.env.VITE_API_TARGET || 'http://localhost:8082',
        changeOrigin: true
      },
      '/uploads': {
        target: process.env.VITE_API_TARGET || 'http://localhost:8082',
        changeOrigin: true
      }
    }
  }
})
