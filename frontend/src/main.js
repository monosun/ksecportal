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
window.addEventListener('keydown', (e) => {
  if (e.code === 'KeyU' && (e.ctrlKey || (e.metaKey && e.altKey))) {
    e.preventDefault()
    location.reload()
  }
}, true)

app.mount('#app')
