<template>
  <!-- 항목 상세는 목록에서 팝업으로 여는 것이 기본 동작이며,
       이 라우트는 북마크·직접 URL 접근을 위한 하위 호환용으로 남겨둔다. -->
  <div class="p-6 max-w-4xl mx-auto">
    <div class="flex items-center gap-3 mb-6">
      <button @click="$router.push(`/isms?year=${year}`)"
        class="text-gray-500 hover:text-gray-700 transition-colors">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
        </svg>
      </button>
      <h1 class="text-xl font-bold text-gray-900">{{ $t('isms.title') }}</h1>
    </div>

    <IsmsEvidencePanel :item-id="itemId" :year="year" @update:year="y => year = y" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import IsmsEvidencePanel from './IsmsEvidencePanel.vue'

const route = useRoute()
const itemId = route.params.id
const year = ref(parseInt(route.query.year) || new Date().getFullYear())
</script>
