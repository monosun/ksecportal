<template>
  <div class="p-6">
    <!-- Header -->
    <div class="flex items-center justify-between mb-4">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">위험 처리 계획</h1>
        <p class="text-sm text-gray-500 mt-1">위험평가 <strong>완료 차수</strong>의 '감소' 처리 항목에 대한 조치 계획을 수립·관리합니다 (연도/차수별 스냅샷)</p>
      </div>
    </div>

    <!-- 연도 네비게이션 + 완료 차수 선택 -->
    <div class="flex items-center gap-3 mb-5 flex-wrap">
      <div class="flex items-center gap-2 bg-white border border-gray-200 rounded-lg px-3 py-1.5">
        <button @click="currentYear--" class="text-gray-400 hover:text-gray-700 px-1">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
          </svg>
        </button>
        <span class="text-base font-bold text-gray-800 min-w-16 text-center">{{ currentYear }}년</span>
        <button @click="currentYear++" class="text-gray-400 hover:text-gray-700 px-1">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
          </svg>
        </button>
      </div>

      <select v-if="completedRounds.length > 0" v-model="selectedRoundId" @change="loadPlans" class="input w-64">
        <option v-for="r in completedRounds" :key="r.id" :value="r.id">
          {{ r.roundNo }}차 ({{ r.roundDate }}){{ r.title ? ' — ' + r.title : '' }}
        </option>
      </select>
      <span v-else class="text-sm text-gray-400">이 연도에 완료된 차수가 없습니다</span>

      <div class="flex-1"></div>

      <div v-if="selectedRound" class="text-sm text-gray-500">
        <span class="font-semibold text-gray-700">{{ items.length }}</span>개 감소 항목 ·
        완료 <span class="font-semibold text-green-600">{{ statusCount('완료') }}</span> /
        진행중 <span class="font-semibold text-blue-600">{{ statusCount('진행중') }}</span> /
        기한초과 <span class="font-semibold text-red-600">{{ overdueCount }}</span>
      </div>
    </div>

    <!-- 선택 차수 정보 -->
    <div v-if="selectedRound" class="bg-blue-50 border border-blue-200 rounded-lg px-4 py-2.5 mb-4 flex items-center gap-4 text-sm flex-wrap">
      <span class="font-semibold text-blue-800">{{ currentYear }}년 {{ selectedRound.roundNo }}차</span>
      <span class="text-blue-600">{{ selectedRound.roundDate }}</span>
      <span v-if="selectedRound.title" class="text-blue-600">· {{ selectedRound.title }}</span>
      <span class="px-2 py-0.5 rounded-full text-xs font-semibold bg-green-100 text-green-700">완료 차수</span>
    </div>

    <!-- Tabs -->
    <div v-if="selectedRound" class="flex gap-1 mb-4 bg-gray-100 p-1 rounded-xl w-fit">
      <button v-for="tab in tabs" :key="tab.key" @click="activeTab = tab.key"
        class="px-4 py-1.5 rounded-lg text-sm font-medium transition-colors"
        :class="activeTab === tab.key ? 'bg-white text-gray-900 shadow-sm' : 'text-gray-500 hover:text-gray-700'">
        {{ tab.label }}
        <span class="ml-1.5 text-xs px-1.5 py-0.5 rounded-full"
          :class="activeTab === tab.key ? 'bg-gray-100 text-gray-700' : 'bg-gray-200 text-gray-500'">
          {{ tabCount(tab.key) }}
        </span>
      </button>
    </div>

    <!-- 검색 필터 -->
    <div v-if="selectedRound" class="bg-white border border-gray-200 rounded-lg px-4 py-3 mb-3 flex items-center gap-3 flex-wrap">
      <svg class="w-4 h-4 text-gray-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
      </svg>
      <input v-model="searchAsset" type="text" placeholder="자산명 검색" class="input w-32 text-sm py-1.5"/>
      <input v-model="searchThreat" type="text" placeholder="위협명 검색" class="input w-32 text-sm py-1.5"/>
      <input v-model="searchVuln" type="text" placeholder="취약점 검색" class="input w-32 text-sm py-1.5"/>
      <select v-model="searchGrade" class="input w-24 text-sm py-1.5">
        <option value="">등급 전체</option>
        <option value="HIGH">고위험</option>
        <option value="MEDIUM">중위험</option>
        <option value="LOW">저위험</option>
      </select>
      <input v-model="searchAssignee" type="text" placeholder="담당자 검색" class="input w-28 text-sm py-1.5"/>
      <button v-if="hasActiveFilter" @click="clearSearch" class="text-xs text-gray-400 hover:text-gray-600">초기화</button>
    </div>

    <!-- Table -->
    <div class="card p-0 overflow-hidden">
      <div v-if="!selectedRound" class="p-12 text-center text-gray-400 text-sm">
        연도를 이동하여 <strong>완료된 차수</strong>를 선택하세요. 해당 차수의 '감소' 항목이 처리 계획 대상으로 표시됩니다.
      </div>
      <div v-else-if="loading" class="p-8 text-center text-gray-400 text-sm">로딩 중...</div>
      <table v-else class="w-full text-sm">
        <thead class="bg-gray-50 border-b border-gray-100">
          <tr>
            <th class="text-left px-5 py-3 font-semibold text-gray-600">위험 항목 (자산 · 위협 · 취약점)</th>
            <th class="text-center px-4 py-3 font-semibold text-gray-600">위험도</th>
            <th class="text-left px-5 py-3 font-semibold text-gray-600">조치 계획</th>
            <th class="text-left px-4 py-3 font-semibold text-gray-600">담당자</th>
            <th class="text-left px-4 py-3 font-semibold text-gray-600">완료 기한</th>
            <th class="text-left px-5 py-3 font-semibold text-gray-600 w-44">진행률</th>
            <th class="text-center px-4 py-3 font-semibold text-gray-600">상태</th>
            <th class="text-center px-4 py-3 font-semibold text-gray-600">관리</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-50">
          <tr v-if="filteredItems.length === 0">
            <td colspan="8" class="px-5 py-10 text-center text-gray-400">
              {{ items.length === 0 ? "이 차수에는 '감소' 처리 항목이 없습니다." : '검색/상태 조건에 맞는 처리 계획이 없습니다.' }}
            </td>
          </tr>
          <tr v-for="item in pagedItems" :key="item.id" class="hover:bg-gray-50 transition-colors">
            <td class="px-5 py-3.5">
              <p class="font-medium text-gray-900">{{ item.assetName || '-' }}</p>
              <p class="text-xs text-gray-500 mt-0.5">
                {{ item.threatName || '-' }}<span v-if="item.vulnerability" class="text-gray-400"> · {{ item.vulnerability }}</span>
              </p>
            </td>
            <td class="px-4 py-3.5 text-center">
              <span class="font-bold" :class="scoreColor(item.riskScore)">{{ item.riskScore }}</span>
              <span class="ml-1 px-1.5 py-0.5 rounded-full text-xs font-bold" :class="gradeBadge(item.riskGrade)">{{ gradeLabel(item.riskGrade) }}</span>
            </td>
            <td class="px-5 py-3.5 text-gray-600 max-w-[220px] truncate" :title="item.plan">
              {{ item.plan || '—' }}
            </td>
            <td class="px-4 py-3.5 text-gray-700">{{ item.planAssignee || '-' }}</td>
            <td class="px-4 py-3.5" :class="isOverdue(item) ? 'text-red-600 font-medium' : 'text-gray-500'">
              {{ item.planDueDate || '-' }}
              <span v-if="isOverdue(item)" class="ml-1 text-xs bg-red-100 text-red-600 px-1 rounded">지연</span>
            </td>
            <td class="px-5 py-3.5">
              <div class="flex items-center gap-2">
                <div class="flex-1 bg-gray-100 rounded-full h-2">
                  <div class="h-2 rounded-full transition-all duration-500" :class="progressBarColor(item.planProgress)"
                    :style="{ width: (item.planProgress || 0) + '%' }"></div>
                </div>
                <span class="text-xs font-semibold text-gray-600 w-8 text-right">{{ item.planProgress || 0 }}%</span>
              </div>
            </td>
            <td class="px-4 py-3.5 text-center">
              <span class="px-2.5 py-1 rounded-full text-xs font-bold" :class="statusBadge(item.planStatus)">{{ item.planStatus || '계획중' }}</span>
            </td>
            <td class="px-4 py-3.5 text-center">
              <button @click="openEdit(item)" class="text-blue-600 hover:text-blue-800 text-xs font-medium">계획 수립</button>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 페이지네이션 -->
      <div v-if="selectedRound && !loading && filteredItems.length > 0"
        class="flex items-center justify-between px-4 py-3 border-t border-gray-100 bg-white">
        <div class="flex items-center gap-3 text-sm text-gray-500">
          <span>
            {{ (currentPage - 1) * pageSize + 1 }}–{{ Math.min(currentPage * pageSize, filteredItems.length) }}
            / {{ filteredItems.length }}건
          </span>
          <select v-model.number="pageSize" class="text-xs border border-gray-200 rounded px-2 py-1 text-gray-600 bg-white">
            <option :value="10">10개</option>
            <option :value="20">20개</option>
            <option :value="50">50개</option>
            <option :value="100">100개</option>
          </select>
        </div>
        <div class="flex items-center gap-1">
          <button @click="currentPage = 1" :disabled="currentPage === 1"
            class="px-2 py-1 rounded text-xs text-gray-500 hover:bg-gray-100 disabled:opacity-30">«</button>
          <button @click="currentPage--" :disabled="currentPage === 1"
            class="px-2 py-1 rounded text-xs text-gray-500 hover:bg-gray-100 disabled:opacity-30">‹</button>
          <button v-for="p in pageRange" :key="p" @click="currentPage = p"
            class="min-w-[28px] px-2 py-1 rounded text-xs font-medium transition-colors"
            :class="p === currentPage ? 'bg-primary-500 text-white' : 'text-gray-600 hover:bg-gray-100'">
            {{ p }}
          </button>
          <button @click="currentPage++" :disabled="currentPage === totalPages"
            class="px-2 py-1 rounded text-xs text-gray-500 hover:bg-gray-100 disabled:opacity-30">›</button>
          <button @click="currentPage = totalPages" :disabled="currentPage === totalPages"
            class="px-2 py-1 rounded text-xs text-gray-500 hover:bg-gray-100 disabled:opacity-30">»</button>
        </div>
      </div>
    </div>

    <!-- 계획 수립/수정 모달 -->
    <div v-if="showModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-xl p-6 max-h-[92vh] overflow-y-auto">
        <div class="flex items-center justify-between mb-5">
          <h2 class="text-lg font-semibold text-gray-900">처리 계획 수립</h2>
          <button @click="closeModal" class="text-gray-400 hover:text-gray-600">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>

        <!-- 위험 항목 정보 (읽기전용, 평가 스냅샷) -->
        <div v-if="editTarget" class="bg-gray-50 rounded-lg p-3 mb-4 text-sm">
          <div class="flex items-center gap-2 mb-1">
            <span class="font-semibold text-gray-800">{{ editTarget.assetName }}</span>
            <span class="text-gray-400">·</span>
            <span class="text-gray-700">{{ editTarget.threatName }}</span>
            <span class="ml-auto font-bold" :class="scoreColor(editTarget.riskScore)">위험점수 {{ editTarget.riskScore }}</span>
            <span class="px-1.5 py-0.5 rounded-full text-xs font-bold" :class="gradeBadge(editTarget.riskGrade)">{{ gradeLabel(editTarget.riskGrade) }}</span>
          </div>
          <p v-if="editTarget.vulnerability" class="text-xs text-gray-500">취약점 · {{ editTarget.vulnerability }}</p>
        </div>

        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">조치 계획</label>
            <textarea v-model="form.plan" class="input w-full" rows="3" placeholder="구체적인 조치 계획을 입력하세요"></textarea>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">담당자</label>
              <input v-model="form.planAssignee" class="input w-full" placeholder="담당자 이름"/>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">완료 기한</label>
              <input v-model="form.planDueDate" type="date" class="input w-full"/>
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">상태</label>
            <select v-model="form.planStatus" class="input w-full">
              <option v-for="s in statusOptions" :key="s" :value="s">{{ s }}</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">진행률 ({{ form.planProgress }}%)</label>
            <input type="range" v-model.number="form.planProgress" min="0" max="100" step="5" class="w-full accent-blue-600"/>
            <div class="flex justify-between text-xs text-gray-400 mt-1"><span>0%</span><span>50%</span><span>100%</span></div>
          </div>
        </div>

        <p v-if="saveError" class="mt-3 text-sm text-red-600">{{ saveError }}</p>
        <div class="flex justify-end gap-3 mt-6">
          <button @click="closeModal" class="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50">취소</button>
          <button @click="saveForm" :disabled="saving" class="btn-primary disabled:opacity-50">
            {{ saving ? '저장 중...' : '계획 저장' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { riskApi } from '@/api'

const tabs = [
  { key: 'ALL', label: '전체' },
  { key: '계획중', label: '계획중' },
  { key: '진행중', label: '진행중' },
  { key: '완료', label: '완료' },
  { key: '기한초과', label: '기한초과' }
]
const statusOptions = ['계획중', '진행중', '완료', '기한초과']

const currentYear = ref(new Date().getFullYear())
const rounds = ref([])
const selectedRoundId = ref(null)
const items = ref([])
const loading = ref(false)
const activeTab = ref('ALL')

const completedRounds = computed(() => rounds.value.filter(r => r.status === 'COMPLETED'))
const selectedRound = computed(() => completedRounds.value.find(r => r.id === selectedRoundId.value) || null)

// 검색 필터
const searchAsset = ref('')
const searchThreat = ref('')
const searchVuln = ref('')
const searchGrade = ref('')
const searchAssignee = ref('')

const hasActiveFilter = computed(() =>
  !!(searchAsset.value || searchThreat.value || searchVuln.value || searchGrade.value || searchAssignee.value)
)

function clearSearch() {
  searchAsset.value = ''
  searchThreat.value = ''
  searchVuln.value = ''
  searchGrade.value = ''
  searchAssignee.value = ''
}

// 검색어로 걸러진 항목 (상태 탭 제외)
const searchFiltered = computed(() => items.value.filter(i => {
  if (searchAsset.value && !i.assetName?.toLowerCase().includes(searchAsset.value.toLowerCase())) return false
  if (searchThreat.value && !i.threatName?.toLowerCase().includes(searchThreat.value.toLowerCase())) return false
  if (searchVuln.value && !i.vulnerability?.toLowerCase().includes(searchVuln.value.toLowerCase())) return false
  if (searchGrade.value && i.riskGrade !== searchGrade.value) return false
  if (searchAssignee.value && !i.planAssignee?.toLowerCase().includes(searchAssignee.value.toLowerCase())) return false
  return true
}))

const filteredItems = computed(() => {
  if (activeTab.value === 'ALL') return searchFiltered.value
  return searchFiltered.value.filter(i => (i.planStatus || '계획중') === activeTab.value)
})

function tabCount(key) {
  if (key === 'ALL') return searchFiltered.value.length
  return searchFiltered.value.filter(i => (i.planStatus || '계획중') === key).length
}

// 페이지네이션
const currentPage = ref(1)
const pageSize = ref(20)
const totalPages = computed(() => Math.ceil(filteredItems.value.length / pageSize.value) || 1)
const pagedItems = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredItems.value.slice(start, start + pageSize.value)
})
const pageRange = computed(() => {
  const total = totalPages.value
  const cur = currentPage.value
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  let start = Math.max(1, cur - 3)
  let end = Math.min(total, cur + 3)
  if (end - start < 6) {
    if (start === 1) end = Math.min(total, 7)
    else start = Math.max(1, end - 6)
  }
  return Array.from({ length: end - start + 1 }, (_, i) => start + i)
})
watch([filteredItems, selectedRoundId, activeTab], () => { currentPage.value = 1 })
function statusCount(status) {
  return items.value.filter(i => (i.planStatus || '계획중') === status).length
}
const overdueCount = computed(() => items.value.filter(isOverdue).length)

function isOverdue(item) {
  if (!item.planDueDate || item.planStatus === '완료') return false
  return new Date(item.planDueDate) < new Date(new Date().toDateString())
}

// ── 모달 ────────────────────────────────────────────────
const showModal = ref(false)
const editTarget = ref(null)
const saving = ref(false)
const saveError = ref('')
const form = reactive({ plan: '', planAssignee: '', planDueDate: '', planProgress: 0, planStatus: '계획중' })

function openEdit(item) {
  editTarget.value = item
  saveError.value = ''
  form.plan = item.plan || ''
  form.planAssignee = item.planAssignee || ''
  form.planDueDate = item.planDueDate || ''
  form.planProgress = item.planProgress || 0
  form.planStatus = item.planStatus || '계획중'
  showModal.value = true
}
function closeModal() { showModal.value = false; editTarget.value = null }

async function saveForm() {
  saving.value = true
  saveError.value = ''
  try {
    const res = await riskApi.updateTreatmentPlan(editTarget.value.id, {
      plan: form.plan || null,
      planAssignee: form.planAssignee || null,
      planDueDate: form.planDueDate || null,
      planProgress: form.planProgress,
      planStatus: form.planStatus
    })
    const idx = items.value.findIndex(i => i.id === editTarget.value.id)
    if (idx !== -1) items.value[idx] = res.data
    closeModal()
  } catch (e) {
    saveError.value = e || '저장에 실패했습니다.'
  } finally {
    saving.value = false
  }
}

// ── 데이터 로드 ──────────────────────────────────────────
async function loadRounds() {
  try {
    const res = await riskApi.listRounds(currentYear.value)
    rounds.value = res.data || []
    const stillExists = completedRounds.value.find(r => r.id === selectedRoundId.value)
    if (!stillExists) {
      selectedRoundId.value = completedRounds.value[0]?.id ?? null
    }
    await loadPlans()
  } catch {
    rounds.value = []
    selectedRoundId.value = null
    items.value = []
  }
}

async function loadPlans() {
  if (!selectedRoundId.value) { items.value = []; return }
  loading.value = true
  try {
    const res = await riskApi.listTreatmentPlans(selectedRoundId.value)
    items.value = res.data || []
  } catch {
    items.value = []
  } finally {
    loading.value = false
  }
}

watch(currentYear, loadRounds)
onMounted(loadRounds)

// ── 헬퍼 ────────────────────────────────────────────────
function gradeBadge(grade) {
  return { HIGH: 'bg-red-100 text-red-700', MEDIUM: 'bg-yellow-100 text-yellow-700', LOW: 'bg-green-100 text-green-700' }[grade] || 'bg-gray-100 text-gray-700'
}
function gradeLabel(grade) {
  return { HIGH: '고위험', MEDIUM: '중위험', LOW: '저위험' }[grade] || grade
}
function scoreColor(score) {
  if (score >= 15) return 'text-red-600'
  if (score >= 8) return 'text-yellow-600'
  return 'text-green-600'
}
function statusBadge(status) {
  return { '계획중': 'bg-gray-100 text-gray-600', '진행중': 'bg-blue-100 text-blue-700', '완료': 'bg-green-100 text-green-700', '기한초과': 'bg-red-100 text-red-700' }[status] || 'bg-gray-100 text-gray-600'
}
function progressBarColor(progress) {
  if (progress >= 100) return 'bg-green-500'
  if (progress >= 50) return 'bg-blue-500'
  return 'bg-yellow-500'
}
</script>
