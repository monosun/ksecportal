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
        <!-- 문서 파일로 등록 — 제목·본문을 문서에서 뽑아 폼에 채운다 -->
        <div class="rounded-lg border border-dashed border-gray-300 bg-gray-50 px-4 py-3">
          <div class="flex items-center justify-between gap-3 flex-wrap">
            <div class="min-w-0">
              <p class="text-sm font-medium text-gray-700">문서 파일로 등록</p>
              <p class="text-xs text-gray-500 mt-0.5">
                PDF · DOCX · TXT · MD 를 올리면 제목과 본문을 채웁니다. 조(條)는 저장할 때 자동 세분화됩니다.
              </p>
            </div>
            <button type="button" @click="docInput?.click()" :disabled="docLoading"
              class="btn-secondary text-sm whitespace-nowrap disabled:opacity-50">
              {{ docLoading ? '읽는 중...' : '파일 선택' }}
            </button>
            <input ref="docInput" type="file" accept=".pdf,.docx,.txt,.md" class="hidden" @change="onDocSelected" />
          </div>
          <p v-if="docName" class="text-xs text-gray-600 mt-2 truncate">불러온 파일: {{ docName }}</p>
          <p v-for="(w, i) in docWarnings" :key="i" class="text-xs text-amber-700 mt-1">· {{ w }}</p>
          <p v-if="docError" class="text-xs text-red-600 mt-1 whitespace-pre-line">{{ docError }}</p>
        </div>

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

// ── 문서 파일로 등록 ───────────────────────────────────────────────────
const MAX_DOC_MB = 20
const docInput = ref(null)
const docLoading = ref(false)
const docName = ref('')
const docError = ref('')
const docWarnings = ref([])

async function onDocSelected(e) {
  const file = e.target.files?.[0]
  if (docInput.value) docInput.value.value = ''   // 같은 파일 다시 고를 수 있게
  if (!file) return

  docError.value = ''
  docWarnings.value = []
  if (file.size > MAX_DOC_MB * 1024 * 1024) {
    docError.value = `파일이 너무 큽니다 (최대 ${MAX_DOC_MB}MB).`
    return
  }
  // 이미 쓴 내용을 말없이 덮지 않는다.
  if (form.value.content?.trim() &&
      !window.confirm('이미 입력한 제목·내용을 문서에서 읽은 내용으로 바꿉니다. 계속할까요?')) {
    return
  }

  docLoading.value = true
  try {
    const r = (await policyApi.extractDocument(file)).data
    form.value.title = r.title || form.value.title
    form.value.content = r.content || ''
    docName.value = file.name
    docWarnings.value = r.warnings || []
    if (r.articleCount) docWarnings.value.unshift(`조(條) ${r.articleCount}개를 찾았습니다.`)
  } catch (err) {
    docError.value = typeof err === 'string' ? err : '문서를 읽지 못했습니다.'
  } finally {
    docLoading.value = false
  }
}

watch(() => props.open, async (open) => {
  if (!open) return
  error.value = ''
  docName.value = ''
  docError.value = ''
  docWarnings.value = []
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
