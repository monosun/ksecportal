<template>
  <div class="p-6">
    <div class="flex flex-wrap items-center justify-between gap-3 mb-5">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">보안성 심의</h1>
        <p class="text-sm text-gray-500 mt-1">
          신규 시스템 구축·변경 시 설계 단계에서 보안 요구사항 충족 여부를 검토합니다
        </p>
      </div>
      <button @click="openCreate" class="btn-primary flex items-center gap-2 text-sm">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
        </svg>
        심의 요청
      </button>
    </div>

    <!-- 현황 -->
    <div v-if="summary" class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3 mb-5">
      <button v-for="c in summaryCards" :key="c.label" @click="applyStatusFilter(c.status)"
        :class="['rounded-xl border p-4 text-center transition-colors',
          c.status === filters.status ? 'ring-2 ring-primary-400 border-primary-200' : 'hover:bg-gray-50', c.bg]">
        <p :class="['text-2xl font-bold', c.color]">{{ c.value }}</p>
        <p class="text-xs text-gray-500 mt-1">{{ c.label }}</p>
      </button>
    </div>

    <!-- 필터 -->
    <div class="card mb-4 flex flex-wrap gap-3 items-end">
      <div class="flex flex-col gap-1">
        <label class="text-xs text-gray-500">진행 상태</label>
        <select v-model="filters.status" @change="load" class="input w-36 text-sm">
          <option value="">전체</option>
          <option v-for="s in STATUSES" :key="s" :value="s">{{ STATUS_LABEL[s] }}</option>
        </select>
      </div>
      <div class="flex flex-col gap-1">
        <label class="text-xs text-gray-500">심의 구분</label>
        <select v-model="filters.reviewType" @change="load" class="input w-36 text-sm">
          <option value="">전체</option>
          <option v-for="t in TYPES" :key="t" :value="t">{{ TYPE_LABEL[t] }}</option>
        </select>
      </div>
      <div class="flex flex-col gap-1 flex-1 min-w-48">
        <label class="text-xs text-gray-500">검색</label>
        <input v-model="filters.keyword" @input="debouncedSearch" placeholder="제목 · 시스템명 · 요청부서"
          class="input text-sm" />
      </div>
      <button v-if="filters.status || filters.reviewType || filters.keyword" @click="resetFilters"
        class="btn-secondary text-sm">초기화</button>
    </div>

    <!-- 목록 -->
    <div ref="listEl" class="card p-0 overflow-hidden">
      <div v-if="loading" class="p-10 text-center text-gray-400">불러오는 중...</div>
      <div v-else-if="!reviews.length" class="p-10 text-center text-gray-400">등록된 심의가 없습니다</div>
      <div v-else class="overflow-x-auto"><table class="w-full text-sm">
        <thead class="bg-gray-50 border-b">
          <tr>
            <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase">심의 건</th>
            <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase w-28">구분</th>
            <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase w-32">요청</th>
            <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase w-28">오픈예정</th>
            <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase w-40">검토 진행</th>
            <th class="text-left px-5 py-3 text-xs font-medium text-gray-500 uppercase w-32">상태</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-50">
          <tr v-for="r in reviews" :key="r.id" class="hover:bg-gray-50 cursor-pointer" @click="openDetail(r.id)">
            <td class="px-5 py-3">
              <p class="font-medium text-gray-900">{{ r.title }}</p>
              <p class="text-xs text-gray-400 mt-0.5">
                {{ r.systemName }}
                <span v-if="r.handlesPersonalData" class="ml-1.5 text-orange-500">개인정보</span>
                <span v-if="r.internetFacing" class="ml-1.5 text-blue-500">외부공개</span>
              </p>
            </td>
            <td class="px-5 py-3"><span class="badge-gray">{{ TYPE_LABEL[r.reviewType] || r.reviewType }}</span></td>
            <td class="px-5 py-3 text-xs text-gray-500">
              <p>{{ r.department || '-' }}</p>
              <p class="text-gray-400">{{ r.requesterName || '-' }}</p>
            </td>
            <td class="px-5 py-3 text-xs text-gray-500">{{ r.targetDate || '-' }}</td>
            <td class="px-5 py-3">
              <div class="flex items-center gap-2">
                <div class="flex-1 h-1.5 bg-gray-100 rounded-full overflow-hidden">
                  <div class="h-full rounded-full bg-primary-500"
                    :style="{ width: progress(r) + '%' }"></div>
                </div>
                <span class="text-xs text-gray-500 tabular-nums">{{ r.itemChecked }}/{{ r.itemTotal }}</span>
              </div>
              <p v-if="r.itemFailed" class="text-[11px] text-red-500 mt-0.5">부적합 {{ r.itemFailed }}건</p>
            </td>
            <td class="px-5 py-3">
              <span :class="statusBadge(r)">{{ statusText(r) }}</span>
            </td>
          </tr>
        </tbody>
      </table></div>
    </div>

    <div v-if="totalPages > 1" class="flex justify-center gap-2 mt-4">
      <button v-for="n in totalPages" :key="n" @click="page = n - 1; load()"
        :class="page === n - 1 ? 'btn-primary' : 'btn-secondary'" class="px-3 py-1 text-sm">{{ n }}</button>
    </div>

    <!-- 심의 요청 모달 -->
    <div v-if="showCreate" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-white rounded-xl shadow-xl w-full max-w-2xl max-h-[92vh] flex flex-col">
        <div class="flex items-center justify-between p-5 border-b">
          <h2 class="text-lg font-semibold text-gray-900">보안성 심의 요청</h2>
          <button @click="showCreate = false" class="text-gray-400 hover:text-gray-600">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
        <div class="p-5 space-y-4 overflow-y-auto flex-1">
          <div>
            <label class="text-sm font-medium text-gray-700">심의 제목 *</label>
            <input v-model="form.title" class="input w-full mt-1" placeholder="예: 고객포털 2.0 신규 구축 보안성 심의" />
          </div>
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label class="text-sm font-medium text-gray-700">대상 시스템·서비스 *</label>
              <input v-model="form.systemName" class="input w-full mt-1" placeholder="예: 고객포털" />
            </div>
            <div>
              <label class="text-sm font-medium text-gray-700">심의 구분</label>
              <select v-model="form.reviewType" class="input w-full mt-1">
                <option v-for="t in TYPES" :key="t" :value="t">{{ TYPE_LABEL[t] }}</option>
              </select>
            </div>
            <div>
              <label class="text-sm font-medium text-gray-700">요청 부서</label>
              <DepartmentInput v-model="form.department" input-class="input w-full mt-1" placeholder="부서 선택 또는 직접 입력 (예: 서비스개발팀)" />
            </div>
            <div>
              <label class="text-sm font-medium text-gray-700">오픈(적용) 예정일</label>
              <input v-model="form.targetDate" type="date" class="input w-full mt-1" />
            </div>
          </div>
          <div>
            <label class="text-sm font-medium text-gray-700">구축·변경 개요</label>
            <textarea v-model="form.description" rows="4" class="input w-full mt-1 resize-y"
              placeholder="주요 기능, 처리 데이터, 연계 시스템, 인프라 구성 등을 적어주세요"></textarea>
          </div>
          <div class="flex flex-wrap gap-5">
            <label class="flex items-center gap-2 text-sm text-gray-700">
              <input type="checkbox" v-model="form.handlesPersonalData" class="w-4 h-4 rounded border-gray-300 text-primary-600" />
              개인정보를 처리함
            </label>
            <label class="flex items-center gap-2 text-sm text-gray-700">
              <input type="checkbox" v-model="form.internetFacing" class="w-4 h-4 rounded border-gray-300 text-primary-600" />
              인터넷에 공개됨
            </label>
          </div>
          <div>
            <label class="text-sm font-medium text-gray-700">설계서 첨부</label>
            <input type="file" @change="e => form.file = e.target.files[0]" ref="createFileInput"
              class="block w-full mt-1 text-sm text-gray-500 file:mr-3 file:py-1.5 file:px-3 file:rounded file:border-0 file:text-sm file:bg-primary-50 file:text-primary-700" />
          </div>
          <p v-if="createError" class="text-sm text-red-600">{{ createError }}</p>
          <p class="text-xs text-gray-400">
            요청을 등록하면 기본 검토 체크리스트({{ DEFAULT_ITEM_COUNT }}개 항목)가 자동으로 생성됩니다.
          </p>
        </div>
        <div class="flex justify-end gap-3 px-5 py-4 border-t bg-gray-50 rounded-b-xl">
          <button @click="showCreate = false" class="btn-secondary text-sm">취소</button>
          <button @click="submitCreate" :disabled="saving || !form.title || !form.systemName"
            class="btn-primary text-sm disabled:opacity-50">{{ saving ? '등록 중...' : '요청 등록' }}</button>
        </div>
      </div>
    </div>

    <!-- 심의 상세 · 검토 모달 -->
    <div v-if="detail" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-gray-50 rounded-xl shadow-xl w-full max-w-5xl max-h-[92vh] flex flex-col">
        <div class="flex items-start justify-between gap-4 px-5 py-3 border-b bg-white rounded-t-xl">
          <div class="min-w-0">
            <h2 class="text-lg font-semibold text-gray-900 truncate">{{ detail.title }}</h2>
            <div class="flex flex-wrap items-center gap-1.5 mt-1.5">
              <span class="badge-gray">{{ TYPE_LABEL[detail.reviewType] }}</span>
              <span :class="statusBadge(detail)">{{ statusText(detail) }}</span>
              <span class="text-xs text-gray-500">{{ detail.systemName }}</span>
              <span v-if="detail.handlesPersonalData" class="badge-orange">개인정보</span>
              <span v-if="detail.internetFacing" class="badge-blue">외부공개</span>
            </div>
          </div>
          <button @click="detail = null" class="text-gray-400 hover:text-gray-600 shrink-0">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>

        <div class="px-5 py-4 overflow-y-auto flex-1 space-y-4">
          <!-- 요청 정보 -->
          <section class="bg-white rounded-xl border p-4">
            <div class="grid grid-cols-2 sm:grid-cols-4 gap-4 text-sm">
              <div><p class="text-xs text-gray-500">요청 부서</p><p class="text-gray-800">{{ detail.department || '-' }}</p></div>
              <div><p class="text-xs text-gray-500">요청자</p><p class="text-gray-800">{{ detail.requesterName || '-' }}</p></div>
              <div><p class="text-xs text-gray-500">오픈 예정일</p><p class="text-gray-800">{{ detail.targetDate || '-' }}</p></div>
              <div><p class="text-xs text-gray-500">요청일</p><p class="text-gray-800">{{ fmtDate(detail.createdAt) }}</p></div>
            </div>
            <div v-if="detail.description" class="mt-3 pt-3 border-t">
              <p class="text-xs text-gray-500 mb-1">구축·변경 개요</p>
              <p class="text-sm text-gray-700 whitespace-pre-line">{{ detail.description }}</p>
            </div>
            <div class="mt-3 pt-3 border-t flex items-center gap-3">
              <p class="text-xs text-gray-500">설계서</p>
              <button v-if="detail.hasFile" @click="downloadFile"
                class="text-sm text-primary-600 hover:underline">📎 {{ detail.fileName }}</button>
              <span v-else class="text-sm text-gray-400">첨부 없음</span>
              <label v-if="isManager" class="text-xs text-gray-500 hover:text-primary-600 cursor-pointer ml-auto">
                파일 {{ detail.hasFile ? '교체' : '첨부' }}
                <input type="file" class="hidden" @change="uploadFile" />
              </label>
            </div>
          </section>

          <!-- 검토 체크리스트 -->
          <section class="bg-white rounded-xl border">
            <div class="flex flex-wrap items-center justify-between gap-2 px-4 py-3 border-b">
              <h3 class="font-semibold text-gray-900 text-sm">
                설계 검토 체크리스트
                <span class="ml-1 text-xs font-normal text-gray-500">
                  {{ detail.itemChecked }}/{{ detail.itemTotal }} 검토 · 부적합 {{ detail.itemFailed }}
                </span>
              </h3>
              <button v-if="isManager" @click="showAddItem = !showAddItem"
                class="text-xs text-gray-500 hover:text-primary-600 px-2 py-1 rounded hover:bg-gray-100">
                + 항목 추가
              </button>
            </div>

            <div v-if="showAddItem && isManager" class="px-4 py-3 border-b bg-gray-50 grid grid-cols-1 sm:grid-cols-4 gap-2">
              <input v-model="newItem.category" class="input text-sm" placeholder="영역 (예: 접근통제)" />
              <input v-model="newItem.itemName" class="input text-sm sm:col-span-2" placeholder="검토 항목" />
              <button @click="addItem" :disabled="!newItem.itemName" class="btn-primary text-sm disabled:opacity-50">추가</button>
              <input v-model="newItem.criteria" class="input text-sm sm:col-span-4" placeholder="검토 기준 (선택)" />
            </div>

            <div v-for="(group, category) in groupedItems" :key="category" class="border-b last:border-b-0">
              <p class="px-4 pt-3 pb-1 text-xs font-bold text-gray-500">{{ category }}</p>
              <div v-for="item in group" :key="item.id" class="px-4 py-3 border-t border-gray-50">
                <div class="flex items-start justify-between gap-3">
                  <div class="min-w-0 flex-1">
                    <p class="text-sm font-medium text-gray-800">{{ item.itemName }}</p>
                    <p v-if="item.criteria" class="text-xs text-gray-400 mt-0.5">{{ item.criteria }}</p>
                  </div>
                  <div class="flex items-center gap-1 shrink-0">
                    <button v-for="r in RESULTS" :key="r" @click="setResult(item, r)" :disabled="!isManager"
                      :class="['text-xs px-2 py-1 rounded border transition-colors disabled:opacity-60',
                        item.result === r ? RESULT_ACTIVE[r] : 'border-gray-200 text-gray-500 hover:bg-gray-50']">
                      {{ RESULT_LABEL[r] }}
                    </button>
                    <button v-if="isManager" @click="removeItem(item)"
                      class="text-xs text-gray-300 hover:text-red-500 px-1">✕</button>
                  </div>
                </div>
                <textarea v-if="isManager" v-model="item.comment" @change="saveComment(item)" rows="1"
                  class="mt-2 w-full border border-gray-200 rounded-lg px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-primary-400 resize-y"
                  placeholder="검토 의견·보완 요구사항"></textarea>
                <p v-else-if="item.comment" class="mt-1 text-xs text-gray-600 whitespace-pre-line">{{ item.comment }}</p>
              </div>
            </div>
          </section>

          <!-- 심의 결과 -->
          <section class="bg-white rounded-xl border p-4">
            <h3 class="font-semibold text-gray-900 text-sm mb-3">심의 결과</h3>
            <template v-if="detail.status === 'COMPLETED'">
              <div class="flex items-center gap-2">
                <span :class="decisionBadge(detail.decision)">{{ DECISION_LABEL[detail.decision] }}</span>
                <span class="text-xs text-gray-500">{{ detail.reviewerName }} · {{ fmtDate(detail.reviewedAt) }}</span>
              </div>
              <p v-if="detail.reviewComment" class="mt-2 text-sm text-gray-700 whitespace-pre-line">{{ detail.reviewComment }}</p>
              <button v-if="isManager" @click="reopen" class="mt-3 text-xs text-gray-500 hover:text-primary-600 underline">
                재검토로 되돌리기
              </button>
            </template>
            <template v-else-if="isManager">
              <div class="flex flex-wrap gap-2 mb-3">
                <button v-for="d in DECISIONS" :key="d" @click="decisionForm.decision = d"
                  :class="['text-sm px-3 py-1.5 rounded-lg border transition-colors',
                    decisionForm.decision === d ? DECISION_ACTIVE[d] : 'border-gray-300 text-gray-600 hover:bg-gray-50']">
                  {{ DECISION_LABEL[d] }}
                </button>
                <button @click="setStatus('REVISION')"
                  class="text-sm px-3 py-1.5 rounded-lg border border-gray-300 text-gray-600 hover:bg-gray-50 ml-auto">
                  보완 요청으로 변경
                </button>
              </div>
              <textarea v-model="decisionForm.reviewComment" rows="3"
                class="input w-full text-sm resize-y" placeholder="심의 의견 · 승인 조건 · 보완 요구사항"></textarea>
              <p v-if="decisionError" class="mt-2 text-sm text-red-600">{{ decisionError }}</p>
              <div class="flex justify-end mt-3">
                <button @click="submitDecision" :disabled="!decisionForm.decision || saving"
                  class="btn-primary text-sm disabled:opacity-50">심의 결과 등록</button>
              </div>
            </template>
            <p v-else class="text-sm text-gray-400">심의 진행 중입니다.</p>
          </section>
        </div>

        <div class="flex justify-between px-5 py-3 border-t bg-white rounded-b-xl">
          <button v-if="isManager" @click="removeReview" class="text-sm text-red-500 hover:text-red-700">심의 삭제</button>
          <button @click="detail = null" class="btn-secondary text-sm ml-auto">닫기</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useFitPageSize, keepFirstRow } from '@/composables/useFitPageSize'
import { securityReviewApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import DepartmentInput from '@/components/DepartmentInput.vue'
import { useDebounceFn } from '@vueuse/core'

const auth = useAuthStore()
const isManager = auth.isManager

const STATUSES = ['REQUESTED', 'IN_REVIEW', 'REVISION', 'COMPLETED']
const STATUS_LABEL = { REQUESTED: '요청 접수', IN_REVIEW: '검토중', REVISION: '보완 요청', COMPLETED: '심의 완료' }
const TYPES = ['NEW', 'CHANGE', 'INTEGRATION', 'DECOMMISSION']
const TYPE_LABEL = { NEW: '신규 구축', CHANGE: '변경·고도화', INTEGRATION: '외부 연계', DECOMMISSION: '폐기·종료' }
const RESULTS = ['PASS', 'FAIL', 'NA']
const RESULT_LABEL = { PASS: '적합', FAIL: '부적합', NA: '해당없음', PENDING: '미검토' }
const RESULT_ACTIVE = {
  PASS: 'border-emerald-500 bg-emerald-50 text-emerald-700',
  FAIL: 'border-red-500 bg-red-50 text-red-700',
  NA: 'border-gray-400 bg-gray-100 text-gray-600',
}
const DECISIONS = ['APPROVED', 'CONDITIONAL', 'REJECTED']
const DECISION_LABEL = { APPROVED: '승인', CONDITIONAL: '조건부 승인', REJECTED: '반려' }
const DECISION_ACTIVE = {
  APPROVED: 'border-emerald-500 bg-emerald-50 text-emerald-700',
  CONDITIONAL: 'border-yellow-500 bg-yellow-50 text-yellow-700',
  REJECTED: 'border-red-500 bg-red-50 text-red-700',
}
const DEFAULT_ITEM_COUNT = 20

const reviews = ref([])
const summary = ref(null)
const loading = ref(false)
const page = ref(0)
const totalPages = ref(0)
const filters = ref({ status: '', reviewType: '', keyword: '' })

const showCreate = ref(false)
const saving = ref(false)
const createError = ref('')
const createFileInput = ref(null)
const form = ref({ title: '', systemName: '', reviewType: 'NEW', department: '', description: '',
  handlesPersonalData: false, internetFacing: false, targetDate: '', file: null })

const detail = ref(null)
const showAddItem = ref(false)
const newItem = ref({ category: '', itemName: '', criteria: '' })
const decisionForm = ref({ decision: '', reviewComment: '' })
const decisionError = ref('')

const summaryCards = computed(() => {
  const s = summary.value || {}
  return [
    { label: '요청 접수', value: s.requested ?? 0, status: 'REQUESTED', color: 'text-gray-800', bg: 'bg-white' },
    { label: '검토중', value: s.inReview ?? 0, status: 'IN_REVIEW', color: 'text-blue-600', bg: 'bg-blue-50/60' },
    { label: '보완 요청', value: s.revision ?? 0, status: 'REVISION', color: 'text-yellow-600', bg: 'bg-yellow-50/60' },
    { label: '심의 완료', value: s.completed ?? 0, status: 'COMPLETED', color: 'text-gray-700', bg: 'bg-white' },
    { label: '승인', value: s.approved ?? 0, status: '', color: 'text-emerald-600', bg: 'bg-emerald-50/60' },
    { label: '조건부·반려', value: (s.conditional ?? 0) + (s.rejected ?? 0), status: '', color: 'text-red-600', bg: 'bg-red-50/60' },
  ]
})

const groupedItems = computed(() => {
  const groups = {}
  for (const item of detail.value?.items || []) {
    (groups[item.category] ||= []).push(item)
  }
  return groups
})

function progress(r) {
  return r.itemTotal ? Math.round((r.itemChecked / r.itemTotal) * 100) : 0
}

function statusText(r) {
  return r.status === 'COMPLETED' ? DECISION_LABEL[r.decision] || '심의 완료' : STATUS_LABEL[r.status]
}

function statusBadge(r) {
  if (r.status === 'COMPLETED') return decisionBadge(r.decision)
  return { REQUESTED: 'badge-gray', IN_REVIEW: 'badge-blue', REVISION: 'badge-yellow' }[r.status] || 'badge-gray'
}

function decisionBadge(d) {
  return { APPROVED: 'badge-green', CONDITIONAL: 'badge-yellow', REJECTED: 'badge-red' }[d] || 'badge-gray'
}

function fmtDate(dt) {
  return dt ? new Date(dt).toLocaleDateString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' }) : '-'
}

// 한 페이지 건수는 화면 높이에 맞춘다(창 크기가 바뀌면 보던 위치를 유지한 채 재조회).
const { listEl, pageSize, refine: refinePageSize } = useFitPageSize({
  onChange: (size, prev) => { page.value = keepFirstRow(page.value, prev, size); load() }
})

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, size: pageSize.value }
    if (filters.value.status) params.status = filters.value.status
    if (filters.value.reviewType) params.reviewType = filters.value.reviewType
    if (filters.value.keyword) params.keyword = filters.value.keyword
    const res = await securityReviewApi.list(params)
    reviews.value = res.data?.content || []
    totalPages.value = res.data?.page?.totalPages ?? res.data?.totalPages ?? 0
    summary.value = (await securityReviewApi.summary()).data
  } finally {
    loading.value = false
  }
}

const debouncedSearch = useDebounceFn(() => { page.value = 0; load() }, 400)

function applyStatusFilter(status) {
  if (!status) return
  filters.value.status = filters.value.status === status ? '' : status
  page.value = 0
  load()
}

function resetFilters() {
  filters.value = { status: '', reviewType: '', keyword: '' }
  page.value = 0
  load()
}

function openCreate() {
  form.value = { title: '', systemName: '', reviewType: 'NEW', department: '', description: '',
    handlesPersonalData: false, internetFacing: false, targetDate: '', file: null }
  createError.value = ''
  if (createFileInput.value) createFileInput.value.value = ''
  showCreate.value = true
}

async function submitCreate() {
  saving.value = true
  createError.value = ''
  try {
    const res = await securityReviewApi.create(form.value)
    showCreate.value = false
    await load()
    detail.value = res.data
    decisionForm.value = { decision: '', reviewComment: '' }
  } catch (e) {
    createError.value = typeof e === 'string' ? e : '심의 요청 등록에 실패했습니다.'
  } finally {
    saving.value = false
  }
}

async function openDetail(id) {
  const res = await securityReviewApi.get(id)
  detail.value = res.data
  decisionForm.value = { decision: '', reviewComment: detail.value.reviewComment || '' }
  decisionError.value = ''
  showAddItem.value = false
}

async function refreshDetail() {
  if (detail.value) detail.value = (await securityReviewApi.get(detail.value.id)).data
}

async function setResult(item, result) {
  if (!isManager) return
  await securityReviewApi.updateItem(item.id, { result })
  item.result = result
  await refreshDetail()
  load()
}

async function saveComment(item) {
  if (!isManager) return
  await securityReviewApi.updateItem(item.id, { comment: item.comment })
}

async function addItem() {
  await securityReviewApi.addItem(detail.value.id, { ...newItem.value })
  newItem.value = { category: '', itemName: '', criteria: '' }
  showAddItem.value = false
  await refreshDetail()
}

async function removeItem(item) {
  if (!confirm('이 검토 항목을 삭제하시겠습니까?')) return
  await securityReviewApi.deleteItem(item.id)
  await refreshDetail()
}

async function setStatus(status) {
  await securityReviewApi.update(detail.value.id, { status })
  await refreshDetail()
  load()
}

async function submitDecision() {
  saving.value = true
  decisionError.value = ''
  try {
    const res = await securityReviewApi.decide(detail.value.id, decisionForm.value)
    detail.value = res.data
    load()
  } catch (e) {
    decisionError.value = typeof e === 'string' ? e : '심의 결과 등록에 실패했습니다.'
  } finally {
    saving.value = false
  }
}

async function reopen() {
  await securityReviewApi.update(detail.value.id, { status: 'IN_REVIEW' })
  await refreshDetail()
  load()
}

async function uploadFile(e) {
  const file = e.target.files[0]
  if (!file) return
  const res = await securityReviewApi.uploadFile(detail.value.id, file)
  detail.value = res.data
}

async function downloadFile() {
  await securityReviewApi.downloadFile(detail.value.id, detail.value.fileName)
}

async function removeReview() {
  if (!confirm(`"${detail.value.title}" 심의를 삭제하시겠습니까? 되돌릴 수 없습니다.`)) return
  await securityReviewApi.delete(detail.value.id)
  detail.value = null
  load()
}

onMounted(async () => { await load(); refinePageSize() })
</script>
