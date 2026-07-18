<template>
  <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
    <div class="absolute inset-0 bg-black/40" @click="$emit('close')"></div>
    <div class="relative bg-white rounded-xl shadow-xl w-full max-w-3xl max-h-[92vh] flex flex-col">
      <!-- 헤더 -->
      <div class="flex items-center justify-between px-5 py-3 border-b shrink-0">
        <div>
          <h2 class="text-lg font-semibold text-gray-900">버전 이력 · 첨부파일</h2>
          <p class="text-sm text-gray-500 mt-0.5">
            {{ consent?.title }} <span class="text-gray-400">— 현재 v{{ currentVersion }}</span>
          </p>
        </div>
        <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <div class="px-5 py-4 overflow-y-auto flex-1">
        <!-- 새 버전 추가 -->
        <div v-if="canWrite" class="mb-5">
          <button v-if="!showAdd" @click="openAdd" class="btn-secondary text-sm flex items-center gap-1.5">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
            </svg>
            새 버전 추가
          </button>

          <form v-else @submit.prevent="submitVersion" class="bg-gray-50 border border-gray-100 rounded-lg p-4">
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">버전 <span class="text-gray-400 font-normal">(자동)</span></label>
                <input :value="'v' + nextVersion" type="text" readonly disabled
                  class="input w-full bg-gray-100 text-gray-500 cursor-not-allowed" />
                <p class="text-xs text-gray-400 mt-1">현재 v{{ currentVersion }} → 자동 증가</p>
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">시행일</label>
                <input v-model="addForm.effectiveDate" type="date" class="input w-full" />
              </div>
              <div class="col-span-2">
                <label class="block text-sm font-medium text-gray-700 mb-1">변경 사유</label>
                <textarea v-model="addForm.changeNote" rows="2" class="input w-full"
                  placeholder="예: 이용목적 문구 개정, 보유기간 변경"></textarea>
              </div>
              <div class="col-span-2">
                <label class="block text-sm font-medium text-gray-700 mb-1">첨부파일</label>
                <input ref="fileInput" type="file" @change="onFileChange"
                  class="block w-full text-sm text-gray-600 file:mr-3 file:py-1.5 file:px-3 file:rounded file:border-0 file:text-sm file:bg-gray-200 file:text-gray-700 hover:file:bg-gray-300" />
                <p class="text-xs text-gray-400 mt-1">PDF·이미지·문서 파일 (동의서 본문/서명본 등)</p>
              </div>
            </div>
            <p v-if="addError" class="text-red-600 text-sm mt-2">{{ addError }}</p>
            <div class="flex justify-end gap-2 mt-3">
              <button type="button" @click="showAdd = false" class="btn-secondary text-sm">취소</button>
              <button type="submit" class="btn-primary text-sm" :disabled="saving">
                {{ saving ? '저장 중...' : '버전 등록' }}
              </button>
            </div>
          </form>
        </div>

        <!-- 이력 목록 -->
        <div v-if="loading" class="py-8 text-center text-gray-400">로딩 중...</div>
        <div v-else-if="!versions.length" class="py-8 text-center text-gray-400">등록된 버전 이력이 없습니다.</div>
        <ol v-else class="relative border-l-2 border-gray-100 ml-2 space-y-4">
          <li v-for="(v, i) in versions" :key="v.id" class="ml-4">
            <span class="absolute -left-[7px] w-3 h-3 rounded-full"
              :class="i === 0 ? 'bg-primary-500' : 'bg-gray-300'"></span>
            <div class="bg-white border border-gray-100 rounded-lg px-4 py-3 shadow-sm">
              <div class="flex items-center justify-between gap-2">
                <div class="flex items-center gap-2">
                  <span class="font-bold text-gray-900">v{{ v.version }}</span>
                  <span v-if="i === 0" class="badge-green text-[11px]">최신</span>
                  <span v-if="v.effectiveDate" class="text-xs text-gray-500">시행 {{ v.effectiveDate }}</span>
                </div>
                <button v-if="canWrite" @click="removeVersion(v)"
                  class="text-xs text-red-500 hover:text-red-600 px-1.5 py-0.5">삭제</button>
              </div>
              <p v-if="v.changeNote" class="text-sm text-gray-600 mt-1.5 whitespace-pre-line">{{ v.changeNote }}</p>
              <div class="flex items-center justify-between gap-2 mt-2">
                <button v-if="v.hasFile" @click="download(v)"
                  class="inline-flex items-center gap-1.5 text-sm text-primary-600 hover:text-primary-700">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M12 10v6m0 0l-3-3m3 3l3-3m2 6H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                  </svg>
                  {{ v.fileName }} <span class="text-gray-400">({{ formatSize(v.fileSize) }})</span>
                </button>
                <span v-else class="text-xs text-gray-300">첨부파일 없음</span>
                <span class="text-xs text-gray-400">
                  {{ v.author || '—' }} · {{ formatDate(v.createdAt) }}
                </span>
              </div>
            </div>
          </li>
        </ol>
      </div>

      <div class="flex justify-end px-5 py-3 border-t shrink-0">
        <button @click="$emit('close')" class="btn-secondary text-sm">닫기</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, computed } from 'vue'
import { privacyConsentApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const props = defineProps({
  open: { type: Boolean, default: false },
  consent: { type: Object, default: null },
})
const emit = defineEmits(['close', 'changed'])

const auth = useAuthStore()
const canWrite = computed(() => auth.canWrite('privacy_consent'))

const versions = ref([])
const loading = ref(false)
const saving = ref(false)
const showAdd = ref(false)
const addError = ref('')
const fileInput = ref(null)
const selectedFile = ref(null)
const addForm = reactive({ effectiveDate: '', changeNote: '' })

// 현재 버전은 최신 이력(있으면)을 우선 사용해 삭제/추가 직후에도 정확히 반영한다.
const currentVersion = computed(() => versions.value[0]?.version || props.consent?.version || '1.0')

// 서버 규칙과 동일하게 minor(소수부) 자동 증가
const nextVersion = computed(() => {
  const parts = String(currentVersion.value).split('.')
  if (parts.length >= 2 && !isNaN(+parts[0]) && !isNaN(+parts[1])) {
    return `${parts[0]}.${+parts[1] + 1}`
  }
  return `${currentVersion.value}.1`
})

watch(() => props.open, (v) => {
  if (v && props.consent) {
    showAdd.value = false
    load()
  }
})

async function load() {
  loading.value = true
  try {
    const res = await privacyConsentApi.listVersions(props.consent.id)
    versions.value = res.data || []
  } catch {
    versions.value = []
  } finally {
    loading.value = false
  }
}

function openAdd() {
  addForm.effectiveDate = ''
  addForm.changeNote = ''
  selectedFile.value = null
  if (fileInput.value) fileInput.value.value = ''
  addError.value = ''
  showAdd.value = true
}

function onFileChange(e) {
  selectedFile.value = e.target.files?.[0] || null
}

async function submitVersion() {
  saving.value = true
  addError.value = ''
  try {
    const data = {
      effectiveDate: addForm.effectiveDate || null,
      changeNote: addForm.changeNote?.trim() || null,
    }
    await privacyConsentApi.addVersion(props.consent.id, data, selectedFile.value)
    showAdd.value = false
    await load()
    emit('changed')
  } catch (e) {
    addError.value = typeof e === 'string' ? e : '버전 등록에 실패했습니다.'
  } finally {
    saving.value = false
  }
}

async function removeVersion(v) {
  if (!confirm(`v${v.version} 버전 이력을 삭제하시겠습니까?\n삭제 시 동의서의 현재 버전은 직전(하위) 버전으로 되돌아갑니다.`)) return
  try {
    await privacyConsentApi.deleteVersion(v.id)
    await load()
    emit('changed')
  } catch (e) {
    alert(typeof e === 'string' ? e : '삭제에 실패했습니다.')
  }
}

function download(v) {
  privacyConsentApi.downloadVersionFile(v.id, v.fileName || 'attachment')
}

function formatSize(bytes) {
  if (!bytes && bytes !== 0) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

function formatDate(dt) {
  if (!dt) return '—'
  return String(dt).slice(0, 10)
}
</script>
