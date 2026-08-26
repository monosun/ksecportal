import { errorLogApi } from '@/api'

/**
 * 화면(JS)에서 발생한 오류를 서버(관리 > 에러 로그 관리)로 보낸다.
 * 오류 보고 자체가 사용자 흐름을 방해하면 안 되므로 모든 실패는 조용히 넘긴다.
 */

// 한 세션에서 너무 많이 보내지 않도록 제한한다(오류가 반복되는 화면 대비)
const MAX_PER_SESSION = 20
// 브라우저가 만들어내는 잡음성 오류는 걸러낸다
const IGNORED = [
  'ResizeObserver loop',
  'Script error.',
  'Non-Error promise rejection captured',
]

const seen = new Set()
let sentCount = 0

function report({ name, message, stack }) {
  try {
    if (sentCount >= MAX_PER_SESSION) return
    const text = String(message ?? '')
    if (!text && !stack) return
    if (IGNORED.some(pattern => text.includes(pattern))) return
    // 로그인 전에는 보낼 권한이 없다
    if (!localStorage.getItem('token')) return

    const key = `${name}|${text}|${String(stack ?? '').slice(0, 200)}`
    if (seen.has(key)) return
    seen.add(key)
    sentCount++

    errorLogApi.report({
      name: name || 'Error',
      message: text.slice(0, 4000),
      stack: String(stack ?? '').slice(0, 20000),
      url: window.location.pathname + window.location.search,
    }).catch(() => {})
  } catch {
    // 보고 실패는 무시
  }
}

export function setupErrorReporting(app) {
  // Vue 컴포넌트 렌더링·이벤트 처리 중 발생한 오류
  app.config.errorHandler = (err, instance, info) => {
    console.error(err)
    report({
      name: err?.name || 'VueError',
      message: `${err?.message ?? err} (${info})`,
      stack: err?.stack,
    })
  }

  // 전역 JS 오류
  window.addEventListener('error', event => {
    if (event?.error) {
      report({ name: event.error.name, message: event.error.message, stack: event.error.stack })
    } else {
      report({
        name: 'Error',
        message: event?.message,
        stack: `${event?.filename ?? ''}:${event?.lineno ?? ''}:${event?.colno ?? ''}`,
      })
    }
  })

  // 처리되지 않은 Promise 거부
  window.addEventListener('unhandledrejection', event => {
    const reason = event?.reason
    if (typeof reason === 'string') {
      // axios 인터셉터가 사용자 안내 문구로 바꿔 거부한 경우 — 서버에 이미 기록돼 있다
      return
    }
    report({
      name: reason?.name || 'UnhandledRejection',
      message: reason?.message ?? String(reason),
      stack: reason?.stack,
    })
  })
}
