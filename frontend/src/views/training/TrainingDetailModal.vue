<template>
  <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
    <div class="absolute inset-0 bg-black/40" @click="$emit('close')"></div>

    <div class="relative bg-white rounded-xl shadow-xl w-full max-w-4xl max-h-[92vh] flex flex-col">
      <div class="flex items-start justify-between gap-4 px-5 py-3 border-b shrink-0">
        <div v-if="course" class="min-w-0">
          <div class="flex gap-1.5 mb-1">
            <span :class="course.mandatory ? 'badge-red' : 'badge-gray'">
              {{ course.mandatory ? $t('training.mandatory') : $t('training.optional') }}
            </span>
            <span class="badge-blue">{{ $t(`training.contentType.${course.contentType}`) }}</span>
          </div>
          <h2 class="text-lg font-semibold text-gray-900 truncate">{{ course.title }}</h2>
        </div>
        <div v-else class="text-lg font-semibold text-gray-900">{{ $t('training.title') }}</div>
        <div class="flex items-center gap-2 shrink-0">
          <button v-if="course && isManager" @click="$emit('edit', course.id)" class="btn-secondary text-sm">{{ $t('common.edit') }}</button>
          <button v-if="course && isAdmin" @click="deleteCourse" class="btn-danger text-sm">{{ $t('common.delete') }}</button>
          <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600 p-1">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
      </div>

      <div class="px-5 py-4 overflow-y-auto flex-1 space-y-5">
        <div v-if="loading" class="py-16 text-center text-gray-400">{{ $t('common.loading') }}</div>
        <div v-else-if="!course" class="py-16 text-center text-gray-400">{{ $t('common.noData') }}</div>
        <template v-else>
          <!-- 개요 -->
          <div class="border border-gray-200 rounded-lg p-4">
            <div class="flex items-start justify-between">
              <p v-if="course.description" class="text-gray-600 text-sm">{{ course.description }}</p>
              <div v-if="course.completedByMe" class="text-center shrink-0 ml-4">
                <div :class="course.myScore >= course.passingScore ? 'text-green-600' : 'text-red-600'" class="text-2xl font-bold">
                  {{ course.myScore }}점
                </div>
                <div :class="course.myScore >= course.passingScore ? 'badge-green' : 'badge-red'" class="mt-1">
                  {{ course.myScore >= course.passingScore ? $t('training.passed') : $t('training.failed') }}
                </div>
              </div>
            </div>
            <div class="flex gap-4 mt-3 text-sm text-gray-500">
              <span>합격 기준: {{ course.passingScore }}점</span>
              <span>이수 인원: {{ course.completionCount }}명</span>
            </div>
          </div>

          <!-- 학습 자료 -->
          <div v-if="course.contentUrl && course.contentType !== 'QUIZ_ONLY'" class="border border-gray-200 rounded-lg p-4">
            <h3 class="font-semibold text-gray-900 mb-3 text-sm">학습 자료</h3>
            <a :href="course.contentUrl" target="_blank" class="btn-secondary inline-flex items-center gap-2 text-sm">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"/>
              </svg>
              자료 열기
            </a>
          </div>

          <!-- Quiz -->
          <div v-if="course.questions?.length && !course.completedByMe && !result" class="border border-gray-200 rounded-lg p-4">
            <div class="flex items-center justify-between mb-3">
              <h3 class="font-semibold text-gray-900 text-sm">{{ $t('training.startQuiz') }}</h3>
              <span class="text-sm text-gray-500">{{ currentIdx + 1 }} / {{ course.questions.length }}문항 · 답변 {{ answeredCount }}개</span>
            </div>
            <div class="h-1.5 bg-gray-100 rounded-full overflow-hidden mb-3">
              <div class="h-full bg-primary-500 rounded-full transition-all duration-300"
                :style="{ width: `${(answeredCount / course.questions.length) * 100}%` }"></div>
            </div>
            <div class="flex flex-wrap gap-1.5 mb-6">
              <button v-for="(q, idx) in course.questions" :key="q.id" type="button" @click="currentIdx = idx"
                class="w-8 h-8 rounded-lg text-xs font-semibold border transition-colors"
                :class="idx === currentIdx
                  ? 'border-primary-500 bg-primary-500 text-white'
                  : answers[q.id]
                    ? 'border-primary-200 bg-primary-50 text-primary-600'
                    : 'border-gray-200 text-gray-400 hover:bg-gray-50'">
                {{ idx + 1 }}
              </button>
            </div>
            <div :key="currentQuestion.id">
              <p class="font-medium text-gray-900 mb-3">{{ currentIdx + 1 }}. {{ currentQuestion.question }}</p>
              <div class="space-y-2">
                <label v-for="opt in options(currentQuestion)" :key="opt.key"
                  class="flex items-center gap-3 p-3 rounded-lg border cursor-pointer hover:bg-gray-50 transition-colors"
                  :class="answers[currentQuestion.id] === opt.key ? 'border-primary-500 bg-primary-50' : 'border-gray-200'">
                  <input type="radio" :name="`q${currentQuestion.id}`" :value="opt.key" v-model="answers[currentQuestion.id]" class="hidden" />
                  <div class="w-6 h-6 rounded-full border-2 flex items-center justify-center flex-shrink-0"
                    :class="answers[currentQuestion.id] === opt.key ? 'border-primary-500 bg-primary-500' : 'border-gray-300'">
                    <span v-if="answers[currentQuestion.id] === opt.key" class="text-white text-xs font-bold">{{ opt.key }}</span>
                  </div>
                  <span class="text-sm">{{ opt.text }}</span>
                </label>
              </div>
            </div>
            <p v-if="quizError" class="text-red-600 text-sm mt-4">{{ quizError }}</p>
            <div class="mt-6 flex items-center justify-between">
              <button type="button" @click="currentIdx--" :disabled="currentIdx === 0" class="btn-secondary disabled:opacity-40">← 이전</button>
              <span v-if="isLastQuestion && !allAnswered" class="text-xs text-amber-600">
                답변하지 않은 문항이 {{ course.questions.length - answeredCount }}개 있습니다
              </span>
              <button v-if="!isLastQuestion" type="button" @click="currentIdx++" class="btn-primary">다음 →</button>
              <button v-else @click="submitQuiz" class="btn-primary" :disabled="submitting || !allAnswered">
                {{ submitting ? $t('common.loading') : $t('training.submitQuiz') }}
              </button>
            </div>
          </div>

          <!-- Result -->
          <div v-if="result" class="border border-gray-200 rounded-lg text-center py-8">
            <div class="text-5xl font-bold mb-2" :class="result.passed ? 'text-green-600' : 'text-red-600'">{{ result.score }}점</div>
            <div class="text-lg font-semibold mb-1" :class="result.passed ? 'text-green-600' : 'text-red-600'">
              {{ result.passed ? $t('training.passed') : $t('training.failed') }}
            </div>
            <p class="text-gray-500 mb-4">{{ result.correctCount }} / {{ result.totalCount }} 정답</p>
            <button v-if="!result.passed" @click="retryQuiz" class="btn-secondary">다시 도전</button>
          </div>

          <!-- 오답 리뷰 -->
          <div v-if="result && wrongQuestions.length" class="border border-gray-200 rounded-lg p-4">
            <h3 class="font-semibold text-gray-900 mb-4 text-sm">오답 리뷰 ({{ wrongQuestions.length }}문항)</h3>
            <div class="space-y-6">
              <div v-for="q in wrongQuestions" :key="q.id">
                <p class="font-medium text-gray-900 mb-2">{{ q.number }}. {{ q.question }}</p>
                <div class="space-y-1.5">
                  <div v-for="opt in options(q)" :key="opt.key"
                    class="flex items-center gap-3 p-2.5 rounded-lg border text-sm"
                    :class="opt.key === q.correctAnswer ? 'border-green-500 bg-green-50'
                      : opt.key === q.myAnswer ? 'border-red-400 bg-red-50' : 'border-gray-200'">
                    <div class="w-5 h-5 rounded-full border-2 flex items-center justify-center flex-shrink-0 text-xs font-bold"
                      :class="opt.key === q.correctAnswer ? 'border-green-500 bg-green-500 text-white'
                        : opt.key === q.myAnswer ? 'border-red-400 bg-red-400 text-white' : 'border-gray-300 text-gray-400'">
                      {{ opt.key }}
                    </div>
                    <span class="flex-1">{{ opt.text }}</span>
                    <span v-if="opt.key === q.correctAnswer" class="text-xs font-semibold text-green-600 flex-shrink-0">정답</span>
                    <span v-else-if="opt.key === q.myAnswer" class="text-xs font-semibold text-red-500 flex-shrink-0">내가 선택한 답</span>
                  </div>
                </div>
                <div v-if="q.explanation" class="mt-2 p-3 bg-blue-50 rounded-lg text-sm text-blue-800">
                  <span class="font-semibold">해설</span> — {{ q.explanation }}
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>

      <div class="flex justify-end px-5 py-3 border-t shrink-0">
        <button type="button" @click="$emit('close')" class="btn-secondary text-sm">닫기</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { trainingApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const props = defineProps({
  open: { type: Boolean, default: false },
  itemId: { type: [Number, String], default: null }
})
const emit = defineEmits(['close', 'edit', 'changed'])

const auth = useAuthStore()
const isManager = auth.isManager
const isAdmin = auth.isAdmin

const course = ref(null)
const loading = ref(true)
const answers = ref({})
const submitting = ref(false)
const quizError = ref('')
const result = ref(null)
const currentIdx = ref(0)

const currentQuestion = computed(() => course.value?.questions?.[currentIdx.value])
const isLastQuestion = computed(() => currentIdx.value >= (course.value?.questions?.length ?? 0) - 1)
const answeredCount = computed(() => course.value?.questions?.filter(q => answers.value[q.id]).length ?? 0)
const allAnswered = computed(() => course.value?.questions?.every(q => answers.value[q.id]) ?? false)

const wrongQuestions = computed(() => {
  if (!result.value || !course.value?.questions) return []
  return course.value.questions
    .map((q, idx) => ({ ...q, number: idx + 1, myAnswer: answers.value[q.id] || null }))
    .filter(q => (q.myAnswer || '').toUpperCase() !== (q.correctAnswer || '').toUpperCase())
})

function options(q) {
  const opts = [{ key: 'A', text: q.optionA }, { key: 'B', text: q.optionB }]
  if (q.optionC) opts.push({ key: 'C', text: q.optionC })
  if (q.optionD) opts.push({ key: 'D', text: q.optionD })
  return opts
}

watch(() => props.open, async (open) => {
  if (!open || !props.itemId) return
  loading.value = true
  course.value = null
  answers.value = {}
  result.value = null
  quizError.value = ''
  currentIdx.value = 0
  try { course.value = (await trainingApi.get(props.itemId)).data }
  catch { /* course stays null */ }
  finally { loading.value = false }
})

async function submitQuiz() {
  submitting.value = true
  quizError.value = ''
  try {
    const res = await trainingApi.submitQuiz(props.itemId, { answers: answers.value })
    result.value = res.data
    if (res.data.passed) {
      course.value.completedByMe = true
      course.value.myScore = res.data.score
      emit('changed')
    }
  } catch (e) {
    quizError.value = typeof e === 'string' ? e : 'Failed to submit'
  } finally {
    submitting.value = false
  }
}

function retryQuiz() {
  result.value = null
  answers.value = {}
  quizError.value = ''
  currentIdx.value = 0
}

async function deleteCourse() {
  if (!confirm('교육을 삭제하시겠습니까?')) return
  try {
    await trainingApi.delete(props.itemId)
    emit('changed')
    emit('close')
  } catch (e) {
    alert(typeof e === 'string' ? e : '삭제에 실패했습니다')
  }
}
</script>
