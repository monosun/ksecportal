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
            <div class="relative">
              <input v-model="form.currentPassword" :type="showCurrent ? 'text' : 'password'" class="input pr-10" required autocomplete="current-password" />
              <button
                type="button"
                class="absolute inset-y-0 right-0 flex items-center pr-3 text-gray-400 hover:text-gray-600 focus:outline-none"
                :title="showCurrent ? $t('auth.hidePassword') : $t('auth.showPassword')"
                @click="showCurrent = !showCurrent"
              >
                <svg v-if="!showCurrent" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                </svg>
                <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21"/>
                </svg>
              </button>
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('auth.newPassword') }}</label>
            <div class="relative">
              <input v-model="form.newPassword" :type="showNew ? 'text' : 'password'" class="input pr-10" required autocomplete="new-password" />
              <button
                type="button"
                class="absolute inset-y-0 right-0 flex items-center pr-3 text-gray-400 hover:text-gray-600 focus:outline-none"
                :title="showNew ? $t('auth.hidePassword') : $t('auth.showPassword')"
                @click="showNew = !showNew"
              >
                <svg v-if="!showNew" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                </svg>
                <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21"/>
                </svg>
              </button>
            </div>
            <p class="text-xs text-gray-400 mt-1">{{ PASSWORD_HINT }}</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('auth.confirmPassword') }}</label>
            <div class="relative">
              <input v-model="form.confirmPassword" :type="showConfirm ? 'text' : 'password'" class="input pr-10" required autocomplete="new-password" />
              <button
                type="button"
                class="absolute inset-y-0 right-0 flex items-center pr-3 text-gray-400 hover:text-gray-600 focus:outline-none"
                :title="showConfirm ? $t('auth.hidePassword') : $t('auth.showPassword')"
                @click="showConfirm = !showConfirm"
              >
                <svg v-if="!showConfirm" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                </svg>
                <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21"/>
                </svg>
              </button>
            </div>
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
const showCurrent = ref(false)
const showNew = ref(false)
const showConfirm = ref(false)
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
