/** 에러 로그 관리 화면에서 공통으로 쓰는 표기 규칙 */

export const STATUS_LABEL = {
  NEW: '미확인',
  IN_PROGRESS: '확인중',
  RESOLVED: '처리완료',
  IGNORED: '무시',
}

export const SOURCE_LABEL = {
  BACKEND: '서버',
  FRONTEND: '화면',
  SCHEDULER: '스케줄러',
}

export const LEVEL_LABEL = {
  ERROR: '오류',
  WARN: '경고',
}

export function levelClass(level) {
  return level === 'WARN' ? 'bg-amber-100 text-amber-700' : 'bg-red-100 text-red-700'
}

export function statusClass(status) {
  switch (status) {
    case 'NEW': return 'bg-red-50 text-red-600 border border-red-200'
    case 'IN_PROGRESS': return 'bg-blue-50 text-blue-600 border border-blue-200'
    case 'RESOLVED': return 'bg-green-50 text-green-700 border border-green-200'
    case 'IGNORED': return 'bg-gray-100 text-gray-500 border border-gray-200'
    default: return 'bg-gray-100 text-gray-600'
  }
}

export function formatDate(dt) {
  if (!dt) return '-'
  return new Date(dt).toLocaleString('ko-KR', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false,
  })
}
