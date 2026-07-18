<template>
  <div class="p-4">

    <!-- 공지사항 -->
    <NoticeBar />

    <!-- 헤더 -->
    <div class="flex items-center justify-between mb-3">
      <div class="flex items-center gap-3">
        <h1 class="text-xl font-bold text-gray-900">{{ $t('dashboard.title') }}</h1>
        <button v-if="isOrderChanged" @click="resetOrder"
          class="flex items-center gap-1 text-xs text-gray-400 hover:text-gray-600 transition-colors px-2 py-1 rounded-lg hover:bg-gray-100">
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
          </svg>
          순서 초기화
        </button>
      </div>
    </div>

    <!-- 드래그 가능한 위젯 그리드 -->
    <div class="grid grid-cols-2 gap-3">
      <div
        v-for="widgetId in widgetOrder" :key="widgetId"
        draggable="true"
        @dragstart="onDragStart($event, widgetId)"
        @dragover.prevent="onDragOver($event, widgetId)"
        @dragleave="onDragLeave"
        @drop.prevent="onDrop($event, widgetId)"
        @dragend="onDragEnd"
        class="group relative transition-all duration-200 flex flex-col"
        :class="{
          'opacity-40 scale-[0.99]': draggingId === widgetId,
          'outline outline-2 outline-primary-400 outline-offset-2 rounded-2xl': dragOverId === widgetId && draggingId !== widgetId,
          'col-span-2': widgetId === 'kpi'
        }">

        <!-- 드래그 핸들 바 -->
        <div class="flex items-center gap-2 mb-1 px-1 select-none cursor-grab active:cursor-grabbing">
          <svg class="w-4 h-4 text-gray-300 group-hover:text-gray-400 transition-colors flex-shrink-0"
            fill="currentColor" viewBox="0 0 24 24">
            <circle cx="9" cy="5" r="1.5"/><circle cx="15" cy="5" r="1.5"/>
            <circle cx="9" cy="12" r="1.5"/><circle cx="15" cy="12" r="1.5"/>
            <circle cx="9" cy="19" r="1.5"/><circle cx="15" cy="19" r="1.5"/>
          </svg>
          <span class="text-xs text-gray-400 group-hover:text-gray-500 transition-colors font-medium">
            {{ WIDGET_LABELS[widgetId] }}
          </span>
        </div>

        <!-- ── 위젯: 보안이벤트 현황 ── -->
        <template v-if="widgetId === 'securityEvents'">
          <div class="card flex-1 flex flex-col">
            <div class="flex items-center justify-between mb-3">
              <div class="flex items-center gap-2">
                <div class="w-7 h-7 rounded-lg bg-gray-900 flex items-center justify-center flex-shrink-0">
                  <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/>
                  </svg>
                </div>
                <h2 class="text-sm font-semibold text-gray-800">보안이벤트 현황</h2>
              </div>
              <RouterLink to="/security-events" class="text-xs text-primary-600 hover:underline font-medium">전체 보기 →</RouterLink>
            </div>

            <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
              <div class="space-y-3">
                <div class="grid grid-cols-2 gap-3">
                  <div class="bg-gray-50 rounded-xl p-3">
                    <p class="text-xs text-gray-500 mb-0.5">연동 솔루션</p>
                    <p class="text-2xl font-bold text-gray-900">{{ metrics.totalIntegrations }}</p>
                    <p class="text-xs mt-0.5">
                      <span class="text-green-600 font-semibold">{{ metrics.connectedIntegrations }}</span>
                      <span class="text-gray-400"> 연결됨</span>
                    </p>
                  </div>
                  <div class="bg-gray-50 rounded-xl p-3">
                    <p class="text-xs text-gray-500 mb-0.5">24시간 이벤트</p>
                    <p class="text-2xl font-bold text-gray-900">{{ metrics.events24h }}</p>
                    <p class="text-xs mt-0.5">
                      <span class="text-red-600 font-semibold">{{ metrics.criticalEvents24h }}</span>
                      <span class="text-gray-400"> 심각</span>
                    </p>
                  </div>
                </div>
                <div>
                  <p class="text-xs font-semibold text-gray-600 mb-2">24시간 심각도 분포</p>
                  <div class="space-y-2">
                    <div v-for="s in secEventSeverities" :key="s.key" class="flex items-center gap-2">
                      <span class="w-14 text-xs text-gray-500 flex-shrink-0">{{ s.label }}</span>
                      <div class="flex-1 bg-gray-100 rounded-full h-1.5">
                        <div class="h-1.5 rounded-full transition-all duration-700" :style="{ width: s.pct + '%', background: s.color }"></div>
                      </div>
                      <span class="text-xs font-semibold w-6 text-right" :style="{ color: s.color }">{{ s.count }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <div class="lg:col-span-2">
                <p class="text-xs font-semibold text-gray-600 mb-2">최근 이벤트</p>
                <div v-if="!metrics.recentSecurityEvents?.length"
                  class="flex items-center justify-center h-24 text-xs text-gray-400 bg-gray-50 rounded-xl">
                  수신된 이벤트가 없습니다
                </div>
                <div v-else class="space-y-1.5 max-h-36 overflow-y-auto">
                  <RouterLink
                    v-for="evt in metrics.recentSecurityEvents.slice(0,5)" :key="evt.id"
                    to="/security-events"
                    class="flex items-start gap-3 p-2.5 rounded-xl bg-gray-50 hover:bg-gray-100 transition-colors">
                    <span class="flex-shrink-0 inline-flex items-center gap-1 text-[11px] font-bold px-1.5 py-0.5 rounded-md mt-0.5"
                      :class="severityBadgeClass(evt.severity)">
                      <span class="w-1.5 h-1.5 rounded-full" :class="severityDotClass(evt.severity)"></span>
                      {{ severityLabel(evt.severity) }}
                    </span>
                    <div class="flex-1 min-w-0">
                      <div class="flex items-center gap-1.5">
                        <span class="text-xs font-semibold text-gray-700 truncate">{{ evt.eventType }}</span>
                        <span class="text-gray-300 text-xs">·</span>
                        <span class="text-xs text-gray-400 truncate">{{ evt.integrationName }}</span>
                      </div>
                      <p class="text-xs text-gray-500 truncate mt-0.5">{{ evt.message }}</p>
                    </div>
                    <span class="text-[11px] text-gray-400 flex-shrink-0">{{ fmtTime(evt.detectedAt) }}</span>
                  </RouterLink>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- ── 위젯: 보안 KPI ── -->
        <template v-else-if="widgetId === 'kpi'">
          <div class="grid grid-cols-2 lg:grid-cols-4 gap-3">
            <RouterLink to="/vulnerabilities?status=OPEN" class="card hover:shadow-md transition-shadow">
              <p class="text-sm text-gray-500">{{ $t('dashboard.openVulnerabilities') }}</p>
              <p class="text-3xl font-bold text-red-600 mt-1">{{ metrics.openVulns }}</p>
              <p class="text-xs text-gray-400 mt-1">기한 초과 <span class="text-red-500 font-semibold">{{ metrics.overdueVulns }}</span>건</p>
            </RouterLink>
            <RouterLink to="/incidents" class="card hover:shadow-md transition-shadow">
              <p class="text-sm text-gray-500">진행 중 인시던트</p>
              <p class="text-3xl font-bold text-orange-600 mt-1">{{ metrics.openIncidents }}</p>
              <p class="text-xs text-gray-400 mt-1">심각(Critical) <span class="text-red-500 font-semibold">{{ metrics.criticalIncidents }}</span>건</p>
            </RouterLink>
            <RouterLink to="/assets" class="card hover:shadow-md transition-shadow">
              <p class="text-sm text-gray-500">{{ $t('metrics.totalAssets') }}</p>
              <p class="text-3xl font-bold text-blue-600 mt-1">{{ metrics.totalAssets }}</p>
              <p class="text-xs text-gray-400 mt-1">고중요도 <span class="text-orange-500 font-semibold">{{ metrics.highCriticalityAssets }}</span>개</p>
            </RouterLink>
            <RouterLink to="/training" class="card hover:shadow-md transition-shadow">
              <p class="text-sm text-gray-500">교육 이수율</p>
              <p class="text-3xl font-bold text-purple-600 mt-1">{{ metrics.trainingCompletionRate }}%</p>
              <div class="w-full bg-gray-100 rounded-full h-1.5 mt-2">
                <div class="h-1.5 rounded-full bg-purple-500 transition-all" :style="{ width: metrics.trainingCompletionRate + '%' }"></div>
              </div>
            </RouterLink>
          </div>
        </template>

        <!-- ── 위젯: 정책 · 취약점 현황 ── -->
        <template v-else-if="widgetId === 'policy'">
          <div class="grid grid-cols-1 lg:grid-cols-3 gap-3">
            <div class="card">
              <div class="flex items-center justify-between mb-3">
                <p class="text-sm font-semibold text-gray-700">정책 인식률</p>
                <span class="text-xl font-bold text-green-600">{{ metrics.policyAckRate }}%</span>
              </div>
              <div class="w-full bg-gray-100 rounded-full h-2.5">
                <div class="h-2.5 rounded-full bg-green-500 transition-all duration-700" :style="{ width: metrics.policyAckRate + '%' }"></div>
              </div>
              <RouterLink to="/policies" class="block mt-3 text-xs text-primary-600 hover:underline">정책 목록 →</RouterLink>
            </div>
            <div class="card">
              <h2 class="text-sm font-semibold text-gray-700 mb-3">취약점 심각도 분포</h2>
              <div class="flex items-center gap-3">
                <canvas ref="doughnutCanvas" width="90" height="90" class="flex-shrink-0"></canvas>
                <div class="space-y-1 text-xs">
                  <div v-for="item in severityLegend" :key="item.label" class="flex items-center justify-between gap-3">
                    <div class="flex items-center gap-1.5">
                      <div class="w-2 h-2 rounded-full flex-shrink-0" :style="{ background: item.color }"></div>
                      <span class="text-gray-600">{{ item.label }}</span>
                    </div>
                    <span class="font-semibold text-gray-800">{{ item.count }}</span>
                  </div>
                </div>
              </div>
            </div>
            <div class="card">
              <h2 class="text-sm font-semibold text-gray-700 mb-3">빠른 이동</h2>
              <div class="grid grid-cols-2 gap-2">
                <RouterLink to="/vulnerabilities/new" class="flex items-center gap-2 p-2.5 rounded-lg bg-red-50 hover:bg-red-100 text-red-700 text-xs font-medium transition-colors">
                  <svg class="w-4 h-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/></svg>
                  취약점 등록
                </RouterLink>
                <RouterLink to="/incidents/new" class="flex items-center gap-2 p-2.5 rounded-lg bg-orange-50 hover:bg-orange-100 text-orange-700 text-xs font-medium transition-colors">
                  <svg class="w-4 h-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/></svg>
                  인시던트 등록
                </RouterLink>
                <RouterLink v-if="isManager" to="/policies/new" class="flex items-center gap-2 p-2.5 rounded-lg bg-blue-50 hover:bg-blue-100 text-blue-700 text-xs font-medium transition-colors">
                  <svg class="w-4 h-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
                  정책 등록
                </RouterLink>
                <RouterLink to="/training" class="flex items-center gap-2 p-2.5 rounded-lg bg-green-50 hover:bg-green-100 text-green-700 text-xs font-medium transition-colors">
                  <svg class="w-4 h-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 14l9-5-9-5-9 5 9 5zm0 0l6.16-3.422a12.083 12.083 0 01.665 6.479A11.952 11.952 0 0012 20.055"/></svg>
                  교육 이수
                </RouterLink>
              </div>
            </div>
          </div>
        </template>

        <!-- ── 위젯: KRCERT RSS ── -->
        <template v-else-if="widgetId === 'rss'">
          <div class="card flex-1 flex flex-col">
            <div class="flex items-center justify-between mb-3">
              <div class="flex items-center gap-2">
                <div class="w-7 h-7 rounded-lg bg-red-600 flex items-center justify-center flex-shrink-0">
                  <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M6 5c7.18 0 13 5.82 13 13M6 11a7 7 0 017 7M6 17a1 1 0 110 2 1 1 0 010-2z"/>
                  </svg>
                </div>
                <h2 class="text-sm font-semibold text-gray-800 flex items-center gap-1.5">
                  {{ rssTitle }}
                  <span class="text-xs text-gray-400 font-normal">최근</span>
                  <!-- 법령: 월 단위 -->
                  <select v-if="activeIsLegal" v-model.number="legalDays" @change="onLegalDaysChange"
                    title="조회 기간 변경"
                    class="text-xs text-gray-600 font-normal border border-gray-200 rounded px-1.5 py-0.5 bg-white cursor-pointer focus:outline-none focus:ring-1 focus:ring-primary-300">
                    <option v-for="p in LEGAL_PERIODS" :key="p.days" :value="p.days">{{ p.label }}</option>
                  </select>
                  <!-- 취약점: 월 단위 -->
                  <select v-else-if="activeIsVuln" v-model.number="vulnDays" @change="onVulnDaysChange"
                    title="조회 기간 변경"
                    class="text-xs text-gray-600 font-normal border border-gray-200 rounded px-1.5 py-0.5 bg-white cursor-pointer focus:outline-none focus:ring-1 focus:ring-primary-300">
                    <option v-for="p in LEGAL_PERIODS" :key="p.days" :value="p.days">{{ p.label }}</option>
                  </select>
                  <!-- 보안공지: 일 단위 -->
                  <select v-else v-model.number="noticeDays" @change="onNoticeDaysChange"
                    title="조회 기간 변경"
                    class="text-xs text-gray-600 font-normal border border-gray-200 rounded px-1.5 py-0.5 bg-white cursor-pointer focus:outline-none focus:ring-1 focus:ring-primary-300">
                    <option v-for="d in NOTICE_PERIODS" :key="d" :value="d">{{ d }}일</option>
                  </select>
                </h2>
              </div>
              <div class="flex gap-1">
                <button v-for="tab in allTabs" :key="tab.category"
                  @click="rssTab = tab.category"
                  :class="rssTab === tab.category ? 'bg-primary-600 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'"
                  class="text-xs px-2.5 py-1 rounded-lg font-medium transition-colors">{{ tab.label }}</button>
              </div>
            </div>

            <div class="flex-1 min-h-0 max-h-[420px] overflow-y-auto overflow-x-hidden">
              <!-- ── 법령 개정 탭 ── -->
              <template v-if="activeIsLegal">
                <div v-if="legalLoading" class="py-8 text-center text-xs text-gray-400">불러오는 중...</div>
                <div v-else-if="legalNoIndustry" class="py-8 text-center text-xs text-gray-400">
                  <RouterLink to="/settings" class="text-primary-600 hover:underline">설정관리 &gt; 업종 설정</RouterLink>에서 업종을 먼저 선택하세요
                </div>
                <div v-else-if="legalKeyMissing" class="py-8 text-center text-xs text-gray-400">
                  <RouterLink to="/settings" class="text-primary-600 hover:underline">설정관리 &gt; API 연동</RouterLink>에서 법제처 API 키를 등록하세요
                </div>
                <div v-else-if="legalError" class="py-8 text-center text-xs text-red-400">{{ legalError }}</div>
                <template v-else>
                  <div v-if="legalItems.length === 0" class="py-8 text-center text-xs text-gray-400">최근 {{ legalPeriodLabel }}간 개정·공포된 법령이 없습니다</div>
                  <div v-else class="divide-y divide-gray-50">
                    <a v-for="item in legalItems.slice(0,8)" :key="item.name"
                      :href="item.url" target="_blank" rel="noopener noreferrer"
                      class="flex items-start gap-3 py-3 hover:bg-gray-50 rounded-lg px-2 -mx-2 transition-colors group">
                      <div class="flex-1 min-w-0">
                        <div class="flex items-center gap-1.5">
                          <span class="text-[10px] font-semibold px-1.5 py-0.5 rounded flex-shrink-0" :class="lawTypeColor(item.type)">{{ item.type }}</span>
                          <p class="text-sm font-medium text-gray-800 group-hover:text-primary-600 transition-colors line-clamp-1">{{ item.lawName }}</p>
                        </div>
                        <p class="text-xs text-gray-400 mt-0.5 line-clamp-1">
                          {{ item.department }}<span v-if="item.amendType"> · {{ item.amendType }}</span><span v-if="item.enforcementDate && item.enforcementDate !== '-'"> · 시행 {{ item.enforcementDate }}</span>
                        </p>
                      </div>
                      <span class="text-[11px] text-gray-400 flex-shrink-0 mt-0.5">공포 {{ item.promulgationDate }}</span>
                    </a>
                  </div>
                </template>
              </template>

              <!-- ── RSS 탭 (취약점 정보 · 보안공지) ── -->
              <template v-else>
                <div v-if="rssLoading" class="py-8 text-center text-xs text-gray-400">불러오는 중...</div>
                <div v-else-if="rssError" class="py-8 text-center text-xs text-red-400">{{ rssError }}</div>
                <template v-else>
                  <div v-if="filteredRss.length === 0" class="py-8 text-center text-xs text-gray-400">최근 {{ rssEmptyLabel }}간 게시물이 없습니다</div>
                  <div v-else class="divide-y divide-gray-50">
                    <a v-for="item in filteredRss" :key="item.link"
                      :href="item.link" target="_blank" rel="noopener noreferrer"
                      class="flex items-start gap-3 py-3 hover:bg-gray-50 rounded-lg px-2 -mx-2 transition-colors group">
                      <div class="flex-1 min-w-0">
                        <p class="text-sm font-medium text-gray-800 group-hover:text-primary-600 transition-colors line-clamp-1">{{ item.title }}</p>
                        <p v-if="item.description" class="text-xs text-gray-400 mt-0.5 line-clamp-1">{{ item.description }}</p>
                      </div>
                      <span class="text-[11px] text-gray-400 flex-shrink-0 mt-0.5">{{ fmtRssDate(item.pubDate) }}</span>
                    </a>
                  </div>
                </template>
              </template>
            </div>
          </div>
        </template>

        <!-- ── 위젯: 월별 트렌드 ── -->
        <template v-else-if="widgetId === 'trends'">
          <div class="grid grid-cols-1 lg:grid-cols-2 gap-3">
            <div class="card">
              <h2 class="text-sm font-semibold text-gray-700 mb-4">{{ $t('metrics.vulnTrend') }}</h2>
              <canvas ref="lineCanvas" height="120"></canvas>
            </div>
            <div class="card">
              <h2 class="text-sm font-semibold text-gray-700 mb-4">취약점 처리 현황</h2>
              <div class="space-y-3">
                <div v-for="item in statusBars" :key="item.key">
                  <div class="flex justify-between text-xs text-gray-600 mb-1">
                    <span>{{ item.label }}</span><span class="font-semibold">{{ item.count }}</span>
                  </div>
                  <div class="w-full bg-gray-100 rounded-full h-2">
                    <div class="h-2 rounded-full transition-all duration-700" :style="{ width: item.pct + '%', background: item.color }"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>

      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { RouterLink } from 'vue-router'
import {
  Chart, DoughnutController, LineController, ArcElement, LineElement,
  PointElement, CategoryScale, LinearScale, Tooltip, Filler
} from 'chart.js'
import { metricsApi, vulnApi, rssApi, appSettingApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import NoticeBar from '@/components/dashboard/NoticeBar.vue'
import { INDUSTRIES } from '@/data/legalIndustries.js'
import { fetchLawMeta } from '@/services/legalApiService.js'

Chart.register(DoughnutController, LineController, ArcElement, LineElement, PointElement, CategoryScale, LinearScale, Tooltip, Filler)

const auth = useAuthStore()
const isManager = auth.isManager
const doughnutCanvas = ref(null)
const lineCanvas = ref(null)

// ── 위젯 순서 관리 ──
const DEFAULT_ORDER = ['rss', 'securityEvents', 'kpi', 'policy', 'trends']
const WIDGET_LABELS = {
  rss:            '보안 · 법령 정보',
  securityEvents: '보안이벤트 현황',
  kpi:            '보안 KPI',
  policy:         '정책 · 취약점 현황',
  trends:         '월별 트렌드'
}

const widgetOrder = ref(
  JSON.parse(localStorage.getItem('dashboard-widget-order') || 'null') || [...DEFAULT_ORDER]
)

const isOrderChanged = computed(() =>
  JSON.stringify(widgetOrder.value) !== JSON.stringify(DEFAULT_ORDER)
)

function resetOrder() {
  widgetOrder.value = [...DEFAULT_ORDER]
  localStorage.removeItem('dashboard-widget-order')
}

// ── 드래그 앤 드롭 ──
const draggingId = ref(null)
const dragOverId = ref(null)

function onDragStart(e, id) {
  draggingId.value = id
  e.dataTransfer.effectAllowed = 'move'
}

function onDragOver(e, id) {
  if (draggingId.value !== id) dragOverId.value = id
}

function onDragLeave(e) {
  if (!e.currentTarget.contains(e.relatedTarget)) dragOverId.value = null
}

function onDrop(e, targetId) {
  if (!draggingId.value || draggingId.value === targetId) {
    draggingId.value = null; dragOverId.value = null; return
  }
  const arr = [...widgetOrder.value]
  const from = arr.indexOf(draggingId.value)
  const to = arr.indexOf(targetId)
  arr.splice(from, 1)
  arr.splice(to, 0, draggingId.value)
  widgetOrder.value = arr
  localStorage.setItem('dashboard-widget-order', JSON.stringify(arr))
  draggingId.value = null
  dragOverId.value = null
  // 차트를 포함하는 위젯이 이동했을 때 재초기화
  nextTick(() => {
    buildDoughnut(rawSeverity.value)
    buildLineChart(metrics.value.vulnTrend)
  })
}

function onDragEnd() {
  draggingId.value = null
  dragOverId.value = null
}

// ── 메트릭 데이터 ──
const metrics = ref({
  openVulns: 0, overdueVulns: 0, totalAssets: 0, highCriticalityAssets: 0,
  openIncidents: 0, criticalIncidents: 0, policyAckRate: 0,
  trainingCompletionRate: 0, vulnTrend: [],
  totalIntegrations: 0, connectedIntegrations: 0,
  events24h: 0, criticalEvents24h: 0, highEvents24h: 0,
  recentSecurityEvents: []
})
const rawSeverity = ref({})

const SEVERITY_COLORS = { CRITICAL: '#dc2626', HIGH: '#ea580c', MEDIUM: '#d97706', LOW: '#16a34a', INFO: '#6b7280' }
const STATUS_COLORS   = { OPEN: '#dc2626', IN_PROGRESS: '#d97706', RESOLVED: '#16a34a', ACCEPTED: '#2563eb', FALSE_POSITIVE: '#6b7280' }
const SEV_META = {
  CRITICAL: { label: '심각', color: '#dc2626' },
  HIGH:     { label: '높음', color: '#ea580c' },
  MEDIUM:   { label: '중간', color: '#d97706' },
  LOW:      { label: '낮음', color: '#2563eb' },
  INFO:     { label: '정보', color: '#6b7280' }
}

const severityLegend = computed(() =>
  ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO'].map(k => ({
    label: k, count: rawSeverity.value[`severity_${k}`] || 0, color: SEVERITY_COLORS[k]
  }))
)

const statusBars = computed(() => {
  const s = rawSeverity.value
  const total = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'ACCEPTED', 'FALSE_POSITIVE']
    .reduce((acc, k) => acc + (s[`status_${k}`] || 0), 0) || 1
  return ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'ACCEPTED', 'FALSE_POSITIVE'].map(k => ({
    key: k, label: k.replace('_', ' '), count: s[`status_${k}`] || 0,
    color: STATUS_COLORS[k], pct: Math.round(((s[`status_${k}`] || 0) / total) * 100)
  }))
})

const secEventSeverities = computed(() => {
  const total = metrics.value.events24h || 1
  return ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO'].map(k => ({
    key: k, label: SEV_META[k].label, color: SEV_META[k].color,
    count: k === 'CRITICAL' ? metrics.value.criticalEvents24h
         : k === 'HIGH'     ? metrics.value.highEvents24h : 0,
    pct:   k === 'CRITICAL' ? Math.round((metrics.value.criticalEvents24h / total) * 100)
         : k === 'HIGH'     ? Math.round((metrics.value.highEvents24h / total) * 100) : 0
  }))
})

// ── 차트 인스턴스 ──
let doughnutChart = null
let lineChart = null

function buildDoughnut(s) {
  if (!doughnutCanvas.value) return
  doughnutChart?.destroy()
  const labels = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO']
  doughnutChart = new Chart(doughnutCanvas.value, {
    type: 'doughnut',
    data: {
      labels,
      datasets: [{ data: labels.map(k => s[`severity_${k}`] || 0), backgroundColor: labels.map(k => SEVERITY_COLORS[k]), borderWidth: 2, borderColor: '#fff' }]
    },
    options: { responsive: false, plugins: { legend: { display: false } } }
  })
}

function buildLineChart(trend) {
  if (!lineCanvas.value || !trend?.length) return
  lineChart?.destroy()
  lineChart = new Chart(lineCanvas.value, {
    type: 'line',
    data: {
      labels: trend.map(p => p.month),
      datasets: [{
        label: '등록 건수', data: trend.map(p => p.count),
        borderColor: '#2563eb', backgroundColor: 'rgba(37,99,235,0.08)',
        fill: true, tension: 0.35, pointBackgroundColor: '#2563eb', pointRadius: 4
      }]
    },
    options: {
      responsive: true,
      plugins: { legend: { display: false } },
      scales: {
        y: { beginAtZero: true, ticks: { stepSize: 1 }, grid: { color: '#f3f4f6' } },
        x: { grid: { display: false } }
      }
    }
  })
}

// ── 보안이벤트 헬퍼 ──
function severityLabel(s)     { return SEV_META[s]?.label || s }
function severityBadgeClass(s) {
  return { CRITICAL: 'bg-red-100 text-red-700', HIGH: 'bg-orange-100 text-orange-700',
           MEDIUM: 'bg-yellow-100 text-yellow-700', LOW: 'bg-blue-100 text-blue-700',
           INFO: 'bg-gray-100 text-gray-600' }[s] || 'bg-gray-100 text-gray-600'
}
function severityDotClass(s) {
  return { CRITICAL: 'bg-red-500', HIGH: 'bg-orange-500', MEDIUM: 'bg-yellow-500',
           LOW: 'bg-blue-500', INFO: 'bg-gray-400' }[s] || 'bg-gray-400'
}
function fmtTime(dt) {
  if (!dt) return ''
  const d = new Date(dt), now = new Date()
  const m = Math.floor((now - d) / 60000)
  if (m < 1) return '방금'
  if (m < 60) return `${m}분 전`
  if (m < 1440) return `${Math.floor(m / 60)}시간 전`
  return d.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })
}

// ── RSS ──
const rssItems = ref([])
const rssLoading = ref(false)
const rssError = ref('')
const rssTab = ref('vuln')
// 취약점·보안공지는 갱신 주기가 달라 조회 기간을 탭별로 따로 관리한다.
const vulnDays = ref(30)    // 취약점: 월 단위 프리셋 (기본 1개월)
const noticeDays = ref(7)   // 보안공지: 일 단위 프리셋 (기본 7일)
const rssTabList = ref([
  { category: 'vuln',   label: '취약점 정보' },
  { category: 'notice', label: '보안공지' }
])

const legalTab = { category: 'legal', label: '법령 개정' }
const allTabs = computed(() => [...rssTabList.value, legalTab])
const activeIsLegal = computed(() => rssTab.value === 'legal')
const activeIsVuln  = computed(() => rssTab.value === 'vuln')
// 법령·취약점 조회 기간 프리셋 (월 단위)
const LEGAL_PERIODS = [
  { days: 7, label: '1주일' },
  { days: 30, label: '1개월' },
  { days: 90, label: '3개월' },
  { days: 180, label: '6개월' },
  { days: 365, label: '12개월' },
]
// 보안공지 조회 기간 프리셋 (일 단위)
const NOTICE_PERIODS = [1, 3, 7, 14, 30]
// 월 단위 기간 라벨(1주일·1개월…)
function daysToPeriodLabel(d) {
  return { 7: '1주일', 30: '1개월', 90: '3개월', 180: '6개월', 365: '12개월' }[d] || `${d}일`
}
const legalPeriodLabel = computed(() => daysToPeriodLabel(legalDays.value))

// 활성 RSS 탭의 조회 기간 및 표시
const activeRssDays = computed(() => activeIsVuln.value ? vulnDays.value : noticeDays.value)
const rssEmptyLabel = computed(() => activeIsVuln.value ? daysToPeriodLabel(vulnDays.value) : `${noticeDays.value}일`)

// 위젯 타이틀 — 탭별로 다르게 표시
const rssTitle = computed(() => {
  if (activeIsLegal.value) return '법령 개정정보'
  if (rssTab.value === 'vuln') return 'KRCERT 취약점정보'
  if (rssTab.value === 'notice') return 'KRCERT 보안공지'
  const t = allTabs.value.find(x => x.category === rssTab.value)
  return 'KRCERT ' + (t?.label || '')
})

// 날짜 필터는 백엔드(rss.days/days 파라미터)가 수행하므로 여기서는 탭(카테고리)만 분류
const filteredRss = computed(() =>
  rssItems.value.filter(item => item.category === rssTab.value)
)

function fmtRssDate(pubDate) {
  if (!pubDate) return ''
  try {
    const d = new Date(pubDate)
    if (isNaN(d)) return pubDate
    return d.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' })
  } catch {
    return pubDate
  }
}

async function loadRss() {
  rssLoading.value = true
  rssError.value = ''
  try {
    const res = await rssApi.krcert(activeRssDays.value)
    rssItems.value = res.data || []
  } catch {
    rssError.value = 'RSS 데이터를 불러오지 못했습니다'
  } finally {
    rssLoading.value = false
  }
}

async function loadRssSettings() {
  try {
    const res = await import('@/api').then(m => m.appSettingApi.getAll())
    const s = res.data || {}
    if (s['rss.days']) noticeDays.value = parseInt(s['rss.days']) || 7
    if (s['rss.vuln.days']) vulnDays.value = parseInt(s['rss.vuln.days']) || 30
    if (s['rss.feeds']) {
      const feeds = JSON.parse(s['rss.feeds'])
      if (Array.isArray(feeds) && feeds.length > 0) {
        rssTabList.value = feeds.map(f => ({ category: f.category, label: f.label || f.category }))
        rssTab.value = rssTabList.value[0]?.category || 'vuln'
      }
    }
    // 법령 개정 위젯 설정
    if (s['legal.days']) legalDays.value = parseInt(s['legal.days']) || 30
    lawKeyPresent.value = !!(s['lawApiKey'] && s['lawApiKey'].trim())
    const rawInd = s['company.industries']
    if (rawInd) {
      try { const ids = JSON.parse(rawInd); if (Array.isArray(ids)) companyIndustryIds.value = ids } catch {}
    }
    const rawIndLaws = s['company.industryLaws']
    if (rawIndLaws) {
      try { const m = JSON.parse(rawIndLaws); if (m && typeof m === 'object') companyIndustryLaws.value = m } catch {}
    }
    // 설정 로드가 탭 클릭보다 늦게 끝난 경우, 법령 탭이 열려 있으면 갱신된 값으로 재조회
    if (rssTab.value === 'legal') { legalLoaded.value = false; loadLegal() }
  } catch {}
}

// ── 법령 개정 정보 (법제처 Open API) ──
const legalItems   = ref([])
const legalLoading = ref(false)
const legalError   = ref('')
const legalLoaded  = ref(false)
const legalKeyMissing   = ref(false)
const legalDays    = ref(30)
const lawKeyPresent = ref(false)
const companyIndustryIds = ref([])
// 업종별 개별 법령 선택 { [업종id]: [법령명, ...] }. 항목 없으면 전체 법령 적용.
const companyIndustryLaws = ref({})

// 선택된 업종의 관련 법령 (법령명 기준 중복 제거, 업종별 개별 선택 반영)
const legalLaws = computed(() => {
  const seen = new Set(); const out = []
  for (const ind of INDUSTRIES) {
    if (!companyIndustryIds.value.includes(ind.id)) continue
    const sel = companyIndustryLaws.value[ind.id]
    for (const law of ind.laws) {
      if (Array.isArray(sel) && !sel.includes(law.name)) continue
      if (seen.has(law.name)) continue
      seen.add(law.name); out.push(law)
    }
  }
  return out
})
const legalNoIndustry = computed(() => companyIndustryIds.value.length === 0)

function lawTypeColor(type) {
  return {
    '법령': 'bg-blue-100 text-blue-700', '시행령': 'bg-indigo-100 text-indigo-700',
    '시행규칙': 'bg-violet-100 text-violet-700', '시행세칙': 'bg-purple-100 text-purple-700',
    '규정': 'bg-teal-100 text-teal-700', '고시': 'bg-amber-100 text-amber-700',
  }[type] || 'bg-gray-100 text-gray-600'
}

const LEGAL_CACHE_KEY = 'legal-meta-cache-v1'
const LEGAL_TTL = 12 * 3600 * 1000  // 12시간

function cutoffYmd(days) {
  const d = new Date(); d.setDate(d.getDate() - days)
  return `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, '0')}${String(d.getDate()).padStart(2, '0')}`
}

async function loadLegal() {
  legalLoaded.value = true
  legalError.value = ''
  legalKeyMissing.value = false
  const laws = legalLaws.value
  if (laws.length === 0) { legalItems.value = []; return }
  if (!lawKeyPresent.value) { legalKeyMissing.value = true; legalItems.value = []; return }

  legalLoading.value = true
  try {
    let cache = {}
    try { cache = JSON.parse(localStorage.getItem(LEGAL_CACHE_KEY) || '{}') } catch {}
    const now = Date.now()
    const results = []
    const queue = [...laws]

    async function worker() {
      while (queue.length) {
        const law = queue.shift()
        const cached = cache[law.name]
        let meta
        if (cached && (now - cached.ts) < LEGAL_TTL) {
          meta = cached.meta
        } else {
          try { meta = await fetchLawMeta(law.name) } catch { meta = null }
          cache[law.name] = { meta, ts: now }
        }
        if (meta) results.push({ ...law, ...meta })
      }
    }
    await Promise.all([worker(), worker(), worker(), worker()])
    try { localStorage.setItem(LEGAL_CACHE_KEY, JSON.stringify(cache)) } catch {}

    const cutoff = cutoffYmd(legalDays.value)
    legalItems.value = results
      .filter(r => r.promulgationRaw && r.promulgationRaw >= cutoff)
      .sort((a, b) => b.promulgationRaw.localeCompare(a.promulgationRaw))
  } catch {
    legalError.value = '법령 정보를 불러오지 못했습니다'
  } finally {
    legalLoading.value = false
  }
}

// 탭 전환 시: 법령 탭은 최초 1회 조회, RSS 탭은 해당 탭의 조회 기간으로 재조회
watch(rssTab, (t) => {
  if (t === 'legal') { if (!legalLoaded.value) loadLegal() }
  else loadRss()
})

// 대시보드에서 직접 조회 기간 변경 → 즉시 재조회, 관리자는 설정값도 함께 갱신
async function onLegalDaysChange() {
  legalLoaded.value = false
  loadLegal()
  if (auth.isAdmin) {
    try { await appSettingApi.update('legal.days', String(legalDays.value)) } catch {}
  }
}

// RSS 조회 기간 변경 → 선택 기간으로 서버 재조회, 관리자는 설정값도 갱신 (탭별 분리)
async function onVulnDaysChange() {
  loadRss()
  if (auth.isAdmin) {
    try { await appSettingApi.update('rss.vuln.days', String(vulnDays.value)) } catch {}
  }
}
async function onNoticeDaysChange() {
  loadRss()
  if (auth.isAdmin) {
    try { await appSettingApi.update('rss.days', String(noticeDays.value)) } catch {}
  }
}

onMounted(async () => {
  // 설정(rss.days 등)을 먼저 로드한 뒤 그 기간으로 RSS 조회
  await loadRssSettings()
  loadRss()
  try {
    const [metricsRes, vulnStatsRes] = await Promise.all([
      metricsApi.summary(),
      vulnApi.stats()
    ])
    metrics.value   = metricsRes.data
    rawSeverity.value = vulnStatsRes.data
    await nextTick()
    buildDoughnut(vulnStatsRes.data)
    buildLineChart(metricsRes.data.vulnTrend)
  } catch (e) {
    console.error(e)
  }
})
</script>
