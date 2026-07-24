<template>
  <div v-if="open" class="fixed inset-0 bg-black/60 flex items-center justify-center z-50 p-4" @click.self="close">
    <div class="bg-white rounded-xl shadow-xl w-full max-w-5xl h-[88vh] flex flex-col">
      <div class="flex items-center justify-between gap-4 px-5 py-3 border-b shrink-0">
        <div class="min-w-0">
          <h2 class="text-base font-semibold text-gray-900 truncate">{{ fileName || '파일 미리보기' }}</h2>
          <p v-if="title" class="text-xs text-gray-400 truncate">{{ title }}</p>
        </div>
        <div class="flex items-center gap-2 shrink-0">
          <button @click="$emit('download')" class="btn-secondary text-sm flex items-center gap-1.5">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
            </svg>
            다운로드
          </button>
          <button @click="close" class="text-gray-400 hover:text-gray-600 p-1">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
      </div>

      <!-- 엑셀 시트 탭 -->
      <div v-if="kind === 'sheet' && sheetNames.length > 1" class="flex gap-1 px-5 pt-2 overflow-x-auto shrink-0">
        <button v-for="name in sheetNames" :key="name" @click="selectSheet(name)"
          :class="['px-3 py-1 text-xs rounded-t-lg border-b-2 whitespace-nowrap transition-colors',
            name === activeSheet ? 'border-primary-600 text-primary-700 font-medium' : 'border-transparent text-gray-500 hover:text-gray-700']">
          {{ name }}
        </button>
      </div>

      <div class="flex-1 min-h-0 overflow-auto bg-gray-50">
        <div v-if="loading" class="h-full flex items-center justify-center text-gray-400 text-sm">불러오는 중...</div>
        <div v-else-if="error" class="h-full flex flex-col items-center justify-center gap-2 text-center px-6">
          <p class="text-sm text-gray-600">{{ error }}</p>
          <button @click="$emit('download')" class="btn-secondary text-sm mt-1">파일 다운로드</button>
        </div>

        <!-- PDF -->
        <iframe v-else-if="kind === 'pdf'" :src="blobUrl" class="w-full h-full border-0" title="PDF 미리보기"></iframe>

        <!-- 이미지 -->
        <div v-else-if="kind === 'image'" class="min-h-full flex items-center justify-center p-4">
          <img :src="blobUrl" :alt="fileName" class="max-w-full max-h-full object-contain" />
        </div>

        <!-- 엑셀·CSV -->
        <div v-else-if="kind === 'sheet'" class="p-4">
          <table class="min-w-full text-xs bg-white border border-gray-200">
            <tbody>
              <tr v-for="(row, ri) in sheetRows" :key="ri" :class="ri === 0 ? 'bg-gray-50 font-semibold' : ''">
                <td v-for="(cell, ci) in row" :key="ci"
                  class="border border-gray-100 px-2 py-1 align-top whitespace-pre-wrap">{{ cell }}</td>
              </tr>
            </tbody>
          </table>
          <p v-if="truncated" class="text-xs text-gray-400 mt-2">
            첫 {{ MAX_ROWS }}행만 표시했습니다. 전체 내용은 다운로드해 확인하세요.
          </p>
        </div>

        <!-- 텍스트 -->
        <pre v-else-if="kind === 'text'"
          class="p-4 text-xs text-gray-700 whitespace-pre-wrap break-words">{{ textContent }}</pre>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import * as XLSX from 'xlsx'

const props = defineProps({
  open: { type: Boolean, default: false },
  fileName: { type: String, default: '' },
  title: { type: String, default: '' },
  /** Blob 을 돌려주는 로더 — 화면마다 다른 API 를 쓰므로 주입받는다 */
  loader: { type: Function, default: null },
})
const emit = defineEmits(['close', 'download'])

const MAX_ROWS = 300

const loading = ref(false)
const error = ref('')
const kind = ref('')          // pdf | image | sheet | text
const blobUrl = ref('')
const textContent = ref('')
const workbook = ref(null)
const sheetNames = ref([])
const activeSheet = ref('')
const sheetRows = ref([])
const truncated = ref(false)

/** 확장자로 미리보기 방식을 정한다 (서버 허용 확장자 기준) */
function detectKind(name) {
  const ext = (name || '').split('.').pop()?.toLowerCase() || ''
  if (ext === 'pdf') return 'pdf'
  if (['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext)) return 'image'
  if (['xlsx', 'xls', 'csv'].includes(ext)) return 'sheet'
  if (ext === 'txt') return 'text'
  return ''
}

function revoke() {
  if (blobUrl.value) {
    URL.revokeObjectURL(blobUrl.value)
    blobUrl.value = ''
  }
}

function reset() {
  revoke()
  error.value = ''
  textContent.value = ''
  workbook.value = null
  sheetNames.value = []
  activeSheet.value = ''
  sheetRows.value = []
  truncated.value = false
}

function close() {
  emit('close')
}

function selectSheet(name) {
  activeSheet.value = name
  const sheet = workbook.value?.Sheets?.[name]
  if (!sheet) return
  const rows = XLSX.utils.sheet_to_json(sheet, { header: 1, raw: false, defval: '' })
  truncated.value = rows.length > MAX_ROWS
  sheetRows.value = rows.slice(0, MAX_ROWS)
}

watch(() => [props.open, props.fileName], async ([open]) => {
  if (!open) { reset(); return }
  reset()
  kind.value = detectKind(props.fileName)
  if (!kind.value) {
    error.value = '이 형식은 미리보기를 지원하지 않습니다. 다운로드해서 확인해 주세요.'
    return
  }
  if (!props.loader) {
    error.value = '파일을 불러올 수 없습니다.'
    return
  }

  loading.value = true
  try {
    const blob = await props.loader()
    if (kind.value === 'pdf' || kind.value === 'image') {
      // 서버가 octet-stream 으로 내려주므로 미리보기용 MIME 을 붙여 준다
      const type = kind.value === 'pdf' ? 'application/pdf' : `image/${guessImageType()}`
      blobUrl.value = URL.createObjectURL(new Blob([blob], { type }))
    } else if (kind.value === 'sheet') {
      const buf = await blob.arrayBuffer()
      workbook.value = XLSX.read(buf, { type: 'array' })
      sheetNames.value = workbook.value.SheetNames || []
      if (!sheetNames.value.length) {
        error.value = '표시할 시트가 없습니다.'
      } else {
        selectSheet(sheetNames.value[0])
      }
    } else {
      textContent.value = await blob.text()
    }
  } catch (e) {
    error.value = typeof e === 'string' ? e : '파일을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}, { immediate: true })

function guessImageType() {
  const ext = (props.fileName || '').split('.').pop()?.toLowerCase()
  return ext === 'jpg' ? 'jpeg' : (ext || 'png')
}

onBeforeUnmount(revoke)
</script>
