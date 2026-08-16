<template>
  <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
    <div class="absolute inset-0 bg-black/40" @click="$emit('close')"></div>

    <div class="relative bg-white rounded-xl shadow-xl w-full max-w-4xl max-h-[92vh] flex flex-col">
      <div class="flex items-start justify-between gap-4 px-5 py-3 border-b shrink-0">
        <div v-if="policy" class="min-w-0">
          <h2 class="text-lg font-semibold text-gray-900 truncate">{{ policy.title }}</h2>
          <div class="flex flex-wrap gap-1.5 mt-1.5">
            <span :class="statusBadgeClass(policy.status)">{{ $t(`policy.status.${policy.status}`) }}</span>
            <span class="badge-blue">{{ $t(`policy.category_label.${policy.category}`) }}</span>
            <span class="badge-gray">v{{ policy.version }}</span>
          </div>
        </div>
        <div v-else class="text-lg font-semibold text-gray-900">{{ $t('policy.title') }}</div>
        <div class="flex items-center gap-2 shrink-0">
          <template v-if="!readonly">
            <button v-if="policy && !policy.acknowledgedByMe && policy.status === 'PUBLISHED'" @click="acknowledge" class="btn-primary text-sm">
              {{ $t('policy.acknowledge') }}
            </button>
            <span v-if="policy && policy.acknowledgedByMe" class="badge-green text-xs px-2 py-1">✓ {{ $t('policy.acknowledged') }}</span>
            <button v-if="policy && isManager" @click="$emit('edit', policy.id)" class="btn-secondary text-sm">{{ $t('common.edit') }}</button>
            <button v-if="policy && isAdmin" @click="deletePolicy" class="btn-danger text-sm">{{ $t('common.delete') }}</button>
          </template>
          <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600 p-1">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
      </div>

      <div class="px-5 py-4 overflow-y-auto flex-1">
        <div v-if="loading" class="py-16 text-center text-gray-400">{{ $t('common.loading') }}</div>
        <div v-else-if="loadError" class="py-16 text-center text-sm text-red-500">{{ loadError }}</div>
        <template v-else-if="policy">
          <div class="border border-gray-200 rounded-lg p-4 mb-4 grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
            <div><p class="text-gray-500">{{ $t('policy.author') }}</p><p class="font-medium mt-1">{{ policy.authorName }}</p></div>
            <div><p class="text-gray-500">{{ $t('policy.effectiveDate') }}</p><p class="font-medium mt-1">{{ policy.effectiveDate || '-' }}</p></div>
            <div><p class="text-gray-500">수신 확인</p><p class="font-medium mt-1">{{ policy.acknowledgmentCount }}명</p></div>
            <div><p class="text-gray-500">최종 수정</p><p class="font-medium mt-1">{{ formatDate(policy.updatedAt) }}</p></div>
          </div>
          <!-- 보기 전환 — 조문별 / 전체 원문 -->
          <div v-if="articles.length" class="flex items-center gap-2 mb-3">
            <div class="inline-flex rounded-lg border border-gray-200 p-0.5 bg-gray-50">
              <button v-for="m in viewModes" :key="m.value" @click="viewMode = m.value"
                :class="['px-3 py-1.5 text-xs font-medium rounded-md transition-colors',
                         viewMode === m.value ? 'bg-white text-primary-700 shadow-sm' : 'text-gray-500 hover:text-gray-700']">
                {{ m.label }}
              </button>
            </div>
            <span class="text-xs text-gray-400">조 {{ articles.length }}개</span>
            <button v-if="viewMode === 'articles'" @click="toggleAllArticles"
              class="ml-auto text-xs text-gray-500 hover:text-primary-600 underline">
              {{ allExpanded ? '모두 접기' : '모두 펼치기' }}
            </button>
          </div>

          <!-- 조문별 보기 -->
          <div v-if="articles.length && viewMode === 'articles'" class="space-y-2">
            <div v-for="a in articles" :key="a.id" class="border border-gray-200 rounded-lg overflow-hidden">
              <button class="w-full flex items-center gap-2 px-4 py-2.5 text-left hover:bg-gray-50 transition-colors"
                @click="toggleArticle(a.id)">
                <span class="shrink-0 px-2 py-0.5 rounded-md bg-primary-50 text-primary-700 text-xs font-bold">
                  {{ a.articleLabel }}
                </span>
                <span class="text-sm font-medium text-gray-900">{{ a.title || '(제목 없음)' }}</span>
                <span v-if="a.note" class="text-xs text-amber-600">{{ a.note }}</span>
                <svg class="w-4 h-4 text-gray-400 ml-auto shrink-0 transition-transform"
                  :class="expandedArticles.has(a.id) ? 'rotate-180' : ''"
                  fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                </svg>
              </button>
              <div v-if="expandedArticles.has(a.id)" class="px-4 pb-3 pt-1 border-t border-gray-100">
                <MarkdownView :content="a.content" />
              </div>
            </div>
          </div>

          <!-- 전체 원문 -->
          <MarkdownView v-else :content="policy.content" />
        </template>
      </div>

      <div class="flex justify-end px-5 py-3 border-t shrink-0">
        <button type="button" @click="$emit('close')" class="btn-secondary text-sm">닫기</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import MarkdownView from '@/components/MarkdownView.vue'
import { policyApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const props = defineProps({
  open: { type: Boolean, default: false },
  itemId: { type: [Number, String], default: null },
  // 내용 확인용 미리보기 — 수신확인/수정/삭제 액션을 감춘다
  readonly: { type: Boolean, default: false },
  // 열 때 펼쳐 둘 조(條) — ISMS 조 매핑에서 넘어올 때 그 조를 바로 보여준다
  focusArticleId: { type: [Number, String], default: null }
})
const emit = defineEmits(['close', 'edit', 'changed'])

const auth = useAuthStore()
const isManager = auth.isManager
const isAdmin = auth.isAdmin

const policy = ref(null)
const loading = ref(false)
const loadError = ref(null)

// 세분화된 조(條)
const viewModes = [
  { value: 'articles', label: '조문별 보기' },
  { value: 'full',     label: '전체 원문' }
]
const viewMode = ref('articles')
const articles = ref([])
const expandedArticles = ref(new Set())

const allExpanded = computed(() =>
  articles.value.length > 0 && articles.value.every(a => expandedArticles.value.has(a.id)))

function toggleArticle(id) {
  const next = new Set(expandedArticles.value)
  next.has(id) ? next.delete(id) : next.add(id)
  expandedArticles.value = next
}

function toggleAllArticles() {
  expandedArticles.value = allExpanded.value ? new Set() : new Set(articles.value.map(a => a.id))
}

watch(() => props.open, async (open) => {
  if (!open || !props.itemId) return
  loading.value = true
  policy.value = null
  articles.value = []
  expandedArticles.value = new Set()
  viewMode.value = 'articles'
  loadError.value = null
  try {
    policy.value = (await policyApi.get(props.itemId)).data
    // 조문 조회가 실패해도 원문 보기는 가능해야 하므로 별도로 감싼다.
    try { articles.value = (await policyApi.articlesOf(props.itemId)).data || [] }
    catch { articles.value = [] }
    // 특정 조를 지정해 열었으면 그 조만 펼쳐 둔다.
    if (props.focusArticleId != null) {
      const focus = Number(props.focusArticleId)
      if (articles.value.some(a => a.id === focus)) expandedArticles.value = new Set([focus])
    }
  }
  catch (e) { loadError.value = typeof e === 'string' ? e : '정책을 불러오지 못했습니다.' }
  finally { loading.value = false }
})

async function acknowledge() {
  await policyApi.acknowledge(props.itemId)
  policy.value.acknowledgedByMe = true
  policy.value.acknowledgmentCount++
}

async function deletePolicy() {
  if (!confirm('정책을 삭제하시겠습니까?')) return
  try {
    await policyApi.delete(props.itemId)
    emit('changed')
    emit('close')
  } catch (e) {
    alert(typeof e === 'string' ? e : '삭제에 실패했습니다')
  }
}

function statusBadgeClass(status) {
  return { DRAFT: 'badge-gray', REVIEW: 'badge-yellow', PUBLISHED: 'badge-green', ARCHIVED: 'badge-gray' }[status] || 'badge-gray'
}
function formatDate(dt) { return dt ? new Date(dt).toLocaleDateString('ko-KR') : '-' }
</script>
