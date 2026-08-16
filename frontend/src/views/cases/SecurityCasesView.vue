<template>
  <div class="p-8">
    <div class="flex items-start justify-between mb-6 gap-4">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">알기쉬운 보안사례집</h1>
        <p class="text-sm text-gray-500 mt-1">
          어려운 보안 개념을 실제 업무 상황으로 풀어 놓은 사례 모음입니다. 항목을 누르면 전체 내용을 볼 수 있습니다.
        </p>
      </div>
      <div class="relative w-64 flex-shrink-0">
        <input v-model="keyword" type="text" class="input w-full pr-8 text-sm" placeholder="사례 검색..." />
        <svg class="w-4 h-4 text-gray-300 absolute right-2.5 top-1/2 -translate-y-1/2 pointer-events-none"
          fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M21 21l-4.35-4.35M17 11A6 6 0 1 1 5 11a6 6 0 0 1 12 0z"/>
        </svg>
      </div>
    </div>

    <!-- 탭 -->
    <div class="flex gap-1 mb-5 border-b border-gray-200">
      <button v-for="t in tabs" :key="t.value" @click="tab = t.value"
        :class="['px-4 py-2.5 text-sm font-medium border-b-2 -mb-px transition-colors',
                 tab === t.value
                   ? 'border-primary-600 text-primary-700'
                   : 'border-transparent text-gray-500 hover:text-gray-700']">
        {{ t.label }}
        <span v-if="docs[t.value].items.length"
          class="ml-1.5 text-[11px] text-gray-400">{{ docs[t.value].items.length }}</span>
      </button>
    </div>

    <div v-if="doc.loading" class="card text-center text-gray-400 py-12">불러오는 중...</div>
    <div v-else-if="doc.error" class="card text-center text-red-500 py-12">
      사례집을 불러오지 못했습니다. ({{ doc.error }})
    </div>

    <template v-else>
      <!-- 머리말 -->
      <div v-if="doc.intro && !keyword" class="card mb-5 bg-primary-50/40 border-primary-100">
        <MarkdownView :content="doc.intro" :breaks="false" />
      </div>

      <p v-if="keyword" class="text-xs text-gray-500 mb-3">
        <b class="text-gray-700">{{ filteredItems.length }}건</b> 검색됨
      </p>

      <div v-if="!filteredItems.length" class="card text-center text-gray-400 py-12">
        검색 결과가 없습니다.
      </div>

      <!-- 그룹별 카드 목록 -->
      <div v-for="g in groupedItems" :key="g.title || '_'" class="mb-7 last:mb-0">
        <div v-if="g.title" class="mb-3">
          <h2 class="text-sm font-bold text-gray-700">{{ g.title }}</h2>
          <p v-if="g.intro" class="text-xs text-gray-400 mt-1 line-clamp-2">{{ g.intro }}</p>
        </div>
        <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-3">
          <button v-for="item in g.items" :key="item.id"
            class="card text-left hover:border-primary-300 hover:shadow-md transition-all group flex flex-col"
            @click="openItem(item)">
            <div class="flex items-start gap-2 mb-2">
              <span v-if="item.label"
                class="flex-shrink-0 px-1.5 py-0.5 rounded-md bg-primary-50 text-primary-700 text-[11px] font-bold">
                {{ item.label }}
              </span>
              <span class="text-sm font-semibold text-gray-900 leading-6 group-hover:text-primary-700">
                {{ item.text }}
              </span>
            </div>
            <p class="text-xs text-gray-500 leading-5 line-clamp-3 flex-1">{{ item.summary }}</p>
            <span class="mt-3 text-[11px] text-gray-400 group-hover:text-primary-600">자세히 보기 →</span>
          </button>
        </div>
      </div>
    </template>

    <!-- 상세 팝업 -->
    <div v-if="selected" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4"
      @click.self="selected = null">
      <div class="bg-white rounded-2xl shadow-2xl w-full max-w-3xl max-h-[92vh] flex flex-col">
        <div class="flex items-start justify-between gap-4 px-6 py-4 border-b border-gray-100">
          <div class="min-w-0">
            <p v-if="selected.group" class="text-[11px] text-gray-400 mb-0.5">{{ selected.group }}</p>
            <h2 class="text-lg font-bold text-gray-900 leading-7">
              <span v-if="selected.label" class="text-primary-600">{{ selected.label }}.</span>
              {{ selected.text }}
            </h2>
          </div>
          <button class="text-gray-300 hover:text-gray-600 transition-colors flex-shrink-0" @click="selected = null">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>

        <div class="flex-1 overflow-y-auto px-6 py-5">
          <MarkdownView :content="selected.body" :breaks="false" />
        </div>

        <!-- 앞뒤 사례로 바로 이동 -->
        <div class="flex items-center justify-between gap-3 px-6 py-3 border-t border-gray-100 bg-gray-50 rounded-b-2xl">
          <button class="btn-secondary text-xs" :disabled="selectedIndex <= 0" @click="move(-1)">‹ 이전 사례</button>
          <span class="text-[11px] text-gray-400">{{ selectedIndex + 1 }} / {{ filteredItems.length }}</span>
          <button class="btn-secondary text-xs" :disabled="selectedIndex >= filteredItems.length - 1" @click="move(1)">
            다음 사례 ›
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import MarkdownView from '@/components/MarkdownView.vue'
import { parseCaseDoc } from '@/utils/markdownCases'

// 본문은 docs/ 의 마크다운을 그대로 싣는다(프론트 이미지 빌드 때 public/help 로 복사된다).
const tabs = [
  { value: 'basic',     label: '어려운 보안, 쉽게 이해하기', url: '/help/security-cases-basic.md' },
  { value: 'executive', label: '경영진 보안 투자 사례',     url: '/help/security-cases-executive.md' }
]
const tab = ref('basic')
const keyword = ref('')
const selected = ref(null)

const docs = reactive(Object.fromEntries(
  tabs.map(t => [t.value, { loading: true, error: '', intro: '', items: [], groupIntros: {} }])
))
const doc = computed(() => docs[tab.value])

const filteredItems = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  if (!q) return doc.value.items
  return doc.value.items.filter(i => i.keywords.includes(q))
})

/**
 * 카드를 그룹(Part 등) 단위로 묶는다. 그룹이 없는 문서는 한 덩어리로 나온다.
 *
 * <p>같은 이름끼리 모으지 않고 <b>이어진 항목만</b> 묶는다. 그룹이 없는 항목(Part 5·6 처럼
 * 하위 머리글이 없는 장)을 이름으로 모으면 문서 뒤쪽 내용이 맨 앞 무그룹 묶음으로 끌려 올라가
 * Part 순서가 뒤바뀐다.
 */
const groupedItems = computed(() => {
  const groups = []
  for (const item of filteredItems.value) {
    const title = item.group || ''
    const last = groups[groups.length - 1]
    if (!last || last.title !== title) {
      groups.push({ title, intro: doc.value.groupIntros[title] || '', items: [item] })
    } else {
      last.items.push(item)
    }
  }
  return groups
})

const selectedIndex = computed(() =>
  selected.value ? filteredItems.value.findIndex(i => i.id === selected.value.id) : -1)

function openItem(item) { selected.value = item }

function move(step) {
  const next = filteredItems.value[selectedIndex.value + step]
  if (next) selected.value = next
}

async function loadDoc(t) {
  const state = docs[t.value]
  state.loading = true
  state.error = ''
  try {
    const res = await fetch(t.url, { cache: 'no-cache' })
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    const parsed = parseCaseDoc(await res.text())
    Object.assign(state, parsed)
  } catch (e) {
    state.error = e?.message || String(e)
  } finally {
    state.loading = false
  }
}

// 팝업에서 ←/→ 로 앞뒤 사례를 넘긴다. Esc 로 닫는다.
function onKey(e) {
  if (!selected.value) return
  if (e.key === 'Escape') { selected.value = null; return }
  if (e.key === 'ArrowLeft') move(-1)
  if (e.key === 'ArrowRight') move(1)
}

onMounted(() => {
  tabs.forEach(loadDoc)
  document.addEventListener('keydown', onKey)
})
onBeforeUnmount(() => document.removeEventListener('keydown', onKey))
</script>
