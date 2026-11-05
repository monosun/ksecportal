// 보안정책관리 화면들이 공유하는 분류·개정구분 라벨과 첨부 제한 (백엔드 PolicyDocService 와 맞춘다)

export const MENU_KEY = 'policy_management'

export const CATEGORIES = [
  { value: 'REGULATION', label: '규정',   badge: 'bg-purple-100 text-purple-700' },
  { value: 'GUIDELINE',  label: '지침',   badge: 'bg-blue-100 text-blue-700' },
  { value: 'GUIDE',      label: '가이드', badge: 'bg-teal-100 text-teal-700' },
  { value: 'PROCEDURE',  label: '절차서', badge: 'bg-amber-100 text-amber-700' },
  { value: 'MANUAL',     label: '매뉴얼', badge: 'bg-indigo-100 text-indigo-700' },
]

export const REVISION_TYPES = [
  { value: 'ENACTMENT', label: '제정', badge: 'badge-green' },
  { value: 'AMENDMENT', label: '개정', badge: 'badge-blue' },
  { value: 'ABOLITION', label: '폐지', badge: 'badge-red' },
]

const byValue = (list) => Object.fromEntries(list.map(i => [i.value, i]))
const CATEGORY_MAP = byValue(CATEGORIES)
const REVISION_MAP = byValue(REVISION_TYPES)

export const categoryLabel = (v) => CATEGORY_MAP[v]?.label || v || '-'
export const categoryBadge = (v) => CATEGORY_MAP[v]?.badge || 'bg-gray-100 text-gray-600'
export const revisionLabel = (v) => REVISION_MAP[v]?.label || v || '-'
export const revisionBadge = (v) => REVISION_MAP[v]?.badge || 'badge-gray'

export const MAX_UPLOAD_MB = 100
export const ALLOWED_EXTS = ['.pdf', '.doc', '.docx', '.odt', '.rtf', '.ppt', '.pptx', '.xls', '.xlsx', '.txt', '.hwp', '.hwpx']
export const ACCEPT = ALLOWED_EXTS.join(',')
export const UPLOAD_HINT = `PDF·DOC(X)·PPT(X)·XLS(X)·TXT·HWP(X) (최대 ${MAX_UPLOAD_MB}MB) — 뷰어로 보려면 PDF·DOCX 권장, HWP 는 다운로드로 확인`

/** 확장자·용량을 업로드 전에 확인한다. 통과하면 빈 문자열. */
export function validateFile(file) {
  if (!file) return ''
  const name = file.name || ''
  const dot = name.lastIndexOf('.')
  const ext = dot > 0 ? name.slice(dot).toLowerCase() : ''
  if (!ALLOWED_EXTS.includes(ext)) return `정책 문서로 등록할 수 없는 형식입니다: ${ext || '(확장자 없음)'}`
  if (file.size > MAX_UPLOAD_MB * 1024 * 1024) {
    return `파일 용량이 ${MAX_UPLOAD_MB}MB를 초과합니다. (현재 ${(file.size / 1024 / 1024).toFixed(1)}MB)`
  }
  return ''
}

export function formatSize(bytes) {
  if (bytes == null) return ''
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

export const today = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/**
 * 서버(@menuAuth)와 같은 기준의 쓰기·삭제 가능 여부.
 * 기본 역할 권한 행이 없어 fullAccess 로 폴백된 경우 서버는 ADMIN·MANAGER 만 허용하므로 화면도 맞춘다.
 */
export function usePolicyDocPermission(auth) {
  const allow = (fn) => auth.isAdmin || (auth.fullAccess ? auth.isManager : fn(MENU_KEY))
  return {
    canWrite: () => allow(auth.canWrite),
    canDelete: () => allow(auth.canDelete),
  }
}
