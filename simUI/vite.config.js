import { defineConfig } from 'vite'

export default defineConfig({
  server: {
    port: 5174,
    host: '127.0.0.1',
    proxy: {
      '/api/simulator': {
        target: 'http://127.0.0.1:9080',
        changeOrigin: true,
        secure: false,
      }
    }
  }
})
