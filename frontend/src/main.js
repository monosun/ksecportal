import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import i18n from './i18n'
import './assets/main.css'
import { setupErrorReporting } from './utils/errorReporter'

const pinia = createPinia()
const app = createApp(App)
app.use(pinia)
app.use(router)
app.use(i18n)

import { useUiSettingsStore } from './stores/uiSettings'
useUiSettingsStore().init()

// 화면에서 발생한 오류를 관리 > 에러 로그 관리로 모은다
setupErrorReporting(app)

// 소스 보기 단축키(Windows·Linux: Ctrl+U, macOS: Cmd+Option+U)를 누르면 소스 창 대신 화면을 새로고침한다.
// 한글 입력 상태에서는 e.key 가 'ㅕ' 가 되므로 물리 키(e.code)로 판별한다.
// 개발자 도구 단축키(F12, Ctrl+Shift+I/J/C, macOS Cmd+Option+I/J/C)는 동작하지 않게 막는다.
// ※ 브라우저 메뉴로 여는 개발자 도구까지 막을 수는 없는 화면 단의 억제 장치이며, 보안 통제는 서버에서 한다.
const DEVTOOLS_KEYS = new Set(['KeyI', 'KeyJ', 'KeyC'])
window.addEventListener('keydown', (e) => {
  if (e.code === 'KeyU' && (e.ctrlKey || (e.metaKey && e.altKey))) {
    e.preventDefault()
    location.reload()
    return
  }
  const devtools = e.code === 'F12'
    || (DEVTOOLS_KEYS.has(e.code) && ((e.ctrlKey && e.shiftKey) || (e.metaKey && e.altKey)))
  if (devtools) {
    e.preventDefault()
    e.stopPropagation()
  }
}, true)

// 우클릭 메뉴(검사·페이지 소스 보기)도 막는다. 입력칸에서는 붙여넣기 등을 위해 허용한다.
window.addEventListener('contextmenu', (e) => {
  const t = e.target
  if (t instanceof Element && t.closest('input, textarea, [contenteditable="true"]')) return
  e.preventDefault()
}, true)

app.mount('#app')
