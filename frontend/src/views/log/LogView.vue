<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ pageTitle }}</h1>
        <p class="text-sm text-gray-400 mt-0.5">{{ pageSubtitle }}</p>
      </div>
      <button @click="showAiPanel = !showAiPanel"
        class="inline-flex items-center gap-2 px-4 py-2 rounded-xl border-2 text-sm font-semibold transition-all"
        :class="showAiPanel
          ? 'border-primary-500 bg-primary-50 text-primary-600'
          : 'border-gray-200 bg-white text-gray-600 hover:border-gray-300'">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"/>
        </svg>
        AI 분석
      </button>
    </div>

    <div class="page-body space-y-4">
      <!-- 통합검색 -->
      <div v-if="logType === 'SEARCH'" class="card">
        <div class="flex gap-3 items-end">
          <div class="flex-1">
            <label class="block text-sm font-semibold text-gray-700 mb-1">통합 검색어</label>
            <input v-model="searchQuery" type="text" class="input w-full" placeholder="검색어를 입력하세요..." @keyup.enter="search" />
          </div>
          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-1">로그 유형</label>
            <select v-model="filterType" class="input">
              <option value="">전체</option>
              <option value="PERSONAL_INFO">개인정보처리시스템</option>
              <option value="AD">AD</option>
              <option value="NAC">NAC</option>
              <option value="NETWORK_LINK">망연계</option>
            </select>
          </div>
          <button @click="search" class="btn-primary px-5 py-2 rounded-xl text-sm">검색</button>
          <button @click="resetFilters" class="px-4 py-2 rounded-xl border border-gray-200 text-sm text-gray-600 hover:bg-gray-50">초기화</button>
        </div>
      </div>

      <!-- 일반 로그 필터 -->
      <div v-else class="card">
        <div class="flex flex-wrap gap-3 items-end">
          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-1">시작일</label>
            <input v-model="dateFrom" type="date" class="input text-sm" />
          </div>
          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-1">종료일</label>
            <input v-model="dateTo" type="date" class="input text-sm" />
          </div>
          <div>
            <label class="block text-sm font-semibold text-gray-700 mb-1">결과</label>
            <select v-model="filterResult" class="input text-sm">
              <option value="">전체</option>
              <option value="SUCCESS">성공</option>
              <option value="FAIL">실패</option>
            </select>
          </div>
          <div class="flex-1 min-w-[180px]">
            <label class="block text-sm font-semibold text-gray-700 mb-1">검색어</label>
            <input v-model="searchQuery" type="text" class="input w-full text-sm" placeholder="IP, 계정, 내용 검색..." @keyup.enter="search" />
          </div>
          <button @click="search" class="btn-primary px-5 py-2 rounded-xl text-sm">검색</button>
          <button @click="resetFilters" class="px-4 py-2 rounded-xl border border-gray-200 text-sm text-gray-600 hover:bg-gray-50">초기화</button>
        </div>
      </div>

      <!-- 연동 안내 -->
      <div class="card">
        <div class="flex flex-col items-center justify-center py-16 text-center">
          <div class="w-16 h-16 rounded-2xl bg-gray-100 flex items-center justify-center mb-4">
            <svg class="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
            </svg>
          </div>
          <p class="text-sm font-semibold text-gray-700 mb-1">로그 연동이 필요합니다</p>
          <p class="text-xs text-gray-400 max-w-xs">{{ integrationGuide }}</p>
          <div class="mt-5 flex gap-3">
            <RouterLink to="/security-events"
              class="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl border border-gray-200 bg-white text-sm font-semibold text-gray-600 hover:border-gray-300 transition-all">
              보안솔루션 연동 설정 →
            </RouterLink>
          </div>
        </div>
      </div>

      <!-- AI 분석 패널 -->
      <Transition name="slide-down">
        <div v-if="showAiPanel" class="card border-2 border-primary-100">
          <div class="flex items-center justify-between mb-4">
            <h2 class="text-base font-bold text-gray-800 flex items-center gap-2">
              <svg class="w-5 h-5 text-primary-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z"/>
              </svg>
              AI 로그 분석
            </h2>
            <div class="flex items-center gap-2">
              <span class="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-600 font-medium">{{ pageTitle }}</span>
              <button @click="showAiPanel = false" class="p-1 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                </svg>
              </button>
            </div>
          </div>

          <!-- 빠른 분석 주제 -->
          <div class="mb-4">
            <p class="text-xs font-semibold text-gray-500 mb-2">빠른 분석 주제</p>
            <div class="flex flex-wrap gap-2">
              <button v-for="preset in presetPrompts" :key="preset"
                @click="aiPrompt = preset"
                class="px-3 py-1.5 rounded-lg border text-xs font-medium transition-all"
                :class="aiPrompt === preset
                  ? 'border-primary-400 bg-primary-50 text-primary-600'
                  : 'border-gray-200 text-gray-600 hover:border-gray-300 hover:bg-gray-50'">
                {{ preset }}
              </button>
            </div>
          </div>

          <!-- 분석 입력 -->
          <div class="mb-4">
            <label class="block text-sm font-semibold text-gray-700 mb-1">분석 요청 내용</label>
            <textarea v-model="aiPrompt" rows="3"
              class="input w-full text-sm resize-none"
              placeholder="예: 지난 7일간 로그인 실패가 많은 계정과 시간대를 분석해주세요."></textarea>
          </div>

          <div class="flex items-center gap-3 mb-4">
            <button @click="runAnalysis" :disabled="aiAnalyzing || !aiPrompt.trim()"
              class="btn-primary text-sm px-6 py-2 disabled:opacity-50 flex items-center gap-2">
              <svg v-if="aiAnalyzing" class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
              </svg>
              {{ aiAnalyzing ? '분석 중...' : '분석 시작' }}
            </button>
            <button v-if="aiResult" @click="aiResult = ''"
              class="px-3 py-2 text-xs text-gray-500 hover:text-gray-700 rounded-lg hover:bg-gray-100 transition-colors">
              결과 지우기
            </button>
            <span v-if="aiError" class="text-sm text-red-500">{{ aiError }}</span>
          </div>

          <!-- 분석 결과 -->
          <Transition name="fade">
            <div v-if="aiResult" class="rounded-xl bg-gray-50 border border-gray-100 p-4">
              <div class="flex items-center justify-between mb-2">
                <p class="text-xs font-semibold text-gray-500">분석 결과</p>
                <button @click="copyResult"
                  class="text-xs text-gray-400 hover:text-gray-600 flex items-center gap-1">
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"/>
                  </svg>
                  {{ copied ? '복사됨' : '복사' }}
                </button>
              </div>
              <pre class="text-sm text-gray-800 whitespace-pre-wrap font-sans leading-relaxed">{{ aiResult }}</pre>
            </div>
          </Transition>
        </div>
      </Transition>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { RouterLink } from 'vue-router'
import { aiApi } from '@/api/index.js'

const props = defineProps({
  logType: { type: String, default: 'AD' }
})

const TITLES = {
  PERSONAL_INFO: { title: '개인정보처리시스템 로그', subtitle: '개인정보처리시스템 접근 및 처리 이력을 조회합니다', guide: 'DRM, DB 접근제어 솔루션과 연동하여 개인정보처리시스템 접근 로그를 수집합니다. 관리자에게 연동을 요청하세요.' },
  AD:            { title: 'AD 로그', subtitle: 'Active Directory 인증·계정 변경 이벤트를 조회합니다', guide: 'Active Directory 이벤트 로그 수집 에이전트 설치가 필요합니다. 관리자에게 연동을 요청하세요.' },
  NAC:           { title: 'NAC 로그', subtitle: 'NAC 네트워크 접근제어 이벤트를 조회합니다', guide: 'NAC 솔루션 Syslog 연동이 필요합니다. 관리자에게 연동을 요청하세요.' },
  NETWORK_LINK:  { title: '망연계 로그', subtitle: '망연계 시스템 파일 전송 및 접근 이력을 조회합니다', guide: '망연계 솔루션 API 또는 Syslog 연동이 필요합니다. 관리자에게 연동을 요청하세요.' },
  SEARCH:        { title: '로그 통합검색', subtitle: '모든 로그 유형을 통합하여 검색합니다', guide: '각 로그 유형의 연동 설정이 완료되면 통합 검색이 가능합니다.' },
}

const PRESETS = {
  PERSONAL_INFO: ['이상 접근 패턴 탐지', '야간 접근 계정 목록', '대용량 다운로드 탐지', '권한 외 접근 시도'],
  AD:            ['로그인 실패 상위 계정', '비업무시간 인증 이벤트', '계정 잠금 발생 현황', '관리자 계정 활동'],
  NAC:           ['미인가 기기 접속 시도', '이상 IP 접근 패턴', '시간대별 접속 현황', '차단된 기기 목록'],
  NETWORK_LINK:  ['대용량 파일 전송 탐지', '비정상 전송 시간대', '전송 실패 패턴 분석', '외부망 접속 현황'],
  SEARCH:        ['전체 로그 위협 요약', '이상 행위 패턴 탐지', '시간대별 이벤트 분포', '주요 경보 요약'],
}

const pageTitle    = computed(() => TITLES[props.logType]?.title    || '로그 조회')
const pageSubtitle = computed(() => TITLES[props.logType]?.subtitle || '')
const integrationGuide = computed(() => TITLES[props.logType]?.guide || '관리자에게 로그 연동을 요청하세요.')
const presetPrompts    = computed(() => PRESETS[props.logType] || PRESETS.SEARCH)

const dateFrom    = ref('')
const dateTo      = ref('')
const filterResult = ref('')
const filterType  = ref('')
const searchQuery = ref('')

const showAiPanel  = ref(false)
const aiPrompt     = ref('')
const aiAnalyzing  = ref(false)
const aiResult     = ref('')
const aiError      = ref('')
const copied       = ref(false)

function search() {}
function resetFilters() {
  dateFrom.value = ''; dateTo.value = ''; filterResult.value = ''; filterType.value = ''; searchQuery.value = ''
}

async function runAnalysis() {
  if (!aiPrompt.value.trim() || aiAnalyzing.value) return
  aiAnalyzing.value = true; aiResult.value = ''; aiError.value = ''
  try {
    const res = await aiApi.analyze({
      logType: props.logType,
      prompt: aiPrompt.value,
      context: {
        dateFrom: dateFrom.value,
        dateTo: dateTo.value,
        filterResult: filterResult.value,
        searchQuery: searchQuery.value,
      }
    })
    aiResult.value = res.data?.result || res.data || '분석이 완료되었습니다.'
  } catch (e) {
    aiError.value = typeof e === 'string' ? e : 'AI 분석에 실패했습니다. 설정관리 → AI 설정을 확인하세요.'
  } finally {
    aiAnalyzing.value = false
  }
}

async function copyResult() {
  try {
    await navigator.clipboard.writeText(aiResult.value)
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  } catch {}
}
</script>

<style scoped>
.slide-down-enter-active, .slide-down-leave-active { transition: all 0.2s ease; }
.slide-down-enter-from, .slide-down-leave-to { opacity: 0; transform: translateY(-8px); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
