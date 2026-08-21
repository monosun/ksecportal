<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">보안용어집</h1>
        <p class="text-sm text-gray-400 mt-0.5">
          보안 실무·교육에서 자주 쓰는 용어를 한글·영문·약어·분류·정의로 찾아봅니다
          <span v-if="summary" class="text-gray-300">· 총 {{ summary.total }}개 용어</span>
        </p>
      </div>
      <RouterLink v-if="isAdmin" to="/admin/codes" class="btn-secondary text-sm">
        용어 관리 (코드 관리) →
      </RouterLink>
    </div>

    <div class="page-body">
      <!-- 검색 -->
      <div class="card !p-4">
        <div class="flex gap-3 flex-wrap">
          <div class="relative flex-1 min-w-[240px]">
            <svg class="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
            </svg>
            <input v-model="keyword" type="text" class="input pl-9 w-full"
              placeholder="용어·영문·약어·정의·키워드 검색 (예: MFA, 암호화, phishing)" />
          </div>
          <button @click="expandAll = !expandAll" class="btn-secondary text-sm px-3 whitespace-nowrap"
            :title="expandAll ? '긴 정의를 4줄로 줄여 한눈에 봅니다' : '긴 정의를 모두 펼쳐서 봅니다'">
            {{ expandAll ? '정의 접기' : '정의 펼치기' }}
          </button>
          <button v-if="keyword || category" @click="resetFilter" class="btn-secondary text-sm px-3">초기화</button>
          <span class="flex items-center text-sm text-gray-500 whitespace-nowrap">{{ filtered.length }}건</span>
        </div>

        <!-- 분류 필터 -->
        <div v-if="categories.length" class="flex flex-wrap gap-1.5 mt-3 pt-3 border-t border-gray-100">
          <button
            class="px-2.5 py-1 rounded-full text-xs font-medium border transition-colors"
            :class="category === ''
              ? 'bg-primary-600 border-primary-600 text-white'
              : 'bg-white border-gray-200 text-gray-600 hover:bg-gray-50'"
            @click="category = ''">
            전체 <span class="tabular-nums">{{ terms.length }}</span>
          </button>
          <button v-for="c in categories" :key="c.category"
            class="px-2.5 py-1 rounded-full text-xs font-medium border transition-colors"
            :class="category === c.category
              ? 'bg-primary-600 border-primary-600 text-white'
              : 'bg-white border-gray-200 text-gray-600 hover:bg-gray-50'"
            @click="category = c.category">
            {{ c.category }} <span class="tabular-nums">{{ c.count }}</span>
          </button>
        </div>

        <!-- 약어 색인 -->
        <div v-if="abbrIndex.length" class="mt-3 pt-3 border-t border-gray-100">
          <div class="flex items-start gap-2 flex-wrap">
            <span class="text-[11px] font-semibold text-gray-500 py-1">약어 색인</span>
            <button v-for="a in abbrIndex" :key="a.id"
              class="px-1.5 py-0.5 rounded text-[11px] font-mono border border-gray-200 text-gray-600 hover:bg-primary-50 hover:border-primary-200 hover:text-primary-700"
              :title="`${a.abbreviation} — ${a.name}`"
              @click="keyword = a.abbreviation">
              {{ a.abbreviation }}
            </button>
          </div>
        </div>
      </div>

      <!-- 목록 -->
      <div v-if="loading" class="card text-center py-12 text-sm text-gray-400">불러오는 중...</div>
      <div v-else-if="filtered.length === 0" class="card text-center py-12">
        <p class="text-sm text-gray-500">검색 결과가 없습니다.</p>
        <p class="text-xs text-gray-400 mt-1">다른 검색어를 입력하거나 분류 필터를 해제해 보세요.</p>
      </div>

      <div v-else class="space-y-4">
        <!-- 분류별 묶어서 표시 (분류 필터 중이면 한 덩어리) -->
        <section v-for="g in grouped" :key="g.category" class="card !p-0 overflow-hidden">
          <div class="px-4 py-2.5 bg-gray-50 border-b border-gray-100 flex items-baseline justify-between">
            <h2 class="text-xs font-bold text-gray-700">{{ g.category }}</h2>
            <span class="text-[11px] text-gray-400 tabular-nums">{{ g.items.length }}개</span>
          </div>
          <!--
            정의는 화면 폭이 넓을수록 단 수를 늘려 채운다.
            한 줄로 길게 늘어놓으면 넓은 모니터에서 오른쪽이 비고 한 줄이 너무 길어져 읽기 어렵다.
          -->
          <div class="p-3 grid gap-2 grid-cols-1 md:grid-cols-2 xl:grid-cols-3 2xl:grid-cols-4">
            <article v-for="t in g.items" :key="t.id"
              class="flex flex-col rounded-lg border border-gray-100 px-3 py-2.5 hover:border-primary-200 hover:bg-primary-50/20 transition-colors">
              <div class="flex items-baseline gap-1.5 flex-wrap">
                <span class="text-sm font-bold text-gray-900">{{ t.name }}</span>
                <span v-if="t.abbreviation"
                  class="px-1.5 py-0.5 rounded bg-primary-50 text-primary-700 text-[11px] font-mono font-semibold">
                  {{ t.abbreviation }}
                </span>
                <span v-if="t.nameEn" class="text-xs text-gray-400 break-all">{{ t.nameEn }}</span>
              </div>
              <p v-if="t.definition"
                :class="['text-[13px] text-gray-600 mt-1.5 leading-relaxed break-words',
                  isOpen(t) ? '' : 'line-clamp-4']">{{ t.definition }}</p>
              <button v-if="t.definition && isLong(t.definition)" type="button"
                class="self-start mt-1 text-[11px] text-primary-600 hover:underline"
                @click="toggleDefinition(t.id)">
                {{ isOpen(t) ? '접기' : '더보기' }}
              </button>
              <div v-if="keywordList(t).length" class="flex flex-wrap gap-1 mt-auto pt-2">
                <button v-for="k in keywordList(t)" :key="k"
                  class="px-1.5 py-0.5 rounded bg-gray-50 border border-gray-100 text-[11px] text-gray-500 hover:bg-gray-100"
                  @click="keyword = k">
                  {{ k }}
                </button>
              </div>
            </article>
          </div>
        </section>
      </div>

      <p class="text-[11px] text-gray-400">
        용어 내용은 <strong>관리 &gt; 코드 관리 &gt; 용어집</strong> 탭에서 관리합니다.
        약어·키워드를 클릭하면 그 값으로 다시 검색합니다.
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { glossaryApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const isAdmin = computed(() => auth.isAdmin)

const terms = ref([])
const summary = ref(null)
const loading = ref(true)
const keyword = ref('')
const category = ref('')
/** 긴 정의는 기본 4줄까지만 보여 카드 높이를 맞추고, 필요할 때만 펼친다 */
const expandAll = ref(false)
const expanded = ref(new Set())

/** 4줄(카드 폭 기준 대략 100자)을 넘길 때만 더보기 버튼을 붙인다 */
function isLong(definition) {
  return (definition || '').length > 100
}

function isOpen(t) {
  return expandAll.value || expanded.value.has(t.id)
}

function toggleDefinition(id) {
  if (expanded.value.has(id)) expanded.value.delete(id)
  else expanded.value.add(id)
}

const categories = computed(() => summary.value?.byCategory || [])

/** 검색·분류 필터는 화면에서 처리한다 (수백 건 규모라 서버 왕복 없이 즉시 반응) */
const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return terms.value
    .filter(t => !category.value || t.category === category.value)
    .filter(t => !kw || [t.name, t.nameEn, t.abbreviation, t.category, t.definition, t.keywords]
      .some(v => (v || '').toLowerCase().includes(kw)))
})

const grouped = computed(() => {
  const map = new Map()
  for (const t of filtered.value) {
    const key = t.category || '미분류'
    if (!map.has(key)) map.set(key, [])
    map.get(key).push(t)
  }
  return [...map.entries()].map(([cat, items]) => ({ category: cat, items }))
})

/** 약어 색인 — 현재 필터 결과에서 약어가 있는 용어만 (중복 제거) */
const abbrIndex = computed(() => {
  const seen = new Set()
  return filtered.value
    .filter(t => t.abbreviation && !seen.has(t.abbreviation) && seen.add(t.abbreviation))
    .sort((a, b) => a.abbreviation.localeCompare(b.abbreviation))
})

function keywordList(t) {
  return (t.keywords || '').split(',').map(s => s.trim()).filter(Boolean)
}

function resetFilter() {
  keyword.value = ''
  category.value = ''
  expanded.value.clear()
}

async function load() {
  loading.value = true
  try {
    const [listRes, sumRes] = await Promise.all([
      glossaryApi.list({ activeOnly: true }),
      glossaryApi.summary(),
    ])
    terms.value = listRes.data || listRes || []
    summary.value = sumRes.data || sumRes || null
  } catch (e) {
    terms.value = []
    summary.value = null
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
