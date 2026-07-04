<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">문제은행 관리</h1>
        <p class="text-sm text-gray-400 mt-0.5">교육 퀴즈 문항을 등록·관리합니다. 교육 등록 시 문제은행에서 문항을 가져올 수 있습니다.</p>
      </div>
      <div class="flex gap-2">
        <button v-if="selected.size > 0" @click="downloadSelected"
          class="btn-secondary text-sm flex items-center gap-1.5 !border-green-300 !text-green-700 hover:!bg-green-50">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"/>
          </svg>
          선택 문제 다운로드 ({{ selected.size }})
        </button>
        <button @click="showBulkModal = true" class="btn-secondary text-sm flex items-center gap-1.5">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l4-4m0 0l4 4m-4-4v12"/>
          </svg>
          Excel 일괄 등록
        </button>
        <button @click="openCreate" class="btn-primary text-sm flex items-center gap-1.5">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          문제 추가
        </button>
      </div>
    </div>

    <div class="page-body">

    <!-- 필터 -->
    <div class="card mb-4 flex flex-wrap gap-3 items-end">
      <div class="flex flex-col gap-1">
        <label class="text-xs text-gray-500">분류</label>
        <select v-model="filters.category" @change="search" class="input w-48 text-sm">
          <option value="">전체</option>
          <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
        </select>
      </div>
      <div class="flex flex-col gap-1">
        <label class="text-xs text-gray-500">난이도</label>
        <select v-model="filters.difficulty" @change="search" class="input w-28 text-sm">
          <option value="">전체</option>
          <option value="상">상</option>
          <option value="중">중</option>
          <option value="하">하</option>
        </select>
      </div>
      <div class="flex flex-col gap-1 flex-1 min-w-48">
        <label class="text-xs text-gray-500">문제 검색</label>
        <input v-model="filters.keyword" @input="debouncedSearch" placeholder="문제 내용 검색..." class="input text-sm" />
      </div>
      <span class="text-xs text-gray-400 ml-auto pb-2">총 {{ totalElements }}문항</span>
    </div>

    <!-- 목록 -->
    <div class="card">
      <div v-if="loading" class="text-center py-10 text-gray-500">로딩 중...</div>
      <div v-else-if="questions.length === 0" class="text-center py-10 text-gray-400">
        등록된 문제가 없습니다. "문제 추가" 또는 "Excel 일괄 등록"으로 문항을 등록하세요.
      </div>
      <template v-else>
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b">
              <th class="py-3 px-4 w-10">
                <input type="checkbox" class="w-4 h-4 rounded text-primary-500 cursor-pointer"
                  :checked="questions.length > 0 && questions.every(q => selected.has(q.id))"
                  :indeterminate="questions.some(q => selected.has(q.id)) && !questions.every(q => selected.has(q.id))"
                  @change="togglePageAll($event.target.checked)" />
              </th>
              <th class="text-left py-3 px-4 font-semibold text-gray-600 w-16">ID</th>
              <th class="text-left py-3 px-4 font-semibold text-gray-600 w-32">분류</th>
              <th class="text-left py-3 px-4 font-semibold text-gray-600 w-20">난이도</th>
              <th class="text-left py-3 px-4 font-semibold text-gray-600">문제</th>
              <th class="text-left py-3 px-4 font-semibold text-gray-600 w-16">정답</th>
              <th class="text-left py-3 px-4 font-semibold text-gray-600 w-28">등록일</th>
              <th class="py-3 px-4 w-32"></th>
            </tr>
          </thead>
          <tbody>
            <template v-for="q in questions" :key="q.id">
              <tr class="border-b hover:bg-gray-50 cursor-pointer" @click="toggleExpand(q.id)">
                <td class="py-3 px-4" @click.stop>
                  <input type="checkbox" class="w-4 h-4 rounded text-primary-500 cursor-pointer"
                    :checked="selected.has(q.id)" @change="toggleSelect(q)" />
                </td>
                <td class="py-3 px-4 text-gray-400 text-xs">{{ q.id }}</td>
                <td class="py-3 px-4">
                  <span v-if="q.category" class="text-xs font-medium px-2 py-0.5 rounded bg-indigo-100 text-indigo-700">{{ q.category }}</span>
                  <span v-else class="text-xs text-gray-400">-</span>
                </td>
                <td class="py-3 px-4">
                  <span class="text-xs font-bold px-2 py-0.5 rounded" :class="difficultyClass(q.difficulty)">{{ q.difficulty || '중' }}</span>
                </td>
                <td class="py-3 px-4 text-gray-800">
                  <div class="flex items-center gap-1.5">
                    <svg class="w-3.5 h-3.5 text-gray-400 flex-shrink-0 transition-transform"
                      :class="expandedId === q.id ? 'rotate-90' : ''" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
                    </svg>
                    <span class="line-clamp-1">{{ q.question }}</span>
                  </div>
                </td>
                <td class="py-3 px-4">
                  <span class="font-mono font-bold text-primary-600">{{ q.correctAnswer }}</span>
                </td>
                <td class="py-3 px-4 text-gray-400 text-xs whitespace-nowrap">{{ formatDate(q.createdAt) }}</td>
                <td class="py-3 px-4" @click.stop>
                  <div class="flex items-center gap-1 justify-end">
                    <button @click="openEdit(q)"
                      class="text-xs px-2 py-1 rounded border border-gray-200 text-gray-600 hover:bg-gray-50">수정</button>
                    <button @click="confirmDelete(q)"
                      class="text-xs px-2 py-1 rounded border border-red-100 text-red-500 hover:bg-red-50">삭제</button>
                  </div>
                </td>
              </tr>
              <!-- 상세(보기·해설) -->
              <tr v-if="expandedId === q.id" class="border-b bg-gray-50/60">
                <td colspan="2"></td>
                <td colspan="6" class="py-3 px-4">
                  <div class="space-y-1 text-sm">
                    <p v-for="letter in ['A','B','C','D']" :key="letter" v-show="optionOf(q, letter)"
                      class="flex gap-2"
                      :class="q.correctAnswer === letter ? 'text-green-700 font-semibold' : 'text-gray-600'">
                      <span class="font-mono w-4 flex-shrink-0">{{ letter }}.</span>
                      <span>{{ optionOf(q, letter) }}</span>
                      <span v-if="q.correctAnswer === letter" class="text-[10px] px-1.5 py-0.5 rounded bg-green-100 text-green-700 font-bold self-center">정답</span>
                    </p>
                    <p v-if="q.explanation" class="mt-2 pt-2 border-t border-gray-200 text-xs text-gray-500">
                      <span class="font-semibold text-gray-600">해설</span> {{ q.explanation }}
                    </p>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </template>
    </div>

    <!-- 페이지네이션 -->
    <div v-if="totalPages > 1" class="flex justify-center items-center gap-1 mt-4">
      <button @click="goPage(page - 1)" :disabled="page === 0"
        class="px-2 py-1.5 rounded border text-xs text-gray-500 border-gray-300 hover:bg-gray-50 disabled:opacity-30">‹</button>
      <span class="px-3 text-xs text-gray-500">{{ page + 1 }} / {{ totalPages }}</span>
      <button @click="goPage(page + 1)" :disabled="page >= totalPages - 1"
        class="px-2 py-1.5 rounded border text-xs text-gray-500 border-gray-300 hover:bg-gray-50 disabled:opacity-30">›</button>
    </div>

    <!-- 추가/수정 모달 -->
    <div v-if="showForm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-2xl p-6 max-h-[90vh] overflow-y-auto">
        <h2 class="text-lg font-bold mb-4">{{ form.id ? '문제 수정' : '문제 추가' }}</h2>
        <div class="space-y-3">
          <div class="grid grid-cols-3 gap-3">
            <div>
              <label class="text-sm font-medium text-gray-700">분류</label>
              <input v-model="form.category" list="qb-categories" class="input w-full mt-1" placeholder="예: 개인정보보호" />
              <datalist id="qb-categories">
                <option v-for="c in categories" :key="c" :value="c" />
              </datalist>
            </div>
            <div>
              <label class="text-sm font-medium text-gray-700">난이도 *</label>
              <select v-model="form.difficulty" class="input w-full mt-1">
                <option value="상">상</option>
                <option value="중">중</option>
                <option value="하">하</option>
              </select>
            </div>
            <div>
              <label class="text-sm font-medium text-gray-700">정답 *</label>
              <select v-model="form.correctAnswer" class="input w-full mt-1">
                <option v-for="l in ['A','B','C','D']" :key="l" :value="l">{{ l }}</option>
              </select>
            </div>
          </div>
          <div>
            <label class="text-sm font-medium text-gray-700">문제 *</label>
            <textarea v-model="form.question" rows="2" class="input w-full mt-1" placeholder="문제를 입력하세요"></textarea>
          </div>
          <div v-for="l in ['A','B','C','D']" :key="l">
            <label class="text-sm font-medium text-gray-700">보기 {{ l }}{{ l === 'A' || l === 'B' ? ' *' : '' }}</label>
            <input v-model="form['option' + l]" class="input w-full mt-1" :placeholder="`보기 ${l}`" />
          </div>
          <div>
            <label class="text-sm font-medium text-gray-700">해설 (선택)</label>
            <textarea v-model="form.explanation" rows="2" class="input w-full mt-1" placeholder="정답 해설을 입력하세요"></textarea>
          </div>
        </div>
        <div class="flex justify-end gap-2 mt-5">
          <button @click="showForm = false" class="btn-secondary">취소</button>
          <button @click="submitForm" :disabled="saving || !form.question || !form.optionA || !form.optionB" class="btn-primary">
            {{ saving ? '저장 중...' : '저장' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Excel 일괄 등록 모달 -->
    <BulkImportModal
      v-if="showBulkModal"
      ref="bulkModalRef"
      title="문제은행 Excel 일괄 등록"
      desc="엑셀 템플릿을 다운로드하여 문항을 작성한 후 업로드하세요. 분류·문제·보기A/B·정답(A~D)은 필수입니다."
      :template-loading="templateLoading"
      @close="showBulkModal = false; bulkModalRef?.reset()"
      @download-template="downloadTemplate"
      @upload="handleBulkUpload"
    />

    </div><!-- /page-body -->
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import * as XLSX from 'xlsx-js-style'
import { quizBankApi } from '@/api'
import BulkImportModal from '@/components/BulkImportModal.vue'
import { useDebounceFn } from '@vueuse/core'

const questions = ref([])
const categories = ref([])
const loading = ref(true)
const page = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const filters = ref({ category: '', difficulty: '', keyword: '' })
const expandedId = ref(null)

const showForm = ref(false)
const saving = ref(false)
const form = ref(emptyForm())

const showBulkModal = ref(false)
const bulkModalRef = ref(null)
const templateLoading = ref(false)

function emptyForm() {
  return { id: null, category: '', difficulty: '중', question: '', optionA: '', optionB: '', optionC: '', optionD: '', correctAnswer: 'A', explanation: '' }
}

function optionOf(q, letter) { return q['option' + letter] }

function difficultyClass(d) {
  return { '상': 'bg-red-100 text-red-600', '중': 'bg-amber-100 text-amber-700', '하': 'bg-green-100 text-green-700' }[d] || 'bg-amber-100 text-amber-700'
}

function toggleExpand(id) { expandedId.value = expandedId.value === id ? null : id }

// ── 선택 & Excel 다운로드 (일괄등록 양식과 동일 형식) ──
const selected = ref(new Map())   // id → 문항 객체 (페이지 이동해도 선택 유지)

function toggleSelect(q) {
  const next = new Map(selected.value)
  if (next.has(q.id)) next.delete(q.id)
  else next.set(q.id, q)
  selected.value = next
}

function togglePageAll(checked) {
  const next = new Map(selected.value)
  for (const q of questions.value) {
    if (checked) next.set(q.id, q)
    else next.delete(q.id)
  }
  selected.value = next
}

function downloadSelected() {
  const items = [...selected.value.values()].sort((a, b) => a.id - b.id)
  if (!items.length) return
  const HEADERS = ['분류', '난이도(상/중/하)*', '문제*', '보기A*', '보기B*', '보기C', '보기D', '정답(A~D)*', '해설']
  const rows = items.map(q => [
    q.category ?? '', q.difficulty ?? '중', q.question, q.optionA, q.optionB,
    q.optionC ?? '', q.optionD ?? '', q.correctAnswer, q.explanation ?? '',
  ])
  const ws = XLSX.utils.aoa_to_sheet([HEADERS, ...rows])
  // 헤더 스타일 (일괄등록 템플릿과 동일한 느낌)
  for (let c = 0; c < HEADERS.length; c++) {
    const addr = XLSX.utils.encode_cell({ r: 0, c })
    ws[addr].s = { font: { bold: true }, fill: { fgColor: { rgb: 'E5E7EB' } } }
  }
  ws['!cols'] = [{ wch: 14 }, { wch: 14 }, { wch: 60 }, { wch: 30 }, { wch: 30 }, { wch: 30 }, { wch: 30 }, { wch: 12 }, { wch: 40 }]
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '문제은행')
  const d = new Date().toISOString().slice(0, 10).replace(/-/g, '')
  XLSX.writeFile(wb, `문제은행_선택${items.length}건_${d}.xlsx`)
}

const debouncedSearch = useDebounceFn(() => { page.value = 0; load() }, 400)
function search() { page.value = 0; load() }
function goPage(p) {
  const t = Math.max(0, Math.min(p, totalPages.value - 1))
  if (t !== page.value) { page.value = t; load() }
}

async function load() {
  loading.value = true
  try {
    const res = await quizBankApi.list({
      page: page.value, size: 20,
      ...(filters.value.category && { category: filters.value.category }),
      ...(filters.value.difficulty && { difficulty: filters.value.difficulty }),
      ...(filters.value.keyword && { keyword: filters.value.keyword }),
    })
    const d = res.data
    questions.value = d?.content || []
    totalPages.value = d?.page?.totalPages ?? d?.totalPages ?? 0
    totalElements.value = d?.page?.totalElements ?? d?.totalElements ?? questions.value.length
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try { categories.value = (await quizBankApi.categories()).data ?? [] } catch { categories.value = [] }
}

function openCreate() { form.value = emptyForm(); showForm.value = true }
function openEdit(q) {
  form.value = { id: q.id, category: q.category ?? '', difficulty: q.difficulty || '중', question: q.question, optionA: q.optionA, optionB: q.optionB,
    optionC: q.optionC ?? '', optionD: q.optionD ?? '', correctAnswer: q.correctAnswer, explanation: q.explanation ?? '' }
  showForm.value = true
}

async function submitForm() {
  saving.value = true
  try {
    const { id, ...data } = form.value
    if (id) await quizBankApi.update(id, data)
    else await quizBankApi.create(data)
    showForm.value = false
    load(); loadCategories()
  } catch (e) {
    alert(e || '저장에 실패했습니다.')
  } finally {
    saving.value = false
  }
}

async function confirmDelete(q) {
  if (!confirm(`문제 #${q.id}를 삭제하시겠습니까?`)) return
  try { await quizBankApi.delete(q.id); load(); loadCategories() }
  catch (e) { alert(e || '삭제에 실패했습니다.') }
}

async function downloadTemplate() {
  templateLoading.value = true
  try { await quizBankApi.template() } finally { templateLoading.value = false }
}

async function handleBulkUpload(file, resolve, reject) {
  try {
    const res = await quizBankApi.upload(file)
    const d = res.data ?? {}
    // BulkImportModal 결과 형식({total, success, failed, errors:[{row,message}]})으로 변환
    resolve({
      total: (d.successCount ?? 0) + (d.failCount ?? 0),
      success: d.successCount ?? 0,
      failed: d.failCount ?? 0,
      errors: (d.errors ?? []).map(msg => {
        const m = String(msg).match(/^(\d+)행:\s*(.*)$/)
        return m ? { row: m[1], message: m[2] } : { row: '-', message: msg }
      }),
    })
    load(); loadCategories()
  } catch (e) {
    reject(typeof e === 'string' ? e : '업로드 중 오류가 발생했습니다.')
  }
}

function formatDate(dt) { return dt ? new Date(dt).toLocaleDateString() : '-' }

onMounted(() => { load(); loadCategories() })
</script>
