<template>
  <div class="p-6">
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('secFinding.title') }}</h1>
        <p class="text-sm text-gray-500 mt-1">{{ $t('secFinding.subtitle') }}</p>
      </div>
      <button v-if="isManager" @click="openCreate" class="btn-primary flex items-center gap-2">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
        </svg>
        {{ $t('secFinding.addFinding') }}
      </button>
    </div>

    <!-- Filters -->
    <div class="card mb-4 flex flex-wrap gap-3 items-end">
      <div class="flex flex-col gap-1">
        <label class="text-xs text-gray-500">{{ $t('secFinding.year') }}</label>
        <select v-model="filters.year" @change="fetchFindings" class="input w-28 text-sm">
          <option value="">{{ $t('common.all') }}</option>
          <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}년</option>
        </select>
      </div>
      <div class="flex flex-col gap-1">
        <label class="text-xs text-gray-500">{{ $t('secFinding.auditType') }}</label>
        <select v-model="filters.auditType" @change="fetchFindings" class="input w-44 text-sm">
          <option value="">{{ $t('common.all') }}</option>
          <option v-for="t in AUDIT_TYPES" :key="t" :value="t">{{ $t(`secFinding.auditType_label.${t}`) }}</option>
        </select>
      </div>
      <div class="flex flex-col gap-1">
        <label class="text-xs text-gray-500">{{ $t('secFinding.riskLevel') }}</label>
        <select v-model="filters.riskLevel" @change="fetchFindings" class="input w-28 text-sm">
          <option value="">{{ $t('common.all') }}</option>
          <option v-for="r in RISK_LEVELS" :key="r" :value="r">{{ $t(`secFinding.riskLevel_label.${r}`) }}</option>
        </select>
      </div>
      <div class="flex flex-col gap-1">
        <label class="text-xs text-gray-500">{{ $t('secFinding.status') }}</label>
        <select v-model="filters.status" @change="fetchFindings" class="input w-32 text-sm">
          <option value="">{{ $t('common.all') }}</option>
          <option v-for="s in STATUSES" :key="s" :value="s">{{ $t(`secFinding.status_label.${s}`) }}</option>
        </select>
      </div>
      <div class="flex flex-col gap-1 flex-1 min-w-40">
        <label class="text-xs text-gray-500">{{ $t('common.search') }}</label>
        <input v-model="filters.keyword" @input="debouncedFetch" class="input text-sm"
          placeholder="결함 요약, 인증기준 코드 검색" />
      </div>
      <button @click="resetFilters" class="btn-secondary text-sm self-end">{{ $t('common.all') }}</button>
    </div>

    <!-- Summary badges -->
    <div class="flex gap-3 mb-4 flex-wrap text-sm">
      <div v-for="s in STATUSES" :key="s"
        class="px-3 py-1 rounded-full border cursor-pointer transition-colors"
        :class="filters.status === s ? 'border-primary-400 bg-primary-50 text-primary-700 font-medium' : 'border-gray-200 text-gray-500'"
        @click="toggleStatus(s)">
        {{ $t(`secFinding.status_label.${s}`) }}
        <span class="ml-1 font-semibold">{{ statusCounts[s] || 0 }}</span>
      </div>
    </div>

    <!-- Table -->
    <div class="card">
      <div v-if="loading" class="text-center py-12 text-gray-400">{{ $t('common.loading') }}</div>
      <div v-else-if="findings.length === 0" class="text-center py-12 text-gray-400">{{ $t('secFinding.noFindings') }}</div>
      <template v-else>
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b">
              <th class="text-left py-3 px-3 font-semibold text-gray-600 w-16">연도</th>
              <th class="text-left py-3 px-3 font-semibold text-gray-600 w-28">{{ $t('secFinding.auditType') }}</th>
              <th class="text-left py-3 px-3 font-semibold text-gray-600 w-24">인증기준</th>
              <th class="text-left py-3 px-3 font-semibold text-gray-600">{{ $t('secFinding.findingSummary') }}</th>
              <th class="text-left py-3 px-3 font-semibold text-gray-600 w-20">위험도</th>
              <th class="text-left py-3 px-3 font-semibold text-gray-600 w-24">조치기한</th>
              <th class="text-left py-3 px-3 font-semibold text-gray-600 w-24">상태</th>
              <th class="py-3 px-3 w-16"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="f in findings" :key="f.id" class="border-b hover:bg-gray-50 cursor-pointer"
              @click="openDetail(f)">
              <td class="py-3 px-3 text-gray-500">{{ f.year }}</td>
              <td class="py-3 px-3">
                <span class="text-xs bg-purple-100 text-purple-700 px-2 py-0.5 rounded">
                  {{ $t(`secFinding.auditType_label.${f.auditType}`) }}
                </span>
              </td>
              <td class="py-3 px-3">
                <div v-if="f.requirementCode" class="font-mono text-xs font-medium text-gray-700">{{ f.requirementCode }}</div>
                <div v-if="f.requirementName" class="text-xs text-gray-400 truncate max-w-[120px]">{{ f.requirementName }}</div>
              </td>
              <td class="py-3 px-3">
                <p class="font-medium text-gray-900">{{ f.findingSummary }}</p>
                <p v-if="f.domain" class="text-xs text-gray-400 mt-0.5">{{ f.domain }}</p>
              </td>
              <td class="py-3 px-3">
                <span :class="riskBadge(f.riskLevel)" class="text-xs px-2 py-0.5 rounded font-medium">
                  {{ $t(`secFinding.riskLevel_label.${f.riskLevel}`) }}
                </span>
              </td>
              <td class="py-3 px-3 text-xs text-gray-500">{{ f.actionDeadline ? formatDate(f.actionDeadline) : '-' }}</td>
              <td class="py-3 px-3">
                <span :class="statusBadge(f.status)" class="text-xs px-2 py-0.5 rounded font-medium">
                  {{ $t(`secFinding.status_label.${f.status}`) }}
                </span>
              </td>
              <td class="py-3 px-3 text-right" @click.stop>
                <div v-if="isManager" class="flex gap-1 justify-end">
                  <button @click="openEdit(f)" class="p-1 rounded hover:bg-gray-100 text-gray-400 hover:text-gray-700">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/></svg>
                  </button>
                  <button @click="confirmDelete(f)" class="p-1 rounded hover:bg-red-50 text-gray-400 hover:text-red-500">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/></svg>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- Pagination -->
        <div v-if="totalPages > 1" class="flex justify-center gap-2 py-4">
          <button v-for="p in totalPages" :key="p" @click="goPage(p - 1)"
            :class="['w-8 h-8 rounded text-sm', currentPage === p - 1 ? 'bg-primary-600 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200']">
            {{ p }}
          </button>
        </div>
      </template>
    </div>

    <!-- Detail Modal -->
    <div v-if="showDetail && detailItem" class="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-2xl max-h-[90vh] flex flex-col">
        <div class="px-6 py-4 border-b flex items-start justify-between">
          <div>
            <div class="flex gap-2 mb-1">
              <span :class="riskBadge(detailItem.riskLevel)" class="text-xs px-2 py-0.5 rounded font-medium">{{ $t(`secFinding.riskLevel_label.${detailItem.riskLevel}`) }}</span>
              <span :class="statusBadge(detailItem.status)" class="text-xs px-2 py-0.5 rounded font-medium">{{ $t(`secFinding.status_label.${detailItem.status}`) }}</span>
            </div>
            <h2 class="text-lg font-semibold">{{ detailItem.findingSummary }}</h2>
          </div>
          <button @click="showDetail = false" class="text-gray-400 hover:text-gray-600 p-1 flex-shrink-0 ml-4">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg>
          </button>
        </div>
        <div class="overflow-y-auto flex-1 px-6 py-5 space-y-4 text-sm">
          <div class="grid grid-cols-2 gap-3">
            <div><span class="text-gray-500">연도</span><p class="font-medium mt-0.5">{{ detailItem.year }}</p></div>
            <div><span class="text-gray-500">심사 유형</span><p class="font-medium mt-0.5">{{ $t(`secFinding.auditType_label.${detailItem.auditType}`) }}</p></div>
            <div><span class="text-gray-500">도메인</span><p class="font-medium mt-0.5">{{ detailItem.domain || '-' }}</p></div>
            <div><span class="text-gray-500">인증기준</span>
              <p class="font-mono font-medium mt-0.5">{{ detailItem.requirementCode || '-' }}</p>
              <p v-if="detailItem.requirementName" class="text-xs text-gray-500">{{ detailItem.requirementName }}</p></div>
            <div><span class="text-gray-500">조치 기한</span><p class="font-medium mt-0.5">{{ detailItem.actionDeadline ? formatDate(detailItem.actionDeadline) : '-' }}</p></div>
            <div><span class="text-gray-500">조치 담당자</span><p class="font-medium mt-0.5">{{ detailItem.resolver || '-' }}</p></div>
            <div v-if="detailItem.resolvedAt"><span class="text-gray-500">조치 완료일</span><p class="font-medium mt-0.5">{{ formatDate(detailItem.resolvedAt) }}</p></div>
          </div>
          <div v-if="detailItem.findingDetail">
            <span class="text-gray-500 block mb-1">결함 상세</span>
            <p class="text-gray-800 whitespace-pre-wrap bg-gray-50 p-3 rounded-lg text-sm">{{ detailItem.findingDetail }}</p>
          </div>
          <div v-if="detailItem.correctiveAction">
            <span class="text-gray-500 block mb-1">시정조치 계획</span>
            <p class="text-gray-800 whitespace-pre-wrap bg-blue-50 p-3 rounded-lg text-sm">{{ detailItem.correctiveAction }}</p>
          </div>
          <div v-if="detailItem.fileName" class="flex items-center gap-3 p-3 bg-gray-50 rounded-lg border">
            <svg class="w-5 h-5 text-gray-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13"/></svg>
            <span class="flex-1 text-sm text-gray-700 truncate">{{ detailItem.fileName }}</span>
            <button @click="download(detailItem)" class="text-primary-600 hover:text-primary-700 text-xs font-medium">다운로드</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Form Modal -->
    <div v-if="showForm" class="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-2xl max-h-[90vh] flex flex-col">
        <div class="px-6 py-4 border-b">
          <h2 class="text-lg font-semibold">{{ editItem ? $t('secFinding.editFinding') : $t('secFinding.addFinding') }}</h2>
        </div>
        <div class="overflow-y-auto flex-1 px-6 py-5 space-y-4">
          <div class="grid grid-cols-3 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">연도</label>
              <input v-model.number="form.year" type="number" class="input w-full" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('secFinding.auditType') }}</label>
              <select v-model="form.auditType" class="input w-full">
                <option v-for="t in AUDIT_TYPES" :key="t" :value="t">{{ $t(`secFinding.auditType_label.${t}`) }}</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('secFinding.riskLevel') }}</label>
              <select v-model="form.riskLevel" class="input w-full">
                <option v-for="r in RISK_LEVELS" :key="r" :value="r">{{ $t(`secFinding.riskLevel_label.${r}`) }}</option>
              </select>
            </div>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('secFinding.requirementCode') }}</label>
              <input v-model="form.requirementCode" class="input w-full" placeholder="예: 2.1.1" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('secFinding.requirementName') }}</label>
              <input v-model="form.requirementName" class="input w-full" placeholder="예: 정책의 유지관리" />
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('secFinding.domain') }}</label>
            <input v-model="form.domain" class="input w-full" placeholder="예: 2. 보호대책 요구사항" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('secFinding.findingSummary') }} *</label>
            <input v-model="form.findingSummary" class="input w-full" placeholder="결함 요약을 입력하세요" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('secFinding.findingDetail') }}</label>
            <textarea v-model="form.findingDetail" rows="3" class="input w-full resize-none" placeholder="결함 상세 내용" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('secFinding.correctiveAction') }}</label>
            <textarea v-model="form.correctiveAction" rows="3" class="input w-full resize-none" placeholder="시정조치 계획" />
          </div>
          <div class="grid grid-cols-3 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('secFinding.actionDeadline') }}</label>
              <input v-model="form.actionDeadline" type="date" class="input w-full" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('secFinding.status') }}</label>
              <select v-model="form.status" class="input w-full">
                <option v-for="s in STATUSES" :key="s" :value="s">{{ $t(`secFinding.status_label.${s}`) }}</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('secFinding.resolver') }}</label>
              <input v-model="form.resolver" class="input w-full" placeholder="조치 담당자" />
            </div>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('secFinding.resolvedAt') }}</label>
              <input v-model="form.resolvedAt" type="date" class="input w-full" />
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('secFinding.attachment') }}</label>
            <input ref="formFileInput" type="file" @change="onFormFileChange"
              class="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0
              file:text-sm file:font-semibold file:bg-primary-50 file:text-primary-700 hover:file:bg-primary-100" />
          </div>
        </div>
        <div class="px-6 py-4 border-t flex justify-end gap-2">
          <button @click="showForm = false" class="btn-secondary">{{ $t('common.cancel') }}</button>
          <button @click="save" :disabled="saving" class="btn-primary">{{ saving ? $t('common.loading') : $t('common.save') }}</button>
        </div>
      </div>
    </div>

    <!-- Confirm -->
    <div v-if="confirmModal.show" class="fixed inset-0 bg-black/50 z-[70] flex items-center justify-center p-4">
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-sm p-6">
        <h3 class="text-base font-semibold mb-2">삭제 확인</h3>
        <p class="text-sm text-gray-500 mb-5">{{ confirmModal.message }}</p>
        <div class="flex justify-end gap-2">
          <button @click="confirmModal.show = false" class="btn-secondary">{{ $t('common.cancel') }}</button>
          <button @click="confirmModal.onConfirm" class="btn-danger">{{ $t('common.delete') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { secFindingApi } from '@/api'

const auth = useAuthStore()
const isManager = computed(() => auth.isAdmin || auth.user?.role === 'MANAGER')

const yearOptions = ref([])
const findings = ref([])
const loading = ref(false)
const saving = ref(false)
const currentPage = ref(0)
const totalPages = ref(1)
const pageSize = 20

const showForm = ref(false)
const showDetail = ref(false)
const editItem = ref(null)
const detailItem = ref(null)
const formFile = ref(null)
const formFileInput = ref(null)
const confirmModal = ref({ show: false, message: '', onConfirm: () => {} })

const AUDIT_TYPES = ['ISMS_P', 'INTERNAL', 'OTHER']
const RISK_LEVELS = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW']
const STATUSES = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'ACCEPTED']

const filters = ref({ year: '', auditType: '', riskLevel: '', status: '', keyword: '' })

const statusCounts = computed(() => {
  const counts = {}
  STATUSES.forEach(s => { counts[s] = findings.value.filter(f => f.status === s).length })
  return counts
})

const form = ref(emptyForm())

function emptyForm() {
  return { year: new Date().getFullYear(), auditType: 'ISMS_P', domain: '', requirementCode: '',
    requirementName: '', findingSummary: '', findingDetail: '', riskLevel: 'MEDIUM',
    correctiveAction: '', actionDeadline: '', status: 'OPEN', resolvedAt: '', resolver: '' }
}

let debounceTimer = null
function debouncedFetch() {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(fetchFindings, 400)
}

onMounted(async () => {
  const years = await secFindingApi.years()
  yearOptions.value = years?.data || []
  if (yearOptions.value.length) filters.value.year = yearOptions.value[0]
  fetchFindings()
})

async function fetchFindings(page = 0) {
  loading.value = true
  currentPage.value = page
  try {
    const params = { page, size: pageSize }
    if (filters.value.year) params.year = filters.value.year
    if (filters.value.auditType) params.auditType = filters.value.auditType
    if (filters.value.riskLevel) params.riskLevel = filters.value.riskLevel
    if (filters.value.status) params.status = filters.value.status
    if (filters.value.keyword) params.keyword = filters.value.keyword
    const res = await secFindingApi.list(params)
    const pageData = res?.data
    findings.value = pageData?.content || []
    totalPages.value = pageData?.totalPages || 1
  } finally {
    loading.value = false
  }
}

function goPage(p) { fetchFindings(p) }

function resetFilters() {
  filters.value = { year: yearOptions.value[0] || '', auditType: '', riskLevel: '', status: '', keyword: '' }
  fetchFindings()
}

function toggleStatus(s) {
  filters.value.status = filters.value.status === s ? '' : s
  fetchFindings()
}

function openCreate() {
  editItem.value = null
  form.value = emptyForm()
  if (filters.value.year) form.value.year = Number(filters.value.year)
  formFile.value = null
  showForm.value = true
}

function openEdit(f) {
  editItem.value = f
  form.value = { year: f.year, auditType: f.auditType, domain: f.domain || '',
    requirementCode: f.requirementCode || '', requirementName: f.requirementName || '',
    findingSummary: f.findingSummary, findingDetail: f.findingDetail || '',
    riskLevel: f.riskLevel, correctiveAction: f.correctiveAction || '',
    actionDeadline: f.actionDeadline || '', status: f.status,
    resolvedAt: f.resolvedAt || '', resolver: f.resolver || '' }
  formFile.value = null
  showForm.value = true
}

function openDetail(f) { detailItem.value = f; showDetail.value = true }

function onFormFileChange(e) { formFile.value = e.target.files[0] || null }

async function save() {
  if (!form.value.findingSummary.trim()) { alert('결함 요약을 입력하세요.'); return }
  saving.value = true
  try {
    const payload = { ...form.value, actionDeadline: form.value.actionDeadline || null, resolvedAt: form.value.resolvedAt || null }
    if (editItem.value) {
      await secFindingApi.update(editItem.value.id, payload, formFile.value)
    } else {
      await secFindingApi.create(payload, formFile.value)
      if (!yearOptions.value.includes(form.value.year)) {
        yearOptions.value = [form.value.year, ...yearOptions.value].sort((a, b) => b - a)
      }
    }
    showForm.value = false
    fetchFindings(currentPage.value)
  } catch (e) { alert(e || '저장 실패') } finally { saving.value = false }
}

function confirmDelete(f) {
  confirmModal.value = { show: true, message: '결함사항을 삭제하시겠습니까?',
    onConfirm: async () => { confirmModal.value.show = false; await secFindingApi.delete(f.id); fetchFindings(currentPage.value) } }
}

function download(f) { secFindingApi.download(f.id, f.fileName) }

function riskBadge(r) {
  return { CRITICAL: 'bg-red-100 text-red-700', HIGH: 'bg-orange-100 text-orange-700',
    MEDIUM: 'bg-yellow-100 text-yellow-700', LOW: 'bg-blue-100 text-blue-700' }[r] || 'bg-gray-100 text-gray-500'
}
function statusBadge(s) {
  return { OPEN: 'bg-red-100 text-red-700', IN_PROGRESS: 'bg-yellow-100 text-yellow-700',
    RESOLVED: 'bg-green-100 text-green-700', ACCEPTED: 'bg-gray-100 text-gray-600' }[s] || 'bg-gray-100 text-gray-500'
}
function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' })
}
</script>
