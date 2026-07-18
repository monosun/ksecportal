<template>
  <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
    <div class="absolute inset-0 bg-black/40" @click="$emit('close')"></div>
    <div class="relative bg-white rounded-xl shadow-xl w-full max-w-3xl max-h-[92vh] flex flex-col">
      <!-- 헤더 -->
      <div class="flex items-center justify-between px-5 py-3 border-b shrink-0">
        <div>
          <h2 class="text-lg font-semibold text-gray-900">첨부파일</h2>
          <p class="text-sm text-gray-500 mt-0.5">
            {{ dpia?.title }}
            <span class="text-gray-400">— 영향평가 결과보고서·개선이행 확인서 등</span>
          </p>
        </div>
        <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <div class="px-5 py-4 overflow-y-auto flex-1">
        <!-- 파일 추가 -->
        <div v-if="canWrite" class="mb-5">
          <button v-if="!showAdd" @click="openAdd" class="btn-secondary text-sm flex items-center gap-1.5">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
            </svg>
            파일 추가
          </button>

          <form v-else @submit.prevent="submitFile" class="bg-gray-50 border border-gray-100 rounded-lg p-4">
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">구분</label>
                <select v-model="addForm.category" class="input w-full">
                  <option v-for="c in CATEGORY_OPTIONS" :key="c.value" :value="c.value">{{ c.label }}</option>
                </select>
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">문서 제목</label>
                <input v-model="addForm.title" type="text" class="input w-full"
                  placeholder="미입력 시 파일명이 사용됩니다" />
              </div>
              <div class="col-span-2">
                <label class="block text-sm font-medium text-gray-700 mb-1">
                  파일 <span class="text-red-500">*</span>
                </label>
                <input ref="fileInput" type="file" required @change="onFileChange"
                  class="block w-full text-sm text-gray-600 file:mr-3 file:py-1.5 file:px-3 file:rounded file:border-0 file:text-sm file:bg-gray-200 file:text-gray-700 hover:file:bg-gray-300" />
                <p class="text-xs text-gray-400 mt-1">PDF·이미지·문서(doc/xls/ppt)·zip 파일</p>
              </div>
            </div>
            <p v-if="addError" class="text-red-600 text-sm mt-2">{{ addError }}</p>
            <div class="flex justify-end gap-2 mt-3">
              <button type="button" @click="showAdd = false" class="btn-secondary text-sm">취소</button>
              <button type="submit" class="btn-primary text-sm" :disabled="saving">
                {{ saving ? '업로드 중...' : '첨부' }}
              </button>
            </div>
          </form>
        </div>

        <!-- 파일 목록 -->
        <div v-if="loading" class="py-8 text-center text-gray-400">로딩 중...</div>
        <div v-else-if="!files.length" class="py-8 text-center text-gray-400">첨부된 파일이 없습니다.</div>
        <ul v-else class="space-y-2">
          <li v-for="f in files" :key="f.id"
            class="flex items-center justify-between gap-3 border border-gray-100 rounded-lg px-4 py-3">
            <div class="min-w-0">
              <div class="flex items-center gap-2">
                <span :class="categoryBadge(f.category)">{{ categoryLabel(f.category) }}</span>
                <button @click="download(f)"
                  class="inline-flex items-center gap-1.5 text-sm font-medium text-primary-600 hover:text-primary-700 truncate">
                  <svg class="w-4 h-4 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M12 10v6m0 0l-3-3m3 3l3-3m2 6H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                  </svg>
                  <span class="truncate">{{ f.title || f.fileName }}</span>
                </button>
              </div>
              <p class="text-xs text-gray-400 mt-1 truncate">
                {{ f.fileName }} · {{ formatSize(f.fileSize) }} · {{ f.uploader || '—' }} · {{ formatDate(f.createdAt) }}
              </p>
            </div>
            <button v-if="canWrite" @click="removeFile(f)"
              class="text-xs text-red-500 hover:text-red-600 px-1.5 py-0.5 shrink-0">삭제</button>
          </li>
        </ul>
      </div>

      <div class="flex justify-end px-5 py-3 border-t shrink-0">
        <button @click="$emit('close')" class="btn-secondary text-sm">닫기</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, computed } from 'vue'
import { privacyDpiaApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const CATEGORY_OPTIONS = [
  { value: 'REPORT', label: '결과보고서' },
  { value: 'IMPROVEMENT', label: '개선이행' },
  { value: 'CHECKLIST', label: '체크리스트' },
  { value: 'OTHER', label: '기타' },
]
const CATEGORY_BADGES = {
  REPORT: 'badge-blue',
  IMPROVEMENT: 'badge-yellow',
  CHECKLIST: 'badge-green',
  OTHER: 'badge-gray',
}

const props = defineProps({
  open: { type: Boolean, default: false },
  dpia: { type: Object, default: null },
})
const emit = defineEmits(['close', 'changed'])

const auth = useAuthStore()
const canWrite = computed(() => auth.canWrite('privacy_dpia'))

const files = ref([])
const loading = ref(false)
const saving = ref(false)
const showAdd = ref(false)
const addError = ref('')
const fileInput = ref(null)
const selectedFile = ref(null)
const addForm = reactive({ category: 'REPORT', title: '' })

watch(() => props.open, (v) => {
  if (v && props.dpia) {
    showAdd.value = false
    load()
  }
})

async function load() {
  loading.value = true
  try {
    const res = await privacyDpiaApi.listFiles(props.dpia.id)
    files.value = res.data || []
  } catch {
    files.value = []
  } finally {
    loading.value = false
  }
}

function openAdd() {
  addForm.category = 'REPORT'
  addForm.title = ''
  selectedFile.value = null
  if (fileInput.value) fileInput.value.value = ''
  addError.value = ''
  showAdd.value = true
}

function onFileChange(e) {
  selectedFile.value = e.target.files?.[0] || null
}

async function submitFile() {
  if (!selectedFile.value) {
    addError.value = '첨부할 파일을 선택하세요.'
    return
  }
  saving.value = true
  addError.value = ''
  try {
    const data = {
      category: addForm.category,
      title: addForm.title?.trim() || null,
    }
    await privacyDpiaApi.addFile(props.dpia.id, data, selectedFile.value)
    showAdd.value = false
    await load()
    emit('changed')
  } catch (e) {
    addError.value = typeof e === 'string' ? e : '첨부에 실패했습니다.'
  } finally {
    saving.value = false
  }
}

async function removeFile(f) {
  if (!confirm(`"${f.title || f.fileName}" 첨부파일을 삭제하시겠습니까?`)) return
  try {
    await privacyDpiaApi.deleteFile(f.id)
    await load()
    emit('changed')
  } catch (e) {
    alert(typeof e === 'string' ? e : '삭제에 실패했습니다.')
  }
}

function download(f) {
  privacyDpiaApi.downloadFile(f.id, f.fileName || 'attachment')
}

function categoryLabel(c) {
  return CATEGORY_OPTIONS.find(o => o.value === c)?.label || '기타'
}

function categoryBadge(c) {
  return (CATEGORY_BADGES[c] || 'badge-gray') + ' text-[11px] shrink-0'
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
