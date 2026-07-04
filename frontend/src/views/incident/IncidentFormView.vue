<template>
  <div class="p-8 max-w-2xl mx-auto">
    <RouterLink :to="isEdit ? `/incidents/${route.params.id}` : '/incidents'" class="text-sm text-primary-600 hover:underline mb-4 inline-block">← {{ $t('common.back') }}</RouterLink>
    <h1 class="text-2xl font-bold text-gray-900 mb-6">
      {{ isEdit ? '인시던트 수정' : $t('incident.create') }}
    </h1>

    <form @submit.prevent="submit" class="card space-y-5">
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('common.title') }} *</label>
        <input v-model="form.title" required class="input w-full" placeholder="인시던트 제목을 입력하세요" />
      </div>

      <div class="grid grid-cols-2 gap-4">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('incident.severity') }} *</label>
          <select v-model="form.severity" required class="input w-full">
            <option value="">선택</option>
            <option v-for="s in severities" :key="s" :value="s">{{ $t(`incident.severity_label.${s}`) }}</option>
          </select>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('incident.type') }} *</label>
          <select v-model="form.type" required class="input w-full">
            <option value="">선택</option>
            <option v-for="t in types" :key="t" :value="t">{{ $t(`incident.type_label.${t}`) }}</option>
          </select>
        </div>
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('incident.detectedAt') }}</label>
        <input v-model="form.detectedAt" type="datetime-local" class="input w-full" />
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">{{ $t('incident.affectedSystems') }}</label>
        <input v-model="form.affectedSystems" class="input w-full" placeholder="영향 받은 시스템, IP 주소 등" />
      </div>

      <!-- 담당자 콤보박스 -->
      <div class="relative" ref="assigneeDropdownRef">
        <label class="block text-sm font-medium text-gray-700 mb-1">
          {{ $t('incident.assignee') }}
          <span class="text-gray-400 font-normal text-xs ml-1">— 사용자 목록에서 선택하거나 직접 입력</span>
        </label>
        <div class="relative">
          <input
            v-model="assigneeSearch"
            type="text"
            class="input w-full pr-8"
            placeholder="담당자 이름 검색 또는 직접 입력"
            @input="onAssigneeInput"
            @focus="showAssigneeDropdown = true"
            @keydown.down.prevent="assigneeIdx = Math.min(assigneeIdx + 1, filteredUsers.length - 1)"
            @keydown.up.prevent="assigneeIdx = Math.max(assigneeIdx - 1, -1)"
            @keydown.enter.prevent="selectUser(filteredUsers[assigneeIdx])"
            @keydown.esc="showAssigneeDropdown = false"
            autocomplete="off" />
          <button v-if="assigneeSearch" type="button"
            class="absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600"
            @mousedown.prevent="clearAssignee">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <div v-if="showAssigneeDropdown && filteredUsers.length"
          class="absolute z-50 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-48 overflow-y-auto">
          <button v-for="(u, idx) in filteredUsers" :key="u.id" type="button"
            class="w-full px-3 py-2.5 text-left text-sm hover:bg-gray-50 flex items-center gap-3 border-b border-gray-100 last:border-0"
            :class="assigneeIdx === idx ? 'bg-primary-50' : ''"
            @mousedown.prevent="selectUser(u)">
            <div class="w-6 h-6 rounded-full bg-primary-500 flex items-center justify-center text-white text-xs font-bold flex-shrink-0">
              {{ u.name?.[0]?.toUpperCase() }}
            </div>
            <div class="flex-1 min-w-0">
              <p class="font-medium text-gray-800 truncate">{{ u.name }}</p>
              <p class="text-xs text-gray-400 truncate">{{ u.email }} · {{ u.department || u.role }}</p>
            </div>
          </button>
        </div>
        <p v-if="form.assigneeId" class="text-xs text-green-600 mt-1 flex items-center gap-1">
          <svg class="w-3.5 h-3.5" fill="currentColor" viewBox="0 0 20 20"><path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clip-rule="evenodd"/></svg>
          등록된 사용자로 지정됨
        </p>
        <p v-else-if="assigneeSearch" class="text-xs text-gray-400 mt-1">직접 입력 — 저장 시 텍스트로 기록됩니다</p>
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">상세 내용</label>
        <textarea v-model="form.description" rows="5" class="input w-full resize-none" placeholder="인시던트 경위, 영향 범위, 초기 대응 내용 등을 기록하세요" />
      </div>

      <div v-if="error" class="text-sm text-red-600 bg-red-50 p-3 rounded-lg">{{ error }}</div>

      <div class="flex gap-3 pt-2">
        <button type="submit" :disabled="submitting" class="btn-primary">
          {{ submitting ? $t('common.loading') : $t('common.save') }}
        </button>
        <RouterLink :to="isEdit ? `/incidents/${route.params.id}` : '/incidents'" class="btn-secondary">
          {{ $t('common.cancel') }}
        </RouterLink>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { incidentApi, adminApi } from '@/api'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id && route.path.includes('/edit'))

const severities = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW']
const types = ['MALWARE', 'PHISHING', 'DATA_BREACH', 'UNAUTHORIZED_ACCESS', 'DDOS', 'INSIDER_THREAT', 'PHYSICAL', 'OTHER']

const form = ref({ title: '', severity: '', type: '', affectedSystems: '', description: '', detectedAt: '', assigneeId: null, assigneeText: '' })
const submitting = ref(false)
const error = ref(null)

// 담당자 콤보박스
const allUsers = ref([])
const assigneeSearch = ref('')
const assigneeDropdownRef = ref(null)
const showAssigneeDropdown = ref(false)
const assigneeIdx = ref(-1)

const filteredUsers = computed(() => {
  const q = assigneeSearch.value.trim().toLowerCase()
  if (!q) return allUsers.value.slice(0, 10)
  return allUsers.value.filter(u =>
    u.name?.toLowerCase().includes(q) || u.email?.toLowerCase().includes(q)
  ).slice(0, 10)
})

function onAssigneeInput() {
  form.value.assigneeId = null
  form.value.assigneeText = assigneeSearch.value
  showAssigneeDropdown.value = true
  assigneeIdx.value = -1
}

function selectUser(u) {
  if (!u) return
  form.value.assigneeId = u.id
  form.value.assigneeText = ''
  assigneeSearch.value = u.name
  showAssigneeDropdown.value = false
  assigneeIdx.value = -1
}

function clearAssignee() {
  form.value.assigneeId = null
  form.value.assigneeText = ''
  assigneeSearch.value = ''
  showAssigneeDropdown.value = false
}

function handleClickOutside(e) {
  if (assigneeDropdownRef.value && !assigneeDropdownRef.value.contains(e.target)) {
    showAssigneeDropdown.value = false
  }
}

onMounted(async () => {
  document.addEventListener('mousedown', handleClickOutside)
  try {
    const usersRes = await adminApi.listUsersSimple()
    allUsers.value = usersRes.data || []
  } catch { allUsers.value = [] }
  if (isEdit.value) {
    const res = await incidentApi.get(route.params.id)
    const i = res.data
    form.value = {
      title: i.title,
      severity: i.severity,
      type: i.type,
      affectedSystems: i.affectedSystems || '',
      description: i.description || '',
      detectedAt: i.detectedAt ? i.detectedAt.slice(0, 16) : '',
      assigneeId: i.assigneeId || null,
      assigneeText: ''
    }
    assigneeSearch.value = i.assigneeName || ''
  } else {
    form.value.detectedAt = new Date().toISOString().slice(0, 16)
  }
})

onUnmounted(() => {
  document.removeEventListener('mousedown', handleClickOutside)
})

async function submit() {
  submitting.value = true
  error.value = null
  try {
    const payload = {
      ...form.value,
      detectedAt: form.value.detectedAt || null
    }
    if (!payload.assigneeId) delete payload.assigneeId
    if (!payload.assigneeText) delete payload.assigneeText
    if (isEdit.value) {
      await incidentApi.update(route.params.id, payload)
      router.push(`/incidents/${route.params.id}`)
    } else {
      const res = await incidentApi.create(payload)
      router.push(`/incidents/${res.data.id}`)
    }
  } catch (e) {
    error.value = e
  } finally {
    submitting.value = false
  }
}
</script>
