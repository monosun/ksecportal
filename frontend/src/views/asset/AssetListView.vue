<template>
  <div class="p-8">
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-gray-900">{{ $t('asset.title') }}</h1>
      <div class="flex gap-2">
        <button v-if="isManager" @click="downloadCsv" :disabled="csvLoading"
          class="btn-secondary flex items-center gap-2 text-sm">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
          </svg>
          {{ csvLoading ? '다운로드 중...' : $t('asset.downloadCsv') }}
        </button>
        <button v-if="isManager" @click="downloadPdf" :disabled="pdfLoading"
          class="btn-secondary flex items-center gap-2 text-sm">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
          </svg>
          {{ pdfLoading ? '...' : $t('asset.downloadPdf') }}
        </button>
        <button v-if="isManager" @click="showUploadModal = true"
          class="btn-secondary flex items-center gap-2 text-sm">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l4-4m0 0l4 4m-4-4v12"/>
          </svg>
          엑셀 일괄 등록
        </button>
        <RouterLink v-if="isManager" to="/assets/new" class="btn-primary flex items-center gap-2">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          {{ $t('asset.create') }}
        </RouterLink>
      </div>
    </div>

    <!-- 엑셀 일괄 등록 모달 -->
    <div v-if="showUploadModal" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-lg">
        <div class="flex items-center justify-between px-6 py-4 border-b">
          <h2 class="text-lg font-semibold text-gray-900">엑셀 일괄 등록</h2>
          <button @click="closeUploadModal" class="text-gray-400 hover:text-gray-600">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>

        <div class="px-6 py-5 space-y-4">
          <!-- 템플릿 다운로드 -->
          <div class="bg-blue-50 rounded-lg p-4 flex items-start gap-3">
            <svg class="w-5 h-5 text-blue-600 mt-0.5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
            <div class="text-sm text-blue-800">
              <p class="font-medium mb-1">등록 전 템플릿을 먼저 다운로드하세요.</p>
              <p class="text-blue-600 text-xs">자산명, 유형은 필수 항목입니다. 두 번째 시트(입력 규칙)에서 허용 값을 확인하세요.</p>
            </div>
          </div>

          <button @click="downloadTemplate" :disabled="templateLoading"
            class="w-full btn-secondary flex items-center justify-center gap-2 text-sm">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
            </svg>
            {{ templateLoading ? '다운로드 중...' : '엑셀 템플릿 다운로드 (.xlsx)' }}
          </button>

          <!-- 파일 업로드 -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">엑셀 파일 선택 (.xlsx)</label>
            <input ref="fileInput" type="file" accept=".xlsx,.xls"
              @change="onFileChange" class="block w-full text-sm text-gray-600
              file:mr-3 file:py-2 file:px-4 file:rounded-lg file:border-0
              file:text-sm file:font-medium file:bg-blue-50 file:text-blue-700
              hover:file:bg-blue-100 cursor-pointer border border-gray-200 rounded-lg p-1" />
            <p v-if="selectedFile" class="text-xs text-gray-500 mt-1">
              선택된 파일: {{ selectedFile.name }} ({{ (selectedFile.size / 1024).toFixed(1) }} KB)
            </p>
          </div>

          <!-- 업로드 결과 -->
          <div v-if="uploadResult" class="rounded-lg border text-sm">
            <div class="flex gap-4 p-3 border-b bg-gray-50 rounded-t-lg font-medium">
              <span>총 {{ uploadResult.total }}건</span>
              <span class="text-green-600">성공 {{ uploadResult.success }}건</span>
              <span v-if="uploadResult.failed > 0" class="text-red-600">실패 {{ uploadResult.failed }}건</span>
            </div>
            <ul v-if="uploadResult.errors?.length" class="divide-y max-h-40 overflow-y-auto">
              <li v-for="e in uploadResult.errors" :key="e.row" class="px-3 py-2 text-red-600 text-xs">
                <span class="font-medium">{{ e.row }}행:</span> {{ e.message }}
              </li>
            </ul>
            <p v-else class="px-3 py-2 text-green-600 text-xs font-medium">모든 행이 성공적으로 등록되었습니다.</p>
          </div>

          <div v-if="uploadError" class="text-sm text-red-600 bg-red-50 p-3 rounded-lg">{{ uploadError }}</div>
        </div>

        <div class="px-6 py-4 border-t flex justify-end gap-3">
          <button @click="closeUploadModal" class="btn-secondary text-sm">닫기</button>
          <button @click="uploadExcel" :disabled="!selectedFile || uploading"
            class="btn-primary text-sm flex items-center gap-2">
            <svg v-if="uploading" class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
            </svg>
            {{ uploading ? '등록 중...' : '등록 시작' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Stats chips -->
    <div class="flex flex-wrap gap-3 mb-6">
      <div class="bg-white rounded-lg border px-4 py-2 text-sm">
        <span class="text-gray-500">총 자산</span>
        <span class="ml-2 font-bold text-gray-800">{{ totalElements }}</span>
      </div>
      <div class="bg-red-50 rounded-lg border border-red-200 px-4 py-2 text-sm">
        <span class="text-red-600">고중요도</span>
        <span class="ml-2 font-bold text-red-700">{{ highCount }}</span>
      </div>
      <div class="bg-orange-50 rounded-lg border border-orange-200 px-4 py-2 text-sm">
        <span class="text-orange-600">클라우드 자산</span>
        <span class="ml-2 font-bold text-orange-700">{{ cloudCount }}</span>
      </div>
    </div>

    <!-- Filters -->
    <div class="card mb-4 flex flex-wrap gap-3">
      <input v-model="filters.keyword" @input="debouncedSearch" :placeholder="$t('common.search')" class="input flex-1 min-w-48" />
      <select v-model="filters.type" @change="load" class="input w-44">
        <option value="">{{ $t('asset.type') }}: {{ $t('common.all') }}</option>
        <option v-for="t in assetTypes" :key="t.value" :value="t.value">{{ t.label }}</option>
      </select>
      <select v-model="filters.cloudProvider" @change="load" class="input w-40">
        <option value="">{{ $t('asset.cloudProvider') }}: {{ $t('common.all') }}</option>
        <option v-for="p in cloudProviders" :key="p" :value="p">{{ $t(`asset.cloud_provider_label.${p}`) }}</option>
      </select>
      <select v-model="filters.environment" @change="load" class="input w-36">
        <option value="">{{ $t('asset.environment') }}: {{ $t('common.all') }}</option>
        <option v-for="e in environments" :key="e" :value="e">{{ $t(`asset.environment_label.${e}`) }}</option>
      </select>
      <select v-model="filters.criticality" @change="load" class="input w-36">
        <option value="">{{ $t('asset.criticality') }}: {{ $t('common.all') }}</option>
        <option v-for="c in criticalities" :key="c" :value="c">{{ $t(`asset.criticality_label.${c}`) }}</option>
      </select>
      <select v-model="filters.status" @change="load" class="input w-36">
        <option value="">상태: {{ $t('common.all') }}</option>
        <option value="OPERATIONAL">운영중</option>
        <option value="SUSPENDED">중지</option>
        <option value="DISPOSED">폐기</option>
      </select>
    </div>

    <!-- Table -->
    <div class="card p-0 overflow-hidden">
      <div v-if="loading" class="p-8 text-center text-gray-400">{{ $t('common.loading') }}</div>
      <div v-else-if="!assets.length" class="p-8 text-center text-gray-400">{{ $t('common.noData') }}</div>
      <div v-else class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead class="bg-gray-50 border-b">
            <tr>
              <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">{{ $t('asset.name') }}</th>
              <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">{{ $t('asset.type') }}</th>
              <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">위치 / 리소스</th>
              <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">{{ $t('asset.owner') }}</th>
              <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">{{ $t('asset.criticality') }}</th>
              <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">{{ $t('common.status') }}</th>
              <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">{{ $t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-50">
            <tr v-for="a in assets" :key="a.id" class="hover:bg-gray-50 cursor-pointer" @click="$router.push(`/assets/${a.id}`)">
              <td class="px-5 py-3">
                <p class="font-medium text-gray-900">{{ a.name }}</p>
                <span v-if="a.environment" :class="environmentBadge(a.environment)" class="text-xs mt-0.5 inline-block">
                  {{ $t(`asset.environment_label.${a.environment}`) }}
                </span>
              </td>
              <td class="px-5 py-3">
                <p class="text-gray-700">{{ $t(`asset.type_label.${a.type}`) }}</p>
                <span v-if="a.cloudProvider && a.cloudProvider !== 'ON_PREMISE'" :class="cloudBadge(a.cloudProvider)" class="text-xs mt-0.5 inline-block">
                  {{ $t(`asset.cloud_provider_label.${a.cloudProvider}`) }}
                </span>
              </td>
              <td class="px-5 py-3 text-gray-500 font-mono text-xs">
                <span v-if="a.ipAddress">{{ a.ipAddress }}</span>
                <span v-else-if="a.region">{{ a.region }}</span>
                <span v-else>-</span>
                <p v-if="a.cloudResourceId" class="text-gray-400 truncate max-w-32">{{ a.cloudResourceId }}</p>
              </td>
              <td class="px-5 py-3 text-gray-600">{{ a.owner || '-' }}</td>
              <td class="px-5 py-3"><span :class="criticalityClass(a.criticality)">{{ $t(`asset.criticality_label.${a.criticality}`) }}</span></td>
              <td class="px-5 py-3">
                <span :class="statusBadge(a.status)">{{ statusLabel(a.status) }}</span>
                <p v-if="a.monthlyCost" class="text-xs text-gray-400 mt-0.5">₩{{ Number(a.monthlyCost).toLocaleString() }}</p>
              </td>
              <td class="px-5 py-3" @click.stop>
                <RouterLink v-if="isManager" :to="`/assets/${a.id}/edit`" class="text-blue-600 hover:underline text-xs mr-3">{{ $t('common.edit') }}</RouterLink>
                <button v-if="isAdmin" @click="confirmDelete(a)" class="text-red-500 hover:underline text-xs">{{ $t('common.delete') }}</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="totalPages > 1" class="flex justify-center gap-2 mt-4">
      <button v-for="p in totalPages" :key="p" @click="page = p - 1; load()"
        :class="['px-3 py-1 rounded border text-sm', page === p - 1 ? 'bg-blue-600 text-white border-blue-600' : 'border-gray-300 hover:bg-gray-50']">
        {{ p }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { assetApi, exportApi, assetBulkApi, codeApi } from '@/api'
import { useDebounceFn } from '@vueuse/core'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const isManager = auth.isManager
const isAdmin = auth.isAdmin

const assets = ref([])
const loading = ref(false)
const page = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const highCount = ref(0)
const cloudCount = ref(0)
const filters = ref({ keyword: '', type: '', criticality: '', active: '', status: '', cloudProvider: '', environment: '' })

// CSV / PDF
const csvLoading = ref(false)
const pdfLoading = ref(false)

async function downloadCsv() {
  csvLoading.value = true
  try { await exportApi.assetCsv() } finally { csvLoading.value = false }
}

async function downloadPdf() {
  pdfLoading.value = true
  try { await exportApi.assetPdf() } finally { pdfLoading.value = false }
}

// 엑셀 일괄 등록
const showUploadModal = ref(false)
const selectedFile = ref(null)
const fileInput = ref(null)
const uploading = ref(false)
const uploadResult = ref(null)
const uploadError = ref(null)
const templateLoading = ref(false)

function onFileChange(e) {
  selectedFile.value = e.target.files[0] || null
  uploadResult.value = null
  uploadError.value = null
}

function closeUploadModal() {
  showUploadModal.value = false
  selectedFile.value = null
  uploadResult.value = null
  uploadError.value = null
  if (fileInput.value) fileInput.value.value = ''
}

async function downloadTemplate() {
  templateLoading.value = true
  try { await exportApi.assetTemplate() } finally { templateLoading.value = false }
}

async function uploadExcel() {
  if (!selectedFile.value) return
  uploading.value = true
  uploadResult.value = null
  uploadError.value = null
  try {
    const res = await assetBulkApi.upload(selectedFile.value)
    uploadResult.value = res.data
    if (res.data?.success > 0) load()
  } catch (e) {
    uploadError.value = e || '업로드 중 오류가 발생했습니다.'
  } finally {
    uploading.value = false
  }
}

const assetTypes = ref([]) // { value, label } from code API
const criticalities = ['HIGH', 'MEDIUM', 'LOW']
const cloudProviders = ['AWS', 'GCP', 'AZURE', 'ON_PREMISE', 'OTHER']
const environments = ['PRODUCTION', 'STAGING', 'DEVELOPMENT', 'TEST']

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, size: 20 }
    if (filters.value.keyword) params.keyword = filters.value.keyword
    if (filters.value.type) params.type = filters.value.type
    if (filters.value.criticality) params.criticality = filters.value.criticality
    if (filters.value.active !== '') params.active = filters.value.active
    if (filters.value.status) params.status = filters.value.status
    if (filters.value.cloudProvider) params.cloudProvider = filters.value.cloudProvider
    if (filters.value.environment) params.environment = filters.value.environment
    const res = await assetApi.list(params)
    assets.value = res.data?.content || []
    totalPages.value = res.data?.page?.totalPages ?? res.data?.totalPages ?? 0
    totalElements.value = res.data?.page?.totalElements ?? res.data?.totalElements ?? 0
  } finally {
    loading.value = false
  }
}

async function confirmDelete(asset) {
  if (!confirm(`"${asset.name}" 자산을 삭제하시겠습니까?`)) return
  await assetApi.delete(asset.id)
  load()
}

const debouncedSearch = useDebounceFn(() => { page.value = 0; load() }, 400)

function criticalityClass(c) {
  return { HIGH: 'badge-red', MEDIUM: 'badge-yellow', LOW: 'badge-green' }[c] || 'badge-gray'
}

function statusLabel(v) {
  return { OPERATIONAL: '운영중', SUSPENDED: '중지', DISPOSED: '폐기' }[v] || (v || '-')
}

function statusBadge(v) {
  return { OPERATIONAL: 'badge-success', SUSPENDED: 'badge-yellow', DISPOSED: 'badge-danger' }[v] || 'badge-gray'
}

function environmentBadge(e) {
  return { PRODUCTION: 'badge-red', STAGING: 'badge-yellow', DEVELOPMENT: 'badge-blue', TEST: 'badge-gray' }[e] || 'badge-gray'
}

function cloudBadge(p) {
  return { AWS: 'badge-orange', GCP: 'badge-blue', AZURE: 'badge-blue' }[p] || 'badge-gray'
}

onMounted(async () => {
  try {
    const codeRes = await codeApi.getValues('ASSET_TYPE')
    assetTypes.value = codeRes.data || []
  } catch {
    assetTypes.value = []
  }

  await load()
  try {
    const [highRes, cloudRes] = await Promise.all([
      assetApi.list({ criticality: 'HIGH', active: true, size: 1 }),
      assetApi.list({ cloudProvider: 'AWS', size: 1 })
    ])
    highCount.value = highRes.data?.page?.totalElements ?? highRes.data?.totalElements ?? 0
    cloudCount.value = cloudRes.data?.page?.totalElements ?? cloudRes.data?.totalElements ?? 0
  } catch {}
})
</script>
