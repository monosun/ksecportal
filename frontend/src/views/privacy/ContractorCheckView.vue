<template>
  <div class="p-6">
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">수탁사 점검</h1>
        <p class="text-sm text-gray-500 mt-1">연도별, 수탁사별 점검항목 이행 결과를 관리합니다</p>
      </div>
      <div class="flex items-center gap-2">
        <select v-model="selectedYear" @change="loadChecks" class="input text-sm">
          <option v-for="y in years" :key="y" :value="y">{{ y }}년</option>
        </select>
        <button @click="openCreateCheck" class="btn-primary flex items-center gap-2">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          수탁사 점검 추가
        </button>
      </div>
    </div>

    <div class="flex gap-5">
      <!-- Left: 수탁사 목록 -->
      <div class="w-72 flex-shrink-0">
        <div class="card p-0 overflow-hidden">
          <div class="px-4 py-3 border-b border-gray-100 bg-gray-50/70">
            <p class="text-xs font-semibold text-gray-500 uppercase tracking-wider">수탁사 목록 ({{ selectedYear }}년)</p>
          </div>

          <div v-if="loadingChecks" class="flex justify-center py-10">
            <div class="w-6 h-6 border-4 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
          </div>

          <div v-else-if="checks.length === 0" class="px-4 py-8 text-center">
            <p class="text-sm text-gray-400">{{ selectedYear }}년 점검 내역이 없습니다</p>
            <button @click="openCreateCheck" class="mt-2 text-xs text-blue-500 hover:underline">점검 추가</button>
          </div>

          <div v-else class="divide-y divide-gray-50">
            <button
              v-for="check in checks"
              :key="check.id"
              @click="selectCheck(check)"
              class="w-full text-left px-4 py-3 hover:bg-gray-50 transition-colors"
              :class="selectedCheck?.id === check.id ? 'bg-blue-50 border-l-2 border-blue-500' : ''">
              <div class="flex items-center justify-between">
                <p class="text-sm font-medium text-gray-900 truncate">{{ check.contractorName }}</p>
                <span class="ml-2 flex-shrink-0 text-xs px-1.5 py-0.5 rounded-full font-semibold"
                  :class="statusClass(check.status)">
                  {{ statusLabel(check.status) }}
                </span>
              </div>
              <p class="mt-0.5 text-xs text-gray-400">점검일 {{ check.checkDate || '미지정' }}</p>
              <div class="mt-1 flex items-center gap-2">
                <div v-if="check.totalItems > 0" class="flex items-center gap-1 text-xs text-gray-500">
                  <span class="text-green-600 font-semibold">{{ check.passCount }}통과</span>
                  <span>/</span>
                  <span class="text-red-500 font-semibold">{{ check.failCount }}미흡</span>
                  <span>/</span>
                  <span class="text-gray-400">{{ check.notCheckedCount }}미점검</span>
                </div>
                <span v-else class="text-xs text-gray-400">항목 없음</span>
              </div>
            </button>
          </div>
        </div>
      </div>

      <!-- Right: 점검 상세 -->
      <div class="flex-1 min-w-0">
        <!-- 미선택 -->
        <div v-if="!selectedCheck" class="card flex flex-col items-center justify-center py-20 text-center">
          <svg class="w-12 h-12 text-gray-300 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"/>
          </svg>
          <p class="text-gray-500 font-medium">좌측에서 수탁사를 선택하세요</p>
          <p class="text-gray-400 text-sm mt-1">점검 결과를 입력하고 관리할 수 있습니다</p>
        </div>

        <!-- 선택된 점검 -->
        <div v-else>
          <!-- 점검 헤더 -->
          <div class="card mb-4">
            <div class="flex items-start justify-between">
              <div>
                <h2 class="text-lg font-bold text-gray-900">{{ selectedCheck.contractorName }}</h2>
                <p class="text-sm text-gray-500 mt-0.5">{{ selectedCheck.checkYear }}년 수탁사 점검</p>
              </div>
              <div class="flex items-center gap-2">
                <button @click="openEditCheck" class="text-sm text-blue-600 hover:text-blue-800 font-medium">점검정보 수정</button>
                <button @click="confirmDeleteCheck" class="text-sm text-red-500 hover:text-red-700 font-medium">삭제</button>
              </div>
            </div>

            <div class="mt-4 grid grid-cols-3 gap-4">
              <div>
                <p class="text-xs text-gray-400">점검일</p>
                <p class="text-sm font-medium text-gray-800">{{ selectedCheck.checkDate || '-' }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-400">점검자</p>
                <p class="text-sm font-medium text-gray-800">{{ selectedCheck.inspector || '-' }}</p>
              </div>
              <div>
                <p class="text-xs text-gray-400">점검 상태</p>
                <span class="text-sm font-semibold px-2 py-0.5 rounded-full" :class="statusClass(selectedCheck.status)">
                  {{ statusLabel(selectedCheck.status) }}
                </span>
              </div>
            </div>

            <!-- 진행률 -->
            <div v-if="detail && detail.results.length > 0" class="mt-4">
              <div class="flex items-center justify-between text-xs text-gray-500 mb-1">
                <span>점검 진행률</span>
                <span>{{ progressPct }}%</span>
              </div>
              <div class="h-2 bg-gray-100 rounded-full overflow-hidden">
                <div class="h-full bg-blue-500 rounded-full transition-all" :style="{ width: progressPct + '%' }"></div>
              </div>
              <div class="flex gap-4 mt-2 text-xs">
                <span class="text-green-600 font-semibold">통과 {{ passCount }}</span>
                <span class="text-red-500 font-semibold">미흡 {{ failCount }}</span>
                <span class="text-gray-400 font-semibold">해당없음 {{ naCount }}</span>
                <span class="text-gray-400">미점검 {{ notCheckedCount }}</span>
              </div>
            </div>
          </div>

          <!-- 점검 결과 -->
          <div class="card p-0 overflow-hidden">
            <div class="px-4 py-3 border-b border-gray-100 bg-gray-50/70">
              <div class="flex items-center justify-between mb-2">
                <p class="text-sm font-semibold text-gray-700">
                  점검 항목별 결과
                  <span v-if="searchKeyword && filteredTotal < totalItems" class="ml-1.5 text-xs font-normal text-gray-400">
                    ({{ filteredTotal }}/{{ totalItems }}건)
                  </span>
                </p>
                <button v-if="detail" @click="syncItems" class="text-xs text-gray-500 hover:text-blue-600 flex items-center gap-1">
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
                  </svg>
                  항목 동기화
                </button>
              </div>
              <div v-if="detail" class="flex items-center gap-2">
                <div class="relative flex-1 max-w-xs">
                  <svg class="absolute left-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0"/>
                  </svg>
                  <input v-model="searchKeyword" type="text" placeholder="항목명·카테고리 검색..."
                    class="w-full pl-8 pr-3 py-1.5 text-xs border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400 bg-white"/>
                </div>
                <button @click="setBulkResult('PASS')" :disabled="settingBulk"
                  class="flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold bg-green-50 text-green-700 border border-green-200 rounded-lg hover:bg-green-100 disabled:opacity-50 transition-colors whitespace-nowrap">
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                  </svg>
                  모두 통과
                </button>
                <button @click="setBulkResult('NOT_CHECKED')" :disabled="settingBulk"
                  class="flex items-center gap-1.5 px-3 py-1.5 text-xs font-semibold bg-gray-50 text-gray-600 border border-gray-200 rounded-lg hover:bg-gray-100 disabled:opacity-50 transition-colors whitespace-nowrap">
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
                  </svg>
                  점검 초기화
                </button>
              </div>
            </div>

            <div v-if="loadingDetail" class="flex justify-center py-10">
              <div class="w-6 h-6 border-4 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
            </div>

            <div v-else-if="!detail || detail.results.length === 0" class="px-4 py-8 text-center">
              <p class="text-sm text-gray-400">점검항목이 없습니다. 관리 &gt; 코드관리 &gt; 수탁사 기본점검항목에서 항목을 먼저 추가하세요.</p>
            </div>

            <div v-else-if="searchKeyword && filteredTotal === 0" class="px-4 py-8 text-center">
              <p class="text-sm text-gray-400">"{{ searchKeyword }}"에 해당하는 항목이 없습니다</p>
            </div>

            <div v-else>
              <div v-for="(group, cat) in groupedResults" :key="cat">
                <div class="px-4 py-2 bg-gray-50 border-y border-gray-100">
                  <p class="text-xs font-bold text-gray-600 uppercase tracking-wider">{{ cat }}</p>
                </div>
                <div v-for="row in group" :key="row.checkItemId"
                  class="px-4 py-3 border-b border-gray-50 hover:bg-gray-50/50 transition-colors">
                  <div class="flex items-start gap-3">
                    <div class="flex-1 min-w-0">
                      <div class="flex items-center gap-2 mb-0.5">
                        <span v-if="row.required" class="text-[10px] px-1.5 py-0.5 rounded bg-red-50 text-red-600 font-semibold flex-shrink-0">필수</span>
                        <p class="text-sm font-medium text-gray-900">{{ row.checkItemName }}</p>
                      </div>
                      <p v-if="row.checkMethod" class="text-xs text-gray-400 mt-0.5">{{ row.checkMethod }}</p>
                      <p v-if="row.checkStandard" class="text-xs text-blue-400 mt-0.5">📋 {{ row.checkStandard }}</p>
                    </div>
                    <div class="flex-shrink-0 flex items-center gap-2">
                      <div class="flex gap-1">
                        <button
                          v-for="opt in resultOptions"
                          :key="opt.value"
                          @click="setResult(row, opt.value)"
                          class="px-2.5 py-1 rounded text-xs font-semibold transition-all border"
                          :class="row.result === opt.value
                            ? opt.activeClass
                            : 'bg-white border-gray-200 text-gray-400 hover:border-gray-300'">
                          {{ opt.label }}
                        </button>
                      </div>
                    </div>
                  </div>
                  <div v-if="row.result === 'FAIL' || editingNoteId === row.checkItemId" class="mt-2">
                    <input
                      v-model="row.notes"
                      @blur="saveNotes(row)"
                      type="text"
                      class="w-full text-xs border border-gray-200 rounded px-2.5 py-1.5 focus:outline-none focus:border-blue-400"
                      placeholder="메모 입력 (미흡 사유, 개선 계획 등)"/>
                  </div>
                  <button v-else-if="row.result !== 'FAIL'" @click="editingNoteId = row.checkItemId"
                    class="mt-1 text-xs text-gray-400 hover:text-gray-600 flex items-center gap-0.5">
                    <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                    </svg>
                    {{ row.notes ? row.notes : '메모 추가' }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Create/Edit Check Modal -->
    <div v-if="showCheckModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
        <div class="flex items-center justify-between mb-5">
          <h2 class="text-lg font-semibold text-gray-900">{{ editingCheck ? '점검정보 수정' : '수탁사 점검 추가' }}</h2>
          <button @click="showCheckModal = false" class="text-gray-400 hover:text-gray-600">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>

        <div class="space-y-4">
          <div v-if="!editingCheck">
            <label class="block text-sm font-medium text-gray-700 mb-1">수탁사 <span class="text-red-500">*</span></label>
            <select v-model="checkForm.contractorId" class="input w-full">
              <option value="">수탁사 선택</option>
              <option v-for="c in contractors" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
          </div>
          <div v-if="!editingCheck">
            <label class="block text-sm font-medium text-gray-700 mb-1">점검 연도 <span class="text-red-500">*</span></label>
            <input v-model.number="checkForm.checkYear" type="number" class="input w-full" :min="2020" :max="2099"/>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">점검일</label>
            <input v-model="checkForm.checkDate" type="date" class="input w-full"/>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">점검자</label>
            <input v-model="checkForm.inspector" class="input w-full" placeholder="점검자 이름"/>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">점검 상태</label>
            <select v-model="checkForm.status" class="input w-full">
              <option value="PLANNED">예정</option>
              <option value="IN_PROGRESS">진행중</option>
              <option value="COMPLETED">완료</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">비고</label>
            <textarea v-model="checkForm.notes" class="input w-full" rows="2" placeholder="비고 (선택)"></textarea>
          </div>
        </div>

        <div class="flex justify-end gap-3 mt-6">
          <button @click="showCheckModal = false" class="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50">취소</button>
          <button @click="saveCheck" :disabled="savingCheck || (!editingCheck && (!checkForm.contractorId || !checkForm.checkYear))"
            class="btn-primary disabled:opacity-50">
            {{ savingCheck ? '저장 중...' : (editingCheck ? '수정 완료' : '추가') }}
          </button>
        </div>
      </div>
    </div>

    <!-- Delete Check Modal -->
    <div v-if="showDeleteCheckModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-sm p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-2">점검 삭제</h2>
        <p class="text-sm text-gray-600 mb-6">
          <span class="font-semibold">{{ selectedCheck?.contractorName }}</span>의
          <span class="font-semibold">{{ selectedCheck?.checkYear }}년</span> 점검을 삭제하시겠습니까?<br/>
          모든 점검 결과가 함께 삭제됩니다.
        </p>
        <div class="flex justify-end gap-3">
          <button @click="showDeleteCheckModal = false" class="px-4 py-2 text-sm border border-gray-300 rounded-lg hover:bg-gray-50">취소</button>
          <button @click="deleteCheckConfirmed" :disabled="deletingCheck" class="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50">
            {{ deletingCheck ? '삭제 중...' : '삭제' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { contractorCheckApi } from '@/api'
import api from '@/api'

const years = ref([])
const selectedYear = ref(new Date().getFullYear())
const checks = ref([])
const selectedCheck = ref(null)
const detail = ref(null)
const contractors = ref([])

const loadingChecks = ref(false)
const loadingDetail = ref(false)
const showCheckModal = ref(false)
const savingCheck = ref(false)
const editingCheck = ref(false)
const showDeleteCheckModal = ref(false)
const deletingCheck = ref(false)
const editingNoteId = ref(null)
const searchKeyword = ref('')
const settingBulk = ref(false)

const checkForm = reactive({
  contractorId: '',
  checkYear: new Date().getFullYear(),
  checkDate: '',
  inspector: '',
  status: 'PLANNED',
  notes: ''
})

const resultOptions = [
  { value: 'PASS',        label: '통과',    activeClass: 'bg-green-500 border-green-500 text-white' },
  { value: 'FAIL',        label: '미흡',    activeClass: 'bg-red-500 border-red-500 text-white' },
  { value: 'NA',          label: '해당없음', activeClass: 'bg-gray-400 border-gray-400 text-white' },
  { value: 'NOT_CHECKED', label: '미점검',  activeClass: 'bg-yellow-400 border-yellow-400 text-white' },
]

const totalItems = computed(() => detail.value?.results.length ?? 0)

const groupedResults = computed(() => {
  if (!detail.value) return {}
  const kw = searchKeyword.value.trim().toLowerCase()
  const filtered = kw
    ? detail.value.results.filter(r =>
        r.checkItemName?.toLowerCase().includes(kw) ||
        r.checkItemCategory?.toLowerCase().includes(kw))
    : detail.value.results
  return filtered.reduce((acc, r) => {
    const cat = r.checkItemCategory || '기타'
    if (!acc[cat]) acc[cat] = []
    acc[cat].push(r)
    return acc
  }, {})
})

const filteredTotal = computed(() =>
  Object.values(groupedResults.value).reduce((s, g) => s + g.length, 0)
)

const passCount = computed(() => detail.value?.results.filter(r => r.result === 'PASS').length ?? 0)
const failCount = computed(() => detail.value?.results.filter(r => r.result === 'FAIL').length ?? 0)
const naCount = computed(() => detail.value?.results.filter(r => r.result === 'NA').length ?? 0)
const notCheckedCount = computed(() => detail.value?.results.filter(r => r.result === 'NOT_CHECKED').length ?? 0)
const progressPct = computed(() => {
  const total = detail.value?.results.length ?? 0
  if (!total) return 0
  return Math.round(((passCount.value + failCount.value + naCount.value) / total) * 100)
})

function statusLabel(s) {
  return { PLANNED: '예정', IN_PROGRESS: '진행중', COMPLETED: '완료' }[s] ?? s
}
function statusClass(s) {
  return {
    PLANNED: 'bg-yellow-100 text-yellow-700',
    IN_PROGRESS: 'bg-blue-100 text-blue-700',
    COMPLETED: 'bg-green-100 text-green-700'
  }[s] ?? 'bg-gray-100 text-gray-600'
}

async function loadYears() {
  try {
    const res = await contractorCheckApi.years()
    const data = res.data ?? res
    years.value = Array.isArray(data) ? data : []
    if (!years.value.includes(selectedYear.value)) {
      years.value = [...new Set([selectedYear.value, ...years.value])].sort((a, b) => b - a)
    }
  } catch {
    years.value = [new Date().getFullYear()]
  }
}

async function loadChecks() {
  loadingChecks.value = true
  selectedCheck.value = null
  detail.value = null
  try {
    const res = await contractorCheckApi.listByYear(selectedYear.value)
    checks.value = res.data ?? res
  } catch (e) {
    console.error(e)
    checks.value = []
  } finally {
    loadingChecks.value = false
  }
}

async function selectCheck(check) {
  selectedCheck.value = check
  detail.value = null
  loadingDetail.value = true
  editingNoteId.value = null
  searchKeyword.value = ''
  try {
    const res = await contractorCheckApi.getDetail(check.id)
    detail.value = res.data ?? res
  } catch (e) {
    console.error(e)
  } finally {
    loadingDetail.value = false
  }
}

async function setResult(row, value) {
  const prev = row.result
  row.result = value
  try {
    await contractorCheckApi.saveResult(selectedCheck.value.id, {
      checkItemId: row.checkItemId,
      result: value,
      notes: row.notes || null
    })
    await refreshCheckStats()
  } catch (e) {
    row.result = prev
    alert(e || '결과 저장에 실패했습니다.')
  }
}

async function saveNotes(row) {
  editingNoteId.value = null
  try {
    await contractorCheckApi.saveResult(selectedCheck.value.id, {
      checkItemId: row.checkItemId,
      result: row.result,
      notes: row.notes || null
    })
  } catch (e) {
    console.error(e)
  }
}

async function refreshCheckStats() {
  try {
    const res = await contractorCheckApi.listByYear(selectedYear.value)
    const updated = res.data ?? res
    checks.value = Array.isArray(updated) ? updated : []
    const found = checks.value.find(c => c.id === selectedCheck.value?.id)
    if (found) selectedCheck.value = found
  } catch {}
}

async function setBulkResult(result) {
  const msg = result === 'PASS'
    ? '모든 점검항목을 "통과"로 설정하시겠습니까?'
    : '모든 점검항목을 "미점검"으로 초기화하시겠습니까?'
  if (!confirm(msg)) return
  settingBulk.value = true
  try {
    await contractorCheckApi.bulkResult(selectedCheck.value.id, result)
    if (detail.value) {
      detail.value.results.forEach(r => {
        r.result = result
        if (result === 'NOT_CHECKED') r.notes = null
      })
    }
    await refreshCheckStats()
  } catch (e) {
    alert(e || '설정에 실패했습니다.')
  } finally {
    settingBulk.value = false
  }
}

async function syncItems() {
  try {
    await contractorCheckApi.syncResults(selectedCheck.value.id)
    await selectCheck(selectedCheck.value)
  } catch (e) {
    alert(e || '동기화에 실패했습니다.')
  }
}

function openCreateCheck() {
  editingCheck.value = false
  Object.assign(checkForm, {
    contractorId: '',
    checkYear: selectedYear.value,
    checkDate: new Date().toISOString().slice(0, 10),
    inspector: '',
    status: 'PLANNED',
    notes: ''
  })
  showCheckModal.value = true
}

function openEditCheck() {
  editingCheck.value = true
  Object.assign(checkForm, {
    contractorId: selectedCheck.value.contractorId,
    checkYear: selectedCheck.value.checkYear,
    checkDate: selectedCheck.value.checkDate || '',
    inspector: selectedCheck.value.inspector || '',
    status: selectedCheck.value.status,
    notes: selectedCheck.value.notes || ''
  })
  showCheckModal.value = true
}

async function saveCheck() {
  savingCheck.value = true
  try {
    const payload = {
      contractorId: Number(checkForm.contractorId),
      checkYear: checkForm.checkYear,
      checkDate: checkForm.checkDate || null,
      inspector: checkForm.inspector || null,
      status: checkForm.status,
      notes: checkForm.notes || null
    }
    if (editingCheck.value) {
      await contractorCheckApi.update(selectedCheck.value.id, payload)
    } else {
      await contractorCheckApi.create(payload)
      if (!years.value.includes(checkForm.checkYear)) {
        selectedYear.value = checkForm.checkYear
      }
    }
    showCheckModal.value = false
    await loadYears()
    await loadChecks()
    if (editingCheck.value) {
      const found = checks.value.find(c => c.id === selectedCheck.value?.id)
      if (found) await selectCheck(found)
    }
  } catch (e) {
    alert(e || '저장에 실패했습니다.')
  } finally {
    savingCheck.value = false
  }
}

function confirmDeleteCheck() {
  showDeleteCheckModal.value = true
}

async function deleteCheckConfirmed() {
  deletingCheck.value = true
  try {
    await contractorCheckApi.delete(selectedCheck.value.id)
    showDeleteCheckModal.value = false
    selectedCheck.value = null
    detail.value = null
    await loadYears()
    await loadChecks()
  } catch (e) {
    alert(e || '삭제에 실패했습니다.')
  } finally {
    deletingCheck.value = false
  }
}

onMounted(async () => {
  await loadYears()
  await loadChecks()
  try {
    const res = await api.get('/privacy/contractors')
    const data = res.data ?? res
    contractors.value = (Array.isArray(data) ? data : []).filter(c => c.status === 'ACTIVE')
  } catch {}
})
</script>
