<template>
  <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
    <div class="absolute inset-0 bg-black/40" @click="$emit('close')"></div>

    <div class="relative bg-white rounded-xl shadow-xl w-full max-w-4xl max-h-[92vh] flex flex-col">
      <div class="flex items-center justify-between px-5 py-3 border-b shrink-0">
        <h2 class="text-lg font-semibold text-gray-900">{{ isEdit ? $t('training.edit') : $t('training.create') }}</h2>
        <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <form id="trainingForm" @submit.prevent="handleSubmit" class="px-5 py-4 overflow-y-auto flex-1 space-y-5">
        <!-- 기본 정보 -->
        <div class="border border-gray-200 rounded-lg p-4 space-y-4">
          <h3 class="font-semibold text-gray-900 text-sm">기본 정보</h3>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="md:col-span-2">
              <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('common.title') }} *</label>
              <input v-model="form.title" type="text" class="input w-full" required />
            </div>
            <div class="md:col-span-2">
              <label class="block text-sm font-medium text-gray-700 mb-1">설명</label>
              <textarea v-model="form.description" class="input w-full" rows="2" />
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">콘텐츠 유형 *</label>
              <select v-model="form.contentType" class="input w-full" required>
                <option value="QUIZ_ONLY">{{ $t('training.contentType.QUIZ_ONLY') }}</option>
                <option value="VIDEO">{{ $t('training.contentType.VIDEO') }}</option>
                <option value="DOCUMENT">{{ $t('training.contentType.DOCUMENT') }}</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">합격 점수 *</label>
              <input v-model.number="form.passingScore" type="number" min="0" max="100" class="input w-full" required />
            </div>
            <div v-if="form.contentType !== 'QUIZ_ONLY'" class="md:col-span-2">
              <label class="block text-sm font-medium text-gray-700 mb-1">콘텐츠 URL</label>
              <input v-model="form.contentUrl" type="url" class="input w-full" placeholder="https://" />
            </div>
          </div>
          <div class="flex items-center gap-2">
            <input id="mandatoryModal" v-model="form.mandatory" type="checkbox" class="w-4 h-4 rounded border-gray-300 text-primary-600" />
            <label for="mandatoryModal" class="text-sm text-gray-700">필수 교육으로 지정</label>
          </div>
        </div>

        <!-- 퀴즈 문항 -->
        <div class="border border-gray-200 rounded-lg p-4 space-y-4">
          <div class="flex items-center justify-between">
            <h3 class="font-semibold text-gray-900 text-sm">퀴즈 문항 ({{ questions.length }}개)</h3>
            <div class="flex gap-2">
              <button type="button" @click="openBankPicker" class="btn-secondary text-sm flex items-center gap-1.5">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M8 14v3m4-3v3m4-3v3M3 21h18M3 10h18M3 7l9-4 9 4M4 10h16v11H4V10z"/>
                </svg>
                문제은행에서 가져오기
              </button>
              <button type="button" @click="addQuestion" class="btn-secondary text-sm">+ 문항 추가</button>
            </div>
          </div>

          <div v-if="!questions.length" class="text-center text-gray-400 py-6 text-sm">문항을 추가하세요.</div>

          <div v-for="(q, idx) in questions" :key="idx" class="border border-gray-200 rounded-lg p-4 space-y-3">
            <div class="flex items-start justify-between">
              <span class="text-sm font-medium text-gray-700">문항 {{ idx + 1 }}</span>
              <button type="button" @click="removeQuestion(idx)" class="text-red-500 hover:text-red-700 text-sm">삭제</button>
            </div>
            <div>
              <label class="block text-xs text-gray-500 mb-1">문제 *</label>
              <input v-model="q.question" type="text" class="input w-full" required />
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-xs text-gray-500 mb-1">A 선택지 *</label>
                <input v-model="q.optionA" type="text" class="input w-full" required />
              </div>
              <div>
                <label class="block text-xs text-gray-500 mb-1">B 선택지 *</label>
                <input v-model="q.optionB" type="text" class="input w-full" required />
              </div>
              <div>
                <label class="block text-xs text-gray-500 mb-1">C 선택지</label>
                <input v-model="q.optionC" type="text" class="input w-full" />
              </div>
              <div>
                <label class="block text-xs text-gray-500 mb-1">D 선택지</label>
                <input v-model="q.optionD" type="text" class="input w-full" />
              </div>
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="block text-xs text-gray-500 mb-1">난이도 *</label>
                <select v-model="q.difficulty" class="input w-full">
                  <option value="상">상</option>
                  <option value="중">중</option>
                  <option value="하">하</option>
                </select>
              </div>
              <div>
                <label class="block text-xs text-gray-500 mb-1">정답 *</label>
                <select v-model="q.correctAnswer" class="input w-full" required>
                  <option value="A">A</option>
                  <option value="B">B</option>
                  <option value="C" :disabled="!q.optionC">C</option>
                  <option value="D" :disabled="!q.optionD">D</option>
                </select>
              </div>
            </div>
            <div>
              <label class="block text-xs text-gray-500 mb-1">해설 (선택)</label>
              <textarea v-model="q.explanation" rows="2" class="input w-full" placeholder="정답 해설을 입력하세요"></textarea>
            </div>
          </div>
        </div>

        <p v-if="error" class="text-red-600 text-sm">{{ error }}</p>
      </form>

      <div class="flex justify-end gap-3 px-5 py-3 border-t shrink-0">
        <button type="button" @click="$emit('close')" class="btn-secondary text-sm">{{ $t('common.cancel') }}</button>
        <button type="submit" form="trainingForm" class="btn-primary text-sm" :disabled="loading">
          {{ loading ? $t('common.loading') : $t('common.save') }}
        </button>
      </div>
    </div>

    <!-- 문제은행 피커 모달 (폼 모달 위에 표시) -->
    <div v-if="showBank" class="fixed inset-0 z-[60] flex items-center justify-center bg-black/40 p-4">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-3xl p-6 flex flex-col" style="max-height: 85vh">
        <div class="flex items-center justify-between mb-3">
          <h2 class="text-lg font-bold">문제은행에서 가져오기</h2>
          <button @click="showBank = false" class="text-gray-400 hover:text-gray-600">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <div class="flex gap-2 mb-2">
          <select v-model="bankFilters.category" @change="loadBank(0)" class="input w-40 text-sm">
            <option value="">전체 분류</option>
            <option v-for="c in bankCategories" :key="c" :value="c">{{ c }}</option>
          </select>
          <select v-model="bankFilters.difficulty" @change="loadBank(0)" class="input w-28 text-sm">
            <option value="">전체 난이도</option>
            <option value="상">상</option>
            <option value="중">중</option>
            <option value="하">하</option>
          </select>
          <input v-model="bankFilters.keyword" @keyup.enter="loadBank(0)" placeholder="문제 검색 후 Enter" class="input flex-1 text-sm" />
          <label class="flex items-center gap-1.5 text-xs text-gray-500 whitespace-nowrap px-1 cursor-pointer">
            <input type="checkbox" class="w-3.5 h-3.5 rounded"
              :checked="selectablePage.length > 0 && selectablePage.every(q => bankSelected.has(q.id))"
              @change="toggleBankPageAll($event.target.checked)" />
            현재 페이지 전체 선택
          </label>
        </div>
        <div class="flex items-center gap-2 mb-3 px-3 py-2 rounded-lg bg-indigo-50/70 border border-indigo-100 flex-wrap">
          <svg class="w-4 h-4 text-indigo-500 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4h5l2 3h9v13H4V4zm7 7l3 3m0-3l-3 3"/>
          </svg>
          <span class="text-xs text-gray-600 whitespace-nowrap">총</span>
          <input v-model.number="randomTotalTarget" type="number" min="0" max="500" class="input w-16 !py-1 text-sm text-center" />
          <span class="text-xs text-gray-600 whitespace-nowrap">문항을</span>
          <span class="flex items-center gap-1">
            <span class="text-[11px] font-bold px-1.5 py-0.5 rounded bg-red-100 text-red-600">상</span>
            <input v-model.number="randomCounts.상" type="number" min="0" max="500" class="input w-16 !py-1 text-sm text-center" />
          </span>
          <span class="flex items-center gap-1">
            <span class="text-[11px] font-bold px-1.5 py-0.5 rounded bg-amber-100 text-amber-700">중</span>
            <input v-model.number="randomCounts.중" type="number" min="0" max="500" class="input w-16 !py-1 text-sm text-center" />
          </span>
          <span class="flex items-center gap-1">
            <span class="text-[11px] font-bold px-1.5 py-0.5 rounded bg-green-100 text-green-700">하</span>
            <input v-model.number="randomCounts.하" type="number" min="0" max="500" class="input w-16 !py-1 text-sm text-center" />
          </span>
          <button @click="randomPick" :disabled="randomPicking || randomTotal === 0 || !totalMatches"
            class="px-3 py-1 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold disabled:opacity-50">
            {{ randomPicking ? '선택 중...' : '랜덤 선택' }}
          </button>
          <span v-if="!totalMatches" class="text-xs text-red-500 whitespace-nowrap">
            상·중·하 합계({{ randomTotal }})가 총 문항 수({{ randomTotalTarget || 0 }})와 일치해야 합니다
          </span>
          <span v-else-if="randomMsg" class="text-xs text-indigo-600 truncate">{{ randomMsg }}</span>
          <span class="ml-auto text-[11px] text-gray-400 whitespace-nowrap">분류·검색어 조건 적용 · 이미 추가된 문제 제외</span>
        </div>
        <div class="flex-1 overflow-y-auto border border-gray-100 rounded-lg">
          <div v-if="bankLoading" class="text-center py-8 text-gray-400 text-sm">로딩 중...</div>
          <div v-else-if="bankQuestions.length === 0" class="text-center py-8 text-gray-400 text-sm">
            문제가 없습니다. 관리 &gt; 문제은행 관리에서 먼저 등록하세요.
          </div>
          <label v-else v-for="q in bankQuestions" :key="q.id"
            class="flex items-start gap-3 px-4 py-2.5 border-b border-gray-50"
            :class="isAdded(q) ? 'opacity-50 cursor-not-allowed bg-gray-50' : 'hover:bg-gray-50 cursor-pointer'">
            <input type="checkbox" class="w-4 h-4 mt-0.5 rounded flex-shrink-0"
              :disabled="isAdded(q)" :checked="bankSelected.has(q.id)" @change="toggleBankSelect(q)" />
            <span v-if="q.category" class="flex-shrink-0 text-[10px] font-medium px-1.5 py-0.5 rounded bg-indigo-100 text-indigo-700 mt-0.5">{{ q.category }}</span>
            <span class="flex-shrink-0 text-[10px] font-bold px-1.5 py-0.5 rounded mt-0.5" :class="difficultyClass(q.difficulty)">{{ q.difficulty || '중' }}</span>
            <span class="text-sm text-gray-700">{{ q.question }}</span>
            <span v-if="isAdded(q)" class="flex-shrink-0 text-[10px] font-bold px-1.5 py-0.5 rounded bg-gray-200 text-gray-500 mt-0.5">추가됨</span>
            <span class="ml-auto flex-shrink-0 text-xs font-mono font-bold text-primary-600">{{ q.correctAnswer }}</span>
          </label>
        </div>
        <div class="flex items-center justify-between mt-3">
          <div class="flex items-center gap-1">
            <button @click="loadBank(bankPage - 1)" :disabled="bankPage === 0"
              class="px-2 py-1 rounded border text-xs text-gray-500 border-gray-300 disabled:opacity-30">‹</button>
            <span class="px-2 text-xs text-gray-500">{{ bankPage + 1 }} / {{ Math.max(bankTotalPages, 1) }}</span>
            <button @click="loadBank(bankPage + 1)" :disabled="bankPage >= bankTotalPages - 1"
              class="px-2 py-1 rounded border text-xs text-gray-500 border-gray-300 disabled:opacity-30">›</button>
          </div>
          <div class="flex gap-2">
            <button @click="showBank = false" class="btn-secondary text-sm">취소</button>
            <button @click="addFromBank" :disabled="bankSelected.size === 0" class="btn-primary text-sm">
              선택한 {{ bankSelected.size }}개 문항 추가
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { trainingApi, quizBankApi } from '@/api'

const props = defineProps({
  open: { type: Boolean, default: false },
  editId: { type: [Number, String], default: null }
})
const emit = defineEmits(['close', 'saved'])

const isEdit = computed(() => !!props.editId)

function emptyForm() {
  return { title: '', description: '', contentType: 'QUIZ_ONLY', contentUrl: '', passingScore: 70, mandatory: false }
}
const form = ref(emptyForm())
const questions = ref([])
const loading = ref(false)
const error = ref('')

watch(() => props.open, async (open) => {
  if (!open) return
  error.value = ''
  showBank.value = false
  if (props.editId) {
    loading.value = true
    try {
      const course = (await trainingApi.get(props.editId)).data
      form.value = {
        title: course.title, description: course.description || '', contentType: course.contentType,
        contentUrl: course.contentUrl || '', passingScore: course.passingScore, mandatory: course.mandatory
      }
      questions.value = (course.questions || []).map(q => ({
        question: q.question, optionA: q.optionA, optionB: q.optionB,
        optionC: q.optionC || '', optionD: q.optionD || '',
        correctAnswer: q.correctAnswer || 'A', difficulty: q.difficulty || '중',
        explanation: q.explanation || '', sortOrder: q.sortOrder
      }))
    } catch {
      error.value = '교육 정보를 불러오지 못했습니다.'
    } finally {
      loading.value = false
    }
  } else {
    form.value = emptyForm()
    questions.value = []
  }
})

function addQuestion() {
  questions.value.push({
    question: '', optionA: '', optionB: '', optionC: '', optionD: '',
    correctAnswer: 'A', difficulty: '중', explanation: '', sortOrder: questions.value.length
  })
}
function removeQuestion(idx) {
  questions.value.splice(idx, 1)
  questions.value.forEach((q, i) => { q.sortOrder = i })
}

// ── 문제은행 피커 ────────────────────────────────────
const showBank = ref(false)
const bankLoading = ref(false)
const bankQuestions = ref([])
const bankCategories = ref([])
const bankFilters = ref({ category: '', difficulty: '', keyword: '' })
const bankPage = ref(0)
const bankTotalPages = ref(0)
const bankSelected = ref(new Map())
const randomCounts = ref({ 상: 2, 중: 6, 하: 2 })
const randomTotalTarget = ref(10)
const randomPicking = ref(false)
const randomMsg = ref('')

const randomTotal = computed(() =>
  (randomCounts.value.상 || 0) + (randomCounts.value.중 || 0) + (randomCounts.value.하 || 0)
)
const totalMatches = computed(() => randomTotal.value === (randomTotalTarget.value || 0))

function difficultyClass(d) {
  return { '상': 'bg-red-100 text-red-600', '중': 'bg-amber-100 text-amber-700', '하': 'bg-green-100 text-green-700' }[d] || 'bg-amber-100 text-amber-700'
}

const existingQuestions = computed(() =>
  new Set(questions.value.map(q => (q.question ?? '').trim()).filter(Boolean))
)
function isAdded(q) { return existingQuestions.value.has((q.question ?? '').trim()) }
const selectablePage = computed(() => bankQuestions.value.filter(q => !isAdded(q)))

async function openBankPicker() {
  showBank.value = true
  bankSelected.value = new Map()
  try { bankCategories.value = (await quizBankApi.categories()).data ?? [] } catch { bankCategories.value = [] }
  loadBank(0)
}

async function loadBank(p) {
  bankPage.value = Math.max(0, p)
  bankLoading.value = true
  try {
    const res = await quizBankApi.list({
      page: bankPage.value, size: 15,
      ...(bankFilters.value.category && { category: bankFilters.value.category }),
      ...(bankFilters.value.difficulty && { difficulty: bankFilters.value.difficulty }),
      ...(bankFilters.value.keyword && { keyword: bankFilters.value.keyword }),
    })
    const d = res.data
    bankQuestions.value = d?.content || []
    bankTotalPages.value = d?.page?.totalPages ?? d?.totalPages ?? 0
  } catch {
    bankQuestions.value = []
  } finally {
    bankLoading.value = false
  }
}

function toggleBankSelect(q) {
  if (isAdded(q)) return
  const next = new Map(bankSelected.value)
  if (next.has(q.id)) next.delete(q.id)
  else next.set(q.id, q)
  bankSelected.value = next
}
function toggleBankPageAll(checked) {
  const next = new Map(bankSelected.value)
  for (const q of selectablePage.value) {
    if (checked) next.set(q.id, q)
    else next.delete(q.id)
  }
  bankSelected.value = next
}

async function fetchBankPool(difficulty) {
  const all = []
  let p = 0, totalPages = 1
  while (p < totalPages && all.length < 1000) {
    const res = await quizBankApi.list({
      page: p, size: 100, difficulty,
      ...(bankFilters.value.category && { category: bankFilters.value.category }),
      ...(bankFilters.value.keyword && { keyword: bankFilters.value.keyword }),
    })
    const d = res.data
    all.push(...(d?.content || []))
    totalPages = d?.page?.totalPages ?? d?.totalPages ?? 1
    p++
  }
  return all
}
function shuffle(arr) {
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[arr[i], arr[j]] = [arr[j], arr[i]]
  }
  return arr
}
async function randomPick() {
  randomPicking.value = true
  randomMsg.value = ''
  try {
    const next = new Map(bankSelected.value)
    const summary = []
    const shortages = []
    for (const diff of ['상', '중', '하']) {
      const want = Math.max(0, Math.min(500, randomCounts.value[diff] || 0))
      if (want === 0) continue
      const pool = await fetchBankPool(diff)
      const eligible = shuffle(pool.filter(q => !isAdded(q) && !next.has(q.id)))
      const picked = eligible.slice(0, want)
      for (const q of picked) next.set(q.id, q)
      summary.push(`${diff}${picked.length}`)
      if (picked.length < want) shortages.push(`${diff}는 ${picked.length}개만 가능`)
    }
    bankSelected.value = next
    randomMsg.value = summary.length
      ? `랜덤 선택 완료 (${summary.join('·')})${shortages.length ? ' — ' + shortages.join(', ') : ''}`
      : '선택할 문항 수를 입력하세요'
    setTimeout(() => { randomMsg.value = '' }, 5000)
  } catch {
    randomMsg.value = '랜덤 선택 중 오류가 발생했습니다'
  } finally {
    randomPicking.value = false
  }
}
function addFromBank() {
  const existing = new Set(existingQuestions.value)
  for (const q of bankSelected.value.values()) {
    const key = (q.question ?? '').trim()
    if (existing.has(key)) continue
    questions.value.push({
      question: q.question, optionA: q.optionA, optionB: q.optionB,
      optionC: q.optionC || '', optionD: q.optionD || '',
      correctAnswer: q.correctAnswer || 'A', difficulty: q.difficulty || '중',
      explanation: q.explanation || '', sortOrder: questions.value.length,
    })
  }
  showBank.value = false
}

async function handleSubmit() {
  loading.value = true
  error.value = ''
  try {
    const payload = {
      ...form.value,
      contentUrl: form.value.contentUrl || null,
      questions: questions.value.map((q, i) => ({
        question: q.question, optionA: q.optionA, optionB: q.optionB,
        optionC: q.optionC || null, optionD: q.optionD || null,
        correctAnswer: q.correctAnswer, difficulty: q.difficulty || '중',
        explanation: q.explanation || null, sortOrder: i
      }))
    }
    if (isEdit.value) await trainingApi.update(props.editId, payload)
    else await trainingApi.create(payload)
    emit('saved')
  } catch (e) {
    error.value = typeof e === 'string' ? e : '저장에 실패했습니다.'
  } finally {
    loading.value = false
  }
}
</script>
