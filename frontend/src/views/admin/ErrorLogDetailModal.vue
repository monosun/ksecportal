<template>
  <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
    <div class="absolute inset-0 bg-black/40" @click="$emit('close')"></div>

    <div class="relative bg-white rounded-xl shadow-xl w-full max-w-4xl max-h-[92vh] flex flex-col">
      <!-- Header -->
      <div class="flex items-start justify-between gap-4 px-5 py-3 border-b shrink-0">
        <div class="min-w-0">
          <h2 class="text-lg font-semibold text-gray-900 truncate">
            {{ summary?.exceptionType || '오류 상세' }}
          </h2>
          <div v-if="summary" class="flex flex-wrap items-center gap-1.5 mt-1.5">
            <span :class="levelClass(summary.level)" class="text-xs font-medium px-2 py-0.5 rounded">
              {{ summary.level }}
            </span>
            <span class="text-xs font-medium px-2 py-0.5 rounded bg-gray-100 text-gray-600">
              {{ SOURCE_LABEL[summary.source] || summary.source }}
            </span>
            <span :class="statusClass(summary.status)" class="text-xs font-medium px-2 py-0.5 rounded">
              {{ STATUS_LABEL[summary.status] || summary.status }}
            </span>
            <span class="text-xs text-gray-400">{{ formatDate(summary.occurredAt) }}</span>
          </div>
        </div>
        <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600 p-1 shrink-0">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <!-- Body -->
      <div class="px-5 py-4 overflow-y-auto flex-1">
        <div v-if="loading" class="py-16 text-center text-gray-400">불러오는 중...</div>
        <template v-else-if="summary">
          <!-- 메시지 -->
          <div class="mb-4">
            <div class="text-xs text-gray-500 mb-1">오류 메시지</div>
            <div class="rounded-lg bg-red-50 border border-red-100 text-sm text-red-800 px-3 py-2 whitespace-pre-wrap break-words">
              {{ summary.message || '(메시지 없음)' }}
            </div>
          </div>

          <!-- 기본 정보 -->
          <div class="grid grid-cols-1 md:grid-cols-2 gap-x-5 gap-y-3 text-sm mb-4">
            <div>
              <div class="text-xs text-gray-500">요청</div>
              <div class="text-gray-800 break-all">
                <span v-if="summary.httpMethod" class="font-mono text-xs mr-1">{{ summary.httpMethod }}</span>
                {{ summary.requestUri || '-' }}
              </div>
            </div>
            <div>
              <div class="text-xs text-gray-500">응답 코드</div>
              <div class="text-gray-800">{{ summary.httpStatus ?? '-' }}</div>
            </div>
            <div>
              <div class="text-xs text-gray-500">사용자</div>
              <div class="text-gray-800">{{ summary.userName || '-' }}</div>
            </div>
            <div>
              <div class="text-xs text-gray-500">접속 IP</div>
              <div class="text-gray-800">{{ summary.ipAddress || '-' }}</div>
            </div>
            <div class="md:col-span-2">
              <div class="text-xs text-gray-500">브라우저(User-Agent)</div>
              <div class="text-gray-600 text-xs break-all">{{ detail?.userAgent || '-' }}</div>
            </div>
            <div v-if="summary.handledByName" class="md:col-span-2">
              <div class="text-xs text-gray-500">처리자</div>
              <div class="text-gray-800">{{ summary.handledByName }} · {{ formatDate(summary.handledAt) }}</div>
            </div>
          </div>

          <!-- 처리 -->
          <div class="rounded-lg border border-gray-200 p-3 mb-4">
            <div class="text-sm font-semibold text-gray-700 mb-2">처리</div>
            <div class="flex flex-wrap items-end gap-3">
              <div class="flex flex-col gap-1">
                <label class="text-xs text-gray-500">처리 상태</label>
                <select v-model="form.status" class="input w-36 text-sm">
                  <option v-for="(label, key) in STATUS_LABEL" :key="key" :value="key">{{ label }}</option>
                </select>
              </div>
              <div class="flex-1 min-w-[240px] flex flex-col gap-1">
                <label class="text-xs text-gray-500">처리 메모</label>
                <input v-model="form.note" class="input text-sm" placeholder="원인·조치 내용을 남겨주세요" />
              </div>
              <button @click="save" :disabled="saving" class="btn-primary text-sm">
                {{ saving ? '저장 중...' : '저장' }}
              </button>
            </div>
          </div>

          <!-- 스택트레이스 -->
          <div>
            <div class="flex items-center justify-between mb-1">
              <div class="text-xs text-gray-500">스택트레이스</div>
              <button v-if="detail?.stackTrace" @click="copyStack"
                class="text-xs text-gray-500 hover:text-primary-600">
                {{ copied ? '복사됨' : '복사' }}
              </button>
            </div>
            <pre class="rounded-lg bg-gray-900 text-gray-100 text-[11px] leading-relaxed p-3 overflow-x-auto max-h-80 whitespace-pre">{{ detail?.stackTrace || '(스택트레이스 없음)' }}</pre>
          </div>
        </template>
        <div v-else class="py-16 text-center text-gray-400">불러오지 못했습니다.</div>
      </div>

      <!-- Footer -->
      <div class="flex items-center justify-between gap-2 px-5 py-3 border-t shrink-0">
        <button @click="remove" class="text-sm text-red-600 hover:text-red-700">이 로그 삭제</button>
        <button @click="$emit('close')" class="btn-secondary text-sm">닫기</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { errorLogApi } from '@/api'
import { STATUS_LABEL, SOURCE_LABEL, levelClass, statusClass, formatDate } from './errorLogLabels'

const props = defineProps({
  open: { type: Boolean, default: false },
  logId: { type: Number, default: null },
})
const emit = defineEmits(['close', 'changed'])

const detail = ref(null)
const loading = ref(false)
const saving = ref(false)
const copied = ref(false)
const form = ref({ status: 'NEW', note: '' })

const summary = computed(() => detail.value?.summary ?? null)

watch(() => [props.open, props.logId], async () => {
  if (!props.open || !props.logId) return
  loading.value = true
  detail.value = null
  copied.value = false
  try {
    const res = await errorLogApi.get(props.logId)
    detail.value = res.data
    form.value = { status: summary.value?.status || 'NEW', note: summary.value?.note || '' }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}, { immediate: true })

async function save() {
  saving.value = true
  try {
    await errorLogApi.handle(props.logId, { status: form.value.status, note: form.value.note })
    emit('changed')
    emit('close')
  } catch (e) {
    alert(e || '저장하지 못했습니다.')
  } finally {
    saving.value = false
  }
}

async function remove() {
  if (!confirm('이 오류 로그를 삭제할까요?')) return
  try {
    await errorLogApi.delete(props.logId)
    emit('changed')
    emit('close')
  } catch (e) {
    alert(e || '삭제하지 못했습니다.')
  }
}

async function copyStack() {
  try {
    await navigator.clipboard.writeText(detail.value?.stackTrace || '')
    copied.value = true
    setTimeout(() => { copied.value = false }, 1500)
  } catch {
    copied.value = false
  }
}
</script>
