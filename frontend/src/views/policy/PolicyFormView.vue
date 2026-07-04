<template>
  <div class="p-8 max-w-3xl mx-auto">
    <RouterLink to="/policies" class="text-sm text-primary-600 hover:underline mb-4 inline-block">← {{ $t('common.back') }}</RouterLink>
    <h1 class="text-2xl font-bold text-gray-900 mb-6">{{ isEdit ? $t('common.edit') : $t('policy.create') }}</h1>

    <form @submit.prevent="handleSubmit" class="card space-y-5">
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('common.title') }} *</label>
        <input v-model="form.title" type="text" class="input" required />
      </div>
      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('policy.category') }} *</label>
          <select v-model="form.category" class="input" required>
            <option v-for="c in categories" :key="c" :value="c">{{ $t(`policy.category_label.${c}`) }}</option>
          </select>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('common.status') }}</label>
          <select v-model="form.status" class="input">
            <option v-for="s in statuses" :key="s" :value="s">{{ $t(`policy.status.${s}`) }}</option>
          </select>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('policy.version') }}</label>
          <input v-model="form.version" type="text" class="input" placeholder="1.0" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('policy.effectiveDate') }}</label>
          <input v-model="form.effectiveDate" type="date" class="input" />
        </div>
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">내용 (Markdown 지원) *</label>
        <textarea v-model="form.content" class="input font-mono text-sm" rows="16" required></textarea>
      </div>
      <p v-if="error" class="text-red-600 text-sm">{{ error }}</p>
      <div class="flex gap-3 justify-end">
        <RouterLink to="/policies" class="btn-secondary">{{ $t('common.cancel') }}</RouterLink>
        <button type="submit" class="btn-primary" :disabled="loading">
          {{ loading ? $t('common.loading') : $t('common.save') }}
        </button>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { policyApi } from '@/api'

const route = useRoute()
const router = useRouter()
const isEdit = !!route.params.id

const form = ref({ title: '', content: '', category: 'GENERAL', status: 'DRAFT', version: '1.0', effectiveDate: '' })
const loading = ref(false)
const error = ref('')

const categories = ['GENERAL', 'ACCESS_CONTROL', 'DATA_PROTECTION', 'INCIDENT_RESPONSE', 'NETWORK', 'PHYSICAL', 'VENDOR', 'OTHER']
const statuses = ['DRAFT', 'REVIEW', 'PUBLISHED', 'ARCHIVED']

onMounted(async () => {
  if (isEdit) {
    const res = await policyApi.get(route.params.id)
    const p = res.data
    form.value = { title: p.title, content: p.content, category: p.category, status: p.status, version: p.version, effectiveDate: p.effectiveDate || '' }
  }
})

async function handleSubmit() {
  loading.value = true
  error.value = ''
  try {
    const payload = { ...form.value }
    if (!payload.effectiveDate) delete payload.effectiveDate
    if (isEdit) {
      await policyApi.update(route.params.id, payload)
    } else {
      await policyApi.create(payload)
    }
    router.push('/policies')
  } catch (e) {
    error.value = typeof e === 'string' ? e : 'Failed to save'
  } finally {
    loading.value = false
  }
}
</script>
