// ISMS-P 증적 유효기간 표시 공통 함수
// 유효기간은 항목 속성(코드관리 > ISMS-P 101항목)이며, 만료일은 서버가 증적 기준일 + 유효기간으로 계산한다.

/** 코드관리에서 고를 수 있는 유효기간(개월). null = 미설정 */
export const VALIDITY_OPTIONS = [
  { value: null, label: '미설정' },
  { value: 1, label: '1개월 (매월)' },
  { value: 3, label: '3개월 (분기)' },
  { value: 6, label: '6개월 (반기)' },
  { value: 12, label: '1년' },
  { value: 24, label: '2년' },
  { value: 36, label: '3년' },
]

/** 만료 임박으로 표시할 남은 일수 */
export const EXPIRY_WARN_DAYS = 30

export function validityLabel(months) {
  if (!months) return '미설정'
  if (months % 12 === 0) return `${months / 12}년`
  return `${months}개월`
}

function daysUntil(dateStr) {
  const [y, m, d] = String(dateStr).slice(0, 10).split('-').map(Number)
  const target = new Date(y, m - 1, d)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return Math.round((target - today) / 86400000)
}

/**
 * 만료일 상태 — { key: 'expired' | 'soon' | 'valid', text, cls }
 * 만료일이 없으면 null (유효기간 미설정 또는 증적 없음)
 */
export function expiryState(dateStr) {
  if (!dateStr) return null
  const days = daysUntil(dateStr)
  if (days < 0) {
    return { key: 'expired', text: '만료', cls: 'bg-red-50 text-red-700 border-red-200' }
  }
  if (days <= EXPIRY_WARN_DAYS) {
    return { key: 'soon', text: days === 0 ? '오늘 만료' : `D-${days}`, cls: 'bg-amber-50 text-amber-700 border-amber-200' }
  }
  return { key: 'valid', text: '유효', cls: 'bg-green-50 text-green-700 border-green-200' }
}

export function formatYmd(dateStr) {
  return dateStr ? String(dateStr).slice(0, 10) : ''
}
