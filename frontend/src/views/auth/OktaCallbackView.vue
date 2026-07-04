<template>
  <div class="min-h-screen bg-gradient-to-br from-primary-900 to-primary-700 flex items-center justify-center p-4">
    <div class="text-center">
      <div v-if="!errorMsg">
        <div class="w-12 h-12 border-4 border-white/30 border-t-white rounded-full animate-spin mx-auto mb-4"></div>
        <p class="text-white text-sm">{{ $t('auth.oktaProcessing') }}</p>
      </div>
      <div v-else class="bg-white rounded-xl shadow-xl p-6 max-w-sm w-full mx-auto">
        <div class="flex items-start gap-3 mb-5">
          <div class="w-10 h-10 bg-red-100 rounded-full flex items-center justify-center flex-shrink-0">
            <svg class="w-5 h-5 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
          </div>
          <div>
            <h3 class="font-semibold text-gray-900">{{ $t('auth.oktaFailed') }}</h3>
            <p class="text-sm text-gray-600 mt-1">{{ errorMsg }}</p>
          </div>
        </div>
        <button @click="goLogin" class="btn-primary w-full">{{ $t('auth.mfaBack') }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const errorMsg = ref('')

function goLogin() {
  router.push('/login')
}

onMounted(async () => {
  const { code, state, error, error_description } = route.query

  if (error) {
    errorMsg.value = error_description || error
    return
  }

  const savedState = sessionStorage.getItem('okta_state')
  const codeVerifier = sessionStorage.getItem('okta_code_verifier')
  sessionStorage.removeItem('okta_state')
  sessionStorage.removeItem('okta_code_verifier')

  if (!code || !codeVerifier) {
    errorMsg.value = '인증 정보가 없습니다. 다시 시도해주세요.'
    return
  }

  if (state !== savedState) {
    errorMsg.value = '보안 검증에 실패했습니다. 다시 시도해주세요.'
    return
  }

  try {
    await auth.oktaLogin(code, codeVerifier)
    router.push('/dashboard')
  } catch (e) {
    errorMsg.value = typeof e === 'string' ? e : 'Okta 인증에 실패했습니다.'
  }
})
</script>
