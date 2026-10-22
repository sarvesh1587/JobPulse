/// <reference types="vitest" />
import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [react()],
    server: {
      port: 5173,
      // The SPA calls /api on its own origin. In dev, Vite forwards that to
      // Spring Boot, so cookies are first-party and no CORS is needed.
      proxy: {
        '/api': {
          target: env.VITE_DEV_API_PROXY || 'http://localhost:8080',
          changeOrigin: false,
        },
      },
    },
    test: {
      environment: 'node',
    },
  }
})
