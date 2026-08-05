<template>
  <div class="flex items-center gap-2">
    <span v-if="pi.revealed"
      class="inline-flex items-center gap-1 px-2 py-1 rounded-lg bg-red-50 text-red-600 text-xs font-semibold">
      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.542-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.542 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21"/>
      </svg>
      개인정보 원문 표시 중
    </span>
    <span v-else class="inline-flex items-center gap-1 px-2 py-1 rounded-lg bg-gray-100 text-gray-500 text-xs font-medium"
      title="관리 > 코드관리 > 개인정보 유형별 항목관리의 항목별 마스킹 기준이 적용됩니다.">
      <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
          d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
      </svg>
      개인정보 마스킹 적용
    </span>
    <button v-if="pi.canReveal" type="button" @click="toggle" :disabled="busy"
      class="text-xs font-medium px-2.5 py-1 rounded-lg border border-gray-200 text-gray-600 hover:bg-gray-50 disabled:opacity-50 transition-colors">
      {{ pi.revealed ? '마스킹 적용' : '마스킹 해제' }}
    </button>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { usePiMaskingStore } from '@/stores/piMasking'

const props = defineProps({
  /** 감사로그에 남길 화면 이름 — MANAGER 해제 허용 여부 판단에도 쓰인다 */
  screen: { type: String, default: '' },
})

const pi = usePiMaskingStore()
const busy = ref(false)

async function toggle() {
  busy.value = true
  try {
    await pi.toggleReveal(props.screen)
  } finally {
    busy.value = false
  }
}

// 화면을 등록해 두면 스토어가 이 화면에서 해제가 가능한지·유효한지 판단한다
watch(() => props.screen, s => pi.setScreen(s), { immediate: true })

onMounted(() => pi.load())
onUnmounted(() => pi.clearScreen(props.screen))
</script>
