<template>
  <div v-if="notices.length" class="mb-5 space-y-2">
    <div
      v-for="notice in notices" :key="notice.id"
      class="flex items-start gap-3 px-4 py-3 rounded-xl border text-sm"
      :class="notice.pinned
        ? 'bg-primary-50 border-primary-200 text-primary-900'
        : 'bg-amber-50 border-amber-200 text-amber-900'">
      <span class="flex-shrink-0 mt-0.5">
        <svg v-if="notice.pinned" class="w-4 h-4 text-primary-500" fill="currentColor" viewBox="0 0 24 24">
          <path d="M16 3a1 1 0 00-1.447-.894L8 6H5a2 2 0 00-2 2v4a2 2 0 002 2h3l1 7h2l1-7 3.553-1.553A1 1 0 0016 11V3z"/>
        </svg>
        <svg v-else class="w-4 h-4 text-amber-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M11 5.882V19.24a1.76 1.76 0 01-3.417.592l-2.147-6.15M18 13a3 3 0 100-6M5.436 13.683A4.001 4.001 0 017 6h1.832c4.1 0 7.625-1.234 9.168-3v14c-1.543-1.766-5.067-3-9.168-3H7a3.988 3.988 0 01-1.564-.317z"/>
        </svg>
      </span>
      <div class="flex-1 min-w-0">
        <span class="font-semibold">{{ notice.title }}</span>
        <span v-if="notice.content" class="ml-2 text-xs opacity-75">{{ notice.content }}</span>
      </div>
      <span v-if="notice.pinned"
        class="flex-shrink-0 text-[10px] font-bold uppercase tracking-widest px-1.5 py-0.5 rounded-full bg-primary-100 text-primary-600">
        {{ $t('notice.pinned') }}
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { noticeApi } from '@/api'

const notices = ref([])

onMounted(async () => {
  try {
    const res = await noticeApi.listActive()
    notices.value = res.data || []
  } catch {
    // silent
  }
})
</script>
