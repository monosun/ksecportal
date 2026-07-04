<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">위험 현황</h1>
        <p class="text-sm text-gray-400 mt-0.5">자산별 위험 평가 현황 및 분포를 확인합니다</p>
      </div>
      <RouterLink to="/risk-assessment" class="btn-primary px-4 py-2 text-sm rounded-xl">위험평가 관리 →</RouterLink>
    </div>

    <div class="page-body">

    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center py-20 text-gray-400">
      <svg class="animate-spin w-6 h-6 mr-2" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
      </svg>
      데이터 불러오는 중...
    </div>

    <template v-else>
      <!-- Stat Cards -->
      <div class="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <div class="card">
          <p class="text-sm text-gray-500">전체 위험</p>
          <p class="text-3xl font-bold text-gray-900 mt-1">{{ totalCount }}</p>
          <p class="text-xs text-gray-400 mt-2">최근 차수 기준</p>
        </div>
        <div class="card">
          <p class="text-sm text-gray-500">고위험</p>
          <p class="text-3xl font-bold text-red-600 mt-1">{{ highCount }}</p>
          <p class="text-xs text-gray-400 mt-2">즉각 조치 필요</p>
        </div>
        <div class="card">
          <p class="text-sm text-gray-500">중위험</p>
          <p class="text-3xl font-bold text-yellow-600 mt-1">{{ mediumCount }}</p>
          <p class="text-xs text-gray-400 mt-2">모니터링 필요</p>
        </div>
        <div class="card">
          <p class="text-sm text-gray-500">저위험</p>
          <p class="text-3xl font-bold text-green-600 mt-1">{{ lowCount }}</p>
          <p class="text-xs text-gray-400 mt-2">정기 검토</p>
        </div>
      </div>

      <!-- No data -->
      <div v-if="totalCount === 0" class="card text-center py-12 text-gray-400">
        <p class="text-sm">등록된 위험평가 데이터가 없습니다.</p>
        <RouterLink to="/risk-assessment" class="text-sm text-blue-500 hover:underline mt-1 inline-block">위험평가 관리에서 차수를 추가하세요 →</RouterLink>
      </div>

      <div v-else class="grid grid-cols-1 lg:grid-cols-2 gap-5 mb-5">
        <!-- Risk Heatmap -->
        <div class="card">
          <h2 class="text-sm font-semibold text-gray-800 mb-4">위험 히트맵 (발생가능성 × 영향도)</h2>
          <div class="flex gap-2">
            <div class="flex flex-col justify-between pb-6 pr-1">
              <span v-for="n in [5,4,3,2,1]" :key="n" class="text-xs text-gray-400 h-10 flex items-center font-medium">{{ n }}</span>
            </div>
            <div class="flex-1">
              <div class="grid" style="grid-template-columns: repeat(5, 1fr); gap: 3px;">
                <template v-for="row in heatmapRows" :key="row.y">
                  <div v-for="cell in row.cells" :key="cell.x"
                    class="h-10 rounded flex items-center justify-center text-xs font-bold transition-all hover:opacity-80 cursor-default"
                    :class="heatmapCellClass(cell.x, row.y)"
                    :title="`발생가능성 ${cell.x} × 영향도 ${row.y} = ${cell.x * row.y}`">
                    {{ cell.count > 0 ? cell.count : '' }}
                  </div>
                </template>
              </div>
              <div class="grid mt-1" style="grid-template-columns: repeat(5, 1fr);">
                <span v-for="n in [1,2,3,4,5]" :key="n" class="text-xs text-gray-400 text-center font-medium">{{ n }}</span>
              </div>
              <div class="flex justify-between mt-1 text-[10px] text-gray-400">
                <span>← 발생가능성 낮음</span>
                <span>높음 →</span>
              </div>
            </div>
          </div>
          <div class="flex items-center gap-3 mt-4 flex-wrap">
            <div v-for="l in heatmapLegend" :key="l.label" class="flex items-center gap-1.5">
              <div class="w-3 h-3 rounded" :class="l.bg"></div>
              <span class="text-xs text-gray-500">{{ l.label }}</span>
            </div>
          </div>
        </div>

        <!-- Recent High Risks -->
        <div class="card">
          <h2 class="text-sm font-semibold text-gray-800 mb-4">최근 고위험 항목</h2>
          <div v-if="recentHighRisks.length === 0" class="text-center py-8 text-sm text-gray-400">
            고위험 항목이 없습니다
          </div>
          <div v-else class="space-y-3">
            <div v-for="risk in recentHighRisks" :key="risk.id"
              class="flex items-center gap-3 p-3 rounded-xl bg-red-50 border border-red-100">
              <div class="w-10 h-10 rounded-lg bg-red-100 flex items-center justify-center flex-shrink-0">
                <svg class="w-5 h-5 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
                </svg>
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-sm font-semibold text-gray-900 truncate">{{ risk.asset }}</p>
                <p class="text-xs text-gray-500 truncate">{{ risk.threat }}</p>
              </div>
              <div class="text-right flex-shrink-0">
                <p class="text-lg font-bold text-red-600">{{ risk.score }}</p>
                <p class="text-xs text-gray-400">{{ risk.date }}</p>
              </div>
            </div>
          </div>
          <p class="text-xs text-gray-400 mt-4 text-center">위험평가 메뉴에서 상세 데이터를 관리하세요</p>
        </div>
      </div>
    </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { riskApi } from '@/api/index.js'

const loading = ref(true)
const assessments = ref([])
const recentHighRisks = ref([])

const heatmapRows = computed(() => {
  const map = {}
  for (const a of assessments.value) {
    const key = `${a.likelihood}-${a.impact}`
    map[key] = (map[key] || 0) + 1
  }
  const rows = []
  for (let y = 5; y >= 1; y--) {
    rows.push({
      y,
      cells: [1, 2, 3, 4, 5].map(x => ({ x, count: map[`${x}-${y}`] || 0 }))
    })
  }
  return rows
})

const totalCount = computed(() => assessments.value.length)
const highCount = computed(() => assessments.value.filter(a => a.riskGrade === 'HIGH').length)
const mediumCount = computed(() => assessments.value.filter(a => a.riskGrade === 'MEDIUM').length)
const lowCount = computed(() => assessments.value.filter(a => a.riskGrade === 'LOW').length)

function heatmapCellClass(x, y) {
  const score = x * y
  if (score >= 20) return 'bg-red-500 text-white'
  if (score >= 12) return 'bg-orange-400 text-white'
  if (score >= 6) return 'bg-yellow-300 text-yellow-900'
  return 'bg-green-200 text-green-800'
}

const heatmapLegend = [
  { label: '저위험 (1-5)', bg: 'bg-green-200' },
  { label: '중위험 (6-11)', bg: 'bg-yellow-300' },
  { label: '고위험 (12-19)', bg: 'bg-orange-400' },
  { label: '심각 (20-25)', bg: 'bg-red-500' }
]

onMounted(async () => {
  try {
    const years = (await riskApi.listYears())?.data || []
    if (!years || years.length === 0) return
    const latestYear = Math.max(...years)
    const rounds = (await riskApi.listRounds(latestYear))?.data || []
    if (!rounds || rounds.length === 0) return
    const latestRound = rounds[rounds.length - 1]
    assessments.value = (await riskApi.listAssessments(latestRound.id))?.data || []

    recentHighRisks.value = assessments.value
      .filter(a => a.riskGrade === 'HIGH')
      .slice(0, 5)
      .map(a => ({
        id: a.id,
        asset: a.assetName,
        threat: a.threatName,
        score: a.riskScore,
        date: a.createdAt ? a.createdAt.slice(0, 10) : ''
      }))
  } catch {
    // silent fail — empty state shown
  } finally {
    loading.value = false
  }
})
</script>
