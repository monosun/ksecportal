<template>
  <div class="p-8">
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-gray-900">{{ $t('policy.title') }}</h1>
      <div class="flex gap-2">
        <button v-if="isManager" @click="downloadCsv" :disabled="csvLoading"
          class="btn-secondary flex items-center gap-2 text-sm">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
          </svg>
          {{ csvLoading ? '다운로드 중...' : $t('policy.downloadCsv') }}
        </button>
        <button v-if="isManager" @click="downloadPdf" :disabled="pdfLoading"
          class="btn-secondary flex items-center gap-2 text-sm">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
          </svg>
          {{ pdfLoading ? '...' : $t('policy.downloadPdf') }}
        </button>
        <button v-if="isManager" @click="showBulkModal = true"
          class="btn-secondary flex items-center gap-2 text-sm">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l4-4m0 0l4 4m-4-4v12"/>
          </svg>
          {{ $t('policy.bulkImport') }}
        </button>
        <button v-if="isManager" @click="openCreate" class="btn-primary flex items-center gap-2">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          {{ $t('policy.create') }}
        </button>
      </div>
    </div>

    <BulkImportModal
      v-if="showBulkModal"
      ref="bulkModalRef"
      :title="$t('policy.bulkImportTitle')"
      :desc="$t('policy.bulkImportDesc')"
      :template-loading="templateLoading"
      @close="showBulkModal = false; bulkModalRef?.reset()"
      @download-template="downloadTemplate"
      @upload="handleBulkUpload"
    />

    <!-- Filters -->
    <div class="card mb-6 flex flex-wrap gap-4">
      <input v-model="filters.keyword" type="text" :placeholder="$t('common.search')"
        class="input flex-1 min-w-48" @input="debouncedSearch" />
      <select v-model="filters.status" class="input w-40" @change="loadPolicies">
        <option value="">{{ $t('common.all') }}</option>
        <option v-for="s in statuses" :key="s" :value="s">{{ $t(`policy.status.${s}`) }}</option>
      </select>
      <select v-model="filters.category" class="input w-48" @change="loadPolicies">
        <option value="">{{ $t('policy.category') }}: {{ $t('common.all') }}</option>
        <option v-for="c in categories" :key="c" :value="c">{{ $t(`policy.category_label.${c}`) }}</option>
      </select>
    </div>

    <!-- Table -->
    <div class="card p-0 overflow-hidden">
      <div v-if="loading" class="p-8 text-center text-gray-400">{{ $t('common.loading') }}</div>
      <div v-else-if="!policies.length" class="p-8 text-center text-gray-400">{{ $t('common.noData') }}</div>
      <table v-else class="w-full">
        <thead class="bg-gray-50 border-b border-gray-100">
          <tr>
            <th class="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">{{ $t('common.title') }}</th>
            <th class="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">{{ $t('policy.category') }}</th>
            <th class="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">{{ $t('common.status') }}</th>
            <th class="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">{{ $t('policy.version') }}</th>
            <th class="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">{{ $t('policy.author') }}</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-50">
          <tr v-for="p in policies" :key="p.id"
            class="hover:bg-gray-50 cursor-pointer transition-colors"
            @click="openDetail(p)">
            <td class="px-6 py-4 text-sm font-medium text-gray-900">{{ p.title }}</td>
            <td class="px-6 py-4"><span class="badge-blue">{{ $t(`policy.category_label.${p.category}`) }}</span></td>
            <td class="px-6 py-4"><span :class="statusBadgeClass(p.status)">{{ $t(`policy.status.${p.status}`) }}</span></td>
            <td class="px-6 py-4 text-sm text-gray-500">v{{ p.version }}</td>
            <td class="px-6 py-4 text-sm text-gray-500">{{ p.authorName }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="flex justify-center gap-2 mt-4">
      <button v-for="n in totalPages" :key="n" @click="page = n - 1; loadPolicies()"
        :class="page === n - 1 ? 'btn-primary' : 'btn-secondary'" class="px-3 py-1 text-sm">{{ n }}</button>
    </div>

    <!-- 정책 상세 모달 -->
    <PolicyDetailModal :open="showDetailModal" :item-id="detailId"
      @close="showDetailModal = false" @edit="onDetailEdit" @changed="loadPolicies" />

    <!-- 정책 등록 모달 -->
    <PolicyFormModal :open="showFormModal" :edit-id="editId" @close="showFormModal = false" @saved="onFormSaved" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { policyApi, exportApi, policyBulkApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useDebounceFn } from '@vueuse/core'
import BulkImportModal from '@/components/BulkImportModal.vue'
import PolicyFormModal from './PolicyFormModal.vue'
import PolicyDetailModal from './PolicyDetailModal.vue'

const auth = useAuthStore()
const isManager = auth.isManager

const showFormModal = ref(false)
const editId = ref(null)
function openCreate() { editId.value = null; showFormModal.value = true }
function onFormSaved() { showFormModal.value = false; page.value = 0; loadPolicies() }

const showDetailModal = ref(false)
const detailId = ref(null)
function openDetail(p) { detailId.value = p.id; showDetailModal.value = true }
function onDetailEdit(id) { showDetailModal.value = false; editId.value = id; showFormModal.value = true }
const csvLoading = ref(false)
const pdfLoading = ref(false)
const showBulkModal = ref(false)
const templateLoading = ref(false)
const bulkModalRef = ref(null)

async function downloadCsv() {
  csvLoading.value = true
  try { await exportApi.policyCsv() } finally { csvLoading.value = false }
}

async function downloadPdf() {
  pdfLoading.value = true
  try { await exportApi.policyPdf() } finally { pdfLoading.value = false }
}

async function downloadTemplate() {
  templateLoading.value = true
  try { await policyBulkApi.template() } finally { templateLoading.value = false }
}

async function handleBulkUpload(file, resolve, reject) {
  try {
    const res = await policyBulkApi.upload(file)
    resolve(res.data)
    loadPolicies()
  } catch (e) {
    reject(typeof e === 'string' ? e : '업로드 중 오류가 발생했습니다.')
  }
}

const policies = ref([])
const loading = ref(false)
const page = ref(0)
const totalPages = ref(0)
const filters = ref({ keyword: '', status: '', category: '' })

const statuses = ['DRAFT', 'REVIEW', 'PUBLISHED', 'ARCHIVED']
const categories = ['GENERAL', 'ACCESS_CONTROL', 'DATA_PROTECTION', 'INCIDENT_RESPONSE', 'NETWORK', 'PHYSICAL', 'VENDOR', 'OTHER']

async function loadPolicies() {
  loading.value = true
  try {
    const params = { page: page.value, size: 20 }
    if (filters.value.keyword) params.keyword = filters.value.keyword
    if (filters.value.status) params.status = filters.value.status
    if (filters.value.category) params.category = filters.value.category
    const res = await policyApi.list(params)
    policies.value = res.data?.content || []
    totalPages.value = res.data?.page?.totalPages ?? res.data?.totalPages ?? 0
  } finally {
    loading.value = false
  }
}

const debouncedSearch = useDebounceFn(() => { page.value = 0; loadPolicies() }, 400)

function statusBadgeClass(status) {
  return { DRAFT: 'badge-gray', REVIEW: 'badge-yellow', PUBLISHED: 'badge-green', ARCHIVED: 'badge-gray' }[status] || 'badge-gray'
}

onMounted(loadPolicies)
</script>
