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
        <h1 class="text-xl font-bold text-white mb-2">{{ $t('auth.forceChangeTitle') }}</h1>
        <p class="text-primary-200 text-sm">{{ $t('auth.forceChangeSubtitle') }}</p>
      </div>

      <div class="card">
        <!-- Warning notice -->
        <div class="flex items-start gap-3 mb-5 p-3 bg-amber-50 rounded-xl border border-amber-200">
          <svg class="w-5 h-5 text-amber-500 mt-0.5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5C2.962 18.333 3.924 20 5.464 20z"/>
          </svg>
          <p class="text-sm text-amber-800">{{ $t('auth.forceChangeWarning') }}</p>
        </div>

        <form @submit.prevent="handleSubmit" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('auth.currentPassword') }}</label>
            <input v-model="form.currentPassword" type="password" class="input" required autocomplete="current-password" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('auth.newPassword') }}</label>
            <input v-model="form.newPassword" type="password" class="input" required autocomplete="new-password" />
            <p class="text-xs text-gray-400 mt-1">{{ PASSWORD_HINT }}</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('auth.confirmPassword') }}</label>
            <input v-model="form.confirmPassword" type="password" class="input" required autocomplete="new-password" />
          </div>

          <div v-if="error" class="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg p-3">
            {{ error }}
          </div>

          <button type="submit" class="btn-primary w-full" :disabled="loading">
            {{ loading ? $t('common.loading') : $t('auth.forceChangeBtn') }}
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useUiSettingsStore } from '@/stores/uiSettings'
import { validatePassword, PASSWORD_HINT } from '@/utils/password'

const router = useRouter()
const auth = useAuthStore()
const ui = useUiSettingsStore()

const form = ref({ currentPassword: '', newPassword: '', confirmPassword: '' })
const loading = ref(false)
const error = ref('')

async function handleSubmit() {
  error.value = ''
  const strengthError = validatePassword(form.value.newPassword)
  if (strengthError) {
    error.value = strengthError
    return
  }
  if (form.value.newPassword !== form.value.confirmPassword) {
    error.value = '새 비밀번호가 일치하지 않습니다.'
    return
  }
  loading.value = true
  try {
    await authApi.changePassword({
      currentPassword: form.value.currentPassword,
      newPassword: form.value.newPassword
    })
    await auth.fetchMe()
    router.push('/dashboard')
  } catch (e) {
    error.value = typeof e === 'string' ? e : '비밀번호 변경에 실패했습니다.'
  } finally {
    loading.value = false
  }
}
</script>
