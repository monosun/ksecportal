<template>
  <div class="p-6">
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('committee.title') }}</h1>
        <p class="text-sm text-gray-500 mt-1">{{ $t('committee.subtitle') }}</p>
      </div>
      <button v-if="isManager" @click="openCreateModal" class="btn-primary flex items-center gap-2">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
        </svg>
        {{ $t('committee.addMeeting') }}
      </button>
    </div>

    <!-- Year Tabs -->
    <div class="flex gap-2 mb-5 flex-wrap">
      <button
        v-for="y in yearOptions" :key="y"
        @click="selectedYear = y"
        :class="['px-4 py-1.5 rounded-full text-sm font-medium transition-colors',
          selectedYear === y ? 'bg-primary-600 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200']">
        {{ y }}년
      </button>
      <button @click="openYearPicker" class="px-4 py-1.5 rounded-full text-sm font-medium bg-gray-100 text-gray-500 hover:bg-gray-200">
        + 연도 선택
      </button>
    </div>

    <!-- Meeting List -->
    <div v-if="loading" class="text-center py-16 text-gray-400">{{ $t('common.loading') }}</div>
    <div v-else-if="meetings.length === 0" class="card text-center py-16 text-gray-400">
      {{ $t('committee.noMeetings') }}
    </div>
    <div v-else class="space-y-4">
      <div v-for="meeting in meetings" :key="meeting.id"
        class="card hover:shadow-md transition-shadow cursor-pointer"
        @click="openDetail(meeting)">
        <div class="flex items-start justify-between">
          <div class="flex items-start gap-4">
            <!-- Session badge -->
            <div class="flex-shrink-0 w-14 h-14 rounded-xl bg-primary-50 flex flex-col items-center justify-center">
              <span class="text-[10px] text-primary-500 font-semibold">제</span>
              <span class="text-xl font-bold text-primary-600 leading-none">{{ meeting.sessionNo }}</span>
              <span class="text-[10px] text-primary-500 font-semibold">회</span>
            </div>
            <div>
              <h3 class="font-semibold text-gray-900 text-base">{{ meeting.title }}</h3>
              <div class="flex flex-wrap gap-3 mt-1.5 text-sm text-gray-500">
                <span v-if="meeting.meetingDate">📅 {{ formatDate(meeting.meetingDate) }}</span>
                <span v-if="meeting.location">📍 {{ meeting.location }}</span>
                <span v-if="meeting.attendees">👥 {{ truncate(meeting.attendees, 40) }}</span>
              </div>
              <div class="flex gap-2 mt-2">
                <span :class="statusBadge(meeting.status)" class="text-xs px-2 py-0.5 rounded font-medium">
                  {{ $t(`committee.status_label.${meeting.status}`) }}
                </span>
                <span v-if="meeting.files && meeting.files.length > 0"
                  class="text-xs px-2 py-0.5 rounded font-medium bg-gray-100 text-gray-600">
                  첨부 {{ meeting.files.length }}건
                </span>
              </div>
            </div>
          </div>
          <div v-if="isManager" class="flex gap-1 flex-shrink-0" @click.stop>
            <button @click="openEditModal(meeting)" class="p-1.5 rounded hover:bg-gray-100 text-gray-400 hover:text-gray-700">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
              </svg>
            </button>
            <button @click="confirmDelete(meeting)" class="p-1.5 rounded hover:bg-red-50 text-gray-400 hover:text-red-500">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Meeting Form Modal -->
    <div v-if="showFormModal" class="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-lg">
        <div class="px-6 py-4 border-b">
          <h2 class="text-lg font-semibold">{{ editTarget ? $t('committee.editMeeting') : $t('committee.addMeeting') }}</h2>
        </div>
        <div class="px-6 py-5 space-y-4">
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('committee.year') }} *</label>
              <input v-model.number="form.year" type="number" class="input w-full" placeholder="2025" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('committee.sessionNo') }} *</label>
              <input v-model.number="form.sessionNo" type="number" class="input w-full" placeholder="1" min="1" />
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('common.title') }} *</label>
            <input v-model="form.title" class="input w-full" placeholder="예: 2025년 제1회 정보보호위원회" />
          </div>
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('committee.meetingDate') }}</label>
              <input v-model="form.meetingDate" type="date" class="input w-full" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('committee.status') }}</label>
              <select v-model="form.status" class="input w-full">
                <option v-for="s in STATUSES" :key="s" :value="s">{{ $t(`committee.status_label.${s}`) }}</option>
              </select>
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('committee.location') }}</label>
            <input v-model="form.location" class="input w-full" placeholder="예: 본사 대회의실" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('committee.attendees') }}</label>
            <textarea v-model="form.attendees" rows="2" class="input w-full resize-none"
              placeholder="예: CISO, 정보보안팀장, 각 본부장" />
          </div>
        </div>
        <div class="px-6 py-4 border-t flex justify-end gap-2">
          <button @click="showFormModal = false" class="btn-secondary">{{ $t('common.cancel') }}</button>
          <button @click="saveMeeting" :disabled="saving" class="btn-primary">
            {{ saving ? $t('common.loading') : $t('common.save') }}
          </button>
        </div>
      </div>
    </div>

    <!-- Detail Modal -->
    <div v-if="showDetailModal && selectedMeeting" class="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-2xl max-h-[90vh] flex flex-col">
        <div class="px-6 py-4 border-b flex items-center justify-between">
          <div>
            <div class="text-xs text-primary-600 font-semibold mb-0.5">
              {{ selectedMeeting.year }}년 제{{ selectedMeeting.sessionNo }}회
            </div>
            <h2 class="text-lg font-semibold">{{ selectedMeeting.title }}</h2>
          </div>
          <button @click="showDetailModal = false" class="text-gray-400 hover:text-gray-600">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <div class="overflow-y-auto flex-1 px-6 py-5 space-y-5">
          <!-- Info -->
          <div class="grid grid-cols-2 gap-3 text-sm">
            <div>
              <span class="text-gray-500">개최일</span>
              <p class="font-medium mt-0.5">{{ selectedMeeting.meetingDate ? formatDate(selectedMeeting.meetingDate) : '-' }}</p>
            </div>
            <div>
              <span class="text-gray-500">상태</span>
              <p class="mt-0.5">
                <span :class="statusBadge(selectedMeeting.status)" class="text-xs px-2 py-0.5 rounded font-medium">
                  {{ $t(`committee.status_label.${selectedMeeting.status}`) }}
                </span>
              </p>
            </div>
            <div>
              <span class="text-gray-500">개최 장소</span>
              <p class="font-medium mt-0.5">{{ selectedMeeting.location || '-' }}</p>
            </div>
            <div>
              <span class="text-gray-500">참석자</span>
              <p class="font-medium mt-0.5">{{ selectedMeeting.attendees || '-' }}</p>
            </div>
          </div>

          <!-- Files -->
          <div>
            <div class="flex items-center justify-between mb-2">
              <h3 class="font-semibold text-gray-800">{{ $t('committee.files') }}</h3>
              <button v-if="isManager" @click="openAddFile" class="btn-secondary text-xs py-1 px-3">
                + {{ $t('committee.addFile') }}
              </button>
            </div>
            <div v-if="!selectedMeeting.files || selectedMeeting.files.length === 0"
              class="text-sm text-gray-400 py-3 text-center border rounded-lg">첨부 파일 없음</div>
            <div v-else class="space-y-2">
              <div v-for="f in selectedMeeting.files" :key="f.id"
                class="flex items-center justify-between p-3 bg-gray-50 rounded-lg border">
                <div class="flex items-center gap-3">
                  <span :class="fileTypeBadge(f.fileType)" class="text-xs px-2 py-0.5 rounded font-medium flex-shrink-0">
                    {{ $t(`committee.fileType_label.${f.fileType}`) }}
                  </span>
                  <div>
                    <p class="text-sm font-medium text-gray-900">{{ f.title }}</p>
                    <p v-if="f.fileName" class="text-xs text-gray-400">{{ f.fileName }}</p>
                  </div>
                </div>
                <div class="flex items-center gap-1">
                  <button v-if="f.fileName" @click="downloadFile(f)"
                    class="p-1.5 rounded hover:bg-gray-200 text-gray-500 hover:text-gray-700">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
                    </svg>
                  </button>
                  <button v-if="isManager" @click="confirmDeleteFile(f)"
                    class="p-1.5 rounded hover:bg-red-50 text-gray-400 hover:text-red-500">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                    </svg>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Add File Modal -->
    <div v-if="showFileModal" class="fixed inset-0 bg-black/50 z-[60] flex items-center justify-center p-4">
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-md">
        <div class="px-6 py-4 border-b">
          <h2 class="text-lg font-semibold">{{ $t('committee.addFile') }}</h2>
        </div>
        <div class="px-6 py-5 space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('committee.fileType') }}</label>
            <select v-model="fileForm.fileType" class="input w-full">
              <option v-for="ft in FILE_TYPES" :key="ft" :value="ft">{{ $t(`committee.fileType_label.${ft}`) }}</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('committee.fileTitle') }} *</label>
            <input v-model="fileForm.title" class="input w-full" placeholder="예: 2025년 제1회 안건" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">파일 선택</label>
            <input ref="fileInput" type="file" @change="onFileChange" class="block w-full text-sm text-gray-500
              file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-sm file:font-semibold
              file:bg-primary-50 file:text-primary-700 hover:file:bg-primary-100" />
          </div>
        </div>
        <div class="px-6 py-4 border-t flex justify-end gap-2">
          <button @click="showFileModal = false" class="btn-secondary">{{ $t('common.cancel') }}</button>
          <button @click="saveFile" :disabled="saving" class="btn-primary">
            {{ saving ? $t('common.loading') : $t('common.save') }}
          </button>
        </div>
      </div>
    </div>

    <!-- Confirm Delete -->
    <div v-if="confirmModal.show" class="fixed inset-0 bg-black/50 z-[70] flex items-center justify-center p-4">
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-sm p-6">
        <h3 class="text-base font-semibold text-gray-900 mb-2">삭제 확인</h3>
        <p class="text-sm text-gray-500 mb-5">{{ confirmModal.message }}</p>
        <div class="flex justify-end gap-2">
          <button @click="confirmModal.show = false" class="btn-secondary">{{ $t('common.cancel') }}</button>
          <button @click="confirmModal.onConfirm" class="btn-danger">{{ $t('common.delete') }}</button>
        </div>
      </div>
    </div>

    <!-- Year Picker -->
    <div v-if="showYearPicker" class="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4">
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-xs p-6">
        <h3 class="text-base font-semibold mb-4">연도 선택</h3>
        <input v-model.number="pickerYear" type="number" class="input w-full mb-4"
          :placeholder="new Date().getFullYear().toString()" />
        <div class="flex justify-end gap-2">
          <button @click="showYearPicker = false" class="btn-secondary">{{ $t('common.cancel') }}</button>
          <button @click="applyYearPicker" class="btn-primary">확인</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { committeeApi } from '@/api'

const auth = useAuthStore()
const isManager = computed(() => auth.isAdmin || auth.user?.role === 'MANAGER')

const currentYear = new Date().getFullYear()
const yearOptions = ref([currentYear])
const selectedYear = ref(currentYear)

const meetings = ref([])
const loading = ref(false)
const saving = ref(false)

const showFormModal = ref(false)
const showDetailModal = ref(false)
const showFileModal = ref(false)
const showYearPicker = ref(false)
const pickerYear = ref(currentYear)

const editTarget = ref(null)
const selectedMeeting = ref(null)

const STATUSES = ['PLANNED', 'COMPLETED', 'CANCELLED']
const FILE_TYPES = ['AGENDA', 'MINUTES', 'OTHER']

const form = ref(emptyForm())
const fileForm = ref({ fileType: 'AGENDA', title: '', file: null })
const fileInput = ref(null)

const confirmModal = ref({ show: false, message: '', onConfirm: () => {} })

function emptyForm() {
  return { year: currentYear, sessionNo: 1, title: '', meetingDate: '', location: '', attendees: '', status: 'PLANNED' }
}

onMounted(async () => {
  const years = await committeeApi.years()
  if (years?.data?.length) {
    yearOptions.value = years.data
    selectedYear.value = years.data[0]
  }
  fetchMeetings()
})

watch(selectedYear, fetchMeetings)

async function fetchMeetings() {
  loading.value = true
  try {
    const res = await committeeApi.list(selectedYear.value)
    meetings.value = res?.data || []
  } finally {
    loading.value = false
  }
}

function openCreateModal() {
  editTarget.value = null
  form.value = emptyForm()
  form.value.year = selectedYear.value
  form.value.sessionNo = (meetings.value.length > 0 ? Math.max(...meetings.value.map(m => m.sessionNo)) + 1 : 1)
  showFormModal.value = true
}

function openEditModal(m) {
  editTarget.value = m
  form.value = { year: m.year, sessionNo: m.sessionNo, title: m.title,
    meetingDate: m.meetingDate || '', location: m.location || '',
    attendees: m.attendees || '', status: m.status }
  showFormModal.value = true
}

async function saveMeeting() {
  saving.value = true
  try {
    if (editTarget.value) {
      await committeeApi.update(editTarget.value.id, form.value)
    } else {
      const res = await committeeApi.create(form.value)
      if (!yearOptions.value.includes(form.value.year)) {
        yearOptions.value = [form.value.year, ...yearOptions.value].sort((a, b) => b - a)
      }
      selectedYear.value = form.value.year
    }
    showFormModal.value = false
    fetchMeetings()
  } catch (e) {
    alert(e || '저장에 실패했습니다.')
  } finally {
    saving.value = false
  }
}

function openDetail(m) {
  selectedMeeting.value = m
  showDetailModal.value = true
}

function openAddFile() {
  fileForm.value = { fileType: 'AGENDA', title: '', file: null }
  showFileModal.value = true
}

function onFileChange(e) {
  fileForm.value.file = e.target.files[0] || null
}

async function saveFile() {
  if (!fileForm.value.title.trim()) { alert('파일 제목을 입력하세요.'); return }
  saving.value = true
  try {
    const newFile = await committeeApi.addFile(selectedMeeting.value.id, fileForm.value)
    selectedMeeting.value.files.push(newFile.data)
    showFileModal.value = false
    fetchMeetings()
  } catch (e) {
    alert(e || '파일 추가에 실패했습니다.')
  } finally {
    saving.value = false
  }
}

function downloadFile(f) {
  committeeApi.downloadFile(f.id, f.fileName)
}

function confirmDelete(m) {
  confirmModal.value = {
    show: true,
    message: '회의를 삭제하시겠습니까? 첨부 파일도 함께 삭제됩니다.',
    onConfirm: async () => {
      confirmModal.value.show = false
      await committeeApi.delete(m.id)
      fetchMeetings()
    }
  }
}

function confirmDeleteFile(f) {
  confirmModal.value = {
    show: true,
    message: '파일을 삭제하시겠습니까?',
    onConfirm: async () => {
      confirmModal.value.show = false
      await committeeApi.deleteFile(f.id)
      selectedMeeting.value.files = selectedMeeting.value.files.filter(x => x.id !== f.id)
      fetchMeetings()
    }
  }
}

function openYearPicker() {
  pickerYear.value = currentYear
  showYearPicker.value = true
}

function applyYearPicker() {
  if (pickerYear.value > 0) {
    if (!yearOptions.value.includes(pickerYear.value)) {
      yearOptions.value = [pickerYear.value, ...yearOptions.value].sort((a, b) => b - a)
    }
    selectedYear.value = pickerYear.value
  }
  showYearPicker.value = false
}

function statusBadge(s) {
  return {
    PLANNED: 'bg-blue-100 text-blue-700',
    COMPLETED: 'bg-green-100 text-green-700',
    CANCELLED: 'bg-gray-100 text-gray-500'
  }[s] || 'bg-gray-100 text-gray-500'
}

function fileTypeBadge(t) {
  return {
    AGENDA: 'bg-purple-100 text-purple-700',
    MINUTES: 'bg-green-100 text-green-700',
    OTHER: 'bg-gray-100 text-gray-600'
  }[t] || 'bg-gray-100 text-gray-600'
}

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

function truncate(s, n) {
  return s && s.length > n ? s.slice(0, n) + '…' : s
}
</script>
