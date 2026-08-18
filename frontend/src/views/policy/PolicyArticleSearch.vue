<template>
  <div>
    <!-- 필터 -->
    <div class="card mb-4 space-y-3">
      <!-- 1행: 검색어 + 검색범위 -->
      <div class="flex flex-wrap gap-3">
        <div class="relative flex-1 min-w-64">
          <svg class="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-4.35-4.35M17 11a6 6 0 11-12 0 6 6 0 0112 0z"/>
          </svg>
          <input v-model="filters.keyword" type="text" class="input pl-9 w-full"
            placeholder="조문 검색 — 본문 내용, 조 제목, 지침명, 장 제목으로 찾기"
            @input="debouncedSearch" @keyup.enter="runSearch" />
        </div>
        <select v-model="filters.scope" class="input w-44" @change="runSearch" title="검색 범위">
          <option v-for="s in scopes" :key="s.value" :value="s.value">{{ s.label }}</option>
        </select>
      </div>

      <!-- 2행: 지침 > 장 > 조 + 카테고리/상태 -->
      <div class="flex flex-wrap gap-3">
        <select v-model="filters.guideline" class="input w-52" @change="onGuidelineChange">
          <option value="">지침: 전체</option>
          <option v-for="g in facets.guidelines" :key="g.name" :value="g.name">{{ g.name }}</option>
        </select>

        <select v-model="filters.chapterKey" class="input w-56" @change="runSearch"
          :disabled="!chapterOptions.length" :title="chapterOptions.length ? '' : '지침을 먼저 선택하세요'">
          <option value="">장: 전체</option>
          <option v-for="c in chapterOptions" :key="c.policyId" :value="String(c.policyId)">{{ c.label }}</option>
        </select>

        <input v-model="filters.articleNo" type="number" min="1" class="input w-28"
          placeholder="조 번호" @input="debouncedSearch" title="조 번호 (예: 3 → 제3조)" />

        <select v-model="filters.category" class="input w-40" @change="runSearch">
          <option value="">{{ $t('policy.category') }}: {{ $t('common.all') }}</option>
          <option v-for="c in categories" :key="c" :value="c">{{ $t(`policy.category_label.${c}`) }}</option>
        </select>

        <select v-model="filters.status" class="input w-36" @change="runSearch">
          <option value="">{{ $t('common.status') }}: {{ $t('common.all') }}</option>
          <option v-for="s in statuses" :key="s" :value="s">{{ $t(`policy.status.${s}`) }}</option>
        </select>

        <button v-if="hasActiveFilter" @click="resetFilters"
          class="text-sm text-gray-500 hover:text-gray-700 underline px-1">필터 초기화</button>
      </div>
    </div>

    <!-- 결과 요약 -->
    <div class="flex flex-wrap items-center gap-3 mb-3 px-1">
      <p class="text-sm text-gray-600">
        <template v-if="loading">검색 중...</template>
        <template v-else>
          총 <b class="text-primary-700">{{ totalElements }}</b>개 조문
          <span v-if="facets.totalArticles" class="text-gray-400">/ 전체 {{ facets.totalArticles }}개</span>
        </template>
      </p>
      <button v-if="isManager" @click="resync" :disabled="resyncing"
        class="ml-auto text-xs text-gray-500 hover:text-primary-600 underline disabled:opacity-50"
        title="정책 본문을 다시 파싱해 조문을 재등록합니다">
        {{ resyncing ? '재등록 중...' : '조문 재등록' }}
      </button>
    </div>
    <p v-if="errorMsg" class="mb-3 px-4 py-3 bg-red-50 border border-red-200 rounded-xl text-sm text-red-700">{{ errorMsg }}</p>

    <!-- 결과 목록 -->
    <div v-if="loading" class="card text-center text-gray-400 py-12">{{ $t('common.loading') }}</div>
    <div v-else-if="!articles.length" class="card text-center text-gray-400 py-12">
      검색 조건에 맞는 조문이 없습니다.
    </div>
    <div v-else ref="listEl" class="space-y-2">
      <div v-for="a in articles" :key="a.id"
        class="card p-0 overflow-hidden hover:border-primary-200 transition-colors">
        <button class="w-full text-left px-5 py-3.5" @click="toggle(a.id)">
          <!-- 경로: 지침 > 장 -->
          <div class="flex flex-wrap items-center gap-1.5 text-xs text-gray-500 mb-1.5">
            <span class="font-medium text-gray-700" v-html="highlight(a.guidelineName || '(미분류)')"></span>
            <span class="text-gray-300">›</span>
            <span v-html="highlight(chapterText(a))"></span>
            <span :class="statusBadgeClass(a.status)" class="ml-1">{{ $t(`policy.status.${a.status}`) }}</span>
            <span class="badge-blue">{{ $t(`policy.category_label.${a.category}`) }}</span>
          </div>
          <!-- 조 제목 -->
          <div class="flex items-start gap-2">
            <span class="shrink-0 px-2 py-0.5 rounded-md bg-primary-50 text-primary-700 text-xs font-bold">
              {{ a.articleLabel }}
            </span>
            <span class="text-sm font-semibold text-gray-900 leading-6" v-html="highlight(a.title || '(제목 없음)')"></span>
            <span v-if="a.note" class="text-xs text-amber-600 shrink-0 mt-0.5">{{ a.note }}</span>
            <svg class="w-4 h-4 text-gray-400 ml-auto shrink-0 mt-1 transition-transform"
              :class="expanded.has(a.id) ? 'rotate-180' : ''" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
            </svg>
          </div>
          <!-- 본문 미리보기 (접힌 상태) -->
          <p v-if="!expanded.has(a.id)" class="mt-1.5 text-xs text-gray-500 line-clamp-2 leading-5"
            v-html="highlight(snippet(a.content))"></p>
        </button>

        <!-- 펼친 본문 -->
        <div v-if="expanded.has(a.id)" class="px-5 pb-4 border-t border-gray-100 pt-3">
          <MarkdownView :content="a.content" />
          <div class="mt-3 pt-3 border-t border-gray-100 flex justify-end">
            <button @click="$emit('open-policy', a.policyId)" class="btn-secondary text-xs">
              소속 장 전체 보기
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 페이지네이션 -->
    <div v-if="totalPages > 1" class="flex justify-center items-center gap-2 mt-4">
      <button @click="goPage(page - 1)" :disabled="page === 0" class="btn-secondary px-3 py-1 text-sm disabled:opacity-40">이전</button>
      <span class="text-sm text-gray-500 px-2">{{ page + 1 }} / {{ totalPages }}</span>
      <button @click="goPage(page + 1)" :disabled="page >= totalPages - 1" class="btn-secondary px-3 py-1 text-sm disabled:opacity-40">다음</button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useFitPageSize, keepFirstRow } from '@/composables/useFitPageSize'
import { useDebounceFn } from '@vueuse/core'
import { policyApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import MarkdownView from '@/components/MarkdownView.vue'

defineEmits(['open-policy'])

const auth = useAuthStore()
const isManager = auth.isManager

// 검색 범위 — 백엔드 PolicyArticleService.Scope 와 1:1
const scopes = [
  { value: 'ALL',       label: '전체 (제목+본문)' },
  { value: 'CONTENT',   label: '본문 내용' },
  { value: 'TITLE',     label: '조 제목' },
  { value: 'GUIDELINE', label: '지침명' },
  { value: 'CHAPTER',   label: '장 제목' },
  { value: 'ARTICLE',   label: '조 표기 (제N조)' },
  { value: 'HEADING',   label: '조 표기 + 조 제목' }
]
const statuses = ['DRAFT', 'REVIEW', 'PUBLISHED', 'ARCHIVED']
const categories = ['GENERAL', 'ACCESS_CONTROL', 'DATA_PROTECTION', 'INCIDENT_RESPONSE', 'NETWORK', 'PHYSICAL', 'VENDOR', 'OTHER']

const filters = reactive({
  keyword: '', scope: 'ALL', guideline: '', chapterKey: '', articleNo: '', category: '', status: ''
})

const facets = ref({ guidelines: [], totalArticles: 0 })
const articles = ref([])
const expanded = ref(new Set())
const loading = ref(false)
const resyncing = ref(false)
const errorMsg = ref('')
const page = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
// 하이라이트는 실제 조회에 쓴 검색어 기준 — 입력 중 깜빡임을 막는다.
const appliedKeyword = ref('')

const chapterOptions = computed(() => {
  const g = facets.value.guidelines.find(x => x.name === filters.guideline)
  return g ? g.chapters : []
})

const hasActiveFilter = computed(() =>
  !!(filters.keyword || filters.guideline || filters.chapterKey || filters.articleNo ||
     filters.category || filters.status || filters.scope !== 'ALL'))

function chapterText(a) {
  if (!a.chapterLabel && !a.chapterTitle) return '(장 없음)'
  return [a.chapterLabel, a.chapterTitle].filter(Boolean).join(' ')
}

function snippet(content) {
  if (!content) return ''
  // 마크다운 기호를 걷어낸 평문 미리보기. 검색어가 있으면 그 주변을 보여준다.
  const plain = content.replace(/[#*`>\-]/g, ' ').replace(/\s+/g, ' ').trim()
  const kw = appliedKeyword.value.trim()
  if (kw) {
    const idx = plain.toLowerCase().indexOf(kw.toLowerCase())
    if (idx > 60) return '… ' + plain.slice(idx - 50, idx + 210)
  }
  return plain.slice(0, 260)
}

function escapeHtml(s) {
  return String(s ?? '').replace(/[&<>"']/g, c =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]))
}

/** 검색어 일치 부분만 표시용으로 강조 — 항상 이스케이프 후 <mark> 만 주입한다. */
function highlight(text) {
  const safe = escapeHtml(text)
  const kw = appliedKeyword.value.trim()
  if (!kw) return safe
  const pattern = escapeHtml(kw).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return safe.replace(new RegExp(pattern, 'gi'),
    m => `<mark class="bg-yellow-200 text-gray-900 rounded px-0.5">${m}</mark>`)
}

function toggle(id) {
  const next = new Set(expanded.value)
  next.has(id) ? next.delete(id) : next.add(id)
  expanded.value = next
}

function onGuidelineChange() {
  filters.chapterKey = ''   // 지침이 바뀌면 장 선택은 무효
  runSearch()
}

function resetFilters() {
  Object.assign(filters, {
    keyword: '', scope: 'ALL', guideline: '', chapterKey: '', articleNo: '', category: '', status: ''
  })
  runSearch()
}

function goPage(n) {
  page.value = n
  load()
}

function runSearch() {
  page.value = 0
  load()
}

// 필터를 빠르게 연속 변경하면 응답이 요청 순서대로 오지 않을 수 있다.
// 마지막으로 보낸 요청의 결과만 화면에 반영한다.
let requestSeq = 0

// 결과 카드가 화면 높이에 맞게 들어가도록 한 페이지 건수를 정한다(카드 높이를 실측).
const { listEl, pageSize, refine: refinePageSize } = useFitPageSize({
  rowSelector: ':scope > div', rowHeight: 104, headHeight: 0, fallbackTop: 430,
  onChange: (size, prev) => { page.value = keepFirstRow(page.value, prev, size); load() }
})

async function load() {
  const seq = ++requestSeq
  loading.value = true
  errorMsg.value = ''
  try {
    const params = { page: page.value, size: pageSize.value }
    if (filters.keyword) { params.keyword = filters.keyword; params.scope = filters.scope }
    if (filters.guideline) params.guideline = filters.guideline
    if (filters.chapterKey) params.policyId = filters.chapterKey
    if (filters.articleNo) params.articleNo = filters.articleNo
    if (filters.category) params.category = filters.category
    if (filters.status) params.status = filters.status

    const res = await policyApi.articles(params)
    if (seq !== requestSeq) return           // 더 최신 요청이 있으면 이 결과는 버린다

    articles.value = res.data?.content || []
    totalPages.value = res.data?.page?.totalPages ?? res.data?.totalPages ?? 0
    totalElements.value = res.data?.page?.totalElements ?? res.data?.totalElements ?? articles.value.length
    appliedKeyword.value = filters.keyword
    expanded.value = new Set()
  } catch (e) {
    if (seq !== requestSeq) return
    errorMsg.value = typeof e === 'string' ? e : '조문을 불러오지 못했습니다.'
    articles.value = []
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

const debouncedSearch = useDebounceFn(runSearch, 400)

async function loadFacets() {
  try { facets.value = (await policyApi.articleFacets()).data }
  catch { /* 필터 목록 실패는 검색 자체를 막지 않는다 */ }
}

async function resync() {
  resyncing.value = true
  errorMsg.value = ''
  try {
    await policyApi.resyncArticles()
    await loadFacets()
    await load()
  } catch (e) {
    errorMsg.value = typeof e === 'string' ? e : '조문 재등록에 실패했습니다.'
  } finally {
    resyncing.value = false
  }
}

function statusBadgeClass(status) {
  return { DRAFT: 'badge-gray', REVIEW: 'badge-yellow', PUBLISHED: 'badge-green', ARCHIVED: 'badge-gray' }[status] || 'badge-gray'
}

onMounted(async () => { loadFacets(); await load(); refinePageSize() })
</script>
