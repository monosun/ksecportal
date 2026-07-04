import { ref, computed, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const WARNING_SECONDS = 120   // 2분 전 경고

export function useSessionTimer() {
  const auth = useAuthStore()
  const router = useRouter()

  const secondsLeft = ref(0)
  const showWarning = ref(false)
  const extending = ref(false)

  let tickInterval = null

  const formattedTime = computed(() => {
    const s = Math.max(0, secondsLeft.value)
    const m = Math.floor(s / 60)
    const sec = s % 60
    return `${m}:${String(sec).padStart(2, '0')}`
  })

  function calcSecondsLeft() {
    if (!auth.tokenExpiresAt) return 0
    return Math.max(0, Math.floor((auth.tokenExpiresAt - Date.now()) / 1000))
  }

  function tick() {
    if (!auth.isAuthenticated) {
      stop()
      return
    }
    secondsLeft.value = calcSecondsLeft()
    if (secondsLeft.value <= 0) {
      stop()
      showWarning.value = false
      auth.logout()
      router.push('/login')
      return
    }
    showWarning.value = secondsLeft.value <= WARNING_SECONDS
  }

  function start() {
    stop()
    secondsLeft.value = calcSecondsLeft()
    tickInterval = setInterval(tick, 1000)
  }

  function stop() {
    if (tickInterval) {
      clearInterval(tickInterval)
      tickInterval = null
    }
  }

  async function extend() {
    if (extending.value) return
    extending.value = true
    try {
      await auth.refreshToken()
      showWarning.value = false
      secondsLeft.value = calcSecondsLeft()
    } catch {
      // refresh 실패 시 로그아웃
      stop()
      auth.logout()
      router.push('/login')
    } finally {
      extending.value = false
    }
  }

  function logout() {
    stop()
    showWarning.value = false
    auth.logout()
    router.push('/login')
  }

  onUnmounted(stop)

  return { secondsLeft, formattedTime, showWarning, extending, start, stop, extend, logout }
}
