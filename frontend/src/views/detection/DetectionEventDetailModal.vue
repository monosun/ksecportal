<template>
  <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
    <div class="absolute inset-0 bg-black/40" @click="$emit('close')"></div>

    <div class="relative bg-white rounded-xl shadow-xl w-full max-w-3xl max-h-[92vh] flex flex-col">
      <!-- Header -->
      <div class="flex items-start justify-between gap-4 px-5 py-3 border-b shrink-0">
        <div class="min-w-0">
          <h2 class="text-lg font-semibold text-gray-900 break-words">{{ ev?.title || '탐지 이벤트' }}</h2>
          <div v-if="ev" class="flex flex-wrap items-center gap-1.5 mt-1.5">
            <span :class="severityClass(ev.severity)" class="text-xs font-semibold px-2 py-0.5 rounded">
              {{ SEVERITY_LABEL[ev.severity] }}
            </span>
            <span class="text-xs font-medium px-2 py-0.5 rounded bg-gray-100 text-gray-700">{{ ev.categoryLabel }}</span>
            <span :class="statusClass(ev.status)" class="text-xs font-medium px-2 py-0.5 rounded">
              {{ STATUS_LABEL[ev.status] }}
            </span>
            <span v-if="ev.autoBlocked" class="text-xs font-medium px-2 py-0.5 rounded bg-red-50 text-red-700">IP 자동 차단</span>
            <span class="text-xs text-gray-400">#{{ ev.id }}</span>
          </div>
        </div>
        <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600 p-1 shrink-0" aria-label="닫기">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <!-- Body -->
      <div class="flex-1 overflow-y-auto px-5 py-4 space-y-4">
        <div v-if="loading" class="text-sm text-gray-400 py-8 text-center">불러오는 중…</div>
        <template v-else-if="ev">
          <dl class="grid grid-cols-1 sm:grid-cols-2 gap-x-6 gap-y-2 text-sm">
            <div class="flex gap-2"><dt class="text-gray-500 w-24 shrink-0">출발지 IP</dt>
              <dd class="font-mono text-gray-800">{{ ev.sourceIp || '-' }}</dd></div>
            <div class="flex gap-2"><dt class="text-gray-500 w-24 shrink-0">계정</dt>
              <dd class="text-gray-800 break-all">{{ ev.userEmail || '-' }}</dd></div>
            <div class="flex gap-2"><dt class="text-gray-500 w-24 shrink-0">최초 탐지</dt>
              <dd class="text-gray-800">{{ formatDate(ev.detectedAt) }}</dd></div>
            <div class="flex gap-2"><dt class="text-gray-500 w-24 shrink-0">최근 탐지</dt>
              <dd class="text-gray-800">{{ formatDate(ev.lastSeenAt) }} <span class="text-gray-400">({{ ev.occurrences }}회)</span></dd></div>
            <div v-if="ev.score != null" class="flex gap-2"><dt class="text-gray-500 w-24 shrink-0">행위 점수</dt>
              <dd class="font-semibold text-orange-600">{{ ev.score }}점</dd></div>
            <div v-if="ev.incidentId" class="flex gap-2"><dt class="text-gray-500 w-24 shrink-0">침해사고</dt>
              <dd><RouterLink :to="`/incidents/${ev.incidentId}`" class="text-primary-600 hover:underline">#{{ ev.incidentId }} 보기</RouterLink></dd></div>
            <div class="sm:col-span-2 flex gap-2"><dt class="text-gray-500 w-24 shrink-0">요청 경로</dt>
              <dd class="font-mono text-xs text-gray-800 break-all">{{ ev.requestPath || '-' }}</dd></div>
            <div class="sm:col-span-2 flex gap-2"><dt class="text-gray-500 w-24 shrink-0">User-Agent</dt>
              <dd class="font-mono text-xs text-gray-700 break-all">{{ ev.userAgent || '(없음)' }}</dd></div>
          </dl>

          <div>
            <div class="text-xs font-semibold text-gray-500 mb-1">탐지 내용</div>
            <pre class="text-sm text-gray-800 whitespace-pre-wrap break-words bg-gray-50 rounded-lg p-3 font-sans">{{ ev.detail || '-' }}</pre>
          </div>

          <div v-if="ev.handledBy" class="text-xs text-gray-500">
            처리: {{ ev.handledBy }} · {{ formatDate(ev.handledAt) }}
            <span v-if="ev.handlerNote"> — {{ ev.handlerNote }}</span>
          </div>

          <!-- 처리 -->
          <div v-if="canWrite && ev.status !== 'ESCALATED'" class="border-t pt-4 space-y-3">
            <div class="text-sm font-semibold text-gray-700">처리</div>
            <textarea v-model="note" rows="2" class="input w-full text-sm" placeholder="처리 메모(선택)"></textarea>
            <div class="flex flex-wrap gap-2">
              <button v-for="s in actions" :key="s.value" @click="setStatus(s.value)" :disabled="busy"
                class="text-sm px-3 py-1.5 rounded-lg border border-gray-200 hover:bg-gray-50 disabled:opacity-50"
                :class="ev.status === s.value ? 'bg-gray-100 font-semibold' : ''">
                {{ s.label }}
              </button>
              <button @click="escalate" :disabled="busy"
                class="text-sm px-3 py-1.5 rounded-lg bg-purple-600 text-white hover:bg-purple-700 disabled:opacity-50">
                침해사고로 전환
              </button>
              <button v-if="ev.sourceIp && !ev.autoBlocked" @click="blockIp" :disabled="busy"
                class="text-sm px-3 py-1.5 rounded-lg bg-red-600 text-white hover:bg-red-700 disabled:opacity-50">
                IP 차단(24시간)
              </button>
            </div>
            <p v-if="error" class="text-sm text-red-600">{{ error }}</p>
          </div>
        </template>
      </div>

      <div class="flex justify-end gap-2 px-5 py-3 border-t shrink-0">
        <button v-if="isAdmin && ev" @click="remove" :disabled="busy"
          class="text-sm px-3 py-1.5 rounded-lg text-red-600 hover:bg-red-50 mr-auto">삭제</button>
        <button @click="$emit('close')" class="btn-secondary text-sm">닫기</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { detectionApi } from '@/api'
import { SEVERITY_LABEL, STATUS_LABEL, severityClass, statusClass, formatDate } from './detectionLabels'

const props = defineProps({
  open: Boolean,
  itemId: { type: Number, default: null },
  canWrite: Boolean,
  isAdmin: Boolean
})
const emit = defineEmits(['close', 'changed'])

const ev = ref(null)
const loading = ref(false)
const busy = ref(false)
const note = ref('')
const error = ref('')

const actions = [
  { value: 'ACKNOWLEDGED', label: '확인 중' },
  { value: 'FALSE_POSITIVE', label: '오탐' },
  { value: 'RESOLVED', label: '조치 완료' }
]

watch(() => [props.open, props.itemId], async ([open, id]) => {
  if (!open || !id) return
  loading.value = true
  error.value = ''
  note.value = ''
  try {
    const res = await detectionApi.event(id)
    ev.value = res.data
  } catch (e) {
    error.value = e
  } finally {
    loading.value = false
  }
}, { immediate: true })

async function run(fn) {
  busy.value = true
  error.value = ''
  try {
    await fn()
    emit('changed')
  } catch (e) {
    error.value = typeof e === 'string' ? e : '처리하지 못했습니다.'
  } finally {
    busy.value = false
  }
}

function setStatus(status) {
  return run(async () => {
    const res = await detectionApi.updateStatus(ev.value.id, { status, note: note.value || null })
    ev.value = res.data
  })
}

function escalate() {
  if (!confirm('이 탐지 이벤트로 침해사고를 등록할까요?')) return
  return run(async () => {
    const res = await detectionApi.escalate(ev.value.id)
    ev.value = res.data
  })
}

function blockIp() {
  if (!confirm(`${ev.value.sourceIp} 를 24시간 차단할까요?`)) return
  return run(async () => {
    await detectionApi.block({ ip: ev.value.sourceIp, minutes: 1440, reason: `[수동] 이벤트 #${ev.value.id} ${ev.value.title}` })
  })
}

function remove() {
  if (!confirm('이 탐지 이벤트를 삭제할까요?')) return
  return run(async () => {
    await detectionApi.deleteEvent(ev.value.id)
    emit('close')
  })
}
</script>
