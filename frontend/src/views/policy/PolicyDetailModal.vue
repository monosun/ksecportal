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
          <button v-if="policy && !policy.acknowledgedByMe && policy.status === 'PUBLISHED'" @click="acknowledge" class="btn-primary text-sm">
            {{ $t('policy.acknowledge') }}
          </button>
          <span v-if="policy && policy.acknowledgedByMe" class="badge-green text-xs px-2 py-1">✓ {{ $t('policy.acknowledged') }}</span>
          <button v-if="policy && isManager" @click="$emit('edit', policy.id)" class="btn-secondary text-sm">{{ $t('common.edit') }}</button>
          <button v-if="policy && isAdmin" @click="deletePolicy" class="btn-danger text-sm">{{ $t('common.delete') }}</button>
          <button @click="$emit('close')" class="text-gray-400 hover:text-gray-600 p-1">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>
      </div>

      <div class="px-5 py-4 overflow-y-auto flex-1">
        <div v-if="loading" class="py-16 text-center text-gray-400">{{ $t('common.loading') }}</div>
        <template v-else-if="policy">
          <div class="border border-gray-200 rounded-lg p-4 mb-4 grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
            <div><p class="text-gray-500">{{ $t('policy.author') }}</p><p class="font-medium mt-1">{{ policy.authorName }}</p></div>
            <div><p class="text-gray-500">{{ $t('policy.effectiveDate') }}</p><p class="font-medium mt-1">{{ policy.effectiveDate || '-' }}</p></div>
            <div><p class="text-gray-500">수신 확인</p><p class="font-medium mt-1">{{ policy.acknowledgmentCount }}명</p></div>
            <div><p class="text-gray-500">최종 수정</p><p class="font-medium mt-1">{{ formatDate(policy.updatedAt) }}</p></div>
          </div>
          <MarkdownView :content="policy.content" />
        </template>
      </div>

      <div class="flex justify-end px-5 py-3 border-t shrink-0">
        <button type="button" @click="$emit('close')" class="btn-secondary text-sm">닫기</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import MarkdownView from '@/components/MarkdownView.vue'
import { policyApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const props = defineProps({
  open: { type: Boolean, default: false },
  itemId: { type: [Number, String], default: null }
})
const emit = defineEmits(['close', 'edit', 'changed'])

const auth = useAuthStore()
const isManager = auth.isManager
const isAdmin = auth.isAdmin

const policy = ref(null)
const loading = ref(false)

watch(() => props.open, async (open) => {
  if (!open || !props.itemId) return
  loading.value = true
  policy.value = null
  try { policy.value = (await policyApi.get(props.itemId)).data }
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
