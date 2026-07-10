<template>
  <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
    <div class="absolute inset-0 bg-black/40" @click="$emit('close')"></div>

    <div class="relative bg-white rounded-xl shadow-xl w-full max-w-4xl max-h-[92vh] flex flex-col">
      <div class="flex items-start justify-between gap-4 px-5 py-3 border-b shrink-0">
        <div v-if="incident" class="min-w-0">
          <h2 class="text-lg font-semibold text-gray-900 truncate">{{ incident.title }}</h2>
          <div class="flex flex-wrap gap-1.5 mt-1.5">
            <span :class="severityClass(incident.severity)">{{ $t(`incident.severity_label.${incident.severity}`) }}</span>
            <span :class="statusClass(incident.status)">{{ $t(`incident.status_label.${incident.status}`) }}</span>
            <span class="badge-blue">{{ $t(`incident.type_label.${incident.type}`) }}</span>
          </div>
        </div>
        <div v-else class="text-lg font-semibold text-gray-900">{{ $t('incident.title') }}</div>
        <div class="flex items-center gap-2 shrink-0">
          <template v-if="incident && isManager">
            <select :value="incident.status" @change="updateStatus($event.target.value)" class="input w-40 text-sm">
              <option v-for="s in statuses" :key="s" :value="s">{{ $t(`incident.status_label.${s}`) }}</option>
            </select>
            <button @click="$emit('edit', incident.id)" class="btn-secondary text-sm">{{ $t('common.edit') }}</button>
          </template>
          <button v-if="incident && isAdmin" @click="deleteIncident" class="btn-danger text-sm">{{ $t('common.delete') }}</button>
          <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600 p-1">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
      </div>

      <div class="px-5 py-4 overflow-y-auto flex-1 space-y-4">
        <div v-if="loading" class="py-16 text-center text-gray-400">{{ $t('common.loading') }}</div>
        <template v-else-if="incident">
          <div class="border border-gray-200 rounded-lg p-4 grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
            <div><p class="text-gray-500 mb-1">{{ $t('incident.reporter') }}</p><p class="font-medium">{{ incident.reporterName }}</p></div>
            <div><p class="text-gray-500 mb-1">{{ $t('incident.assignee') }}</p><p class="font-medium">{{ incident.assigneeName || '-' }}</p></div>
            <div><p class="text-gray-500 mb-1">{{ $t('incident.detectedAt') }}</p><p class="font-medium">{{ formatDt(incident.detectedAt) }}</p></div>
            <div><p class="text-gray-500 mb-1">{{ $t('incident.resolvedAt') }}</p><p class="font-medium">{{ formatDt(incident.resolvedAt) }}</p></div>
          </div>

          <div v-if="incident.affectedSystems" class="border border-gray-200 rounded-lg p-4">
            <h3 class="font-semibold text-gray-900 mb-2 text-sm">{{ $t('incident.affectedSystems') }}</h3>
            <p class="text-gray-700 text-sm whitespace-pre-wrap">{{ incident.affectedSystems }}</p>
          </div>

          <div class="border border-gray-200 rounded-lg p-4">
            <h3 class="font-semibold text-gray-900 mb-3 text-sm">상세 내용</h3>
            <p class="text-gray-700 text-sm whitespace-pre-wrap leading-relaxed">{{ incident.description || '-' }}</p>
          </div>

          <div class="border border-gray-200 rounded-lg p-4">
            <h3 class="font-semibold text-gray-900 mb-4 text-sm">대응 타임라인</h3>
            <div class="relative pl-6 border-l-2 border-gray-200 space-y-5">
              <div class="relative">
                <div class="absolute -left-[1.45rem] w-4 h-4 rounded-full bg-red-500 border-2 border-white"></div>
                <p class="text-xs text-gray-400">{{ formatDt(incident.detectedAt) }}</p>
                <p class="text-sm font-medium text-gray-800">인시던트 탐지</p>
                <p class="text-xs text-gray-500">{{ $t(`incident.type_label.${incident.type}`) }} 유형으로 탐지됨</p>
              </div>
              <div class="relative">
                <div class="absolute -left-[1.45rem] w-4 h-4 rounded-full bg-blue-500 border-2 border-white"></div>
                <p class="text-xs text-gray-400">{{ formatDt(incident.createdAt) }}</p>
                <p class="text-sm font-medium text-gray-800">포탈 등록</p>
                <p class="text-xs text-gray-500">{{ incident.reporterName }}에 의해 등록됨</p>
              </div>
              <div v-if="incident.resolvedAt" class="relative">
                <div class="absolute -left-[1.45rem] w-4 h-4 rounded-full bg-green-500 border-2 border-white"></div>
                <p class="text-xs text-gray-400">{{ formatDt(incident.resolvedAt) }}</p>
                <p class="text-sm font-medium text-gray-800">해결 완료</p>
              </div>
              <div class="relative">
                <div class="absolute -left-[1.45rem] w-4 h-4 rounded-full border-2 border-gray-300 bg-white"></div>
                <p class="text-sm text-gray-400">현재 상태: {{ $t(`incident.status_label.${incident.status}`) }}</p>
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
import { ref, watch } from 'vue'
import { incidentApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const props = defineProps({
  open: { type: Boolean, default: false },
  itemId: { type: [Number, String], default: null }
})
const emit = defineEmits(['close', 'edit', 'changed'])

const auth = useAuthStore()
const isManager = auth.isManager
const isAdmin = auth.isAdmin

const incident = ref(null)
const loading = ref(false)
const statuses = ['OPEN', 'INVESTIGATING', 'CONTAINED', 'RESOLVED', 'CLOSED']

watch(() => props.open, async (open) => {
  if (!open || !props.itemId) return
  loading.value = true
  incident.value = null
  try { incident.value = (await incidentApi.get(props.itemId)).data }
  finally { loading.value = false }
})

async function updateStatus(status) {
  const res = await incidentApi.update(props.itemId, { status })
  incident.value.status = res.data.status
  incident.value.resolvedAt = res.data.resolvedAt
  emit('changed')
}

async function deleteIncident() {
  if (!confirm('인시던트를 삭제하시겠습니까?')) return
  try {
    await incidentApi.delete(props.itemId)
    emit('changed')
    emit('close')
  } catch (e) {
    alert(typeof e === 'string' ? e : '삭제에 실패했습니다')
  }
}

const formatDt = (dt) => dt ? new Date(dt).toLocaleString() : '-'
function severityClass(s) { return { CRITICAL: 'badge-red', HIGH: 'badge-orange', MEDIUM: 'badge-yellow', LOW: 'badge-green' }[s] || 'badge-gray' }
function statusClass(s) { return { OPEN: 'badge-red', INVESTIGATING: 'badge-orange', CONTAINED: 'badge-yellow', RESOLVED: 'badge-green', CLOSED: 'badge-gray' }[s] || 'badge-gray' }
</script>
