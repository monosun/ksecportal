<template>
  <div v-if="open" class="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4">
    <div class="absolute inset-0 bg-black/40" @click="$emit('close')"></div>

    <div class="relative bg-gray-50 rounded-xl shadow-xl w-full max-w-4xl max-h-[92vh] flex flex-col">
      <div class="flex items-center justify-between gap-4 px-5 py-3 border-b bg-white rounded-t-xl shrink-0">
        <div class="min-w-0">
          <h2 class="text-lg font-semibold text-gray-900 truncate">
            <span v-if="itemCode" class="font-mono text-primary-700 mr-2">{{ itemCode }}</span>
            {{ itemName || $t('isms.title') }}
          </h2>
        </div>

        <!-- 목록 순서대로 이전/다음 항목 이동 (← / → 키도 동작) -->
        <div v-if="total > 1" class="flex items-center gap-1 shrink-0 ml-auto mr-1">
          <button @click="$emit('prev')" :disabled="!hasPrev"
            class="p-1.5 rounded-lg text-gray-500 hover:bg-gray-100 hover:text-gray-700 disabled:opacity-30 disabled:hover:bg-transparent transition-colors"
            title="이전 항목 (←)">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
            </svg>
          </button>
          <span class="text-xs text-gray-500 tabular-nums whitespace-nowrap px-1">
            {{ position }} / {{ total }}
          </span>
          <button @click="$emit('next')" :disabled="!hasNext"
            class="p-1.5 rounded-lg text-gray-500 hover:bg-gray-100 hover:text-gray-700 disabled:opacity-30 disabled:hover:bg-transparent transition-colors"
            title="다음 항목 (→)">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
            </svg>
          </button>
        </div>

        <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600 p-1 shrink-0">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <div class="px-5 py-4 overflow-y-auto flex-1">
        <IsmsEvidencePanel v-if="itemId" :item-id="itemId" :year="year"
          @changed="$emit('changed')" @update:year="$emit('update:year', $event)" />
      </div>

      <div class="flex items-center justify-between gap-3 px-5 py-3 border-t bg-white rounded-b-xl shrink-0">
        <div class="flex items-center gap-2">
          <button v-if="total > 1" @click="$emit('prev')" :disabled="!hasPrev"
            class="btn-secondary text-sm flex items-center gap-1 disabled:opacity-40 disabled:cursor-not-allowed">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
            </svg>
            이전 항목
          </button>
          <button v-if="total > 1" @click="$emit('next')" :disabled="!hasNext"
            class="btn-secondary text-sm flex items-center gap-1 disabled:opacity-40 disabled:cursor-not-allowed">
            다음 항목
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
            </svg>
          </button>
        </div>
        <button @click="$emit('close')" class="btn-secondary text-sm">닫기</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { watch, onBeforeUnmount } from 'vue'
import IsmsEvidencePanel from './IsmsEvidencePanel.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  itemId: { type: [Number, String], default: null },
  itemCode: { type: String, default: '' },
  itemName: { type: String, default: '' },
  year: { type: [Number, String], default: () => new Date().getFullYear() },
  // 목록에서의 위치 — 이전/다음 이동용
  position: { type: Number, default: 0 },
  total: { type: Number, default: 0 },
  hasPrev: { type: Boolean, default: false },
  hasNext: { type: Boolean, default: false },
})
const emit = defineEmits(['close', 'changed', 'update:year', 'prev', 'next'])

/** 입력 중에는 방향키를 항목 이동으로 가로채지 않는다. */
function isTyping(target) {
  if (!target) return false
  return ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName) || target.isContentEditable
}

function onKeydown(e) {
  if (!props.open || e.altKey || e.ctrlKey || e.metaKey || isTyping(e.target)) return
  if (e.key === 'ArrowLeft' && props.hasPrev) { e.preventDefault(); emit('prev') }
  else if (e.key === 'ArrowRight' && props.hasNext) { e.preventDefault(); emit('next') }
  else if (e.key === 'Escape') emit('close')
}

watch(() => props.open, (open) => {
  if (open) document.addEventListener('keydown', onKeydown)
  else document.removeEventListener('keydown', onKeydown)
}, { immediate: true })

onBeforeUnmount(() => document.removeEventListener('keydown', onKeydown))
</script>
