<template>
  <div>
    <div class="page-header">
      <div>
        <h1 class="page-title">{{ $t('inbox.title') }}</h1>
      </div>
      <div class="flex items-center gap-2">
        <button v-if="messages.some(m => !m.read)" @click="handleMarkAllRead" class="btn-secondary text-sm">
          {{ $t('inbox.markAllRead') }}
        </button>
        <button v-if="messages.length" @click="showClearConfirm = true"
          class="flex items-center gap-1.5 px-3 py-2 text-sm border border-red-200 text-red-600 rounded-lg hover:bg-red-50 transition-colors">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
          </svg>
          {{ $t('inbox.clearInbox') }}
        </button>
      </div>
    </div>

    <div class="page-body max-w-3xl">
      <div v-if="loading" class="py-16 text-center text-gray-400 text-sm">{{ $t('common.loading') }}</div>

      <div v-else-if="messages.length === 0" class="py-16 text-center text-gray-400 text-sm">
        <svg class="w-12 h-12 mx-auto mb-3 text-gray-200" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
            d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0l-8 4-8-4"/>
        </svg>
        {{ $t('inbox.empty') }}
      </div>

      <div v-else class="space-y-3">
        <div
          v-for="msg in messages" :key="msg.id"
          class="card transition-all"
          :class="!msg.read ? 'ring-2 ring-primary-200' : ''">

          <div class="flex items-start gap-3">
            <!-- 타입 아이콘 -->
            <div class="flex-shrink-0 w-9 h-9 rounded-full flex items-center justify-center mt-0.5"
              :class="{
                'bg-amber-100': msg.type === 'APPROVAL_REQUEST',
                'bg-blue-100': msg.type === 'INFO',
                'bg-gray-100': msg.type === 'SYSTEM'
              }">
              <svg v-if="msg.type === 'APPROVAL_REQUEST'" class="w-4 h-4 text-amber-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
              </svg>
              <svg v-else-if="msg.type === 'INFO'" class="w-4 h-4 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
              </svg>
              <svg v-else class="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/>
              </svg>
            </div>

            <!-- 내용 -->
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 flex-wrap">
                <span class="text-xs font-semibold px-2 py-0.5 rounded-full"
                  :class="{
                    'bg-amber-100 text-amber-700': msg.type === 'APPROVAL_REQUEST',
                    'bg-blue-100 text-blue-700': msg.type === 'INFO',
                    'bg-gray-100 text-gray-600': msg.type === 'SYSTEM'
                  }">
                  {{ typeLabel(msg.type) }}
                </span>
                <span v-if="!msg.read"
                  class="text-xs font-semibold px-2 py-0.5 rounded-full bg-primary-100 text-primary-700">
                  {{ $t('inbox.unread') }}
                </span>
                <span class="text-xs text-gray-400 ml-auto">{{ formatDate(msg.createdAt) }}</span>
              </div>
              <p class="mt-1.5 font-semibold text-gray-900 text-sm">{{ msg.title }}</p>
              <p v-if="msg.content" class="mt-1 text-sm text-gray-600">{{ msg.content }}</p>

              <!-- 승인 요청 액션 -->
              <div v-if="msg.hasAction && msg.type === 'APPROVAL_REQUEST' && msg.actionStatus === 'PENDING'"
                class="mt-3 flex items-center gap-2">
                <button @click="handleApprove(msg)" :disabled="processing[msg.id]"
                  class="btn-primary text-xs px-3 py-1.5">
                  {{ $t('inbox.approve') }}
                </button>
                <button @click="handleReject(msg)" :disabled="processing[msg.id]"
                  class="btn-secondary text-xs px-3 py-1.5 border-red-300 text-red-600 hover:bg-red-50">
                  {{ $t('inbox.reject') }}
                </button>
              </div>
              <div v-else-if="msg.hasAction && msg.actionStatus && msg.actionStatus !== 'PENDING'"
                class="mt-3 text-sm font-semibold"
                :class="msg.actionStatus === 'APPROVED' ? 'text-green-600' : 'text-red-500'">
                ✓ {{ msg.actionStatus === 'APPROVED' ? $t('inbox.approved') : $t('inbox.rejected') }}
              </div>
            </div>

            <!-- 읽음 처리 버튼 -->
            <button v-if="!msg.read" @click="handleMarkRead(msg)"
              class="flex-shrink-0 text-xs text-gray-400 hover:text-gray-700 px-2 py-1 rounded-lg hover:bg-gray-100 transition-colors mt-0.5">
              읽음
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
  <!-- 수신함 비우기 확인 모달 -->
  <div v-if="showClearConfirm" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
    <div class="bg-white rounded-xl shadow-xl w-full max-w-md p-6">
      <div class="flex items-center gap-3 mb-3">
        <div class="flex-shrink-0 w-10 h-10 bg-red-100 rounded-full flex items-center justify-center">
          <svg class="w-5 h-5 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
          </svg>
        </div>
        <h3 class="text-lg font-semibold text-gray-900">{{ $t('inbox.clearInboxConfirmTitle') }}</h3>
      </div>
      <p class="text-sm text-gray-600 mb-3">{{ $t('inbox.clearInboxConfirmMsg') }}</p>
      <p class="text-xs text-gray-500 bg-gray-50 border border-gray-200 rounded-lg px-3 py-2">
        총 <span class="font-semibold text-gray-800">{{ messages.length }}개</span>의 메시지가 삭제됩니다.
      </p>
      <div class="flex justify-end gap-3 mt-6">
        <button @click="showClearConfirm = false"
          class="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-100">
          {{ $t('common.cancel') }}
        </button>
        <button @click="handleClearInbox" :disabled="clearing"
          class="px-4 py-2 text-sm bg-red-600 text-white rounded-lg hover:bg-red-700 disabled:opacity-50 transition-colors">
          {{ clearing ? $t('common.loading') : $t('inbox.clearInboxConfirm') }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { inboxApi } from '@/api'
import { useInboxStore } from '@/stores/inbox'

const { t } = useI18n()
const inboxStore = useInboxStore()

const messages = ref([])
const loading = ref(true)
const processing = ref({})
const showClearConfirm = ref(false)
const clearing = ref(false)

onMounted(load)

async function load() {
  loading.value = true
  try {
    const res = await inboxApi.list()
    messages.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function handleMarkRead(msg) {
  await inboxApi.markRead(msg.id)
  msg.read = true
  inboxStore.fetchUnread()
}

async function handleMarkAllRead() {
  await inboxApi.markAllRead()
  messages.value.forEach(m => (m.read = true))
  inboxStore.fetchUnread()
}

async function handleClearInbox() {
  clearing.value = true
  try {
    await inboxApi.deleteAll()
    messages.value = []
    showClearConfirm.value = false
    inboxStore.fetchUnread()
  } finally {
    clearing.value = false
  }
}

async function handleApprove(msg) {
  processing.value[msg.id] = true
  try {
    await inboxApi.approve(msg.id)
    msg.actionStatus = 'APPROVED'
    msg.read = true
    inboxStore.fetchUnread()
  } catch (e) {
    alert(e || '오류가 발생했습니다.')
  } finally {
    processing.value[msg.id] = false
  }
}

async function handleReject(msg) {
  processing.value[msg.id] = true
  try {
    await inboxApi.reject(msg.id)
    msg.actionStatus = 'REJECTED'
    msg.read = true
    inboxStore.fetchUnread()
  } catch (e) {
    alert(e || '오류가 발생했습니다.')
  } finally {
    processing.value[msg.id] = false
  }
}

function typeLabel(type) {
  const map = { APPROVAL_REQUEST: t('inbox.approvalRequest'), INFO: t('inbox.info'), SYSTEM: t('inbox.system') }
  return map[type] || type
}

function formatDate(dt) {
  if (!dt) return ''
  return new Date(dt).toLocaleString('ko-KR', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}
</script>
