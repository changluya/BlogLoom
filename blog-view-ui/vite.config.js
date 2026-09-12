import { defineConfig } from 'vite'
import { createVuePlugin } from 'vite-plugin-vue2'
import { fileURLToPath, URL } from 'node:url'

const sourceRoot = fileURLToPath(new URL('./src', import.meta.url))
const sourcePath = (directory) => fileURLToPath(new URL(`./src/${directory}`, import.meta.url))

export default defineConfig({
  plugins: [createVuePlugin()],
  base: '/',
  resolve: {
    dedupe: ['vue'],
    alias: {
      '@': sourceRoot,
      assets: sourcePath('assets'),
      common: sourcePath('common'),
      components: sourcePath('components'),
      api: sourcePath('api'),
      views: sourcePath('views'),
      plugins: sourcePath('plugins')
    },
    extensions: ['.mjs', '.js', '.ts', '.jsx', '.tsx', '.json', '.vue']
  },
  server: {
    host: '127.0.0.1',
    port: Number(process.env.npm_config_port || process.env.PORT || 8080)
  },
  build: {
    outDir: 'dist',
    assetsDir: 'static',
    sourcemap: false
  }
})
