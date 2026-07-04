<template>
  <div class="p-8">
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-gray-900">{{ $t('incident.title') }}</h1>
      <div class="flex gap-2">
        <button v-if="isManager" @click="exportCsv" :disabled="csvLoading" class="btn-secondary text-sm flex items-center gap-1.5">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/></svg>
          {{ csvLoading ? '...' : $t('incident.downloadCsv') }}
        </button>
        <button v-if="isManager" @click="downloadPdf" :disabled="pdfLoading" class="btn-secondary text-sm flex items-center gap-1.5">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
          {{ pdfLoading ? '...' : $t('incident.downloadPdf') }}
        </button>
        <button v-if="isManager" @click="showBulkModal = true" class="btn-secondary text-sm flex items-center gap-1.5">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l4-4m0 0l4 4m-4-4v12"/></svg>
          {{ $t('incident.bulkImport') }}
        </button>
        <RouterLink v-if="isManager" to="/incidents/new" class="btn-primary flex items-center gap-2">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/></svg>
          {{ $t('incident.create') }}
        </RouterLink>
      </div>
    </div>

    <BulkImportModal
      v-if="showBulkModal"
      ref="bulkModalRef"
      :title="$t('incident.bulkImportTitle')"
      :desc="$t('incident.bulkImportDesc')"
      :template-loading="templateLoading"
      @close="showBulkModal = false; bulkModalRef?.reset()"
      @download-template="downloadTemplate"
      @upload="handleBulkUpload"
    />

    <!-- Status summary chips -->
    <div class="flex flex-wrap gap-2 mb-5">
      <span v-for="s in statusSummary" :key="s.key"
        class="px-3 py-1 rounded-full text-xs font-medium cursor-pointer border transition-colors"
        :class="filters.status === s.key ? s.activeClass : 'bg-white border-gray-200 text-gray-600 hover:bg-gray-50'"
        @click="toggleStatus(s.key)">
        {{ s.label }}: {{ s.count }}
      </span>
    </div>

    <!-- Filters -->
    <div class="card mb-4 flex flex-wrap gap-3">
      <input v-model="filters.keyword" @input="debouncedSearch" :placeholder="$t('common.search')" class="input flex-1 min-w-48" />
      <select v-model="filters.severity" @change="load" class="input w-36">
        <option value="">{{ $t('incident.severity') }}: {{ $t('common.all') }}</option>
        <option v-for="s in severities" :key="s" :value="s">{{ $t(`incident.severity_label.${s}`) }}</option>
      </select>
      <select v-model="filters.type" @change="load" class="input w-44">
        <option value="">{{ $t('incident.type') }}: {{ $t('common.all') }}</option>
        <option v-for="t in types" :key="t" :value="t">{{ $t(`incident.type_label.${t}`) }}</option>
      </select>
    </div>

    <div class="card p-0 overflow-hidden">
      <div v-if="loading" class="p-8 text-center text-gray-400">{{ $t('common.loading') }}</div>
      <div v-else-if="!incidents.length" class="p-8 text-center text-gray-400">{{ $t('common.noData') }}</div>
      <table v-else class="w-full text-sm">
        <thead class="bg-gray-50 border-b">
          <tr>
            <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">{{ $t('common.title') }}</th>
            <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">{{ $t('incident.type') }}</th>
            <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">{{ $t('incident.severity') }}</th>
            <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">{{ $t('common.status') }}</th>
            <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">{{ $t('incident.assignee') }}</th>
            <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">{{ $t('incident.detectedAt') }}</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-50">
          <tr v-for="i in incidents" :key="i.id"
            class="hover:bg-gray-50 cursor-pointer transition-colors"
            @click="$router.push(`/incidents/${i.id}`)">
            <td class="px-5 py-3 font-medium text-gray-900">{{ i.title }}</td>
            <td class="px-5 py-3 text-gray-600">{{ $t(`incident.type_label.${i.type}`) }}</td>
            <td class="px-5 py-3"><span :class="severityClass(i.severity)">{{ $t(`incident.severity_label.${i.severity}`) }}</span></td>
            <td class="px-5 py-3"><span :class="statusClass(i.status)">{{ $t(`incident.status_label.${i.status}`) }}</span></td>
            <td class="px-5 py-3 text-gray-500">{{ i.assigneeName || '-' }}</td>
            <td class="px-5 py-3 text-gray-400 text-xs">{{ formatDt(i.detectedAt) }}</td>
          </tr>
        </tbody>
      </table>
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
import { ref, computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { incidentApi, exportApi, incidentBulkApi } from '@/api'
import { useDebounceFn } from '@vueuse/core'
import { useAuthStore } from '@/stores/auth'
import BulkImportModal from '@/components/BulkImportModal.vue'

const auth = useAuthStore()
const isManager = auth.isManager
const csvLoading = ref(false)
const pdfLoading = ref(false)
const showBulkModal = ref(false)
const templateLoading = ref(false)
const bulkModalRef = ref(null)

const incidents = ref([])
const loading = ref(false)
const page = ref(0)
const totalPages = ref(0)
const filters = ref({ keyword: '', severity: '', status: '', type: '' })

const severities = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW']
const types = ['MALWARE', 'PHISHING', 'DATA_BREACH', 'UNAUTHORIZED_ACCESS', 'DDOS', 'INSIDER_THREAT', 'PHYSICAL', 'OTHER']

const statusSummary = [
  { key: 'OPEN',          label: '미처리',  count: ref(0), activeClass: 'bg-red-100 border-red-400 text-red-700' },
  { key: 'INVESTIGATING', label: '조사중',  count: ref(0), activeClass: 'bg-orange-100 border-orange-400 text-orange-700' },
  { key: 'CONTAINED',     label: '격리됨',  count: ref(0), activeClass: 'bg-yellow-100 border-yellow-400 text-yellow-700' },
  { key: 'RESOLVED',      label: '해결됨',  count: ref(0), activeClass: 'bg-green-100 border-green-400 text-green-700' },
  { key: 'CLOSED',        label: '종료',    count: ref(0), activeClass: 'bg-gray-100 border-gray-400 text-gray-700' },
]

function toggleStatus(key) {
  filters.value.status = filters.value.status === key ? '' : key
  page.value = 0
  load()
}

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, size: 20 }
    if (filters.value.keyword) params.keyword = filters.value.keyword
    if (filters.value.severity) params.severity = filters.value.severity
    if (filters.value.status) params.status = filters.value.status
    if (filters.value.type) params.type = filters.value.type
    const res = await incidentApi.list(params)
    incidents.value = res.data?.content || []
    totalPages.value = res.data?.page?.totalPages ?? res.data?.totalPages ?? 0
  } finally {
    loading.value = false
  }
}

async function loadSummary() {
  for (const s of statusSummary) {
    try {
      const res = await incidentApi.list({ status: s.key, size: 1 })
      s.count = res.data?.totalElements || 0
    } catch {}
  }
}

async function exportCsv() {
  csvLoading.value = true
  try { await exportApi.incidentCsv() } finally { csvLoading.value = false }
}

async function downloadPdf() {
  pdfLoading.value = true
  try { await exportApi.incidentPdf() } finally { pdfLoading.value = false }
}

async function downloadTemplate() {
  templateLoading.value = true
  try { await incidentBulkApi.template() } finally { templateLoading.value = false }
}

async function handleBulkUpload(file, resolve, reject) {
  try {
    const res = await incidentBulkApi.upload(file)
    resolve(res.data)
    load()
  } catch (e) {
    reject(typeof e === 'string' ? e : '업로드 중 오류가 발생했습니다.')
  }
}

const debouncedSearch = useDebounceFn(() => { page.value = 0; load() }, 400)

const formatDt = (dt) => dt ? new Date(dt).toLocaleString() : '-'

function severityClass(s) { return { CRITICAL: 'badge-red', HIGH: 'badge-orange', MEDIUM: 'badge-yellow', LOW: 'badge-green' }[s] || 'badge-gray' }
function statusClass(s) { return { OPEN: 'badge-red', INVESTIGATING: 'badge-orange', CONTAINED: 'badge-yellow', RESOLVED: 'badge-green', CLOSED: 'badge-gray' }[s] || 'badge-gray' }

onMounted(() => { load(); loadSummary() })
</script>
