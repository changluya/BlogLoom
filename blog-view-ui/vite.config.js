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
  css: {
    preprocessorOptions: {
      scss: {
        // Vite 4 仍走 Sass legacy API，Dart Sass 1.8x+ 会打印 legacy-js-api 弃用告警。
        // 这里显式静默，待升级到 Vite 5.4+ 后改用 api: 'modern-compiler'。
        silenceDeprecations: ['legacy-js-api']
      }
    }
  },
  build: {
    outDir: 'dist',
    // 后端同源部署时 /static/** 已被后台上传资源占用，允许通过环境变量切换产物目录。
    assetsDir: process.env.VITE_ASSETS_DIR || 'static',
    sourcemap: false
  }
})
