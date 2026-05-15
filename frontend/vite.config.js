import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        timeout: 120000,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.setTimeout(120000)
          })
        },
      },
    },
  },
  optimizeDeps: {
    include: [
      '@deck.gl/core',
      '@deck.gl/aggregation-layers',
      '@deck.gl/layers',
      '@deck.gl/mapbox',
    ],
  },
  build: {
    commonjsOptions: {
      include: [/node_modules/],
    },
  },
  define: {
    'process.env': '{}',
  },
})
