<template>
  <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
    <div class="absolute inset-0 bg-black/40" @click="$emit('close')"></div>

    <div class="relative bg-white rounded-xl shadow-xl w-full max-w-2xl max-h-[92vh] flex flex-col">
      <div class="flex items-center justify-between px-5 py-3 border-b shrink-0">
        <h2 class="text-lg font-semibold text-gray-900">{{ isEdit ? '정책 문서 수정' : '정책 문서 등록' }}</h2>
        <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600 p-1">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <div class="px-5 py-4 overflow-y-auto flex-1 space-y-4">
        <div v-if="loading" class="py-10 text-center text-gray-400">{{ $t('common.loading') }}</div>
        <template v-else>
          <section class="space-y-3">
            <h3 class="text-sm font-semibold text-gray-700">문서 기본정보</h3>
            <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div>
                <label class="text-xs text-gray-500">분류 *</label>
                <select v-model="doc.category" class="input w-full mt-1">
                  <option value="">선택하세요</option>
                  <option v-for="c in CATEGORIES" :key="c.value" :value="c.value">{{ c.label }}</option>
                </select>
              </div>
              <div class="sm:col-span-2">
                <label class="text-xs text-gray-500">문서명 *</label>
                <input v-model="doc.title" class="input w-full mt-1" placeholder="예: 정보보호 규정" />
              </div>
              <div>
                <label class="text-xs text-gray-500">문서번호</label>
                <input v-model="doc.docNo" class="input w-full mt-1" placeholder="예: ISP-REG-001" />
              </div>
              <div>
                <label class="text-xs text-gray-500">주관부서</label>
                <DepartmentInput v-model="doc.department" input-class="input w-full mt-1" />
              </div>
              <div>
                <label class="text-xs text-gray-500">관리 담당자</label>
                <UserNameInput v-model="doc.owner" class="mt-1" />
              </div>
            </div>
            <div>
              <label class="text-xs text-gray-500">문서 설명</label>
              <textarea v-model="doc.description" rows="2" class="input w-full mt-1"
                placeholder="문서의 목적·적용 범위 등"></textarea>
            </div>
          </section>

          <!-- 신규 등록 시 최초 버전을 함께 받는다. 수정 시 버전은 상세 화면의 버전 이력에서 관리한다. -->
          <section v-if="!isEdit" class="space-y-3 border-t pt-4">
            <h3 class="text-sm font-semibold text-gray-700">최초 버전</h3>
            <RevisionFields v-model="revision" />
            <div>
              <label class="text-xs text-gray-500">정책 문서 파일</label>
              <input type="file" :accept="ACCEPT" @change="onFileChange" class="mt-1 w-full text-sm text-gray-600" />
              <p class="text-xs text-gray-400 mt-1">{{ UPLOAD_HINT }}</p>
              <p v-if="fileError" class="text-xs text-red-600 mt-1">{{ fileError }}</p>
            </div>
          </section>
          <p v-else class="text-xs text-gray-400 border-t pt-3">
            버전·개정사항·파일은 상세 화면의 <strong>버전 이력</strong>에서 추가·수정합니다.
          </p>

          <p v-if="error" class="text-sm text-red-600">{{ error }}</p>
        </template>
      </div>

      <div class="flex justify-end gap-2 px-5 py-3 border-t shrink-0">
        <button @click="$emit('close')" class="btn-secondary">{{ $t('common.cancel') }}</button>
        <button @click="submit" :disabled="saving || loading" class="btn-primary">
          {{ saving ? '저장 중...' : (isEdit ? $t('common.save') : $t('common.create')) }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { policyDocApi } from '@/api'
import RevisionFields from './PolicyDocRevisionFields.vue'
import UserNameInput from '@/components/UserNameInput.vue'
import DepartmentInput from '@/components/DepartmentInput.vue'
import { CATEGORIES, ACCEPT, UPLOAD_HINT, validateFile, today } from './policyDocMeta'

const props = defineProps({
  open: { type: Boolean, default: false },
  itemId: { type: [Number, String], default: null },
  // 목록에서 분류 탭을 고른 상태로 등록하면 그 분류를 기본값으로 쓴다
  defaultCategory: { type: String, default: '' },
})
const emit = defineEmits(['close', 'saved'])

const isEdit = computed(() => !!props.itemId)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const fileError = ref('')
const file = ref(null)

const emptyDoc = () => ({ category: props.defaultCategory || '', title: '', docNo: '', department: '', owner: '', description: '' })
const emptyRevision = () => ({
  version: '1.0', revisionType: 'ENACTMENT', revisionDate: today(), effectiveDate: today(),
  approver: '', changeReason: '', changeDetails: '',
})
const doc = ref(emptyDoc())
const revision = ref(emptyRevision())

watch(() => props.open, async (open) => {
  if (!open) return
  error.value = ''
  fileError.value = ''
  file.value = null
  revision.value = emptyRevision()
  if (!isEdit.value) { doc.value = emptyDoc(); return }
  loading.value = true
  try {
    const res = await policyDocApi.get(props.itemId)
    const d = res.data
    doc.value = {
      category: d.category, title: d.title, docNo: d.docNo || '', department: d.department || '',
      owner: d.owner || '', description: d.description || '',
    }
  } catch (e) {
    error.value = e || '문서를 불러오지 못했습니다.'
  } finally {
    loading.value = false
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
  if (!doc.value.category) { error.value = '분류를 선택해주세요.'; return }
  if (!doc.value.title.trim()) { error.value = '문서명을 입력해주세요.'; return }
  if (fileError.value) return
  saving.value = true
  try {
    if (isEdit.value) {
      await policyDocApi.update(props.itemId, doc.value)
      emit('saved', props.itemId)
    } else {
      const res = await policyDocApi.create({ doc: doc.value, revision: revision.value }, file.value)
      emit('saved', res.data?.id)
    }
  } catch (e) {
    error.value = e || '저장에 실패했습니다.'
  } finally {
    saving.value = false
  }
}
</script>
