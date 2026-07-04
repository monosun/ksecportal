<template>
  <div class="min-h-screen bg-gradient-to-br from-primary-900 to-primary-700 flex items-center justify-center p-4">
    <div class="w-full max-w-md">
      <div class="text-center mb-8">
        <h1 class="text-3xl font-bold text-white">SecPortal</h1>
        <p class="text-primary-200 mt-1">{{ $t('auth.register') }}</p>
      </div>
      <div class="card">
        <form @submit.prevent="handleRegister" class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('auth.name') }}</label>
            <input v-model="form.name" type="text" class="input" required />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('auth.email') }}</label>
            <input v-model="form.email" type="email" class="input" required />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('auth.department') }}</label>
            <select v-model="form.department" class="input">
              <option value="">부서를 선택하세요</option>
              <option v-for="dept in departments" :key="dept.value" :value="dept.value">{{ dept.label }}</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('auth.password') }}</label>
            <input v-model="form.password" type="password" class="input" required />
            <p class="text-xs text-gray-400 mt-1">{{ PASSWORD_HINT }}</p>
          </div>
          <p v-if="error" class="text-red-600 text-sm">{{ error }}</p>
          <button type="submit" class="btn-primary w-full" :disabled="loading">
            {{ loading ? $t('common.loading') : $t('auth.register') }}
          </button>
        </form>
        <p class="text-center mt-4 text-sm text-gray-600">
          {{ $t('auth.hasAccount') }}
          <RouterLink to="/login" class="text-primary-600 hover:underline font-medium">{{ $t('auth.login') }}</RouterLink>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { authApi, codeApi } from '@/api'
import { validatePassword, PASSWORD_HINT } from '@/utils/password'

const router = useRouter()
const form = ref({ name: '', email: '', password: '', department: '' })
const loading = ref(false)
const error = ref('')
const departments = ref([])

async function handleRegister() {
  error.value = ''
  const strengthError = validatePassword(form.value.password)
  if (strengthError) {
    error.value = strengthError
    return
  }
  loading.value = true
  try {
    await authApi.register(form.value)
    router.push('/login')
  } catch (e) {
    error.value = typeof e === 'string' ? e : 'Registration failed'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    const res = await codeApi.getValues('DEPARTMENT')
    departments.value = res.data
  } catch {
    // 코드 없으면 빈 목록 유지
  }
})
</script>
