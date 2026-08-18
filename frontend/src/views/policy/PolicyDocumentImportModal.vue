<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
    <div class="absolute inset-0 bg-black/40" @click="$emit('close')"></div>

    <div class="relative bg-white rounded-xl shadow-xl w-full max-w-3xl max-h-[92vh] flex flex-col">
      <div class="flex items-center justify-between px-5 py-3 border-b shrink-0">
        <div>
          <h2 class="text-lg font-semibold text-gray-900">지침 문서 등록</h2>
          <p class="text-xs text-gray-500 mt-0.5">
            지침 문서를 올리면 <b>지침 &gt; 장(章) &gt; 조(條)</b> 구조로 나눠 정책으로 등록합니다.
          </p>
        </div>
        <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <div class="px-5 py-4 overflow-y-auto flex-1 space-y-4">
        <!-- 파일 선택 -->
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">지침 문서 *</label>
          <input ref="fileInput" type="file" accept=".pdf,.docx,.txt,.md"
            class="input w-full text-sm" @change="onFileChange" />
          <p class="text-xs text-gray-400 mt-1">
            PDF · DOCX · TXT · MD (최대 {{ MAX_MB }}MB). 목차·쪽번호·머리말은 자동으로 제외합니다.
            스캔 이미지로만 된 PDF 는 글자를 읽을 수 없습니다.
          </p>
        </div>

        <!-- 공통 옵션 -->
        <div class="grid grid-cols-2 gap-4">
          <div class="col-span-2">
            <label class="block text-sm font-medium text-gray-700 mb-1">지침명</label>
            <input v-model="options.guidelineName" type="text" class="input w-full"
              placeholder="비워두면 문서 제목 또는 파일명에서 자동 인식" />
            <p class="text-xs text-gray-400 mt-1">
              장별 정책 제목이 <span class="font-mono">지침명 - 제N장 장제목</span> 으로 만들어집니다.
            </p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('policy.category') }}</label>
            <select v-model="options.category" class="input w-full">
              <option v-for="c in categories" :key="c" :value="c">{{ $t(`policy.category_label.${c}`) }}</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('common.status') }}</label>
            <select v-model="options.status" class="input w-full">
              <option v-for="s in statuses" :key="s" :value="s">{{ $t(`policy.status.${s}`) }}</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('policy.version') }}</label>
            <input v-model="options.version" type="text" class="input w-full" placeholder="1.0" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('policy.effectiveDate') }}</label>
            <input v-model="options.effectiveDate" type="date" class="input w-full" />
          </div>
        </div>

        <!-- 분석 결과 -->
        <div v-if="result" class="border rounded-lg overflow-hidden">
          <div class="px-4 py-2.5 bg-gray-50 border-b flex items-center justify-between flex-wrap gap-2">
            <p class="text-sm">
              <span class="font-semibold text-gray-900">{{ result.guidelineName }}</span>
              <span class="text-gray-500 ml-2">장 {{ result.chapters.length }}개 · 조 {{ result.articleCount }}개</span>
            </p>
            <p v-if="!result.dryRun" class="text-xs font-semibold text-green-700">
              신규 {{ result.created }}건 · 갱신 {{ result.updated }}건 등록 완료
            </p>
            <p v-else class="text-xs text-blue-700">미리보기 — 아직 저장되지 않았습니다</p>
          </div>

          <div class="max-h-64 overflow-y-auto divide-y divide-gray-50">
            <div v-for="(c, i) in result.chapters" :key="i" class="px-4 py-2 flex items-center gap-2">
              <span class="text-[10px] px-1.5 py-0.5 rounded font-bold flex-shrink-0"
                :class="c.existing ? 'bg-amber-100 text-amber-800' : 'bg-blue-100 text-blue-800'">
                {{ c.existing ? '갱신' : '신규' }}
              </span>
              <span class="text-sm text-gray-800 truncate flex-1 min-w-0" :title="c.title">{{ c.title }}</span>
              <span class="text-xs text-gray-400 flex-shrink-0">조 {{ c.articleCount }}개</span>
            </div>
          </div>

          <div v-if="result.warnings?.length" class="px-4 py-2.5 bg-amber-50 border-t border-amber-100">
            <p v-for="(w, i) in result.warnings" :key="i" class="text-xs text-amber-800">· {{ w }}</p>
          </div>
        </div>

        <div v-if="result?.chapters?.some(c => c.existing)" class="text-xs text-gray-500 leading-relaxed">
          이미 있는 장은 <b>본문만 갱신</b>합니다. 조는 조 표기 기준으로 재사용되므로
          ISMS-P 통제항목에 걸어둔 조 매핑은 유지됩니다.
        </div>

        <p v-if="error" class="text-sm text-red-600 whitespace-pre-line">{{ error }}</p>
      </div>

      <div class="flex justify-end gap-3 px-5 py-3 border-t shrink-0">
        <button type="button" @click="$emit('close')" class="btn-secondary text-sm">{{ $t('common.close') }}</button>
        <button type="button" @click="run(true)" :disabled="!file || loading"
          class="btn-secondary text-sm disabled:opacity-50">
          {{ loading && dryRunning ? '분석 중...' : '미리보기' }}
        </button>
        <button type="button" @click="run(false)" :disabled="!file || loading || saved"
          class="btn-primary text-sm disabled:opacity-50">
          {{ loading && !dryRunning ? '등록 중...' : '등록' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { policyApi } from '@/api/index.js'

const emit = defineEmits(['close', 'imported'])

const MAX_MB = 20
const categories = ['GENERAL', 'ACCESS_CONTROL', 'DATA_PROTECTION', 'INCIDENT_RESPONSE', 'NETWORK', 'PHYSICAL', 'VENDOR', 'OTHER']
const statuses = ['DRAFT', 'REVIEW', 'PUBLISHED', 'ARCHIVED']

const fileInput = ref(null)
const file = ref(null)
const loading = ref(false)
const dryRunning = ref(false)
const saved = ref(false)
const error = ref('')
const result = ref(null)

const options = reactive({
  guidelineName: '',
  category: 'GENERAL',
  status: 'DRAFT',
  version: '',
  effectiveDate: ''
})

function onFileChange(e) {
  const f = e.target.files?.[0] || null
  error.value = ''
  result.value = null
  saved.value = false
  if (f && f.size > MAX_MB * 1024 * 1024) {
    error.value = `파일이 너무 큽니다 (최대 ${MAX_MB}MB).`
    file.value = null
    if (fileInput.value) fileInput.value.value = ''
    return
  }
  file.value = f
}

async function run(dryRun) {
  if (!file.value || loading.value) return
  loading.value = true
  dryRunning.value = dryRun
  error.value = ''
  try {
    const res = await policyApi.importDocument(file.value, { ...options, dryRun })
    result.value = res.data
    if (!dryRun) {
      saved.value = true
      emit('imported', res.data)
    }
  } catch (e) {
    result.value = null
    error.value = typeof e === 'string' ? e : '문서를 등록하지 못했습니다.'
  } finally {
    loading.value = false
  }
}
</script>
