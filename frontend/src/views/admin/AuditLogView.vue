<template>
  <div>
    <div class="page-header">
      <h1 class="page-title">{{ $t('admin.auditLogs') }}</h1>
    </div>

    <div class="page-body">

    <div class="card mb-4">
      <div class="flex flex-wrap gap-3 items-end">

        <!-- 액션 필터 (콤보박스) -->
        <div class="flex flex-col gap-1">
          <label class="text-xs text-gray-500">{{ $t('admin.action') }}</label>
          <select v-model="actionSelect" @change="onActionSelectChange" class="input w-52 text-sm">
            <option value="">전체</option>
            <optgroup label="인증">
              <option v-for="a in ACTION_GROUPS.auth" :key="a" :value="a">{{ a }}</option>
            </optgroup>
            <optgroup label="사용자">
              <option v-for="a in ACTION_GROUPS.user" :key="a" :value="a">{{ a }}</option>
            </optgroup>
            <optgroup label="정책">
              <option v-for="a in ACTION_GROUPS.policy" :key="a" :value="a">{{ a }}</option>
            </optgroup>
            <optgroup label="취약점">
              <option v-for="a in ACTION_GROUPS.vuln" :key="a" :value="a">{{ a }}</option>
            </optgroup>
            <optgroup label="인시던트">
              <option v-for="a in ACTION_GROUPS.incident" :key="a" :value="a">{{ a }}</option>
            </optgroup>
            <optgroup label="자산">
              <option v-for="a in ACTION_GROUPS.asset" :key="a" :value="a">{{ a }}</option>
            </optgroup>
            <option value="__custom__">직접 입력...</option>
          </select>
          <input v-if="actionSelect === '__custom__'"
            v-model="filters.action"
            @input="search"
            placeholder="액션 직접 입력"
            class="input w-52 text-sm mt-0.5"
          />
        </div>

        <!-- 리소스 필터 (콤보박스) -->
        <div class="flex flex-col gap-1">
          <label class="text-xs text-gray-500">{{ $t('admin.resource') }}</label>
          <select v-model="resourceSelect" @change="onResourceSelectChange" class="input w-44 text-sm">
            <option value="">전체</option>
            <option v-for="r in RESOURCE_LIST" :key="r" :value="r">{{ r }}</option>
            <option value="__custom__">직접 입력...</option>
          </select>
          <input v-if="resourceSelect === '__custom__'"
            v-model="filters.resourceType"
            @input="search"
            placeholder="리소스 직접 입력"
            class="input w-44 text-sm mt-0.5"
          />
        </div>

        <!-- 날짜 필터 -->
        <div class="flex items-end gap-1.5">
          <label class="text-xs text-gray-500 whitespace-nowrap pb-1.5">{{ $t('admin.dateFrom') }}</label>
          <input type="datetime-local" v-model="filters.dateFrom" @change="search" class="input w-44 text-sm" />
        </div>
        <div class="flex items-end gap-1.5">
          <label class="text-xs text-gray-500 whitespace-nowrap pb-1.5">{{ $t('admin.dateTo') }}</label>
          <input type="datetime-local" v-model="filters.dateTo" @change="search" class="input w-44 text-sm" />
        </div>

        <!-- 초기화 + 페이지당 행 수 -->
        <div class="flex items-end gap-2">
          <button @click="resetFilters" class="btn-secondary text-sm">{{ $t('common.all') }}</button>
          <div class="flex items-center gap-1.5">
            <label class="text-xs text-gray-500 whitespace-nowrap">행/페이지</label>
            <input
              v-model.number="pageSize"
              @change="onPageSizeChange"
              type="number"
              min="5"
              max="200"
              class="input w-16 text-sm text-center"
            />
          </div>
        </div>
      </div>
    </div>

    <div class="card">
      <div v-if="loading" class="text-center py-10 text-gray-500">{{ $t('common.loading') }}</div>
      <div v-else-if="logs.length === 0" class="text-center py-10 text-gray-400">{{ $t('common.noData') }}</div>
      <template v-else>
        <table class="w-full text-sm">
          <thead>
            <tr class="border-b">
              <th class="text-left py-3 px-4 font-semibold text-gray-600">{{ $t('admin.timestamp') }}</th>
              <th class="text-left py-3 px-4 font-semibold text-gray-600">{{ $t('admin.user') }}</th>
              <th class="text-left py-3 px-4 font-semibold text-gray-600">{{ $t('admin.action') }}</th>
              <th class="text-left py-3 px-4 font-semibold text-gray-600">{{ $t('admin.resource') }}</th>
              <th class="text-left py-3 px-4 font-semibold text-gray-600">{{ $t('admin.detail') }}</th>
              <th class="text-left py-3 px-4 font-semibold text-gray-600">{{ $t('admin.ip') }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="log in logs" :key="log.id" class="border-b hover:bg-gray-50">
              <td class="py-3 px-4 text-gray-500 whitespace-nowrap">{{ formatDate(log.createdAt) }}</td>
              <td class="py-3 px-4 font-medium">{{ log.userName }}</td>
              <td class="py-3 px-4">
                <span :class="actionBadgeClass(log.action)"
                  class="text-xs font-medium px-2 py-0.5 rounded">{{ log.action }}</span>
              </td>
              <td class="py-3 px-4 text-gray-600">{{ log.resourceType }} #{{ log.resourceId }}</td>
              <td class="py-3 px-4 text-gray-500 max-w-xs truncate" :title="log.detail">{{ log.detail }}</td>
              <td class="py-3 px-4 text-gray-400">{{ log.ipAddress || '-' }}</td>
            </tr>
          </tbody>
        </table>

      </template>
    </div>

    <!-- 페이지네이션 -->
    <div v-if="totalPages > 1" class="flex justify-center items-center gap-1 mt-4">
      <!-- 처음/이전 -->
      <button @click="goPage(0)" :disabled="page === 0"
        class="px-2 py-1.5 rounded border text-xs text-gray-500 border-gray-300 hover:bg-gray-50 disabled:opacity-30 disabled:cursor-not-allowed">
        «
      </button>
      <button @click="goPage(page - 1)" :disabled="page === 0"
        class="px-2 py-1.5 rounded border text-xs text-gray-500 border-gray-300 hover:bg-gray-50 disabled:opacity-30 disabled:cursor-not-allowed">
        ‹
      </button>

      <!-- 페이지 번호 -->
      <template v-for="p in visiblePages" :key="p">
        <span v-if="p === '...'" class="px-2 py-1.5 text-xs text-gray-400">…</span>
        <button v-else @click="goPage(p - 1)"
          :class="[
            'px-3 py-1.5 rounded border text-xs transition-colors',
            page === p - 1
              ? 'bg-primary-600 text-white border-primary-600 font-semibold'
              : 'border-gray-300 text-gray-600 hover:bg-gray-50'
          ]">
          {{ p }}
        </button>
      </template>

      <!-- 다음/끝 -->
      <button @click="goPage(page + 1)" :disabled="page >= totalPages - 1"
        class="px-2 py-1.5 rounded border text-xs text-gray-500 border-gray-300 hover:bg-gray-50 disabled:opacity-30 disabled:cursor-not-allowed">
        ›
      </button>
      <button @click="goPage(totalPages - 1)" :disabled="page >= totalPages - 1"
        class="px-2 py-1.5 rounded border text-xs text-gray-500 border-gray-300 hover:bg-gray-50 disabled:opacity-30 disabled:cursor-not-allowed">
        »
      </button>
    </div>
    </div><!-- /page-body -->
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { adminApi } from '@/api'

// ─── 정의된 액션 목록 ────────────────────────────────────────────────────────
const ACTION_GROUPS = {
  auth: ['LOGIN_SUCCESS', 'LOGIN_FAILED', 'PASSWORD_CHANGED'],
  user: [
    'USER_CREATED', 'USER_UPDATED', 'USER_DEACTIVATED', 'USER_PROMOTED_ADMIN',
    'USER_DELETE_REQUESTED', 'USER_HARD_DELETE_REQUESTED', 'USER_HARD_DELETED',
    'USER_PROMOTE_ADMIN_REQUESTED', 'USER_BULK_UPLOAD', 'ADMIN_ACTION_REJECTED'
  ],
  policy: ['POLICY_CREATED', 'POLICY_UPDATED', 'POLICY_DELETED', 'POLICY_ACKNOWLEDGED', 'POLICY_BULK_UPLOAD'],
  vuln: ['VULN_CREATED', 'VULN_STATUS_CHANGED', 'VULN_DELETED', 'VULN_BULK_UPLOAD'],
  incident: ['INCIDENT_CREATED', 'INCIDENT_STATUS_CHANGED', 'INCIDENT_DELETED', 'INCIDENT_BULK_UPLOAD'],
  asset: ['ASSET_CREATED', 'ASSET_UPDATED', 'ASSET_DELETED', 'ASSET_BULK_UPLOAD'],
}

// ─── 정의된 리소스 타입 목록 ──────────────────────────────────────────────────
const RESOURCE_LIST = ['USER', 'POLICY', 'VULNERABILITY', 'INCIDENT', 'ASSET']

// ─── 상태 ─────────────────────────────────────────────────────────────────────
const logs = ref([])
const loading = ref(true)
const page = ref(0)
const pageSize = ref(20)
const totalPages = ref(0)
const filters = ref({ action: '', resourceType: '', dateFrom: '', dateTo: '' })

const actionSelect = ref('')
const resourceSelect = ref('')

let searchTimer = null

// ─── 콤보박스 핸들러 ─────────────────────────────────────────────────────────
function onActionSelectChange() {
  if (actionSelect.value !== '__custom__') {
    filters.value.action = actionSelect.value
    page.value = 0
    load()
  }
}

function onResourceSelectChange() {
  if (resourceSelect.value !== '__custom__') {
    filters.value.resourceType = resourceSelect.value
    page.value = 0
    load()
  }
}

// ─── 검색 ─────────────────────────────────────────────────────────────────────
function search() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { page.value = 0; load() }, 400)
}

function resetFilters() {
  filters.value = { action: '', resourceType: '', dateFrom: '', dateTo: '' }
  actionSelect.value = ''
  resourceSelect.value = ''
  page.value = 0
  load()
}

function onPageSizeChange() {
  if (pageSize.value < 5) pageSize.value = 5
  if (pageSize.value > 200) pageSize.value = 200
  page.value = 0
  load()
}

// ─── 페이지 이동 ──────────────────────────────────────────────────────────────
function goPage(p) {
  const target = Math.max(0, Math.min(p, totalPages.value - 1))
  if (target !== page.value) {
    page.value = target
    load()
  }
}

// ─── 스마트 페이지네이션 계산 ─────────────────────────────────────────────────
const visiblePages = computed(() => {
  const total = totalPages.value
  const cur = page.value + 1 // 1-based

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

// ─── 데이터 로드 ──────────────────────────────────────────────────────────────
async function load() {
  loading.value = true
  try {
    const params = {
      page: page.value,
      size: pageSize.value,
      ...(filters.value.action && { action: filters.value.action }),
      ...(filters.value.resourceType && { resourceType: filters.value.resourceType }),
      ...(filters.value.dateFrom && { dateFrom: filters.value.dateFrom }),
      ...(filters.value.dateTo && { dateTo: filters.value.dateTo })
    }
    const res = await adminApi.listAuditLogs(params)
    logs.value = res.data.content
    totalPages.value = res.data?.page?.totalPages ?? res.data?.totalPages ?? 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

// ─── 액션 배지 색상 ───────────────────────────────────────────────────────────
function actionBadgeClass(action) {
  if (!action) return 'bg-gray-100 text-gray-600'
  if (action.startsWith('LOGIN')) return action === 'LOGIN_SUCCESS' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
  if (action.includes('DELETED') || action.includes('DELETE')) return 'bg-red-100 text-red-700'
  if (action.includes('CREATED')) return 'bg-blue-100 text-blue-700'
  if (action.includes('UPDATED') || action.includes('CHANGED')) return 'bg-yellow-100 text-yellow-700'
  if (action.includes('BULK')) return 'bg-purple-100 text-purple-700'
  if (action.includes('PROMOTED') || action.includes('PROMOTE')) return 'bg-indigo-100 text-indigo-700'
  return 'bg-gray-100 text-gray-600'
}

function formatDate(dt) {
  if (!dt) return '-'
  return new Date(dt).toLocaleString('ko-KR', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
  })
}

onMounted(load)
</script>
