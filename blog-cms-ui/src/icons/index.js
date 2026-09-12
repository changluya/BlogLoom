import Vue from 'vue'
import SvgIcon from '@/components/SvgIcon'

// Register the shared icon component.
Vue.component('svg-icon', SvgIcon)

// Vite replaces Webpack's require.context with import.meta.glob.
const iconModules = import.meta.glob('./svg/*.svg', {
  eager: true,
  query: '?raw',
  import: 'default'
})

const symbols = Object.entries(iconModules).map(([file, source]) => {
  const iconName = file.split('/').pop().replace(/\.svg$/, '')
  const viewBox = source.match(/viewBox="([^"]+)"/i)
  const content = source
    .replace(/^[\s\S]*?<svg[^>]*>/i, '')
    .replace(/<\/svg>\s*$/i, '')

  return `<symbol id="icon-${iconName}"${viewBox ? ` viewBox="${viewBox[1]}"` : ''}>${content}</symbol>`
}).join('')

const sprite = document.createElement('svg')
sprite.setAttribute('xmlns', 'http://www.w3.org/2000/svg')
sprite.setAttribute('aria-hidden', 'true')
sprite.style.position = 'absolute'
sprite.style.width = '0'
sprite.style.height = '0'
sprite.style.overflow = 'hidden'
sprite.innerHTML = symbols
document.body.insertBefore(sprite, document.body.firstChild)
