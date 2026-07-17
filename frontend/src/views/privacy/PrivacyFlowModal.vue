<template>
  <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
    <div class="absolute inset-0 bg-black/40" @click="$emit('close')"></div>

    <div class="relative bg-white rounded-xl shadow-xl w-full max-w-5xl max-h-[92vh] flex flex-col">
      <div class="flex items-center justify-between px-5 py-3 border-b shrink-0">
        <div>
          <h2 class="text-lg font-semibold text-gray-900">{{ item?.name }} — 개인정보 흐름도</h2>
          <p class="text-xs text-gray-500 mt-0.5">
            <span v-if="item?.department">{{ item.department }}</span>
            <span v-if="item?.department && item?.systemName" class="mx-1.5 text-gray-300">·</span>
            <span v-if="item?.systemName">{{ item.systemName }}</span>
          </p>
        </div>
        <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <div v-if="item" class="px-5 py-5 overflow-y-auto flex-1 space-y-6">

        <!-- 라이프사이클 -->
        <section>
          <h3 class="text-xs font-bold text-gray-500 uppercase tracking-wider mb-3">개인정보 라이프사이클</h3>
          <div class="flex items-center gap-1 overflow-x-auto pb-1">
            <template v-for="(stage, i) in STAGES" :key="stage">
              <div class="flex flex-col items-center gap-1 shrink-0">
                <div class="px-4 py-2.5 rounded-xl text-sm font-semibold border-2 transition-colors"
                  :class="isActive(stage)
                    ? 'bg-primary-50 border-primary-500 text-primary-600'
                    : 'bg-gray-50 border-gray-200 text-gray-300'">
                  {{ stage }}
                </div>
                <span class="text-[10px]" :class="isActive(stage) ? 'text-primary-500' : 'text-gray-300'">
                  {{ isActive(stage) ? '해당' : '—' }}
                </span>
              </div>
              <svg v-if="i < STAGES.length - 1" class="w-5 h-5 shrink-0 -mt-4"
                :class="isActive(stage) && isActive(STAGES[i + 1]) ? 'text-primary-400' : 'text-gray-200'"
                fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"/>
              </svg>
            </template>
          </div>
          <p v-if="!item.lifecycle" class="text-xs text-gray-400 mt-2">
            라이프사이클이 입력되지 않았습니다. 처리현황 수정에서 해당 단계를 입력하면 표시됩니다.
          </p>
        </section>

        <!-- 업무흐름 -->
        <section>
          <h3 class="text-xs font-bold text-gray-500 uppercase tracking-wider mb-3">업무 흐름</h3>

          <div v-if="steps.length >= 2" class="flex flex-wrap items-stretch gap-2">
            <template v-for="(step, i) in steps" :key="i">
              <div class="flex items-center gap-2">
                <div class="relative bg-white border-2 border-gray-200 rounded-xl px-4 py-3 min-w-[150px] max-w-[240px]">
                  <span class="absolute -top-2.5 -left-2.5 w-6 h-6 rounded-full bg-primary-500 text-white
                               text-[11px] font-bold flex items-center justify-center">
                    {{ i + 1 }}
                  </span>
                  <p class="text-sm text-gray-700 leading-snug">{{ step }}</p>
                </div>
                <svg v-if="i < steps.length - 1" class="w-5 h-5 text-gray-300 shrink-0"
                  fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"/>
                </svg>
              </div>
            </template>
          </div>

          <div v-else-if="item.workflow" class="rounded-xl bg-gray-50 border border-gray-100 p-4">
            <p class="text-sm text-gray-600 whitespace-pre-wrap">{{ item.workflow }}</p>
            <p class="text-xs text-gray-400 mt-2">
              단계를 <code class="px-1 bg-white rounded border">→</code> 또는 줄바꿈으로 구분하면 흐름도로 그려집니다.
            </p>
          </div>

          <div v-else class="rounded-xl bg-gray-50 border border-gray-100 p-6 text-center">
            <p class="text-sm text-gray-400">업무흐름도가 입력되지 않았습니다.</p>
            <p class="text-xs text-gray-400 mt-1">
              처리현황 수정에서 단계를 <code class="px-1 bg-white rounded border">→</code>로 구분해 입력하세요.
            </p>
          </div>
        </section>

        <!-- 제공처 -->
        <section v-if="provisions.length">
          <h3 class="text-xs font-bold text-gray-500 uppercase tracking-wider mb-3">이 업무에서 나가는 개인정보</h3>
          <div class="flex flex-wrap gap-2">
            <div v-for="p in provisions" :key="p.id"
              class="flex items-center gap-2 rounded-xl border border-gray-200 px-3 py-2">
              <svg class="w-4 h-4 text-gray-300 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"/>
              </svg>
              <div>
                <p class="text-sm font-medium text-gray-800">
                  {{ p.recipient }}<span v-if="p.country" class="text-gray-400"> ({{ p.country }})</span>
                </p>
                <span :class="TYPE_BADGE[p.provisionType]">{{ TYPE[p.provisionType] }}</span>
              </div>
            </div>
          </div>
        </section>

        <!-- 처리 정보 -->
        <section class="grid grid-cols-2 gap-4">
          <div class="rounded-xl bg-gray-50 border border-gray-100 p-4">
            <p class="text-xs font-semibold text-gray-500 mb-1">개인정보 항목</p>
            <p class="text-sm text-gray-700">{{ item.infoItems || '—' }}</p>
          </div>
          <div class="rounded-xl bg-gray-50 border border-gray-100 p-4">
            <p class="text-xs font-semibold text-gray-500 mb-1">처리목적</p>
            <p class="text-sm text-gray-700">{{ item.purpose || '—' }}</p>
          </div>
          <div class="rounded-xl bg-gray-50 border border-gray-100 p-4">
            <p class="text-xs font-semibold text-gray-500 mb-1">보유기간</p>
            <p class="text-sm text-gray-700">{{ item.retentionPeriod || '—' }}</p>
          </div>
          <div class="rounded-xl bg-gray-50 border border-gray-100 p-4">
            <p class="text-xs font-semibold text-gray-500 mb-1">처리근거</p>
            <p class="text-sm text-gray-700">{{ item.legalBasis || '—' }}</p>
          </div>
        </section>
      </div>

      <div class="flex justify-end px-5 py-3 border-t shrink-0">
        <button @click="$emit('close')" class="btn-secondary text-sm">닫기</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { privacyProcessingApi, privacyProvisionApi } from '@/api'

const props = defineProps({
  open: { type: Boolean, default: false },
  itemId: { type: [Number, String], default: null },
})
defineEmits(['close'])

const STAGES = ['수집', '보유', '이용', '제공', '파기']
const TYPE = { THIRD_PARTY: '제3자 제공', JOINT_USE: '공동이용', OVERSEAS: '국외이전' }
const TYPE_BADGE = {
  THIRD_PARTY: 'badge-blue', JOINT_USE: 'badge-yellow', OVERSEAS: 'badge-orange',
}

const item = ref(null)
const provisions = ref([])

function isActive(stage) {
  return (item.value?.lifecycle || '').includes(stage)
}

/** 업무흐름도 자유 텍스트를 단계 노드로 분해한다. 화살표 우선, 없으면 줄바꿈. */
const steps = computed(() => {
  const text = item.value?.workflow
  if (!text) return []
  let parts = text.split(/\s*(?:→|->|⇒|=>)\s*/)
  if (parts.length < 2) parts = text.split(/\r?\n/)
  return parts
    .map(s => s.trim().replace(/^(?:\d+\s*[).\]]|[①-⑳]|[-*•])\s*/, '').trim())
    .filter(Boolean)
})

watch(() => props.open, async (open) => {
  if (!open || !props.itemId) return
  item.value = null
  provisions.value = []
  try {
    const [detail, provs] = await Promise.all([
      privacyProcessingApi.get(props.itemId),
      privacyProvisionApi.list(),
    ])
    item.value = detail.data
    provisions.value = (provs.data || []).filter(p => p.processingId === Number(props.itemId))
  } catch {
    item.value = null
  }
})
</script>
