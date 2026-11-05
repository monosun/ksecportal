<template>
  <div class="p-8">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">보안정책관리</h1>
        <p class="text-sm text-gray-500 mt-1">규정·지침·가이드·절차서·매뉴얼 등 정책 문서의 버전과 개정사항을 파일 단위로 관리합니다.</p>
      </div>
      <button v-if="perm.canWrite()" @click="openCreate" class="btn-primary flex items-center gap-2">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
        </svg>
        정책 문서 등록
      </button>
    </div>

    <!-- 분류 탭 -->
    <div class="flex flex-wrap gap-1 border-b border-gray-200 mb-4">
      <button v-for="tab in tabs" :key="tab.value" @click="selectCategory(tab.value)"
        :class="['px-4 py-2 text-sm font-medium border-b-2 -mb-px transition-colors',
                 filters.category === tab.value ? 'border-primary-600 text-primary-700' : 'border-transparent text-gray-500 hover:text-gray-700']">
        {{ tab.label }}
        <span :class="['ml-1 text-xs px-1.5 py-0.5 rounded-full',
                       filters.category === tab.value ? 'bg-primary-100 text-primary-700' : 'bg-gray-100 text-gray-500']">
          {{ tab.count }}
        </span>
      </button>
    </div>

    <!-- 필터 -->
    <div class="card mb-4 flex flex-wrap gap-3 items-end">
      <div class="flex flex-col gap-1">
        <label class="text-xs text-gray-500">상태</label>
        <select v-model="filters.status" @change="reload()" class="input w-32 text-sm">
          <option value="">전체</option>
          <option value="ACTIVE">시행</option>
          <option value="ABOLISHED">폐지</option>
        </select>
      </div>
      <div class="flex flex-col gap-1 flex-1 min-w-48">
        <label class="text-xs text-gray-500">{{ $t('common.search') }}</label>
        <input v-model="filters.keyword" @input="debouncedSearch" placeholder="문서명·문서번호·주관부서·담당자·설명"
          class="input text-sm" />
      </div>
      <button @click="resetFilters" class="btn-secondary text-sm">초기화</button>
    </div>

    <!-- 목록 -->
    <div class="card">
      <div v-if="loading" class="text-center py-10 text-gray-500">{{ $t('common.loading') }}</div>
      <div v-else-if="loadError" class="text-center py-10 text-sm text-red-500">{{ loadError }}</div>
      <div v-else-if="docs.length === 0" class="text-center py-10 text-gray-400">
        {{ $t('common.noData') }}
        <p v-if="perm.canWrite()" class="text-xs mt-1">우측 상단의 <strong>정책 문서 등록</strong>으로 추가할 수 있습니다.</p>
      </div>
      <div v-else class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b text-left text-gray-600">
              <th class="py-3 px-3 font-semibold">분류</th>
              <th class="py-3 px-3 font-semibold">문서번호</th>
              <th class="py-3 px-3 font-semibold">문서명</th>
              <th class="py-3 px-3 font-semibold">현행 버전</th>
              <th class="py-3 px-3 font-semibold whitespace-nowrap">제·개정일</th>
              <th class="py-3 px-3 font-semibold whitespace-nowrap">시행일</th>
              <th class="py-3 px-3 font-semibold">주관부서</th>
              <th class="py-3 px-3 font-semibold">상태</th>
              <th class="py-3 px-3 font-semibold">문서</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="d in docs" :key="d.id" class="border-b hover:bg-gray-50 cursor-pointer" @click="openDetail(d)">
              <td class="py-3 px-3">
                <span :class="categoryBadge(d.category)" class="text-xs font-medium px-2 py-0.5 rounded whitespace-nowrap">{{ categoryLabel(d.category) }}</span>
              </td>
              <td class="py-3 px-3 text-xs font-mono text-gray-500 whitespace-nowrap">{{ d.docNo || '-' }}</td>
              <td class="py-3 px-3">
                <div class="font-medium text-gray-900 hover:text-primary-600">{{ d.title }}</div>
                <div v-if="d.current?.changeReason" class="text-xs text-gray-400 mt-0.5 line-clamp-1">{{ d.current.changeReason }}</div>
              </td>
              <td class="py-3 px-3 whitespace-nowrap">
                <template v-if="d.current">
                  <span class="font-mono">v{{ d.current.version }}</span>
                  <span :class="revisionBadge(d.current.revisionType)" class="ml-1">{{ revisionLabel(d.current.revisionType) }}</span>
                  <span v-if="d.revisionCount > 1" class="ml-1 text-xs bg-gray-100 text-gray-500 px-1.5 py-0.5 rounded">{{ d.revisionCount }}개 버전</span>
                </template>
                <span v-else class="text-xs text-gray-400">-</span>
              </td>
              <td class="py-3 px-3 text-xs text-gray-500 whitespace-nowrap">{{ d.current?.revisionDate || '-' }}</td>
              <td class="py-3 px-3 text-xs text-gray-500 whitespace-nowrap">{{ d.current?.effectiveDate || '-' }}</td>
              <td class="py-3 px-3 text-xs text-gray-500">{{ d.department || '-' }}</td>
              <td class="py-3 px-3">
                <span :class="d.status === 'ABOLISHED' ? 'badge-red' : 'badge-green'">{{ d.status === 'ABOLISHED' ? '폐지' : '시행' }}</span>
              </td>
              <td class="py-3 px-3" @click.stop>
                <button v-if="d.current?.fileName" @click="openDetail(d, true)" :title="`${d.current.fileName} 보기`"
                  class="flex items-center gap-1 text-primary-600 hover:text-primary-800 text-xs whitespace-nowrap">
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                  </svg>
                  보기
                </button>
                <span v-else class="text-xs text-gray-400">파일 없음</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 페이지네이션 -->
    <div v-if="totalPages > 1" class="flex justify-center items-center gap-1 mt-4">
      <button @click="goPage(page - 1)" :disabled="page === 0"
        class="px-2 py-1.5 rounded border text-xs text-gray-500 border-gray-300 hover:bg-gray-50 disabled:opacity-30 disabled:cursor-not-allowed">‹</button>
      <span class="px-3 text-xs text-gray-500">{{ page + 1 }} / {{ totalPages }}</span>
      <button @click="goPage(page + 1)" :disabled="page >= totalPages - 1"
        class="px-2 py-1.5 rounded border text-xs text-gray-500 border-gray-300 hover:bg-gray-50 disabled:opacity-30 disabled:cursor-not-allowed">›</button>
    </div>

    <PolicyDocDetailModal :open="detail.open" :item-id="detail.id" :auto-view="detail.autoView"
      @close="detail.open = false" @edit="openEdit" @changed="reload(false)" />
    <PolicyDocFormModal :open="form.open" :item-id="form.id" :default-category="filters.category"
      @close="form.open = false" @saved="onSaved" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useDebounceFn } from '@vueuse/core'
import { policyDocApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { estimateRows } from '@/composables/useFitPageSize'
import PolicyDocDetailModal from './PolicyDocDetailModal.vue'
import PolicyDocFormModal from './PolicyDocFormModal.vue'
import {
  CATEGORIES, categoryLabel, categoryBadge, revisionLabel, revisionBadge, usePolicyDocPermission,
} from './policyDocMeta'

const auth = useAuthStore()
const perm = usePolicyDocPermission(auth)

const docs = ref([])
const loading = ref(true)
const loadError = ref('')
const page = ref(0)
const pageSize = estimateRows({ top: 420, max: 100 })
const totalPages = ref(0)
const counts = ref({})
const filters = ref({ category: '', status: '', keyword: '' })

const detail = reactive({ open: false, id: null, autoView: false })
const form = reactive({ open: false, id: null })

const tabs = computed(() => {
  const total = Object.values(counts.value).reduce((a, b) => a + b, 0)
  return [
    { value: '', label: '전체', count: total },
    ...CATEGORIES.map(c => ({ value: c.value, label: c.label, count: counts.value[c.value] || 0 })),
  ]
})

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const params = {
      page: page.value,
      size: pageSize,
      ...(filters.value.category && { category: filters.value.category }),
      ...(filters.value.status && { status: filters.value.status }),
      ...(filters.value.keyword && { keyword: filters.value.keyword }),
    }
    const res = await policyDocApi.list(params)
    docs.value = res.data?.content || []
    totalPages.value = res.data?.page?.totalPages ?? res.data?.totalPages ?? 0
  } catch (e) {
    loadError.value = e || '목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

async function loadCounts() {
  try {
    const res = await policyDocApi.summary(filters.value.status ? { status: filters.value.status } : {})
    counts.value = res.data || {}
  } catch {
    counts.value = {}
  }
}

/** @param resetPage 필터가 바뀌면 첫 페이지로, 저장·삭제 후에는 현재 페이지 유지 */
function reload(resetPage = true) {
  if (resetPage) page.value = 0
  load()
  loadCounts()
}

const debouncedSearch = useDebounceFn(() => { page.value = 0; load() }, 400)

function selectCategory(value) {
  filters.value.category = value
  page.value = 0
  load()
}

function resetFilters() {
  filters.value = { category: '', status: '', keyword: '' }
  reload()
}

function goPage(p) {
  const target = Math.max(0, Math.min(p, totalPages.value - 1))
  if (target !== page.value) { page.value = target; load() }
}

function openDetail(d, autoView = false) {
  detail.id = d.id
  detail.autoView = autoView
  detail.open = true
}

function openCreate() {
  form.id = null
  form.open = true
}

function openEdit(id) {
  detail.open = false
  form.id = id
  form.open = true
}

function onSaved(id) {
  form.open = false
  reload(false)
  if (id) openDetail({ id })
}

onMounted(() => reload())
</script>
