<template>
  <div class="p-8">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">{{ $t('secDoc.title') }}</h1>
        <p class="text-sm text-gray-500 mt-1">{{ $t('secDoc.subtitle') }}</p>
      </div>
      <button v-if="isManager" @click="openCreate" class="btn-primary flex items-center gap-2">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
        </svg>
        {{ $t('secDoc.register') }}
      </button>
    </div>

    <!-- 필터 -->
    <div class="card mb-4 flex flex-wrap gap-3 items-end">
      <div class="flex flex-col gap-1">
        <label class="text-xs text-gray-500">{{ $t('secDoc.category') }}</label>
        <select v-model="filters.category" @change="search" class="input w-44 text-sm">
          <option value="">{{ $t('common.all') }}</option>
          <option v-for="c in CATEGORIES" :key="c" :value="c">{{ $t(`secDoc.category_label.${c}`) }}</option>
        </select>
      </div>
      <div class="flex flex-col gap-1">
        <label class="text-xs text-gray-500">검색 대상</label>
        <select v-model="filters.field" @change="search" class="input w-32 text-sm">
          <option value="ALL">전체</option>
          <option value="TITLE">제목</option>
          <option value="DESCRIPTION">내용</option>
          <option value="FILENAME">파일명</option>
          <option value="VERSION">버전</option>
          <option value="ORG">제작기관</option>
        </select>
      </div>
      <div class="flex flex-col gap-1 flex-1 min-w-48">
        <label class="text-xs text-gray-500">{{ $t('common.search') }}</label>
        <input v-model="filters.keyword" @input="debouncedSearch" :placeholder="$t('common.search')"
          class="input text-sm" />
      </div>
      <div class="flex items-center gap-2">
        <button @click="resetFilters" class="btn-secondary text-sm">{{ $t('common.all') }}</button>
        <div class="flex items-center gap-1.5">
          <label class="text-xs text-gray-500 whitespace-nowrap">행/페이지</label>
          <input v-model.number="pageSize" @change="onPageSizeChange" type="number" min="5" max="100"
            class="input w-16 text-sm text-center" />
        </div>
      </div>
    </div>

    <!-- 목록 -->
    <div class="card">
      <div v-if="loading" class="text-center py-10 text-gray-500">{{ $t('common.loading') }}</div>
      <div v-else-if="docs.length === 0" class="text-center py-10 text-gray-400">{{ $t('common.noData') }}</div>
      <template v-else>
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b">
              <th class="text-left py-3 px-4 font-semibold text-gray-600">{{ $t('common.title') }}</th>
              <th class="text-left py-3 px-4 font-semibold text-gray-600">{{ $t('secDoc.category') }}</th>
              <th class="text-left py-3 px-4 font-semibold text-gray-600">{{ $t('secDoc.version') }}</th>
              <th class="text-left py-3 px-4 font-semibold text-gray-600">{{ $t('secDoc.fileName') }}</th>
              <th class="text-left py-3 px-4 font-semibold text-gray-600">제작기관</th>
              <th class="text-left py-3 px-4 font-semibold text-gray-600">{{ $t('secDoc.uploader') }}</th>
              <th class="text-left py-3 px-4 font-semibold text-gray-600">등록일</th>
              <th class="py-3 px-4"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="doc in docs" :key="doc.id" class="border-b hover:bg-gray-50">
              <td class="py-3 px-4">
                <div class="font-medium text-gray-900 cursor-pointer hover:text-primary-600"
                  @click="openDetail(doc)">{{ doc.title }}</div>
                <div v-if="doc.description" class="text-xs text-gray-400 mt-0.5 line-clamp-1">{{ doc.description }}</div>
              </td>
              <td class="py-3 px-4">
                <span :class="categoryBadge(doc.category)" class="text-xs font-medium px-2 py-0.5 rounded">
                  {{ $t(`secDoc.category_label.${doc.category}`) }}
                </span>
              </td>
              <td class="py-3 px-4">
                <div class="flex items-center gap-1.5">
                  <span class="font-mono text-sm">v{{ doc.version }}</span>
                  <span v-if="doc.versionCount > 1"
                    class="text-xs bg-gray-100 text-gray-500 px-1.5 py-0.5 rounded cursor-pointer hover:bg-gray-200"
                    @click="openVersions(doc)">
                    {{ doc.versionCount }}개 버전
                  </span>
                </div>
              </td>
              <td class="py-3 px-4">
                <button v-if="doc.fileName" @click="download(doc)"
                  class="flex items-center gap-1 text-primary-600 hover:text-primary-800 text-xs">
                  <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
                  </svg>
                  {{ doc.fileName }}
                </button>
                <span v-else class="text-xs text-gray-400">{{ $t('secDoc.noFile') }}</span>
              </td>
              <td class="py-3 px-4 text-gray-500 text-xs">{{ doc.producingOrg || '-' }}</td>
              <td class="py-3 px-4 text-gray-500 text-xs">{{ doc.uploaderName || '-' }}</td>
              <td class="py-3 px-4 text-gray-400 text-xs whitespace-nowrap">{{ formatDate(doc.createdAt) }}</td>
              <td class="py-3 px-4">
                <div v-if="isManager" class="flex items-center gap-1">
                  <button @click="openAddVersion(doc)"
                    class="text-xs px-2 py-1 rounded border border-gray-200 text-gray-600 hover:bg-gray-50">
                    + 버전
                  </button>
                  <button @click="confirmDelete(doc)"
                    class="text-xs px-2 py-1 rounded border border-red-100 text-red-500 hover:bg-red-50">
                    삭제
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </template>
    </div>

    <!-- 페이지네이션 -->
    <div v-if="totalPages > 1" class="flex justify-center items-center gap-1 mt-4">
      <button @click="goPage(0)" :disabled="page === 0"
        class="px-2 py-1.5 rounded border text-xs text-gray-500 border-gray-300 hover:bg-gray-50 disabled:opacity-30 disabled:cursor-not-allowed">«</button>
      <button @click="goPage(page - 1)" :disabled="page === 0"
        class="px-2 py-1.5 rounded border text-xs text-gray-500 border-gray-300 hover:bg-gray-50 disabled:opacity-30 disabled:cursor-not-allowed">‹</button>
      <template v-for="p in visiblePages" :key="p">
        <span v-if="p === '...'" class="px-2 py-1.5 text-xs text-gray-400">…</span>
        <button v-else @click="goPage(p - 1)"
          :class="['px-3 py-1.5 rounded border text-xs transition-colors',
            page === p - 1 ? 'bg-primary-600 text-white border-primary-600 font-semibold' : 'border-gray-300 text-gray-600 hover:bg-gray-50']">
          {{ p }}
        </button>
      </template>
      <button @click="goPage(page + 1)" :disabled="page >= totalPages - 1"
        class="px-2 py-1.5 rounded border text-xs text-gray-500 border-gray-300 hover:bg-gray-50 disabled:opacity-30 disabled:cursor-not-allowed">›</button>
      <button @click="goPage(totalPages - 1)" :disabled="page >= totalPages - 1"
        class="px-2 py-1.5 rounded border text-xs text-gray-500 border-gray-300 hover:bg-gray-50 disabled:opacity-30 disabled:cursor-not-allowed">»</button>
    </div>

    <!-- 문서 등록 모달 -->
    <div v-if="showCreate" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-lg mx-4 p-6">
        <h2 class="text-lg font-bold mb-4">{{ $t('secDoc.register') }}</h2>
        <div class="space-y-3">
          <div>
            <label class="text-sm font-medium text-gray-700">{{ $t('secDoc.form.title') }} *</label>
            <input v-model="form.title" class="input w-full mt-1" :placeholder="$t('secDoc.form.titlePlaceholder')" />
          </div>
          <div>
            <label class="text-sm font-medium text-gray-700">{{ $t('secDoc.category') }} *</label>
            <select v-model="form.category" class="input w-full mt-1">
              <option value="">선택하세요</option>
              <option v-for="c in CATEGORIES" :key="c" :value="c">{{ $t(`secDoc.category_label.${c}`) }}</option>
            </select>
          </div>
          <div>
            <label class="text-sm font-medium text-gray-700">{{ $t('secDoc.version') }}</label>
            <input v-model="form.version" class="input w-full mt-1" :placeholder="$t('secDoc.form.versionPlaceholder')" />
          </div>
          <div>
            <label class="text-sm font-medium text-gray-700">제작기관</label>
            <select v-model="form.producingOrgSelect" class="input w-full mt-1">
              <option value="">선택 안 함</option>
              <option v-for="o in orgCodes" :key="o.value" :value="o.value">{{ o.label || o.value }}</option>
              <option value="__custom__">직접 입력...</option>
            </select>
            <input v-if="form.producingOrgSelect === '__custom__'" v-model="form.producingOrgCustom"
              class="input w-full mt-2" placeholder="제작기관명을 직접 입력하세요" />
            <p class="text-xs text-gray-400 mt-1">목록은 코드 관리 &gt; 문서 제작기관에서 관리합니다.</p>
          </div>
          <div>
            <label class="text-sm font-medium text-gray-700">{{ $t('secDoc.description') }}</label>
            <textarea v-model="form.description" rows="3" class="input w-full mt-1"
              :placeholder="$t('secDoc.form.descPlaceholder')"></textarea>
          </div>
          <div>
            <label class="text-sm font-medium text-gray-700">파일 첨부</label>
            <input type="file" @change="onFileChange" class="mt-1 w-full text-sm text-gray-600" />
          </div>
        </div>
        <div class="flex justify-end gap-2 mt-5">
          <button @click="closeCreate" class="btn-secondary">{{ $t('common.cancel') }}</button>
          <button @click="submitCreate" :disabled="saving" class="btn-primary">
            {{ saving ? '등록 중...' : $t('common.create') }}
          </button>
        </div>
      </div>
    </div>

    <!-- 새 버전 추가 모달 -->
    <div v-if="showAddVersion" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-lg mx-4 p-6">
        <h2 class="text-lg font-bold mb-1">{{ $t('secDoc.addVersion') }}</h2>
        <p class="text-sm text-gray-500 mb-4">{{ selectedDoc?.title }}</p>
        <div class="space-y-3">
          <div>
            <label class="text-sm font-medium text-gray-700">새 버전 번호</label>
            <input v-model="versionForm.version" class="input w-full mt-1"
              :placeholder="`현재: v${selectedDoc?.version} → 다음 버전`" />
          </div>
          <div>
            <label class="text-sm font-medium text-gray-700">변경 설명 (선택)</label>
            <textarea v-model="versionForm.description" rows="3" class="input w-full mt-1"
              :placeholder="$t('secDoc.form.newVersionDesc')"></textarea>
          </div>
          <div>
            <label class="text-sm font-medium text-gray-700">파일 첨부 (선택)</label>
            <input type="file" @change="onVersionFileChange" class="mt-1 w-full text-sm text-gray-600" />
          </div>
        </div>
        <div class="flex justify-end gap-2 mt-5">
          <button @click="showAddVersion = false" class="btn-secondary">{{ $t('common.cancel') }}</button>
          <button @click="submitAddVersion" :disabled="saving" class="btn-primary">
            {{ saving ? '저장 중...' : $t('common.save') }}
          </button>
        </div>
      </div>
    </div>

    <!-- 버전 이력 모달 -->
    <div v-if="showVersions" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-2xl mx-4 p-6">
        <div class="flex items-center justify-between mb-4">
          <h2 class="text-lg font-bold">{{ $t('secDoc.versionHistory') }}</h2>
          <button @click="showVersions = false" class="text-gray-400 hover:text-gray-600">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <p class="text-sm text-gray-500 mb-4">{{ selectedDoc?.title }}</p>
        <div class="space-y-2">
          <div v-for="v in versions" :key="v.id"
            class="flex items-center justify-between p-3 rounded-lg border border-gray-100 hover:bg-gray-50">
            <div>
              <div class="flex items-center gap-2">
                <span class="font-mono font-semibold text-sm">v{{ v.version }}</span>
                <span v-if="v.latest" class="text-xs bg-green-100 text-green-700 px-1.5 py-0.5 rounded font-medium">
                  {{ $t('secDoc.latestVersion') }}
                </span>
              </div>
              <div v-if="v.description" class="text-xs text-gray-500 mt-0.5">{{ v.description }}</div>
              <div class="text-xs text-gray-400 mt-0.5">{{ v.uploaderName }} · {{ formatDate(v.createdAt) }}</div>
            </div>
            <div class="flex items-center gap-2">
              <button v-if="v.fileName" @click="downloadById(v)"
                class="text-xs text-primary-600 hover:text-primary-800 flex items-center gap-1">
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
                </svg>
                {{ $t('secDoc.download') }}
              </button>
              <button v-if="isManager" @click="confirmDeleteVersion(v)"
                class="text-xs text-red-400 hover:text-red-600">삭제</button>
            </div>
          </div>
        </div>
        <div class="flex justify-end mt-4">
          <button @click="showVersions = false" class="btn-secondary">{{ $t('common.cancel') }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { secDocApi, codeApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useDebounceFn } from '@vueuse/core'

const auth = useAuthStore()
const isManager = auth.isManager

const CATEGORIES = ['GUIDE', 'POLICY', 'PROCEDURE', 'STANDARD', 'CHECKLIST', 'TEMPLATE', 'REPORT', 'OTHER']

const docs = ref([])
const loading = ref(true)
const page = ref(0)
const pageSize = ref(20)
const totalPages = ref(0)
const filters = ref({ category: '', keyword: '', field: 'ALL' })
const orgCodes = ref([])

const showCreate = ref(false)
const showAddVersion = ref(false)
const showVersions = ref(false)
const saving = ref(false)
const selectedDoc = ref(null)
const versions = ref([])

const form = ref({ title: '', category: '', version: '1.0', description: '', producingOrgSelect: '', producingOrgCustom: '' })
const formFile = ref(null)
const versionForm = ref({ version: '', description: '' })
const versionFile = ref(null)

let searchTimer = null

const visiblePages = computed(() => {
  const total = totalPages.value
  const cur = page.value + 1
  if (total <= 9) return Array.from({ length: total }, (_, i) => i + 1)
  const pages = []
  const start = Math.max(2, cur - 2)
  const end = Math.min(total - 1, cur + 2)
  pages.push(1)
  if (start > 2) pages.push('...')
  for (let i = start; i <= end; i++) pages.push(i)
  if (end < total - 1) pages.push('...')
  pages.push(total)
  return pages
})

function search() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { page.value = 0; load() }, 400)
}

const debouncedSearch = useDebounceFn(() => { page.value = 0; load() }, 400)

function resetFilters() {
  filters.value = { category: '', keyword: '', field: 'ALL' }
  page.value = 0
  load()
}

function onPageSizeChange() {
  if (pageSize.value < 5) pageSize.value = 5
  if (pageSize.value > 100) pageSize.value = 100
  page.value = 0
  load()
}

function goPage(p) {
  const target = Math.max(0, Math.min(p, totalPages.value - 1))
  if (target !== page.value) { page.value = target; load() }
}

async function load() {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: pageSize.value,
      ...(filters.value.category && { category: filters.value.category }),
      ...(filters.value.keyword && { keyword: filters.value.keyword }),
      ...(filters.value.keyword && filters.value.field !== 'ALL' && { searchField: filters.value.field })
    }
    const res = await secDocApi.list(params)
    docs.value = res.data?.content || res.content || []
    totalPages.value = res.data?.page?.totalPages ?? res.data?.totalPages ?? 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.value = { title: '', category: '', version: '1.0', description: '', producingOrgSelect: '', producingOrgCustom: '' }
  formFile.value = null
  showCreate.value = true
}

function closeCreate() {
  showCreate.value = false
}

function onFileChange(e) {
  formFile.value = e.target.files[0] || null
}

function onVersionFileChange(e) {
  versionFile.value = e.target.files[0] || null
}

async function submitCreate() {
  if (!form.value.title || !form.value.category) return
  saving.value = true
  try {
    const { producingOrgSelect, producingOrgCustom, ...data } = form.value
    data.producingOrg = producingOrgSelect === '__custom__'
      ? producingOrgCustom.trim()
      : producingOrgSelect
    await secDocApi.create(data, formFile.value)
    showCreate.value = false
    load()
  } catch (e) {
    alert(e || '등록에 실패했습니다.')
  } finally {
    saving.value = false
  }
}

function openAddVersion(doc) {
  selectedDoc.value = doc
  versionForm.value = { version: '', description: '' }
  versionFile.value = null
  showAddVersion.value = true
}

async function submitAddVersion() {
  saving.value = true
  try {
    await secDocApi.addVersion(selectedDoc.value.id, versionForm.value, versionFile.value)
    showAddVersion.value = false
    load()
  } catch (e) {
    alert(e || '버전 추가에 실패했습니다.')
  } finally {
    saving.value = false
  }
}

async function openVersions(doc) {
  selectedDoc.value = doc
  try {
    const res = await secDocApi.versions(doc.id)
    versions.value = res.data || res
    showVersions.value = true
  } catch (e) {
    console.error(e)
  }
}

function openDetail(doc) {
  openVersions(doc)
}

async function download(doc) {
  try {
    await secDocApi.download(doc.id, doc.fileName)
  } catch (e) {
    alert('다운로드에 실패했습니다.')
  }
}

async function downloadById(v) {
  try {
    await secDocApi.download(v.id, v.fileName)
  } catch (e) {
    alert('다운로드에 실패했습니다.')
  }
}

async function confirmDelete(doc) {
  if (!confirm(`"${doc.title}" 문서의 모든 버전을 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.`)) return
  try {
    await secDocApi.delete(doc.id)
    load()
  } catch (e) {
    alert(e || '삭제에 실패했습니다.')
  }
}

async function confirmDeleteVersion(v) {
  if (!confirm(`v${v.version} 버전을 삭제하시겠습니까?`)) return
  try {
    await secDocApi.deleteVersion(v.id)
    if (versions.value.length <= 1) {
      showVersions.value = false
    } else {
      const res = await secDocApi.versions(selectedDoc.value.id)
      versions.value = res.data || res
    }
    load()
  } catch (e) {
    alert(e || '삭제에 실패했습니다.')
  }
}

function categoryBadge(cat) {
  const map = {
    GUIDE: 'bg-blue-100 text-blue-700',
    POLICY: 'bg-purple-100 text-purple-700',
    PROCEDURE: 'bg-yellow-100 text-yellow-700',
    STANDARD: 'bg-indigo-100 text-indigo-700',
    CHECKLIST: 'bg-green-100 text-green-700',
    TEMPLATE: 'bg-teal-100 text-teal-700',
    REPORT: 'bg-orange-100 text-orange-700',
    OTHER: 'bg-gray-100 text-gray-600'
  }
  return map[cat] || 'bg-gray-100 text-gray-600'
}

function formatDate(dt) {
  if (!dt) return '-'
  return new Date(dt).toLocaleDateString()
}

async function loadOrgCodes() {
  try {
    const res = await codeApi.getValues('SEC_DOC_ORG')
    orgCodes.value = res.data ?? res ?? []
  } catch {
    orgCodes.value = []
  }
}

onMounted(() => { load(); loadOrgCodes() })
</script>
