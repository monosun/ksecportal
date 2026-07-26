<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">관련 사이트</h1>
        <p class="text-sm text-gray-400 mt-0.5">
          보안·개인정보 업무에 참고하는 사이트를 등록해 두고, 각 사이트의 최신 게시물을 모아 봅니다
          <span v-if="sites.length" class="text-gray-300">· 총 {{ sites.length }}개 사이트</span>
        </p>
      </div>
      <div v-if="canWrite" class="flex gap-2">
        <button @click="refreshAll" class="btn-secondary text-sm" :disabled="refreshing">
          {{ refreshing ? '가져오는 중…' : '전체 새로고침' }}
        </button>
        <button @click="openCreate" class="btn-primary text-sm">사이트 등록</button>
      </div>
    </div>

    <div class="page-body">
      <!-- 검색 · 필터 -->
      <div class="card !p-4">
        <div class="flex gap-3 flex-wrap">
          <div class="relative flex-1 min-w-[240px]">
            <svg class="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
            </svg>
            <input v-model="keyword" type="text" class="input pl-9 w-full"
              placeholder="사이트명·주소·설명·게시물 제목 검색" />
          </div>
          <button v-if="keyword || category" @click="resetFilter" class="btn-secondary text-sm px-3">초기화</button>
          <label v-if="canWrite" class="flex items-center gap-1.5 text-sm text-gray-500 whitespace-nowrap">
            <input v-model="includeInactive" type="checkbox" class="rounded border-gray-300" @change="load" />
            미사용 포함
          </label>
          <span class="flex items-center text-sm text-gray-500 whitespace-nowrap">{{ filtered.length }}건</span>
        </div>

        <div v-if="categories.length" class="flex flex-wrap gap-1.5 mt-3 pt-3 border-t border-gray-100">
          <button
            class="px-2.5 py-1 rounded-full text-xs font-medium border transition-colors"
            :class="category === '' ? 'bg-primary-600 border-primary-600 text-white'
                                    : 'bg-white border-gray-200 text-gray-600 hover:bg-gray-50'"
            @click="category = ''">
            전체 <span class="tabular-nums">{{ sites.length }}</span>
          </button>
          <button v-for="c in categories" :key="c.name"
            class="px-2.5 py-1 rounded-full text-xs font-medium border transition-colors"
            :class="category === c.name ? 'bg-primary-600 border-primary-600 text-white'
                                        : 'bg-white border-gray-200 text-gray-600 hover:bg-gray-50'"
            @click="category = c.name">
            {{ c.name }} <span class="tabular-nums">{{ c.count }}</span>
          </button>
        </div>

        <p v-if="lastFetchedLabel" class="text-[11px] text-gray-400 mt-3">
          최근 수집: {{ lastFetchedLabel }}
          <span class="text-gray-300">· 매일 새벽 자동으로 다시 가져옵니다</span>
        </p>
      </div>

      <div v-if="loading" class="card text-center py-12 text-sm text-gray-400">불러오는 중...</div>
      <div v-else-if="filtered.length === 0" class="card text-center py-12">
        <p class="text-sm text-gray-500">등록된 관련 사이트가 없습니다.</p>
        <p v-if="canWrite" class="text-xs text-gray-400 mt-1">우측 상단의 <strong>사이트 등록</strong>으로 추가할 수 있습니다.</p>
      </div>

      <!-- 사이트 카드 -->
      <div v-else class="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <section v-for="s in filtered" :key="s.id"
          class="card !p-0 overflow-hidden flex flex-col" :class="{ 'opacity-60': !s.active }">
          <!-- 헤더 -->
          <div class="px-4 py-3 border-b border-gray-100 bg-gray-50/60">
            <div class="flex items-start justify-between gap-2">
              <div class="min-w-0">
                <div class="flex items-center gap-1.5 flex-wrap">
                  <a :href="s.url" target="_blank" rel="noopener noreferrer"
                    class="text-sm font-bold text-gray-900 hover:text-primary-700 hover:underline">
                    {{ s.name }}
                  </a>
                  <span v-if="s.category"
                    class="px-1.5 py-0.5 rounded bg-primary-50 text-primary-700 text-[11px] font-medium">
                    {{ s.category }}
                  </span>
                  <span v-if="!s.active"
                    class="px-1.5 py-0.5 rounded bg-gray-100 text-gray-500 text-[11px]">미사용</span>
                </div>
                <a :href="s.url" target="_blank" rel="noopener noreferrer"
                  class="text-[11px] text-gray-400 hover:text-primary-600 break-all">{{ hostOf(s.url) }}</a>
              </div>
              <div v-if="canWrite" class="flex gap-1 shrink-0">
                <button class="text-[11px] px-1.5 py-0.5 rounded border border-gray-200 text-gray-500 hover:bg-white"
                  :disabled="busyId === s.id" @click="refreshOne(s)">
                  {{ busyId === s.id ? '…' : '새로고침' }}
                </button>
                <button class="text-[11px] px-1.5 py-0.5 rounded border border-gray-200 text-gray-500 hover:bg-white"
                  @click="openEdit(s)">수정</button>
                <button class="text-[11px] px-1.5 py-0.5 rounded border border-gray-200 text-red-500 hover:bg-white"
                  @click="remove(s)">삭제</button>
              </div>
            </div>
            <p v-if="s.description" class="text-xs text-gray-500 mt-1.5 leading-relaxed">{{ s.description }}</p>
          </div>

          <!-- 가져온 내용 -->
          <div class="flex-1">
            <ul v-if="s.items && s.items.length" class="divide-y divide-gray-100">
              <li v-for="it in s.items" :key="it.id" class="px-4 py-2.5 hover:bg-gray-50/60">
                <a :href="it.link || s.url" target="_blank" rel="noopener noreferrer"
                  class="text-sm text-gray-800 hover:text-primary-700 hover:underline font-medium line-clamp-2">
                  {{ it.title }}
                </a>
                <p v-if="it.summary" class="text-xs text-gray-500 mt-0.5 line-clamp-2 leading-relaxed">{{ it.summary }}</p>
                <p v-if="dateLabel(it)" class="text-[11px] text-gray-400 mt-0.5">{{ dateLabel(it) }}</p>
              </li>
            </ul>

            <div v-else-if="s.fetchedSummary" class="px-4 py-3">
              <p class="text-sm text-gray-600 leading-relaxed">{{ s.fetchedSummary }}</p>
            </div>

            <div v-else class="px-4 py-5 text-center">
              <p class="text-xs text-gray-400">
                {{ s.fetchMessage || '아직 사이트 내용을 가져오지 않았습니다.' }}
              </p>
              <a :href="s.url" target="_blank" rel="noopener noreferrer"
                class="text-xs text-primary-600 hover:underline mt-1 inline-block">사이트 바로가기 →</a>
            </div>
          </div>

          <!-- 푸터 -->
          <div class="px-4 py-2 border-t border-gray-100 flex items-center justify-between">
            <span class="text-[11px]" :class="statusClass(s.fetchStatus)">{{ statusLabel(s) }}</span>
            <a :href="s.url" target="_blank" rel="noopener noreferrer"
              class="text-[11px] text-gray-400 hover:text-primary-600">사이트 바로가기 →</a>
          </div>
        </section>
      </div>

      <p class="text-[11px] text-gray-400">
        게시물은 각 사이트가 제공하는 RSS/Atom 피드에서 최대 5건까지 가져와 보관합니다.
        폐쇄망 등으로 외부 접속이 막혀 있으면 내용 없이 링크만 표시됩니다.
      </p>
    </div>

    <RelatedSiteFormModal :open="formOpen" :edit-id="editId" :categories="categories.map(c => c.name)"
      @close="formOpen = false" @saved="onSaved" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { relatedSiteApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import RelatedSiteFormModal from './RelatedSiteFormModal.vue'

const auth = useAuthStore()
const canWrite = computed(() => auth.isManager && auth.canWrite('related_sites'))

const sites = ref([])
const loading = ref(true)
const refreshing = ref(false)
const busyId = ref(null)
const keyword = ref('')
const category = ref('')
const includeInactive = ref(false)
const formOpen = ref(false)
const editId = ref(null)

const categories = computed(() => {
  const map = new Map()
  for (const s of sites.value) {
    const c = s.category || '기타'
    map.set(c, (map.get(c) || 0) + 1)
  }
  return [...map.entries()].map(([name, count]) => ({ name, count }))
})

/** 검색은 사이트 정보뿐 아니라 가져온 게시물 제목·요약까지 훑는다 */
const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return sites.value
    .filter(s => !category.value || (s.category || '기타') === category.value)
    .filter(s => !kw || matches(s, kw))
})

function matches(s, kw) {
  const fields = [s.name, s.url, s.category, s.description, s.fetchedSummary]
  if (fields.some(v => (v || '').toLowerCase().includes(kw))) return true
  return (s.items || []).some(i =>
    (i.title || '').toLowerCase().includes(kw) || (i.summary || '').toLowerCase().includes(kw))
}

const lastFetchedLabel = computed(() => {
  const times = sites.value.map(s => s.lastFetchedAt).filter(Boolean).sort()
  return times.length ? formatDateTime(times[times.length - 1]) : ''
})

function hostOf(url) {
  try { return new URL(url).host } catch { return url }
}

function dateLabel(item) {
  if (item.publishedAt) return formatDate(item.publishedAt)
  return item.publishedText || ''
}

function formatDate(v) {
  const d = new Date(v)
  return isNaN(d) ? v : d.toLocaleDateString('ko-KR')
}

function formatDateTime(v) {
  const d = new Date(v)
  return isNaN(d) ? v : d.toLocaleString('ko-KR', { dateStyle: 'medium', timeStyle: 'short' })
}

const STATUS_TEXT = {
  FEED: '최신 게시물 수집됨',
  SUMMARY: '사이트 소개만 수집됨',
  EMPTY: '가져올 내용 없음',
  ERROR: '수집 실패',
  NONE: '수집 전',
}

function statusLabel(s) {
  const base = STATUS_TEXT[s.fetchStatus] || '수집 전'
  return s.lastFetchedAt ? `${base} · ${formatDateTime(s.lastFetchedAt)}` : base
}

function statusClass(status) {
  if (status === 'FEED') return 'text-green-600'
  if (status === 'SUMMARY') return 'text-gray-500'
  if (status === 'ERROR') return 'text-red-500'
  return 'text-gray-400'
}

function resetFilter() {
  keyword.value = ''
  category.value = ''
}

function openCreate() {
  editId.value = null
  formOpen.value = true
}

function openEdit(s) {
  editId.value = s.id
  formOpen.value = true
}

async function onSaved() {
  formOpen.value = false
  await load()
}

async function remove(s) {
  if (!confirm(`'${s.name}' 사이트를 삭제할까요?\n가져온 게시물도 함께 삭제됩니다.`)) return
  try {
    await relatedSiteApi.remove(s.id)
    await load()
  } catch (e) {
    alert(e?.message || e || '삭제에 실패했습니다.')
  }
}

async function refreshOne(s) {
  busyId.value = s.id
  try {
    const res = await relatedSiteApi.refresh(s.id)
    const updated = res.data || res
    const idx = sites.value.findIndex(x => x.id === s.id)
    if (idx >= 0) sites.value[idx] = updated
  } catch (e) {
    alert(e?.message || e || '새로고침에 실패했습니다.')
  } finally {
    busyId.value = null
  }
}

async function refreshAll() {
  refreshing.value = true
  try {
    const res = await relatedSiteApi.refreshAll()
    const result = res.data || res
    await load()
    alert(`새로고침 완료 — 사이트 ${result.total}건 중 ${result.succeeded}건 수집, ${result.failed}건 실패 (게시물 ${result.items}건)`)
  } catch (e) {
    alert(e?.message || e || '새로고침에 실패했습니다.')
  } finally {
    refreshing.value = false
  }
}

async function load() {
  loading.value = true
  try {
    const res = await relatedSiteApi.list({ activeOnly: !includeInactive.value })
    sites.value = res.data || res || []
  } catch (e) {
    sites.value = []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
