<template>
  <div class="min-h-screen bg-gradient-to-br from-primary-900 to-primary-700 flex items-center justify-center p-4">
    <div class="w-full max-w-md">
      <div class="text-center mb-8">
        <div class="flex justify-center items-center gap-3 mb-6">
          <img
            v-if="ui.effectiveLogoUrl()"
            :src="ui.effectiveLogoUrl()"
            alt="SecPortal"
            style="height:36px; width:auto;"
          />
          <span class="text-2xl font-bold tracking-tight text-white">{{ ui.effectiveLogoText() }}</span>
        </div>
        <p class="text-primary-200 text-sm">{{ $t('auth.loginSubtitle') }}</p>
      </div>

      <!-- 1단계: ID/PW 입력 -->
      <div v-if="!mfaStep" class="card">
        <form @submit.prevent="handleLogin" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('auth.email') }}</label>
            <input v-model="form.email" type="email" class="input" required autocomplete="email" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('auth.password') }}</label>
            <div class="relative">
              <input v-model="form.password" :type="showPassword ? 'text' : 'password'" class="input pr-10" required autocomplete="current-password" />
              <button
                type="button"
                class="absolute inset-y-0 right-0 flex items-center pr-3 text-gray-400 hover:text-gray-600 focus:outline-none"
                :title="showPassword ? $t('auth.hidePassword') : $t('auth.showPassword')"
                @click="showPassword = !showPassword"
              >
                <svg v-if="!showPassword" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                </svg>
                <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21"/>
                </svg>
              </button>
            </div>
          </div>
          <div class="flex items-center">
            <input id="save-id" v-model="saveId" type="checkbox" class="w-4 h-4 text-primary-600 border-gray-300 rounded focus:ring-primary-500 cursor-pointer" />
            <label for="save-id" class="ml-2 text-sm text-gray-600 cursor-pointer select-none">{{ $t('auth.saveId') }}</label>
          </div>
          <button type="submit" class="btn-primary w-full" :disabled="loading">
            {{ loading ? $t('common.loading') : $t('auth.login') }}
          </button>
        </form>

        <template v-if="oktaConfig">
          <div class="flex items-center gap-3 mt-5">
            <div class="flex-1 h-px bg-gray-200"></div>
            <span class="text-xs text-gray-400">{{ $t('auth.or') }}</span>
            <div class="flex-1 h-px bg-gray-200"></div>
          </div>
          <button
            type="button"
            class="mt-4 w-full flex items-center justify-center gap-3 border border-gray-300 rounded-lg px-4 py-2.5 text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 transition-colors focus:outline-none focus:ring-2 focus:ring-primary-500"
            :disabled="loading"
            @click="loginWithOkta"
          >
            <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none">
              <path d="M12 0C5.373 0 0 5.373 0 12s5.373 12 12 12 12-5.373 12-12S18.627 0 12 0z" fill="#007DC1"/>
              <path d="M12 5.5a6.5 6.5 0 100 13 6.5 6.5 0 000-13z" fill="#fff"/>
              <path d="M12 8a4 4 0 100 8 4 4 0 000-8z" fill="#007DC1"/>
            </svg>
            {{ $t('auth.loginWithOkta') }}
          </button>
        </template>

        <p class="text-center mt-4 text-sm text-gray-600">
          {{ $t('auth.noAccount') }}
          <RouterLink to="/register" class="text-primary-600 hover:underline font-medium">{{ $t('auth.register') }}</RouterLink>
        </p>
      </div>

      <!-- 2단계: MFA 코드 입력 -->
      <div v-else class="card">
        <div class="flex items-center gap-3 mb-5">
          <div class="w-10 h-10 bg-primary-100 rounded-full flex items-center justify-center flex-shrink-0">
            <svg class="w-5 h-5 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
            </svg>
          </div>
          <div>
            <h3 class="font-semibold text-gray-900">{{ $t('auth.mfaTitle') }}</h3>
            <p class="text-sm text-gray-500 mt-0.5">{{ $t('auth.mfaSubtitle') }}</p>
          </div>
        </div>
        <form @submit.prevent="handleMfaVerify" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('auth.mfaCode') }}</label>
            <input
              v-model="otpCode"
              type="text"
              inputmode="numeric"
              pattern="[0-9]*"
              maxlength="6"
              class="input text-center text-2xl tracking-widest font-mono"
              placeholder="000000"
              autofocus
              required
            />
          </div>
          <button type="submit" class="btn-primary w-full" :disabled="loading || otpCode.length !== 6">
            {{ loading ? $t('common.loading') : $t('auth.mfaVerify') }}
          </button>
          <button type="button" @click="mfaStep = false; otpCode = ''" class="w-full text-sm text-gray-500 hover:text-gray-700 py-2">
            ← {{ $t('auth.mfaBack') }}
          </button>
        </form>
      </div>

      <!-- 버전 정보 -->
      <p class="text-center mt-5 text-xs text-primary-300/50 select-none tracking-wide">
        SecPortal v{{ appVersion }}
      </p>
    </div>

    <!-- Error Modal -->
    <Transition name="modal-fade">
      <div v-if="showError"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
        @click.self="showError = false"
        @keydown.enter.prevent="showError = false">
        <div class="bg-white rounded-xl shadow-xl p-6 max-w-sm w-full mx-4">
          <div class="flex items-start gap-3 mb-5">
            <div class="w-10 h-10 bg-red-100 rounded-full flex items-center justify-center flex-shrink-0">
              <svg class="w-5 h-5 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
              </svg>
            </div>
            <div>
              <h3 class="font-semibold text-gray-900">{{ $t('auth.loginFailed') }}</h3>
              <p class="text-sm text-gray-600 mt-1">{{ error }}</p>
            </div>
          </div>
          <button @click="showError = false" autofocus class="btn-primary w-full">{{ $t('common.confirm') }}</button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useUiSettingsStore } from '@/stores/uiSettings'
import { authApi } from '@/api'
import { version } from '../../../package.json'

const appVersion = version

const SAVED_ID_KEY = 'login_saved_email'

const auth = useAuthStore()
const ui = useUiSettingsStore()
const router = useRouter()
const form = ref({ email: '', password: '' })
const loading = ref(false)
const error = ref('')
const showError = ref(false)
const showPassword = ref(false)
const saveId = ref(false)
const mfaStep = ref(false)
const tempToken = ref('')
const otpCode = ref('')
const oktaConfig = ref(null)

onMounted(async () => {
  const saved = localStorage.getItem(SAVED_ID_KEY)
  if (saved) {
    form.value.email = saved
    saveId.value = true
  }
  try {
    const res = await authApi.oktaConfig()
    if (res.data?.enabled) oktaConfig.value = res.data
  } catch {}
})

function generateCodeVerifier() {
  const array = new Uint8Array(32)
  crypto.getRandomValues(array)
  return btoa(String.fromCharCode(...array))
    .replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '')
}

async function generateCodeChallenge(verifier) {
  const encoded = new TextEncoder().encode(verifier)
  const digest = await crypto.subtle.digest('SHA-256', encoded)
  return btoa(String.fromCharCode(...new Uint8Array(digest)))
    .replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '')
}

async function loginWithOkta() {
  if (!oktaConfig.value) return
  const codeVerifier = generateCodeVerifier()
  const codeChallenge = await generateCodeChallenge(codeVerifier)
  const state = crypto.randomUUID()

  sessionStorage.setItem('okta_code_verifier', codeVerifier)
  sessionStorage.setItem('okta_state', state)

  const params = new URLSearchParams({
    client_id: oktaConfig.value.clientId,
    response_type: 'code',
    scope: 'openid profile email',
    redirect_uri: oktaConfig.value.redirectUri,
    state,
    code_challenge: codeChallenge,
    code_challenge_method: 'S256'
  })

  window.location.href = `${oktaConfig.value.issuer}/v1/authorize?${params}`
}

async function handleLogin() {
  if (showError.value) {
    showError.value = false
    return
  }
  loading.value = true
  error.value = ''
  try {
    if (saveId.value) {
      localStorage.setItem(SAVED_ID_KEY, form.value.email)
    } else {
      localStorage.removeItem(SAVED_ID_KEY)
    }
    const result = await auth.login(form.value)
    if (result.mfaRequired) {
      tempToken.value = result.tempToken
      mfaStep.value = true
    } else {
      router.push('/dashboard')
    }
  } catch (e) {
    error.value = typeof e === 'string' ? e : '로그인에 실패했습니다'
    showError.value = true
  } finally {
    loading.value = false
  }
}

async function handleMfaVerify() {
  loading.value = true
  error.value = ''
  try {
    await auth.verifyMfa(tempToken.value, otpCode.value)
    router.push('/dashboard')
  } catch (e) {
    error.value = typeof e === 'string' ? e : '인증 코드가 올바르지 않습니다'
    showError.value = true
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.2s ease;
}
.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}
</style>
