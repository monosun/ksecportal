<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">ISMS-P 이행률</h1>
        <p class="text-sm text-gray-400 mt-0.5">ISMS-P 인증 통제항목 이행 현황을 확인합니다</p>
      </div>
      <RouterLink to="/isms" class="btn-primary px-4 py-2 text-sm rounded-xl">증적관리 →</RouterLink>
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
        <!-- 전체 이행률 -->
        <div class="card">
          <div class="flex items-center gap-8">
            <div class="text-center flex-shrink-0">
              <div class="w-28 h-28 rounded-full flex items-center justify-center mx-auto"
                :style="{ background: `conic-gradient(#0064FF ${overallRate * 3.6}deg, #E5E7EB 0deg)` }">
                <div class="w-20 h-20 bg-white rounded-full flex items-center justify-center">
                  <span class="text-xl font-black text-primary-600">{{ overallRate }}%</span>
                </div>
              </div>
              <p class="text-xs text-gray-500 mt-2 font-semibold">전체 이행률</p>
            </div>
            <div class="flex-1 grid grid-cols-3 gap-4">
              <div v-for="s in summaryStats" :key="s.label" class="text-center">
                <p class="text-2xl font-black" :class="s.color">{{ s.value }}</p>
                <p class="text-xs text-gray-500 mt-0.5">{{ s.label }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 도메인별 이행률 -->
        <div class="card">
          <h2 class="text-sm font-bold text-gray-800 mb-4">도메인별 이행률</h2>
          <div v-if="!summary || domains.length === 0" class="text-center py-6 text-sm text-gray-400">
            도메인 데이터가 없습니다
          </div>
          <div v-else class="space-y-4">
            <div v-for="d in domains" :key="d.name">
              <div class="flex items-center justify-between mb-1">
                <span class="text-sm font-medium text-gray-700">{{ d.name }}</span>
                <span class="text-sm font-bold"
                  :class="d.rate >= 80 ? 'text-green-600' : d.rate >= 60 ? 'text-yellow-600' : 'text-red-600'">
                  {{ d.rate }}%
                </span>
              </div>
              <div class="w-full bg-gray-100 rounded-full h-2.5 overflow-hidden">
                <div class="h-2.5 rounded-full transition-all"
                  :class="d.rate >= 80 ? 'bg-green-500' : d.rate >= 60 ? 'bg-yellow-400' : 'bg-red-500'"
                  :style="{ width: `${d.rate}%` }"></div>
              </div>
              <p class="text-xs text-gray-400 mt-0.5">{{ d.compliant }}/{{ d.total }} 항목 준수</p>
            </div>
          </div>
        </div>

        <!-- 항목별 현황 -->
        <div class="card">
          <h2 class="text-sm font-bold text-gray-800 mb-4">{{ year }}년도 통제항목 현황</h2>
          <div class="space-y-3">
            <div v-for="item in statusItems" :key="item.label"
              class="flex items-center justify-between p-3 rounded-xl border border-gray-100">
              <span class="text-sm text-gray-700">{{ item.label }}</span>
              <span class="font-bold text-sm" :class="item.color">{{ item.value }}</span>
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
import { ismsApi } from '@/api/index.js'

const loading = ref(true)
const summary = ref(null)
const year = new Date().getFullYear()

const overallRate = computed(() => {
  if (!summary.value) return 0
  const { totalItems, na, compliant } = summary.value
  const effective = (totalItems || 0) - (na || 0)
  if (effective <= 0) return 0
  return Math.round((compliant || 0) / effective * 100)
})

const summaryStats = computed(() => {
  if (!summary.value) return []
  return [
    { label: '준수',      value: summary.value.compliant    || 0, color: 'text-green-600' },
    { label: '부분 준수', value: summary.value.partial       || 0, color: 'text-yellow-600' },
    { label: '미준수',    value: summary.value.nonCompliant || 0, color: 'text-red-600' },
  ]
})

const domains = computed(() => {
  if (!summary.value?.byDomain) return []
  return summary.value.byDomain.map(d => {
    const effective = (d.total || 0) - (d.na || 0)
    const rate = effective > 0 ? Math.round((d.compliant || 0) / effective * 100) : 0
    return {
      name: d.domainName || d.domainCode,
      rate,
      compliant: d.compliant || 0,
      total: d.total || 0,
    }
  })
})

const statusItems = computed(() => {
  if (!summary.value) return []
  return [
    { label: '전체 항목 수',  value: `${summary.value.totalItems || 0}건`,    color: 'text-gray-700' },
    { label: '준수 항목',     value: `${summary.value.compliant || 0}건`,     color: 'text-green-600' },
    { label: '부분 준수',     value: `${summary.value.partial || 0}건`,       color: 'text-yellow-600' },
    { label: '미준수 항목',   value: `${summary.value.nonCompliant || 0}건`,  color: 'text-red-600' },
    { label: '증적 미제출',   value: `${summary.value.noEvidence || 0}건`,    color: 'text-orange-600' },
    { label: '해당 없음(NA)', value: `${summary.value.na || 0}건`,            color: 'text-gray-400' },
  ]
})

onMounted(async () => {
  try {
    summary.value = (await ismsApi.summary(year))?.data || null
  } catch {
    // silent fail
  } finally {
    loading.value = false
  }
})
</script>
