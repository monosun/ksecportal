<template>
  <div>
    <div class="page-header">
      <h1 class="page-title">{{ $t('admin.performance') }}</h1>
    </div>

    <div class="page-body">

    <!-- 기준 설정 -->
    <div class="card mb-4">
      <div class="flex items-center justify-between mb-3">
        <h2 class="text-sm font-semibold text-gray-700">기록 기준</h2>
        <span class="text-xs text-gray-400">기준을 넘긴 화면 요청과 SQL만 기록됩니다</span>
      </div>
      <div class="flex flex-wrap gap-4 items-end">
        <div class="flex flex-col gap-1">
          <label class="text-xs text-gray-500">지연 기준 (초)</label>
          <input v-model.number="configForm.thresholdSec" type="number" min="0.1" max="600" step="0.1"
            class="input w-28 text-sm text-center" />
        </div>
        <div class="flex flex-col gap-1">
          <label class="text-xs text-gray-500">보관 기간 (일)</label>
          <input v-model.number="configForm.retentionDays" type="number" min="1" max="3650"
            class="input w-28 text-sm text-center" />
        </div>
        <label class="flex items-center gap-2 text-sm text-gray-700 pb-2 cursor-pointer select-none">
          <input v-model="configForm.enabled" type="checkbox" class="w-4 h-4 text-primary-600" />
          성능 기록 사용
        </label>
        <label class="flex items-center gap-2 text-sm text-gray-700 pb-2 cursor-pointer select-none"
          :class="configForm.enabled ? '' : 'opacity-40'">
          <input v-model="configForm.sqlEnabled" type="checkbox" :disabled="!configForm.enabled"
            class="w-4 h-4 text-primary-600" />
          SQL 기록 포함
        </label>
        <button @click="saveConfig" :disabled="configSaving" class="btn-primary text-sm disabled:opacity-50">
          {{ configSaving ? '저장 중...' : '기준 저장' }}
        </button>
        <span v-if="configSaved" class="text-xs text-green-600 pb-2">저장되었습니다</span>
        <div class="flex-1"></div>
        <button @click="confirmPurge" class="text-sm text-red-500 hover:text-red-700 border border-red-200 rounded-lg px-3 py-1.5 hover:bg-red-50">
          기록 삭제
        </button>
      </div>
      <p class="text-xs text-gray-400 mt-2">
        현재 기준: <strong class="text-gray-600">{{ (config.thresholdMs / 1000).toFixed(1) }}초</strong> 이상 ·
        보관 {{ config.retentionDays }}일 · 기록은 5초 간격으로 저장됩니다
      </p>
    </div>

    <!-- 요약 -->
    <div class="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-4">
      <div class="card text-center">
        <p class="text-3xl font-bold text-gray-900">{{ stats.total }}</p>
        <p class="text-sm text-gray-500 mt-1">전체 기록</p>
      </div>
      <div class="card text-center">
        <p class="text-3xl font-bold text-blue-600">{{ stats.screenCount }}</p>
        <p class="text-sm text-gray-500 mt-1">화면(요청)</p>
      </div>
      <div class="card text-center">
        <p class="text-3xl font-bold text-purple-600">{{ stats.sqlCount }}</p>
        <p class="text-sm text-gray-500 mt-1">SQL</p>
      </div>
      <div class="card text-center">
        <p class="text-3xl font-bold text-red-600">{{ formatMs(stats.maxDurationMs) }}</p>
        <p class="text-sm text-gray-500 mt-1">최대 소요시간</p>
      </div>
    </div>

    <!-- 필터 -->
    <div class="card mb-4">
      <div class="flex flex-wrap gap-3 items-end">
        <div class="flex flex-col gap-1">
          <label class="text-xs text-gray-500">유형</label>
          <select v-model="filters.logType" @change="search" class="input w-36 text-sm">
            <option value="">전체</option>
            <option value="SCREEN">화면(요청)</option>
            <option value="SQL">SQL</option>
          </select>
        </div>
        <div class="flex flex-col gap-1">
          <label class="text-xs text-gray-500">검색어</label>
          <input v-model="filters.keyword" @input="debouncedSearch" placeholder="URL·SQL 내용 검색"
            class="input w-64 text-sm" />
        </div>
        <div class="flex flex-col gap-1">
          <label class="text-xs text-gray-500">최소 소요시간 (초)</label>
          <input v-model.number="minSec" @input="debouncedSearch" type="number" min="0" step="0.1"
            class="input w-28 text-sm text-center" />
        </div>
        <div class="flex items-end gap-1.5">
          <label class="text-xs text-gray-500 whitespace-nowrap pb-1.5">시작</label>
          <input type="datetime-local" v-model="filters.dateFrom" @change="search" class="input w-44 text-sm" />
        </div>
        <div class="flex items-end gap-1.5">
          <label class="text-xs text-gray-500 whitespace-nowrap pb-1.5">종료</label>
          <input type="datetime-local" v-model="filters.dateTo" @change="search" class="input w-44 text-sm" />
        </div>
        <div class="flex items-end gap-2">
          <button @click="resetFilters" class="btn-secondary text-sm">전체</button>
          <button @click="reload" class="btn-secondary text-sm">새로고침</button>
          <div class="flex items-center gap-1.5">
            <label class="text-xs text-gray-500 whitespace-nowrap">행/페이지</label>
            <input v-model.number="pageSize" @change="onPageSizeChange" type="number" min="5" max="200"
              class="input w-16 text-sm text-center" />
          </div>
        </div>
      </div>
    </div>

    <!-- 목록 -->
    <div class="card">
      <div v-if="loading" class="text-center py-10 text-gray-500">불러오는 중...</div>
      <div v-else-if="logs.length === 0" class="text-center py-10 text-gray-400">
        기준을 넘긴 기록이 없습니다.
      </div>
      <template v-else>
        <div class="overflow-x-auto"><table class="w-full text-sm">
          <thead>
            <tr class="border-b">
              <th class="text-left py-3 px-4 font-semibold text-gray-600 whitespace-nowrap">발생시각</th>
              <th class="text-left py-3 px-4 font-semibold text-gray-600 w-24">유형</th>
              <th class="text-right py-3 px-4 font-semibold text-gray-600 w-28">소요시간</th>
              <th class="text-left py-3 px-4 font-semibold text-gray-600">대상</th>
              <th class="text-left py-3 px-4 font-semibold text-gray-600 w-32">사용자</th>
              <th class="text-left py-3 px-4 font-semibold text-gray-600 w-24">상태</th>
            </tr>
          </thead>
          <tbody>
            <template v-for="log in logs" :key="log.id">
              <tr class="border-b hover:bg-gray-50 cursor-pointer" @click="toggle(log.id)">
                <td class="py-3 px-4 text-gray-500 whitespace-nowrap">{{ formatDate(log.occurredAt) }}</td>
                <td class="py-3 px-4">
                  <span class="text-xs font-medium px-2 py-0.5 rounded"
                    :class="log.logType === 'SQL' ? 'bg-purple-100 text-purple-700' : 'bg-blue-100 text-blue-700'">
                    {{ log.logType === 'SQL' ? 'SQL' : '화면' }}
                  </span>
                </td>
                <td class="py-3 px-4 text-right font-mono font-semibold" :class="durationClass(log.durationMs)">
                  {{ formatMs(log.durationMs) }}
                </td>
                <td class="py-3 px-4 text-gray-700 max-w-md truncate" :title="log.target">{{ log.target }}</td>
                <td class="py-3 px-4 text-gray-500">{{ log.username || '-' }}</td>
                <td class="py-3 px-4 text-gray-400">{{ log.statusCode || '-' }}</td>
              </tr>
              <tr v-if="expandedId === log.id" class="border-b bg-gray-50/60">
                <td colspan="6" class="py-3 px-4">
                  <div class="text-xs text-gray-600 space-y-1">
                    <p><span class="font-semibold text-gray-700">대상</span>
                      <span class="font-mono break-all ml-2">{{ log.target }}</span></p>
                    <p v-if="log.detail"><span class="font-semibold text-gray-700">상세</span>
                      <span class="font-mono break-all ml-2">{{ log.detail }}</span></p>
                    <p class="text-gray-400">
                      기록 시점 기준 {{ (log.thresholdMs / 1000).toFixed(1) }}초 ·
                      {{ log.httpMethod || '-' }} · IP {{ log.ipAddress || '-' }}
                    </p>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table></div>
      </template>
    </div>

    <!-- 페이지네이션 -->
    <div v-if="totalPages > 1" class="flex justify-center items-center gap-1 mt-4">
      <button @click="goPage(0)" :disabled="page === 0"
        class="px-2 py-1.5 rounded border text-xs text-gray-500 border-gray-300 hover:bg-gray-50 disabled:opacity-30">«</button>
      <button @click="goPage(page - 1)" :disabled="page === 0"
        class="px-2 py-1.5 rounded border text-xs text-gray-500 border-gray-300 hover:bg-gray-50 disabled:opacity-30">‹</button>
      <span class="px-3 text-xs text-gray-500">{{ page + 1 }} / {{ totalPages }}</span>
      <button @click="goPage(page + 1)" :disabled="page >= totalPages - 1"
        class="px-2 py-1.5 rounded border text-xs text-gray-500 border-gray-300 hover:bg-gray-50 disabled:opacity-30">›</button>
      <button @click="goPage(totalPages - 1)" :disabled="page >= totalPages - 1"
        class="px-2 py-1.5 rounded border text-xs text-gray-500 border-gray-300 hover:bg-gray-50 disabled:opacity-30">»</button>
    </div>

    <!-- 삭제 확인 -->
    <div v-if="showPurgeModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-sm p-6">
        <h2 class="text-lg font-semibold text-gray-900 mb-2">성능 기록 삭제</h2>
        <p class="text-sm text-gray-600 mb-4">삭제할 범위를 선택하세요. 되돌릴 수 없습니다.</p>
        <select v-model.number="purgeDays" class="input w-full text-sm">
          <option :value="0">전체 삭제</option>
          <option :value="7">7일 이전 기록 삭제</option>
          <option :value="30">30일 이전 기록 삭제</option>
          <option :value="90">90일 이전 기록 삭제</option>
        </select>
        <div class="flex justify-end gap-3 mt-6">
          <button @click="showPurgeModal = false" class="px-4 py-2 text-sm border border-gray-300 rounded-lg hover:bg-gray-50">취소</button>
          <button @click="doPurge" :disabled="purging"
            class="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50">
            {{ purging ? '삭제 중...' : '삭제' }}
          </button>
        </div>
      </div>
    </div>

    </div><!-- /page-body -->
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { performanceApi } from '@/api'

const logs = ref([])
const loading = ref(true)
const page = ref(0)
const pageSize = ref(50)
const totalPages = ref(0)
const expandedId = ref(null)

const filters = reactive({ logType: '', keyword: '', dateFrom: '', dateTo: '' })
const minSec = ref(null)

const config = ref({ thresholdMs: 3000, enabled: true, sqlEnabled: true, retentionDays: 30 })
const configForm = reactive({ thresholdSec: 3, enabled: true, sqlEnabled: true, retentionDays: 30 })
const configSaving = ref(false)
const configSaved = ref(false)

const stats = ref({ total: 0, screenCount: 0, sqlCount: 0, maxDurationMs: 0 })

const showPurgeModal = ref(false)
const purgeDays = ref(0)
const purging = ref(false)

let searchTimer = null

function formatDate(dt) {
  if (!dt) return '-'
  return String(dt).replace('T', ' ').slice(0, 19)
}

function formatMs(ms) {
  if (!ms && ms !== 0) return '-'
  return ms >= 1000 ? `${(ms / 1000).toFixed(2)}초` : `${ms}ms`
}

function durationClass(ms) {
  if (ms >= 10000) return 'text-red-600'
  if (ms >= 5000) return 'text-orange-500'
  return 'text-gray-700'
}

function toggle(id) { expandedId.value = expandedId.value === id ? null : id }

async function loadLogs() {
  loading.value = true
  try {
    const params = { page: page.value, size: pageSize.value }
    if (filters.logType) params.logType = filters.logType
    if (filters.keyword) params.keyword = filters.keyword
    if (minSec.value) params.minMs = Math.round(minSec.value * 1000)
    if (filters.dateFrom) params.dateFrom = filters.dateFrom
    if (filters.dateTo) params.dateTo = filters.dateTo
    const res = await performanceApi.listLogs(params)
    const data = res.data ?? res
    logs.value = data.content ?? []
    totalPages.value = data.totalPages ?? 0
  } catch (e) {
    logs.value = []
    totalPages.value = 0
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const res = await performanceApi.stats()
    stats.value = res.data ?? res
  } catch { /* 통계 실패는 화면을 막지 않음 */ }
}

async function loadConfig() {
  try {
    const res = await performanceApi.getConfig()
    const c = res.data ?? res
    config.value = c
    configForm.thresholdSec = Number((c.thresholdMs / 1000).toFixed(1))
    configForm.enabled = c.enabled
    configForm.sqlEnabled = c.sqlEnabled
    configForm.retentionDays = c.retentionDays
  } catch { /* 기본값 유지 */ }
}

async function saveConfig() {
  configSaving.value = true
  configSaved.value = false
  try {
    const res = await performanceApi.saveConfig({
      thresholdMs: Math.round((configForm.thresholdSec || 3) * 1000),
      enabled: configForm.enabled,
      sqlEnabled: configForm.sqlEnabled,
      retentionDays: configForm.retentionDays,
    })
    config.value = res.data ?? res
    configSaved.value = true
    setTimeout(() => { configSaved.value = false }, 2500)
  } catch (e) {
    alert(e || '기준 저장에 실패했습니다.')
  } finally {
    configSaving.value = false
  }
}

function search() {
  page.value = 0
  loadLogs()
}

function debouncedSearch() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(search, 400)
}

function resetFilters() {
  filters.logType = ''
  filters.keyword = ''
  filters.dateFrom = ''
  filters.dateTo = ''
  minSec.value = null
  search()
}

function reload() {
  loadLogs()
  loadStats()
}

function onPageSizeChange() {
  if (pageSize.value < 5) pageSize.value = 5
  if (pageSize.value > 200) pageSize.value = 200
  search()
}

function goPage(p) {
  if (p < 0 || p >= totalPages.value) return
  page.value = p
  loadLogs()
}

function confirmPurge() { showPurgeModal.value = true }

async function doPurge() {
  purging.value = true
  try {
    const res = await performanceApi.purge(purgeDays.value || null)
    const deleted = res.data ?? res
    showPurgeModal.value = false
    alert(`${deleted}건이 삭제되었습니다.`)
    reload()
  } catch (e) {
    alert(e || '삭제에 실패했습니다.')
  } finally {
    purging.value = false
  }
}

onMounted(async () => {
  await loadConfig()
  await Promise.all([loadLogs(), loadStats()])
})
</script>
