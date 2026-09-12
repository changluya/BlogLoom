import { defineConfig } from 'vite'
import { createVuePlugin } from 'vite-plugin-vue2'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [createVuePlugin()],
  base: '/',
  resolve: {
    dedupe: ['vue'],
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
    extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue']
  },
  server: {
    host: '127.0.0.1',
    port: Number(process.env.npm_config_port || process.env.PORT || 8079),
    open: true
  },
  build: {
    outDir: 'dist',
    assetsDir: 'static',
    sourcemap: false
  }
})
