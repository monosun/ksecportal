<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">보안 설정</h1>
        <p class="text-sm text-gray-400 mt-0.5">로그인 보안 정책 및 Okta SSO 연동을 설정합니다.</p>
      </div>
    </div>

    <div class="page-body">
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">

      <!-- 보안 설정 -->
      <div class="card">
        <h2 class="text-base font-bold text-gray-800 mb-1">보안 설정</h2>
        <p class="text-sm text-gray-400 mb-5">로그인 실패 시 계정 잠금 정책을 설정합니다.</p>
        <div class="space-y-4 max-w-sm">
          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-1">최대 로그인 실패 횟수</label>
            <div class="flex items-center gap-2">
              <input v-model.number="secCfg.maxAttempts" type="number" min="1" max="5" class="input w-24 text-sm" />
              <span class="text-sm text-gray-400">회 초과 시 계정 잠금</span>
            </div>
          </div>
          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-1">기본 잠금 시간 (분)</label>
            <div class="flex items-center gap-2">
              <input v-model.number="secCfg.lockoutMinutes" type="number" min="1" max="1440" class="input w-24 text-sm" />
              <span class="text-sm text-gray-400">분, 재시도 실패 시 2배씩 증가</span>
            </div>
          </div>
          <div class="flex items-center gap-3 pt-1">
            <button @click="saveSecurityConfig" :disabled="secSaving"
              class="btn-primary px-6 py-2 text-sm rounded-xl disabled:opacity-50">
              {{ secSaving ? '저장 중...' : '저장' }}
            </button>
            <span v-if="secSaved" class="text-sm text-green-600 font-semibold">저장되었습니다.</span>
          </div>
        </div>
      </div>

      <!-- Okta SSO 설정 -->
      <div class="card">
        <div class="flex items-center justify-between mb-1">
          <h2 class="text-base font-bold text-gray-800 flex items-center gap-2">
            <svg class="w-5 h-5 text-blue-600" viewBox="0 0 24 24" fill="none">
              <path d="M12 2C6.477 2 2 6.477 2 12s4.477 10 10 10 10-4.477 10-10S17.523 2 12 2z" fill="#007DC1"/>
              <path d="M12 6.5a5.5 5.5 0 100 11 5.5 5.5 0 000-11z" fill="#fff"/>
              <path d="M12 9a3 3 0 100 6 3 3 0 000-6z" fill="#007DC1"/>
            </svg>
            Okta SSO 연동 설정
          </h2>
          <span v-if="oktaStatus === 'ok'" class="inline-flex items-center gap-1.5 text-xs font-bold px-2.5 py-1 rounded-full bg-green-100 text-green-700">
            <svg class="w-3 h-3" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/></svg>
            연결됨
          </span>
          <span v-else-if="oktaStatus === 'fail'" class="inline-flex items-center gap-1.5 text-xs font-bold px-2.5 py-1 rounded-full bg-red-100 text-red-700">
            연결 실패
          </span>
        </div>
        <p class="text-sm text-gray-400 mb-5">Okta OIDC 앱 정보를 입력하면 로그인 화면에 "Okta로 로그인" 버튼이 표시됩니다.</p>

        <div class="space-y-4 max-w-2xl">
          <div class="flex items-center gap-3">
            <button
              @click="oktaCfg.enabled = !oktaCfg.enabled"
              class="relative inline-flex h-6 w-11 flex-shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 focus:outline-none"
              :class="oktaCfg.enabled ? 'bg-primary-500' : 'bg-gray-200'">
              <span
                class="pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200"
                :class="oktaCfg.enabled ? 'translate-x-5' : 'translate-x-0'"/>
            </button>
            <span class="text-sm font-semibold text-gray-700">Okta SSO 활성화</span>
            <span class="text-xs px-2 py-0.5 rounded-full font-semibold"
              :class="oktaCfg.enabled ? 'bg-primary-100 text-primary-700' : 'bg-gray-100 text-gray-500'">
              {{ oktaCfg.enabled ? '활성화' : '비활성화' }}
            </span>
          </div>

          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-1">
              Client ID <span class="text-red-500">*</span>
            </label>
            <input v-model="oktaCfg.clientId" type="text" class="input w-full font-mono text-sm"
              placeholder="0oaXXXXXXXXXXXXXXXXX" />
            <p class="text-xs text-gray-400 mt-1">Okta 관리자 콘솔 → Applications → 앱 선택 → General 탭에서 확인</p>
          </div>

          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-1">
              Issuer URI <span class="text-red-500">*</span>
            </label>
            <input v-model="oktaCfg.issuer" type="url" class="input w-full font-mono text-sm"
              placeholder="https://dev-XXXXXX.okta.com/oauth2/default" />
            <p class="text-xs text-gray-400 mt-1">Security → API → Authorization Servers → default의 Issuer URI</p>
          </div>

          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-1">
              Redirect URI <span class="text-red-500">*</span>
            </label>
            <input v-model="oktaCfg.redirectUri" type="url" class="input w-full font-mono text-sm"
              placeholder="https://yourdomain.com/auth/okta/callback" />
            <p class="text-xs text-gray-400 mt-1">Okta 앱의 Sign-in redirect URIs에 등록한 값과 정확히 일치해야 합니다.</p>
          </div>

          <div class="flex items-start gap-2 p-3 rounded-xl bg-amber-50 border border-amber-200 text-sm text-amber-800">
            <svg class="w-4 h-4 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
            <span>DB 설정이 환경변수(<code class="font-mono bg-amber-100 px-1 rounded">OKTA_*</code>)보다 우선 적용됩니다. 비워두면 환경변수 값을 사용합니다.</span>
          </div>

          <div class="flex items-center gap-3 flex-wrap pt-1">
            <button @click="saveOktaConfig" :disabled="oktaSaving"
              class="btn-primary px-6 py-2 text-sm rounded-xl disabled:opacity-50">
              {{ oktaSaving ? '저장 중...' : '저장' }}
            </button>
            <button @click="testOktaConnection" :disabled="oktaTesting"
              class="px-4 py-2 text-sm rounded-xl border-2 border-gray-200 bg-white text-gray-600 hover:border-gray-300 transition-all disabled:opacity-50">
              {{ oktaTesting ? '연결 확인 중...' : '연결 테스트' }}
            </button>
            <span v-if="oktaSaved" class="text-sm text-green-600 font-semibold">저장되었습니다.</span>
            <span v-if="oktaTestMsg" class="text-sm font-semibold"
              :class="oktaStatus === 'ok' ? 'text-green-600' : 'text-red-500'">
              {{ oktaTestMsg }}
            </span>
          </div>
        </div>
      </div>

      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { securityConfigApi, appSettingApi, authApi } from '@/api/index.js'

const secCfg = ref({ maxAttempts: 5, lockoutMinutes: 10 })
const secSaving = ref(false)
const secSaved = ref(false)

const oktaCfg = ref({ enabled: false, clientId: '', issuer: '', redirectUri: '' })
const oktaSaving = ref(false)
const oktaSaved = ref(false)
const oktaTesting = ref(false)
const oktaTestMsg = ref('')
const oktaStatus = ref('')

async function loadSecurityConfig() {
  try {
    const res = await securityConfigApi.get()
    secCfg.value = res.data
  } catch {}
}

async function saveSecurityConfig() {
  secSaving.value = true
  secSaved.value = false
  try {
    await securityConfigApi.update(secCfg.value)
    secSaved.value = true
    setTimeout(() => { secSaved.value = false }, 3000)
  } finally {
    secSaving.value = false
  }
}

async function loadOktaConfig() {
  try {
    const res = await appSettingApi.getAll()
    const s = res.data || {}
    oktaCfg.value = {
      enabled:     s['okta.enabled']     === 'true',
      clientId:    s['okta.client_id']   || '',
      issuer:      s['okta.issuer']      || '',
      redirectUri: s['okta.redirect_uri'] || ''
    }
    if (!oktaCfg.value.clientId && !oktaCfg.value.issuer) {
      const cfgRes = await authApi.oktaConfig()
      const c = cfgRes.data || {}
      if (c.enabled) {
        oktaCfg.value = {
          enabled:     true,
          clientId:    c.clientId    || '',
          issuer:      c.issuer      || '',
          redirectUri: c.redirectUri || ''
        }
      }
    }
  } catch {}
}

async function saveOktaConfig() {
  oktaSaving.value = true
  oktaSaved.value = false
  oktaTestMsg.value = ''
  try {
    await appSettingApi.update('okta.enabled',      String(oktaCfg.value.enabled))
    await appSettingApi.update('okta.client_id',    oktaCfg.value.clientId)
    await appSettingApi.update('okta.issuer',       oktaCfg.value.issuer)
    await appSettingApi.update('okta.redirect_uri', oktaCfg.value.redirectUri)
    oktaSaved.value = true
    setTimeout(() => { oktaSaved.value = false }, 3000)
  } catch {
    alert('Okta 설정 저장에 실패했습니다.')
  } finally {
    oktaSaving.value = false
  }
}

async function testOktaConnection() {
  oktaTesting.value = true
  oktaTestMsg.value = ''
  oktaStatus.value = ''
  try {
    const res = await authApi.oktaTest()
    const r = res.data || {}
    oktaStatus.value = r.ok ? 'ok' : 'fail'
    oktaTestMsg.value = r.message || ''
  } catch (e) {
    oktaStatus.value = 'fail'
    oktaTestMsg.value = typeof e === 'string' ? e : '연결 테스트 실패'
  } finally {
    oktaTesting.value = false
    setTimeout(() => { oktaTestMsg.value = ''; oktaStatus.value = '' }, 6000)
  }
}

onMounted(() => {
  loadSecurityConfig()
  loadOktaConfig()
})
</script>
