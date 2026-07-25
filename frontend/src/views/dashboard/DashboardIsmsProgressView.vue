<template>
  <!-- 한 화면(스크롤 없이)에 담기도록 설계한 레이아웃.
       상단 요약 1줄 + 하단 도메인 3열(섹션별)로 고정하고, 여백·글자 크기를 축소한다. -->
  <div class="p-4 sm:p-6 viz">
    <div class="flex items-center justify-between gap-3 flex-wrap mb-3">
      <div>
        <h1 class="text-xl font-bold text-gray-900 tracking-tight">ISMS-P 이행률</h1>
        <p class="text-xs text-gray-500 mt-0.5">
          {{ year }}년도 ISMS-P 인증 통제항목 이행 현황
          <span v-if="summary" class="text-gray-400">· 전체 {{ summary.totalItems }}개 항목</span>
        </p>
      </div>
      <div class="flex gap-2">
        <button @click="load" :disabled="loading" class="btn-secondary flex items-center gap-2 text-sm">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
          </svg>
          {{ loading ? '집계 중...' : '새로고침' }}
        </button>
        <RouterLink to="/isms" class="btn-primary px-4 py-2 text-sm rounded-xl">증적관리 →</RouterLink>
      </div>
    </div>

    <div v-if="loading" class="card text-center py-16 text-gray-400 text-sm">데이터 불러오는 중...</div>
    <div v-else-if="!summary" class="card text-center py-16 text-gray-400 text-sm">집계 데이터가 없습니다.</div>

    <template v-else>
      <!-- ── 요약: 게이지 + 상태별 건수 (기존 '전체 이행률'·'통제항목 현황' 두 카드를 한 줄로 통합) ── -->
      <section class="card !p-4 mb-3">
        <div class="flex items-center gap-5 flex-wrap">
          <!-- 전체 이행률 게이지 -->
          <div class="flex items-center gap-3 shrink-0">
            <div class="w-20 h-20 rounded-full flex items-center justify-center"
              :style="{ background: `conic-gradient(${overallColor} ${overallRate * 3.6}deg, #E5E7EB 0deg)` }"
              :title="`전체 이행률 ${overallRate}% (준수 ${summary.compliant}/평가대상 ${effectiveTotal}건)`">
              <div class="w-[62px] h-[62px] bg-white rounded-full flex items-center justify-center">
                <span class="text-lg font-black tabular-nums" :style="{ color: overallColor }">{{ overallRate }}%</span>
              </div>
            </div>
            <div class="leading-tight">
              <p class="text-xs font-bold text-gray-700">전체 이행률</p>
              <p class="text-[11px] text-gray-400 mt-0.5 tabular-nums">
                준수 {{ summary.compliant }} / 평가대상 {{ effectiveTotal }}건
              </p>
              <p class="text-[11px] text-gray-400 tabular-nums">해당없음(NA) {{ summary.na }}건 제외</p>
            </div>
          </div>

          <!-- 상태별 구성 막대 + 상태별 건수 타일 -->
          <div class="flex-1 min-w-[320px]">
            <div class="flex gap-0.5 h-3 mb-2">
              <div v-for="s in visibleSegments" :key="s.label"
                class="h-full first:rounded-l-full last:rounded-r-full"
                :style="{ width: `${s.value / summary.totalItems * 100}%`, background: s.color }"
                :title="`${s.label} ${s.value}건 (${Math.round(s.value / summary.totalItems * 100)}%)`"></div>
            </div>
            <div class="grid grid-cols-3 sm:grid-cols-5 gap-x-4 gap-y-2">
              <div v-for="s in statusSegments" :key="s.label" class="flex items-center gap-1.5">
                <span class="w-2 h-2 rounded-sm shrink-0" :style="{ background: s.color }"></span>
                <span class="text-[11px] text-gray-500">{{ s.label }}</span>
                <span class="text-sm font-bold text-gray-900 tabular-nums ml-auto">{{ s.value }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ── 도메인별 이행률 — 섹션(1·2·3)별 3열. 21개 도메인이 세로로 흐르지 않도록 고정 ── -->
      <div v-if="sections.length === 0" class="card !p-4 text-center py-10 text-sm text-gray-400">
        도메인 데이터가 없습니다
      </div>
      <div v-else class="grid grid-cols-1 lg:grid-cols-3 gap-3 items-start">
        <section v-for="sec in sections" :key="sec.num" class="card !p-4">
          <div class="flex items-baseline justify-between gap-2 mb-2.5">
            <h2 class="text-xs font-bold text-gray-700 truncate">{{ sec.num }}. {{ sec.name }}</h2>
            <span class="text-xs font-bold tabular-nums shrink-0" :style="{ color: rateColor(sec.rate, sec.effective) }">
              {{ sec.effective > 0 ? `${sec.rate}%` : '—' }}
            </span>
          </div>

          <div class="space-y-2">
            <button v-for="d in sec.domains" :key="d.code" type="button"
              class="w-full text-left group focus:outline-none focus:ring-2 focus:ring-primary-500 rounded-md"
              :title="`${d.code} ${d.name} — 준수 ${d.compliant}/${d.effective}건${d.na ? ` (NA ${d.na}건 제외)` : ''}`"
              @click="goToDomain(d.code)">
              <div class="flex items-baseline gap-1.5">
                <span class="text-[11px] font-semibold text-gray-400 tabular-nums shrink-0 w-8">{{ d.code }}</span>
                <span class="text-[11px] text-gray-700 truncate group-hover:text-primary-600">{{ d.name }}</span>
                <span class="text-[11px] text-gray-400 tabular-nums ml-auto shrink-0">
                  {{ d.compliant }}/{{ d.effective }}
                </span>
                <span class="text-[11px] font-bold tabular-nums shrink-0 w-8 text-right"
                  :style="{ color: rateColor(d.rate, d.effective) }">
                  {{ d.effective > 0 ? `${d.rate}%` : '—' }}
                </span>
              </div>
              <div class="mt-1 h-1.5 rounded-full bg-gray-100 overflow-hidden">
                <div class="h-full rounded-full transition-[width] duration-500"
                  :style="{ width: `${d.rate}%`, background: rateColor(d.rate, d.effective) }"></div>
              </div>
            </button>
          </div>
        </section>
      </div>

      <p class="text-[11px] text-gray-400 mt-2">
        이행률 = 준수 ÷ (전체 − 해당없음). 도메인을 클릭하면 증적관리에서 해당 도메인이 선택됩니다.
      </p>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { ismsApi } from '@/api/index.js'

const router = useRouter()
const loading = ref(true)
const summary = ref(null)
const year = new Date().getFullYear()

// ISMS-P 인증기준 3개 섹션. 도메인 코드 앞자리(sectionNum)로 묶는다.
const SECTION_NAMES = {
  1: '관리체계 수립 및 운영',
  2: '보호대책 요구사항',
  3: '개인정보 처리단계별 요구사항',
}

// 상태색은 상태 표현 전용 — 이행률 구간(양호/주의/미흡)에만 쓴다.
const C = {
  good: 'var(--good)', warning: 'var(--warning)', critical: 'var(--critical)',
  neutral: 'var(--neutral)', partial: 'var(--serious)', noEvidence: 'var(--noevi)',
}

function rateColor(rate, effective) {
  if (!effective) return C.neutral
  return rate >= 80 ? C.good : rate >= 60 ? C.warning : C.critical
}

/** 이행률 = 준수 ÷ (전체 − 해당없음). NA 는 평가 대상에서 제외한다. */
function rateOf(compliant, total, na) {
  const effective = (total || 0) - (na || 0)
  return effective > 0 ? Math.round((compliant || 0) / effective * 100) : 0
}

const effectiveTotal = computed(() =>
  summary.value ? (summary.value.totalItems || 0) - (summary.value.na || 0) : 0)

const overallRate = computed(() =>
  summary.value ? rateOf(summary.value.compliant, summary.value.totalItems, summary.value.na) : 0)

const overallColor = computed(() => rateColor(overallRate.value, effectiveTotal.value))

/** 전체 항목의 상태 구성 — 요약 막대와 범례가 같은 데이터를 쓴다 */
const statusSegments = computed(() => {
  const s = summary.value
  if (!s) return []
  return [
    { label: '준수',        value: s.compliant || 0,    color: C.good },
    { label: '부분 준수',   value: s.partial || 0,      color: C.warning },
    { label: '미준수',      value: s.nonCompliant || 0, color: C.critical },
    { label: '증적 미제출', value: s.noEvidence || 0,   color: C.noEvidence },
    { label: '해당없음',    value: s.na || 0,           color: C.neutral },
  ]
})

/** 요약 막대에는 0건 상태를 그리지 않는다(0폭 조각이 모서리 라운딩을 먹는 것 방지) */
const visibleSegments = computed(() => statusSegments.value.filter(s => s.value > 0))

/** 도메인을 섹션별로 묶는다. byDomain 은 백엔드에서 sortOrder 순으로 오므로 순서를 그대로 쓴다. */
const sections = computed(() => {
  const byDomain = summary.value?.byDomain
  if (!byDomain?.length) return []

  const grouped = new Map()
  for (const d of byDomain) {
    // sectionNum 이 비어 있으면 도메인 코드 앞자리로 보정한다.
    const num = d.sectionNum || parseInt(String(d.domainCode || '').split('.')[0], 10) || 0
    if (!grouped.has(num)) grouped.set(num, [])
    grouped.get(num).push({
      code: d.domainCode,
      name: d.domainName || d.domainCode,
      rate: rateOf(d.compliant, d.total, d.na),
      compliant: d.compliant || 0,
      effective: (d.total || 0) - (d.na || 0),
      total: d.total || 0,
      na: d.na || 0,
    })
  }

  return [...grouped.entries()]
    .sort((a, b) => a[0] - b[0])
    .map(([num, domains]) => {
      const compliant = domains.reduce((a, d) => a + d.compliant, 0)
      const effective = domains.reduce((a, d) => a + d.effective, 0)
      return {
        num,
        name: SECTION_NAMES[num] || `섹션 ${num}`,
        domains,
        effective,
        rate: effective > 0 ? Math.round(compliant / effective * 100) : 0,
      }
    })
})

function goToDomain(code) {
  router.push({ path: '/isms', query: { domain: code } })
}

async function load() {
  loading.value = true
  try {
    summary.value = (await ismsApi.summary(year))?.data || null
  } catch {
    // silent fail — 요약 조회 실패 시 빈 상태로 표시한다
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
/* 그래프 색 — 역할별로만 정의한다(개인정보 현황보고서와 동일한 토큰). */
.viz {
  --good: #0ca30c;
  --warning: #fab219;
  --serious: #ec835a;
  --critical: #d03b3b;
  --neutral: #b9b8b2;
  --noevi: #9aa3b0;
}
</style>
