<template>
  <div class="p-8">
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold text-gray-900">{{ $t('training.title') }}</h1>
      <div class="flex items-center gap-2">
        <button v-if="isManager" @click="downloadPdf" :disabled="pdfLoading"
          class="btn-secondary flex items-center gap-2 text-sm">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
          </svg>
          {{ pdfLoading ? '...' : 'PDF' }}
        </button>
        <button v-if="isManager" @click="openCreate" class="btn-primary flex items-center gap-2">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          {{ $t('training.create') }}
        </button>
      </div>
    </div>

    <!-- 검색 · 항목 필터 -->
    <div class="card mb-6 flex flex-wrap items-center gap-3">
      <input v-model="keyword" type="text" :placeholder="`${$t('common.search')} (제목 · 설명)`"
        class="input flex-1 min-w-48" @input="debouncedSearch" />
      <select v-model="filters.mandatory" class="input w-36" @change="loadCourses">
        <option value="">이수구분: {{ $t('common.all') }}</option>
        <option value="true">{{ $t('training.mandatory') }}</option>
        <option value="false">{{ $t('training.optional') }}</option>
      </select>
      <select v-model="filters.contentType" class="input w-40" @change="loadCourses">
        <option value="">교육유형: {{ $t('common.all') }}</option>
        <option v-for="t in contentTypes" :key="t" :value="t">{{ $t(`training.contentType.${t}`) }}</option>
      </select>
      <select v-model="filters.completed" class="input w-36">
        <option value="">이수여부: {{ $t('common.all') }}</option>
        <option value="done">{{ $t('training.completed') }}</option>
        <option value="todo">미이수</option>
      </select>
      <button v-if="hasFilter" @click="resetFilters" class="btn-secondary text-sm px-3">초기화</button>
      <span class="text-sm text-gray-500">{{ filteredCourses.length }}개</span>
    </div>

    <div v-if="loading" class="text-center text-gray-400 py-16">{{ $t('common.loading') }}</div>
    <div v-else-if="!filteredCourses.length" class="text-center text-gray-400 py-16">{{ $t('common.noData') }}</div>
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div v-for="course in filteredCourses" :key="course.id"
        class="card hover:shadow-md transition-shadow cursor-pointer"
        @click="openDetail(course)">
        <div class="flex items-start justify-between mb-3">
          <div class="flex gap-2">
            <span :class="course.mandatory ? 'badge-red' : 'badge-gray'">
              {{ course.mandatory ? $t('training.mandatory') : $t('training.optional') }}
            </span>
            <span class="badge-blue">{{ $t(`training.contentType.${course.contentType}`) }}</span>
          </div>
          <span v-if="course.completedByMe" class="badge-green">✓ {{ $t('training.completed') }}</span>
        </div>
        <h3 class="font-semibold text-gray-900 mb-2">{{ course.title }}</h3>
        <div class="flex items-center justify-between text-sm text-gray-500 mt-4">
          <span>합격 기준: {{ course.passingScore }}점</span>
          <span v-if="course.completedByMe && course.myScore != null" class="font-medium text-green-600">
            {{ course.myScore }}점
          </span>
        </div>
      </div>
    </div>

    <!-- 교육 상세 모달 -->
    <TrainingDetailModal :open="showDetailModal" :item-id="detailId"
      @close="showDetailModal = false" @edit="onDetailEdit" @changed="loadCourses" />

    <!-- 교육 등록 모달 -->
    <TrainingFormModal :open="showFormModal" :edit-id="editId" @close="showFormModal = false" @saved="onFormSaved" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { trainingApi, exportApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useDebounceFn } from '@vueuse/core'
import TrainingFormModal from './TrainingFormModal.vue'
import TrainingDetailModal from './TrainingDetailModal.vue'

const auth = useAuthStore()
const isManager = auth.isManager

const showFormModal = ref(false)
const editId = ref(null)
function openCreate() { editId.value = null; showFormModal.value = true }
function onFormSaved() { showFormModal.value = false; loadCourses() }

const showDetailModal = ref(false)
const detailId = ref(null)
function openDetail(course) { detailId.value = course.id; showDetailModal.value = true }
function onDetailEdit(id) { showDetailModal.value = false; editId.value = id; showFormModal.value = true }

const courses = ref([])
const loading = ref(false)
const keyword = ref('')

const contentTypes = ['VIDEO', 'DOCUMENT', 'QUIZ_ONLY']
// 이수구분·교육유형은 서버 조회 조건, 이수여부는 사용자별 값이라 조회 결과에서 거른다.
const filters = ref({ mandatory: '', contentType: '', completed: '' })

const hasFilter = computed(() =>
  !!(keyword.value || filters.value.mandatory || filters.value.contentType || filters.value.completed))

const filteredCourses = computed(() => {
  if (filters.value.completed === 'done') return courses.value.filter(c => c.completedByMe)
  if (filters.value.completed === 'todo') return courses.value.filter(c => !c.completedByMe)
  return courses.value
})

function resetFilters() {
  keyword.value = ''
  filters.value = { mandatory: '', contentType: '', completed: '' }
  loadCourses()
}

async function loadCourses() {
  loading.value = true
  try {
    const params = { size: 50 }
    if (keyword.value) params.keyword = keyword.value
    if (filters.value.mandatory) params.mandatory = filters.value.mandatory
    if (filters.value.contentType) params.contentType = filters.value.contentType
    const res = await trainingApi.list(params)
    courses.value = res.data?.content || []
  } finally {
    loading.value = false
  }
}

const debouncedSearch = useDebounceFn(loadCourses, 400)
onMounted(loadCourses)

const pdfLoading = ref(false)
async function downloadPdf() {
  pdfLoading.value = true
  try { await exportApi.trainingPdf() } finally { pdfLoading.value = false }
}
</script>
