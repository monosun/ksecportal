<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">법령준수관리</h1>
        <p class="text-sm text-gray-400 mt-0.5">업종별 적용 법령을 확인하고 국가법령정보센터에서 전문을 조회합니다.</p>
      </div>
      <div class="flex items-center gap-2">
        <!-- 법령 검토 버튼 -->
        <button @click="showReviewModal = true"
          class="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-primary-600 hover:bg-primary-700 text-white text-sm font-semibold transition-all shadow-sm">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
          </svg>
          법령 검토{{ checkedCount > 0 ? ` (${checkedCount}건)` : '' }}
        </button>
      </div>
    </div>

    <div class="page-body">
      <!-- 필터 바 -->
      <div class="card">
        <div class="flex flex-wrap gap-3 items-center">
          <!-- 검색 -->
          <div class="relative flex-1 min-w-[200px]">
            <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none"
              fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
            </svg>
            <input
              v-model="searchInput"
              type="text"
              class="input pl-9 w-full text-sm"
              placeholder="업종명 또는 법령명 검색..."
              @input="onSearchInput"
            />
          </div>

          <!-- 카테고리 필터 -->
          <div class="flex flex-wrap gap-1.5">
            <button @click="selectedCategory = ''"
              class="px-3 py-1.5 rounded-lg border text-xs font-semibold transition-all"
              :class="selectedCategory === '' ? 'border-primary-500 bg-primary-50 text-primary-600' : 'border-gray-200 text-gray-500 hover:border-gray-300'">
              전체
            </button>
            <button v-for="cat in CATEGORIES" :key="cat.key"
              @click="selectedCategory = cat.key"
              class="px-3 py-1.5 rounded-lg border text-xs font-semibold transition-all"
              :class="selectedCategory === cat.key ? 'border-primary-500 bg-primary-50 text-primary-600' : 'border-gray-200 text-gray-500 hover:border-gray-300'">
              {{ cat.label }}
            </button>
          </div>

          <!-- 우리 업종만 토글 -->
          <button v-if="companyIndustryIds.length > 0"
            @click="showMyIndustryOnly = !showMyIndustryOnly"
            class="px-3 py-1.5 rounded-lg border text-xs font-semibold transition-all flex items-center gap-1.5"
            :class="showMyIndustryOnly
              ? 'border-primary-500 bg-primary-50 text-primary-600'
              : 'border-gray-200 text-gray-500 hover:border-gray-300'">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/>
            </svg>
            우리 업종만
          </button>

          <span class="text-xs text-gray-400 ml-auto">{{ filteredIndustries.length }}개 업종</span>
        </div>
      </div>

      <!-- 전체 선택 / 해제 -->
      <div v-if="filteredIndustries.length > 0" class="flex items-center gap-3 px-1">
        <button @click="selectAll" class="text-xs text-primary-600 hover:text-primary-800 font-medium">전체 선택</button>
        <span class="text-gray-300">|</span>
        <button @click="clearAll" class="text-xs text-gray-500 hover:text-gray-700 font-medium">전체 해제</button>
        <span v-if="checkedCount > 0" class="text-xs text-gray-400">{{ checkedCount }}건 선택됨</span>
      </div>

      <!-- 업종 목록 -->
      <div class="space-y-2">
        <div v-for="industry in filteredIndustries" :key="industry.id"
          class="card transition-all hover:shadow-md p-0 overflow-hidden">

          <!-- 업종 헤더 행 -->
          <div class="flex items-center gap-3 px-4 py-3">
            <!-- 업종 전체 선택 체크박스 -->
            <input
              type="checkbox"
              class="w-4 h-4 rounded text-primary-500 cursor-pointer flex-shrink-0"
              :checked="isIndustryAllChecked(industry)"
              :indeterminate="isIndustryPartialChecked(industry)"
              @change="toggleIndustryAll(industry, $event.target.checked)"
            />
            <!-- 카테고리 뱃지 -->
            <span class="text-[11px] font-bold px-2 py-0.5 rounded-full flex-shrink-0"
              :class="categoryColor(industry.category)">
              {{ categoryLabel(industry.category) }}
            </span>
            <!-- 업종명 -->
            <h3 class="text-sm font-bold text-gray-800 flex-1 min-w-0">{{ industry.name }}</h3>
            <!-- 법령 수 뱃지 -->
            <span class="text-xs text-gray-400 flex-shrink-0">{{ visibleLaws(industry).length }}개 법령</span>
            <!-- 펼치기 버튼 -->
            <button @click="toggleExpand(industry.id)"
              class="flex-shrink-0 p-1 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100 transition-colors">
              <svg class="w-4 h-4 transition-transform duration-200"
                :class="expanded.has(industry.id) ? 'rotate-180' : ''"
                fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
              </svg>
            </button>
          </div>

          <!-- 법령 태그 (접힌 상태) -->
          <div v-if="!expanded.has(industry.id)" class="px-4 pb-3 flex flex-wrap gap-1.5">
            <span v-for="law in visibleLaws(industry)" :key="law.name"
              class="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-xs font-medium"
              :class="checkedKey(industry.id, law.name) in checkedLaws
                ? 'bg-primary-100 text-primary-700 border border-primary-300'
                : 'bg-gray-100 text-gray-600 border border-gray-200'">
              {{ law.name }}
            </span>
          </div>

          <!-- 법령 상세 테이블 (펼쳐진 상태) -->
          <div v-if="expanded.has(industry.id)" class="border-t border-gray-100">
            <table class="w-full text-xs">
              <thead>
                <tr class="bg-gray-50 text-left text-gray-500 font-semibold">
                  <th class="px-4 py-2 w-8">
                    <input type="checkbox" class="w-3.5 h-3.5 rounded text-primary-500 cursor-pointer"
                      :checked="isIndustryAllChecked(industry)"
                      :indeterminate="isIndustryPartialChecked(industry)"
                      @change="toggleIndustryAll(industry, $event.target.checked)" />
                  </th>
                  <th class="px-2 py-2 w-16">구분</th>
                  <th class="px-2 py-2">법령명</th>
                  <th class="px-2 py-2 w-36">소관부처</th>
                  <th class="px-4 py-2 w-24">바로가기</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-50">
                <tr v-for="law in visibleLaws(industry)" :key="law.name"
                  class="hover:bg-gray-50 transition-colors cursor-pointer"
                  @click="toggleLaw(industry.id, law.name)">
                  <td class="px-4 py-2.5">
                    <input type="checkbox" class="w-3.5 h-3.5 rounded text-primary-500 cursor-pointer"
                      :checked="checkedKey(industry.id, law.name) in checkedLaws"
                      @click.stop
                      @change="toggleLaw(industry.id, law.name)" />
                  </td>
                  <td class="px-2 py-2.5">
                    <span class="px-1.5 py-0.5 rounded text-[10px] font-semibold"
                      :class="typeColor(law.type)">{{ law.type }}</span>
                  </td>
                  <td class="px-2 py-2.5 font-medium text-gray-800">{{ law.name }}</td>
                  <td class="px-2 py-2.5 text-gray-500">{{ law.ministry }}</td>
                  <td class="px-4 py-2.5" @click.stop>
                    <a v-if="law.url" :href="law.url" target="_blank" rel="noopener noreferrer"
                      class="inline-flex items-center gap-1 text-blue-600 hover:text-blue-800 font-medium">
                      법령 보기
                      <svg class="w-3 h-3 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                          d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"/>
                      </svg>
                    </a>
                    <span v-else class="text-gray-400">-</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div v-if="filteredIndustries.length === 0" class="card text-center py-12 text-sm text-gray-400">
          검색 결과가 없습니다.
        </div>
      </div>
    </div>

    <!-- 법령 검토 모달 -->
    <LegalReviewModal
      v-if="showReviewModal"
      :laws="selectedLawsForReview"
      @close="showReviewModal = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { INDUSTRIES, CATEGORIES } from '@/data/legalIndustries.js'
import { appSettingApi } from '@/api/index.js'
import LegalReviewModal from '@/components/LegalReviewModal.vue'

// ── 법령 검토 모달 ────────────────────────────────
const showReviewModal = ref(false)

const selectedLawsForReview = computed(() => {
  const result = []
  const seen = new Set()
  for (const ind of industriesData.value) {
    for (const law of ind.laws) {
      if (!(checkedKey(ind.id, law.name) in checkedLaws.value)) continue
      if (!seen.has(law.name)) {
        seen.add(law.name)
        result.push(law)
      }
    }
  }
  return result
})

// ── 필터 상태 ─────────────────────────────────────
const searchInput      = ref('')
const searchQuery      = ref('')
const selectedCategory = ref('')
const showMyIndustryOnly = ref(false)
const companyIndustryIds = ref([])
// 업종별 개별 법령 선택 { [업종id]: [법령명, ...] }. 항목 없으면 전체 법령.
const companyIndustryLaws = ref({})
// 설정관리>업종설정에서 검색으로 추가한 업종별 법령 { [업종id]: [{name,type,ministry,url}] }
const customLaws = ref({})

// 정적 업종 법령 + 사용자 추가 법령 병합
const industriesData = computed(() =>
  INDUSTRIES.map(ind => {
    const extra = customLaws.value[ind.id]
    return extra?.length ? { ...ind, laws: [...ind.laws, ...extra] } : ind
  })
)

// "우리 업종만 보기"가 켜져 있고 해당 업종에 개별 법령 설정이 있으면 선택된 법령만 노출
function visibleLaws(industry) {
  if (!showMyIndustryOnly.value) return industry.laws
  const sel = companyIndustryLaws.value[industry.id]
  if (!Array.isArray(sel)) return industry.laws
  return industry.laws.filter(l => sel.includes(l.name))
}

let searchTimer = null
function onSearchInput() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { searchQuery.value = searchInput.value }, 200)
}

// ── 펼치기 상태 ──────────────────────────────────
const expanded = ref(new Set())
function toggleExpand(id) {
  const s = new Set(expanded.value)
  if (s.has(id)) s.delete(id)
  else s.add(id)
  expanded.value = s
}

// ── 체크박스 상태 ─────────────────────────────────
const checkedLaws = ref({})

function checkedKey(industryId, lawName) {
  return `${industryId}__${lawName}`
}

function toggleLaw(industryId, lawName) {
  const key = checkedKey(industryId, lawName)
  const next = { ...checkedLaws.value }
  if (key in next) delete next[key]
  else next[key] = true
  checkedLaws.value = next
}

function isIndustryAllChecked(industry) {
  const laws = visibleLaws(industry)
  return laws.length > 0 && laws.every(l => checkedKey(industry.id, l.name) in checkedLaws.value)
}

function isIndustryPartialChecked(industry) {
  const laws = visibleLaws(industry)
  const some = laws.some(l => checkedKey(industry.id, l.name) in checkedLaws.value)
  return some && !isIndustryAllChecked(industry)
}

function toggleIndustryAll(industry, checked) {
  const next = { ...checkedLaws.value }
  for (const law of visibleLaws(industry)) {
    const key = checkedKey(industry.id, law.name)
    if (checked) next[key] = true
    else delete next[key]
  }
  checkedLaws.value = next
}

const checkedCount = computed(() => Object.keys(checkedLaws.value).length)

function selectAll() {
  const next = {}
  for (const ind of filteredIndustries.value) {
    for (const law of visibleLaws(ind)) {
      next[checkedKey(ind.id, law.name)] = true
    }
  }
  checkedLaws.value = next
}
function clearAll() { checkedLaws.value = {} }

// ── 필터링 ─────────────────────────────────────────
const filteredIndustries = computed(() => {
  const q   = searchQuery.value.trim()
  const cat = selectedCategory.value
  const myIds = companyIndustryIds.value

  return industriesData.value.filter(ind => {
    // 우리 업종만 필터
    if (showMyIndustryOnly.value && myIds.length > 0 && !myIds.includes(ind.id)) return false
    // 카테고리 필터
    if (cat && ind.category !== cat) return false
    // 검색어 필터
    if (q) {
      const nameMatch = ind.name.includes(q)
      const lawMatch  = ind.laws.some(l => l.name.includes(q) || (l.ministry && l.ministry.includes(q)))
      if (!nameMatch && !lawMatch) return false
    }
    return true
  })
})

// ── 유틸 ──────────────────────────────────────────
function categoryLabel(key) {
  return CATEGORIES.find(c => c.key === key)?.label || key
}
function categoryColor(key) {
  return CATEGORIES.find(c => c.key === key)?.color || 'bg-gray-100 text-gray-600'
}
function typeColor(type) {
  return {
    '법령':   'bg-blue-100 text-blue-700',
    '시행령': 'bg-indigo-100 text-indigo-700',
    '시행규칙': 'bg-violet-100 text-violet-700',
    '시행세칙': 'bg-purple-100 text-purple-700',
    '규정':   'bg-teal-100 text-teal-700',
    '고시':   'bg-amber-100 text-amber-700',
    '지침·가이드': 'bg-orange-100 text-orange-700',
    '국제기준': 'bg-rose-100 text-rose-700',
  }[type] || 'bg-gray-100 text-gray-600'
}

// ── 마운트: 업종 설정 로드 ──────────────────────────
onMounted(async () => {
  try {
    const res = await appSettingApi.getAll()
    const raw = res.data?.['company.industries']
    if (raw) {
      const ids = JSON.parse(raw)
      if (Array.isArray(ids) && ids.length > 0) {
        companyIndustryIds.value = ids
        showMyIndustryOnly.value = true
      }
    }
    const rawLaws = res.data?.['company.industryLaws']
    if (rawLaws) {
      const m = JSON.parse(rawLaws)
      if (m && typeof m === 'object') companyIndustryLaws.value = m
    }
    const rawCustom = res.data?.['company.customLaws']
    if (rawCustom) {
      const c = JSON.parse(rawCustom)
      if (c && typeof c === 'object') customLaws.value = c
    }
  } catch {}
})
</script>
