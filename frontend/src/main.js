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

app.mount('#app')
