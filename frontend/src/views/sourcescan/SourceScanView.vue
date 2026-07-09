<template>
  <div class="p-8">
    <div class="flex items-center justify-between mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">소스 취약점 점검 (SAST)</h1>
        <p class="text-sm text-gray-500 mt-1">GitHub 저장소의 의존성(Dependabot)·코드(Code scanning)·시크릿(Secret scanning) 알림과 <span class="font-medium text-gray-700">내장 OWASP 정적분석(소스코드 직접 점검)</span>을 함께 수행합니다.</p>
      </div>
    </div>

    <!-- 점검 실행 -->
    <div class="card mb-6">
      <h2 class="text-base font-semibold text-gray-800 mb-3">점검 실행</h2>
      <div class="flex flex-wrap gap-3 items-end">
        <div class="flex-1 min-w-64">
          <label class="block text-sm font-medium text-gray-700 mb-1">저장소 (owner/repo)</label>
          <div class="flex gap-2">
            <select v-if="repos.length" v-model="selectedRepo" class="input flex-1 font-mono text-sm">
              <option value="">저장소 선택</option>
              <option v-for="r in repos" :key="r.fullName" :value="r.fullName">
                {{ r.fullName }}{{ r.privateRepo ? ' (private)' : '' }}
              </option>
            </select>
            <input v-else v-model="selectedRepo" class="input flex-1 font-mono text-sm" placeholder="monosun/ksecportal" />
            <button v-if="isManager" @click="loadRepos" :disabled="reposLoading" class="btn-secondary text-sm whitespace-nowrap">
              {{ reposLoading ? '불러오는 중...' : '저장소 목록 불러오기' }}
            </button>
          </div>
        </div>
        <button v-if="isManager" @click="runScan" :disabled="!selectedRepo || scanning"
          class="btn-primary flex items-center gap-2">
          <svg v-if="scanning" class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
          </svg>
          <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
          </svg>
          {{ scanning ? '점검 중...' : '점검 실행' }}
        </button>
      </div>
      <div v-if="runError" class="mt-3 text-sm text-red-600 bg-red-50 p-3 rounded-lg whitespace-pre-wrap">{{ runError }}</div>
      <p class="text-xs text-gray-400 mt-3">
        GitHub 연동 설정(토큰)은 <RouterLink to="/admin/settings-management" class="text-blue-600 hover:underline">설정관리 → API 연동</RouterLink>에서 등록합니다.
        의존성·코드·시크릿 카테고리는 저장소에서 해당 GitHub 기능이 활성화되어 있어야 조회됩니다. <span class="font-medium text-gray-600">OWASP 정적분석(SAST)</span>은 저장소 소스를 직접 내려받아 점검하므로 별도 활성화가 필요 없습니다.
      </p>
    </div>

    <!-- 점검 이력 -->
    <div class="card p-0 overflow-hidden mb-6">
      <div class="px-5 py-3 border-b flex items-center justify-between">
        <h2 class="text-base font-semibold text-gray-800">점검 이력</h2>
        <button @click="loadScans" class="text-xs text-blue-600 hover:underline">새로고침</button>
      </div>
      <div v-if="scansLoading" class="p-8 text-center text-gray-400">{{ $t('common.loading') }}</div>
      <div v-else-if="!scans.length" class="p-8 text-center text-gray-400">점검 이력이 없습니다. 저장소를 선택하고 점검을 실행하세요.</div>
      <div v-else class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead class="bg-gray-50 border-b">
            <tr>
              <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">저장소</th>
              <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">점검 일시</th>
              <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">상태</th>
              <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">심각도</th>
              <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">카테고리</th>
              <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">{{ $t('common.actions') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-50">
            <tr v-for="s in scans" :key="s.id" class="hover:bg-gray-50 cursor-pointer"
              :class="detail?.scan?.id === s.id ? 'bg-blue-50/50' : ''" @click="openDetail(s.id)">
              <td class="px-5 py-3 font-mono text-gray-800">{{ s.repository }}</td>
              <td class="px-5 py-3 text-gray-500 text-xs">{{ formatDateTime(s.createdAt) }}</td>
              <td class="px-5 py-3"><span :class="statusBadge(s.status)">{{ statusLabel(s.status) }}</span></td>
              <td class="px-5 py-3">
                <div class="flex gap-1.5 flex-wrap">
                  <span v-if="s.criticalCount" class="badge-red">심각 {{ s.criticalCount }}</span>
                  <span v-if="s.highCount" class="badge-orange">높음 {{ s.highCount }}</span>
                  <span v-if="s.mediumCount" class="badge-yellow">중간 {{ s.mediumCount }}</span>
                  <span v-if="s.lowCount" class="badge-gray">낮음 {{ s.lowCount }}</span>
                  <span v-if="!s.totalCount" class="badge-success">발견 없음</span>
                </div>
              </td>
              <td class="px-5 py-3 text-xs text-gray-500">
                의존성 {{ s.dependencyCount }} · 코드 {{ s.codeCount }} · 시크릿 {{ s.secretCount }} · SAST {{ s.sastCount || 0 }}
              </td>
              <td class="px-5 py-3" @click.stop>
                <button class="text-blue-600 hover:underline text-xs mr-3" @click="openDetail(s.id)">상세</button>
                <button v-if="isManager" class="text-red-500 hover:underline text-xs" @click="confirmDelete(s)">{{ $t('common.delete') }}</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="totalPages > 1" class="flex justify-center gap-2 py-3 border-t">
        <button v-for="p in totalPages" :key="p" @click="page = p - 1; loadScans()"
          :class="['px-3 py-1 rounded border text-sm', page === p - 1 ? 'bg-blue-600 text-white border-blue-600' : 'border-gray-300 hover:bg-gray-50']">
          {{ p }}
        </button>
      </div>
    </div>

    <!-- 점검 상세 -->
    <div v-if="detail" class="card p-0 overflow-hidden">
      <div class="px-5 py-4 border-b">
        <div class="flex items-center justify-between flex-wrap gap-2">
          <div>
            <h2 class="text-base font-semibold text-gray-800">
              점검 상세 — <span class="font-mono">{{ detail.scan.repository }}</span>
              <span class="text-xs text-gray-400 font-normal ml-2">{{ formatDateTime(detail.scan.createdAt) }}</span>
            </h2>
            <p v-if="detail.scan.message" class="text-xs text-amber-600 mt-1 whitespace-pre-wrap">{{ detail.scan.message }}</p>
          </div>
          <div class="flex gap-2">
            <button v-for="c in categoryFilters" :key="c.value" @click="categoryFilter = c.value"
              :class="['px-3 py-1.5 rounded-lg text-xs font-medium border transition-colors',
                categoryFilter === c.value ? 'bg-blue-600 text-white border-blue-600' : 'border-gray-300 text-gray-600 hover:bg-gray-50']">
              {{ c.label }} ({{ c.count() }})
            </button>
          </div>
        </div>
      </div>

      <div v-if="!filteredFindings.length" class="p-8 text-center text-gray-400 text-sm">
        해당 카테고리에서 발견된 항목이 없습니다.
      </div>
      <div v-else class="overflow-x-auto">
        <table class="w-full text-sm">
          <thead class="bg-gray-50 border-b">
            <tr>
              <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">심각도</th>
              <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">카테고리</th>
              <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">내용</th>
              <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">대상 / 위치</th>
              <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">CVE</th>
              <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">링크</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-50">
            <tr v-for="f in filteredFindings" :key="f.id" class="hover:bg-gray-50">
              <td class="px-5 py-3"><span :class="severityBadge(f.severity)">{{ severityLabel(f.severity) }}</span></td>
              <td class="px-5 py-3"><span class="badge-gray">{{ categoryLabel(f.category) }}</span></td>
              <td class="px-5 py-3 text-gray-800 max-w-md">{{ f.title }}</td>
              <td class="px-5 py-3 font-mono text-xs text-gray-500">
                <p v-if="f.identifier">{{ f.identifier }}</p>
                <p v-if="f.location" class="text-gray-400">{{ f.location }}</p>
                <span v-if="!f.identifier && !f.location">-</span>
              </td>
              <td class="px-5 py-3 font-mono text-xs">{{ f.cveId || '-' }}</td>
              <td class="px-5 py-3">
                <a v-if="f.htmlUrl" :href="f.htmlUrl" target="_blank" rel="noopener"
                  class="text-blue-600 hover:underline text-xs">GitHub ↗</a>
                <span v-else class="text-gray-400 text-xs">-</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { sourceScanApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const isManager = auth.isManager

// ── 저장소 / 점검 실행 ────────────────────────────────────────────
const repos = ref([])
const reposLoading = ref(false)
const selectedRepo = ref('')
const scanning = ref(false)
const runError = ref(null)

async function loadRepos() {
  reposLoading.value = true
  runError.value = null
  try {
    const res = await sourceScanApi.repos()
    repos.value = res.data || []
  } catch (e) {
    runError.value = e || '저장소 목록 조회에 실패했습니다. GitHub 연동 설정을 확인하세요.'
  } finally {
    reposLoading.value = false
  }
}

async function runScan() {
  if (!selectedRepo.value) return
  scanning.value = true
  runError.value = null
  try {
    const res = await sourceScanApi.run(selectedRepo.value)
    detail.value = res.data
    categoryFilter.value = ''
    await loadScans()
  } catch (e) {
    runError.value = e || '점검 실행에 실패했습니다.'
  } finally {
    scanning.value = false
  }
}

// ── 점검 이력 ────────────────────────────────────────────────────
const scans = ref([])
const scansLoading = ref(false)
const page = ref(0)
const totalPages = ref(0)

async function loadScans() {
  scansLoading.value = true
  try {
    const res = await sourceScanApi.scans({ page: page.value, size: 20 })
    scans.value = res.data?.content || []
    totalPages.value = res.data?.page?.totalPages ?? res.data?.totalPages ?? 0
  } finally {
    scansLoading.value = false
  }
}

async function confirmDelete(scan) {
  if (!confirm(`"${scan.repository}" ${formatDateTime(scan.createdAt)} 점검 이력을 삭제하시겠습니까?`)) return
  await sourceScanApi.delete(scan.id)
  if (detail.value?.scan?.id === scan.id) detail.value = null
  loadScans()
}

// ── 상세 ─────────────────────────────────────────────────────────
const detail = ref(null)
const categoryFilter = ref('')

async function openDetail(id) {
  const res = await sourceScanApi.scan(id)
  detail.value = res.data
  categoryFilter.value = ''
}

const categoryFilters = [
  { value: '', label: '전체', count: () => detail.value?.findings?.length || 0 },
  { value: 'DEPENDENCY', label: '의존성', count: () => detail.value?.scan?.dependencyCount || 0 },
  { value: 'CODE_SCANNING', label: '코드', count: () => detail.value?.scan?.codeCount || 0 },
  { value: 'SECRET', label: '시크릿', count: () => detail.value?.scan?.secretCount || 0 },
  { value: 'SAST', label: 'SAST', count: () => detail.value?.scan?.sastCount || 0 },
]

const filteredFindings = computed(() => {
  const findings = detail.value?.findings || []
  if (!categoryFilter.value) return findings
  return findings.filter(f => f.category === categoryFilter.value)
})

// ── 라벨 / 뱃지 ──────────────────────────────────────────────────
function severityLabel(s) {
  return { CRITICAL: '심각', HIGH: '높음', MEDIUM: '중간', LOW: '낮음', INFO: '정보' }[s] || s
}
function severityBadge(s) {
  return { CRITICAL: 'badge-red', HIGH: 'badge-orange', MEDIUM: 'badge-yellow', LOW: 'badge-gray', INFO: 'badge-gray' }[s] || 'badge-gray'
}
function categoryLabel(c) {
  return { DEPENDENCY: '의존성', CODE_SCANNING: '코드', SECRET: '시크릿', SAST: 'SAST' }[c] || c
}
function statusLabel(s) {
  return { SUCCESS: '완료', PARTIAL: '부분 완료', FAILED: '실패' }[s] || s
}
function statusBadge(s) {
  return { SUCCESS: 'badge-success', PARTIAL: 'badge-yellow', FAILED: 'badge-danger' }[s] || 'badge-gray'
}
function formatDateTime(dt) {
  return dt ? new Date(dt).toLocaleString('ko-KR', { dateStyle: 'short', timeStyle: 'short' }) : '-'
}

onMounted(loadScans)
</script>
