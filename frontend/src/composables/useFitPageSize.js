import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useDebounceFn } from '@vueuse/core'

/**
 * 목록 한 페이지 건수를 **모니터 화면 높이에 맞춰** 정한다.
 *
 * <p>페이지 크기를 20건으로 고정하면 큰 모니터에서는 표 아래가 비고, 노트북에서는 목록이
 * 화면 밖으로 잘려 스크롤해야 한다. 표가 시작되는 위치부터 창 아래까지 남는 높이를 재서
 * 한 화면에 들어가는 만큼만 불러온다.
 *
 * <p>행 높이·머리글 높이는 화면마다 다르므로 **실제로 그려진 DOM 을 재서** 쓴다.
 * 아직 아무것도 그려지지 않은 최초 조회에는 옵션의 추정치를 쓴다.
 *
 * @example
 * const { listEl, pageSize } = useFitPageSize({
 *   onChange: (size, prev) => { page.value = keepFirstRow(page.value, prev, size); load() }
 * })
 * // 템플릿: <div ref="listEl" class="card"> … <table> … </table></div>
 */
/**
 * 화면 높이만으로 행 수를 어림잡는다 — 표를 아직 그리지 않았거나,
 * 사용자가 페이지 크기를 직접 고르는 화면의 **기본값**을 정할 때 쓴다.
 */
export function estimateRows({ rowHeight = 57, headHeight = 45, footerHeight = 96, top = 330, min = 8, max = 60 } = {}) {
  const rows = Math.floor((window.innerHeight - top - headHeight - footerHeight) / rowHeight)
  return Math.min(max, Math.max(min, rows))
}

export function useFitPageSize({
  onChange = null,
  rowHeight = 57,      // 행 높이 추정치 (px-6 py-4 + text-sm 한 줄)
  rowSelector = 'tbody tr',  // 실측할 행 — 표가 아닌 카드 목록이면 카드 선택자를 준다
  headHeight = 45,     // 표 머리글 추정치
  footerHeight = 96,   // 페이지 버튼 + 아래 여백
  fallbackTop = 330,   // 표 위치를 아직 못 잰 최초 조회용 추정치
  min = 8,
  max = 60,
  initial = 20
} = {}) {
  const listEl = ref(null)
  const pageSize = ref(initial)

  /** 지금 화면에 들어가는 행 수 */
  function fit() {
    const el = listEl.value
    const top = el?.getBoundingClientRect().top ?? fallbackTop
    const head = el?.querySelector('thead')?.getBoundingClientRect().height ?? headHeight
    // 이미 그려진 행이 있으면 그 높이가 가장 정확하다(행마다 줄 수·여백이 다르다).
    const row = el?.querySelector(rowSelector)?.getBoundingClientRect().height || rowHeight
    const rows = Math.floor((window.innerHeight - top - head - footerHeight) / row)
    return Math.min(max, Math.max(min, rows))
  }

  function apply() {
    const next = fit()
    if (next === pageSize.value) return false
    const prev = pageSize.value
    pageSize.value = next
    onChange?.(next, prev)
    return true
  }

  // 최초 조회 전에 추정치로 한 번 잡아 둔다. 이 훅은 화면의 onMounted 보다 먼저 실행되므로
  // 화면이 목록을 불러올 때 이미 화면 크기에 맞는 값이 들어가 있다.
  pageSize.value = fit()

  const onResize = useDebounceFn(apply, 250)

  // 첫 조회 결과가 그려진 뒤 실제 행 높이로 한 번만 다시 맞춘다.
  // (추정치가 맞았다면 값이 그대로라 재조회도 일어나지 않는다)
  let refined = false
  async function refine() {
    if (refined) return
    refined = true
    await nextTick()
    apply()
  }

  onMounted(() => window.addEventListener('resize', onResize))
  onBeforeUnmount(() => window.removeEventListener('resize', onResize))

  return { listEl, pageSize, fit, apply, refine }
}

/**
 * 페이지 크기가 바뀌어도 **보고 있던 첫 행**이 화면에 남도록 새 페이지 번호를 계산한다.
 * 0-based 페이지 기준이며, 1-based 화면은 호출 쪽에서 ±1 한다.
 */
export function keepFirstRow(page, prevSize, nextSize) {
  return Math.max(0, Math.floor((page * prevSize) / nextSize))
}
