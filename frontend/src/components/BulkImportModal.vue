<template>
  <div class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
    <div class="bg-white rounded-xl shadow-xl w-full max-w-lg">
      <div class="flex items-center justify-between px-6 py-4 border-b">
        <h2 class="text-lg font-semibold text-gray-900">{{ title }}</h2>
        <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <div class="px-6 py-5 space-y-4">
        <p class="text-sm text-gray-500">{{ desc }}</p>

        <div class="bg-blue-50 rounded-lg p-4 flex items-start gap-3">
          <svg class="w-5 h-5 text-blue-600 mt-0.5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
          <div class="text-sm text-blue-800">
            <p class="font-medium mb-1">{{ $t('common.loading') === '로딩 중...' ? '등록 전 템플릿을 먼저 다운로드하세요.' : 'Download the template before importing.' }}</p>
          </div>
        </div>

        <button @click="$emit('download-template')" :disabled="templateLoading"
          class="w-full btn-secondary flex items-center justify-center gap-2 text-sm">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
          </svg>
          {{ templateLoading ? $t('common.loading') : templateLabel }}
        </button>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-2">{{ fileLabel }}</label>
          <input ref="fileInput" type="file" accept=".xlsx,.xls"
            @change="onFileChange" class="block w-full text-sm text-gray-600
            file:mr-3 file:py-2 file:px-4 file:rounded-lg file:border-0
            file:text-sm file:font-medium file:bg-blue-50 file:text-blue-700
            hover:file:bg-blue-100 cursor-pointer border border-gray-200 rounded-lg p-1" />
          <p v-if="selectedFile" class="text-xs text-gray-500 mt-1">
            {{ selectedFile.name }} ({{ (selectedFile.size / 1024).toFixed(1) }} KB)
          </p>
        </div>

        <div v-if="result" class="rounded-lg border text-sm">
          <div class="flex gap-4 p-3 border-b bg-gray-50 rounded-t-lg font-medium">
            <span>{{ $t('isms.importTotal') }} {{ result.total }}{{ $t('common.loading') === '로딩 중...' ? '건' : '' }}</span>
            <span class="text-green-600">{{ $t('isms.importSuccess') }} {{ result.success }}{{ $t('common.loading') === '로딩 중...' ? '건' : '' }}</span>
            <span v-if="result.skipped > 0" class="text-amber-600">{{ $t('common.loading') === '로딩 중...' ? '중복 제외' : 'Skipped' }} {{ result.skipped }}{{ $t('common.loading') === '로딩 중...' ? '건' : '' }}</span>
            <span v-if="result.failed > 0" class="text-red-600">{{ $t('isms.importFailed') }} {{ result.failed }}{{ $t('common.loading') === '로딩 중...' ? '건' : '' }}</span>
          </div>
          <ul v-if="result.errors?.length" class="divide-y max-h-40 overflow-y-auto">
            <li v-for="e in result.errors" :key="e.row" class="px-3 py-2 text-red-600 text-xs">
              <span class="font-medium">{{ e.row }}행:</span> {{ e.message }}
            </li>
          </ul>
          <p v-else-if="result.skipped > 0" class="px-3 py-2 text-amber-600 text-xs font-medium">
            {{ $t('common.loading') === '로딩 중...'
              ? `동일한 문제 ${result.skipped}건은 중복으로 제외되었습니다.`
              : `${result.skipped} duplicate row(s) were skipped.` }}
          </p>
          <p v-else class="px-3 py-2 text-green-600 text-xs font-medium">
            {{ $t('common.loading') === '로딩 중...' ? '모든 행이 성공적으로 등록되었습니다.' : 'All rows imported successfully.' }}
          </p>
        </div>

        <div v-if="error" class="text-sm text-red-600 bg-red-50 p-3 rounded-lg">{{ error }}</div>
      </div>

      <div class="px-6 py-4 border-t flex justify-end gap-3">
        <button @click="$emit('close')" class="btn-secondary text-sm">{{ $t('common.cancel') }}</button>
        <button @click="doUpload" :disabled="!selectedFile || uploading || !!result"
          class="btn-primary text-sm flex items-center gap-2">
          <svg v-if="uploading" class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
          </svg>
          {{ uploading ? $t('common.loading') : $t('isms.startImport') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  title: String,
  desc: String,
  templateLabel: { type: String, default: '엑셀 템플릿 다운로드 (.xlsx)' },
  fileLabel: { type: String, default: '엑셀 파일 선택 (.xlsx)' },
  templateLoading: { type: Boolean, default: false }
})

const emit = defineEmits(['close', 'download-template', 'upload'])

const fileInput = ref(null)
const selectedFile = ref(null)
const uploading = ref(false)
const result = ref(null)
const error = ref(null)

function onFileChange(e) {
  selectedFile.value = e.target.files[0] || null
  result.value = null
  error.value = null
}

async function doUpload() {
  if (!selectedFile.value) return
  uploading.value = true
  result.value = null
  error.value = null
  try {
    const res = await new Promise((resolve, reject) => {
      emit('upload', selectedFile.value, resolve, reject)
    })
    result.value = res
  } catch (e) {
    error.value = typeof e === 'string' ? e : '업로드 중 오류가 발생했습니다.'
  } finally {
    uploading.value = false
  }
}

function reset() {
  selectedFile.value = null
  result.value = null
  error.value = null
  if (fileInput.value) fileInput.value.value = ''
}

defineExpose({ reset })
</script>
