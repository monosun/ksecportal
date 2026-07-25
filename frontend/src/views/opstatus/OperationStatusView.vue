<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">운영현황관리</h1>
        <p class="text-sm text-gray-400 mt-0.5">
          정보보호·개인정보보호 관리체계의 연간 운영 점검 항목을 연도별로 구성하고 월별 이행 현황을 관리합니다
        </p>
      </div>
      <div class="flex items-center gap-2">
        <!-- 연도 이동 -->
        <div class="flex items-center gap-1">
          <button @click="changeYear(-1)" class="px-2 py-1.5 rounded border border-gray-200 text-gray-500 hover:bg-gray-50">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
            </svg>
          </button>
          <span class="text-base font-bold text-gray-900 tabular-nums w-16 text-center">{{ year }}년</span>
          <button @click="changeYear(1)" class="px-2 py-1.5 rounded border border-gray-200 text-gray-500 hover:bg-gray-50">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
            </svg>
          </button>
        </div>
        <button v-if="canWrite" @click="openForm(null)" class="btn-primary flex items-center gap-2 text-sm">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          항목 추가
        </button>
      </div>
    </div>

    <div class="page-body">
      <!-- 구분 탭 -->
      <div class="flex items-center justify-between gap-3 flex-wrap border-b border-gray-100">
        <div class="flex">
          <button v-for="t in TYPES" :key="t.key"
            class="px-4 py-2.5 text-sm font-medium transition-colors"
            :class="type === t.key
              ? 'border-b-2 border-primary-600 text-primary-700'
              : 'text-gray-500 hover:text-gray-700'"
            @click="selectType(t.key)">
            {{ t.label }}
            <span class="ml-1 text-xs text-gray-400 tabular-nums">{{ countOf(t.key) }}</span>
          </button>
        </div>
        <div v-if="canWrite" class="flex gap-2 pb-1.5">
          <button @click="loadDefaults" :disabled="busy"
            class="text-xs px-2.5 py-1.5 rounded border border-gray-200 text-gray-600 hover:bg-gray-50 disabled:opacity-50">
            기본 항목 불러오기
          </button>
          <button @click="copyPrevious" :disabled="busy"
            class="text-xs px-2.5 py-1.5 rounded border border-gray-200 text-gray-600 hover:bg-gray-50 disabled:opacity-50">
            전년도 구성 복사
          </button>
          <button v-if="items.length" @click="clearYear" :disabled="busy"
            class="text-xs px-2.5 py-1.5 rounded border border-red-100 text-red-500 hover:bg-red-50 disabled:opacity-50">
            이 구분 전체 삭제
          </button>
        </div>
      </div>

      <!-- 요약 -->
      <section v-if="typeSummary" class="card !p-4">
        <div class="flex items-center gap-6 flex-wrap">
          <div>
            <p class="text-[11px] text-gray-500">이행률</p>
            <p class="text-2xl font-bold tabular-nums leading-tight"
              :style="{ color: rateColor(typeSummary.rate, typeSummary.planned) }">
              {{ typeSummary.planned > 0 ? `${typeSummary.rate}%` : '—' }}
            </p>
            <p class="text-[11px] text-gray-400 tabular-nums">
              이행 {{ typeSummary.done }} / 계획 {{ typeSummary.planned }}칸
            </p>
          </div>
          <div class="h-10 w-px bg-gray-100"></div>
          <div class="flex gap-5">
            <div>
              <p class="text-[11px] text-gray-500">점검 항목</p>
              <p class="text-lg font-bold text-gray-900 tabular-nums">{{ typeSummary.items }}</p>
            </div>
            <div>
              <p class="text-[11px] text-gray-500">미이행</p>
              <p class="text-lg font-bold tabular-nums"
                :class="typeSummary.planned - typeSummary.done > 0 ? 'text-red-600' : 'text-gray-400'">
                {{ typeSummary.planned - typeSummary.done }}
              </p>
            </div>
            <div>
              <p class="text-[11px] text-gray-500">계획 외 수행</p>
              <p class="text-lg font-bold text-gray-500 tabular-nums">{{ typeSummary.unplannedDone }}</p>
            </div>
          </div>

          <!-- 월별 이행/계획 미니 막대 -->
          <div class="flex-1 min-w-[280px]">
            <p class="text-[11px] text-gray-500 mb-1">월별 계획 대비 이행</p>
            <div class="flex items-end gap-1 h-10">
              <div v-for="(p, i) in typeSummary.plannedByMonth" :key="i" class="flex-1 flex flex-col items-center gap-0.5"
                :title="`${i + 1}월 — 계획 ${p}건 / 이행 ${typeSummary.doneByMonth[i]}건`">
                <div class="w-full bg-gray-100 rounded-sm relative" :style="{ height: `${barHeight(p)}px` }">
                  <div class="absolute bottom-0 left-0 right-0 bg-primary-500 rounded-sm"
                    :style="{ height: `${p > 0 ? (typeSummary.doneByMonth[i] / p) * 100 : 0}%` }"></div>
                </div>
                <span class="text-[9px] text-gray-400 tabular-nums">{{ i + 1 }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 항목 표 -->
      <div v-if="loading" class="card text-center py-12 text-sm text-gray-400">불러오는 중...</div>
      <div v-else-if="items.length === 0" class="card text-center py-12">
        <p class="text-sm text-gray-500">{{ year }}년 {{ typeLabel }} 항목이 없습니다.</p>
        <p v-if="canWrite" class="text-xs text-gray-400 mt-1">
          상단의 <strong>기본 항목 불러오기</strong>로 표준 항목을 구성하거나, <strong>항목 추가</strong>로 직접 등록하세요.
        </p>
      </div>

      <div v-else class="card !p-0 overflow-x-auto">
        <table class="w-full text-sm min-w-[1100px]">
          <thead class="bg-gray-50 text-xs text-gray-500 sticky top-0">
            <tr>
              <th v-if="type === 'ISMS'" class="text-left px-3 py-2.5 font-medium w-32">구분</th>
              <th class="text-left px-3 py-2.5 font-medium min-w-[180px]">{{ type === 'ISMS' ? '점검 기준' : '점검항목' }}</th>
              <th class="text-left px-3 py-2.5 font-medium w-24">{{ type === 'ISMS' ? '주기·시점' : '점검주기' }}</th>
              <th class="text-left px-3 py-2.5 font-medium min-w-[200px]">{{ type === 'ISMS' ? '보안적용 실적' : '상세 내용' }}</th>
              <th v-if="type === 'ISMS'" class="text-left px-3 py-2.5 font-medium w-20">책임자</th>
              <th v-if="type === 'ISMS'" class="text-left px-3 py-2.5 font-medium w-24">실무자</th>
              <th v-for="m in 12" :key="m" class="px-1 py-2.5 font-medium text-center w-8">{{ m }}</th>
              <th class="px-3 py-2.5 font-medium text-center w-16">이행</th>
              <th v-if="canWrite" class="px-3 py-2.5 font-medium text-center w-20"></th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            <tr v-for="(item, idx) in items" :key="item.id" class="hover:bg-gray-50/60 align-top">
              <td v-if="type === 'ISMS'" class="px-3 py-2 text-xs text-gray-500">
                <span v-if="isCategoryStart(idx)" class="font-semibold text-gray-700">{{ item.category }}</span>
              </td>
              <td class="px-3 py-2 text-gray-800">{{ item.name }}</td>
              <td class="px-3 py-2 text-xs text-gray-500">{{ item.cycle }}</td>
              <td class="px-3 py-2 text-xs text-gray-500 whitespace-pre-line">{{ formatDeliverable(item.deliverable) }}</td>
              <td v-if="type === 'ISMS'" class="px-3 py-2 text-xs text-gray-500">{{ item.owner }}</td>
              <td v-if="type === 'ISMS'" class="px-3 py-2 text-xs text-gray-500">{{ item.manager }}</td>

              <!-- 월별 계획/이행 셀 — 클릭하면 이행 토글, 계획은 수정 팝업에서 지정 -->
              <td v-for="m in 12" :key="m" class="px-0.5 py-2 text-center">
                <button type="button"
                  class="w-6 h-6 rounded flex items-center justify-center mx-auto transition-colors"
                  :class="cellClass(item, m)"
                  :disabled="!canWrite || busy"
                  :title="cellTitle(item, m)"
                  @click="toggleDone(item, m)">
                  <span class="text-[13px] leading-none">{{ cellMark(item, m) }}</span>
                </button>
              </td>

              <td class="px-3 py-2 text-center text-xs tabular-nums"
                :class="item.planCount && item.doneCount >= item.planCount ? 'text-green-600 font-semibold' : 'text-gray-500'">
                {{ item.doneCount }}/{{ item.planCount }}
              </td>

              <td v-if="canWrite" class="px-3 py-2 text-center whitespace-nowrap">
                <button @click="openForm(item)" class="text-xs text-blue-600 hover:text-blue-800 px-1.5">수정</button>
                <button @click="removeItem(item)" class="text-xs text-red-500 hover:text-red-700 px-1.5">삭제</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <p class="text-[11px] text-gray-400">
        월 칸: <span class="text-gray-500">○ 계획</span> ·
        <span class="text-green-600 font-semibold">● 이행 완료</span> ·
        <span class="text-amber-600 font-semibold">▲ 계획 외 수행</span> — 칸을 클릭하면 이행 여부가 바뀝니다.
        계획(○)은 항목 수정에서 지정합니다.
      </p>
    </div>

    <!-- 항목 등록/수정 모달 -->
    <div v-if="showForm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-3xl max-h-[92vh] flex flex-col">
        <div class="px-6 py-4 border-b border-gray-100 flex items-center justify-between">
          <h2 class="text-lg font-bold">{{ editing ? '항목 수정' : '항목 추가' }} — {{ year }}년 {{ typeLabel }}</h2>
          <button @click="showForm = false" class="text-gray-400 hover:text-gray-600">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>

        <div class="p-6 space-y-4 overflow-y-auto">
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div v-if="type === 'ISMS'">
              <label class="text-sm font-medium text-gray-700">구분</label>
              <input v-model="form.category" list="ops-categories" class="input w-full mt-1" placeholder="예: 접근통제" />
              <datalist id="ops-categories">
                <option v-for="c in categories" :key="c" :value="c" />
              </datalist>
            </div>
            <div>
              <label class="text-sm font-medium text-gray-700">{{ type === 'ISMS' ? '주기·시점' : '점검주기' }}</label>
              <input v-model="form.cycle" list="ops-cycles" class="input w-full mt-1" placeholder="예: 연1회, 반기1회, 매월, 수시, 상시" />
              <datalist id="ops-cycles">
                <option v-for="c in CYCLE_SUGGESTIONS" :key="c" :value="c" />
              </datalist>
            </div>
          </div>

          <!-- 점검 기준 — 코드 관리의 기본항목에서 선택하거나 직접 입력 -->
          <div>
            <div class="flex items-center justify-between mb-1">
              <label class="text-sm font-medium text-gray-700">{{ type === 'ISMS' ? '점검 기준' : '점검항목' }} *</label>
              <span class="text-[11px] text-gray-400">
                기본항목 {{ defaultOptions.length }}건 · 관리 &gt; 코드 관리 &gt; 운영현황 기본항목
              </span>
            </div>

            <select v-if="!manualName" v-model="selectedDefaultId" class="input w-full" @change="applyDefault">
              <option :value="''" disabled>기본항목에서 선택하세요</option>
              <optgroup v-for="g in groupedDefaults" :key="g.label || '기타'" :label="g.label || '구분 없음'">
                <option v-for="d in g.items" :key="d.id" :value="d.id">{{ d.name }}</option>
              </optgroup>
              <option value="__manual__">＋ 목록에 없음 — 직접 입력</option>
            </select>

            <div v-else class="flex gap-2">
              <input v-model="form.name" class="input flex-1" placeholder="점검 항목명 직접 입력" />
              <button v-if="defaultOptions.length" type="button" @click="backToSelect"
                class="text-xs px-2.5 rounded border border-gray-200 text-gray-500 hover:bg-gray-50 whitespace-nowrap">
                목록에서 선택
              </button>
            </div>

            <p v-if="!manualName && defaultOptions.length === 0" class="text-xs text-amber-600 mt-1">
              등록된 기본항목이 없습니다. 직접 입력으로 전환해 등록하세요.
            </p>
            <p v-else-if="!manualName" class="text-xs text-gray-400 mt-1">
              선택하면 주기·산출물·담당자·월별 계획이 함께 채워집니다. 이후 자유롭게 수정할 수 있습니다.
            </p>
          </div>

          <div>
            <label class="text-sm font-medium text-gray-700">{{ type === 'ISMS' ? '보안적용 실적 (산출물)' : '상세 내용' }}</label>
            <textarea v-model="form.deliverable" rows="4" class="input w-full mt-1"
              placeholder="산출물·근거를 한 줄에 하나씩 적거나 ' / ' 로 구분해 입력합니다"></textarea>
          </div>

          <div v-if="type === 'ISMS'" class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label class="text-sm font-medium text-gray-700">책임자</label>
              <input v-model="form.owner" class="input w-full mt-1" placeholder="예: CISO" />
            </div>
            <div>
              <label class="text-sm font-medium text-gray-700">실무자</label>
              <input v-model="form.manager" class="input w-full mt-1" placeholder="예: 정보보호매니저" />
            </div>
          </div>

          <div>
            <div class="flex items-center justify-between mb-2">
              <label class="text-sm font-medium text-gray-700">월별 계획</label>
              <div class="flex gap-1.5">
                <button type="button" @click="setPlanAll(true)"
                  class="text-xs px-2 py-1 rounded border border-gray-200 text-gray-600 hover:bg-gray-50">매월</button>
                <button type="button" @click="setPlanAll(false)"
                  class="text-xs px-2 py-1 rounded border border-gray-200 text-gray-600 hover:bg-gray-50">전체 해제</button>
              </div>
            </div>
            <div class="grid grid-cols-6 sm:grid-cols-12 gap-1">
              <button v-for="m in 12" :key="m" type="button"
                class="py-2 rounded text-xs font-medium border transition-colors"
                :class="form.plan[m - 1]
                  ? 'bg-primary-50 border-primary-300 text-primary-700'
                  : 'bg-white border-gray-200 text-gray-400 hover:bg-gray-50'"
                @click="form.plan[m - 1] = !form.plan[m - 1]">
                {{ m }}월
              </button>
            </div>
            <p class="text-xs text-gray-400 mt-1.5">
              상시·수시 항목처럼 특정 월 계획이 없으면 모두 해제해 두고, 수행한 달에만 표에서 이행 표시를 하면 됩니다.
            </p>
          </div>

          <div>
            <label class="text-sm font-medium text-gray-700">비고</label>
            <input v-model="form.note" class="input w-full mt-1" placeholder="비고 (선택)" />
          </div>
        </div>

        <div class="px-6 py-4 border-t border-gray-100 flex justify-end gap-2">
          <button @click="showForm = false" class="btn-secondary">취소</button>
          <button @click="submitForm" :disabled="saving" class="btn-primary">
            {{ saving ? '저장 중...' : (editing ? '수정' : '추가') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { operationStatusApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const canWrite = computed(() => auth.canWrite('operation_status'))

const TYPES = [
  { key: 'ISMS', label: '정보보호 관리체계' },
  { key: 'PRIVACY', label: '개인정보보호 관리체계' },
]
const CYCLE_SUGGESTIONS = ['연1회', '반기1회', '분기1회', '매월', '수시', '상시', '2년 1회 이상 (의무)']

const year = ref(new Date().getFullYear())
const type = ref('ISMS')
const items = ref([])
const summary = ref(null)
const loading = ref(true)
const busy = ref(false)
const saving = ref(false)

const showForm = ref(false)
const editing = ref(null)
const form = ref(emptyForm())

// 기본항목(코드 관리) — 항목 추가 시 선택 목록으로 쓴다
const defaults = ref([])
const selectedDefaultId = ref('')
const manualName = ref(false)

/** 현재 구분의 사용 중인 기본항목 */
const defaultOptions = computed(() =>
  defaults.value.filter(d => d.type === type.value && d.active))

/** 구분(category)별로 묶어 optgroup 으로 보여준다 */
const groupedDefaults = computed(() => {
  const map = new Map()
  for (const d of defaultOptions.value) {
    const key = d.category || ''
    if (!map.has(key)) map.set(key, [])
    map.get(key).push(d)
  }
  return [...map.entries()].map(([label, items]) => ({ label, items }))
})

const typeLabel = computed(() => TYPES.find(t => t.key === type.value)?.label || type.value)
const typeSummary = computed(() => summary.value?.byType?.find(t => t.type === type.value) || null)

const categories = computed(() => [...new Set(items.value.map(i => i.category).filter(Boolean))])

function emptyForm() {
  return {
    category: '', name: '', cycle: '', deliverable: '',
    owner: '', manager: '', note: '',
    plan: Array(12).fill(false),
  }
}

function countOf(key) {
  return summary.value?.byType?.find(t => t.type === key)?.items ?? 0
}

function rateColor(rate, planned) {
  if (!planned) return '#9ca3af'
  return rate >= 90 ? '#0ca30c' : rate >= 70 ? '#fab219' : '#d03b3b'
}

/** 월별 막대 높이 — 계획 건수에 비례하되 최소 높이를 둬서 0건도 자리를 차지하게 한다 */
function barHeight(planned) {
  const max = Math.max(1, ...(typeSummary.value?.plannedByMonth || [1]))
  return 6 + Math.round((planned / max) * 30)
}

/** 같은 구분이 연속될 때 첫 행에만 구분명을 표시한다(엑셀 병합셀과 같은 모양) */
function isCategoryStart(idx) {
  if (idx === 0) return true
  return items.value[idx].category !== items.value[idx - 1].category
}

function formatDeliverable(text) {
  return (text || '').split(' / ').join('\n')
}

function cellMark(item, m) {
  const plan = item.plan[m - 1]
  const done = item.done[m - 1]
  if (done) return plan ? '●' : '▲'
  return plan ? '○' : ''
}

function cellClass(item, m) {
  const plan = item.plan[m - 1]
  const done = item.done[m - 1]
  if (done && plan) return 'bg-green-50 text-green-600 hover:bg-green-100'
  if (done) return 'bg-amber-50 text-amber-600 hover:bg-amber-100'
  if (plan) return 'bg-gray-50 text-gray-400 hover:bg-gray-100'
  return 'text-gray-200 hover:bg-gray-50'
}

function cellTitle(item, m) {
  const plan = item.plan[m - 1]
  const done = item.done[m - 1]
  const state = done ? (plan ? '이행 완료' : '계획 외 수행') : (plan ? '계획 (미이행)' : '계획 없음')
  return `${item.name} — ${m}월: ${state}`
}

// ── 데이터 로드 ─────────────────────────────────────────────────────────────

async function load() {
  loading.value = true
  try {
    const [listRes, sumRes] = await Promise.all([
      operationStatusApi.list({ year: year.value, type: type.value }),
      operationStatusApi.summary(year.value),
    ])
    items.value = listRes.data || listRes || []
    summary.value = sumRes.data || sumRes || null
  } catch (e) {
    items.value = []
    summary.value = null
  } finally {
    loading.value = false
  }
}

function changeYear(delta) {
  year.value += delta
  load()
}

function selectType(key) {
  if (type.value === key) return
  type.value = key
  load()
}

// ── 월 칸 토글 ──────────────────────────────────────────────────────────────

async function toggleDone(item, m) {
  if (!canWrite.value) return
  const next = !item.done[m - 1]
  busy.value = true
  try {
    const res = await operationStatusApi.toggleMonth(item.id, { field: 'DONE', month: m, value: next })
    const updated = res.data || res
    const idx = items.value.findIndex(i => i.id === item.id)
    if (idx >= 0) items.value[idx] = updated
    // 요약(이행률·월별 막대)도 함께 갱신한다
    const sumRes = await operationStatusApi.summary(year.value)
    summary.value = sumRes.data || sumRes
  } catch (e) {
    alert(e || '변경에 실패했습니다.')
  } finally {
    busy.value = false
  }
}

// ── 연도 구성 ───────────────────────────────────────────────────────────────

async function loadDefaults() {
  if (!confirm(`${year.value}년 ${typeLabel.value} 기본 항목을 불러옵니다.\n계속할까요?`)) return
  busy.value = true
  try {
    const res = await operationStatusApi.loadDefaults(year.value, type.value)
    const d = res.data || res
    alert(`기본 항목 ${d.created}건을 등록했습니다.`)
    await load()
  } catch (e) {
    alert(e || '불러오기에 실패했습니다.')
  } finally {
    busy.value = false
  }
}

async function copyPrevious() {
  const from = year.value - 1
  if (!confirm(`${from}년 ${typeLabel.value} 구성을 ${year.value}년으로 복사합니다.\n항목과 월별 계획은 그대로 가져오고 이행 실적은 초기화됩니다.`)) return
  busy.value = true
  try {
    const res = await operationStatusApi.copy(from, year.value, type.value)
    const d = res.data || res
    alert(`${d.copied}건을 복사했습니다.`)
    await load()
  } catch (e) {
    alert(e || '복사에 실패했습니다.')
  } finally {
    busy.value = false
  }
}

async function clearYear() {
  if (!confirm(`${year.value}년 ${typeLabel.value} 항목 ${items.value.length}건을 모두 삭제합니다.\n이행 실적도 함께 사라집니다. 계속할까요?`)) return
  busy.value = true
  try {
    await operationStatusApi.clear(year.value, type.value)
    await load()
  } catch (e) {
    alert(e || '삭제에 실패했습니다.')
  } finally {
    busy.value = false
  }
}

// ── 항목 등록·수정 ──────────────────────────────────────────────────────────

function openForm(item) {
  editing.value = item
  selectedDefaultId.value = ''
  // 수정은 이미 이름이 정해져 있으므로 항상 직접 입력, 신규는 기본항목이 있으면 선택부터
  manualName.value = !!item || defaultOptions.value.length === 0
  form.value = item
    ? {
        category: item.category || '', name: item.name || '', cycle: item.cycle || '',
        deliverable: (item.deliverable || '').split(' / ').join('\n'),
        owner: item.owner || '', manager: item.manager || '', note: item.note || '',
        plan: [...item.plan],
      }
    : emptyForm()
  showForm.value = true
}

/** 기본항목을 고르면 나머지 칸도 함께 채운다(이후 자유롭게 수정 가능) */
function applyDefault() {
  if (selectedDefaultId.value === '__manual__') {
    manualName.value = true
    selectedDefaultId.value = ''
    form.value.name = ''
    return
  }
  const d = defaultOptions.value.find(x => x.id === selectedDefaultId.value)
  if (!d) return
  form.value = {
    category: d.category || '',
    name: d.name || '',
    cycle: d.cycle || '',
    deliverable: (d.deliverable || '').split(' / ').join('\n'),
    owner: d.owner || '',
    manager: d.manager || '',
    note: d.note || '',
    plan: [...(d.plan || Array(12).fill(false))],
  }
}

function backToSelect() {
  manualName.value = false
  selectedDefaultId.value = ''
  form.value.name = ''
}

async function loadDefaultOptions() {
  try {
    const res = await operationStatusApi.listDefaultItems()
    defaults.value = res.data || res || []
  } catch (e) {
    defaults.value = []   // 조회 실패 시에는 직접 입력으로만 등록한다
  }
}

function setPlanAll(on) {
  form.value.plan = Array(12).fill(on)
}

async function submitForm() {
  if (!form.value.name.trim()) {
    alert('점검 항목명을 입력해주세요.')
    return
  }
  saving.value = true
  try {
    // 줄바꿈으로 입력한 산출물은 저장 시 ' / ' 구분으로 통일한다(엑셀 서식과 동일한 표기)
    const payload = {
      year: year.value,
      type: type.value,
      category: form.value.category,
      name: form.value.name.trim(),
      cycle: form.value.cycle,
      deliverable: form.value.deliverable.split('\n').map(s => s.trim()).filter(Boolean).join(' / '),
      owner: form.value.owner,
      manager: form.value.manager,
      note: form.value.note,
      plan: form.value.plan,
    }
    if (editing.value) {
      await operationStatusApi.update(editing.value.id, payload)
    } else {
      await operationStatusApi.create(payload)
    }
    showForm.value = false
    await load()
  } catch (e) {
    alert(e || '저장에 실패했습니다.')
  } finally {
    saving.value = false
  }
}

async function removeItem(item) {
  if (!confirm(`"${item.name}" 항목을 삭제하시겠습니까?`)) return
  try {
    await operationStatusApi.remove(item.id)
    await load()
  } catch (e) {
    alert(e || '삭제에 실패했습니다.')
  }
}

onMounted(() => { load(); if (canWrite.value) loadDefaultOptions() })
</script>
