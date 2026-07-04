<template>
  <div class="p-6">
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">위협 카탈로그 관리</h1>
        <p class="text-sm text-gray-500 mt-1">조직의 정보보안 위협 목록을 관리합니다</p>
      </div>
      <div class="flex items-center gap-2">
        <button @click="handleReset" class="btn-secondary flex items-center gap-2 text-red-600 border-red-200 hover:bg-red-50">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
          </svg>
          초기화
        </button>
        <button @click="handleLoadDefaults" class="btn-secondary flex items-center gap-2">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12"/>
          </svg>
          기본 위협항목 추가
        </button>
        <button @click="openCreate" class="btn-primary flex items-center gap-2">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          위협 추가
        </button>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center items-center py-20">
      <div class="w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
    </div>

    <!-- Empty (데이터 자체가 없을 때) -->
    <div v-else-if="threats.length === 0" class="card flex flex-col items-center justify-center py-16 text-center">
      <svg class="w-12 h-12 text-gray-300 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 9v2m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/>
      </svg>
      <p class="text-gray-500 font-medium">등록된 위협 항목이 없습니다</p>
      <p class="text-gray-400 text-sm mt-1">"기본 위협항목 추가" 버튼으로 560개 기본 항목을 불러오거나<br/>"위협 추가" 버튼으로 직접 추가할 수 있습니다.</p>
    </div>

    <!-- Table -->
    <div v-else class="card p-0 overflow-hidden">
      <!-- 결과 요약 & 필터 초기화 -->
      <div class="flex items-center justify-between px-5 py-2.5 border-b border-gray-100 bg-gray-50/70">
        <span class="text-sm text-gray-500">
          전체 <span class="font-semibold text-gray-800">{{ threats.length }}</span>건 중
          <span class="font-semibold text-blue-600">{{ filteredThreats.length }}</span>건 표시
        </span>
        <button v-if="hasFilter" @click="clearFilter"
          class="flex items-center gap-1 text-xs text-gray-500 hover:text-blue-600 transition-colors">
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
          필터 초기화
        </button>
      </div>

      <div class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <!-- 컬럼 헤더 -->
            <tr class="bg-gray-50 border-b border-gray-100">
              <th class="text-left px-4 py-3 font-semibold text-gray-600 whitespace-nowrap">위협명</th>
              <th class="text-left px-4 py-3 font-semibold text-gray-600 whitespace-nowrap">유형</th>
              <th class="text-left px-4 py-3 font-semibold text-gray-600 whitespace-nowrap">카테고리</th>
              <th class="text-center px-4 py-3 font-semibold text-gray-600 whitespace-nowrap">발생가능성</th>
              <th class="text-center px-4 py-3 font-semibold text-gray-600 whitespace-nowrap">잠재영향</th>
              <th class="text-center px-4 py-3 font-semibold text-gray-600 whitespace-nowrap">위험점수</th>
              <th class="text-left px-4 py-3 font-semibold text-gray-600 whitespace-nowrap">수정일</th>
              <th class="text-center px-4 py-3 font-semibold text-gray-600 whitespace-nowrap">관리</th>
            </tr>
            <!-- 필터 행 -->
            <tr class="bg-blue-50/40 border-b border-blue-100">
              <th class="px-3 py-2">
                <input
                  v-model="filterName"
                  type="text"
                  placeholder="위협명 검색"
                  class="w-full text-xs border border-gray-200 rounded px-2 py-1.5 font-normal focus:outline-none focus:border-blue-400 focus:ring-1 focus:ring-blue-200 bg-white placeholder-gray-400"
                />
              </th>
              <th class="px-3 py-2">
                <select
                  v-model="filterType"
                  class="w-full text-xs border border-gray-200 rounded px-2 py-1.5 font-normal focus:outline-none focus:border-blue-400 focus:ring-1 focus:ring-blue-200 bg-white text-gray-700"
                >
                  <option value="">전체 유형</option>
                  <option v-for="t in threatTypes" :key="t" :value="t">{{ t }}</option>
                </select>
              </th>
              <th class="px-3 py-2">
                <input
                  v-model="filterCategory"
                  type="text"
                  placeholder="카테고리 검색"
                  class="w-full text-xs border border-gray-200 rounded px-2 py-1.5 font-normal focus:outline-none focus:border-blue-400 focus:ring-1 focus:ring-blue-200 bg-white placeholder-gray-400"
                />
              </th>
              <th class="px-3 py-2">
                <select
                  v-model="filterLikelihood"
                  class="w-full text-xs border border-gray-200 rounded px-2 py-1.5 font-normal focus:outline-none focus:border-blue-400 focus:ring-1 focus:ring-blue-200 bg-white text-gray-700"
                >
                  <option value="">전체</option>
                  <option v-for="n in 5" :key="n" :value="n">{{ n }}</option>
                </select>
              </th>
              <th class="px-3 py-2">
                <select
                  v-model="filterImpact"
                  class="w-full text-xs border border-gray-200 rounded px-2 py-1.5 font-normal focus:outline-none focus:border-blue-400 focus:ring-1 focus:ring-blue-200 bg-white text-gray-700"
                >
                  <option value="">전체</option>
                  <option v-for="n in 5" :key="n" :value="n">{{ n }}</option>
                </select>
              </th>
              <th class="px-3 py-2">
                <select
                  v-model="filterRiskLevel"
                  class="w-full text-xs border border-gray-200 rounded px-2 py-1.5 font-normal focus:outline-none focus:border-blue-400 focus:ring-1 focus:ring-blue-200 bg-white text-gray-700"
                >
                  <option value="">전체</option>
                  <option value="low">낮음 (≤7)</option>
                  <option value="mid">중간 (8~15)</option>
                  <option value="high">높음 (≥16)</option>
                </select>
              </th>
              <th class="px-3 py-2">
                <input
                  v-model="filterDate"
                  type="date"
                  class="w-full text-xs border border-gray-200 rounded px-2 py-1.5 font-normal focus:outline-none focus:border-blue-400 focus:ring-1 focus:ring-blue-200 bg-white text-gray-700"
                />
              </th>
              <th class="px-3 py-2 text-center">
                <button
                  v-if="hasFilter"
                  @click="clearFilter"
                  title="필터 초기화"
                  class="inline-flex items-center justify-center w-6 h-6 rounded text-gray-400 hover:text-red-500 hover:bg-red-50 transition-colors"
                >
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                  </svg>
                </button>
              </th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-50">
            <!-- 필터 결과 없음 -->
            <tr v-if="filteredThreats.length === 0">
              <td colspan="8" class="text-center py-14 text-gray-400">
                <svg class="w-8 h-8 mx-auto mb-2 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
                </svg>
                <p class="text-sm">검색 조건에 맞는 위협 항목이 없습니다</p>
                <button @click="clearFilter" class="mt-2 text-xs text-blue-500 hover:underline">필터 초기화</button>
              </td>
            </tr>
            <tr v-for="threat in filteredThreats" :key="threat.id" class="hover:bg-gray-50 transition-colors">
              <td class="px-4 py-3.5 font-medium text-gray-900">{{ threat.name }}</td>
              <td class="px-4 py-3.5">
                <span class="px-2 py-0.5 rounded-full text-xs font-semibold" :class="typeClass(threat.type)">{{ threat.type }}</span>
              </td>
              <td class="px-4 py-3.5 text-gray-600 text-sm">{{ threat.category || '-' }}</td>
              <td class="px-4 py-3.5 text-center">
                <div class="flex items-center justify-center gap-0.5">
                  <span v-for="n in 5" :key="n"
                    class="w-3 h-3 rounded-full"
                    :class="n <= threat.likelihood ? 'bg-blue-500' : 'bg-gray-200'">
                  </span>
                  <span class="ml-1.5 text-xs font-semibold text-gray-700">{{ threat.likelihood }}</span>
                </div>
              </td>
              <td class="px-4 py-3.5 text-center">
                <div class="flex items-center justify-center gap-0.5">
                  <span v-for="n in 5" :key="n"
                    class="w-3 h-3 rounded-full"
                    :class="n <= threat.impact ? 'bg-orange-500' : 'bg-gray-200'">
                  </span>
                  <span class="ml-1.5 text-xs font-semibold text-gray-700">{{ threat.impact }}</span>
                </div>
              </td>
              <td class="px-4 py-3.5 text-center">
                <span class="px-2.5 py-1 rounded-full text-xs font-bold" :class="riskScoreClass(threat.riskScore)">
                  {{ threat.riskScore }}
                </span>
              </td>
              <td class="px-4 py-3.5 text-sm text-gray-500 whitespace-nowrap">{{ formatDate(threat.updatedAt) }}</td>
              <td class="px-4 py-3.5 text-center">
                <div class="flex items-center justify-center gap-2">
                  <button @click="openEdit(threat)" class="text-blue-600 hover:text-blue-800 text-xs font-medium">수정</button>
                  <button @click="confirmDelete(threat)" class="text-red-500 hover:text-red-700 text-xs font-medium">삭제</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Add/Edit Modal -->
    <div v-if="showModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-xl p-6 max-h-[90vh] overflow-y-auto">
        <div class="flex items-center justify-between mb-5">
          <h2 class="text-lg font-semibold text-gray-900">{{ editTarget ? '위협 수정' : '위협 추가' }}</h2>
          <button @click="closeModal" class="text-gray-400 hover:text-gray-600">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>

        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">위협명 <span class="text-red-500">*</span></label>
            <input v-model="form.name" class="input w-full" placeholder="위협명을 입력하세요"/>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">위협 유형 <span class="text-red-500">*</span></label>
            <select v-model="form.type" class="input w-full">
              <option value="">유형 선택</option>
              <option v-for="t in threatTypes" :key="t" :value="t">{{ t }}</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">카테고리</label>
            <input v-model="form.category" class="input w-full" placeholder="예: IAM, WEB/API, EKS"/>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">설명</label>
            <textarea v-model="form.description" class="input w-full" rows="3" placeholder="위협에 대한 설명을 입력하세요"></textarea>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">발생 가능성 ({{ form.likelihood }})</label>
            <div class="flex items-center gap-2">
              <div class="flex gap-1">
                <button v-for="n in 5" :key="n" @click="form.likelihood = n"
                  class="w-8 h-8 rounded-full text-xs font-bold transition-colors"
                  :class="form.likelihood >= n ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-500 hover:bg-gray-200'">
                  {{ n }}
                </button>
              </div>
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">잠재 영향 ({{ form.impact }})</label>
            <div class="flex items-center gap-2">
              <div class="flex gap-1">
                <button v-for="n in 5" :key="n" @click="form.impact = n"
                  class="w-8 h-8 rounded-full text-xs font-bold transition-colors"
                  :class="form.impact >= n ? 'bg-orange-500 text-white' : 'bg-gray-100 text-gray-500 hover:bg-gray-200'">
                  {{ n }}
                </button>
              </div>
            </div>
          </div>
          <div class="flex items-center gap-3 p-3 rounded-lg bg-gray-50">
            <span class="text-sm text-gray-600">위험점수:</span>
            <span class="px-3 py-1 rounded-full text-sm font-bold" :class="riskScoreClass(form.likelihood * form.impact)">
              {{ form.likelihood * form.impact }}
            </span>
            <span class="text-xs text-gray-400">(발생가능성 × 잠재영향)</span>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">비고</label>
            <input v-model="form.remark" class="input w-full" placeholder="추가 메모 (선택)"/>
          </div>
        </div>

        <div class="flex justify-end gap-3 mt-6">
          <button @click="closeModal" class="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50">취소</button>
          <button @click="saveForm" :disabled="saving || !form.name || !form.type" class="btn-primary disabled:opacity-50">
            {{ saving ? '저장 중...' : (editTarget ? '수정 완료' : '추가') }}
          </button>
        </div>
      </div>
    </div>

    <!-- Delete Confirm Modal -->
    <div v-if="showDeleteModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-sm p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-2">위협 삭제</h2>
        <p class="text-sm text-gray-600 mb-6">
          <span class="font-semibold text-gray-900">{{ deleteTarget?.name }}</span> 위협을 삭제하시겠습니까?<br/>
          이 작업은 되돌릴 수 없습니다.
        </p>
        <div class="flex justify-end gap-3">
          <button @click="showDeleteModal = false" class="px-4 py-2 text-sm border border-gray-300 rounded-lg hover:bg-gray-50">취소</button>
          <button @click="deleteConfirmed" :disabled="deleting" class="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50">
            {{ deleting ? '삭제 중...' : '삭제' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { threatApi, codeApi } from '@/api'

const threatTypes = ref([])

const threats = ref([])
const loading = ref(false)
const saving = ref(false)
const deleting = ref(false)
const showModal = ref(false)
const showDeleteModal = ref(false)
const editTarget = ref(null)
const deleteTarget = ref(null)

// 필터 상태
const filterName = ref('')
const filterType = ref('')
const filterCategory = ref('')
const filterLikelihood = ref('')
const filterImpact = ref('')
const filterRiskLevel = ref('')
const filterDate = ref('')

const hasFilter = computed(() =>
  filterName.value ||
  filterType.value ||
  filterCategory.value ||
  filterLikelihood.value !== '' ||
  filterImpact.value !== '' ||
  filterRiskLevel.value ||
  filterDate.value
)

const filteredThreats = computed(() => {
  return threats.value.filter(t => {
    if (filterName.value && !t.name.toLowerCase().includes(filterName.value.toLowerCase())) return false
    if (filterType.value && t.type !== filterType.value) return false
    if (filterCategory.value && !(t.category || '').toLowerCase().includes(filterCategory.value.toLowerCase())) return false
    if (filterLikelihood.value !== '' && t.likelihood !== Number(filterLikelihood.value)) return false
    if (filterImpact.value !== '' && t.impact !== Number(filterImpact.value)) return false
    if (filterRiskLevel.value) {
      const score = t.riskScore
      if (filterRiskLevel.value === 'low' && score > 7) return false
      if (filterRiskLevel.value === 'mid' && (score < 8 || score > 15)) return false
      if (filterRiskLevel.value === 'high' && score < 16) return false
    }
    if (filterDate.value && !String(t.updatedAt || '').startsWith(filterDate.value)) return false
    return true
  })
})

function clearFilter() {
  filterName.value = ''
  filterType.value = ''
  filterCategory.value = ''
  filterLikelihood.value = ''
  filterImpact.value = ''
  filterRiskLevel.value = ''
  filterDate.value = ''
}

const defaultForm = () => ({ name: '', type: '', category: '', description: '', likelihood: 3, impact: 3, remark: '' })
const form = reactive(defaultForm())

async function loadList() {
  loading.value = true
  try {
    const res = await threatApi.list()
    threats.value = res.data ?? res
  } catch (e) {
    console.error('위협 목록 로드 실패', e)
  } finally {
    loading.value = false
  }
}

function riskScoreClass(score) {
  if (score <= 7) return 'bg-green-100 text-green-700'
  if (score <= 15) return 'bg-yellow-100 text-yellow-700'
  return 'bg-red-100 text-red-700'
}

function typeClass(type) {
  const map = {
    '외부공격': 'bg-red-50 text-red-700',
    '내부위협': 'bg-orange-50 text-orange-700',
    '기술적위협': 'bg-blue-50 text-blue-700',
    '물리적위협': 'bg-purple-50 text-purple-700',
    '인적위협': 'bg-yellow-50 text-yellow-700',
    '자연재해': 'bg-green-50 text-green-700',
    '기타': 'bg-gray-100 text-gray-700',
  }
  return map[type] || 'bg-gray-100 text-gray-600'
}

function formatDate(dt) {
  if (!dt) return '-'
  return String(dt).slice(0, 10)
}

function openCreate() {
  editTarget.value = null
  Object.assign(form, defaultForm())
  showModal.value = true
}

function openEdit(threat) {
  editTarget.value = threat
  Object.assign(form, {
    name: threat.name,
    type: threat.type,
    category: threat.category || '',
    description: threat.description || '',
    likelihood: threat.likelihood,
    impact: threat.impact,
    remark: threat.remark || ''
  })
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  editTarget.value = null
}

async function saveForm() {
  if (!form.name || !form.type) return
  saving.value = true
  try {
    const payload = {
      name: form.name,
      type: form.type,
      category: form.category || null,
      description: form.description || null,
      likelihood: form.likelihood,
      impact: form.impact,
      remark: form.remark || null
    }
    if (editTarget.value) {
      await threatApi.update(editTarget.value.id, payload)
    } else {
      await threatApi.create(payload)
    }
    closeModal()
    await loadList()
  } catch (e) {
    alert(e || '저장에 실패했습니다.')
  } finally {
    saving.value = false
  }
}

function confirmDelete(threat) {
  deleteTarget.value = threat
  showDeleteModal.value = true
}

async function deleteConfirmed() {
  deleting.value = true
  try {
    await threatApi.delete(deleteTarget.value.id)
    showDeleteModal.value = false
    deleteTarget.value = null
    await loadList()
  } catch (e) {
    alert(e || '삭제에 실패했습니다.')
  } finally {
    deleting.value = false
  }
}

async function handleLoadDefaults() {
  try {
    const checkRes = await threatApi.checkDefaults()
    const checkData = checkRes.data ?? checkRes
    if (checkData.alreadyLoaded) {
      alert('이미 모든 기본 항목이 로드되어 있습니다.')
      return
    }
    if (!confirm(`560개 기본 위협 항목을 추가하시겠습니까?\n(이미 존재하는 항목은 중복 추가되지 않습니다.)`)) return
    const loadRes = await threatApi.loadDefaults()
    const loadData = loadRes.data ?? loadRes
    const loaded = loadData.loaded ?? 0
    alert(`${loaded}개 항목이 추가되었습니다.`)
    await loadList()
  } catch (e) {
    alert(e || '기본 항목 추가에 실패했습니다.')
  }
}

async function handleReset() {
  if (!confirm('현재 등록된 모든 위협 항목이 삭제되고\n기본 위협항목(560개)으로 초기화됩니다.\n\n계속하시겠습니까?')) return
  try {
    const res = await threatApi.reset()
    const data = res.data ?? res
    alert(`초기화 완료: ${data.loaded}개 기본 항목이 로드되었습니다.`)
    await loadList()
  } catch (e) {
    alert(e || '초기화에 실패했습니다.')
  }
}

onMounted(async () => {
  try {
    const res = await codeApi.getValues('THREAT_TYPE')
    threatTypes.value = (res.data || []).map(t => t.value)
  } catch {
    threatTypes.value = ['외부공격', '내부위협', '기술적위협', '물리적위협', '인적위협', '자연재해', '기타']
  }
  loadList()
})
</script>
