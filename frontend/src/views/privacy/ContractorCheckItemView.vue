<template>
  <div class="p-6">
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">수탁사 점검항목 관리</h1>
        <p class="text-sm text-gray-500 mt-1">수탁사 점검에 사용할 점검항목 목록을 관리합니다</p>
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
          기본 점검항목 추가
        </button>
        <button @click="openCreate" class="btn-primary flex items-center gap-2">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          점검항목 추가
        </button>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center items-center py-20">
      <div class="w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
    </div>

    <!-- Empty -->
    <div v-else-if="items.length === 0" class="card flex flex-col items-center justify-center py-16 text-center">
      <svg class="w-12 h-12 text-gray-300 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"/>
      </svg>
      <p class="text-gray-500 font-medium">등록된 점검항목이 없습니다</p>
      <p class="text-gray-400 text-sm mt-1">"기본 점검항목 추가" 버튼으로 기본 항목을 불러오거나<br/>"점검항목 추가" 버튼으로 직접 추가할 수 있습니다.</p>
    </div>

    <!-- Table -->
    <div v-else class="card p-0 overflow-hidden">
      <div class="flex items-center justify-between px-5 py-2.5 border-b border-gray-100 bg-gray-50/70">
        <span class="text-sm text-gray-500">
          전체 <span class="font-semibold text-gray-800">{{ items.length }}</span>건 중
          <span class="font-semibold text-blue-600">{{ filteredItems.length }}</span>건 표시
        </span>
        <button v-if="filterName || filterCategory" @click="clearFilter"
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
            <tr class="bg-gray-50 border-b border-gray-100">
              <th class="text-left px-4 py-3 font-semibold text-gray-600 whitespace-nowrap w-8">#</th>
              <th class="text-left px-4 py-3 font-semibold text-gray-600 whitespace-nowrap">점검 분야</th>
              <th class="text-left px-4 py-3 font-semibold text-gray-600">점검항목명</th>
              <th class="text-left px-4 py-3 font-semibold text-gray-600">점검방법</th>
              <th class="text-center px-4 py-3 font-semibold text-gray-600 whitespace-nowrap">필수</th>
              <th class="text-center px-4 py-3 font-semibold text-gray-600 whitespace-nowrap">관리</th>
            </tr>
            <tr class="bg-blue-50/40 border-b border-blue-100">
              <th class="px-3 py-2"></th>
              <th class="px-3 py-2">
                <input v-model="filterCategory" type="text" placeholder="분야 검색"
                  class="w-full text-xs border border-gray-200 rounded px-2 py-1.5 font-normal focus:outline-none focus:border-blue-400 bg-white placeholder-gray-400"/>
              </th>
              <th class="px-3 py-2">
                <input v-model="filterName" type="text" placeholder="항목명 검색"
                  class="w-full text-xs border border-gray-200 rounded px-2 py-1.5 font-normal focus:outline-none focus:border-blue-400 bg-white placeholder-gray-400"/>
              </th>
              <th class="px-3 py-2"></th>
              <th class="px-3 py-2"></th>
              <th class="px-3 py-2 text-center">
                <button v-if="filterName || filterCategory" @click="clearFilter" title="필터 초기화"
                  class="inline-flex items-center justify-center w-6 h-6 rounded text-gray-400 hover:text-red-500 hover:bg-red-50 transition-colors">
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                  </svg>
                </button>
              </th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-50">
            <tr v-if="filteredItems.length === 0">
              <td colspan="6" class="text-center py-14 text-gray-400">
                <p class="text-sm">검색 조건에 맞는 항목이 없습니다</p>
                <button @click="clearFilter" class="mt-2 text-xs text-blue-500 hover:underline">필터 초기화</button>
              </td>
            </tr>
            <tr v-for="(item, idx) in filteredItems" :key="item.id" class="hover:bg-gray-50 transition-colors">
              <td class="px-4 py-3 text-gray-400 text-xs">{{ idx + 1 }}</td>
              <td class="px-4 py-3 whitespace-nowrap">
                <span class="px-2 py-0.5 rounded-full text-xs font-semibold" :class="categoryClass(item.category)">
                  {{ item.category }}
                </span>
              </td>
              <td class="px-4 py-3 text-gray-900 font-medium">{{ item.itemName }}</td>
              <td class="px-4 py-3 text-gray-500 text-xs max-w-xs truncate" :title="item.checkMethod">
                {{ item.checkMethod || '-' }}
              </td>
              <td class="px-4 py-3 text-center">
                <span v-if="item.required" class="text-xs font-semibold text-red-600">필수</span>
                <span v-else class="text-xs text-gray-400">선택</span>
              </td>
              <td class="px-4 py-3 text-center">
                <div class="flex items-center justify-center gap-2">
                  <button @click="openEdit(item)" class="text-blue-600 hover:text-blue-800 text-xs font-medium">수정</button>
                  <button @click="confirmDelete(item)" class="text-red-500 hover:text-red-700 text-xs font-medium">삭제</button>
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
          <h2 class="text-lg font-semibold text-gray-900">{{ editTarget ? '점검항목 수정' : '점검항목 추가' }}</h2>
          <button @click="closeModal" class="text-gray-400 hover:text-gray-600">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>

        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">점검 분야 <span class="text-red-500">*</span></label>
            <input v-model="form.category" class="input w-full" placeholder="예: 계약 관리, 기술적 보호조치"/>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">점검항목명 <span class="text-red-500">*</span></label>
            <input v-model="form.itemName" class="input w-full" placeholder="점검항목명을 입력하세요"/>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">점검방법</label>
            <textarea v-model="form.checkMethod" class="input w-full" rows="2" placeholder="점검방법을 입력하세요 (선택)"></textarea>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">점검기준</label>
            <textarea v-model="form.checkStandard" class="input w-full" rows="2" placeholder="관련 법령 또는 기준 (선택)"></textarea>
          </div>
          <div class="flex items-center gap-3">
            <label class="flex items-center gap-2 cursor-pointer">
              <input type="checkbox" v-model="form.required" class="w-4 h-4 rounded border-gray-300 text-blue-600"/>
              <span class="text-sm font-medium text-gray-700">필수 점검항목</span>
            </label>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">정렬 순서</label>
            <input v-model.number="form.sortOrder" type="number" class="input w-32" min="0"/>
          </div>
        </div>

        <div class="flex justify-end gap-3 mt-6">
          <button @click="closeModal" class="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50">취소</button>
          <button @click="saveForm" :disabled="saving || !form.category || !form.itemName" class="btn-primary disabled:opacity-50">
            {{ saving ? '저장 중...' : (editTarget ? '수정 완료' : '추가') }}
          </button>
        </div>
      </div>
    </div>

    <!-- Delete Confirm Modal -->
    <div v-if="showDeleteModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-sm p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-2">점검항목 삭제</h2>
        <p class="text-sm text-gray-600 mb-6">
          <span class="font-semibold text-gray-900">{{ deleteTarget?.itemName }}</span> 항목을 삭제하시겠습니까?<br/>
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
import { contractorCheckItemApi } from '@/api'

const items = ref([])
const loading = ref(false)
const saving = ref(false)
const deleting = ref(false)
const showModal = ref(false)
const showDeleteModal = ref(false)
const editTarget = ref(null)
const deleteTarget = ref(null)

const filterName = ref('')
const filterCategory = ref('')

const filteredItems = computed(() =>
  items.value.filter(i => {
    if (filterCategory.value && !i.category.toLowerCase().includes(filterCategory.value.toLowerCase())) return false
    if (filterName.value && !i.itemName.toLowerCase().includes(filterName.value.toLowerCase())) return false
    return true
  })
)

function clearFilter() {
  filterName.value = ''
  filterCategory.value = ''
}

const defaultForm = () => ({ category: '', itemName: '', checkMethod: '', checkStandard: '', required: true, sortOrder: 0 })
const form = reactive(defaultForm())

const categoryColors = [
  'bg-blue-50 text-blue-700', 'bg-purple-50 text-purple-700',
  'bg-green-50 text-green-700', 'bg-orange-50 text-orange-700',
  'bg-pink-50 text-pink-700', 'bg-teal-50 text-teal-700',
  'bg-yellow-50 text-yellow-700', 'bg-red-50 text-red-700',
]
const categoryColorMap = {}
function categoryClass(cat) {
  if (!categoryColorMap[cat]) {
    const idx = Object.keys(categoryColorMap).length % categoryColors.length
    categoryColorMap[cat] = categoryColors[idx]
  }
  return categoryColorMap[cat]
}

async function loadList() {
  loading.value = true
  try {
    const res = await contractorCheckItemApi.list()
    items.value = res.data ?? res
  } catch (e) {
    console.error('점검항목 로드 실패', e)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editTarget.value = null
  Object.assign(form, defaultForm())
  showModal.value = true
}

function openEdit(item) {
  editTarget.value = item
  Object.assign(form, {
    category: item.category,
    itemName: item.itemName,
    checkMethod: item.checkMethod || '',
    checkStandard: item.checkStandard || '',
    required: item.required,
    sortOrder: item.sortOrder
  })
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  editTarget.value = null
}

async function saveForm() {
  if (!form.category || !form.itemName) return
  saving.value = true
  try {
    const payload = { ...form }
    if (editTarget.value) {
      await contractorCheckItemApi.update(editTarget.value.id, payload)
    } else {
      await contractorCheckItemApi.create(payload)
    }
    closeModal()
    await loadList()
  } catch (e) {
    alert(e || '저장에 실패했습니다.')
  } finally {
    saving.value = false
  }
}

function confirmDelete(item) {
  deleteTarget.value = item
  showDeleteModal.value = true
}

async function deleteConfirmed() {
  deleting.value = true
  try {
    await contractorCheckItemApi.delete(deleteTarget.value.id)
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
    const checkRes = await contractorCheckItemApi.checkDefaults()
    const checkData = checkRes.data ?? checkRes
    if (checkData.alreadyLoaded) {
      alert('이미 모든 기본 항목이 로드되어 있습니다.')
      return
    }
    if (!confirm(`${checkData.defaultCount}개 기본 점검항목을 추가하시겠습니까?\n(이미 존재하는 항목은 중복 추가되지 않습니다.)`)) return
    const loadRes = await contractorCheckItemApi.loadDefaults()
    const loadData = loadRes.data ?? loadRes
    alert(`${loadData.loaded}개 항목이 추가되었습니다.`)
    await loadList()
  } catch (e) {
    alert(e || '기본 항목 추가에 실패했습니다.')
  }
}

async function handleReset() {
  if (!confirm('현재 등록된 모든 점검항목과 점검결과가 삭제되고\n기본 점검항목으로 초기화됩니다.\n\n계속하시겠습니까?')) return
  try {
    const res = await contractorCheckItemApi.reset()
    const data = res.data ?? res
    alert(`초기화 완료: ${data.loaded}개 기본 항목이 로드되었습니다.`)
    await loadList()
  } catch (e) {
    alert(e || '초기화에 실패했습니다.')
  }
}

onMounted(loadList)
</script>
