<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">인시던트 현황</h1>
        <p class="text-sm text-gray-400 mt-0.5">보안 인시던트 현황 및 월별 추이를 확인합니다</p>
      </div>
      <RouterLink to="/incidents" class="btn-primary px-4 py-2 text-sm rounded-xl">인시던트 관리 →</RouterLink>
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
        <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <div v-for="s in stats" :key="s.label" class="card">
            <p class="text-xs font-semibold text-gray-500 mb-1">{{ s.label }}</p>
            <p class="text-3xl font-black" :class="s.color">{{ s.value }}</p>
            <p class="text-xs text-gray-400 mt-0.5">{{ s.sub }}</p>
          </div>
        </div>

        <!-- 월별 추이 -->
        <div class="card">
          <h2 class="text-sm font-bold text-gray-800 mb-4">월별 인시던트 발생 추이 (최근 12개월)</h2>
          <div v-if="maxCount === 0" class="text-center py-8 text-sm text-gray-400">
            최근 12개월 인시던트 데이터가 없습니다
          </div>
          <div v-else class="flex items-end gap-1 h-32">
            <div v-for="m in monthlyTrend" :key="m.month" class="flex-1 flex flex-col items-center gap-1">
              <span class="text-[10px] text-gray-500 font-semibold">{{ m.count || '' }}</span>
              <div class="w-full rounded-t-sm transition-all"
                :style="{ height: maxCount > 0 ? `${(m.count / maxCount) * 96}px` : '2px' }"
                :class="m.count >= 5 ? 'bg-red-400' : m.count >= 3 ? 'bg-yellow-400' : 'bg-primary-400'"></div>
              <span class="text-[9px] text-gray-400">{{ m.month }}</span>
            </div>
          </div>
        </div>

        <!-- 최근 인시던트 -->
        <div class="card">
          <h2 class="text-sm font-bold text-gray-800 mb-4">최근 인시던트</h2>
          <div v-if="recentIncidents.length === 0" class="text-center py-6 text-sm text-gray-400">
            등록된 인시던트가 없습니다
          </div>
          <div v-else class="space-y-3">
            <div v-for="i in recentIncidents" :key="i.id"
              class="flex items-center gap-3 p-3 rounded-xl border border-gray-100 hover:bg-gray-50">
              <span class="text-xs px-2 py-0.5 rounded-full font-semibold flex-shrink-0" :class="i.sevClass">{{ i.severity }}</span>
              <div class="flex-1 min-w-0">
                <p class="text-sm font-semibold text-gray-900 truncate">{{ i.title }}</p>
                <p class="text-xs text-gray-500">{{ i.type }} · {{ i.detectedAt }}</p>
              </div>
              <span class="text-xs px-2 py-0.5 rounded-full font-semibold flex-shrink-0" :class="i.statusClass">{{ i.status }}</span>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { incidentApi } from '@/api/index.js'

const loading = ref(true)
const incidents = ref([])

const SEV_LABEL = { CRITICAL: '심각', HIGH: '높음', MEDIUM: '중간', LOW: '낮음' }
const SEV_CLASS  = {
  CRITICAL: 'bg-red-100 text-red-700',
  HIGH:     'bg-orange-100 text-orange-700',
  MEDIUM:   'bg-yellow-100 text-yellow-700',
  LOW:      'bg-blue-100 text-blue-700',
}
const STATUS_LABEL = {
  OPEN: '미처리', INVESTIGATING: '조사중', CONTAINED: '격리됨',
  RESOLVED: '해결됨', CLOSED: '종결',
}
const STATUS_CLASS = {
  OPEN:          'bg-red-100 text-red-700',
  INVESTIGATING: 'bg-yellow-100 text-yellow-700',
  CONTAINED:     'bg-blue-100 text-blue-700',
  RESOLVED:      'bg-green-100 text-green-700',
  CLOSED:        'bg-gray-100 text-gray-600',
}
const TYPE_LABEL = {
  MALWARE: '악성코드', PHISHING: '피싱', DATA_BREACH: '데이터침해',
  UNAUTHORIZED_ACCESS: '비인가접근', DDOS: 'DDoS',
  INSIDER_THREAT: '내부자위협', PHYSICAL: '물리적위협', OTHER: '기타',
}

const stats = computed(() => {
  const open       = incidents.value.filter(i => i.status === 'OPEN').length
  const invest     = incidents.value.filter(i => i.status === 'INVESTIGATING').length
  const resolved   = incidents.value.filter(i => i.status === 'RESOLVED' || i.status === 'CLOSED').length
  return [
    { label: '전체 인시던트', value: incidents.value.length, sub: '전체 등록', color: 'text-gray-900' },
    { label: '미처리',        value: open,     sub: '즉각 대응 필요', color: 'text-red-600' },
    { label: '조사중',        value: invest,   sub: '진행중',         color: 'text-yellow-600' },
    { label: '해결됨',        value: resolved, sub: '처리 완료',      color: 'text-green-600' },
  ]
})

const monthlyTrend = computed(() => {
  const now = new Date()
  const months = []
  for (let i = 11; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    months.push({
      key: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`,
      month: String(d.getMonth() + 1).padStart(2, '0'),
      count: 0,
    })
  }
  for (const inc of incidents.value) {
    if (!inc.detectedAt) continue
    const key = inc.detectedAt.slice(0, 7)
    const slot = months.find(m => m.key === key)
    if (slot) slot.count++
  }
  return months
})

const maxCount = computed(() => Math.max(...monthlyTrend.value.map(m => m.count), 0))

const recentIncidents = computed(() =>
  [...incidents.value]
    .sort((a, b) => (b.detectedAt || '').localeCompare(a.detectedAt || ''))
    .slice(0, 5)
    .map(i => ({
      id: i.id,
      title: i.title,
      type: TYPE_LABEL[i.type] || i.type,
      severity: SEV_LABEL[i.severity] || i.severity,
      sevClass: SEV_CLASS[i.severity] || 'bg-gray-100 text-gray-600',
      status: STATUS_LABEL[i.status] || i.status,
      statusClass: STATUS_CLASS[i.status] || 'bg-gray-100 text-gray-600',
      detectedAt: i.detectedAt ? i.detectedAt.slice(0, 10) : '',
    }))
)

onMounted(async () => {
  try {
    const page = await incidentApi.list({ size: 500 })
    incidents.value = page?.data?.content || []
  } catch {
    // silent fail
  } finally {
    loading.value = false
  }
})
</script>
