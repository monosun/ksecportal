<template>
  <div>
    <div class="page-header">
      <h1 class="page-title">{{ $t('admin.errorLogs') }}</h1>
      <PiMaskToggle screen="에러 로그 관리" />
    </div>

    <div class="page-body">

      <!-- 요약 -->
      <div class="grid grid-cols-2 md:grid-cols-4 gap-3 mb-4">
        <button v-for="card in cards" :key="card.key" @click="applyStatusCard(card)"
          :class="[
            'card text-left transition-colors hover:border-primary-300',
            card.status && filters.status === card.status ? 'border-primary-400 ring-1 ring-primary-200' : ''
          ]">
          <div class="text-xs text-gray-500">{{ card.label }}</div>
          <div class="text-2xl font-semibold mt-1" :class="card.color">{{ card.value }}</div>
          <div class="text-[11px] text-gray-400 mt-0.5">{{ card.hint }}</div>
        </button>
      </div>

      <!-- 반복되는 오류 -->
      <div v-if="stats.top?.length" class="card mb-4">
        <div class="text-sm font-semibold text-gray-700 mb-2">최근 7일 반복 오류 Top {{ stats.top.length }}</div>
        <div class="overflow-x-auto"><table class="w-full text-sm">
          <tbody>
            <tr v-for="(t, i) in stats.top" :key="i" class="border-b last:border-0">
              <td class="py-1.5 pr-3 text-gray-400 w-6">{{ i + 1 }}</td>
              <td class="py-1.5 pr-3 font-medium text-gray-800 truncate max-w-xs" :title="t.exceptionType">
                {{ shortType(t.exceptionType) }}
              </td>
              <td class="py-1.5 pr-3 text-gray-500 truncate max-w-md" :title="t.requestUri">{{ t.requestUri || '-' }}</td>
              <td class="py-1.5 pr-3 text-right whitespace-nowrap">
                <span class="text-xs font-semibold px-2 py-0.5 rounded bg-red-50 text-red-600">{{ t.count }}건</span>
              </td>
              <td class="py-1.5 text-right text-xs text-gray-400 whitespace-nowrap">{{ formatDate(t.lastOccurredAt) }}</td>
            </tr>
          </tbody>
        </table></div>
      </div>

      <!-- 필터 -->
      <div class="card mb-4">
        <div class="flex flex-wrap gap-3 items-end">
          <div class="flex flex-col gap-1">
            <label class="text-xs text-gray-500">등급</label>
            <select v-model="filters.level" @change="reload" class="input w-28 text-sm">
              <option value="">전체</option>
              <option value="ERROR">오류</option>
              <option value="WARN">경고</option>
            </select>
          </div>

          <div class="flex flex-col gap-1">
            <label class="text-xs text-gray-500">발생 위치</label>
            <select v-model="filters.source" @change="reload" class="input w-32 text-sm">
              <option value="">전체</option>
              <option v-for="(label, key) in SOURCE_LABEL" :key="key" :value="key">{{ label }}</option>
            </select>
          </div>

          <div class="flex flex-col gap-1">
            <label class="text-xs text-gray-500">처리 상태</label>
            <select v-model="filters.status" @change="reload" class="input w-32 text-sm">
              <option value="">전체</option>
              <option v-for="(label, key) in STATUS_LABEL" :key="key" :value="key">{{ label }}</option>
            </select>
          </div>

          <div class="flex flex-col gap-1">
            <label class="text-xs text-gray-500">검색어</label>
            <input v-model="filters.keyword" @input="search" class="input w-56 text-sm"
              placeholder="오류 종류·메시지·요청 경로·사용자" />
          </div>

          <div class="flex items-end gap-1.5">
            <label class="text-xs text-gray-500 whitespace-nowrap pb-1.5">시작</label>
            <input type="datetime-local" v-model="filters.dateFrom" @change="reload" class="input w-44 text-sm" />
          </div>
          <div class="flex items-end gap-1.5">
            <label class="text-xs text-gray-500 whitespace-nowrap pb-1.5">종료</label>
            <input type="datetime-local" v-model="filters.dateTo" @change="reload" class="input w-44 text-sm" />
          </div>

          <div class="flex items-end gap-2">
            <button @click="resetFilters" class="btn-secondary text-sm">초기화</button>
            <div class="flex items-center gap-1.5">
              <label class="text-xs text-gray-500 whitespace-nowrap">행/페이지</label>
              <input v-model.number="pageSize" @change="onPageSizeChange" type="number" min="5" max="200"
                class="input w-16 text-sm text-center" />
            </div>
          </div>
        </div>

        <div class="flex flex-wrap items-center gap-2 mt-3 pt-3 border-t">
          <ExcelDownloadButton :busy="exporting" label="엑셀" title="조회 조건대로 엑셀 내려받기" @click="exportExcel" />
          <button @click="purgeHandled" class="text-xs px-2 py-1 rounded-lg border border-gray-200 text-gray-600 hover:bg-gray-50">
            처리완료·무시 로그 비우기
          </button>
          <div class="flex items-center gap-1.5">
            <select v-model.number="purgeDays" class="input w-28 text-xs py-1">
              <option :value="7">7일 이전</option>
              <option :value="30">30일 이전</option>
              <option :value="90">90일 이전</option>
              <option :value="180">180일 이전</option>
              <option :value="0">전체</option>
            </select>
            <button @click="purgeOld" class="text-xs px-2 py-1 rounded-lg border border-gray-200 text-gray-600 hover:bg-gray-50">
              오래된 로그 정리
            </button>
          </div>
          <span class="text-xs text-gray-400 ml-auto">보관 기간이 지난 로그는 매일 새벽 자동 정리됩니다.</span>
        </div>
      </div>

      <!-- 목록 -->
      <div class="card">
        <div v-if="loading" class="text-center py-10 text-gray-500">불러오는 중...</div>
        <div v-else-if="logs.length === 0" class="text-center py-10 text-gray-400">기록된 오류가 없습니다.</div>
        <template v-else>
          <div class="overflow-x-auto"><table class="w-full text-sm">
            <thead>
              <tr class="border-b">
                <th class="text-left py-3 px-3 font-semibold text-gray-600 whitespace-nowrap">발생일시</th>
                <th class="text-left py-3 px-3 font-semibold text-gray-600">등급</th>
                <th class="text-left py-3 px-3 font-semibold text-gray-600">위치</th>
                <th class="text-left py-3 px-3 font-semibold text-gray-600">오류 종류</th>
                <th class="text-left py-3 px-3 font-semibold text-gray-600">메시지</th>
                <th class="text-left py-3 px-3 font-semibold text-gray-600">요청</th>
                <th class="text-left py-3 px-3 font-semibold text-gray-600">사용자</th>
                <th class="text-left py-3 px-3 font-semibold text-gray-600">처리 상태</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="log in logs" :key="log.id" @click="openDetail(log.id)"
                class="border-b hover:bg-gray-50 cursor-pointer">
                <td class="py-2.5 px-3 text-gray-500 whitespace-nowrap">{{ formatDate(log.occurredAt) }}</td>
                <td class="py-2.5 px-3">
                  <span :class="levelClass(log.level)" class="text-xs font-medium px-2 py-0.5 rounded">
                    {{ LEVEL_LABEL[log.level] || log.level }}
                  </span>
                </td>
                <td class="py-2.5 px-3 text-gray-600 whitespace-nowrap">{{ SOURCE_LABEL[log.source] || log.source }}</td>
                <td class="py-2.5 px-3 font-medium text-gray-800 max-w-[14rem] truncate" :title="log.exceptionType">
                  {{ shortType(log.exceptionType) }}
                </td>
                <td class="py-2.5 px-3 text-gray-500 max-w-sm truncate" :title="log.message">{{ log.message || '-' }}</td>
                <td class="py-2.5 px-3 text-gray-500 max-w-[16rem] truncate" :title="log.requestUri">
                  <span v-if="log.httpMethod" class="text-[11px] font-mono text-gray-400 mr-1">{{ log.httpMethod }}</span>
                  {{ log.requestUri || '-' }}
                </td>
                <td class="py-2.5 px-3 text-gray-600 whitespace-nowrap">{{ pi.mask('name', log.userName) }}</td>
                <td class="py-2.5 px-3">
                  <span :class="statusClass(log.status)" class="text-xs font-medium px-2 py-0.5 rounded">
                    {{ STATUS_LABEL[log.status] || log.status }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table></div>
        </template>
      </div>

      <!-- 페이지네이션 -->
      <div v-if="totalPages > 1" class="flex justify-center items-center gap-1 mt-4">
        <button @click="goPage(0)" :disabled="page === 0"
          class="px-2 py-1.5 rounded border text-xs text-gray-500 border-gray-300 hover:bg-gray-50 disabled:opacity-30 disabled:cursor-not-allowed">«</button>
        <button @click="goPage(page - 1)" :disabled="page === 0"
          class="px-2 py-1.5 rounded border text-xs text-gray-500 border-gray-300 hover:bg-gray-50 disabled:opacity-30 disabled:cursor-not-allowed">‹</button>
        <template v-for="p in visiblePages" :key="p">
          <span v-if="p === '...'" class="px-2 py-1.5 text-xs text-gray-400">…</span>
          <button v-else @click="goPage(p - 1)"
            :class="[
              'px-3 py-1.5 rounded border text-xs transition-colors',
              page === p - 1 ? 'bg-primary-600 text-white border-primary-600 font-semibold'
                             : 'border-gray-300 text-gray-600 hover:bg-gray-50'
            ]">{{ p }}</button>
        </template>
        <button @click="goPage(page + 1)" :disabled="page >= totalPages - 1"
          class="px-2 py-1.5 rounded border text-xs text-gray-500 border-gray-300 hover:bg-gray-50 disabled:opacity-30 disabled:cursor-not-allowed">›</button>
        <button @click="goPage(totalPages - 1)" :disabled="page >= totalPages - 1"
          class="px-2 py-1.5 rounded border text-xs text-gray-500 border-gray-300 hover:bg-gray-50 disabled:opacity-30 disabled:cursor-not-allowed">»</button>
      </div>
    </div><!-- /page-body -->

    <ErrorLogDetailModal :open="detailOpen" :log-id="detailId"
      @close="detailOpen = false" @changed="onChanged" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { estimateRows } from '@/composables/useFitPageSize'
import { errorLogApi } from '@/api'
import PiMaskToggle from '@/components/privacy/PiMaskToggle.vue'
import ExcelDownloadButton from '@/components/ExcelDownloadButton.vue'
import { usePiMaskingStore } from '@/stores/piMasking'
import ErrorLogDetailModal from './ErrorLogDetailModal.vue'
import { STATUS_LABEL, SOURCE_LABEL, LEVEL_LABEL, levelClass, statusClass, formatDate } from './errorLogLabels'

const pi = usePiMaskingStore()

const logs = ref([])
const stats = ref({})
const loading = ref(true)
const exporting = ref(false)
const page = ref(0)
const pageSize = ref(estimateRows({ top: 560, max: 200 }))
const totalPages = ref(0)
const purgeDays = ref(90)
const detailOpen = ref(false)
const detailId = ref(null)

const filters = ref({ level: '', source: '', status: '', keyword: '', dateFrom: '', dateTo: '' })

let searchTimer = null

const cards = computed(() => [
  { key: 'new', label: '미확인', value: stats.value.newCount ?? 0, status: 'NEW',
    color: 'text-red-600', hint: '아직 확인하지 않은 오류' },
  { key: 'progress', label: '확인중', value: stats.value.inProgress ?? 0, status: 'IN_PROGRESS',
    color: 'text-blue-600', hint: '원인 확인·조치 중' },
  { key: 'day', label: '최근 24시간', value: stats.value.last24h ?? 0, status: null,
    color: 'text-gray-800', hint: `오류 ${stats.value.errorLast24h ?? 0}건 포함` },
  { key: 'total', label: '전체 보관', value: stats.value.total ?? 0, status: null,
    color: 'text-gray-800', hint: `최근 7일 ${stats.value.last7d ?? 0}건` },
])

function shortType(type) {
  if (!type) return '-'
  const parts = type.split('.')
  return parts[parts.length - 1] || type
}

function applyStatusCard(card) {
  if (!card.status) return
  filters.value.status = filters.value.status === card.status ? '' : card.status
  reload()
}

function search() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(reload, 400)
}

function reload() {
  page.value = 0
  load()
}

function resetFilters() {
  filters.value = { level: '', source: '', status: '', keyword: '', dateFrom: '', dateTo: '' }
  reload()
}

function onPageSizeChange() {
  if (pageSize.value < 5) pageSize.value = 5
  if (pageSize.value > 200) pageSize.value = 200
  reload()
}

function goPage(p) {
  const target = Math.max(0, Math.min(p, totalPages.value - 1))
  if (target !== page.value) {
    page.value = target
    load()
  }
}

const visiblePages = computed(() => {
  const total = totalPages.value
  const cur = page.value + 1
  if (total <= 9) return Array.from({ length: total }, (_, i) => i + 1)

  const pages = []
  const start = Math.max(2, cur - 2)
  const end = Math.min(total - 1, cur + 2)
  pages.push(1)
  if (start > 2) pages.push('...')
  for (let i = start; i <= end; i++) pages.push(i)
  if (end < total - 1) pages.push('...')
  pages.push(total)
  return pages
})

function queryParams() {
  const f = filters.value
  return {
    ...(f.level && { level: f.level }),
    ...(f.source && { source: f.source }),
    ...(f.status && { status: f.status }),
    ...(f.keyword && { keyword: f.keyword }),
    ...(f.dateFrom && { dateFrom: f.dateFrom }),
    ...(f.dateTo && { dateTo: f.dateTo }),
  }
}

async function load() {
  loading.value = true
  try {
    const res = await errorLogApi.list({ page: page.value, size: pageSize.value, ...queryParams() })
    logs.value = res.data.content
    totalPages.value = res.data?.page?.totalPages ?? res.data?.totalPages ?? 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    stats.value = (await errorLogApi.stats()).data
  } catch (e) {
    console.error(e)
  }
}

function openDetail(id) {
  detailId.value = id
  detailOpen.value = true
}

function onChanged() {
  load()
  loadStats()
}

async function exportExcel() {
  exporting.value = true
  try {
    await errorLogApi.export(queryParams())
  } catch (e) {
    alert(e || '내려받지 못했습니다.')
  } finally {
    exporting.value = false
  }
}

async function purgeOld() {
  const label = purgeDays.value === 0 ? '전체 오류 로그를' : `${purgeDays.value}일 이전 오류 로그를`
  if (!confirm(`${label} 삭제할까요? 삭제한 로그는 복구할 수 없습니다.`)) return
  try {
    const res = await errorLogApi.purge(purgeDays.value)
    alert(`${res.data ?? 0}건을 삭제했습니다.`)
    onChanged()
  } catch (e) {
    alert(e || '삭제하지 못했습니다.')
  }
}

async function purgeHandled() {
  if (!confirm('처리완료·무시 상태의 오류 로그를 모두 삭제할까요?')) return
  try {
    const res = await errorLogApi.purgeHandled()
    alert(`${res.data ?? 0}건을 삭제했습니다.`)
    onChanged()
  } catch (e) {
    alert(e || '삭제하지 못했습니다.')
  }
}

onMounted(() => {
  load()
  loadStats()
})
</script>
