<template>
  <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
    <div class="absolute inset-0 bg-black/40" @click="$emit('close')"></div>

    <div class="relative bg-white rounded-xl shadow-xl w-full max-w-3xl max-h-[92vh] flex flex-col">
      <div class="flex items-center justify-between px-5 py-3 border-b shrink-0">
        <h2 class="text-lg font-semibold text-gray-900">{{ isEdit ? $t('common.edit') : $t('policy.create') }}</h2>
        <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <form id="policyForm" @submit.prevent="handleSubmit" class="px-5 py-4 overflow-y-auto flex-1 space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('common.title') }} *</label>
          <input v-model="form.title" type="text" class="input w-full" required />
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('policy.category') }} *</label>
            <select v-model="form.category" class="input w-full" required>
              <option v-for="c in categories" :key="c" :value="c">{{ $t(`policy.category_label.${c}`) }}</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('common.status') }}</label>
            <select v-model="form.status" class="input w-full">
              <option v-for="s in statuses" :key="s" :value="s">{{ $t(`policy.status.${s}`) }}</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('policy.version') }}</label>
            <input v-model="form.version" type="text" class="input w-full" placeholder="1.0" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('policy.effectiveDate') }}</label>
            <input v-model="form.effectiveDate" type="date" class="input w-full" />
          </div>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">내용 (Markdown) *</label>
          <MarkdownEditor v-model="form.content" :rows="14" required
            placeholder="## 목적&#10;&#10;이 정책은 …" />
        </div>
        <p v-if="error" class="text-red-600 text-sm">{{ error }}</p>
      </form>

      <div class="flex justify-end gap-3 px-5 py-3 border-t shrink-0">
        <button type="button" @click="$emit('close')" class="btn-secondary text-sm">{{ $t('common.cancel') }}</button>
        <button type="submit" form="policyForm" class="btn-primary text-sm" :disabled="loading">
          {{ loading ? $t('common.loading') : $t('common.save') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import { policyApi } from '@/api'

const props = defineProps({
  open: { type: Boolean, default: false },
  editId: { type: [Number, String], default: null }
})
const emit = defineEmits(['close', 'saved'])

const isEdit = computed(() => !!props.editId)

const categories = ['GENERAL', 'ACCESS_CONTROL', 'DATA_PROTECTION', 'INCIDENT_RESPONSE', 'NETWORK', 'PHYSICAL', 'VENDOR', 'OTHER']
const statuses = ['DRAFT', 'REVIEW', 'PUBLISHED', 'ARCHIVED']

function emptyForm() {
  return { title: '', content: '', category: 'GENERAL', status: 'DRAFT', version: '1.0', effectiveDate: '' }
}
const form = ref(emptyForm())
const loading = ref(false)
const error = ref('')

watch(() => props.open, async (open) => {
  if (!open) return
  error.value = ''
  if (props.editId) {
    try {
      const p = (await policyApi.get(props.editId)).data
      form.value = { title: p.title, content: p.content, category: p.category, status: p.status, version: p.version, effectiveDate: p.effectiveDate || '' }
    } catch (e) { error.value = typeof e === 'string' ? e : '불러오기에 실패했습니다.' }
  } else {
    form.value = emptyForm()
  }
})

async function handleSubmit() {
  // 미리보기 모드에서는 textarea 가 DOM 에 없어 required 검증이 걸리지 않으므로 직접 확인한다.
  if (!form.value.content?.trim()) {
    error.value = '내용을 입력하세요.'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const payload = { ...form.value }
    if (!payload.effectiveDate) delete payload.effectiveDate
    if (isEdit.value) await policyApi.update(props.editId, payload)
    else await policyApi.create(payload)
    emit('saved')
  } catch (e) {
    error.value = typeof e === 'string' ? e : 'Failed to save'
  } finally {
    loading.value = false
  }
}
</script>
