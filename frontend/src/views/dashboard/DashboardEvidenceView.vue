<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">증적 제출 현황</h1>
        <p class="text-sm text-gray-400 mt-0.5">ISMS-P 인증 항목별 증적 제출 현황을 확인합니다</p>
      </div>
      <RouterLink to="/isms" class="btn-primary px-4 py-2 text-sm rounded-xl">증적 관리 →</RouterLink>
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
        <!-- 요약 통계 -->
        <div class="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <div v-for="s in stats" :key="s.label" class="card">
            <p class="text-xs font-semibold text-gray-500 mb-1">{{ s.label }}</p>
            <p class="text-3xl font-black" :class="s.color">{{ s.value }}</p>
            <p class="text-xs text-gray-400 mt-0.5">{{ s.sub }}</p>
          </div>
        </div>

        <!-- 도메인별 제출 현황 -->
        <div class="card">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-sm font-bold text-gray-800">도메인별 증적 제출 현황</h2>
            <div class="flex items-center gap-3 text-xs">
              <span class="flex items-center gap-1"><span class="w-3 h-3 rounded-full bg-green-500 inline-block"></span>제출</span>
              <span class="flex items-center gap-1"><span class="w-3 h-3 rounded-full bg-gray-200 inline-block"></span>미제출</span>
            </div>
          </div>
          <div v-if="domains.length === 0" class="text-center py-6 text-sm text-gray-400">
            도메인 데이터가 없습니다
          </div>
          <div v-else class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead>
                <tr class="border-b border-gray-100">
                  <th class="text-left py-2.5 text-xs font-bold text-gray-500">ISMS-P 도메인</th>
                  <th class="text-right py-2.5 text-xs font-bold text-gray-500 w-20">총 항목</th>
                  <th class="text-right py-2.5 text-xs font-bold text-gray-500 w-20">제출</th>
                  <th class="text-right py-2.5 text-xs font-bold text-gray-500 w-20">미제출</th>
                  <th class="text-left py-2.5 text-xs font-bold text-gray-500 w-40">이행률</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-50">
                <tr v-for="d in domains" :key="d.name">
                  <td class="py-3 font-medium text-gray-900">{{ d.name }}</td>
                  <td class="py-3 text-right text-gray-600">{{ d.total }}</td>
                  <td class="py-3 text-right font-semibold text-green-600">{{ d.submitted }}</td>
                  <td class="py-3 text-right font-semibold" :class="d.pending > 0 ? 'text-red-500' : 'text-gray-400'">{{ d.pending }}</td>
                  <td class="py-3">
                    <div class="flex items-center gap-2">
                      <div class="flex-1 bg-gray-100 rounded-full h-2 overflow-hidden">
                        <div class="h-2 rounded-full bg-green-500 transition-all"
                          :style="{ width: `${d.rate}%` }"></div>
                      </div>
                      <span class="text-xs font-bold w-9 text-right"
                        :class="d.rate >= 80 ? 'text-green-600' : d.rate >= 60 ? 'text-yellow-600' : 'text-red-600'">
                        {{ d.rate }}%
                      </span>
                    </div>
                  </td>
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
import { ismsApi } from '@/api/index.js'

const loading = ref(true)
const summary = ref(null)
const year = new Date().getFullYear()

const submitted = computed(() =>
  (summary.value?.compliant || 0) + (summary.value?.partial || 0)
)
const submitRate = computed(() => {
  const total = summary.value?.totalItems || 0
  return total > 0 ? Math.round(submitted.value / total * 100) : 0
})

const stats = computed(() => [
  { label: '전체 항목',   value: summary.value?.totalItems   || 0, sub: `${year}년도 기준`,  color: 'text-gray-900' },
  { label: '증적 제출',   value: submitted.value,               sub: `${submitRate.value}% 완료`, color: 'text-green-600' },
  { label: '증적 미제출', value: summary.value?.noEvidence    || 0, sub: '조치 필요',       color: 'text-red-600' },
  { label: '해당 없음',   value: summary.value?.na            || 0, sub: 'NA 항목',         color: 'text-gray-500' },
])

const domains = computed(() => {
  if (!summary.value?.byDomain) return []
  return summary.value.byDomain.map(d => {
    const sub = (d.compliant || 0) + (d.partial || 0)
    const rate = d.total > 0 ? Math.round(sub / d.total * 100) : 0
    return {
      name: d.domainName || d.domainCode,
      total: d.total || 0,
      submitted: sub,
      pending: d.noEvidence || 0,
      rate,
    }
  })
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
