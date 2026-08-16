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

    <!-- 보기 전환 — 장(章) 목록 / 조(條) 검색 -->
    <div class="flex gap-1 mb-5 border-b border-gray-200">
      <button v-for="t in tabs" :key="t.value" @click="tab = t.value"
        :class="['px-4 py-2.5 text-sm font-medium border-b-2 -mb-px transition-colors',
                 tab === t.value
                   ? 'border-primary-600 text-primary-700'
                   : 'border-transparent text-gray-500 hover:text-gray-700']">
        {{ t.label }}
      </button>
    </div>

    <!-- 조문 검색 -->
    <PolicyArticleSearch v-if="tab === 'articles'" @open-policy="openDetailById" />

    <template v-else>
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

    <!-- 선택 삭제 바 (관리자 · 선택된 항목이 있을 때만) -->
    <div v-if="canDelete && selectedIds.length"
      class="mb-3 flex flex-wrap items-center gap-3 px-4 py-3 bg-primary-50 border border-primary-100 rounded-xl">
      <span class="text-sm text-gray-700">
        <b class="text-primary-700">{{ selectedIds.length }}건</b> 선택됨
      </span>
      <button @click="clearSelection" class="text-sm text-gray-500 hover:text-gray-700 underline">선택 해제</button>
      <button @click="showDeleteConfirm = true" :disabled="deleting"
        class="ml-auto flex items-center gap-1.5 px-3 py-1.5 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 transition-colors">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
        </svg>
        {{ deleting ? '삭제 중...' : '선택 삭제' }}
      </button>
    </div>
    <p v-if="deleteError" class="mb-3 px-4 py-3 bg-red-50 border border-red-200 rounded-xl text-sm text-red-700">
      {{ deleteError }}
    </p>

    <!-- Table -->
    <div class="card p-0 overflow-hidden">
      <div v-if="loading" class="p-8 text-center text-gray-400">{{ $t('common.loading') }}</div>
      <div v-else-if="!policies.length" class="p-8 text-center text-gray-400">{{ $t('common.noData') }}</div>
      <div v-else class="overflow-x-auto"><table class="w-full">
        <thead class="bg-gray-50 border-b border-gray-100">
          <tr>
            <th v-if="canDelete" class="w-12 px-6 py-3">
              <input type="checkbox" class="w-4 h-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500 cursor-pointer"
                :checked="allSelected" :indeterminate.prop="someSelected && !allSelected"
                @change="toggleAll" title="현재 페이지 전체 선택" />
            </th>
            <th class="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">지침</th>
            <th class="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">장</th>
            <th class="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">조</th>
            <th class="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">{{ $t('policy.category') }}</th>
            <th class="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">{{ $t('common.status') }}</th>
            <th class="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">{{ $t('policy.version') }}</th>
            <th class="text-left px-6 py-3 text-xs font-medium text-gray-500 uppercase tracking-wider">{{ $t('policy.author') }}</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-50">
          <tr v-for="p in policies" :key="p.id"
            :class="['hover:bg-gray-50 cursor-pointer transition-colors', isSelected(p.id) ? 'bg-primary-50/60' : '']"
            @click="openDetail(p)">
            <td v-if="canDelete" class="px-6 py-4" @click.stop>
              <input type="checkbox" class="w-4 h-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500 cursor-pointer"
                :checked="isSelected(p.id)" @change="toggleOne(p.id)" :title="p.title" />
            </td>
            <td class="px-6 py-4 text-sm font-medium text-gray-900">{{ p.guidelineName || p.title }}</td>
            <td class="px-6 py-4 text-sm text-gray-700">
              <span v-if="p.chapterLabel" class="font-medium">{{ p.chapterLabel }}</span>
              <span v-if="p.chapterTitle" class="text-gray-500 ml-1">{{ p.chapterTitle }}</span>
              <span v-if="!p.chapterLabel && !p.chapterTitle" class="text-gray-300">-</span>
            </td>
            <td class="px-6 py-4">
              <span :class="p.articleCount ? 'badge-blue' : 'badge-gray'">{{ p.articleCount }}개 조</span>
            </td>
            <td class="px-6 py-4"><span class="badge-blue">{{ $t(`policy.category_label.${p.category}`) }}</span></td>
            <td class="px-6 py-4"><span :class="statusBadgeClass(p.status)">{{ $t(`policy.status.${p.status}`) }}</span></td>
            <td class="px-6 py-4 text-sm text-gray-500">v{{ p.version }}</td>
            <td class="px-6 py-4 text-sm text-gray-500">{{ p.authorName }}</td>
          </tr>
        </tbody>
      </table></div>
    </div>

    <!-- Pagination -->
    <div v-if="totalPages > 1" class="flex justify-center gap-2 mt-4">
      <button v-for="n in totalPages" :key="n" @click="page = n - 1; loadPolicies()"
        :class="page === n - 1 ? 'btn-primary' : 'btn-secondary'" class="px-3 py-1 text-sm">{{ n }}</button>
    </div>

    <!-- 선택 삭제 확인 모달 -->
    <div v-if="showDeleteConfirm" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-md">
        <div class="flex items-center justify-between p-5 border-b">
          <h2 class="text-lg font-semibold text-gray-900">선택한 정책 삭제</h2>
          <button @click="showDeleteConfirm = false" class="text-gray-400 hover:text-gray-600">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <div class="p-5 space-y-3 text-sm text-gray-600">
          <p>선택한 <b class="text-gray-900">{{ selectedIds.length }}건</b>의 정책을 삭제합니다. 되돌릴 수 없습니다.</p>
          <ul class="max-h-40 overflow-y-auto px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg space-y-1">
            <li v-for="t in selectedTitles" :key="t" class="text-xs text-gray-600 truncate">· {{ t }}</li>
            <li v-if="selectedIds.length > selectedTitles.length" class="text-xs text-gray-400">
              외 {{ selectedIds.length - selectedTitles.length }}건 (다른 페이지 선택분)
            </li>
          </ul>
          <p class="text-xs text-gray-400">정책에 연결된 열람 확인 기록도 함께 삭제됩니다.</p>
        </div>
        <div class="flex justify-end gap-3 px-5 py-4 border-t bg-gray-50 rounded-b-xl">
          <button @click="showDeleteConfirm = false"
            class="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-100">
            {{ $t('common.cancel') }}
          </button>
          <button @click="deleteSelected" :disabled="deleting"
            class="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 transition-colors">
            {{ deleting ? '삭제 중...' : '삭제' }}
          </button>
        </div>
      </div>
    </div>
    </template>

    <!-- 정책 상세 모달 -->
    <PolicyDetailModal :open="showDetailModal" :item-id="detailId"
      @close="showDetailModal = false" @edit="onDetailEdit" @changed="loadPolicies" />

    <!-- 정책 등록 모달 -->
    <PolicyFormModal :open="showFormModal" :edit-id="editId" @close="showFormModal = false" @saved="onFormSaved" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { policyApi, exportApi, policyBulkApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useDebounceFn } from '@vueuse/core'
import BulkImportModal from '@/components/BulkImportModal.vue'
import PolicyFormModal from './PolicyFormModal.vue'
import PolicyDetailModal from './PolicyDetailModal.vue'
import PolicyArticleSearch from './PolicyArticleSearch.vue'

// 장(章) 목록 ↔ 조(條) 검색 전환
const tabs = [
  { value: 'chapters', label: '장 목록' },
  { value: 'articles', label: '조문 검색' }
]
const tab = ref('chapters')

const auth = useAuthStore()
const isManager = auth.isManager
// 정책 삭제는 ADMIN 전용(백엔드 @PreAuthorize와 동일) — 체크박스도 관리자에게만 보인다.
const canDelete = auth.isAdmin

// ── 다중 선택 삭제 ────────────────────────────────────────────────────
const selectedIds = ref([])
const selectedTitleMap = ref({})   // 다른 페이지에서 고른 항목의 제목도 확인창에 보여주기 위해 보관
const showDeleteConfirm = ref(false)
const deleting = ref(false)
const deleteError = ref('')

const allSelected = computed(() =>
  policies.value.length > 0 && policies.value.every(p => selectedIds.value.includes(p.id)))
const someSelected = computed(() => policies.value.some(p => selectedIds.value.includes(p.id)))
const selectedTitles = computed(() =>
  selectedIds.value.map(id => selectedTitleMap.value[id]).filter(Boolean))

function isSelected(id) { return selectedIds.value.includes(id) }

function toggleOne(id) {
  const idx = selectedIds.value.indexOf(id)
  if (idx === -1) {
    selectedIds.value.push(id)
    const p = policies.value.find(x => x.id === id)
    if (p) selectedTitleMap.value[id] = p.title
  } else {
    selectedIds.value.splice(idx, 1)
  }
}

/** 현재 페이지 전체 선택/해제 (다른 페이지에서 고른 항목은 유지) */
function toggleAll() {
  const pageIds = policies.value.map(p => p.id)
  if (allSelected.value) {
    selectedIds.value = selectedIds.value.filter(id => !pageIds.includes(id))
  } else {
    for (const p of policies.value) {
      if (!selectedIds.value.includes(p.id)) {
        selectedIds.value.push(p.id)
        selectedTitleMap.value[p.id] = p.title
      }
    }
  }
}

function clearSelection() {
  selectedIds.value = []
  selectedTitleMap.value = {}
}

async function deleteSelected() {
  if (!selectedIds.value.length) return
  deleting.value = true
  deleteError.value = ''
  try {
    await policyApi.deleteBulk(selectedIds.value)
    showDeleteConfirm.value = false
    clearSelection()
    await loadPolicies()
    // 마지막 페이지를 통째로 지운 경우 빈 페이지가 남지 않도록 한 페이지 앞으로
    if (!policies.value.length && page.value > 0) {
      page.value--
      await loadPolicies()
    }
  } catch (e) {
    deleteError.value = typeof e === 'string' ? e : '정책 삭제 중 오류가 발생했습니다.'
    showDeleteConfirm.value = false
  } finally {
    deleting.value = false
  }
}

const showFormModal = ref(false)
const editId = ref(null)
function openCreate() { editId.value = null; showFormModal.value = true }
function onFormSaved() { showFormModal.value = false; page.value = 0; loadPolicies() }

const showDetailModal = ref(false)
const detailId = ref(null)
function openDetail(p) { detailId.value = p.id; showDetailModal.value = true }
/** 조문 검색 결과에서 "소속 장 전체 보기"로 넘어올 때 */
function openDetailById(id) { detailId.value = id; showDetailModal.value = true }
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
