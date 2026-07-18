<template>
  <div class="p-6">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('isms.title') }}</h1>
        <p class="text-sm text-gray-500 mt-1">{{ $t('isms.subtitle') }}</p>
      </div>
      <div class="flex items-center gap-3">
        <select v-model="selectedYear" @change="loadItems"
          class="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500">
          <option v-for="y in years" :key="y" :value="y">{{ y }}{{ $t('isms.year') }}</option>
        </select>
        <button @click="downloadCsv" :disabled="csvLoading"
          class="flex items-center gap-1.5 px-3 py-2 text-sm border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 transition-colors">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
          </svg>
          {{ csvLoading ? $t('common.loading') : $t('isms.downloadCsv') }}
        </button>
        <button @click="downloadPdf" :disabled="pdfLoading"
          class="flex items-center gap-1.5 px-3 py-2 text-sm border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50 transition-colors">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
          </svg>
          {{ pdfLoading ? $t('common.loading') : $t('isms.downloadPdf') }}
        </button>
        <button @click="showImportModal = true"
          class="flex items-center gap-1.5 px-3 py-2 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l4-4m0 0l4 4m-4-4v12"/>
          </svg>
          {{ $t('isms.bulkImport') }}
        </button>
      </div>
    </div>

    <!-- Summary Cards -->
    <div v-if="summary" class="grid grid-cols-5 gap-4 mb-6">
      <div class="bg-white rounded-lg border p-4 text-center">
        <p class="text-2xl font-bold text-gray-900">{{ summary.totalItems }}</p>
        <p class="text-xs text-gray-500 mt-1">{{ $t('isms.totalItems') }}</p>
      </div>
      <div class="bg-green-50 rounded-lg border border-green-200 p-4 text-center">
        <p class="text-2xl font-bold text-green-700">{{ summary.compliant }}</p>
        <p class="text-xs text-green-600 mt-1">{{ $t('isms.statusCompliant') }}</p>
      </div>
      <div class="bg-yellow-50 rounded-lg border border-yellow-200 p-4 text-center">
        <p class="text-2xl font-bold text-yellow-700">{{ summary.partial }}</p>
        <p class="text-xs text-yellow-600 mt-1">{{ $t('isms.statusPartial') }}</p>
      </div>
      <div class="bg-red-50 rounded-lg border border-red-200 p-4 text-center">
        <p class="text-2xl font-bold text-red-700">{{ summary.nonCompliant }}</p>
        <p class="text-xs text-red-600 mt-1">{{ $t('isms.statusNonCompliant') }}</p>
      </div>
      <div class="bg-gray-50 rounded-lg border p-4 text-center">
        <p class="text-2xl font-bold text-gray-500">{{ summary.noEvidence }}</p>
        <p class="text-xs text-gray-500 mt-1">{{ $t('isms.noEvidence') }}</p>
      </div>
    </div>

    <!-- Domain Filter Tabs -->
    <div class="bg-white rounded-xl shadow-sm border mb-4">
      <div class="flex flex-wrap border-b overflow-x-auto">
        <button
          :class="['px-4 py-3 text-sm font-medium whitespace-nowrap transition-colors',
            selectedDomain === '' ? 'border-b-2 border-primary-600 text-primary-700' : 'text-gray-500 hover:text-gray-700']"
          @click="selectDomain('')">
          {{ $t('common.all') }}
        </button>
        <button v-for="domain in domains" :key="domain.code"
          :class="['px-4 py-3 text-sm font-medium whitespace-nowrap transition-colors',
            selectedDomain === domain.code ? 'border-b-2 border-primary-600 text-primary-700' : 'text-gray-500 hover:text-gray-700']"
          @click="selectDomain(domain.code)">
          {{ domain.code }} {{ domain.name }}
        </button>
      </div>

      <!-- Items Table -->
      <div v-if="loading" class="py-12 text-center text-gray-500">{{ $t('common.loading') }}</div>
      <table v-else class="w-full">
        <thead class="bg-gray-50">
          <tr>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-24">
              {{ $t('isms.itemCode') }}
            </th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
              {{ $t('isms.itemName') }}
            </th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-40 hidden md:table-cell">
              {{ $t('isms.domain') }}
            </th>
            <th class="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider w-24">
              {{ $t('isms.evidenceCount') }}
            </th>
            <th class="px-6 py-3 text-center text-xs font-medium text-gray-500 uppercase tracking-wider w-32">
              {{ $t('common.status') }}
            </th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100">
          <tr v-if="filteredItems.length === 0">
            <td colspan="5" class="px-6 py-8 text-center text-gray-400 text-sm">{{ $t('common.noData') }}</td>
          </tr>
          <tr v-for="item in filteredItems" :key="item.id"
            class="hover:bg-gray-50 cursor-pointer transition-colors"
            @click="openItem(item)">
            <td class="px-6 py-4 font-mono text-sm font-semibold text-primary-700">{{ item.itemCode }}</td>
            <td class="px-6 py-4">
              <p class="text-sm font-medium text-gray-900">{{ item.itemName }}</p>
            </td>
            <td class="px-6 py-4 text-sm text-gray-500 hidden md:table-cell">
              {{ item.domainName }}
            </td>
            <td class="px-6 py-4 text-center">
              <span class="text-sm text-gray-700">{{ item.evidenceCount ?? 0 }}</span>
            </td>
            <td class="px-6 py-4 text-center">
              <StatusBadge :status="item.latestStatus" />
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>

  <!-- 항목 증적 팝업 -->
  <IsmsEvidenceModal
    :open="evidenceOpen"
    :item-id="evidenceItem?.id"
    :item-code="evidenceItem?.itemCode"
    :item-name="evidenceItem?.itemName"
    :year="selectedYear"
    @close="evidenceOpen = false"
    @changed="loadItems"
    @update:year="onModalYearChange"
  />

  <!-- 일괄 등록 모달 -->
  <div v-if="showImportModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
    <div class="bg-white rounded-xl shadow-xl w-full max-w-lg">
      <div class="flex items-center justify-between p-5 border-b">
        <h2 class="text-lg font-semibold text-gray-900">{{ $t('isms.importTitle') }}</h2>
        <button @click="closeImportModal" class="text-gray-400 hover:text-gray-600">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <div class="p-5 space-y-5">
        <p class="text-sm text-gray-500">{{ $t('isms.importDesc') }}</p>

        <!-- 템플릿 다운로드 -->
        <div class="flex items-center justify-between p-3 bg-blue-50 rounded-lg border border-blue-100">
          <div class="text-sm text-blue-800">
            <p class="font-medium">엑셀 템플릿</p>
            <p class="text-xs text-blue-600 mt-0.5">항목코드 · 증적제목 · 증적내용 · 파일명 · 준수상태</p>
          </div>
          <button @click="downloadTemplate"
            class="text-sm text-blue-700 border border-blue-300 px-3 py-1.5 rounded-lg hover:bg-blue-100 transition-colors">
            {{ $t('isms.downloadTemplate') }}
          </button>
        </div>

        <!-- 연도 선택 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('isms.year') }}</label>
          <select v-model="importYear"
            class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500">
            <option v-for="y in years" :key="y" :value="y">{{ y }}</option>
          </select>
        </div>

        <!-- 파일 선택 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('isms.selectFile') }}</label>
          <input type="file" accept=".xlsx,.csv" @change="onFileChange" ref="fileInputRef"
            class="block w-full text-sm text-gray-500 file:mr-3 file:py-1.5 file:px-3 file:rounded file:border-0 file:text-sm file:font-medium file:bg-primary-50 file:text-primary-700 hover:file:bg-primary-100"/>
          <p v-if="importFile" class="mt-1 text-xs text-gray-500">{{ importFile.name }}</p>
        </div>

        <!-- 결과 -->
        <div v-if="importResult" class="rounded-lg border overflow-hidden">
          <div class="grid grid-cols-3 divide-x bg-gray-50 text-center text-sm font-medium">
            <div class="p-3">
              <p class="text-gray-500 text-xs">{{ $t('isms.importTotal') }}</p>
              <p class="text-xl font-bold text-gray-800 mt-0.5">{{ importResult.total }}</p>
            </div>
            <div class="p-3">
              <p class="text-green-600 text-xs">{{ $t('isms.importSuccess') }}</p>
              <p class="text-xl font-bold text-green-700 mt-0.5">{{ importResult.success }}</p>
            </div>
            <div class="p-3">
              <p class="text-red-500 text-xs">{{ $t('isms.importFailed') }}</p>
              <p class="text-xl font-bold text-red-600 mt-0.5">{{ importResult.failed }}</p>
            </div>
          </div>
          <div v-if="importResult.errors?.length" class="max-h-40 overflow-y-auto border-t">
            <table class="w-full text-xs">
              <thead class="bg-gray-50 sticky top-0">
                <tr>
                  <th class="px-3 py-2 text-left text-gray-500">{{ $t('isms.importErrorRow') }}</th>
                  <th class="px-3 py-2 text-left text-gray-500">항목코드</th>
                  <th class="px-3 py-2 text-left text-gray-500">오류 내용</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-100">
                <tr v-for="err in importResult.errors" :key="err.row" class="text-red-600">
                  <td class="px-3 py-2">{{ err.row }}</td>
                  <td class="px-3 py-2 font-mono">{{ err.itemCode }}</td>
                  <td class="px-3 py-2">{{ err.message }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <p v-if="importError" class="text-sm text-red-600">{{ importError }}</p>
      </div>

      <div class="flex justify-end gap-3 px-5 py-4 border-t bg-gray-50 rounded-b-xl">
        <button @click="closeImportModal"
          class="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-100">
          {{ importResult ? $t('common.confirm') : $t('common.cancel') }}
        </button>
        <button v-if="!importResult" @click="startImport" :disabled="importLoading || !importFile"
          class="px-4 py-2 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 transition-colors">
          {{ importLoading ? $t('common.loading') : $t('isms.startImport') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ismsApi, exportApi } from '@/api'
import StatusBadge from './IsmsStatusBadge.vue'
import IsmsEvidenceModal from './IsmsEvidenceModal.vue'

// 항목 클릭 시 상세 페이지로 이동하지 않고 팝업으로 연다.
const evidenceOpen = ref(false)
const evidenceItem = ref(null)

function openItem(item) {
  evidenceItem.value = item
  evidenceOpen.value = true
}

// 팝업 안에서 연도를 바꾸면 목록의 연도도 맞춰 다시 읽는다.
function onModalYearChange(year) {
  selectedYear.value = year
  loadItems()
}

const selectedYear = ref(new Date().getFullYear())
const selectedDomain = ref('')
const items = ref([])
const summary = ref(null)
const loading = ref(false)

// CSV / PDF 다운로드
const csvLoading = ref(false)
const pdfLoading = ref(false)

// 일괄등록 모달
const showImportModal = ref(false)
const importYear = ref(new Date().getFullYear())
const importFile = ref(null)
const importLoading = ref(false)
const importResult = ref(null)
const importError = ref('')
const fileInputRef = ref(null)

const years = computed(() => {
  const cur = new Date().getFullYear()
  return Array.from({ length: 5 }, (_, i) => cur - i)
})

const domains = computed(() => {
  const map = new Map()
  for (const item of items.value) {
    if (!map.has(item.domainCode)) {
      map.set(item.domainCode, { code: item.domainCode, name: item.domainName })
    }
  }
  return [...map.values()]
})

const filteredItems = computed(() => {
  if (!selectedDomain.value) return items.value
  return items.value.filter(i => i.domainCode === selectedDomain.value)
})

function selectDomain(code) {
  selectedDomain.value = code
}

async function loadItems() {
  loading.value = true
  try {
    const [itemsData, summaryData] = await Promise.all([
      ismsApi.listItems({ year: selectedYear.value }),
      ismsApi.summary(selectedYear.value)
    ])
    items.value = itemsData.data
    summary.value = summaryData.data
  } finally {
    loading.value = false
  }
}

async function downloadCsv() {
  csvLoading.value = true
  try { await ismsApi.exportCsv(selectedYear.value) } finally { csvLoading.value = false }
}

async function downloadPdf() {
  pdfLoading.value = true
  try { await exportApi.ismsPdf(selectedYear.value) } finally { pdfLoading.value = false }
}

async function downloadTemplate() {
  await ismsApi.importTemplate()
}

function onFileChange(e) {
  importFile.value = e.target.files[0] || null
  importResult.value = null
  importError.value = ''
}

async function startImport() {
  if (!importFile.value) return
  importLoading.value = true
  importError.value = ''
  importResult.value = null
  try {
    const res = await ismsApi.bulkImport(importYear.value, importFile.value)
    importResult.value = res.data
    if (res.data.success > 0) await loadItems()
  } catch (e) {
    importError.value = typeof e === 'string' ? e : '등록 중 오류가 발생했습니다.'
  } finally {
    importLoading.value = false
  }
}

function closeImportModal() {
  showImportModal.value = false
  importFile.value = null
  importResult.value = null
  importError.value = ''
  if (fileInputRef.value) fileInputRef.value.value = ''
}

onMounted(loadItems)
</script>
