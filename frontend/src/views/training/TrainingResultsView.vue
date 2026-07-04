<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">교육·훈련 결과</h1>
        <p class="text-sm text-gray-400 mt-0.5">IT 및 정보보호 교육 이수 현황과 모의 악성메일 훈련 결과를 조회합니다</p>
      </div>
    </div>

    <div class="page-body">

    <!-- 탭 -->
    <div class="flex gap-1 border-b border-gray-200 mb-4">
      <button @click="activeTab = 'training'"
        class="px-4 py-2 text-sm font-semibold border-b-2 -mb-px transition-colors"
        :class="activeTab === 'training' ? 'border-primary-500 text-primary-600' : 'border-transparent text-gray-500 hover:text-gray-700'">
        IT 및 정보보호 교육 결과
      </button>
      <button @click="activeTab = 'phishing'"
        class="px-4 py-2 text-sm font-semibold border-b-2 -mb-px transition-colors"
        :class="activeTab === 'phishing' ? 'border-primary-500 text-primary-600' : 'border-transparent text-gray-500 hover:text-gray-700'">
        모의훈련 결과
      </button>
    </div>

    <div v-if="forbidden" class="card text-center py-12 text-gray-400 text-sm">
      결과 조회 권한이 없습니다. (MANAGER 이상)
    </div>
    <div v-else-if="loading" class="flex items-center justify-center py-20 text-gray-400">
      <svg class="animate-spin w-6 h-6 mr-2" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
      </svg>
      데이터 불러오는 중...
    </div>

    <!-- ── 탭: 보안교육 결과 ── -->
    <template v-else-if="activeTab === 'training'">
      <!-- Stat Cards -->
      <div class="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <div class="card">
          <p class="text-xs text-gray-400 mb-1">교육 코스</p>
          <p class="text-2xl font-bold text-gray-900">{{ courses.length }}<span class="text-sm font-normal text-gray-400 ml-1">개</span></p>
        </div>
        <div class="card">
          <p class="text-xs text-gray-400 mb-1">총 이수 건수</p>
          <p class="text-2xl font-bold text-blue-600">{{ totalCompleted }}<span class="text-sm font-normal text-gray-400 ml-1">건</span></p>
        </div>
        <div class="card">
          <p class="text-xs text-gray-400 mb-1">전체 합격률</p>
          <p class="text-2xl font-bold text-green-600">{{ overallPassRate }}<span class="text-sm font-normal text-gray-400 ml-1">%</span></p>
        </div>
        <div class="card">
          <p class="text-xs text-gray-400 mb-1">전체 평균 점수</p>
          <p class="text-2xl font-bold text-indigo-600">{{ overallAvgScore }}<span class="text-sm font-normal text-gray-400 ml-1">점</span></p>
        </div>
      </div>

      <!-- 코스별 이수 현황 -->
      <div class="card mb-6">
        <h2 class="text-sm font-bold text-gray-800 mb-4">코스별 이수 현황</h2>
        <div v-if="courses.length === 0" class="text-center py-8 text-gray-400 text-sm">등록된 교육 코스가 없습니다.</div>
        <div v-else class="space-y-4">
          <div v-for="c in courses" :key="c.courseId">
            <div class="flex items-center justify-between text-sm mb-1">
              <div class="flex items-center gap-2 min-w-0">
                <span class="font-medium text-gray-800 truncate">{{ c.title }}</span>
                <span v-if="c.mandatory" class="flex-shrink-0 text-[10px] font-bold px-1.5 py-0.5 rounded bg-red-100 text-red-600">필수</span>
              </div>
              <span class="text-xs text-gray-500 flex-shrink-0 ml-3">
                이수 {{ c.completedCount }}/{{ c.totalUsers }}명 · 합격 {{ c.passedCount }}명
                <template v-if="c.avgScore != null"> · 평균 {{ Math.round(c.avgScore) }}점</template>
              </span>
            </div>
            <div class="h-3 rounded-full bg-gray-100 overflow-hidden">
              <div class="h-full rounded-full bg-blue-500 transition-all"
                :style="{ width: rate(c.completedCount, c.totalUsers) + '%' }"></div>
            </div>
            <p class="text-[11px] text-gray-400 mt-0.5 text-right">이수율 {{ rate(c.completedCount, c.totalUsers) }}%</p>
          </div>
        </div>
      </div>

      <!-- 이수 이력 조회 -->
      <div class="card">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-sm font-bold text-gray-800">이수 이력 ({{ filteredCompletions.length }}건)</h2>
          <select v-model="completionFilter" class="input w-64 text-sm !py-1.5">
            <option :value="null">전체 코스</option>
            <option v-for="c in courses" :key="c.courseId" :value="c.courseId">{{ c.title }}</option>
          </select>
        </div>
        <div v-if="filteredCompletions.length === 0" class="text-center py-8 text-gray-400 text-sm">이수 이력이 없습니다.</div>
        <table v-else class="w-full text-sm">
          <thead>
            <tr class="border-b text-left text-gray-500">
              <th class="py-2.5 px-3 font-semibold">사용자</th>
              <th class="py-2.5 px-3 font-semibold">부서</th>
              <th class="py-2.5 px-3 font-semibold">교육 코스</th>
              <th class="py-2.5 px-3 font-semibold text-center w-20">점수</th>
              <th class="py-2.5 px-3 font-semibold text-center w-20">결과</th>
              <th class="py-2.5 px-3 font-semibold w-40">이수 일시</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in filteredCompletions" :key="r.id" class="border-b hover:bg-gray-50">
              <td class="py-2.5 px-3 text-gray-800">{{ r.userName || '-' }}</td>
              <td class="py-2.5 px-3 text-gray-500">{{ r.userDepartment || '-' }}</td>
              <td class="py-2.5 px-3 text-gray-600">{{ r.courseTitle }}</td>
              <td class="py-2.5 px-3 text-center font-mono">{{ r.score ?? '-' }}</td>
              <td class="py-2.5 px-3 text-center">
                <span class="text-[11px] font-bold px-2 py-0.5 rounded-full"
                  :class="r.passed ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-600'">
                  {{ r.passed ? '합격' : '불합격' }}
                </span>
              </td>
              <td class="py-2.5 px-3 text-gray-400 text-xs">{{ formatDt(r.completedAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>

    <!-- ── 탭: 모의훈련 결과 ── -->
    <template v-else>
      <!-- Stat Cards -->
      <div class="grid grid-cols-2 lg:grid-cols-5 gap-4 mb-6">
        <div class="card">
          <p class="text-xs text-gray-400 mb-1">캠페인</p>
          <p class="text-2xl font-bold text-gray-900">{{ campaigns.length }}<span class="text-sm font-normal text-gray-400 ml-1">개</span></p>
        </div>
        <div class="card">
          <p class="text-xs text-gray-400 mb-1">총 발송</p>
          <p class="text-2xl font-bold text-blue-600">{{ sum('sentCount') }}<span class="text-sm font-normal text-gray-400 ml-1">건</span></p>
        </div>
        <div class="card">
          <p class="text-xs text-gray-400 mb-1">평균 열람률</p>
          <p class="text-2xl font-bold text-amber-500">{{ avgRate('openedCount') }}<span class="text-sm font-normal text-gray-400 ml-1">%</span></p>
        </div>
        <div class="card">
          <p class="text-xs text-gray-400 mb-1">평균 클릭률</p>
          <p class="text-2xl font-bold text-red-500">{{ avgRate('clickedCount') }}<span class="text-sm font-normal text-gray-400 ml-1">%</span></p>
        </div>
        <div class="card">
          <p class="text-xs text-gray-400 mb-1">신고</p>
          <p class="text-2xl font-bold text-green-600">{{ sum('reportedCount') }}<span class="text-sm font-normal text-gray-400 ml-1">건</span></p>
        </div>
      </div>

      <!-- 캠페인별 결과 시각화 -->
      <div class="card mb-6">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-sm font-bold text-gray-800">캠페인별 결과</h2>
          <div class="flex items-center gap-3 text-[11px] text-gray-500">
            <span class="flex items-center gap-1"><span class="w-2.5 h-2.5 rounded-sm bg-amber-400 inline-block"></span>열람</span>
            <span class="flex items-center gap-1"><span class="w-2.5 h-2.5 rounded-sm bg-red-500 inline-block"></span>클릭</span>
            <span class="flex items-center gap-1"><span class="w-2.5 h-2.5 rounded-sm bg-green-500 inline-block"></span>신고</span>
          </div>
        </div>
        <div v-if="campaigns.length === 0" class="text-center py-8 text-gray-400 text-sm">실시된 캠페인이 없습니다.</div>
        <div v-else class="space-y-5">
          <div v-for="c in campaigns" :key="c.id">
            <div class="flex items-center justify-between text-sm mb-1.5">
              <div class="flex items-center gap-2 min-w-0">
                <span class="font-medium text-gray-800 truncate">{{ c.name }}</span>
                <span class="flex-shrink-0 text-[10px] font-bold px-1.5 py-0.5 rounded"
                  :class="campaignStatusClass(c.status)">{{ campaignStatusLabel(c.status) }}</span>
              </div>
              <span class="text-xs text-gray-500 flex-shrink-0 ml-3">
                대상 {{ c.totalTargets }} · 발송 {{ c.sentCount }} · 열람 {{ c.openedCount }} · 클릭 {{ c.clickedCount }} · 신고 {{ c.reportedCount }}
              </span>
            </div>
            <div class="space-y-1">
              <div v-for="bar in campaignBars(c)" :key="bar.label" class="flex items-center gap-2">
                <span class="w-8 text-[10px] text-gray-400 text-right flex-shrink-0">{{ bar.label }}</span>
                <div class="flex-1 h-2.5 rounded-full bg-gray-100 overflow-hidden">
                  <div class="h-full rounded-full transition-all" :class="bar.color" :style="{ width: bar.pct + '%' }"></div>
                </div>
                <span class="w-10 text-[10px] text-gray-500 flex-shrink-0">{{ bar.pct }}%</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 캠페인 상세 표 -->
      <div class="card">
        <h2 class="text-sm font-bold text-gray-800 mb-4">캠페인 목록</h2>
        <div v-if="campaigns.length === 0" class="text-center py-8 text-gray-400 text-sm">캠페인이 없습니다.</div>
        <table v-else class="w-full text-sm">
          <thead>
            <tr class="border-b text-left text-gray-500">
              <th class="py-2.5 px-3 font-semibold">캠페인</th>
              <th class="py-2.5 px-3 font-semibold">템플릿</th>
              <th class="py-2.5 px-3 font-semibold text-center w-20">상태</th>
              <th class="py-2.5 px-3 font-semibold text-center w-16">대상</th>
              <th class="py-2.5 px-3 font-semibold text-center w-16">발송</th>
              <th class="py-2.5 px-3 font-semibold text-center w-24">열람(률)</th>
              <th class="py-2.5 px-3 font-semibold text-center w-24">클릭(률)</th>
              <th class="py-2.5 px-3 font-semibold text-center w-24">신고(률)</th>
              <th class="py-2.5 px-3 font-semibold w-32">일시</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in campaigns" :key="c.id" class="border-b hover:bg-gray-50">
              <td class="py-2.5 px-3 text-gray-800 font-medium">{{ c.name }}</td>
              <td class="py-2.5 px-3 text-gray-500">{{ c.templateName }}</td>
              <td class="py-2.5 px-3 text-center">
                <span class="text-[11px] font-bold px-2 py-0.5 rounded" :class="campaignStatusClass(c.status)">
                  {{ campaignStatusLabel(c.status) }}
                </span>
              </td>
              <td class="py-2.5 px-3 text-center">{{ c.totalTargets }}</td>
              <td class="py-2.5 px-3 text-center">{{ c.sentCount }}</td>
              <td class="py-2.5 px-3 text-center text-amber-600">{{ c.openedCount }} ({{ rate(c.openedCount, c.sentCount) }}%)</td>
              <td class="py-2.5 px-3 text-center text-red-600 font-semibold">{{ c.clickedCount }} ({{ rate(c.clickedCount, c.sentCount) }}%)</td>
              <td class="py-2.5 px-3 text-center text-green-600">{{ c.reportedCount }} ({{ rate(c.reportedCount, c.sentCount) }}%)</td>
              <td class="py-2.5 px-3 text-gray-400 text-xs">{{ formatDt(c.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>

    </div><!-- /page-body -->
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { trainingApi, phishingApi } from '@/api'

const activeTab = ref('training')
const loading = ref(true)
const forbidden = ref(false)

const courses = ref([])          // 코스별 이수 요약
const completions = ref([])      // 이수 이력
const completionFilter = ref(null)
const campaigns = ref([])        // 모의훈련 캠페인

const totalCompleted = computed(() => courses.value.reduce((s, c) => s + c.completedCount, 0))
const overallPassRate = computed(() => {
  const total = totalCompleted.value
  const passed = courses.value.reduce((s, c) => s + c.passedCount, 0)
  return rate(passed, total)
})
const overallAvgScore = computed(() => {
  const scored = completions.value.filter(c => c.score != null)
  if (!scored.length) return 0
  return Math.round(scored.reduce((s, c) => s + c.score, 0) / scored.length)
})

const filteredCompletions = computed(() =>
  completionFilter.value == null
    ? completions.value
    : completions.value.filter(c => c.courseId === completionFilter.value)
)

function rate(n, d) { return d > 0 ? Math.round((n / d) * 100) : 0 }
function sum(key) { return campaigns.value.reduce((s, c) => s + (c[key] ?? 0), 0) }
function avgRate(key) { return rate(sum(key), sum('sentCount')) }

function campaignBars(c) {
  const d = c.sentCount || c.totalTargets
  return [
    { label: '열람', pct: rate(c.openedCount, d), color: 'bg-amber-400' },
    { label: '클릭', pct: rate(c.clickedCount, d), color: 'bg-red-500' },
    { label: '신고', pct: rate(c.reportedCount, d), color: 'bg-green-500' },
  ]
}

function campaignStatusLabel(s) {
  return { DRAFT: '대기', SCHEDULED: '예약', RUNNING: '진행중', SENT: '발송됨', COMPLETED: '완료', CANCELLED: '취소' }[s] || s
}
function campaignStatusClass(s) {
  return {
    RUNNING: 'bg-blue-100 text-blue-700', SENT: 'bg-blue-100 text-blue-700',
    COMPLETED: 'bg-green-100 text-green-700',
    CANCELLED: 'bg-gray-100 text-gray-500',
  }[s] || 'bg-gray-100 text-gray-600'
}

function formatDt(dt) {
  if (!dt) return '-'
  const d = new Date(dt)
  return `${d.toLocaleDateString()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

onMounted(async () => {
  loading.value = true
  try {
    const [r1, r2, r3] = await Promise.all([
      trainingApi.results(),
      trainingApi.resultCompletions(),
      phishingApi.listCampaigns(),
    ])
    courses.value = r1.data ?? []
    completions.value = r2.data ?? []
    campaigns.value = r3.data ?? []
  } catch (e) {
    if (String(e).includes('403') || e?.response?.status === 403) forbidden.value = true
    else console.error(e)
  } finally {
    loading.value = false
  }
})
</script>
