<template>
  <div v-if="open" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4" @click.self="$emit('close')">
    <div class="bg-white rounded-xl shadow-xl w-full max-w-lg max-h-[85vh] flex flex-col">
      <div class="flex items-start justify-between gap-4 p-5 border-b">
        <div class="min-w-0">
          <p class="font-mono text-sm font-bold text-blue-700">{{ cveId }}</p>
          <div v-if="data" class="flex flex-wrap items-center gap-2 mt-1.5">
            <span v-if="data.severity" :class="severityBadge(data.severity)">{{ severityLabel(data.severity) }}</span>
            <span v-if="data.cvssScore != null" class="text-xs text-gray-500">
              CVSS <b class="text-gray-800">{{ data.cvssScore }}</b>
            </span>
          </div>
        </div>
        <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600 shrink-0">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <div class="p-5 overflow-y-auto flex-1 text-sm">
        <div v-if="loading" class="py-8 text-center text-gray-400">NVD에서 조회 중...</div>
        <div v-else-if="error" class="py-6 text-center">
          <p class="text-gray-500">{{ error }}</p>
          <p class="text-xs text-gray-400 mt-2">아래 NVD 원문 링크에서 직접 확인할 수 있습니다.</p>
        </div>
        <template v-else-if="data">
          <p class="text-xs font-semibold text-gray-500 mb-1">취약점 설명 (NVD)</p>
          <p class="text-gray-700 leading-relaxed whitespace-pre-line">{{ data.description || '설명이 제공되지 않는 CVE입니다.' }}</p>
        </template>
      </div>

      <div class="flex items-center justify-between gap-3 px-5 py-4 border-t bg-gray-50 rounded-b-xl">
        <a :href="`https://nvd.nist.gov/vuln/detail/${cveId}`" target="_blank" rel="noopener noreferrer"
          class="text-sm text-blue-600 hover:underline">NVD 원문 보기 ↗</a>
        <button @click="$emit('close')"
          class="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-100">닫기</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { nvdApi } from '@/api'

const props = defineProps({
  open: { type: Boolean, default: false },
  cveId: { type: String, default: '' },
})
defineEmits(['close'])

const data = ref(null)
const loading = ref(false)
const error = ref('')

// 같은 팝업이 CVE 를 바꿔가며 재사용되므로 열릴 때마다 다시 조회한다.
watch(() => [props.open, props.cveId], async ([open, cveId]) => {
  if (!open || !cveId) return
  data.value = null
  error.value = ''
  loading.value = true
  try {
    const res = await nvdApi.lookup(cveId)
    data.value = res.data
    if (!data.value) error.value = 'NVD에서 해당 CVE 정보를 찾지 못했습니다.'
  } catch (e) {
    error.value = typeof e === 'string' ? e : 'NVD 조회에 실패했습니다. 잠시 후 다시 시도해 주세요.'
  } finally {
    loading.value = false
  }
}, { immediate: true })

function severityLabel(s) {
  return { CRITICAL: '심각', HIGH: '높음', MEDIUM: '중간', LOW: '낮음' }[String(s).toUpperCase()] || s
}

function severityBadge(s) {
  return {
    CRITICAL: 'badge-red', HIGH: 'badge-orange', MEDIUM: 'badge-yellow', LOW: 'badge-gray',
  }[String(s).toUpperCase()] || 'badge-gray'
}
</script>
