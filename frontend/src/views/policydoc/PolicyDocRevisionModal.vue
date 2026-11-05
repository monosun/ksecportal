<template>
  <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
    <div class="absolute inset-0 bg-black/40" @click="$emit('close')"></div>

    <div class="relative bg-white rounded-xl shadow-xl w-full max-w-2xl max-h-[92vh] flex flex-col">
      <div class="flex items-center justify-between px-5 py-3 border-b shrink-0">
        <div class="min-w-0">
          <h2 class="text-lg font-semibold text-gray-900">{{ isEdit ? '버전 정보 수정' : '새 버전 등록' }}</h2>
          <p class="text-xs text-gray-400 truncate">{{ docTitle }}</p>
        </div>
        <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600 p-1">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <div class="px-5 py-4 overflow-y-auto flex-1 space-y-4">
        <RevisionFields v-model="form" :version-placeholder="versionPlaceholder" />
        <div>
          <label class="text-xs text-gray-500">{{ isEdit ? '파일 교체 (선택)' : '정책 문서 파일' }}</label>
          <p v-if="isEdit && revision?.fileName" class="text-xs text-gray-500 mt-1">
            현재 파일: <span class="font-medium">{{ revision.fileName }}</span>
          </p>
          <input type="file" :accept="ACCEPT" @change="onFileChange" class="mt-1 w-full text-sm text-gray-600" />
          <p class="text-xs text-gray-400 mt-1">{{ UPLOAD_HINT }}</p>
          <p v-if="fileError" class="text-xs text-red-600 mt-1">{{ fileError }}</p>
        </div>
        <p v-if="form.revisionType === 'ABOLITION'" class="text-xs text-red-600 bg-red-50 rounded px-3 py-2">
          폐지 버전이 최신 버전이 되면 문서 상태가 <strong>폐지</strong>로 바뀝니다.
        </p>
        <p v-if="error" class="text-sm text-red-600">{{ error }}</p>
      </div>

      <div class="flex justify-end gap-2 px-5 py-3 border-t shrink-0">
        <button @click="$emit('close')" class="btn-secondary">{{ $t('common.cancel') }}</button>
        <button @click="submit" :disabled="saving" class="btn-primary">
          {{ saving ? '저장 중...' : $t('common.save') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { policyDocApi } from '@/api'
import RevisionFields from './PolicyDocRevisionFields.vue'
import { ACCEPT, UPLOAD_HINT, validateFile, today } from './policyDocMeta'

const props = defineProps({
  open: { type: Boolean, default: false },
  docId: { type: [Number, String], default: null },
  docTitle: { type: String, default: '' },
  // 수정할 버전 — 없으면 새 버전 등록
  revision: { type: Object, default: null },
  // 새 버전 등록 시 버전 번호 안내용 현재 버전
  currentVersion: { type: String, default: '' },
})
const emit = defineEmits(['close', 'saved'])

const isEdit = computed(() => !!props.revision)
const form = ref({})
const file = ref(null)
const fileError = ref('')
const error = ref('')
const saving = ref(false)

const versionPlaceholder = computed(() =>
  !isEdit.value && props.currentVersion ? `현재 v${props.currentVersion} — 비우면 자동 증가` : '')

watch(() => props.open, (open) => {
  if (!open) return
  error.value = ''
  fileError.value = ''
  file.value = null
  const r = props.revision
  form.value = r
    ? {
        version: r.version, revisionType: r.revisionType, revisionDate: r.revisionDate,
        effectiveDate: r.effectiveDate, approver: r.approver || '',
        changeReason: r.changeReason || '', changeDetails: r.changeDetails || '',
      }
    : {
        version: '', revisionType: 'AMENDMENT', revisionDate: today(), effectiveDate: today(),
        approver: '', changeReason: '', changeDetails: '',
      }
})

function onFileChange(e) {
  const f = e.target.files[0] || null
  fileError.value = validateFile(f)
  file.value = fileError.value ? null : f
  if (fileError.value) e.target.value = ''
}

async function submit() {
  error.value = ''
  if (!form.value.revisionDate) { error.value = '제·개정일을 입력해주세요.'; return }
  if (isEdit.value && !String(form.value.version || '').trim()) { error.value = '버전을 입력해주세요.'; return }
  if (fileError.value) return
  saving.value = true
  try {
    if (isEdit.value) {
      await policyDocApi.updateRevision(props.revision.id, form.value)
      if (file.value) await policyDocApi.replaceFile(props.revision.id, file.value)
    } else {
      await policyDocApi.addRevision(props.docId, form.value, file.value)
    }
    emit('saved')
  } catch (e) {
    error.value = e || '저장에 실패했습니다.'
  } finally {
    saving.value = false
  }
}
</script>
