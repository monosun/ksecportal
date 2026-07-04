<template>
  <div class="p-6 max-w-4xl mx-auto">
    <!-- Header -->
    <div class="flex items-center gap-3 mb-6">
      <button @click="$router.push(`/isms?year=${selectedYear}`)"
        class="text-gray-500 hover:text-gray-700 transition-colors">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
        </svg>
      </button>
      <h1 class="text-xl font-bold text-gray-900">{{ $t('isms.title') }}</h1>
    </div>

    <!-- Item Card -->
    <div v-if="item" class="bg-white rounded-xl shadow-sm border p-6 mb-6">
      <div class="flex items-start justify-between">
        <div class="flex-1">
          <div class="flex items-center gap-2 mb-2">
            <span class="font-mono text-sm font-bold text-primary-700 bg-primary-50 px-2 py-0.5 rounded">
              {{ item.itemCode }}
            </span>
            <span class="text-sm text-gray-500">{{ item.sectionName }} › {{ item.domainName }}</span>
          </div>
          <h2 class="text-lg font-bold text-gray-900 mb-3">{{ item.itemName }}</h2>
          <p class="text-sm text-gray-600 leading-relaxed">{{ item.description }}</p>
        </div>
        <select v-model="selectedYear" @change="loadEvidences"
          class="ml-4 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500">
          <option v-for="y in years" :key="y" :value="y">{{ y }}{{ $t('isms.year') }}</option>
        </select>
      </div>
    </div>

    <!-- Save feedback -->
    <div v-if="saveSuccess"
      class="mb-4 flex items-center gap-2 px-4 py-3 bg-green-50 border border-green-200 rounded-lg text-sm text-green-700">
      <svg class="w-4 h-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
      </svg>
      {{ saveSuccess }}
    </div>
    <div v-if="saveError"
      class="mb-4 flex items-center gap-2 px-4 py-3 bg-red-50 border border-red-200 rounded-lg text-sm text-red-700">
      <svg class="w-4 h-4 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
      </svg>
      {{ saveError }}
    </div>

    <!-- Evidence Section -->
    <div class="bg-white rounded-xl shadow-sm border">
      <div class="flex items-center justify-between p-6 border-b">
        <h3 class="font-semibold text-gray-900">
          {{ selectedYear }}{{ $t('isms.year') }} {{ $t('isms.evidences') }}
          <span class="ml-2 text-sm font-normal text-gray-500">({{ evidences.length }}건)</span>
        </h3>
        <button v-if="!showForm" @click="openForm('direct')"
          class="inline-flex items-center gap-2 px-4 py-2 bg-primary-600 text-white text-sm font-medium rounded-lg hover:bg-primary-700 transition-colors">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          {{ $t('isms.addEvidence') }}
        </button>
      </div>

      <!-- Add Evidence Form -->
      <div v-if="showForm" class="bg-gray-50 border-b">
        <!-- Mode tabs -->
        <div class="flex border-b border-gray-200 px-6 pt-4">
          <button @click="switchFormMode('direct')"
            :class="formMode === 'direct' ? 'border-b-2 border-primary-600 text-primary-600 font-medium' : 'text-gray-500 hover:text-gray-700'"
            class="px-4 py-2 text-sm mr-2 transition-colors">
            직접 등록
          </button>
          <button @click="switchFormMode('ref')"
            :class="formMode === 'ref' ? 'border-b-2 border-primary-600 text-primary-600 font-medium' : 'text-gray-500 hover:text-gray-700'"
            class="px-4 py-2 text-sm transition-colors">
            다른 항목 참조
          </button>
        </div>

        <!-- Direct register form -->
        <div v-if="formMode === 'direct'" class="p-6">
          <div class="grid grid-cols-1 gap-4">
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('isms.evidenceTitle') }} *</label>
                <input v-model="form.title" type="text"
                  class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                  :placeholder="$t('isms.evidenceTitlePlaceholder')" />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('common.status') }} *</label>
                <select v-model="form.status"
                  class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500">
                  <option value="COMPLIANT">{{ $t('isms.statusCompliant') }}</option>
                  <option value="PARTIAL">{{ $t('isms.statusPartial') }}</option>
                  <option value="NON_COMPLIANT">{{ $t('isms.statusNonCompliant') }}</option>
                  <option value="NA">{{ $t('isms.statusNa') }}</option>
                </select>
              </div>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('isms.evidenceContent') }}</label>
              <textarea v-model="form.content" rows="4"
                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none"
                :placeholder="$t('isms.evidenceContentPlaceholder')"></textarea>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('isms.evidenceFile') }}</label>
              <input type="file" @change="e => form.file = e.target.files[0]" ref="createFileInput"
                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 file:mr-3 file:py-1 file:px-3 file:rounded file:border-0 file:text-xs file:font-medium file:bg-primary-50 file:text-primary-700 hover:file:bg-primary-100" />
              <p v-if="form.file" class="mt-1 text-xs text-gray-500">{{ form.file.name }}</p>
            </div>
            <div class="flex justify-end gap-2">
              <button @click="resetForm"
                class="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors">
                {{ $t('common.cancel') }}
              </button>
              <button @click="submitEvidence" :disabled="!form.title || submitting"
                class="px-4 py-2 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 transition-colors">
                {{ $t('common.save') }}
              </button>
            </div>
          </div>
        </div>

        <!-- Reference register form -->
        <div v-if="formMode === 'ref'" class="p-6">
          <div class="grid grid-cols-1 gap-4">
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">제목 *</label>
                <input v-model="refForm.title" type="text"
                  class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                  placeholder="이 항목에서 참조 증적의 제목" />
              </div>
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('common.status') }} *</label>
                <select v-model="refForm.status"
                  class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500">
                  <option value="COMPLIANT">{{ $t('isms.statusCompliant') }}</option>
                  <option value="PARTIAL">{{ $t('isms.statusPartial') }}</option>
                  <option value="NON_COMPLIANT">{{ $t('isms.statusNonCompliant') }}</option>
                  <option value="NA">{{ $t('isms.statusNa') }}</option>
                </select>
              </div>
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">참조할 증적 검색 *</label>
              <p class="text-xs text-gray-400 mb-2">다른 항목에 등록된 파일이 있는 증적을 검색하여 참조로 등록합니다.</p>
              <div class="flex gap-2">
                <input v-model="refSearch"
                  @keyup.enter="doRefSearch"
                  class="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
                  placeholder="항목코드, 증적제목, 파일명으로 검색..." />
                <button @click="doRefSearch" :disabled="refSearching"
                  class="px-4 py-2 text-sm bg-gray-600 text-white rounded-lg hover:bg-gray-700 disabled:opacity-50 transition-colors whitespace-nowrap">
                  검색
                </button>
              </div>

              <!-- Selected reference -->
              <div v-if="selectedRef" class="mt-3 p-3 bg-blue-50 rounded-lg border border-blue-200 flex items-center justify-between">
                <div>
                  <div class="flex items-center gap-2">
                    <span class="font-mono text-xs font-bold text-primary-700 bg-primary-100 px-2 py-0.5 rounded">{{ selectedRef.itemCode }}</span>
                    <span class="text-sm font-medium text-gray-900">{{ selectedRef.title }}</span>
                  </div>
                  <div class="mt-0.5 text-xs text-gray-500">{{ selectedRef.itemName }} | 📎 {{ selectedRef.fileName }}</div>
                </div>
                <button @click="selectedRef = null; refResults = []"
                  class="ml-3 text-gray-400 hover:text-red-500 transition-colors text-lg leading-none">✕</button>
              </div>

              <!-- Search results -->
              <div v-if="!selectedRef && refResults.length > 0" class="mt-2 border border-gray-200 rounded-lg max-h-52 overflow-y-auto divide-y divide-gray-100">
                <div v-for="r in refResults" :key="r.id" @click="selectRef(r)"
                  class="p-3 hover:bg-primary-50 cursor-pointer transition-colors">
                  <div class="flex items-center gap-2">
                    <span class="font-mono text-xs font-bold text-primary-700 bg-primary-50 px-2 py-0.5 rounded">{{ r.itemCode }}</span>
                    <span class="text-sm font-medium text-gray-900">{{ r.title }}</span>
                    <StatusBadge :status="r.status" class="ml-auto" />
                  </div>
                  <div class="mt-0.5 text-xs text-gray-500">{{ r.itemName }} | 📎 {{ r.fileName }}</div>
                </div>
              </div>
              <p v-if="!selectedRef && refSearched && refResults.length === 0"
                class="mt-2 text-xs text-gray-400 text-center py-3">검색 결과가 없습니다.</p>
            </div>

            <div class="flex justify-end gap-2">
              <button @click="resetForm"
                class="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors">
                {{ $t('common.cancel') }}
              </button>
              <button @click="submitRef" :disabled="!selectedRef || !refForm.title || submitting"
                class="px-4 py-2 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 transition-colors">
                참조 등록
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Evidence List -->
      <div v-if="loadingEvidences" class="py-8 text-center text-gray-500 text-sm">{{ $t('common.loading') }}</div>
      <div v-else-if="evidences.length === 0" class="py-12 text-center text-gray-400">
        <svg class="w-12 h-12 mx-auto mb-3 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
            d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
        </svg>
        <p class="text-sm">{{ $t('isms.noEvidenceForYear', { year: selectedYear }) }}</p>
      </div>
      <div v-else class="divide-y divide-gray-100">
        <div v-for="ev in evidences" :key="ev.id" class="p-6">
          <div v-if="editingId !== ev.id">
            <div class="flex items-start justify-between">
              <div class="flex-1">
                <!-- Reference badge -->
                <div v-if="ev.sourceEvidenceId"
                  class="inline-flex items-center gap-1 text-xs font-medium text-blue-600 bg-blue-50 border border-blue-100 px-2 py-0.5 rounded mb-2">
                  🔗 참조
                </div>
                <div class="flex items-center gap-2 mb-2">
                  <StatusBadge :status="ev.status" />
                  <span class="text-sm font-semibold text-gray-900">{{ ev.title }}</span>
                </div>
                <!-- Content (only for direct evidences) -->
                <p v-if="ev.content && !ev.sourceEvidenceId"
                  class="text-sm text-gray-600 leading-relaxed whitespace-pre-line mb-2">{{ ev.content }}</p>
                <!-- Source info for references -->
                <div v-if="ev.sourceEvidenceId" class="text-xs text-gray-500 mb-2">
                  <span class="font-mono font-bold text-primary-700 bg-primary-50 px-1.5 py-0.5 rounded mr-1">{{ ev.sourceItemCode }}</span>
                  {{ ev.sourceEvidenceTitle }}
                </div>
                <div class="flex items-center gap-4 text-xs text-gray-400">
                  <span v-if="ev.filePath" class="flex items-center gap-1">
                    📎
                    <button @click="downloadFile(ev)" class="underline hover:text-primary-600 transition-colors">
                      {{ ev.fileName }}
                    </button>
                    <!-- Remove file only for non-reference evidences -->
                    <button v-if="!ev.sourceEvidenceId" @click="removeFile(ev)" title="첨부 파일 삭제"
                      class="text-gray-300 hover:text-red-500 transition-colors ml-1">✕</button>
                  </span>
                  <span>{{ ev.registrantName }}</span>
                  <span>{{ formatDate(ev.createdAt) }}</span>
                </div>
              </div>
              <div class="flex items-center gap-2 ml-4 flex-shrink-0">
                <button @click="startEdit(ev)"
                  class="text-xs text-gray-500 hover:text-primary-600 px-2 py-1 rounded hover:bg-gray-100 transition-colors">
                  {{ $t('common.edit') }}
                </button>
                <button @click="deleteEvidence(ev.id)"
                  class="text-xs text-gray-500 hover:text-red-600 px-2 py-1 rounded hover:bg-gray-100 transition-colors">
                  {{ $t('common.delete') }}
                </button>
              </div>
            </div>
          </div>
          <div v-else>
            <div class="grid grid-cols-1 gap-3">
              <div class="grid grid-cols-2 gap-3">
                <input v-model="editForm.title" type="text"
                  class="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500" />
                <select v-model="editForm.status"
                  class="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500">
                  <option value="COMPLIANT">{{ $t('isms.statusCompliant') }}</option>
                  <option value="PARTIAL">{{ $t('isms.statusPartial') }}</option>
                  <option value="NON_COMPLIANT">{{ $t('isms.statusNonCompliant') }}</option>
                  <option value="NA">{{ $t('isms.statusNa') }}</option>
                </select>
              </div>
              <!-- Content and file only for direct evidences -->
              <template v-if="!editingIsRef">
                <textarea v-model="editForm.content" rows="3"
                  class="border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 resize-none"></textarea>
                <div>
                  <input type="file" @change="e => editForm.file = e.target.files[0]"
                    class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 file:mr-3 file:py-1 file:px-3 file:rounded file:border-0 file:text-xs file:font-medium file:bg-primary-50 file:text-primary-700 hover:file:bg-primary-100" />
                  <p v-if="editForm.file" class="mt-1 text-xs text-gray-500">{{ editForm.file.name }}</p>
                </div>
              </template>
              <div class="flex justify-end gap-2">
                <button @click="cancelEdit"
                  class="px-3 py-1.5 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50">
                  {{ $t('common.cancel') }}
                </button>
                <button @click="saveEdit(ev.id)" :disabled="submitting"
                  class="px-3 py-1.5 text-sm bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50">
                  {{ $t('common.save') }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ismsApi } from '@/api'
import StatusBadge from './IsmsStatusBadge.vue'

const route = useRoute()
const itemId = route.params.id

const item = ref(null)
const evidences = ref([])
const selectedYear = ref(parseInt(route.query.year) || new Date().getFullYear())
const showForm = ref(false)
const formMode = ref('direct')
const loading = ref(false)
const loadingEvidences = ref(false)
const submitting = ref(false)
const editingId = ref(null)
const editingIsRef = ref(false)
const createFileInput = ref(null)

const form = ref({ title: '', content: '', status: 'COMPLIANT', file: null })
const editForm = ref({ title: '', content: '', status: 'COMPLIANT', file: null })

const refForm = ref({ title: '', status: 'COMPLIANT' })
const refSearch = ref('')
const refResults = ref([])
const refSearching = ref(false)
const refSearched = ref(false)
const selectedRef = ref(null)

const saveSuccess = ref('')
const saveError = ref('')

function showSaveSuccess(msg) {
  saveSuccess.value = msg
  saveError.value = ''
  setTimeout(() => { saveSuccess.value = '' }, 3000)
}
function showSaveError(msg) {
  saveError.value = msg || '저장 중 오류가 발생했습니다.'
  saveSuccess.value = ''
  setTimeout(() => { saveError.value = '' }, 4000)
}

const years = computed(() => {
  const cur = new Date().getFullYear()
  return Array.from({ length: 5 }, (_, i) => cur - i)
})

function openForm(mode) {
  formMode.value = mode
  showForm.value = true
}

function switchFormMode(mode) {
  formMode.value = mode
  resetFormState()
}

function resetFormState() {
  form.value = { title: '', content: '', status: 'COMPLIANT', file: null }
  if (createFileInput.value) createFileInput.value.value = ''
  refForm.value = { title: '', status: 'COMPLIANT' }
  refSearch.value = ''
  refResults.value = []
  refSearched.value = false
  selectedRef.value = null
}

async function loadItem() {
  const res = await ismsApi.getItem(itemId)
  item.value = res.data
}

async function loadEvidences() {
  loadingEvidences.value = true
  try {
    const res = await ismsApi.listEvidences(itemId, selectedYear.value)
    evidences.value = res.data
  } finally {
    loadingEvidences.value = false
  }
}

async function submitEvidence() {
  submitting.value = true
  try {
    await ismsApi.createEvidence(itemId, { ...form.value, year: selectedYear.value })
    resetForm()
    await loadEvidences()
    showSaveSuccess('증적이 저장되었습니다.')
  } catch (e) {
    showSaveError(e || '증적 저장에 실패했습니다.')
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  resetFormState()
  showForm.value = false
}

function startEdit(ev) {
  editingId.value = ev.id
  editingIsRef.value = !!ev.sourceEvidenceId
  editForm.value = { title: ev.title, content: ev.content || '', status: ev.status, file: null }
}

function cancelEdit() {
  editingId.value = null
  editingIsRef.value = false
}

async function saveEdit(id) {
  submitting.value = true
  try {
    await ismsApi.updateEvidence(id, editForm.value)
    editingId.value = null
    editingIsRef.value = false
    await loadEvidences()
    showSaveSuccess('증적이 수정되었습니다.')
  } catch (e) {
    showSaveError(e || '수정 중 오류가 발생했습니다.')
  } finally {
    submitting.value = false
  }
}

async function deleteEvidence(id) {
  if (!confirm('이 증적을 삭제하시겠습니까?')) return
  await ismsApi.deleteEvidence(id)
  await loadEvidences()
}

async function downloadFile(ev) {
  try {
    await ismsApi.downloadFile(ev.id, ev.fileName)
  } catch {
    showSaveError('파일을 다운로드할 수 없습니다. 파일이 서버에 존재하지 않을 수 있습니다.')
  }
}

async function removeFile(ev) {
  if (!confirm('첨부 파일을 삭제하시겠습니까?')) return
  const res = await ismsApi.removeFile(ev.id)
  const idx = evidences.value.findIndex(e => e.id === ev.id)
  if (idx !== -1) evidences.value[idx] = res.data
}

async function doRefSearch() {
  if (!refSearch.value.trim()) return
  refSearching.value = true
  refSearched.value = false
  selectedRef.value = null
  try {
    const res = await ismsApi.searchEvidences({
      excludeItemId: itemId,
      year: selectedYear.value,
      keyword: refSearch.value.trim()
    })
    refResults.value = res.data || []
    refSearched.value = true
  } catch (e) {
    showSaveError(e || '검색 중 오류가 발생했습니다.')
  } finally {
    refSearching.value = false
  }
}

function selectRef(r) {
  selectedRef.value = r
  refResults.value = []
  if (!refForm.value.title) refForm.value.title = r.title
}

async function submitRef() {
  if (!selectedRef.value || !refForm.value.title) return
  submitting.value = true
  try {
    await ismsApi.createEvidenceRef(itemId, {
      year: selectedYear.value,
      title: refForm.value.title,
      status: refForm.value.status,
      sourceEvidenceId: selectedRef.value.id
    })
    resetForm()
    await loadEvidences()
    showSaveSuccess('참조 증적이 등록되었습니다.')
  } catch (e) {
    showSaveError(e || '참조 등록에 실패했습니다.')
  } finally {
    submitting.value = false
  }
}

function formatDate(dt) {
  if (!dt) return ''
  return new Date(dt).toLocaleDateString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

onMounted(async () => {
  await Promise.all([loadItem(), loadEvidences()])
})
</script>
