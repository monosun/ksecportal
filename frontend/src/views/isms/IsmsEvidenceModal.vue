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

      <div class="flex justify-end px-5 py-3 border-t bg-white rounded-b-xl shrink-0">
        <button @click="$emit('close')" class="btn-secondary text-sm">닫기</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import IsmsEvidencePanel from './IsmsEvidencePanel.vue'

defineProps({
  open: { type: Boolean, default: false },
  itemId: { type: [Number, String], default: null },
  itemCode: { type: String, default: '' },
  itemName: { type: String, default: '' },
  year: { type: [Number, String], default: () => new Date().getFullYear() },
})
defineEmits(['close', 'changed', 'update:year'])
</script>
