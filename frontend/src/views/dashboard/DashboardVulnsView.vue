<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">취약점 현황</h1>
        <p class="text-sm text-gray-400 mt-0.5">취약점 처리 현황 및 심각도별 분포를 확인합니다</p>
      </div>
      <RouterLink to="/vulnerabilities" class="btn-primary px-4 py-2 text-sm rounded-xl">취약점 관리 →</RouterLink>
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
        <!-- 통계 카드 -->
        <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <div v-for="s in stats" :key="s.label" class="card">
            <p class="text-xs font-semibold text-gray-500 mb-1">{{ s.label }}</p>
            <p class="text-3xl font-black" :class="s.color">{{ s.value }}</p>
            <p class="text-xs text-gray-400 mt-0.5">{{ s.sub }}</p>
          </div>
        </div>

        <!-- 심각도별 분포 -->
        <div class="card">
          <h2 class="text-sm font-bold text-gray-800 mb-4">심각도별 미처리 취약점</h2>
          <div v-if="severities.every(s => s.count === 0)" class="text-center py-6 text-sm text-gray-400">
            미처리 취약점이 없습니다
          </div>
          <div v-else class="space-y-3">
            <div v-for="s in severities" :key="s.label" class="flex items-center gap-3">
              <span class="w-16 text-xs font-semibold text-right" :class="s.textColor">{{ s.label }}</span>
              <div class="flex-1 bg-gray-100 rounded-full h-3 overflow-hidden">
                <div class="h-3 rounded-full transition-all" :class="s.barColor"
                  :style="{ width: maxSeverity > 0 ? `${(s.count / maxSeverity) * 100}%` : '0%' }"></div>
              </div>
              <span class="w-8 text-xs font-bold text-gray-700 text-right">{{ s.count }}</span>
            </div>
          </div>
        </div>

        <!-- 기한 초과 취약점 -->
        <div class="card">
          <h2 class="text-sm font-bold text-gray-800 mb-4">기한 초과 취약점 ({{ overdueVulns.length }}건)</h2>
          <div v-if="overdueVulns.length === 0" class="text-center py-6 text-sm text-gray-400">
            기한 초과 취약점이 없습니다
          </div>
          <div v-else class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead>
                <tr class="border-b border-gray-100">
                  <th class="text-left py-2.5 text-xs font-bold text-gray-500">자산</th>
                  <th class="text-left py-2.5 text-xs font-bold text-gray-500">취약점</th>
                  <th class="text-left py-2.5 text-xs font-bold text-gray-500">심각도</th>
                  <th class="text-left py-2.5 text-xs font-bold text-gray-500">기한</th>
                  <th class="text-left py-2.5 text-xs font-bold text-gray-500">초과일수</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-50">
                <tr v-for="v in overdueVulns" :key="v.id">
                  <td class="py-2.5 font-medium text-gray-900">{{ v.assetName || '-' }}</td>
                  <td class="py-2.5 text-gray-600">{{ v.title }}</td>
                  <td class="py-2.5">
                    <span class="text-xs px-2 py-0.5 rounded-full font-semibold" :class="sevClass(v.severity)">{{ sevLabel(v.severity) }}</span>
                  </td>
                  <td class="py-2.5 text-gray-500 text-xs">{{ v.dueDate }}</td>
                  <td class="py-2.5 font-bold text-red-600">+{{ overdueDays(v.dueDate) }}일</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { vulnApi } from '@/api/index.js'

const loading = ref(true)
const statsMap = ref({})
const vulnList = ref([])

const SEV_LABEL = { CRITICAL: '심각', HIGH: '높음', MEDIUM: '중간', LOW: '낮음', INFO: '정보' }
const SEV_CLASS = {
  CRITICAL: 'bg-red-100 text-red-700',
  HIGH: 'bg-orange-100 text-orange-700',
  MEDIUM: 'bg-yellow-100 text-yellow-700',
  LOW: 'bg-blue-100 text-blue-700',
  INFO: 'bg-gray-100 text-gray-600',
}

function sevLabel(s) { return SEV_LABEL[s] || s }
function sevClass(s) { return SEV_CLASS[s] || 'bg-gray-100 text-gray-600' }

function overdueDays(dueDate) {
  if (!dueDate) return 0
  const diff = Date.now() - new Date(dueDate).getTime()
  return Math.max(0, Math.floor(diff / 86400000))
}

const total = computed(() =>
  ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'ACCEPTED', 'FALSE_POSITIVE']
    .reduce((s, k) => s + (statsMap.value[`status_${k}`] || 0), 0)
)

const stats = computed(() => [
  { label: '전체 취약점', value: total.value, sub: '전체 등록', color: 'text-gray-900' },
  { label: '미처리', value: statsMap.value.status_OPEN || 0, sub: '조치 필요', color: 'text-red-600' },
  { label: '처리중', value: statsMap.value.status_IN_PROGRESS || 0, sub: '담당자 배정', color: 'text-yellow-600' },
  { label: '해결됨', value: statsMap.value.status_RESOLVED || 0, sub: '완료', color: 'text-green-600' },
])

const openVulns = computed(() =>
  vulnList.value.filter(v => v.status === 'OPEN' || v.status === 'IN_PROGRESS')
)

const severities = computed(() => {
  const counts = { CRITICAL: 0, HIGH: 0, MEDIUM: 0, LOW: 0 }
  for (const v of openVulns.value) {
    if (counts[v.severity] !== undefined) counts[v.severity]++
  }
  return [
    { label: '심각', count: counts.CRITICAL, barColor: 'bg-red-500',    textColor: 'text-red-600' },
    { label: '높음', count: counts.HIGH,     barColor: 'bg-orange-400', textColor: 'text-orange-600' },
    { label: '중간', count: counts.MEDIUM,   barColor: 'bg-yellow-400', textColor: 'text-yellow-600' },
    { label: '낮음', count: counts.LOW,      barColor: 'bg-blue-400',   textColor: 'text-blue-600' },
  ]
})

const maxSeverity = computed(() => Math.max(...severities.value.map(s => s.count), 1))

const overdueVulns = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return vulnList.value.filter(v =>
    v.dueDate && v.dueDate < today &&
    (v.status === 'OPEN' || v.status === 'IN_PROGRESS')
  )
})

onMounted(async () => {
  try {
    const [sm, page] = await Promise.all([
      vulnApi.stats(),
      vulnApi.list({ size: 500 }),
    ])
    statsMap.value = sm?.data || {}
    vulnList.value = page?.data?.content || []
  } catch {
    // silent fail
  } finally {
    loading.value = false
  }
})
</script>
