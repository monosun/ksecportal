<template>
  <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
    <div class="absolute inset-0 bg-black/40" @click="$emit('close')"></div>

    <div class="relative bg-white rounded-xl shadow-xl w-full max-w-6xl max-h-[92vh] flex flex-col">
      <!-- Header -->
      <div class="flex items-center justify-between px-5 py-3 border-b shrink-0">
        <div class="flex items-center gap-3 min-w-0">
          <button v-if="selected" @click="selected = null" class="text-gray-400 hover:text-gray-700" title="목록으로">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
            </svg>
          </button>
          <h2 class="text-lg font-semibold text-gray-900 truncate">
            <template v-if="selected">{{ selected.title || '제목 없음' }} — {{ formatDateTime(selected.createdAt) }}</template>
            <template v-else>자산 시점 이력</template>
          </h2>
        </div>
        <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600 p-1">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <!-- 시점 저장 바 (목록 화면에서만) -->
      <div v-if="!selected" class="px-5 py-3 border-b bg-gray-50 shrink-0">
        <div class="flex flex-wrap items-end gap-3">
          <div class="flex-1 min-w-48">
            <label class="block text-xs font-medium text-gray-600 mb-1">시점 제목 (선택)</label>
            <input v-model="newTitle" class="input w-full" placeholder="예: 2026년 상반기 자산 현황" @keyup.enter="saveSnapshot" />
          </div>
          <button @click="saveSnapshot" :disabled="saving" class="btn-primary text-sm flex items-center gap-2 whitespace-nowrap">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7H5a2 2 0 00-2 2v9a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-3m-1 4l-3 3m0 0l-3-3m3 3V4"/>
            </svg>
            {{ saving ? '저장 중...' : '현재 자산 목록 저장' }}
          </button>
        </div>
        <p class="text-xs text-gray-400 mt-2">저장 시점의 전체 자산 목록이 날짜·시간과 함께 이력으로 보관됩니다. 이후 자산이 변경·삭제되어도 시점 기록은 그대로 유지됩니다.</p>
      </div>

      <!-- Body -->
      <div class="px-5 py-4 overflow-y-auto flex-1">
        <div v-if="loading" class="py-16 text-center text-gray-400">불러오는 중...</div>

        <!-- 시점 목록 -->
        <template v-else-if="!selected">
          <div v-if="snapshots.length === 0" class="py-12 text-center text-gray-400 text-sm">
            저장된 시점 이력이 없습니다. 위에서 "현재 자산 목록 저장"을 눌러 첫 시점을 만들어 보세요.
          </div>
          <div v-else class="overflow-x-auto">
            <table class="w-full text-sm">
              <thead class="bg-gray-50 border-b">
                <tr>
                  <th class="text-left px-4 py-2 text-xs font-medium text-gray-500">저장 일시</th>
                  <th class="text-left px-4 py-2 text-xs font-medium text-gray-500">제목</th>
                  <th class="text-right px-4 py-2 text-xs font-medium text-gray-500">자산 수</th>
                  <th class="text-left px-4 py-2 text-xs font-medium text-gray-500">저장자</th>
                  <th class="text-right px-4 py-2 text-xs font-medium text-gray-500">작업</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-50">
                <tr v-for="s in snapshots" :key="s.id" class="hover:bg-gray-50 cursor-pointer" @click="openSnapshot(s)">
                  <td class="px-4 py-2.5 font-mono text-xs text-gray-700">{{ formatDateTime(s.createdAt) }}</td>
                  <td class="px-4 py-2.5 text-gray-800">{{ s.title || '-' }}</td>
                  <td class="px-4 py-2.5 text-right font-bold text-blue-700">{{ s.assetCount }}</td>
                  <td class="px-4 py-2.5 text-gray-600">{{ s.createdBy || '-' }}</td>
                  <td class="px-4 py-2.5 text-right" @click.stop>
                    <button @click="openSnapshot(s)" class="text-blue-600 hover:underline text-xs mr-3">보기</button>
                    <button v-if="isAdmin" @click="removeSnapshot(s)" class="text-red-500 hover:underline text-xs">삭제</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>

        <!-- 선택한 시점의 자산 목록 -->
        <template v-else>
          <p v-if="selected.memo" class="text-sm text-gray-600 bg-gray-50 rounded-lg p-3 mb-3 whitespace-pre-wrap">{{ selected.memo }}</p>
          <div class="flex items-center gap-2 mb-3 text-xs text-gray-500">
            <span class="badge-blue">자산 {{ items.length }}개</span>
            <span>저장자 {{ selected.createdBy || '-' }}</span>
          </div>
          <div v-if="items.length === 0" class="py-12 text-center text-gray-400 text-sm">이 시점에 저장된 자산이 없습니다.</div>
          <div v-else class="overflow-x-auto border rounded-lg">
            <table class="w-full text-sm">
              <thead class="bg-gray-50 border-b">
                <tr>
                  <th class="text-left px-4 py-2 text-xs font-medium text-gray-500">자산명</th>
                  <th class="text-left px-4 py-2 text-xs font-medium text-gray-500">유형</th>
                  <th class="text-left px-4 py-2 text-xs font-medium text-gray-500">위치 / 리소스</th>
                  <th class="text-left px-4 py-2 text-xs font-medium text-gray-500">담당자</th>
                  <th class="text-left px-4 py-2 text-xs font-medium text-gray-500">운영담당자</th>
                  <th class="text-left px-4 py-2 text-xs font-medium text-gray-500">중요도</th>
                  <th class="text-left px-4 py-2 text-xs font-medium text-gray-500">상태</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-50">
                <tr v-for="(it, idx) in items" :key="idx" class="hover:bg-gray-50">
                  <td class="px-4 py-2.5">
                    <p class="font-medium text-gray-900">{{ it.name }}</p>
                    <span v-if="it.environment" :class="environmentBadge(it.environment)" class="text-xs mt-0.5 inline-block">
                      {{ $t(`asset.environment_label.${it.environment}`) }}
                    </span>
                  </td>
                  <td class="px-4 py-2.5 text-gray-700">{{ typeLabel(it.type) }}</td>
                  <td class="px-4 py-2.5 text-xs">
                    <p v-if="it.location" class="text-gray-700">{{ it.location }}</p>
                    <p class="text-gray-500 font-mono">
                      <span v-if="it.ipAddress">{{ it.ipAddress }}</span>
                      <span v-else-if="it.region">{{ it.region }}</span>
                      <span v-else-if="!it.location">-</span>
                    </p>
                  </td>
                  <td class="px-4 py-2.5 text-gray-600">{{ it.owner || '-' }}</td>
                  <td class="px-4 py-2.5 text-gray-600">{{ it.operator || '-' }}</td>
                  <td class="px-4 py-2.5"><span :class="criticalityClass(it.criticality)">{{ $t(`asset.criticality_label.${it.criticality}`) }}</span></td>
                  <td class="px-4 py-2.5"><span :class="statusBadge(it.status)">{{ statusLabel(it.status) }}</span></td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
      </div>

      <!-- Footer -->
      <div class="flex justify-end px-5 py-3 border-t shrink-0">
        <button type="button" @click="$emit('close')" class="btn-secondary text-sm">닫기</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { assetApi, codeApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  open: { type: Boolean, default: false }
})
const emit = defineEmits(['close', 'saved'])

const { t } = useI18n()
const auth = useAuthStore()
const isAdmin = auth.isAdmin

const snapshots = ref([])
const items = ref([])
const selected = ref(null)
const loading = ref(false)
const saving = ref(false)
const newTitle = ref('')
const assetTypes = ref([])

watch(() => props.open, async (open) => {
  if (!open) return
  selected.value = null
  newTitle.value = ''
  if (!assetTypes.value.length) {
    try { assetTypes.value = (await codeApi.getValues('ASSET_TYPE')).data || [] } catch { assetTypes.value = [] }
  }
  await loadSnapshots()
})

async function loadSnapshots() {
  loading.value = true
  try { snapshots.value = (await assetApi.listSnapshots()).data ?? [] }
  catch { snapshots.value = [] }
  finally { loading.value = false }
}

async function saveSnapshot() {
  saving.value = true
  try {
    await assetApi.createSnapshot({ title: newTitle.value?.trim() || null })
    newTitle.value = ''
    await loadSnapshots()
    emit('saved')
  } catch (e) {
    alert(e || '시점 저장에 실패했습니다.')
  } finally {
    saving.value = false
  }
}

async function openSnapshot(s) {
  selected.value = s
  loading.value = true
  try { items.value = (await assetApi.snapshotItems(s.id)).data ?? [] }
  catch { items.value = [] }
  finally { loading.value = false }
}

async function removeSnapshot(s) {
  if (!confirm(`${formatDateTime(s.createdAt)} 시점(자산 ${s.assetCount}개)을 삭제하시겠습니까?\n삭제된 이력은 복구할 수 없습니다.`)) return
  try {
    await assetApi.deleteSnapshot(s.id)
    await loadSnapshots()
  } catch (e) {
    alert(e || '삭제에 실패했습니다.')
  }
}

function typeLabel(value) {
  if (!value) return '-'
  const found = assetTypes.value.find(x => x.value === value)
  if (found) return found.label
  const key = `asset.type_label.${value}`
  const translated = t(key)
  return translated === key ? value : translated
}

function formatDateTime(dt) {
  if (!dt) return '-'
  return new Date(dt).toLocaleString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}
function statusLabel(v) { return { OPERATIONAL: '운영중', SUSPENDED: '중지', DISPOSED: '폐기' }[v] || (v || '-') }
function statusBadge(v) { return { OPERATIONAL: 'badge-success', SUSPENDED: 'badge-yellow', DISPOSED: 'badge-danger' }[v] || 'badge-gray' }
function criticalityClass(c) { return { HIGH: 'badge-red', MEDIUM: 'badge-yellow', LOW: 'badge-green' }[c] || 'badge-gray' }
function environmentBadge(e) { return { PRODUCTION: 'badge-red', STAGING: 'badge-yellow', DEVELOPMENT: 'badge-blue', TEST: 'badge-gray' }[e] || 'badge-gray' }
</script>
