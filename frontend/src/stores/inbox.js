import { defineStore } from 'pinia'
import { ref } from 'vue'
import { inboxApi } from '@/api'

export const useInboxStore = defineStore('inbox', () => {
  const unreadCount = ref(0)
  let pollTimer = null

  async function fetchUnread() {
    try {
      const res = await inboxApi.unreadCount()
      unreadCount.value = res.data?.count ?? 0
    } catch {
      // silent — not critical
    }
  }

  function startPolling(intervalMs = 30000) {
    fetchUnread()
    if (pollTimer) clearInterval(pollTimer)
    pollTimer = setInterval(fetchUnread, intervalMs)
  }

  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }

  return { unreadCount, fetchUnread, startPolling, stopPolling }
})
