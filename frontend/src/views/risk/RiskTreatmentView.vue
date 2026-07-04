<template>
  <div class="p-6">
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">위험 처리 계획</h1>
        <p class="text-sm text-gray-500 mt-1">위험 항목별 처리 계획과 진행 상황을 관리합니다</p>
      </div>
      <button @click="openCreate" class="btn-primary flex items-center gap-2">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
        </svg>
        처리계획 추가
      </button>
    </div>

    <!-- Tabs -->
    <div class="flex gap-1 mb-5 bg-gray-100 p-1 rounded-xl w-fit">
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

    <!-- Table -->
    <div class="card p-0 overflow-hidden">
      <table class="w-full text-sm">
        <thead class="bg-gray-50 border-b border-gray-100">
          <tr>
            <th class="text-left px-5 py-3 font-semibold text-gray-600">위험 항목</th>
            <th class="text-left px-5 py-3 font-semibold text-gray-600">처리 유형</th>
            <th class="text-left px-5 py-3 font-semibold text-gray-600">조치 계획</th>
            <th class="text-left px-5 py-3 font-semibold text-gray-600">담당자</th>
            <th class="text-left px-5 py-3 font-semibold text-gray-600">완료 기한</th>
            <th class="text-left px-5 py-3 font-semibold text-gray-600 w-48">진행률</th>
            <th class="text-center px-5 py-3 font-semibold text-gray-600">상태</th>
            <th class="text-center px-5 py-3 font-semibold text-gray-600">관리</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-50">
          <tr v-if="filteredItems.length === 0">
            <td colspan="8" class="px-5 py-10 text-center text-gray-400">해당 상태의 처리 계획이 없습니다</td>
          </tr>
          <tr v-for="item in filteredItems" :key="item.id" class="hover:bg-gray-50 transition-colors">
            <td class="px-5 py-3.5 font-medium text-gray-900 max-w-[160px] truncate" :title="item.riskItem">{{ item.riskItem }}</td>
            <td class="px-5 py-3.5">
              <span class="px-2 py-0.5 rounded text-xs font-medium" :class="treatmentBadge(item.treatmentType)">{{ item.treatmentType }}</span>
            </td>
            <td class="px-5 py-3.5 text-gray-600 max-w-[200px] truncate" :title="item.plan">{{ item.plan }}</td>
            <td class="px-5 py-3.5 text-gray-700">{{ item.assignee }}</td>
            <td class="px-5 py-3.5 text-gray-500">{{ item.dueDate }}</td>
            <td class="px-5 py-3.5">
              <div class="flex items-center gap-2">
                <div class="flex-1 bg-gray-100 rounded-full h-2">
                  <div class="h-2 rounded-full transition-all duration-500"
                    :class="progressBarColor(item.progress)"
                    :style="{ width: item.progress + '%' }">
                  </div>
                </div>
                <span class="text-xs font-semibold text-gray-600 w-8 text-right">{{ item.progress }}%</span>
              </div>
            </td>
            <td class="px-5 py-3.5 text-center">
              <span class="px-2.5 py-1 rounded-full text-xs font-bold" :class="statusBadge(item.status)">
                {{ item.status }}
              </span>
            </td>
            <td class="px-5 py-3.5 text-center">
              <div class="flex items-center justify-center gap-2">
                <button @click="openEdit(item)" class="text-blue-600 hover:text-blue-800 text-xs font-medium">수정</button>
                <button @click="confirmDelete(item)" class="text-red-500 hover:text-red-700 text-xs font-medium">삭제</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Add/Edit Modal -->
    <div v-if="showModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-xl p-6">
        <div class="flex items-center justify-between mb-5">
          <h2 class="text-lg font-semibold text-gray-900">{{ editTarget ? '처리계획 수정' : '처리계획 추가' }}</h2>
          <button @click="closeModal" class="text-gray-400 hover:text-gray-600">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>

        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">위험 항목 <span class="text-red-500">*</span></label>
            <input v-model="form.riskItem" class="input w-full" placeholder="위험 항목을 입력하세요"/>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">처리 유형</label>
              <select v-model="form.treatmentType" class="input w-full">
                <option v-for="t in treatmentTypes" :key="t" :value="t">{{ t }}</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">담당자</label>
              <input v-model="form.assignee" class="input w-full" placeholder="담당자 이름"/>
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">조치 계획 <span class="text-red-500">*</span></label>
            <textarea v-model="form.plan" class="input w-full" rows="3" placeholder="구체적인 조치 계획을 입력하세요"></textarea>
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">완료 기한</label>
              <input v-model="form.dueDate" type="date" class="input w-full"/>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">상태</label>
              <select v-model="form.status" class="input w-full">
                <option v-for="s in statusOptions" :key="s" :value="s">{{ s }}</option>
              </select>
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">진행률 ({{ form.progress }}%)</label>
            <input type="range" v-model.number="form.progress" min="0" max="100" step="5" class="w-full accent-blue-600"/>
            <div class="flex justify-between text-xs text-gray-400 mt-1">
              <span>0%</span><span>50%</span><span>100%</span>
            </div>
          </div>
        </div>

        <div class="flex justify-end gap-3 mt-6">
          <button @click="closeModal" class="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50">취소</button>
          <button @click="saveForm" :disabled="!form.riskItem || !form.plan" class="btn-primary disabled:opacity-50">
            {{ editTarget ? '수정 완료' : '추가' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Delete Confirm Modal -->
    <div v-if="showDeleteModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-sm p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-2">처리계획 삭제</h2>
        <p class="text-sm text-gray-600 mb-6">선택한 처리계획을 삭제하시겠습니까?</p>
        <div class="flex justify-end gap-3">
          <button @click="showDeleteModal = false" class="px-4 py-2 text-sm border border-gray-300 rounded-lg hover:bg-gray-50">취소</button>
          <button @click="deleteConfirmed" class="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700">삭제</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'

const tabs = [
  { key: 'ALL', label: '전체' },
  { key: '진행중', label: '진행중' },
  { key: '완료', label: '완료' },
  { key: '기한초과', label: '기한초과' }
]

const treatmentTypes = ['경감', '수용', '회피', '이전']
const statusOptions = ['계획중', '진행중', '완료', '기한초과']

const items = ref([
  { id: 1, riskItem: '웹 서버 SQL 인젝션 취약점', treatmentType: '경감', plan: 'WAF 솔루션 도입 및 입력값 검증 로직 강화', assignee: '김보안', dueDate: '2026-07-31', progress: 60, status: '진행중' },
  { id: 2, riskItem: 'DB 서버 랜섬웨어 감염 위협', treatmentType: '경감', plan: '오프라인 백업 구성 및 EDR 솔루션 적용', assignee: '이시스템', dueDate: '2026-06-30', progress: 100, status: '완료' },
  { id: 3, riskItem: '내부자 정보유출 위협', treatmentType: '회피', plan: 'DLP 솔루션 도입 및 퇴직자 즉시 접근 차단 프로세스 수립', assignee: '박정책', dueDate: '2026-05-31', progress: 30, status: '기한초과' }
])

const activeTab = ref('ALL')
const showModal = ref(false)
const showDeleteModal = ref(false)
const editTarget = ref(null)
const deleteTarget = ref(null)
let nextId = 4

const defaultForm = () => ({ riskItem: '', treatmentType: '경감', plan: '', assignee: '', dueDate: '', progress: 0, status: '계획중' })
const form = reactive(defaultForm())

const filteredItems = computed(() => {
  if (activeTab.value === 'ALL') return items.value
  return items.value.filter(i => i.status === activeTab.value)
})

function tabCount(key) {
  if (key === 'ALL') return items.value.length
  return items.value.filter(i => i.status === key).length
}

function treatmentBadge(type) {
  return { '경감': 'bg-blue-50 text-blue-700', '수용': 'bg-gray-100 text-gray-700', '회피': 'bg-purple-50 text-purple-700', '이전': 'bg-orange-50 text-orange-700' }[type] || 'bg-gray-100 text-gray-600'
}

function statusBadge(status) {
  return { '계획중': 'bg-gray-100 text-gray-600', '진행중': 'bg-blue-100 text-blue-700', '완료': 'bg-green-100 text-green-700', '기한초과': 'bg-red-100 text-red-700' }[status] || 'bg-gray-100 text-gray-600'
}

function progressBarColor(progress) {
  if (progress >= 100) return 'bg-green-500'
  if (progress >= 50) return 'bg-blue-500'
  return 'bg-yellow-500'
}

function openCreate() {
  editTarget.value = null
  Object.assign(form, defaultForm())
  showModal.value = true
}

function openEdit(item) {
  editTarget.value = item
  Object.assign(form, { riskItem: item.riskItem, treatmentType: item.treatmentType, plan: item.plan, assignee: item.assignee, dueDate: item.dueDate, progress: item.progress, status: item.status })
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  editTarget.value = null
}

function saveForm() {
  if (editTarget.value) {
    const idx = items.value.findIndex(i => i.id === editTarget.value.id)
    if (idx !== -1) items.value[idx] = { ...items.value[idx], ...form }
  } else {
    items.value.push({ id: nextId++, ...form })
  }
  closeModal()
}

function confirmDelete(item) {
  deleteTarget.value = item
  showDeleteModal.value = true
}

function deleteConfirmed() {
  items.value = items.value.filter(i => i.id !== deleteTarget.value.id)
  showDeleteModal.value = false
  deleteTarget.value = null
}

onMounted(() => {})
</script>
