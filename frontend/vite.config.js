import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'
import { createHash } from 'node:crypto'
import { readFileSync, writeFileSync, existsSync } from 'node:fs'
import { resolve } from 'node:path'

/**
 * SRI(Subresource Integrity) — 빌드된 index.html 의 /assets/ 스크립트·스타일·modulepreload 에
 * integrity="sha384-..." 를 붙인다. 전송 구간(프록시·캐시)에서 자산이 바뀌면 브라우저가 실행을 거부한다.
 * 출력 파일이 다 써진 뒤(closeBundle) 실제 디스크 내용으로 해시를 계산해, 이후 플러그인이 코드를 고쳐도 어긋나지 않는다.
 * 사용자가 자기 브라우저에서 고치는 것은 막지 못한다(서버 권한 검사가 담당).
 */
function subresourceIntegrity() {
  let outDir
  return {
    name: 'ksp-sri',
    apply: 'build',
    configResolved(config) {
      outDir = resolve(config.root, config.build.outDir)
    },
    closeBundle() {
      const htmlPath = resolve(outDir, 'index.html')
      if (!existsSync(htmlPath)) return
      let count = 0
      const html = readFileSync(htmlPath, 'utf8').replace(
        /<(script|link)\b([^>]*?)\s(src|href)="(\/assets\/[^"?#]+)"([^>]*)>/g,
        (tag, name, pre, attr, url, post) => {
          if (/\sintegrity=/.test(tag)) return tag
          const file = resolve(outDir, url.slice(1))
          if (!existsSync(file)) return tag
          const hash = createHash('sha384').update(readFileSync(file)).digest('base64')
          const cross = /\scrossorigin\b/.test(tag) ? '' : ' crossorigin'
          count++
          return `<${name}${pre} ${attr}="${url}" integrity="sha384-${hash}"${cross}${post}>`
        }
      )
      writeFileSync(htmlPath, html)
      console.log(`[ksp-sri] integrity 적용 ${count}개`)
    }
  }
}

export default defineConfig({
  plugins: [vue(), subresourceIntegrity()],
  build: {
    sourcemap: false
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
