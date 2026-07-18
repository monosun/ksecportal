<template>
  <div class="p-8 max-w-4xl mx-auto">
    <div v-if="loading" class="text-center text-gray-400 py-16">{{ $t('common.loading') }}</div>
    <template v-else-if="policy">
      <!-- Header -->
      <div class="flex items-start justify-between mb-6">
        <div>
          <RouterLink to="/policies" class="text-sm text-primary-600 hover:underline mb-2 inline-block">← {{ $t('common.back') }}</RouterLink>
          <h1 class="text-2xl font-bold text-gray-900">{{ policy.title }}</h1>
          <div class="flex flex-wrap gap-2 mt-2">
            <span :class="statusBadgeClass(policy.status)">{{ $t(`policy.status.${policy.status}`) }}</span>
            <span class="badge-blue">{{ $t(`policy.category_label.${policy.category}`) }}</span>
            <span class="badge-gray">v{{ policy.version }}</span>
          </div>
        </div>
        <div class="flex gap-2">
          <RouterLink v-if="isManager" :to="`/policies/${policy.id}/edit`" class="btn-secondary">{{ $t('common.edit') }}</RouterLink>
          <button v-if="isAdmin" @click="deletePolicy" class="btn-danger">{{ $t('common.delete') }}</button>
          <button v-if="!policy.acknowledgedByMe && policy.status === 'PUBLISHED'" @click="acknowledge" class="btn-primary">
            {{ $t('policy.acknowledge') }}
          </button>
          <span v-if="policy.acknowledgedByMe" class="badge-green text-sm px-3 py-1.5">✓ {{ $t('policy.acknowledged') }}</span>
        </div>
      </div>

      <!-- Meta -->
      <div class="card mb-6 grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
        <div><p class="text-gray-500">{{ $t('policy.author') }}</p><p class="font-medium mt-1">{{ policy.authorName }}</p></div>
        <div><p class="text-gray-500">{{ $t('policy.effectiveDate') }}</p><p class="font-medium mt-1">{{ policy.effectiveDate || '-' }}</p></div>
        <div><p class="text-gray-500">수신 확인</p><p class="font-medium mt-1">{{ policy.acknowledgmentCount }}명</p></div>
        <div><p class="text-gray-500">최종 수정</p><p class="font-medium mt-1">{{ formatDate(policy.updatedAt) }}</p></div>
      </div>

      <!-- Content (Markdown) -->
      <div class="card">
        <MarkdownView :content="policy.content" />
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import MarkdownView from '@/components/MarkdownView.vue'
import { policyApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const isManager = auth.isManager
const isAdmin = auth.isAdmin

const policy = ref(null)
const loading = ref(true)

async function loadPolicy() {
  try {
    const res = await policyApi.get(route.params.id)
    policy.value = res.data
  } finally {
    loading.value = false
  }
}

async function acknowledge() {
  await policyApi.acknowledge(route.params.id)
  policy.value.acknowledgedByMe = true
  policy.value.acknowledgmentCount++
}

async function deletePolicy() {
  if (!confirm('정책을 삭제하시겠습니까?')) return
  try {
    await policyApi.delete(route.params.id)
    router.push('/policies')
  } catch (e) {
    alert(typeof e === 'string' ? e : '삭제에 실패했습니다')
  }
}

function statusBadgeClass(status) {
  return { DRAFT: 'badge-gray', REVIEW: 'badge-yellow', PUBLISHED: 'badge-green', ARCHIVED: 'badge-gray' }[status] || 'badge-gray'
}

function formatDate(dt) {
  return dt ? new Date(dt).toLocaleDateString('ko-KR') : '-'
}

onMounted(loadPolicy)
</script>
