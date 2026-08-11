<template>
  <button type="button" :disabled="busy" :title="title"
    class="inline-flex items-center gap-1 px-2 py-1 rounded-lg border border-gray-200 text-[11px] font-medium
           text-gray-600 hover:text-green-700 hover:border-green-300 hover:bg-green-50
           disabled:opacity-50 disabled:cursor-wait transition-colors">
    <svg v-if="busy" class="w-3.5 h-3.5 animate-spin" fill="none" viewBox="0 0 24 24">
      <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
      <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
    </svg>
    <svg v-else class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
        d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
    </svg>
    <span>{{ busy ? '다운로드 중' : label }}</span>
  </button>
</template>

<script setup>
defineProps({
  label: { type: String, default: '엑셀' },
  title: { type: String, default: '엑셀로 내려받기' },
  busy: { type: Boolean, default: false },
})
</script>
