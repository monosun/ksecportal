<template>
  <div
    class="fixed inset-0 z-50 flex items-start justify-center bg-black/50 overflow-y-auto p-4"
    @click.self="$emit('close')"
  >
    <div
      class="bg-white rounded-2xl shadow-2xl w-full max-w-6xl my-6 flex flex-col"
      style="max-height: calc(100vh - 3rem)"
    >
      <!-- ── 헤더 ───────────────────────────────────── -->
      <div class="flex items-center justify-between px-6 py-4 border-b border-gray-100 flex-shrink-0">
        <div>
          <h2 class="text-base font-bold text-gray-800">법령 검토</h2>
          <p class="text-xs text-gray-400 mt-0.5">
            <template v-if="laws.length > 0">
              선택 법령 {{ laws.length }}건 &middot; law.go.kr 실시간 전체 조회 &middot;
              이전 검토와 달라진 조문은 <span class="text-red-500 font-semibold">빨간색</span>으로 표시됩니다
            </template>
            <template v-else>법령준수관리에서 검토할 법령을 선택하세요</template>
          </p>
        </div>
        <div class="flex items-center gap-2">
          <!-- 이력 패널 토글 (선택 법령 전체 단위) -->
          <button
            v-if="laws.length > 0"
            @click="showHistoryPanel = !showHistoryPanel"
            class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border text-xs font-semibold transition-colors"
            :class="showHistoryPanel
              ? 'border-indigo-400 bg-indigo-50 text-indigo-700'
              : 'border-gray-200 bg-white text-gray-600 hover:bg-gray-50'"
          >
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
            검토 이력{{ sessionHistories.length > 0 ? ` (${sessionHistories.length})` : '' }}
          </button>
          <!-- 다운로드 -->
          <button
            v-if="downloadableLawCount > 0"
            @click="downloadAllReviews"
            class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold transition-colors shadow-sm"
          >
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
            </svg>
            검토결과 다운로드 ({{ downloadableLawCount }}건)
          </button>
          <button @click="$emit('close')"
            class="p-1.5 hover:bg-gray-100 rounded-lg text-gray-400 hover:text-gray-600 transition-colors">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
      </div>

      <!-- ── 탭 바 ────────────────────────────────────── -->
      <div class="px-4 py-3 border-b border-gray-100 flex-shrink-0">
        <div v-if="laws.length === 0" class="text-xs text-gray-400 py-1">
          법령준수관리에서 법령을 선택한 후 검토를 시작하세요.
        </div>
        <div v-else class="flex flex-wrap gap-1.5">
          <button
            v-for="(law, idx) in laws" :key="law.name"
            @click="selectTab(idx)"
            class="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg border text-xs font-semibold transition-all"
            :class="tabClass(idx)"
          >
            <span class="w-2.5 h-2.5 rounded-full flex-shrink-0 transition-colors" :class="tabDotClass(idx)"></span>
            {{ law.name }}
          </button>
        </div>
        <!-- 전체 실시간 조회 진행률 -->
        <div v-if="loadingAll" class="mt-2.5 flex items-center gap-2 text-[11px] text-gray-500">
          <svg class="w-3.5 h-3.5 animate-spin text-primary-400 flex-shrink-0" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
          </svg>
          law.go.kr 실시간 전체 조회 중... ({{ loadProgress.done }}/{{ loadProgress.total }})
          <span class="font-medium text-gray-600 truncate">{{ loadProgress.current }}</span>
        </div>
      </div>

      <!-- ── 법령 메타 바 ─────────────────────────────── -->
      <div v-if="activeMeta || activeSourceLink" class="px-6 py-2.5 bg-blue-50/70 border-b border-blue-100 flex-shrink-0">
        <div class="flex flex-wrap items-center gap-x-5 gap-y-1 text-xs">
          <template v-if="activeMeta">
            <span class="text-gray-500"><span class="font-semibold text-gray-700">공포일자</span> {{ activeMeta.promulgationDate || '-' }}</span>
            <span class="text-gray-500"><span class="font-semibold text-gray-700">시행일자</span> {{ activeMeta.enforcementDate || '-' }}</span>
            <span v-if="activeMeta.amendType" class="text-gray-500">
              <span class="font-semibold text-gray-700">제개정</span> {{ activeMeta.amendType }}
              <template v-if="activeMeta.amendDate"> ({{ activeMeta.amendDate }})</template>
            </span>
            <span class="text-gray-500"><span class="font-semibold text-gray-700">소관부처</span> {{ activeMeta.department || '-' }}</span>
            <span class="text-gray-500"><span class="font-semibold text-gray-700">유형</span> {{ activeMeta.lawType || '-' }}</span>
          </template>
          <span v-else class="text-gray-500">{{ activeLawName }}</span>
          <a v-if="activeSourceLink" :href="activeSourceLink" target="_blank" rel="noopener noreferrer"
            class="inline-flex items-center gap-0.5 text-blue-600 hover:text-blue-800 font-medium">
            원문 보기 (law.go.kr)
            <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"/>
            </svg>
          </a>
        </div>
      </div>

      <!-- ── 본문 (2-col when history open) ───────────── -->
      <div class="flex-1 overflow-hidden flex min-h-0">

        <!-- 조문 영역 -->
        <div class="flex-1 overflow-y-auto">
          <div v-if="activeIdx === -1 && laws.length > 0"
            class="flex flex-col items-center justify-center py-20 gap-2 text-gray-400">
            <svg class="w-10 h-10 opacity-30" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
            </svg>
            <p class="text-sm">위의 탭에서 검토할 법령을 선택하세요.</p>
          </div>
          <div v-else-if="laws.length === 0"
            class="flex items-center justify-center py-20 text-sm text-gray-400">
            법령준수관리에서 법령을 선택해 주세요.
          </div>
          <div v-else-if="activeTabLoading"
            class="flex flex-col items-center justify-center py-20 gap-3">
            <svg class="w-9 h-9 animate-spin text-primary-400" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
            </svg>
            <p class="text-sm text-gray-500">법령 조문 전체 조회 중...</p>
          </div>
          <div v-else-if="activeIdx >= 0 && !activeTabLoading && activeArticles.length === 0"
            class="flex items-center justify-center py-20 text-sm text-gray-400">
            조문 데이터를 불러올 수 없습니다.
          </div>
          <div v-else class="px-6 py-4 space-y-1.5">
            <!-- 이전 검토 대비 변경 배너 -->
            <div v-if="activeDiff" class="rounded-xl border px-4 py-2.5 text-xs mb-3"
              :class="activeDiffHasChanges
                ? 'border-red-200 bg-red-50 text-red-700'
                : activeDiff.isFirst
                  ? 'border-blue-200 bg-blue-50 text-blue-700'
                  : 'border-emerald-200 bg-emerald-50 text-emerald-700'">
              <template v-if="activeDiff.isFirst">
                첫 검토입니다. "전체 저장" 클릭 시 조회 내용이 이력으로 저장되어 다음 검토 시 비교 기준이 됩니다.
              </template>
              <template v-else-if="activeDiffHasChanges">
                이전 검토({{ activeDiff.prevDate }}) 이후 변경·신설된 조문
                <span class="font-bold">{{ activeDiff.changedCount }}건</span>이
                <span class="font-bold">빨간색</span>으로 표시됩니다.
                <span v-if="activeDiff.deletedNos?.length" class="block mt-0.5">
                  삭제된 조문: {{ activeDiff.deletedNos.join(', ') }}
                </span>
              </template>
              <template v-else>
                이전 검토({{ activeDiff.prevDate }})와 비교하여 변경된 조문이 없습니다.
              </template>
            </div>
            <template v-for="(art, artIdx) in activeArticles" :key="artIdx">
              <!-- 장/편/절 헤더 -->
              <div v-if="isHeader(art)"
                class="px-4 py-2 rounded-lg border font-bold text-sm mt-4 first:mt-0"
                :class="artDiff(artIdx)
                  ? 'bg-red-50 border-red-200 text-red-600'
                  : 'bg-gray-50 border-gray-200 text-gray-700'">
                {{ art.no }} {{ art.title }}
              </div>
              <!-- 일반 조문 -->
              <div v-else class="border rounded-xl overflow-hidden"
                :class="artDiff(artIdx) ? 'border-red-200' : 'border-gray-200'">
                <div class="flex items-center gap-3 px-4 py-2.5 bg-gray-50/80 border-b border-gray-100">
                  <span class="text-sm font-semibold flex-1 min-w-0"
                    :class="artDiff(artIdx)?.isNew || artDiff(artIdx)?.titleChanged ? 'text-red-600' : 'text-gray-800'">
                    {{ art.no }}{{ art.title ? ` (${art.title})` : '' }}
                  </span>
                  <span v-if="artDiff(artIdx)"
                    class="flex-shrink-0 px-2 py-0.5 rounded text-[10px] font-bold bg-red-100 text-red-600">
                    {{ artDiff(artIdx).isNew ? '신설' : '변경' }}
                  </span>
                  <span class="flex-shrink-0 px-2 py-0.5 rounded text-[10px] font-semibold"
                    :class="artBadgeClass(artIdx)">
                    {{ artBadgeLabel(artIdx) }}
                  </span>
                </div>
                <div v-if="art.clauses?.length"
                  class="px-4 py-3 text-xs space-y-1 leading-relaxed bg-white">
                  <p v-for="(clause, ci) in art.clauses" :key="ci"
                    :class="isClauseChanged(artIdx, ci) ? 'text-red-600 font-medium' : 'text-gray-600'">{{ clause }}</p>
                </div>
                <!-- 검토 입력 -->
                <div class="px-4 pb-3 pt-2 bg-gray-50/40 border-t border-gray-100">
                  <textarea
                    :value="getInput(artIdx)"
                    @input="onInput(artIdx, $event.target.value)"
                    rows="2"
                    placeholder="검토 내용을 입력하세요..."
                    class="w-full text-xs border border-gray-200 rounded-lg px-3 py-2 resize-none focus:outline-none focus:border-primary-400 bg-white"
                  />
                  <!-- 비교 모드: 이전 이력 텍스트 표시 -->
                  <div v-if="compareEntry && getHistoryReview(artIdx)" class="mt-2 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2">
                    <p class="text-[10px] font-semibold text-amber-700 mb-1">
                      이전 이력 ({{ compareEntry.date }})
                    </p>
                    <div class="text-xs leading-relaxed">
                      <template v-for="(seg, si) in diffSegments(getHistoryReview(artIdx), getInput(artIdx))" :key="si">
                        <br v-if="seg.text === '\n'" />
                        <span v-else :class="seg.changed ? 'text-red-600 font-medium bg-red-50 rounded px-0.5' : 'text-gray-600'">{{ seg.text }}</span>
                      </template>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </div>

        <!-- ── 이력 패널 (선택 법령 전체 세션 단위) ──────── -->
        <div v-if="showHistoryPanel && laws.length > 0"
          class="w-72 border-l border-gray-200 flex flex-col flex-shrink-0 overflow-hidden bg-gray-50/50">
          <div class="px-4 py-3 border-b border-gray-200 bg-white flex items-center justify-between flex-shrink-0">
            <h3 class="text-xs font-bold text-gray-700">검토 이력 (전체 법령)</h3>
            <span class="text-[10px] text-gray-400">{{ sessionHistories.length }}건</span>
          </div>
          <div v-if="sessionHistories.length === 0"
            class="flex-1 flex items-center justify-center text-xs text-gray-400 py-8 px-4 text-center">
            저장된 이력이 없습니다.<br>"전체 저장" 클릭 시 검토 이력이 생성됩니다.
          </div>
          <div v-else class="flex-1 overflow-y-auto p-3 space-y-2">
            <div
              v-for="entry in sessionHistories" :key="entry.id"
              class="rounded-xl border p-3 bg-white transition-all"
              :class="compareEntry?.id === entry.id
                ? 'border-amber-400 ring-1 ring-amber-300 bg-amber-50'
                : 'border-gray-200 hover:border-gray-300'"
            >
              <div class="flex items-start justify-between gap-2">
                <div class="min-w-0">
                  <p class="text-xs font-semibold text-gray-700 truncate">
                    {{ entry.date }}
                    <span class="ml-1 px-1 py-0 rounded text-[9px] font-bold align-middle"
                      :class="entry.kind === 'auto' ? 'bg-indigo-50 text-indigo-500' : 'bg-primary-50 text-primary-600'">
                      {{ entry.kind === 'auto' ? '이전 검토' : '전체 저장' }}
                    </span>
                  </p>
                  <p class="text-[10px] text-gray-400 mt-0.5">
                    법령 {{ Object.keys(entry.laws ?? {}).length }}건 · 검토의견 {{ sessionReviewCount(entry) }}개
                  </p>
                </div>
                <div class="flex gap-1 flex-shrink-0">
                  <button
                    @click="toggleCompare(entry)"
                    class="px-2 py-1 rounded text-[10px] font-semibold transition-colors"
                    :class="compareEntry?.id === entry.id
                      ? 'bg-amber-500 text-white'
                      : 'bg-gray-100 text-gray-600 hover:bg-amber-100 hover:text-amber-700'"
                  >
                    {{ compareEntry?.id === entry.id ? '비교중' : '비교' }}
                  </button>
                  <button
                    @click="confirmDeleteHistory(entry)"
                    class="px-2 py-1 rounded text-[10px] font-semibold bg-gray-100 text-gray-500 hover:bg-red-100 hover:text-red-600 transition-colors"
                  >삭제</button>
                </div>
              </div>
              <!-- 이력 미리보기 -->
              <div v-if="compareEntry?.id === entry.id" class="mt-2 pt-2 border-t border-amber-200">
                <p class="text-[10px] text-amber-700 font-medium">
                  빨간색: 현재와 다른 내용
                </p>
              </div>
            </div>
          </div>
          <!-- 이력 패널 안내 -->
          <div class="px-4 py-2 border-t border-gray-200 bg-white flex-shrink-0">
            <p class="text-[10px] text-gray-400">
              "전체 저장" 클릭 시 선택 법령 전체의 조문 스냅샷과 검토의견이
              하나의 이력으로 저장되어 다음 검토 시 비교 기준이 됩니다.
            </p>
          </div>
        </div>
      </div>

      <!-- ── 하단 저장 바 ────────────────────────────── -->
      <div v-if="activeIdx >= 0 && !activeTabLoading && activeArticles.length > 0"
        class="border-t border-gray-100 px-6 py-3 flex items-center justify-between flex-shrink-0 bg-gray-50/60">
        <div class="flex items-center gap-3">
          <span class="text-xs text-gray-500">
            {{ activeLawName }} {{ activeInputCount }}개 &nbsp;·&nbsp; 전체 작성된 검토:
            <span class="font-semibold text-gray-700">{{ totalReviewCount }}개</span> 조문
          </span>
          <span v-if="saveSuccess" class="text-xs text-green-600 font-semibold flex items-center gap-1">
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M5 13l4 4L19 7"/>
            </svg>
            저장 완료
          </span>
        </div>
        <button
          @click="saveAllReviews"
          :disabled="totalReviewCount === 0"
          class="inline-flex items-center gap-1.5 px-4 py-2 rounded-lg text-xs font-bold transition-all shadow-sm"
          :class="totalReviewCount > 0
            ? 'bg-primary-600 hover:bg-primary-700 text-white'
            : 'bg-gray-200 text-gray-400 cursor-not-allowed'"
        >
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4"/>
          </svg>
          전체 저장 (이력 생성)
        </button>
      </div>
    </div>

    <!-- 삭제 확인 다이얼로그 -->
    <div v-if="deleteTarget"
      class="fixed inset-0 flex items-center justify-center bg-black/40" style="z-index: 60"
      @click.self="deleteTarget = null">
      <div class="bg-white rounded-2xl shadow-xl p-6 w-80">
        <h3 class="text-sm font-bold text-gray-800 mb-2">이력 삭제</h3>
        <p class="text-xs text-gray-500 mb-4">
          <span class="font-semibold text-gray-700">{{ deleteTarget.date }}</span> 이력을 삭제하시겠습니까?<br>
          삭제 후 복구할 수 없습니다.
        </p>
        <div class="flex justify-end gap-2">
          <button @click="deleteTarget = null"
            class="px-3 py-1.5 rounded-lg text-xs text-gray-600 bg-gray-100 hover:bg-gray-200 font-semibold">취소</button>
          <button @click="doDeleteHistory"
            class="px-3 py-1.5 rounded-lg text-xs text-white bg-red-500 hover:bg-red-600 font-semibold">삭제</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import * as XLSX from 'xlsx-js-style'
import { fetchLawFull } from '@/services/legalApiService.js'
import { LAW_ARTICLES } from '@/data/legalArticles.js'
import { appSettingApi } from '@/api/index.js'
import { useAuthStore } from '@/stores/auth.js'

const props = defineProps({ laws: { type: Array, default: () => [] } })
defineEmits(['close'])

const auth = useAuthStore()
const companyName = ref('')

onMounted(() => {
  appSettingApi.getAll()
    .then(res => {
      const s = res?.data || {}
      // 설정관리 > 회사정보의 회사명 우선, 없으면 로고 텍스트
      companyName.value = s['company.name'] || s.logo_text || ''
    })
    .catch(() => {})
  // 검토 이력(전체 법령 세션 단위) 로드 후 전체 법령 실시간 조회 시작
  loadSessionHistories()
  loadAllLaws()
})

// ── 탭 상태 ──────────────────────────────────────────
const activeIdx   = ref(-1)
const tabStates   = ref({})
const tabArticles = ref({})
const tabMeta     = ref({})

const activeLawName = computed(() => props.laws[activeIdx.value]?.name ?? null)
const activeLaw     = computed(() => props.laws[activeIdx.value] ?? null)
const activeTabLoading = computed(() =>
  activeIdx.value >= 0 && tabStates.value[activeLawName.value] === 'loading'
)
const activeArticles = computed(() =>
  activeIdx.value >= 0 ? (tabArticles.value[activeLawName.value] ?? []) : []
)
const activeMeta = computed(() =>
  activeIdx.value >= 0 ? (tabMeta.value[activeLawName.value] ?? null) : null
)
// 원문보기 링크: 실시간 조회 메타의 링크 우선, 없으면 법령 데이터의 law.go.kr URL
const activeSourceLink = computed(() =>
  activeMeta.value?.link || activeLaw.value?.url || null
)

function isHeader(art) {
  return !art.clauses || art.clauses.length === 0
}

async function selectTab(idx) {
  activeIdx.value = idx
  compareEntryId.value = null  // 탭 전환 시 비교 해제
  const lawName = props.laws[idx]?.name
  if (!lawName) return
  await loadLaw(lawName)
}

// ── 전체 법령 실시간 조회 (법령검토 클릭 시 law.go.kr 순차 호출) ──
const loadingAll  = ref(false)
const loadProgress = ref({ done: 0, total: 0, current: '' })

async function loadAllLaws() {
  if (!props.laws.length) return
  loadingAll.value = true
  loadProgress.value = { done: 0, total: props.laws.length, current: '' }
  if (activeIdx.value === -1) activeIdx.value = 0
  for (let i = 0; i < props.laws.length; i++) {
    const lawName = props.laws[i].name
    loadProgress.value = { done: i, total: props.laws.length, current: lawName }
    await loadLaw(lawName)
  }
  loadProgress.value = { done: props.laws.length, total: props.laws.length, current: '' }
  loadingAll.value = false
  // 전체 조회 완료 후 이전 이력 대비 변경 조문만 계산 (이력 기록은 "전체 저장" 클릭 시)
  computeAllDiffs()
}

async function loadLaw(lawName) {
  if (tabStates.value[lawName]) return  // 이미 조회 중이거나 완료
  tabStates.value = { ...tabStates.value, [lawName]: 'loading' }
  try {
    const { articles, meta } = await fetchLawFull(lawName)
    tabArticles.value = {
      ...tabArticles.value,
      [lawName]: articles?.length ? articles : (LAW_ARTICLES[lawName] ?? [])
    }
    if (meta) tabMeta.value = { ...tabMeta.value, [lawName]: meta }
  } catch {
    tabArticles.value = { ...tabArticles.value, [lawName]: LAW_ARTICLES[lawName] ?? [] }
  }
  tabStates.value = { ...tabStates.value, [lawName]: 'loaded' }
  loadDrafts(lawName)
}

// ── 검토 입력 (법령별 드래프트) ──────────────────────
const allInputs = ref({})  // `${lawName}__${artIdx}` → text

function inputKey(lawName, artIdx) { return `${lawName}__${artIdx}` }
function draftLsKey(lawName, artIdx) { return `lrD__${lawName}__${artIdx}` }

function loadDrafts(lawName) {
  const arts = tabArticles.value[lawName] ?? []
  const ni = { ...allInputs.value }
  for (let i = 0; i < arts.length; i++) {
    const ik = inputKey(lawName, i)
    if (ik in ni) continue
    const v = localStorage.getItem(draftLsKey(lawName, i))
    if (v !== null && v !== '') ni[ik] = v
  }
  allInputs.value = ni
}

function getInput(artIdx) {
  if (!activeLawName.value) return ''
  return allInputs.value[inputKey(activeLawName.value, artIdx)] ?? ''
}

function onInput(artIdx, value) {
  const lawName = activeLawName.value
  if (!lawName) return
  const ik = inputKey(lawName, artIdx)
  allInputs.value = { ...allInputs.value, [ik]: value }
  if (value.trim()) localStorage.setItem(draftLsKey(lawName, artIdx), value)
  else localStorage.removeItem(draftLsKey(lawName, artIdx))
}

const activeInputCount = computed(() => {
  const lawName = activeLawName.value
  if (!lawName) return 0
  return activeArticles.value.filter((_, i) => (allInputs.value[inputKey(lawName, i)] ?? '').trim()).length
})

// 전체 법령의 작성된 검토의견 수 (전체 저장 버튼 활성화 기준)
const totalReviewCount = computed(() => {
  let n = 0
  for (const law of props.laws) {
    const arts = tabArticles.value[law.name] ?? []
    for (let i = 0; i < arts.length; i++) {
      if ((allInputs.value[inputKey(law.name, i)] ?? '').trim()) n++
    }
  }
  return n
})

// ── 조문 뱃지 ────────────────────────────────────────
function artBadgeClass(artIdx) {
  const lawName = activeLawName.value
  if (!lawName) return 'bg-gray-100 text-gray-500'
  const text = (allInputs.value[inputKey(lawName, artIdx)] ?? '').trim()
  if (!text) return 'bg-gray-100 text-gray-500'
  const latest = latestLawEntry(lawName)
  if (latest && (latest.entry.reviews?.[artIdx] ?? '').trim() === text) return 'bg-blue-100 text-blue-700'
  return 'bg-amber-100 text-amber-700'
}
function artBadgeLabel(artIdx) {
  const lawName = activeLawName.value
  if (!lawName) return '미검토'
  const text = (allInputs.value[inputKey(lawName, artIdx)] ?? '').trim()
  if (!text) return '미검토'
  const latest = latestLawEntry(lawName)
  if (latest && (latest.entry.reviews?.[artIdx] ?? '').trim() === text) return '저장됨'
  return '미저장'
}

// ── 탭 dot & 클래스 ──────────────────────────────────
function tabStatus(idx) {
  const lawName = props.laws[idx]?.name
  if (!lawName) return 'unloaded'
  const state = tabStates.value[lawName]
  if (!state) return 'unloaded'
  if (state === 'loading') return 'loading'
  const hasHistory = !!latestLawEntry(lawName)
  const arts = tabArticles.value[lawName] ?? []
  const hasDraft = arts.some((_, i) => (allInputs.value[inputKey(lawName, i)] ?? '').trim())
  if (hasHistory) return 'saved'
  if (hasDraft)   return 'draft'
  return 'loaded'
}
function tabDotClass(idx) {
  const s = tabStatus(idx)
  if (s === 'unloaded') return 'border-2 border-gray-300 bg-white'
  if (s === 'loading')  return 'bg-gray-300 animate-pulse'
  if (s === 'saved')    return 'bg-blue-500'
  if (s === 'draft')    return 'bg-amber-400'
  return 'bg-gray-400'
}
function tabClass(idx) {
  const s = tabStatus(idx)
  const active = idx === activeIdx.value
  let color
  if (s === 'unloaded' || s === 'loading') color = 'border-gray-200 bg-white text-gray-500'
  else if (s === 'saved')  color = 'border-blue-300 bg-blue-50 text-blue-700'
  else if (s === 'draft')  color = 'border-amber-300 bg-amber-50 text-amber-700'
  else                     color = 'border-gray-300 bg-gray-50 text-gray-600'
  return `${color} ${active ? 'ring-2 ring-offset-1 ring-primary-400' : 'hover:opacity-80'}`
}

// ── 이력 관리 (선택 법령 전체 세션 단위) ─────────────────
// 세션 엔트리: { id, kind, date, laws: { [lawName]: { hash, articles|null, reviews } } }
// articles가 null이면 더 오래된 세션의 같은 법령 스냅샷과 내용 동일 (용량 절약)
const SESS_KEY = 'lrSessH'
const sessionHistories = ref([])
const showHistoryPanel = ref(false)
const compareEntryId   = ref(null)
const saveSuccess      = ref(false)
const deleteTarget     = ref(null)

function loadSessionHistories() {
  try {
    sessionHistories.value = JSON.parse(localStorage.getItem(SESS_KEY) || '[]')
  } catch {
    sessionHistories.value = []
  }
  if (!sessionHistories.value.length) migrateOldHistories()
}

// 구 버전(법령별 lrH__ 키) 이력 → 각 법령의 최신 스냅샷을 모아 세션 1건으로 이관
function migrateOldHistories() {
  const laws = {}
  let newestDate = null
  for (const law of props.laws) {
    try {
      const old = JSON.parse(localStorage.getItem(`lrH__${law.name}`) || '[]')
      if (!old.length) continue
      const latest = old[0]
      const snapshot = old.find(e => e.articles?.length)?.articles ?? null
      laws[law.name] = { hash: latest.hash, articles: snapshot, reviews: latest.reviews ?? {} }
      if (!newestDate || latest.date > newestDate) newestDate = latest.date
    } catch { /* 손상된 구 이력은 건너뜀 */ }
  }
  if (!Object.keys(laws).length) return
  sessionHistories.value = persistSessions([
    { id: newEntryId(), kind: 'auto', date: newestDate ?? nowStamp(), laws },
  ])
}

// 특정 법령의 가장 최근 세션 엔트리
function latestLawEntry(lawName) {
  for (const s of sessionHistories.value) {
    if (s.laws?.[lawName]) return { session: s, entry: s.laws[lawName] }
  }
  return null
}

// 특정 법령의 가장 최근 조문 스냅샷 (null 엔트리는 더 오래된 스냅샷과 동일)
function resolveLawSnapshot(lawName) {
  for (const s of sessionHistories.value) {
    const e = s.laws?.[lawName]
    if (e?.articles?.length) return e.articles
  }
  return null
}

function sessionReviewCount(entry) {
  let n = 0
  for (const l of Object.values(entry.laws ?? {})) n += Object.keys(l.reviews ?? {}).length
  return n
}

const compareEntry = computed(() => {
  if (!compareEntryId.value) return null
  return sessionHistories.value.find(h => h.id === compareEntryId.value) ?? null
})

function getHistoryReview(artIdx) {
  const lawName = activeLawName.value
  if (!lawName) return ''
  return compareEntry.value?.laws?.[lawName]?.reviews?.[artIdx] ?? ''
}

function toggleCompare(entry) {
  if (compareEntryId.value === entry.id) {
    compareEntryId.value = null
  } else {
    compareEntryId.value = entry.id
    showHistoryPanel.value = true
  }
}

function confirmDeleteHistory(entry) {
  deleteTarget.value = entry
}

function doDeleteHistory() {
  if (!deleteTarget.value) return
  const prev = sessionHistories.value
  const delIdx = prev.findIndex(h => h.id === deleteTarget.value.id)
  if (delIdx === -1) { deleteTarget.value = null; return }
  const deleted = prev[delIdx]
  let next = prev.filter((_, i) => i !== delIdx)
  // 삭제 세션이 스냅샷 보유 시, 같은 내용을 참조하던 바로 위(더 최신) 세션에 법령별로 승계
  if (delIdx - 1 >= 0) {
    const above = next[delIdx - 1]
    if (above) {
      let aboveLaws = above.laws ?? {}
      let changed = false
      for (const [lawName, e] of Object.entries(deleted.laws ?? {})) {
        const ae = aboveLaws[lawName]
        if (e.articles?.length && ae && !ae.articles && ae.hash === e.hash) {
          aboveLaws = { ...aboveLaws, [lawName]: { ...ae, articles: e.articles } }
          changed = true
        }
      }
      if (changed) next = next.map((s, i) => i === delIdx - 1 ? { ...s, laws: aboveLaws } : s)
    }
  }
  sessionHistories.value = persistSessions(next)
  if (compareEntryId.value === deleteTarget.value.id) compareEntryId.value = null
  deleteTarget.value = null
}

function saveAllReviews() {
  if (totalReviewCount.value === 0) return
  recordSessionHistory('manual')
  saveSuccess.value = true
  setTimeout(() => { saveSuccess.value = false }, 2500)
}

// ── 검토이력 자동 기록 + 조문 스냅샷 (법령검토 시 전체 법령을 세션 1건으로 저장) ──
const MAX_HISTORY = 20
const contentDiffs = ref({})   // lawName → { isFirst, prevDate, changedCount, changes, deletedNos }

function newEntryId() {
  return `${Date.now()}_${Math.random().toString(36).slice(2, 7)}`
}

function nowStamp() {
  const now = new Date()
  const pad = n => String(n).padStart(2, '0')
  return `${now.getFullYear()}.${pad(now.getMonth()+1)}.${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}`
}

function hashArticles(articles) {
  const str = JSON.stringify(articles)
  let h = 5381
  for (let i = 0; i < str.length; i++) h = ((h << 5) + h + str.charCodeAt(i)) | 0
  return String(h)
}

// 세션 이력 저장 — localStorage 용량 초과 시 오래된 스냅샷부터 제거
function persistSessions(entries) {
  let list = entries
  for (;;) {
    try {
      localStorage.setItem(SESS_KEY, JSON.stringify(list))
      return list
    } catch {
      let stripped = false
      for (let i = list.length - 1; i >= 0 && !stripped; i--) {
        for (const ln of Object.keys(list[i].laws ?? {})) {
          if (list[i].laws[ln].articles) {
            list = list.map((s, si) => si === i
              ? { ...s, laws: { ...s.laws, [ln]: { ...s.laws[ln], articles: null } } }
              : s)
            stripped = true
            break
          }
        }
      }
      if (!stripped) {
        if (list.length > 1) list = list.slice(0, -1)
        else return list
      }
    }
  }
}

// 전체 조회 완료 후 각 법령별로 최신 이력 스냅샷 대비 변경 조문 계산 (기록은 하지 않음)
function computeAllDiffs() {
  for (const law of props.laws) {
    const lawName = law.name
    const arts = tabArticles.value[lawName] ?? []
    if (!arts.length) continue
    const hash = hashArticles(arts)
    const latest = latestLawEntry(lawName)
    const prevSnapshot = resolveLawSnapshot(lawName)
    let diff
    if (!latest) {
      diff = { isFirst: true, changedCount: 0, changes: {}, deletedNos: [] }
    } else if (latest.entry.hash === hash || !prevSnapshot) {
      diff = { isFirst: false, prevDate: latest.session.date, changedCount: 0, changes: {}, deletedNos: [] }
    } else {
      diff = computeContentDiff(arts, prevSnapshot, latest.session.date)
    }
    contentDiffs.value = { ...contentDiffs.value, [lawName]: diff }
  }
}

// "전체 저장" 클릭 시 로딩된 전체 법령을 세션 1건의 이력으로 기록
function recordSessionHistory(kind) {
  const lawsObj = {}
  for (const law of props.laws) {
    const lawName = law.name
    const arts = tabArticles.value[lawName] ?? []
    if (!arts.length) continue
    const hash = hashArticles(arts)
    const latest = latestLawEntry(lawName)

    const reviews = {}
    for (let i = 0; i < arts.length; i++) {
      const t = (allInputs.value[inputKey(lawName, i)] ?? '').trim()
      if (t) reviews[i] = t
    }
    lawsObj[lawName] = {
      hash,
      // 직전 세션과 조문 내용이 같으면 스냅샷 생략 (용량 절약)
      articles: latest?.entry.hash === hash ? null : arts,
      reviews,
    }
  }
  if (!Object.keys(lawsObj).length) return

  // 직전 이력과 완전히 동일(법령 구성·조문 해시·검토의견)하면 중복 기록 생략
  const prev = sessionHistories.value[0]
  if (prev && sameSessionContent(prev.laws ?? {}, lawsObj)) return

  const entry = { id: newEntryId(), kind, date: nowStamp(), laws: lawsObj }
  sessionHistories.value = persistSessions([entry, ...sessionHistories.value].slice(0, MAX_HISTORY))
}

function sameSessionContent(prevLaws, curLaws) {
  const pk = Object.keys(prevLaws), ck = Object.keys(curLaws)
  if (pk.length !== ck.length) return false
  for (const k of ck) {
    const p = prevLaws[k], c = curLaws[k]
    if (!p || p.hash !== c.hash) return false
    if (JSON.stringify(p.reviews ?? {}) !== JSON.stringify(c.reviews ?? {})) return false
  }
  return true
}

// ── 조문 내용 diff (이전 검토 스냅샷 대비 변경·신설·삭제) ──────────
function artKey(art) {
  return art.no ? `A:${art.no}` : `H:${art.title}`
}

function computeContentDiff(current, prevArts, prevDate) {
  const prevMap = new Map()
  for (const a of prevArts) prevMap.set(artKey(a), a)

  const changes = {}   // artIdx → { isNew, titleChanged, clauseChanged: {ci: true} }
  let changedCount = 0
  const curKeys = new Set()

  current.forEach((art, idx) => {
    curKeys.add(artKey(art))
    const prevArt = prevMap.get(artKey(art))
    if (!prevArt) {
      changes[idx] = { isNew: true, titleChanged: true, clauseChanged: {} }
      changedCount++
      return
    }
    const titleChanged = (art.title || '') !== (prevArt.title || '')
    const prevSet = new Set((prevArt.clauses ?? []).map(c => c.trim()))
    const clauseChanged = {}
    let any = titleChanged
    ;(art.clauses ?? []).forEach((c, ci) => {
      if (!prevSet.has(c.trim())) { clauseChanged[ci] = true; any = true }
    })
    if (any) { changes[idx] = { isNew: false, titleChanged, clauseChanged }; changedCount++ }
  })

  const deletedNos = prevArts
    .filter(a => !curKeys.has(artKey(a)))
    .map(a => a.no || a.title)
    .filter(Boolean)

  return { isFirst: false, prevDate, changedCount, changes, deletedNos }
}

const activeDiff = computed(() =>
  activeLawName.value ? (contentDiffs.value[activeLawName.value] ?? null) : null
)
const activeDiffHasChanges = computed(() =>
  !!activeDiff.value && (activeDiff.value.changedCount > 0 || activeDiff.value.deletedNos?.length > 0)
)

function artDiff(artIdx) {
  return activeDiff.value?.changes?.[artIdx] ?? null
}
function isClauseChanged(artIdx, ci) {
  const d = artDiff(artIdx)
  return !!(d && (d.isNew || d.clauseChanged?.[ci]))
}

// ── 비교 diff ────────────────────────────────────────
function diffSegments(oldText, curText) {
  if (!oldText) return []
  if (oldText === curText) return [{ text: oldText, changed: false }]
  const oldLines = oldText.split('\n')
  const curLines = (curText || '').split('\n')
  const segs = []
  const max = Math.max(oldLines.length, curLines.length)
  for (let i = 0; i < max; i++) {
    const o = oldLines[i] ?? ''
    const c = curLines[i] ?? ''
    if (i > 0) segs.push({ text: '\n', changed: false })
    segs.push({ text: o, changed: o !== c })
  }
  return segs
}

// ── 다운로드 관련 ─────────────────────────────────────
const downloadableLawCount = computed(() =>
  props.laws.filter(law => !!latestLawEntry(law.name) ||
    (tabArticles.value[law.name] ?? []).some((_, i) =>
      (allInputs.value[inputKey(law.name, i)] ?? '').trim()
    )
  ).length
)

const THIN_BLACK = { style: 'thin', color: { rgb: '000000' } }
const BORDER     = { top: THIN_BLACK, bottom: THIN_BLACK, left: THIN_BLACK, right: THIN_BLACK }
const NAVY       = '1E3A5F'
const LT_BLUE    = 'EFF6FF'
const META_BG    = 'DBEAFE'
const WHITE      = 'FFFFFF'
const CHAPTER_BG = 'F1F5F9'

function sc(ws, r, c, v, s) {
  const addr = XLSX.utils.encode_cell({ r, c })
  ws[addr] = { t: typeof v === 'number' ? 'n' : 's', v: v ?? '', s: s ?? {} }
}

function buildCoverSheet(savedLaws, todayStr, userName, company, department) {
  const ws = {}
  const COLS = 4
  for (let r = 0; r < 3; r++) for (let c = 0; c < COLS; c++) sc(ws, r, c, '', {})
  sc(ws, 3, 0, '법적 요구사항 준수 검토 보고서', {
    font: { sz: 20, bold: true, color: { rgb: WHITE } },
    fill: { fgColor: { rgb: NAVY } },
    alignment: { horizontal: 'center', vertical: 'center' },
  })
  for (let r = 3; r <= 5; r++) for (let c = 0; c < COLS; c++) {
    if (r === 3 && c === 0) continue
    sc(ws, r, c, '', { fill: { fgColor: { rgb: NAVY } } })
  }
  sc(ws, 6, 0, 'Legal Compliance Review Report', {
    font: { sz: 11, italic: true, color: { rgb: '64748B' } },
    fill: { fgColor: { rgb: 'F8FAFC' } },
    alignment: { horizontal: 'center', vertical: 'center' },
  })
  for (let c = 1; c < COLS; c++) sc(ws, 6, c, '', { fill: { fgColor: { rgb: 'F8FAFC' } } })
  for (let c = 0; c < COLS; c++) sc(ws, 7, c, '', {})
  const hdrStyle = {
    font: { sz: 10, bold: true, color: { rgb: WHITE } },
    fill: { fgColor: { rgb: NAVY } },
    border: BORDER,
    alignment: { horizontal: 'center', vertical: 'center' },
  }
  sc(ws, 8, 0, '항   목', hdrStyle)
  sc(ws, 8, 1, '내   용', hdrStyle)
  for (let c = 2; c < COLS; c++) sc(ws, 8, c, '', { fill: { fgColor: { rgb: NAVY } }, border: BORDER })
  const metaRows = [
    ['작  성  일', todayStr],
    ['작  성  자', userName || ''],
    ['부  서  명', department || ''],
    ['회  사  명', company || ''],
    ['검 토 법 령', `총 ${savedLaws.length}건`],
  ]
  metaRows.forEach(([label, val], i) => {
    const r = 9 + i
    const bg = i % 2 === 0 ? LT_BLUE : WHITE
    sc(ws, r, 0, label, { font: { sz: 10, bold: true, color: { rgb: NAVY } }, fill: { fgColor: { rgb: bg } }, border: BORDER, alignment: { horizontal: 'center', vertical: 'center' } })
    sc(ws, r, 1, val, { font: { sz: 10, color: { rgb: '374151' } }, fill: { fgColor: { rgb: bg } }, border: BORDER, alignment: { horizontal: 'left', vertical: 'center' } })
    for (let c = 2; c < COLS; c++) sc(ws, r, c, '', { fill: { fgColor: { rgb: bg } }, border: BORDER })
  })
  for (let c = 0; c < COLS; c++) sc(ws, 14, c, '', {})
  ws['!ref'] = XLSX.utils.encode_range({ s: { r: 0, c: 0 }, e: { r: 14, c: COLS - 1 } })
  ws['!merges'] = [
    { s: { r: 3, c: 0 }, e: { r: 5, c: COLS - 1 } },
    { s: { r: 6, c: 0 }, e: { r: 6, c: COLS - 1 } },
    { s: { r: 7, c: 0 }, e: { r: 7, c: COLS - 1 } },
    { s: { r: 8, c: 1 }, e: { r: 8, c: COLS - 1 } },
    ...metaRows.map((_, i) => ({ s: { r: 9 + i, c: 1 }, e: { r: 9 + i, c: COLS - 1 } })),
  ]
  ws['!cols'] = [{ wch: 18 }, { wch: 36 }, { wch: 8 }, { wch: 8 }]
  ws['!rows'] = [
    { hpt: 12 }, { hpt: 12 }, { hpt: 16 },
    { hpt: 36 }, { hpt: 36 }, { hpt: 36 },
    { hpt: 24 }, { hpt: 14 }, { hpt: 24 },
    ...metaRows.map(() => ({ hpt: 22 })),
    { hpt: 12 },
  ]
  return ws
}

function buildLawInfoSheet(savedLaws, tabMetaVal) {
  const hdr = ['순번', '법령명', '유형', '소관부처', '공포일자', '시행일자', '제개정구분', '법령 링크']
  const rows = savedLaws.map((law, i) => {
    const m = tabMetaVal[law.name]
    return [i + 1, law.name, m?.lawType||law.type||'-', m?.department||law.ministry||'-', m?.promulgationDate||'-', m?.enforcementDate||'-', m?.amendType||'-', m?.link||law.url||'-']
  })
  const ws = XLSX.utils.aoa_to_sheet([hdr, ...rows])
  const range = XLSX.utils.decode_range(ws['!ref'])
  for (let R = range.s.r; R <= range.e.r; R++) {
    for (let C = range.s.c; C <= range.e.c; C++) {
      const addr = XLSX.utils.encode_cell({ r: R, c: C })
      if (!ws[addr]) ws[addr] = { t: 's', v: '' }
      const isHdr = R === 0
      ws[addr].s = {
        font: { sz: 9, bold: isHdr, color: { rgb: isHdr ? WHITE : '111827' } },
        fill: { fgColor: { rgb: isHdr ? NAVY : (R % 2 === 0 ? WHITE : LT_BLUE) } },
        border: BORDER,
        alignment: { wrapText: false, vertical: 'top', horizontal: C === 0 ? 'center' : 'left' },
      }
    }
  }
  ws['!cols'] = [{ wch: 4 }, { wch: 40 }, { wch: 10 }, { wch: 22 }, { wch: 12 }, { wch: 12 }, { wch: 10 }, { wch: 55 }]
  return ws
}

const RED = 'DC2626'

function buildLawSheet(law, arts, meta, reviewsForLaw, diff) {
  const ws = {}
  let r = 0
  const changes = diff?.changes ?? {}
  const hasDiff = !!diff && !diff.isFirst && (diff.changedCount > 0 || diff.deletedNos?.length > 0)
  const metaLabelStyle = { font: { sz: 9, bold: true, color: { rgb: NAVY } }, fill: { fgColor: { rgb: META_BG } }, border: BORDER, alignment: { horizontal: 'center', vertical: 'center', wrapText: false } }
  const metaValueStyle = { font: { sz: 9, color: { rgb: '374151' } }, fill: { fgColor: { rgb: LT_BLUE } }, border: BORDER, alignment: { horizontal: 'left', vertical: 'center', wrapText: false } }

  sc(ws, r, 0, '법 령 명', metaLabelStyle)
  sc(ws, r, 1, law.name, { ...metaValueStyle, font: { sz: 10, bold: true, color: { rgb: NAVY } } })
  sc(ws, r, 2, '유   형', metaLabelStyle)
  sc(ws, r, 3, meta?.lawType || law.type || '-', metaValueStyle)
  r++
  sc(ws, r, 0, '공포일자', metaLabelStyle)
  sc(ws, r, 1, meta?.promulgationDate || '-', metaValueStyle)
  sc(ws, r, 2, '시행일자', metaLabelStyle)
  sc(ws, r, 3, meta?.enforcementDate || '-', metaValueStyle)
  r++
  sc(ws, r, 0, '소관부처', metaLabelStyle)
  sc(ws, r, 1, meta?.department || law.ministry || '-', metaValueStyle)
  sc(ws, r, 2, '제개정구분', metaLabelStyle)
  sc(ws, r, 3, meta?.amendType || '-', metaValueStyle)
  r++
  sc(ws, r, 0, '법령 링크', metaLabelStyle)
  sc(ws, r, 1, meta?.link || law.url || '-', metaValueStyle)
  sc(ws, r, 2, '', { fill: { fgColor: { rgb: LT_BLUE } }, border: BORDER })
  sc(ws, r, 3, '', { fill: { fgColor: { rgb: LT_BLUE } }, border: BORDER })
  r++
  for (let c = 0; c < 4; c++) sc(ws, r, c, '', {})
  r++

  // 이전 검토 대비 변경 안내 (빨간색 범례)
  let legendRow = -1
  if (hasDiff) {
    legendRow = r
    const delTxt = diff.deletedNos?.length ? ` · 삭제된 조문: ${diff.deletedNos.join(', ')}` : ''
    sc(ws, r, 0, `※ 빨간색: 이전 검토(${diff.prevDate}) 이후 변경·신설된 조문 ${diff.changedCount}건${delTxt}`, {
      font: { sz: 9, bold: true, color: { rgb: RED } },
      alignment: { horizontal: 'left', vertical: 'center', wrapText: false },
    })
    for (let c = 1; c < 4; c++) sc(ws, r, c, '', {})
    r++
  }

  const tHdr = { font: { sz: 9, bold: true, color: { rgb: WHITE } }, fill: { fgColor: { rgb: NAVY } }, border: BORDER, alignment: { horizontal: 'center', vertical: 'center', wrapText: false } }
  ;['조문번호', '조문제목', '조문내용', '검토의견'].forEach((h, c) => sc(ws, r, c, h, tHdr))
  const tblHdrRow = r
  r++

  for (let i = 0; i < arts.length; i++) {
    const art = arts[i]
    const isChapter = !art.clauses?.length
    const review = reviewsForLaw?.[i] ?? ''
    const changed = !!changes[i]   // 이전 검토 대비 변경·신설 → 빨간색
    const rowBg = isChapter ? CHAPTER_BG : (r % 2 === 0 ? WHITE : 'F9FAFB')
    const ds = (bold = false, align = 'left', red = changed) => ({
      font: { sz: 9, bold: bold || red, color: { rgb: red ? RED : '111827' } },
      fill: { fgColor: { rgb: rowBg } },
      border: BORDER,
      alignment: { horizontal: align, vertical: 'top', wrapText: true },
    })
    if (isChapter) {
      sc(ws, r, 0, '', { fill: { fgColor: { rgb: CHAPTER_BG } }, border: BORDER })
      sc(ws, r, 1, '', { fill: { fgColor: { rgb: CHAPTER_BG } }, border: BORDER })
      sc(ws, r, 2, `${art.no} ${art.title}`.trim(), { font: { sz: 9, bold: true, color: { rgb: changed ? RED : NAVY } }, fill: { fgColor: { rgb: CHAPTER_BG } }, border: BORDER, alignment: { horizontal: 'left', vertical: 'center', wrapText: false } })
      sc(ws, r, 3, '', { fill: { fgColor: { rgb: CHAPTER_BG } }, border: BORDER })
    } else {
      const marker = changed ? (changes[i].isNew ? ' [신설]' : ' [변경]') : ''
      sc(ws, r, 0, (art.no || '') + marker, ds(false, 'center'))
      sc(ws, r, 1, art.title || '', ds())
      sc(ws, r, 2, art.clauses?.join('\n') || '', ds())
      sc(ws, r, 3, review, ds(false, 'left', false))
    }
    r++
  }

  ws['!ref'] = XLSX.utils.encode_range({ s: { r: 0, c: 0 }, e: { r: r - 1, c: 3 } })
  const merges = [{ s: { r: 3, c: 1 }, e: { r: 3, c: 3 } }]
  if (legendRow >= 0) merges.push({ s: { r: legendRow, c: 0 }, e: { r: legendRow, c: 3 } })
  ws['!merges'] = merges
  ws['!cols'] = [{ wch: 14 }, { wch: 22 }, { wch: 80 }, { wch: 45 }]
  ws['!rows'] = Array.from({ length: r }, (_, i) => {
    if (i < 4) return { hpt: 18 }
    if (i === 4) return { hpt: 6 }
    if (i === legendRow) return { hpt: 18 }
    if (i === tblHdrRow) return { hpt: 20 }
    return { hpt: 50 }
  })
  return ws
}

function safeSheetName(name, used) {
  const clean = name.replace(/[:/\\?*[\]]/g, ' ').trim()
  let sheet = clean.length > 31 ? clean.slice(0, 29) + '..' : clean
  let n = 2
  while (used.has(sheet)) {
    const base = clean.length > 28 ? clean.slice(0, 28) : clean
    sheet = `${base}(${n++})`
    if (sheet.length > 31) sheet = sheet.slice(0, 31)
  }
  return sheet
}

async function downloadAllReviews() {
  const today = new Date()
  const pad = n => String(n).padStart(2, '0')
  const todayStr = `${today.getFullYear()}.${pad(today.getMonth()+1)}.${pad(today.getDate())}`
  const wb = XLSX.utils.book_new()

  // 검토 내용이 있는 법령만 (이력 또는 현재 드래프트)
  const targetLaws = props.laws.filter(law => {
    const hasHistory = !!latestLawEntry(law.name)
    const arts = tabArticles.value[law.name] ?? []
    const hasDraft = arts.some((_, i) => (allInputs.value[inputKey(law.name, i)] ?? '').trim())
    return hasHistory || hasDraft
  })
  if (!targetLaws.length) return

  XLSX.utils.book_append_sheet(wb,
    buildCoverSheet(targetLaws, todayStr, auth.user?.name, companyName.value, auth.user?.department), '표지')
  XLSX.utils.book_append_sheet(wb, buildLawInfoSheet(targetLaws, tabMeta.value), '법령정보')

  const usedNames = new Set(['표지', '법령정보'])
  for (const law of targetLaws) {
    const arts = tabArticles.value[law.name] ?? []
    // 현재 입력 우선, 없으면 최신 이력
    const reviews = {}
    const latestEntry = latestLawEntry(law.name)?.entry
    for (let i = 0; i < arts.length; i++) {
      const cur = (allInputs.value[inputKey(law.name, i)] ?? '').trim()
      if (cur) reviews[i] = cur
      else if (latestEntry?.reviews?.[i]) reviews[i] = latestEntry.reviews[i]
    }
    // 이전 검토 대비 변경 조문은 빨간색으로 표시하여 다운로드
    const ws = buildLawSheet(law, arts, tabMeta.value[law.name], reviews, contentDiffs.value[law.name])
    const sn = safeSheetName(law.name, usedNames)
    usedNames.add(sn)
    XLSX.utils.book_append_sheet(wb, ws, sn)
  }

  const now = new Date().toISOString().slice(0, 10).replace(/-/g, '')
  XLSX.writeFile(wb, `법령준수검토_${now}.xlsx`)
}
</script>
