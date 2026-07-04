<template>
  <div class="flex h-full min-h-screen">

    <!-- 왼쪽 패널: 연동 솔루션 목록 -->
    <div class="w-72 flex-shrink-0 flex flex-col border-r bg-white">

      <!-- 헤더 + 연동 추가 버튼 -->
      <div class="px-4 py-3 border-b flex items-center justify-between">
        <div>
          <h2 class="text-sm font-bold text-gray-800">{{ $t('securityEvent.integrations') }}</h2>
          <p class="text-xs text-gray-400 mt-0.5">{{ integrations.length }}개 솔루션 연동</p>
        </div>
        <button @click="openCreateModal"
          class="flex items-center gap-1 px-3 py-1.5 rounded-lg bg-primary-500 text-white text-xs font-semibold hover:bg-primary-600 transition-colors flex-shrink-0">
          <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          연동 추가
        </button>
      </div>

      <!-- 솔루션 목록 -->
      <div class="flex-1 overflow-y-auto py-2 space-y-0.5 px-2">
        <div v-if="loadingIntegrations" class="p-6 text-center text-xs text-gray-400">{{ $t('common.loading') }}</div>
        <div v-else-if="!integrations.length" class="p-6 text-center">
          <p class="text-xs text-gray-400 mb-3">{{ $t('securityEvent.noIntegration') }}</p>
          <button @click="openCreateModal"
            class="text-xs text-primary-600 font-semibold hover:underline">
            + 첫 번째 솔루션 연동하기
          </button>
        </div>

        <!-- 솔루션 카드 (각 카드에 수정/삭제 버튼 인라인) -->
        <div
          v-for="item in integrations" :key="item.id"
          @click="selectIntegration(item)"
          class="rounded-xl border p-3 cursor-pointer transition-all group"
          :class="selected?.id === item.id
            ? 'bg-primary-50 border-primary-200'
            : 'bg-white border-gray-100 hover:border-gray-200 hover:bg-gray-50'">

          <div class="flex items-start gap-2.5">
            <!-- 솔루션 유형 아이콘 -->
            <div class="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0 text-white text-[10px] font-bold"
              :class="solutionTypeColor(item.solutionType)">
              {{ solutionTypeAbbr(item.solutionType) }}
            </div>

            <!-- 솔루션 정보 -->
            <div class="flex-1 min-w-0">
              <p class="text-sm font-semibold text-gray-800 truncate">{{ item.name }}</p>
              <p class="text-xs text-gray-400">{{ solutionTypeLabel(item.solutionType) }}<span v-if="item.vendor"> · {{ item.vendor }}</span></p>
              <div class="flex items-center gap-2 mt-1.5">
                <span class="flex items-center gap-1 text-xs font-medium" :class="statusColor(item.status)">
                  <span class="w-1.5 h-1.5 rounded-full" :class="statusDot(item.status)"></span>
                  {{ $t(`securityEvent.status_label.${item.status}`) }}
                </span>
                <span class="text-xs text-gray-300">|</span>
                <span class="text-xs text-gray-400">이벤트 {{ item.eventCount }}건</span>
              </div>
            </div>

            <!-- 수정 / 삭제 버튼 (항상 표시) -->
            <div class="flex flex-col gap-1 flex-shrink-0 ml-1" @click.stop>
              <button
                @click="openEditModalFor(item)"
                class="p-1.5 rounded-lg text-gray-400 hover:text-primary-600 hover:bg-primary-50 transition-colors"
                title="수정">
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                </svg>
              </button>
              <button
                @click="confirmDeleteItem(item)"
                class="p-1.5 rounded-lg text-gray-400 hover:text-red-600 hover:bg-red-50 transition-colors"
                title="삭제">
                <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 오른쪽 패널: 이벤트 모니터링 -->
    <div class="flex-1 flex flex-col bg-gray-50 min-w-0">

      <!-- 솔루션 미선택 상태 -->
      <div v-if="!selected" class="flex-1 flex items-center justify-center">
        <div class="text-center">
          <div class="w-16 h-16 bg-gray-200 rounded-2xl flex items-center justify-center mx-auto mb-4">
            <svg class="w-8 h-8 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
                d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/>
            </svg>
          </div>
          <p class="text-sm font-semibold text-gray-600">{{ $t('securityEvent.selectIntegration') }}</p>
        </div>
      </div>

      <!-- 이벤트 패널 -->
      <template v-else>

        <!-- 헤더 -->
        <div class="px-6 py-4 bg-white border-b flex items-center justify-between">
          <div class="flex items-center gap-3">
            <div class="w-9 h-9 rounded-xl flex items-center justify-center text-white text-xs font-bold"
              :class="solutionTypeColor(selected.solutionType)">
              {{ solutionTypeAbbr(selected.solutionType) }}
            </div>
            <div>
              <h2 class="text-base font-bold text-gray-900">{{ selected.name }}</h2>
              <p class="text-xs text-gray-400">
                {{ selected.vendor || '-' }}
                <span v-if="selected.host"> · {{ selected.host }}</span>
                <span v-if="selected.lastSyncAt"> · 마지막 동기화 {{ formatDate(selected.lastSyncAt) }}</span>
              </p>
            </div>
          </div>
          <div class="flex items-center gap-2">
            <!-- 심각도 필터 -->
            <select v-model="severityFilter" @change="loadEvents"
              class="input text-xs py-1.5 px-2.5 w-28">
              <option value="">심각도: 전체</option>
              <option v-for="s in severities" :key="s" :value="s">{{ $t(`securityEvent.severity_label.${s}`) }}</option>
            </select>
            <!-- 이벤트 수동 등록 -->
            <button v-if="isManager" @click="openEventModal"
              class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-white border border-gray-200 text-xs font-semibold text-gray-600 hover:bg-gray-50 transition-colors">
              <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
              </svg>
              {{ $t('securityEvent.addEvent') }}
            </button>
            <!-- 새로고침 -->
            <button @click="loadEvents" :class="['p-1.5 rounded-lg border border-gray-200 text-gray-500 hover:bg-gray-50 transition-colors', loadingEvents ? 'animate-spin' : '']">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
              </svg>
            </button>
          </div>
        </div>

        <!-- 요약 통계 -->
        <div class="px-6 py-3 bg-white border-b flex gap-4">
          <div v-for="s in severities" :key="s" class="flex items-center gap-1.5">
            <span class="w-2 h-2 rounded-full flex-shrink-0" :class="severityDot(s)"></span>
            <span class="text-xs text-gray-500">{{ $t(`securityEvent.severity_label.${s}`) }}</span>
            <span class="text-xs font-bold" :class="severityTextColor(s)">{{ severityCount(s) }}</span>
          </div>
          <div class="ml-auto text-xs text-gray-400">총 {{ totalEvents }}건</div>
        </div>

        <!-- 이벤트 테이블 -->
        <div class="flex-1 overflow-auto px-6 py-4">
          <div v-if="loadingEvents" class="py-12 text-center text-sm text-gray-400">{{ $t('common.loading') }}</div>
          <div v-else-if="!events.length" class="py-12 text-center text-sm text-gray-400">{{ $t('securityEvent.noEvents') }}</div>
          <div v-else class="bg-white rounded-xl border overflow-hidden">
            <table class="w-full text-sm">
              <thead class="bg-gray-50 border-b">
                <tr>
                  <th class="text-left px-4 py-2.5 text-xs font-medium text-gray-500 uppercase w-20">{{ $t('securityEvent.severity') }}</th>
                  <th class="text-left px-4 py-2.5 text-xs font-medium text-gray-500 uppercase w-32">{{ $t('securityEvent.eventType') }}</th>
                  <th class="text-left px-4 py-2.5 text-xs font-medium text-gray-500 uppercase">{{ $t('securityEvent.message') }}</th>
                  <th class="text-left px-4 py-2.5 text-xs font-medium text-gray-500 uppercase w-28">{{ $t('securityEvent.sourceIp') }}</th>
                  <th class="text-left px-4 py-2.5 text-xs font-medium text-gray-500 uppercase w-36">{{ $t('securityEvent.detectedAt') }}</th>
                  <th v-if="isManager" class="w-10"></th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-50">
                <tr v-for="evt in events" :key="evt.id" class="hover:bg-gray-50 transition-colors">
                  <td class="px-4 py-2.5">
                    <span class="inline-flex items-center gap-1 text-xs font-semibold px-2 py-0.5 rounded-full"
                      :class="severityBadge(evt.severity)">
                      <span class="w-1.5 h-1.5 rounded-full" :class="severityDot(evt.severity)"></span>
                      {{ $t(`securityEvent.severity_label.${evt.severity}`) }}
                    </span>
                  </td>
                  <td class="px-4 py-2.5 text-xs font-mono text-gray-600">{{ evt.eventType }}</td>
                  <td class="px-4 py-2.5 text-xs text-gray-700 max-w-xs truncate">{{ evt.message }}</td>
                  <td class="px-4 py-2.5 text-xs font-mono text-gray-500">{{ evt.sourceIp || '-' }}</td>
                  <td class="px-4 py-2.5 text-xs text-gray-400">{{ formatDate(evt.detectedAt || evt.createdAt) }}</td>
                  <td v-if="isManager" class="px-2 py-2.5 text-right">
                    <button @click="deleteEvent(evt.id)"
                      class="text-gray-300 hover:text-red-500 transition-colors p-1 rounded">
                      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                      </svg>
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- 페이지네이션 -->
          <div v-if="totalPages > 1" class="flex justify-center gap-2 mt-4">
            <button v-for="p in totalPages" :key="p" @click="eventPage = p - 1; loadEvents()"
              :class="['px-3 py-1 rounded border text-xs', eventPage === p - 1
                ? 'bg-primary-500 text-white border-primary-500'
                : 'border-gray-200 hover:bg-gray-50 text-gray-600']">
              {{ p }}
            </button>
          </div>
        </div>
      </template>
    </div>

    <!-- 연동 추가/수정 모달 -->
    <div v-if="showIntegrationModal" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-md">
        <div class="flex items-center justify-between px-6 py-4 border-b">
          <h3 class="text-base font-bold text-gray-900">
            {{ editingIntegration ? $t('securityEvent.editIntegration') : $t('securityEvent.addIntegration') }}
            <span v-if="editingIntegration" class="ml-1 text-gray-400 font-normal">— {{ editingIntegration.name }}</span>
          </h3>
          <button @click="showIntegrationModal = false" class="text-gray-400 hover:text-gray-600">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <div class="px-6 py-5 space-y-4">

          <!-- 솔루션 이름: 자산 자동완성 콤보박스 -->
          <div>
            <label class="block text-xs font-semibold text-gray-700 mb-1">
              {{ $t('securityEvent.name') }} *
              <span class="text-gray-400 font-normal ml-1">— 자산 목록에서 선택하거나 직접 입력</span>
            </label>
            <div class="relative" ref="nameComboRef">
              <input
                v-model="integrationForm.name"
                @input="onNameInput"
                @focus="showAssetDropdown = true"
                @keydown.down.prevent="assetDropdownIdx = Math.min(assetDropdownIdx + 1, filteredAssets.length - 1)"
                @keydown.up.prevent="assetDropdownIdx = Math.max(assetDropdownIdx - 1, -1)"
                @keydown.enter.prevent="selectAsset(filteredAssets[assetDropdownIdx])"
                @keydown.esc="showAssetDropdown = false"
                class="input w-full text-sm pr-8"
                placeholder="자산명 검색 또는 직접 입력" />
              <svg v-if="integrationForm.name" @click="integrationForm.name = ''; showAssetDropdown = false"
                class="absolute right-2.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-300 hover:text-gray-500 cursor-pointer"
                fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
              <!-- 자산 드롭다운 -->
              <div v-if="showAssetDropdown && filteredAssets.length"
                class="absolute z-10 w-full mt-1 bg-white border border-gray-200 rounded-xl shadow-lg max-h-48 overflow-y-auto">
                <button
                  v-for="(asset, idx) in filteredAssets" :key="asset.id"
                  @click="selectAsset(asset)"
                  class="w-full text-left px-3 py-2.5 text-sm hover:bg-gray-50 flex items-center gap-2.5 transition-colors"
                  :class="assetDropdownIdx === idx ? 'bg-primary-50' : ''">
                  <span class="text-xs px-1.5 py-0.5 rounded bg-gray-100 text-gray-500 font-mono flex-shrink-0">{{ asset.type }}</span>
                  <span class="font-medium text-gray-800 truncate">{{ asset.name }}</span>
                  <span v-if="asset.ipAddress" class="text-xs text-gray-400 font-mono ml-auto flex-shrink-0">{{ asset.ipAddress }}</span>
                </button>
              </div>
            </div>
          </div>

          <!-- 솔루션 유형: 코드관리에서 동적 로드 -->
          <div>
            <label class="block text-xs font-semibold text-gray-700 mb-1">
              {{ $t('securityEvent.solutionType') }} *
              <RouterLink to="/admin/codes" target="_blank"
                class="text-primary-500 hover:underline font-normal ml-1 text-[11px]">
                코드관리에서 편집 ↗
              </RouterLink>
            </label>
            <select v-model="integrationForm.solutionType" class="input w-full text-sm">
              <option value="">선택하세요</option>
              <option v-for="t in solutionTypeCodes" :key="t.value" :value="t.value">{{ t.label }}</option>
            </select>
            <p v-if="!solutionTypeCodes.length" class="text-xs text-orange-500 mt-1">
              코드관리에서 SECURITY_SOLUTION_TYPE 그룹을 추가해주세요.
            </p>
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block text-xs font-semibold text-gray-700 mb-1">{{ $t('securityEvent.vendor') }}</label>
              <input v-model="integrationForm.vendor" class="input w-full text-sm" placeholder="예: Palo Alto" />
            </div>
            <div>
              <label class="block text-xs font-semibold text-gray-700 mb-1">{{ $t('securityEvent.host') }}</label>
              <input v-model="integrationForm.host" class="input w-full text-sm" placeholder="예: 192.168.1.1" />
            </div>
          </div>
          <div>
            <label class="block text-xs font-semibold text-gray-700 mb-1">{{ $t('securityEvent.apiKey') }}</label>
            <input v-model="integrationForm.apiKey" class="input w-full text-sm" type="password" placeholder="API 키 (선택)" />
          </div>
          <div>
            <label class="block text-xs font-semibold text-gray-700 mb-1">{{ $t('securityEvent.description') }}</label>
            <textarea v-model="integrationForm.description" class="input w-full text-sm" rows="2" placeholder="설명 (선택)"></textarea>
          </div>
          <div v-if="integrationFormError" class="text-xs text-red-600">{{ integrationFormError }}</div>
        </div>
        <div class="px-6 py-4 border-t flex justify-end gap-3">
          <button @click="showIntegrationModal = false" class="btn-secondary text-sm">{{ $t('common.cancel') }}</button>
          <button @click="saveIntegration" :disabled="savingIntegration" class="btn-primary text-sm">
            {{ savingIntegration ? $t('common.loading') : $t('common.save') }}
          </button>
        </div>
      </div>
    </div>

    <!-- 이벤트 수동 등록 모달 -->
    <div v-if="showEventModal" class="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-2xl shadow-xl w-full max-w-md">
        <div class="flex items-center justify-between px-6 py-4 border-b">
          <h3 class="text-base font-bold text-gray-900">{{ $t('securityEvent.addEvent') }}</h3>
          <button @click="showEventModal = false" class="text-gray-400 hover:text-gray-600">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <div class="px-6 py-5 space-y-4">
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block text-xs font-semibold text-gray-700 mb-1">{{ $t('securityEvent.severity') }} *</label>
              <select v-model="eventForm.severity" class="input w-full text-sm">
                <option value="">선택</option>
                <option v-for="s in severities" :key="s" :value="s">{{ $t(`securityEvent.severity_label.${s}`) }}</option>
              </select>
            </div>
            <div>
              <label class="block text-xs font-semibold text-gray-700 mb-1">{{ $t('securityEvent.eventType') }} *</label>
              <input v-model="eventForm.eventType" class="input w-full text-sm" placeholder="예: INTRUSION_DETECTED" />
            </div>
          </div>
          <div class="grid grid-cols-2 gap-3">
            <div>
              <label class="block text-xs font-semibold text-gray-700 mb-1">{{ $t('securityEvent.sourceIp') }}</label>
              <input v-model="eventForm.sourceIp" class="input w-full text-sm font-mono" placeholder="0.0.0.0" />
            </div>
            <div>
              <label class="block text-xs font-semibold text-gray-700 mb-1">{{ $t('securityEvent.destinationIp') }}</label>
              <input v-model="eventForm.destinationIp" class="input w-full text-sm font-mono" placeholder="0.0.0.0" />
            </div>
          </div>
          <div>
            <label class="block text-xs font-semibold text-gray-700 mb-1">{{ $t('securityEvent.message') }} *</label>
            <textarea v-model="eventForm.message" class="input w-full text-sm" rows="3" placeholder="이벤트 메시지"></textarea>
          </div>
          <div v-if="eventFormError" class="text-xs text-red-600">{{ eventFormError }}</div>
        </div>
        <div class="px-6 py-4 border-t flex justify-end gap-3">
          <button @click="showEventModal = false" class="btn-secondary text-sm">{{ $t('common.cancel') }}</button>
          <button @click="saveEvent" :disabled="savingEvent" class="btn-primary text-sm">
            {{ savingEvent ? $t('common.loading') : $t('common.create') }}
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, onBeforeUnmount } from 'vue'
import { RouterLink } from 'vue-router'
import { securityIntegrationApi, codeApi, assetApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const isManager = auth.isManager

// 연동 목록
const integrations = ref([])
const loadingIntegrations = ref(false)
const selected = ref(null)

// 이벤트 목록
const events = ref([])
const loadingEvents = ref(false)
const eventPage = ref(0)
const totalPages = ref(0)
const totalEvents = ref(0)
const severityFilter = ref('')

// 모달
const showIntegrationModal = ref(false)
const editingIntegration = ref(null)
const integrationForm = ref({ name: '', solutionType: '', vendor: '', host: '', apiKey: '', description: '' })
const integrationFormError = ref('')
const savingIntegration = ref(false)

const showEventModal = ref(false)
const eventForm = ref({ severity: '', eventType: '', sourceIp: '', destinationIp: '', message: '' })
const eventFormError = ref('')
const savingEvent = ref(false)

// 코드관리에서 동적 로드
const solutionTypeCodes = ref([])
const severities = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO']

// 자산 자동완성
const allAssets = ref([])
const nameComboRef = ref(null)
const showAssetDropdown = ref(false)
const assetDropdownIdx = ref(-1)

const filteredAssets = computed(() => {
  const q = (integrationForm.value.name || '').trim().toLowerCase()
  if (!q) return allAssets.value.slice(0, 20)
  return allAssets.value.filter(a => a.name.toLowerCase().includes(q)).slice(0, 20)
})

function onNameInput() {
  showAssetDropdown.value = true
  assetDropdownIdx.value = -1
}

function selectAsset(asset) {
  if (!asset) return
  integrationForm.value.name = asset.name
  if (!integrationForm.value.host && asset.ipAddress) integrationForm.value.host = asset.ipAddress
  showAssetDropdown.value = false
  assetDropdownIdx.value = -1
}

function onClickOutside(e) {
  if (nameComboRef.value && !nameComboRef.value.contains(e.target)) {
    showAssetDropdown.value = false
  }
}

let autoRefreshTimer = null

async function loadIntegrations() {
  loadingIntegrations.value = true
  try {
    const data = await securityIntegrationApi.list()
    integrations.value = data.data || []
    if (selected.value) {
      const updated = integrations.value.find(i => i.id === selected.value.id)
      if (updated) selected.value = updated
    }
  } finally {
    loadingIntegrations.value = false
  }
}

async function loadEvents() {
  if (!selected.value) return
  loadingEvents.value = true
  try {
    const params = { page: eventPage.value, size: 50 }
    if (severityFilter.value) params.severity = severityFilter.value
    const data = await securityIntegrationApi.listEvents(selected.value.id, params)
    events.value = data.data?.content || []
    totalPages.value = data.data?.totalPages || 0
    totalEvents.value = data.data?.totalElements || 0
  } finally {
    loadingEvents.value = false
  }
}

function selectIntegration(item) {
  selected.value = item
  eventPage.value = 0
  severityFilter.value = ''
  loadEvents()
}

function openCreateModal() {
  editingIntegration.value = null
  integrationForm.value = { name: '', solutionType: '', vendor: '', host: '', apiKey: '', description: '' }
  integrationFormError.value = ''
  showIntegrationModal.value = true
}

function openEditModalFor(item) {
  editingIntegration.value = item
  integrationForm.value = {
    name: item.name,
    solutionType: item.solutionType,
    vendor: item.vendor || '',
    host: item.host || '',
    apiKey: '',
    description: item.description || ''
  }
  integrationFormError.value = ''
  showIntegrationModal.value = true
}

async function saveIntegration() {
  if (!integrationForm.value.name || !integrationForm.value.solutionType) {
    integrationFormError.value = '솔루션 이름과 유형은 필수입니다.'
    return
  }
  savingIntegration.value = true
  integrationFormError.value = ''
  try {
    const payload = { ...integrationForm.value }
    if (!payload.apiKey) delete payload.apiKey
    if (editingIntegration.value) {
      await securityIntegrationApi.update(editingIntegration.value.id, payload)
    } else {
      await securityIntegrationApi.create(payload)
    }
    showIntegrationModal.value = false
    await loadIntegrations()
  } catch (e) {
    integrationFormError.value = e || '저장 중 오류가 발생했습니다.'
  } finally {
    savingIntegration.value = false
  }
}

async function confirmDeleteItem(item) {
  if (!confirm(`"${item.name}" 연동을 삭제하시겠습니까?\n연동된 모든 이벤트도 함께 삭제됩니다.`)) return
  await securityIntegrationApi.delete(item.id)
  if (selected.value?.id === item.id) {
    selected.value = null
    events.value = []
  }
  await loadIntegrations()
}

function openEventModal() {
  eventForm.value = { severity: '', eventType: '', sourceIp: '', destinationIp: '', message: '' }
  eventFormError.value = ''
  showEventModal.value = true
}

async function saveEvent() {
  if (!eventForm.value.severity || !eventForm.value.eventType || !eventForm.value.message) {
    eventFormError.value = '심각도, 이벤트 유형, 메시지는 필수입니다.'
    return
  }
  savingEvent.value = true
  eventFormError.value = ''
  try {
    await securityIntegrationApi.createEvent(selected.value.id, eventForm.value)
    showEventModal.value = false
    await loadEvents()
    await loadIntegrations()
  } catch (e) {
    eventFormError.value = e || '등록 중 오류가 발생했습니다.'
  } finally {
    savingEvent.value = false
  }
}

async function deleteEvent(eventId) {
  if (!confirm('이 이벤트를 삭제하시겠습니까?')) return
  await securityIntegrationApi.deleteEvent(eventId)
  await loadEvents()
  await loadIntegrations()
}

// 30초마다 자동 새로고침
function startAutoRefresh() {
  autoRefreshTimer = setInterval(() => { if (selected.value) loadEvents() }, 30000)
}

function severityCount(severity) {
  return events.value.filter(e => e.severity === severity).length
}

function formatDate(dt) {
  if (!dt) return '-'
  const d = new Date(dt)
  return d.toLocaleString('ko-KR', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function solutionTypeLabel(type) {
  const found = solutionTypeCodes.value.find(c => c.value === type)
  if (found) return found.label
  const fallback = {
    FIREWALL: '방화벽', IDS_IPS: 'IDS/IPS', WAF: 'WAF', SIEM: 'SIEM',
    EDR: 'EDR', DLP: 'DLP', ANTIVIRUS: '안티바이러스', NAC: 'NAC', VPN: 'VPN', OTHER: '기타'
  }
  return fallback[type] || type
}

function solutionTypeColor(type) {
  const map = {
    FIREWALL: 'bg-orange-500', IDS_IPS: 'bg-red-500', WAF: 'bg-purple-500',
    SIEM: 'bg-blue-600', EDR: 'bg-green-600', DLP: 'bg-yellow-600',
    ANTIVIRUS: 'bg-teal-500', NAC: 'bg-indigo-500', VPN: 'bg-cyan-600', OTHER: 'bg-gray-500'
  }
  return map[type] || 'bg-gray-500'
}

function solutionTypeAbbr(type) {
  const map = {
    FIREWALL: 'FW', IDS_IPS: 'IDS', WAF: 'WAF', SIEM: 'SIEM',
    EDR: 'EDR', DLP: 'DLP', ANTIVIRUS: 'AV', NAC: 'NAC', VPN: 'VPN', OTHER: 'ETC'
  }
  return map[type] || type.slice(0, 3)
}

function statusColor(s) {
  return { CONNECTED: 'text-green-600', DISCONNECTED: 'text-gray-400', ERROR: 'text-red-500' }[s] || 'text-gray-400'
}

function statusDot(s) {
  return { CONNECTED: 'bg-green-500', DISCONNECTED: 'bg-gray-300', ERROR: 'bg-red-500' }[s] || 'bg-gray-300'
}

function severityBadge(s) {
  return {
    CRITICAL: 'bg-red-100 text-red-700',
    HIGH: 'bg-orange-100 text-orange-700',
    MEDIUM: 'bg-yellow-100 text-yellow-700',
    LOW: 'bg-blue-100 text-blue-700',
    INFO: 'bg-gray-100 text-gray-600'
  }[s] || 'bg-gray-100 text-gray-600'
}

function severityDot(s) {
  return {
    CRITICAL: 'bg-red-500', HIGH: 'bg-orange-500', MEDIUM: 'bg-yellow-500',
    LOW: 'bg-blue-500', INFO: 'bg-gray-400'
  }[s] || 'bg-gray-400'
}

function severityTextColor(s) {
  return {
    CRITICAL: 'text-red-600', HIGH: 'text-orange-600', MEDIUM: 'text-yellow-600',
    LOW: 'text-blue-600', INFO: 'text-gray-500'
  }[s] || 'text-gray-500'
}

onMounted(() => {
  loadIntegrations()
  startAutoRefresh()
  loadSolutionTypeCodes()
  loadAssets()
  document.addEventListener('click', onClickOutside)
})

onUnmounted(() => {
  if (autoRefreshTimer) clearInterval(autoRefreshTimer)
  document.removeEventListener('click', onClickOutside)
})

async function loadSolutionTypeCodes() {
  try {
    const res = await codeApi.getValues('SECURITY_SOLUTION_TYPE')
    solutionTypeCodes.value = res.data || []
  } catch {
    solutionTypeCodes.value = []
  }
}

async function loadAssets() {
  try {
    const res = await assetApi.list({ size: 200, active: true })
    allAssets.value = res.data?.content || []
  } catch {
    allAssets.value = []
  }
}
</script>
