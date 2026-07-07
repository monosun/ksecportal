<template>
  <div class="p-8">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">SBOM 관리</h1>
        <p class="text-sm text-gray-500 mt-1">소프트웨어(SW)별 버전과 포함된 라이브러리 구성요소를 관리합니다. SW 자산 등록 시 여기에 등록된 SW를 맵핑할 수 있습니다.</p>
      </div>
      <div class="flex gap-2">
        <button v-if="isManager" @click="showUploadModal = true"
          class="btn-secondary flex items-center gap-2 text-sm">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l4-4m0 0l4 4m-4-4v12"/>
          </svg>
          엑셀 일괄 등록
        </button>
        <button v-if="isManager" @click="openSwForm()" class="btn-primary flex items-center gap-2">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          SW 등록
        </button>
      </div>
    </div>

    <!-- 검색 -->
    <div class="card mb-4 flex flex-wrap gap-3">
      <input v-model="keyword" @input="debouncedSearch" placeholder="SW명 / 버전 / 공급업체 검색" class="input flex-1 min-w-48" />
    </div>

    <!-- SW 목록 -->
    <div class="card p-0 overflow-hidden">
      <div v-if="loading" class="p-8 text-center text-gray-400">{{ $t('common.loading') }}</div>
      <div v-else-if="!softwareList.length" class="p-8 text-center text-gray-400">{{ $t('common.noData') }}</div>
      <div v-else class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead class="bg-gray-50 border-b">
            <tr>
              <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">SW명</th>
              <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">버전</th>
              <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">공급업체</th>
              <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">라이브러리 수</th>
              <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">등록일</th>
              <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">{{ $t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-50">
            <tr v-for="s in softwareList" :key="s.id" class="hover:bg-gray-50 cursor-pointer" @click="openDetail(s)">
              <td class="px-5 py-3">
                <p class="font-medium text-gray-900">{{ s.name }}</p>
                <p v-if="s.description" class="text-xs text-gray-400 truncate max-w-md">{{ s.description }}</p>
              </td>
              <td class="px-5 py-3 font-mono text-gray-700">{{ s.version }}</td>
              <td class="px-5 py-3 text-gray-600">{{ s.vendor || '-' }}</td>
              <td class="px-5 py-3">
                <span class="badge-blue">{{ s.componentCount }}개</span>
              </td>
              <td class="px-5 py-3 text-gray-500 text-xs">{{ formatDate(s.createdAt) }}</td>
              <td class="px-5 py-3" @click.stop>
                <button class="text-blue-600 hover:underline text-xs mr-3" @click="openDetail(s)">구성요소</button>
                <button v-if="isManager" class="text-blue-600 hover:underline text-xs mr-3" @click="openSwForm(s)">{{ $t('common.edit') }}</button>
                <button v-if="isManager" class="text-red-500 hover:underline text-xs" @click="confirmDeleteSw(s)">{{ $t('common.delete') }}</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div v-if="totalPages > 1" class="flex justify-center gap-2 mt-4">
      <button v-for="p in totalPages" :key="p" @click="page = p - 1; load()"
        :class="['px-3 py-1 rounded border text-sm', page === p - 1 ? 'bg-blue-600 text-white border-blue-600' : 'border-gray-300 hover:bg-gray-50']">
        {{ p }}
      </button>
    </div>

    <!-- SW 등록/수정 모달 -->
    <div v-if="showSwForm" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-lg">
        <div class="flex items-center justify-between px-6 py-4 border-b">
          <h2 class="text-lg font-semibold text-gray-900">{{ editingSw ? 'SW 수정' : 'SW 등록' }}</h2>
          <button @click="showSwForm = false" class="text-gray-400 hover:text-gray-600">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <form @submit.prevent="submitSw" class="px-6 py-5 space-y-4">
          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">SW명 *</label>
              <input v-model="swForm.name" required class="input w-full" placeholder="KSecPortal Backend" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">버전 *</label>
              <input v-model="swForm.version" required class="input w-full font-mono" placeholder="1.1.0" />
            </div>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">공급업체</label>
            <input v-model="swForm.vendor" class="input w-full" placeholder="제조사 / 공급업체" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">설명</label>
            <textarea v-model="swForm.description" rows="2" class="input w-full resize-none" placeholder="용도 및 역할" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">비고</label>
            <textarea v-model="swForm.remarks" rows="2" class="input w-full resize-none" />
          </div>
          <div v-if="swFormError" class="text-sm text-red-600 bg-red-50 p-3 rounded-lg">{{ swFormError }}</div>
          <div class="flex justify-end gap-3 pt-2">
            <button type="button" @click="showSwForm = false" class="btn-secondary text-sm">{{ $t('common.cancel') }}</button>
            <button type="submit" :disabled="swSubmitting" class="btn-primary text-sm">
              {{ swSubmitting ? $t('common.loading') : $t('common.save') }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- 구성요소(라이브러리) 관리 모달 -->
    <div v-if="detail" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-3xl max-h-[85vh] flex flex-col">
        <div class="flex items-center justify-between px-6 py-4 border-b">
          <div>
            <h2 class="text-lg font-semibold text-gray-900">
              {{ detail.name }} <span class="font-mono text-blue-600">{{ detail.version }}</span>
            </h2>
            <p class="text-xs text-gray-500 mt-0.5">포함 라이브러리 {{ detail.components.length }}건{{ detail.vendor ? ` · ${detail.vendor}` : '' }}</p>
          </div>
          <button @click="detail = null" class="text-gray-400 hover:text-gray-600">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>

        <div class="px-6 py-4 overflow-y-auto flex-1">
          <!-- 라이브러리 추가 -->
          <form v-if="isManager" @submit.prevent="submitComponent" class="flex flex-wrap gap-2 mb-4 items-end bg-gray-50 rounded-lg p-3">
            <div class="flex-1 min-w-40">
              <label class="block text-xs font-medium text-gray-500 mb-1">라이브러리명 *</label>
              <input v-model="componentForm.libraryName" required class="input w-full text-sm" placeholder="spring-boot-starter-web" />
            </div>
            <div class="w-28">
              <label class="block text-xs font-medium text-gray-500 mb-1">버전</label>
              <input v-model="componentForm.libraryVersion" class="input w-full text-sm font-mono" placeholder="3.2.4" />
            </div>
            <div class="w-32">
              <label class="block text-xs font-medium text-gray-500 mb-1">라이선스</label>
              <input v-model="componentForm.license" class="input w-full text-sm" placeholder="Apache-2.0" />
            </div>
            <div class="flex-1 min-w-32">
              <label class="block text-xs font-medium text-gray-500 mb-1">비고</label>
              <input v-model="componentForm.remarks" class="input w-full text-sm" />
            </div>
            <button type="submit" :disabled="componentSubmitting" class="btn-primary text-sm whitespace-nowrap">
              {{ editingComponentId ? '수정' : '추가' }}
            </button>
            <button v-if="editingComponentId" type="button" @click="resetComponentForm" class="btn-secondary text-sm">취소</button>
          </form>
          <div v-if="componentError" class="text-sm text-red-600 bg-red-50 p-3 rounded-lg mb-3">{{ componentError }}</div>

          <div v-if="!detail.components.length" class="p-6 text-center text-gray-400 text-sm">
            등록된 라이브러리가 없습니다. {{ isManager ? '위 입력란 또는 엑셀 일괄 등록으로 추가하세요.' : '' }}
          </div>
          <table v-else class="w-full text-sm">
            <thead class="bg-gray-50 border-b">
              <tr>
                <th class="text-left px-4 py-2 text-xs font-medium text-gray-500 uppercase">라이브러리명</th>
                <th class="text-left px-4 py-2 text-xs font-medium text-gray-500 uppercase">버전</th>
                <th class="text-left px-4 py-2 text-xs font-medium text-gray-500 uppercase">라이선스</th>
                <th class="text-left px-4 py-2 text-xs font-medium text-gray-500 uppercase">비고</th>
                <th v-if="isManager" class="text-left px-4 py-2 text-xs font-medium text-gray-500 uppercase">{{ $t('common.actions') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-50">
              <tr v-for="c in detail.components" :key="c.id" class="hover:bg-gray-50">
                <td class="px-4 py-2 font-medium text-gray-800">{{ c.libraryName }}</td>
                <td class="px-4 py-2 font-mono text-gray-600">{{ c.libraryVersion || '-' }}</td>
                <td class="px-4 py-2">
                  <span v-if="c.license" class="badge-gray">{{ c.license }}</span>
                  <span v-else class="text-gray-400">-</span>
                </td>
                <td class="px-4 py-2 text-gray-500 text-xs">{{ c.remarks || '-' }}</td>
                <td v-if="isManager" class="px-4 py-2">
                  <button class="text-blue-600 hover:underline text-xs mr-3" @click="editComponent(c)">{{ $t('common.edit') }}</button>
                  <button class="text-red-500 hover:underline text-xs" @click="confirmDeleteComponent(c)">{{ $t('common.delete') }}</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="px-6 py-4 border-t flex justify-end">
          <button @click="detail = null" class="btn-secondary text-sm">닫기</button>
        </div>
      </div>
    </div>

    <!-- 엑셀 일괄 등록 모달 -->
    <div v-if="showUploadModal" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-lg">
        <div class="flex items-center justify-between px-6 py-4 border-b">
          <h2 class="text-lg font-semibold text-gray-900">엑셀 일괄 등록</h2>
          <button @click="closeUploadModal" class="text-gray-400 hover:text-gray-600">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>

        <div class="px-6 py-5 space-y-4">
          <div class="bg-blue-50 rounded-lg p-4 flex items-start gap-3">
            <svg class="w-5 h-5 text-blue-600 mt-0.5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
            <div class="text-sm text-blue-800">
              <p class="font-medium mb-1">등록 전 템플릿을 먼저 다운로드하세요.</p>
              <p class="text-blue-600 text-xs">한 행에 라이브러리 1건씩, SW명·SW버전·라이브러리명은 필수입니다. 같은 SW명+버전은 하나의 SW로 묶여 등록됩니다.</p>
            </div>
          </div>

          <button @click="downloadTemplate" :disabled="templateLoading"
            class="w-full btn-secondary flex items-center justify-center gap-2 text-sm">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
            </svg>
            {{ templateLoading ? '다운로드 중...' : '엑셀 템플릿 다운로드 (.xlsx)' }}
          </button>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">엑셀 파일 선택 (.xlsx)</label>
            <input ref="fileInput" type="file" accept=".xlsx,.xls"
              @change="onFileChange" class="block w-full text-sm text-gray-600
              file:mr-3 file:py-2 file:px-4 file:rounded-lg file:border-0
              file:text-sm file:font-medium file:bg-blue-50 file:text-blue-700
              hover:file:bg-blue-100 cursor-pointer border border-gray-200 rounded-lg p-1" />
            <p v-if="selectedFile" class="text-xs text-gray-500 mt-1">
              선택된 파일: {{ selectedFile.name }} ({{ (selectedFile.size / 1024).toFixed(1) }} KB)
            </p>
          </div>

          <div v-if="uploadResult" class="rounded-lg border text-sm">
            <div class="flex gap-4 p-3 border-b bg-gray-50 rounded-t-lg font-medium">
              <span>총 {{ uploadResult.total }}건</span>
              <span class="text-green-600">성공 {{ uploadResult.success }}건</span>
              <span v-if="uploadResult.failed > 0" class="text-red-600">실패 {{ uploadResult.failed }}건</span>
              <span class="text-blue-600">신규 SW {{ uploadResult.softwareCount }}건</span>
            </div>
            <ul v-if="uploadResult.errors?.length" class="divide-y max-h-40 overflow-y-auto">
              <li v-for="e in uploadResult.errors" :key="e.row" class="px-3 py-2 text-red-600 text-xs">
                <span class="font-medium">{{ e.row }}행:</span> {{ e.message }}
              </li>
            </ul>
            <p v-else class="px-3 py-2 text-green-600 text-xs font-medium">모든 행이 성공적으로 등록되었습니다.</p>
          </div>

          <div v-if="uploadError" class="text-sm text-red-600 bg-red-50 p-3 rounded-lg">{{ uploadError }}</div>
        </div>

        <div class="px-6 py-4 border-t flex justify-end gap-3">
          <button @click="closeUploadModal" class="btn-secondary text-sm">닫기</button>
          <button @click="uploadExcel" :disabled="!selectedFile || uploading"
            class="btn-primary text-sm flex items-center gap-2">
            <svg v-if="uploading" class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
            </svg>
            {{ uploading ? '등록 중...' : '등록 시작' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { sbomApi } from '@/api'
import { useDebounceFn } from '@vueuse/core'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const isManager = auth.isManager

const softwareList = ref([])
const loading = ref(false)
const keyword = ref('')
const page = ref(0)
const totalPages = ref(0)

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, size: 20 }
    if (keyword.value) params.keyword = keyword.value
    const res = await sbomApi.list(params)
    softwareList.value = res.data?.content || []
    totalPages.value = res.data?.page?.totalPages ?? res.data?.totalPages ?? 0
  } finally {
    loading.value = false
  }
}

const debouncedSearch = useDebounceFn(() => { page.value = 0; load() }, 400)

// ── SW 등록/수정 ──────────────────────────────────────────────────
const showSwForm = ref(false)
const editingSw = ref(null)
const swForm = ref({ name: '', version: '', vendor: '', description: '', remarks: '' })
const swSubmitting = ref(false)
const swFormError = ref(null)

function openSwForm(sw = null) {
  editingSw.value = sw
  swForm.value = sw
    ? { name: sw.name, version: sw.version, vendor: sw.vendor || '', description: sw.description || '', remarks: sw.remarks || '' }
    : { name: '', version: '', vendor: '', description: '', remarks: '' }
  swFormError.value = null
  showSwForm.value = true
}

async function submitSw() {
  swSubmitting.value = true
  swFormError.value = null
  try {
    if (editingSw.value) {
      await sbomApi.update(editingSw.value.id, swForm.value)
    } else {
      await sbomApi.create(swForm.value)
    }
    showSwForm.value = false
    load()
  } catch (e) {
    swFormError.value = e || '저장 중 오류가 발생했습니다.'
  } finally {
    swSubmitting.value = false
  }
}

async function confirmDeleteSw(sw) {
  if (!confirm(`"${sw.name} ${sw.version}" SW와 포함된 라이브러리 정보를 모두 삭제하시겠습니까?\n이 SW에 맵핑된 자산은 맵핑이 해제됩니다.`)) return
  await sbomApi.delete(sw.id)
  load()
}

// ── 구성요소(라이브러리) 관리 ────────────────────────────────────
const detail = ref(null)
const componentForm = ref({ libraryName: '', libraryVersion: '', license: '', remarks: '' })
const editingComponentId = ref(null)
const componentSubmitting = ref(false)
const componentError = ref(null)

async function openDetail(sw) {
  const res = await sbomApi.get(sw.id)
  detail.value = res.data
  resetComponentForm()
}

function resetComponentForm() {
  componentForm.value = { libraryName: '', libraryVersion: '', license: '', remarks: '' }
  editingComponentId.value = null
  componentError.value = null
}

function editComponent(c) {
  editingComponentId.value = c.id
  componentForm.value = {
    libraryName: c.libraryName,
    libraryVersion: c.libraryVersion || '',
    license: c.license || '',
    remarks: c.remarks || ''
  }
}

async function submitComponent() {
  componentSubmitting.value = true
  componentError.value = null
  try {
    if (editingComponentId.value) {
      await sbomApi.updateComponent(editingComponentId.value, componentForm.value)
    } else {
      await sbomApi.addComponent(detail.value.id, componentForm.value)
    }
    await refreshDetail()
    resetComponentForm()
    load()
  } catch (e) {
    componentError.value = e || '저장 중 오류가 발생했습니다.'
  } finally {
    componentSubmitting.value = false
  }
}

async function confirmDeleteComponent(c) {
  if (!confirm(`라이브러리 "${c.libraryName}"을(를) 삭제하시겠습니까?`)) return
  await sbomApi.deleteComponent(c.id)
  await refreshDetail()
  load()
}

async function refreshDetail() {
  if (!detail.value) return
  const res = await sbomApi.get(detail.value.id)
  detail.value = res.data
}

// ── 엑셀 일괄 등록 ────────────────────────────────────────────────
const showUploadModal = ref(false)
const selectedFile = ref(null)
const fileInput = ref(null)
const uploading = ref(false)
const uploadResult = ref(null)
const uploadError = ref(null)
const templateLoading = ref(false)

function onFileChange(e) {
  selectedFile.value = e.target.files[0] || null
  uploadResult.value = null
  uploadError.value = null
}

function closeUploadModal() {
  showUploadModal.value = false
  selectedFile.value = null
  uploadResult.value = null
  uploadError.value = null
  if (fileInput.value) fileInput.value.value = ''
}

async function downloadTemplate() {
  templateLoading.value = true
  try { await sbomApi.template() } finally { templateLoading.value = false }
}

async function uploadExcel() {
  if (!selectedFile.value) return
  uploading.value = true
  uploadResult.value = null
  uploadError.value = null
  try {
    const res = await sbomApi.upload(selectedFile.value)
    uploadResult.value = res.data
    if (res.data?.success > 0) load()
  } catch (e) {
    uploadError.value = e || '업로드 중 오류가 발생했습니다.'
  } finally {
    uploading.value = false
  }
}

function formatDate(dt) { return dt ? new Date(dt).toLocaleDateString('ko-KR') : '-' }

onMounted(load)
</script>
