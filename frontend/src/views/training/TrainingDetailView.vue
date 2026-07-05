<template>
  <div class="p-8 max-w-4xl mx-auto">
    <div v-if="loading" class="text-center text-gray-400 py-16">{{ $t('common.loading') }}</div>
    <div v-else-if="!course" class="text-center text-gray-400 py-16">{{ $t('common.noData') }}</div>
    <template v-else>
      <div class="flex items-center justify-between mb-4">
        <RouterLink to="/training" class="text-sm text-primary-600 hover:underline inline-block">← {{ $t('common.back') }}</RouterLink>
        <div class="flex gap-2">
          <RouterLink v-if="isManager" :to="`/training/${course.id}/edit`" class="btn-secondary text-sm">{{ $t('common.edit') }}</RouterLink>
          <button v-if="isAdmin" @click="deleteCourse" class="btn-danger text-sm">{{ $t('common.delete') }}</button>
        </div>
      </div>

      <div class="card mb-6">
        <div class="flex items-start justify-between mb-4">
          <div>
            <div class="flex gap-2 mb-2">
              <span :class="course.mandatory ? 'badge-red' : 'badge-gray'">
                {{ course.mandatory ? $t('training.mandatory') : $t('training.optional') }}
              </span>
              <span class="badge-blue">{{ $t(`training.contentType.${course.contentType}`) }}</span>
            </div>
            <h1 class="text-2xl font-bold text-gray-900">{{ course.title }}</h1>
          </div>
          <div v-if="course.completedByMe" class="text-center">
            <div :class="course.myScore >= course.passingScore ? 'text-green-600' : 'text-red-600'" class="text-3xl font-bold">
              {{ course.myScore }}점
            </div>
            <div :class="course.myScore >= course.passingScore ? 'badge-green' : 'badge-red'" class="mt-1">
              {{ course.myScore >= course.passingScore ? $t('training.passed') : $t('training.failed') }}
            </div>
          </div>
        </div>
        <p v-if="course.description" class="text-gray-600 text-sm">{{ course.description }}</p>
        <div class="flex gap-4 mt-4 text-sm text-gray-500">
          <span>합격 기준: {{ course.passingScore }}점</span>
          <span>이수 인원: {{ course.completionCount }}명</span>
        </div>
      </div>

      <!-- Content link -->
      <div v-if="course.contentUrl && course.contentType !== 'QUIZ_ONLY'" class="card mb-6">
        <h2 class="font-semibold text-gray-900 mb-3">학습 자료</h2>
        <a :href="course.contentUrl" target="_blank" class="btn-secondary inline-flex items-center gap-2">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"/>
          </svg>
          자료 열기
        </a>
      </div>

      <!-- Quiz: result가 있으면(제출 직후) 숨김, 이수 완료 상태에서도 숨김 -->
      <div v-if="course.questions?.length && !course.completedByMe && !result" class="card">
        <div class="flex items-center justify-between mb-3">
          <h2 class="font-semibold text-gray-900">{{ $t('training.startQuiz') }}</h2>
          <span class="text-sm text-gray-500">{{ currentIdx + 1 }} / {{ course.questions.length }}문항 · 답변 {{ answeredCount }}개</span>
        </div>

        <!-- 진행 바 -->
        <div class="h-1.5 bg-gray-100 rounded-full overflow-hidden mb-3">
          <div class="h-full bg-primary-500 rounded-full transition-all duration-300"
            :style="{ width: `${(answeredCount / course.questions.length) * 100}%` }"></div>
        </div>

        <!-- 문항 번호 이동 (답변 여부 표시) -->
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

        <!-- 현재 문항 -->
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

        <!-- 이전/다음 이동, 마지막 문항에서 제출 -->
        <div class="mt-6 flex items-center justify-between">
          <button type="button" @click="currentIdx--" :disabled="currentIdx === 0" class="btn-secondary disabled:opacity-40">
            ← 이전
          </button>
          <span v-if="isLastQuestion && !allAnswered" class="text-xs text-amber-600">
            답변하지 않은 문항이 {{ course.questions.length - answeredCount }}개 있습니다
          </span>
          <button v-if="!isLastQuestion" type="button" @click="currentIdx++" class="btn-primary">
            다음 →
          </button>
          <button v-else @click="submitQuiz" class="btn-primary" :disabled="submitting || !allAnswered">
            {{ submitting ? $t('common.loading') : $t('training.submitQuiz') }}
          </button>
        </div>
      </div>

      <!-- Result -->
      <div v-if="result" class="card text-center py-8">
        <div class="text-5xl font-bold mb-2" :class="result.passed ? 'text-green-600' : 'text-red-600'">
          {{ result.score }}점
        </div>
        <div class="text-lg font-semibold mb-1" :class="result.passed ? 'text-green-600' : 'text-red-600'">
          {{ result.passed ? $t('training.passed') : $t('training.failed') }}
        </div>
        <p class="text-gray-500 mb-4">{{ result.correctCount }} / {{ result.totalCount }} 정답</p>
        <button v-if="!result.passed" @click="retryQuiz" class="btn-secondary">다시 도전</button>
      </div>

      <!-- 오답 리뷰: 제출 직후 틀린 문항의 선택 답·정답·해설 확인 -->
      <div v-if="result && wrongQuestions.length" class="card mt-6">
        <h2 class="font-semibold text-gray-900 mb-4">오답 리뷰 ({{ wrongQuestions.length }}문항)</h2>
        <div class="space-y-6">
          <div v-for="q in wrongQuestions" :key="q.id">
            <p class="font-medium text-gray-900 mb-2">{{ q.number }}. {{ q.question }}</p>
            <div class="space-y-1.5">
              <div v-for="opt in options(q)" :key="opt.key"
                class="flex items-center gap-3 p-2.5 rounded-lg border text-sm"
                :class="opt.key === q.correctAnswer
                  ? 'border-green-500 bg-green-50'
                  : opt.key === q.myAnswer
                    ? 'border-red-400 bg-red-50'
                    : 'border-gray-200'">
                <div class="w-5 h-5 rounded-full border-2 flex items-center justify-center flex-shrink-0 text-xs font-bold"
                  :class="opt.key === q.correctAnswer
                    ? 'border-green-500 bg-green-500 text-white'
                    : opt.key === q.myAnswer
                      ? 'border-red-400 bg-red-400 text-white'
                      : 'border-gray-300 text-gray-400'">
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
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { trainingApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const isManager = auth.isManager
const isAdmin = auth.isAdmin

const route = useRoute()
const router = useRouter()
const course = ref(null)
const loading = ref(true)
const answers = ref({})
const submitting = ref(false)
const quizError = ref('')
const result = ref(null)

const currentIdx = ref(0)
const currentQuestion = computed(() => course.value?.questions?.[currentIdx.value])
const isLastQuestion = computed(() => currentIdx.value >= (course.value?.questions?.length ?? 0) - 1)
const answeredCount = computed(() =>
  course.value?.questions?.filter(q => answers.value[q.id]).length ?? 0
)
const allAnswered = computed(() =>
  course.value?.questions?.every(q => answers.value[q.id]) ?? false
)

// 제출 직후 오답 리뷰용 — 백엔드 채점(equalsIgnoreCase)과 동일 기준으로 비교
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

async function loadCourse() {
  loading.value = true
  course.value = null
  answers.value = {}
  result.value = null
  currentIdx.value = 0
  try {
    const res = await trainingApi.get(route.params.id)
    course.value = res.data
  } catch {
    // 코스 로드 실패 — course stays null, 빈 화면 대신 에러 메시지 표시
  } finally {
    loading.value = false
  }
}

async function submitQuiz() {
  submitting.value = true
  quizError.value = ''
  try {
    const res = await trainingApi.submitQuiz(route.params.id, { answers: answers.value })
    result.value = res.data
    if (res.data.passed) {
      course.value.completedByMe = true
      course.value.myScore = res.data.score
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
    await trainingApi.delete(route.params.id)
    router.push('/training')
  } catch (e) {
    alert(typeof e === 'string' ? e : '삭제에 실패했습니다')
  }
}

// 라우트 파라미터 변경 시 재로드 (same-component navigation 대응)
watch(() => route.params.id, loadCourse, { immediate: true })
</script>
