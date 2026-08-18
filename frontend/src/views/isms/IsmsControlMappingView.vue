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

            <div class="overflow-x-auto"><table class="w-full text-sm">
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
                        <button v-for="p in item.mappedPolicies.slice(0, 2)" :key="refKey(p)"
                          class="text-[10px] px-1.5 py-0.5 rounded font-medium truncate max-w-[120px] transition-colors"
                          :class="p.articleId
                            ? 'bg-amber-100 text-amber-800 hover:bg-amber-200'
                            : 'bg-indigo-100 text-indigo-700 hover:bg-indigo-200'"
                          :title="`${refTooltip(p)} — 클릭하면 내용 미리보기`"
                          @click.stop="openPreview(p)">{{ refChipLabel(p) }}</button>
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
                        <!-- Left: current mappings — 장(章) 으로 묶고 그 아래 조(條) 를 나열 -->
                        <div class="flex-1 min-w-0">
                          <p class="text-xs font-semibold text-gray-600 mb-2">매핑된 정책</p>
                          <div v-if="mappedGroups(item).length" class="space-y-1.5">
                            <div v-for="g in mappedGroups(item)" :key="g.policyId"
                              class="px-3 py-2 bg-white rounded-lg border border-blue-100">
                              <!-- 장 -->
                              <div class="flex items-center justify-between gap-2 group">
                                <button class="flex items-center gap-2 min-w-0 text-left group/preview"
                                  title="정책 내용 미리보기"
                                  @click.stop="openPreview(g.whole || g.articles[0])">
                                  <span class="text-[10px] px-1 py-0.5 rounded font-semibold flex-shrink-0"
                                    :class="policyStatusBadge(g.status)">{{ policyStatusLabel(g.status) }}</span>
                                  <span class="text-xs text-gray-800 truncate group-hover/preview:text-blue-700 group-hover/preview:underline">{{ g.title }}</span>
                                  <span v-if="g.whole"
                                    class="text-[10px] px-1 py-0.5 rounded bg-indigo-100 text-indigo-700 font-semibold flex-shrink-0">장 전체</span>
                                  <span class="text-[10px] text-gray-400 flex-shrink-0">[{{ categoryLabel(g.category) }}]</span>
                                  <svg class="w-3.5 h-3.5 text-gray-300 flex-shrink-0 group-hover/preview:text-blue-600"
                                    fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                      d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                      d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                                  </svg>
                                </button>
                                <button
                                  v-if="canEdit && g.whole"
                                  class="flex-shrink-0 text-gray-300 hover:text-red-500 transition-colors opacity-0 group-hover:opacity-100"
                                  :disabled="unmappingId === refKeyOf(item, g.whole)"
                                  @click.stop="removeMapping(item, g.whole)"
                                  title="장 전체 매핑 해제">
                                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                                  </svg>
                                </button>
                              </div>
                              <!-- 조 -->
                              <div v-if="g.articles.length" class="flex flex-wrap gap-1 mt-1.5">
                                <span v-for="a in g.articles" :key="a.articleId"
                                  class="inline-flex items-center gap-1 pl-1.5 pr-1 py-0.5 rounded bg-amber-50 border border-amber-200">
                                  <button class="text-[10px] text-amber-800 hover:underline max-w-[220px] truncate"
                                    :title="`${a.articleDisplayName} — 클릭하면 조 본문 미리보기`"
                                    @click.stop="openPreview(a)">{{ a.articleDisplayName }}</button>
                                  <button v-if="canEdit"
                                    class="text-amber-400 hover:text-red-500 transition-colors"
                                    :disabled="unmappingId === refKeyOf(item, a)"
                                    @click.stop="removeArticleMapping(item, a)"
                                    title="조 매핑 해제">
                                    <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                                    </svg>
                                  </button>
                                </span>
                              </div>
                            </div>
                          </div>
                          <p v-else class="text-xs text-gray-400 py-2">매핑된 정책이 없습니다</p>
                        </div>

                        <!-- Right: add policy — 장 전체 또는 조 단위로 매핑 -->
                        <div v-if="canEdit" class="w-80 flex-shrink-0">
                          <p class="text-xs font-semibold text-gray-600 mb-2">정책 추가</p>
                          <div class="relative">
                            <input
                              v-model="searchQuery[item.id]"
                              class="input text-xs w-full pr-8"
                              placeholder="지침·장·조 번호·조 제목 검색..."
                              @focus="openPicker(item.id)"
                              @input="onSearchInput(item.id)"
                              @click.stop
                            />
                            <svg class="absolute right-2.5 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-gray-400 pointer-events-none"
                              fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                d="M21 21l-4.35-4.35M17 11A6 6 0 1 1 5 11a6 6 0 0 1 12 0z"/>
                            </svg>
                          </div>
                          <div v-if="activePicker === item.id && (candidatePolicies(item).length > 0 || articleHits.length > 0 || articleSearching)"
                            class="mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-72 overflow-y-auto"
                            @click.stop>
                            <!-- 조 검색 결과 — 검색어와 조 표기·조 제목이 맞는 조를 바로 매핑 -->
                            <template v-if="articleSearching || articleHits.length">
                              <p class="sticky top-0 z-10 px-3 py-1.5 bg-amber-50 border-b border-amber-100 text-[10px] text-amber-800">
                                <template v-if="articleSearching">조 검색 중...</template>
                                <template v-else>
                                  <b>조 검색 결과 {{ articleHits.length }}건</b><span v-if="articleHitsTruncated"> (상위 {{ ARTICLE_HIT_LIMIT }}건)</span>
                                  — 누르면 그 조만 바로 매핑됩니다
                                </template>
                              </p>
                              <button v-for="a in articleHits" :key="`hit-${a.id}`"
                                class="w-full text-left px-3 py-2 border-b border-amber-50 last:border-0 hover:bg-amber-50 transition-colors disabled:opacity-50 disabled:hover:bg-transparent"
                                :disabled="isArticleMapped(item, a) || mappingId === `${item.id}_a${a.id}`"
                                :title="isArticleMapped(item, a) ? '이미 매핑된 조입니다' : '이 조만 매핑'"
                                @click.stop="addArticleFromSearch(item, a)">
                                <div class="flex items-center gap-1.5">
                                  <span class="text-[10px] px-1 py-0.5 rounded bg-amber-200 text-amber-900 font-bold flex-shrink-0">
                                    {{ a.articleLabel }}
                                  </span>
                                  <span class="text-[11px] text-gray-800 truncate">{{ a.title || '(제목 없음)' }}</span>
                                  <span v-if="isArticleMapped(item, a)"
                                    class="text-[10px] text-green-600 font-semibold ml-auto flex-shrink-0">매핑됨</span>
                                </div>
                                <p class="text-[10px] text-gray-400 mt-0.5 truncate">
                                  {{ a.guidelineName || '(지침 미분류)' }}
                                  <span v-if="a.chapterLabel"> &gt; {{ a.chapterLabel }}</span>
                                  <span v-if="a.chapterTitle"> {{ a.chapterTitle }}</span>
                                </p>
                              </button>
                            </template>

                            <p v-if="candidatePolicies(item).length > 0"
                              class="sticky top-0 z-10 px-3 py-1.5 bg-gray-50 border-b border-gray-100 text-[10px] text-gray-500">
                              정책(장) {{ candidatePolicies(item).length }}건 — 장 전체를 누르거나 <b class="text-amber-700">조</b> 를 펼쳐 조문만 매핑
                            </p>
                            <div v-for="p in candidatePolicies(item)" :key="p.id"
                              class="border-b border-gray-50 last:border-0">
                              <div class="flex items-stretch">
                                <button
                                  class="flex-1 min-w-0 text-left px-3 py-2 hover:bg-blue-50 transition-colors disabled:hover:bg-transparent disabled:opacity-60"
                                  :disabled="isWholeMapped(item, p) || mappingId === `${item.id}_${p.id}`"
                                  :title="isWholeMapped(item, p) ? '이미 장 전체가 매핑되어 있습니다' : '장 전체 매핑'"
                                  @click.stop="addMapping(item, p)">
                                  <div class="flex items-center gap-2">
                                    <span class="text-[10px] px-1 py-0.5 rounded font-semibold flex-shrink-0"
                                      :class="policyStatusBadge(p.status)">{{ policyStatusLabel(p.status) }}</span>
                                    <span class="text-xs text-gray-800 truncate">{{ p.title }}</span>
                                    <span v-if="isWholeMapped(item, p)"
                                      class="text-[10px] text-green-600 font-semibold flex-shrink-0">매핑됨</span>
                                  </div>
                                  <p class="text-[10px] text-gray-400 mt-0.5">
                                    {{ categoryLabel(p.category) }}
                                    <span v-if="mappedArticleCount(item, p)" class="text-amber-600">
                                      · 조 {{ mappedArticleCount(item, p) }}건 매핑됨
                                    </span>
                                  </p>
                                </button>
                                <button v-if="p.articleCount"
                                  class="px-2 flex items-center gap-0.5 border-l border-gray-50 text-[10px] font-semibold text-amber-700 hover:bg-amber-50 transition-colors"
                                  :title="`조 ${p.articleCount}개 펼치기`"
                                  @click.stop="toggleArticleList(p)">
                                  조 {{ p.articleCount }}
                                  <svg class="w-3 h-3 transition-transform" :class="openArticleList === p.id ? 'rotate-180' : ''"
                                    fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                                  </svg>
                                </button>
                              </div>

                              <!-- 조 목록 -->
                              <div v-if="openArticleList === p.id"
                                class="bg-amber-50/50 border-t border-amber-100 max-h-48 overflow-y-auto">
                                <p v-if="articleLoading === p.id" class="px-3 py-2 text-[10px] text-gray-400">조문 불러오는 중...</p>
                                <p v-else-if="!(articleCache[p.id] || []).length" class="px-3 py-2 text-[10px] text-gray-400">
                                  세분화된 조가 없습니다
                                </p>
                                <button v-for="a in articleCache[p.id] || []" :key="a.id"
                                  class="w-full text-left px-3 py-1.5 hover:bg-amber-100 transition-colors flex items-center gap-1.5 disabled:opacity-50 disabled:hover:bg-transparent"
                                  :disabled="isArticleMapped(item, a) || mappingId === `${item.id}_a${a.id}`"
                                  :title="isArticleMapped(item, a) ? '이미 매핑된 조입니다' : '이 조만 매핑'"
                                  @click.stop="addArticleMapping(item, p, a)">
                                  <span class="text-[10px] px-1 py-0.5 rounded bg-amber-200 text-amber-900 font-bold flex-shrink-0">
                                    {{ a.articleLabel }}
                                  </span>
                                  <span class="text-[11px] text-gray-700 truncate">{{ a.title || '(제목 없음)' }}</span>
                                  <span v-if="isArticleMapped(item, a)"
                                    class="text-[10px] text-green-600 font-semibold ml-auto flex-shrink-0">매핑됨</span>
                                </button>
                              </div>
                            </div>
                          </div>
                          <p v-else-if="activePicker === item.id"
                            class="mt-1 text-xs text-gray-400 text-center py-2">
                            {{ searchQuery[item.id] ? '지침·장·조에서 검색 결과가 없습니다' : '등록된 정책이 없습니다' }}
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
            </table></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 매핑 정책 내용 미리보기 -->
    <PolicyDetailModal :open="showPreview" :item-id="previewPolicyId" :focus-article-id="previewArticleId"
      readonly @close="showPreview = false" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ismsApi, policyApi } from '@/api/index.js'
import { useAuthStore } from '@/stores/auth'
import PolicyDetailModal from '@/views/policy/PolicyDetailModal.vue'

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
function onDocClick() { activePicker.value = null; openArticleList.value = null; resetArticleHits() }
onMounted(() => document.addEventListener('click', onDocClick))
onBeforeUnmount(() => { document.removeEventListener('click', onDocClick); resetArticleHits() })

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
    if (activePicker.value === id) { activePicker.value = null; openArticleList.value = null; resetArticleHits() }
  } else {
    next.add(id)
  }
  expandedRows.value = next
}

// ── 정책 미리보기 ─────────────────────────────────────────────
const showPreview = ref(false)
const previewPolicyId = ref(null)
const previewArticleId = ref(null)

/** 조 매핑을 누르면 소속 장을 열고 그 조를 펼친 채로 보여준다. */
function openPreview(ref_) {
  previewPolicyId.value = ref_.id
  previewArticleId.value = ref_.articleId || null
  showPreview.value = true
  activePicker.value = null
}

// ── 매핑 표시 (장 > 조) ───────────────────────────────────────

/** 매핑 1건의 고유 키 — 같은 장에 조가 여러 건 걸릴 수 있어 정책 id 만으로는 부족하다. */
function refKey(r) {
  return r.articleId ? `a${r.articleId}` : `p${r.id}`
}
function refKeyOf(item, r) {
  return `${item.id}_${refKey(r)}`
}
function refChipLabel(r) {
  return r.articleId ? `${r.chapterLabel || ''} ${r.articleLabel}`.trim() : r.title
}
function refTooltip(r) {
  return r.articleId ? `${r.title} ${r.articleDisplayName}` : r.title
}

/** 매핑 목록을 장(정책) 기준으로 묶는다. 장 전체 매핑은 whole, 조 매핑은 articles 로 나뉜다. */
function mappedGroups(item) {
  const groups = new Map()
  for (const r of item.mappedPolicies || []) {
    if (!groups.has(r.id)) {
      groups.set(r.id, {
        policyId: r.id, title: r.title, status: r.status, category: r.category,
        whole: null, articles: []
      })
    }
    const g = groups.get(r.id)
    if (r.articleId) g.articles.push(r)
    else g.whole = r
  }
  return Array.from(groups.values())
}

// ── Policy picker ─────────────────────────────────────────────
const articleCache = ref({})       // policyId → 조 목록
const openArticleList = ref(null)  // 조 목록을 펼친 정책 id
const articleLoading = ref(null)

// ── 조 검색 — 검색어를 조 표기(제N조)·조 제목에 맞춰 서버에서 찾는다 ──
// 장을 펼치지 않고도 조를 한 번에 매핑할 수 있게 하는 것이 목적이라, 결과는
// 정책(장) 목록 위에 따로 띄우고 클릭 한 번으로 그 조만 매핑한다.
const ARTICLE_HIT_LIMIT = 30
const articleHits = ref([])
const articleHitsTruncated = ref(false)
const articleSearching = ref(false)
let articleSearchTimer = null
let articleSearchSeq = 0

function onSearchInput(itemId) {
  activePicker.value = itemId
  const q = (searchQuery.value[itemId] || '').trim()
  clearTimeout(articleSearchTimer)
  // 한 글자로는 조 전체가 걸려 의미가 없으므로 2자부터 찾는다.
  if (q.length < 2) {
    articleHits.value = []
    articleHitsTruncated.value = false
    articleSearching.value = false
    return
  }
  articleSearching.value = true
  articleSearchTimer = setTimeout(() => searchArticles(q), 250)
}

async function searchArticles(q) {
  const seq = ++articleSearchSeq
  try {
    const res = await policyApi.articles({
      keyword: q, scope: 'HEADING', page: 0, size: ARTICLE_HIT_LIMIT
    })
    if (seq !== articleSearchSeq) return   // 뒤늦게 도착한 이전 요청은 버린다
    const page = res.data || {}
    articleHits.value = page.content || []
    articleHitsTruncated.value = (page.totalElements || 0) > articleHits.value.length
  } catch {
    if (seq !== articleSearchSeq) return
    articleHits.value = []
    articleHitsTruncated.value = false
  } finally {
    if (seq === articleSearchSeq) articleSearching.value = false
  }
}

function resetArticleHits() {
  clearTimeout(articleSearchTimer)
  articleSearchSeq++
  articleHits.value = []
  articleHitsTruncated.value = false
  articleSearching.value = false
}

function openPicker(itemId) {
  activePicker.value = itemId
  // 다른 항목의 검색 결과가 남아 보이지 않도록 현재 검색어 기준으로 다시 맞춘다.
  const q = (searchQuery.value[itemId] || '').trim()
  if (q.length < 2) resetArticleHits()
  else if (!articleHits.value.length && !articleSearching.value) onSearchInput(itemId)
}

/** 조 검색 결과 1건을 그대로 조 단위 매핑으로 넘긴다. */
function addArticleFromSearch(item, hit) {
  return addArticleMapping(
    item,
    {
      id: hit.policyId,
      title: hit.policyTitle,
      status: hit.status,
      category: hit.category,
      guidelineName: hit.guidelineName,
      chapterLabel: hit.chapterLabel,
      chapterTitle: hit.chapterTitle
    },
    { id: hit.id, articleLabel: hit.articleLabel, title: hit.title, displayName: hit.displayName }
  )
}

/**
 * 매핑 후보 정책(장) — 검색어로만 거른 전체를 돌려준다.
 * 장 전체가 이미 매핑됐어도 그 안의 조를 따로 매핑할 수 있어야 하므로 목록에서 빼지 않고
 * "매핑됨" 으로 표시만 한다. 목록 자체가 스크롤되므로 건수도 자르지 않는다.
 */
function candidatePolicies(item) {
  const q = (searchQuery.value[item.id] || '').trim().toLowerCase()
  return allPolicies.value.filter(p => !q || matchesQuery(p, q))
}

/** 제목뿐 아니라 지침명·장 제목으로도 찾을 수 있게 한다. */
function matchesQuery(p, q) {
  return [p.title, p.guidelineName, p.chapterLabel, p.chapterTitle]
    .some(v => v && v.toLowerCase().includes(q))
}

function isWholeMapped(item, policy) {
  return (item.mappedPolicies || []).some(r => r.id === policy.id && !r.articleId)
}
function isArticleMapped(item, article) {
  return (item.mappedPolicies || []).some(r => r.articleId === article.id)
}
function mappedArticleCount(item, policy) {
  return (item.mappedPolicies || []).filter(r => r.id === policy.id && r.articleId).length
}

async function toggleArticleList(policy) {
  if (openArticleList.value === policy.id) {
    openArticleList.value = null
    return
  }
  openArticleList.value = policy.id
  if (articleCache.value[policy.id]) return

  articleLoading.value = policy.id
  try {
    articleCache.value = { ...articleCache.value, [policy.id]: (await policyApi.articlesOf(policy.id)).data || [] }
  } catch {
    articleCache.value = { ...articleCache.value, [policy.id]: [] }
  } finally {
    articleLoading.value = null
  }
}

// ── Mapping actions ───────────────────────────────────────────

/** 장(章) 전체 매핑 */
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
      category: policy.category,
      guidelineName: policy.guidelineName,
      chapterLabel: policy.chapterLabel,
      chapterTitle: policy.chapterTitle,
      articleId: null
    })
    // 조를 이어서 고를 수 있도록 검색창·목록은 닫지 않는다.
  } catch {
    // silent fail
  } finally {
    mappingId.value = null
  }
}

/** 조(條) 단위 매핑 */
async function addArticleMapping(item, policy, article) {
  const key = `${item.id}_a${article.id}`
  if (mappingId.value === key) return
  mappingId.value = key
  try {
    await ismsApi.mapArticle(item.id, article.id)
    if (!item.mappedPolicies) item.mappedPolicies = []
    item.mappedPolicies.push({
      id: policy.id,
      title: policy.title,
      status: policy.status,
      category: policy.category,
      guidelineName: policy.guidelineName,
      chapterLabel: policy.chapterLabel,
      chapterTitle: policy.chapterTitle,
      articleId: article.id,
      articleLabel: article.articleLabel,
      articleTitle: article.title,
      articleDisplayName: article.displayName
    })
  } catch {
    // silent fail
  } finally {
    mappingId.value = null
  }
}

/** 장 전체 매핑만 해제한다(같은 장의 조 매핑은 그대로). */
async function removeMapping(item, policyRef) {
  const key = refKeyOf(item, policyRef)
  if (unmappingId.value === key) return
  unmappingId.value = key
  try {
    await ismsApi.unmapPolicy(item.id, policyRef.id)
    item.mappedPolicies = item.mappedPolicies.filter(r => !(r.id === policyRef.id && !r.articleId))
  } catch {
    // silent fail
  } finally {
    unmappingId.value = null
  }
}

async function removeArticleMapping(item, articleRef) {
  const key = refKeyOf(item, articleRef)
  if (unmappingId.value === key) return
  unmappingId.value = key
  try {
    await ismsApi.unmapArticle(item.id, articleRef.articleId)
    item.mappedPolicies = item.mappedPolicies.filter(r => r.articleId !== articleRef.articleId)
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
