<template>
  <div class="p-8 max-w-4xl mx-auto">
    <div v-if="loading" class="text-center py-16 text-gray-400">{{ $t('common.loading') }}</div>
    <template v-else-if="incident">
      <RouterLink to="/incidents" class="text-sm text-primary-600 hover:underline mb-4 inline-block">← {{ $t('common.back') }}</RouterLink>

      <div class="flex items-start justify-between mb-6">
        <div>
          <h1 class="text-2xl font-bold text-gray-900">{{ incident.title }}</h1>
          <div class="flex flex-wrap gap-2 mt-2">
            <span :class="severityClass(incident.severity)">{{ $t(`incident.severity_label.${incident.severity}`) }}</span>
            <span :class="statusClass(incident.status)">{{ $t(`incident.status_label.${incident.status}`) }}</span>
            <span class="badge-blue">{{ $t(`incident.type_label.${incident.type}`) }}</span>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <div v-if="isManager" class="flex items-center gap-2">
            <select @change="updateStatus($event.target.value)" class="input w-44 text-sm">
              <option v-for="s in statuses" :key="s" :value="s" :selected="s === incident.status">
                {{ $t(`incident.status_label.${s}`) }}
              </option>
            </select>
            <RouterLink :to="`/incidents/${incident.id}/edit`" class="btn-secondary text-sm">{{ $t('common.edit') }}</RouterLink>
          </div>
          <button v-if="isAdmin" @click="deleteIncident" class="btn-danger text-sm">{{ $t('common.delete') }}</button>
        </div>
      </div>

      <!-- Timeline / Meta -->
      <div class="card mb-5 grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
        <div><p class="text-gray-500 mb-1">{{ $t('incident.reporter') }}</p><p class="font-medium">{{ incident.reporterName }}</p></div>
        <div><p class="text-gray-500 mb-1">{{ $t('incident.assignee') }}</p><p class="font-medium">{{ incident.assigneeName || '-' }}</p></div>
        <div><p class="text-gray-500 mb-1">{{ $t('incident.detectedAt') }}</p><p class="font-medium">{{ formatDt(incident.detectedAt) }}</p></div>
        <div><p class="text-gray-500 mb-1">{{ $t('incident.resolvedAt') }}</p><p class="font-medium">{{ formatDt(incident.resolvedAt) }}</p></div>
      </div>

      <div v-if="incident.affectedSystems" class="card mb-5">
        <h2 class="font-semibold text-gray-900 mb-2 text-sm">{{ $t('incident.affectedSystems') }}</h2>
        <p class="text-gray-700 text-sm whitespace-pre-wrap">{{ incident.affectedSystems }}</p>
      </div>

      <div class="card mb-5">
        <h2 class="font-semibold text-gray-900 mb-3">상세 내용</h2>
        <p class="text-gray-700 text-sm whitespace-pre-wrap leading-relaxed">{{ incident.description || '-' }}</p>
      </div>

      <!-- Status History / Actions -->
      <div class="card">
        <h2 class="font-semibold text-gray-900 mb-4">대응 타임라인</h2>
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
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { incidentApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const isManager = auth.isManager
const isAdmin = auth.isAdmin

const incident = ref(null)
const loading = ref(true)
const statuses = ['OPEN', 'INVESTIGATING', 'CONTAINED', 'RESOLVED', 'CLOSED']

onMounted(async () => {
  try {
    const res = await incidentApi.get(route.params.id)
    incident.value = res.data
  } finally {
    loading.value = false
  }
})

async function updateStatus(status) {
  const res = await incidentApi.update(route.params.id, { status })
  incident.value.status = res.data.status
  incident.value.resolvedAt = res.data.resolvedAt
}

async function deleteIncident() {
  if (!confirm('인시던트를 삭제하시겠습니까?')) return
  try {
    await incidentApi.delete(route.params.id)
    router.push('/incidents')
  } catch (e) {
    alert(typeof e === 'string' ? e : '삭제에 실패했습니다')
  }
}

const formatDt = (dt) => dt ? new Date(dt).toLocaleString() : '-'
function severityClass(s) { return { CRITICAL: 'badge-red', HIGH: 'badge-orange', MEDIUM: 'badge-yellow', LOW: 'badge-green' }[s] || 'badge-gray' }
function statusClass(s) { return { OPEN: 'badge-red', INVESTIGATING: 'badge-orange', CONTAINED: 'badge-yellow', RESOLVED: 'badge-green', CLOSED: 'badge-gray' }[s] || 'badge-gray' }
</script>
