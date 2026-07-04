<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">ISMS-P 통제항목 매핑</h1>
        <p class="text-sm text-gray-400 mt-0.5">통제항목과 보안 정책을 연결·관리합니다</p>
      </div>
      <div class="flex items-center gap-3">
        <select v-model="filterStatus" class="input text-sm w-36">
          <option value="">준수상태 전체</option>
          <option value="COMPLIANT">준수</option>
          <option value="PARTIAL">부분준수</option>
          <option value="NON_COMPLIANT">미준수</option>
          <option value="NA">N/A</option>
          <option value="NONE">증적없음</option>
        </select>
        <select v-model="filterPolicy" class="input text-sm w-36">
          <option value="">매핑여부 전체</option>
          <option value="mapped">매핑됨</option>
          <option value="unmapped">미매핑</option>
        </select>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center py-20 text-gray-400">
      <svg class="animate-spin w-6 h-6 mr-2" fill="none" viewBox="0 0 24 24">
        <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
        <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
      </svg>
      통제항목 불러오는 중...
    </div>

    <div v-else class="page-body">
      <div class="flex gap-4">
        <!-- Left: domain nav -->
        <div class="w-60 flex-shrink-0">
          <div class="card p-0 overflow-hidden sticky top-4">
            <div class="px-4 py-3 bg-gray-50 border-b flex items-center justify-between">
              <p class="text-xs font-semibold text-gray-600 uppercase tracking-wide">도메인</p>
              <span class="text-xs text-gray-400">{{ allItems.length }}개 항목</span>
            </div>
            <div class="overflow-y-auto max-h-[calc(100vh-220px)] divide-y divide-gray-50">
              <template v-for="section in sections" :key="section.num">
                <div class="px-3 py-1.5 bg-gray-50 border-b">
                  <p class="text-[10px] font-bold text-gray-400 uppercase tracking-widest">
                    Section {{ section.num }} · {{ section.name }}
                  </p>
                </div>
                <button
                  v-for="domain in section.domains" :key="domain.code"
                  class="w-full text-left px-3 py-2.5 transition-colors hover:bg-gray-50"
                  :class="selectedDomain === domain.code ? 'bg-blue-50 border-l-4 border-blue-600' : 'border-l-4 border-transparent'"
                  @click="selectedDomain = domain.code; expandedRows.clear()"
                >
                  <p class="text-[10px] font-bold text-gray-400 mb-0.5">{{ domain.code }}</p>
                  <p class="text-xs font-medium leading-tight"
                    :class="selectedDomain === domain.code ? 'text-blue-700' : 'text-gray-700'">
                    {{ domain.name }}
                  </p>
                  <div class="flex items-center gap-1 mt-1">
                    <span class="text-[9px] px-1 py-0.5 rounded bg-green-100 text-green-700 font-semibold">
                      준수 {{ domainCount(domain.code, 'COMPLIANT') }}
                    </span>
                    <span v-if="domainCount(domain.code, 'NON_COMPLIANT') > 0"
                      class="text-[9px] px-1 py-0.5 rounded bg-red-100 text-red-600 font-semibold">
                      미준수 {{ domainCount(domain.code, 'NON_COMPLIANT') }}
                    </span>
                    <span class="text-[9px] px-1 py-0.5 rounded bg-blue-100 text-blue-600 font-semibold">
                      매핑 {{ domainMappedCount(domain.code) }}
                    </span>
                  </div>
                </button>
              </template>
            </div>
          </div>
        </div>

        <!-- Main: items table -->
        <div class="flex-1 min-w-0">
          <div class="card p-0 overflow-hidden">
            <div class="flex items-center justify-between px-5 py-3 bg-gray-50 border-b">
              <div class="flex items-center gap-2">
                <span class="text-sm font-semibold text-gray-700">{{ currentDomainName }}</span>
                <span class="text-xs text-gray-400">{{ filteredItems.length }}개 항목</span>
              </div>
              <div class="flex items-center gap-2">
                <span class="text-xs text-gray-400">정책 매핑: {{ domainMappedCount(selectedDomain) }} /
                  {{ itemsForDomain(selectedDomain).length }}개</span>
              </div>
            </div>

            <table class="w-full text-sm">
              <thead class="border-b border-gray-100">
                <tr>
                  <th class="text-left px-4 py-3 font-semibold text-gray-500 text-xs w-20">코드</th>
                  <th class="text-left px-4 py-3 font-semibold text-gray-500 text-xs">통제항목명</th>
                  <th class="text-left px-4 py-3 font-semibold text-gray-500 text-xs w-52">매핑 정책</th>
                  <th class="text-center px-4 py-3 font-semibold text-gray-500 text-xs w-24">준수상태</th>
                  <th class="text-center px-4 py-3 font-semibold text-gray-500 text-xs w-16">증적</th>
                  <th class="w-8"></th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-50">
                <template v-for="item in filteredItems" :key="item.id">
                  <tr class="hover:bg-gray-50 transition-colors cursor-pointer"
                    :class="expandedRows.has(item.id) ? 'bg-blue-50' : ''"
                    @click="toggleExpand(item.id)">
                    <td class="px-4 py-3">
                      <span class="font-mono text-xs font-bold text-blue-700">{{ item.itemCode }}</span>
                    </td>
                    <td class="px-4 py-3 font-medium text-gray-900 text-xs leading-snug">
                      {{ item.itemName }}
                    </td>
                    <td class="px-4 py-3">
                      <div v-if="item.mappedPolicies && item.mappedPolicies.length" class="flex flex-wrap gap-1">
                        <span v-for="p in item.mappedPolicies.slice(0, 2)" :key="p.id"
                          class="text-[10px] px-1.5 py-0.5 bg-indigo-100 text-indigo-700 rounded font-medium truncate max-w-[120px]"
                          :title="p.title">{{ p.title }}</span>
                        <span v-if="item.mappedPolicies.length > 2"
                          class="text-[10px] px-1.5 py-0.5 bg-gray-100 text-gray-500 rounded">
                          +{{ item.mappedPolicies.length - 2 }}
                        </span>
                      </div>
                      <span v-else class="text-xs text-gray-300">미매핑</span>
                    </td>
                    <td class="px-4 py-3 text-center">
                      <span class="text-[10px] px-2 py-0.5 rounded-full font-semibold"
                        :class="complianceBadge(item.latestStatus)">
                        {{ complianceLabel(item.latestStatus) }}
                      </span>
                    </td>
                    <td class="px-4 py-3 text-center text-xs text-gray-500">
                      {{ item.evidenceCount || 0 }}
                    </td>
                    <td class="px-4 py-3 text-center">
                      <svg class="w-4 h-4 text-gray-400 mx-auto transition-transform"
                        :class="expandedRows.has(item.id) ? 'rotate-180' : ''"
                        fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                      </svg>
                    </td>
                  </tr>

                  <!-- Expanded row: policy mapping management -->
                  <tr v-if="expandedRows.has(item.id)">
                    <td colspan="6" class="bg-blue-50 px-5 py-4 border-b border-blue-100">
                      <div class="flex gap-6">
                        <!-- Left: current mappings -->
                        <div class="flex-1 min-w-0">
                          <p class="text-xs font-semibold text-gray-600 mb-2">매핑된 정책</p>
                          <div v-if="item.mappedPolicies && item.mappedPolicies.length" class="space-y-1.5">
                            <div v-for="p in item.mappedPolicies" :key="p.id"
                              class="flex items-center justify-between gap-2 px-3 py-2 bg-white rounded-lg border border-blue-100 group">
                              <div class="flex items-center gap-2 min-w-0">
                                <span class="text-[10px] px-1 py-0.5 rounded font-semibold flex-shrink-0"
                                  :class="policyStatusBadge(p.status)">{{ policyStatusLabel(p.status) }}</span>
                                <span class="text-xs text-gray-800 truncate" :title="p.title">{{ p.title }}</span>
                                <span class="text-[10px] text-gray-400 flex-shrink-0">[{{ categoryLabel(p.category) }}]</span>
                              </div>
                              <button
                                v-if="canEdit"
                                class="flex-shrink-0 text-gray-300 hover:text-red-500 transition-colors opacity-0 group-hover:opacity-100"
                                :disabled="unmappingId === `${item.id}_${p.id}`"
                                @click.stop="removeMapping(item, p)"
                                title="매핑 해제">
                                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                                </svg>
                              </button>
                            </div>
                          </div>
                          <p v-else class="text-xs text-gray-400 py-2">매핑된 정책이 없습니다</p>
                        </div>

                        <!-- Right: add policy -->
                        <div v-if="canEdit" class="w-72 flex-shrink-0">
                          <p class="text-xs font-semibold text-gray-600 mb-2">정책 추가</p>
                          <div class="relative">
                            <input
                              v-model="searchQuery[item.id]"
                              class="input text-xs w-full pr-8"
                              placeholder="정책명 검색..."
                              @focus="openPicker(item.id)"
                              @click.stop
                            />
                            <svg class="absolute right-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-gray-400 pointer-events-none"
                              fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                d="M21 21l-4.35-4.35M17 11A6 6 0 1 1 5 11a6 6 0 0 1 12 0z"/>
                            </svg>
                          </div>
                          <div v-if="activePicker === item.id && availablePolicies(item).length > 0"
                            class="mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-44 overflow-y-auto"
                            @click.stop>
                            <button
                              v-for="p in availablePolicies(item)" :key="p.id"
                              class="w-full text-left px-3 py-2 hover:bg-blue-50 transition-colors border-b border-gray-50 last:border-0"
                              :disabled="mappingId === `${item.id}_${p.id}`"
                              @click.stop="addMapping(item, p)">
                              <div class="flex items-center gap-2">
                                <span class="text-[10px] px-1 py-0.5 rounded font-semibold flex-shrink-0"
                                  :class="policyStatusBadge(p.status)">{{ policyStatusLabel(p.status) }}</span>
                                <span class="text-xs text-gray-800 truncate">{{ p.title }}</span>
                              </div>
                              <p class="text-[10px] text-gray-400 mt-0.5">{{ categoryLabel(p.category) }}</p>
                            </button>
                          </div>
                          <p v-else-if="activePicker === item.id && availablePolicies(item).length === 0 && searchQuery[item.id]"
                            class="mt-1 text-xs text-gray-400 text-center py-2">
                            검색 결과가 없습니다
                          </p>
                        </div>
                      </div>
                    </td>
                  </tr>
                </template>

                <tr v-if="filteredItems.length === 0">
                  <td colspan="6" class="px-4 py-12 text-center text-gray-400 text-sm">
                    해당 조건의 항목이 없습니다
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ismsApi, policyApi } from '@/api/index.js'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const canEdit = computed(() => ['ADMIN', 'MANAGER'].includes(authStore.user?.role))

const loading = ref(true)
const allItems = ref([])
const allPolicies = ref([])
const selectedDomain = ref('')
const filterStatus = ref('')
const filterPolicy = ref('')
const expandedRows = ref(new Set())
const searchQuery = ref({})
const activePicker = ref(null)
const mappingId = ref(null)
const unmappingId = ref(null)
const year = new Date().getFullYear()

// ── Data loading ──────────────────────────────────────────────
onMounted(async () => {
  try {
    const [itemsRes, policiesRes] = await Promise.all([
      ismsApi.listItems({ year }),
      policyApi.list({ size: 500 })
    ])
    allItems.value = itemsRes?.data || []
    allPolicies.value = policiesRes?.data?.content || []
    if (sections.value.length > 0 && sections.value[0].domains.length > 0) {
      selectedDomain.value = sections.value[0].domains[0].code
    }
  } catch {
    // silent fail
  } finally {
    loading.value = false
  }
})

// close picker on outside click
function onDocClick() { activePicker.value = null }
onMounted(() => document.addEventListener('click', onDocClick))
onBeforeUnmount(() => document.removeEventListener('click', onDocClick))

// ── Domain / Section grouping ─────────────────────────────────
const sections = computed(() => {
  const sectionMap = new Map()
  for (const item of allItems.value) {
    if (!sectionMap.has(item.sectionNum)) {
      sectionMap.set(item.sectionNum, { num: item.sectionNum, name: item.sectionName, domainMap: new Map() })
    }
    const sec = sectionMap.get(item.sectionNum)
    if (!sec.domainMap.has(item.domainCode)) {
      sec.domainMap.set(item.domainCode, { code: item.domainCode, name: item.domainName })
    }
  }
  return Array.from(sectionMap.values()).map(s => ({
    num: s.num,
    name: s.name,
    domains: Array.from(s.domainMap.values())
  }))
})

const currentDomainName = computed(() => {
  for (const sec of sections.value) {
    const d = sec.domains.find(d => d.code === selectedDomain.value)
    if (d) return d.name
  }
  return ''
})

function itemsForDomain(code) {
  return allItems.value.filter(i => i.domainCode === code)
}

const filteredItems = computed(() => {
  let items = itemsForDomain(selectedDomain.value)
  if (filterStatus.value) {
    if (filterStatus.value === 'NONE') {
      items = items.filter(i => !i.latestStatus)
    } else {
      items = items.filter(i => i.latestStatus === filterStatus.value)
    }
  }
  if (filterPolicy.value === 'mapped') {
    items = items.filter(i => i.mappedPolicies && i.mappedPolicies.length > 0)
  } else if (filterPolicy.value === 'unmapped') {
    items = items.filter(i => !i.mappedPolicies || i.mappedPolicies.length === 0)
  }
  return items
})

// ── Domain stats ──────────────────────────────────────────────
function domainCount(code, status) {
  return itemsForDomain(code).filter(i => i.latestStatus === status).length
}
function domainMappedCount(code) {
  return itemsForDomain(code).filter(i => i.mappedPolicies && i.mappedPolicies.length > 0).length
}

// ── Expand ────────────────────────────────────────────────────
function toggleExpand(id) {
  const next = new Set(expandedRows.value)
  if (next.has(id)) {
    next.delete(id)
    if (activePicker.value === id) activePicker.value = null
  } else {
    next.add(id)
  }
  expandedRows.value = next
}

// ── Policy picker ─────────────────────────────────────────────
function openPicker(itemId) {
  activePicker.value = itemId
}

function availablePolicies(item) {
  const mappedIds = new Set((item.mappedPolicies || []).map(p => p.id))
  const q = (searchQuery.value[item.id] || '').toLowerCase()
  return allPolicies.value.filter(p =>
    !mappedIds.has(p.id) &&
    (!q || p.title.toLowerCase().includes(q))
  ).slice(0, 10)
}

// ── Mapping actions ───────────────────────────────────────────
async function addMapping(item, policy) {
  const key = `${item.id}_${policy.id}`
  if (mappingId.value === key) return
  mappingId.value = key
  try {
    await ismsApi.mapPolicy(item.id, policy.id)
    if (!item.mappedPolicies) item.mappedPolicies = []
    item.mappedPolicies.push({
      id: policy.id,
      title: policy.title,
      status: policy.status,
      category: policy.category
    })
    searchQuery.value[item.id] = ''
    activePicker.value = null
  } catch {
    // silent fail
  } finally {
    mappingId.value = null
  }
}

async function removeMapping(item, policy) {
  const key = `${item.id}_${policy.id}`
  if (unmappingId.value === key) return
  unmappingId.value = key
  try {
    await ismsApi.unmapPolicy(item.id, policy.id)
    item.mappedPolicies = item.mappedPolicies.filter(p => p.id !== policy.id)
  } catch {
    // silent fail
  } finally {
    unmappingId.value = null
  }
}

// ── Badge helpers ─────────────────────────────────────────────
function complianceBadge(status) {
  return {
    COMPLIANT:     'bg-green-100 text-green-700',
    PARTIAL:       'bg-yellow-100 text-yellow-700',
    NON_COMPLIANT: 'bg-red-100 text-red-700',
    NA:            'bg-gray-100 text-gray-500'
  }[status] || 'bg-gray-50 text-gray-400'
}
function complianceLabel(status) {
  return { COMPLIANT: '준수', PARTIAL: '부분준수', NON_COMPLIANT: '미준수', NA: 'N/A' }[status] || '미제출'
}
function policyStatusBadge(status) {
  return {
    PUBLISHED: 'bg-green-100 text-green-700',
    DRAFT:     'bg-yellow-100 text-yellow-700',
    REVIEW:    'bg-blue-100 text-blue-700',
    ARCHIVED:  'bg-gray-100 text-gray-500'
  }[status] || 'bg-gray-100 text-gray-500'
}
function policyStatusLabel(status) {
  return { PUBLISHED: '발효', DRAFT: '초안', REVIEW: '검토', ARCHIVED: '보관' }[status] || status
}
function categoryLabel(cat) {
  return {
    GENERAL: '일반', ACCESS_CONTROL: '접근통제', DATA_PROTECTION: '데이터보호',
    INCIDENT_RESPONSE: '사고대응', NETWORK: '네트워크', PHYSICAL: '물리보안',
    VENDOR: '공급망', OTHER: '기타'
  }[cat] || cat
}
</script>
