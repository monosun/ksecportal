// 공격 탐지 화면 공용 라벨·색상

export const CATEGORY_LABEL = {
  HONEYPOT_SCAN: '공격 도구 경로 탐색',
  HONEYPOT_BAIT: '미끼 경로 접근',
  LLM_AGENT: 'LLM 에이전트 확인',
  HONEYTOKEN: '허니토큰 사용',
  HONEY_ACCOUNT: '미끼 계정 로그인',
  BEHAVIOR: '자동화 행위',
  LOGIN_ATTACK: '로그인 공격',
  TEST: '테스트'
}

export const CATEGORY_HINT = {
  HONEYPOT_SCAN: '.env·.git·wp-admin 등 공격 도구가 흔히 찔러 보는 경로 접근',
  HONEYPOT_BAIT: 'robots.txt·숨김 링크로만 알 수 있는 미끼 경로 접근(HTML 파싱 자동화)',
  LLM_AGENT: '미끼 페이지의 AI 에이전트용 지시문을 그대로 따른 요청',
  HONEYTOKEN: '미끼에만 노출한 가짜 API 키 사용',
  HONEY_ACCOUNT: '존재하지 않는 미끼 계정으로 로그인 시도',
  BEHAVIOR: '속도·간격 균일성·404/403 열거·즉시 재시도·도구 UA 점수 초과',
  LOGIN_ATTACK: '한 IP 에서 여러 계정·다수 로그인 실패(크리덴셜 스터핑·무차별 대입)',
  TEST: '알림 경로 점검용'
}

export const SEVERITY_LABEL = {
  CRITICAL: '심각', HIGH: '높음', MEDIUM: '중간', LOW: '낮음', INFO: '정보'
}

export const STATUS_LABEL = {
  NEW: '신규', ACKNOWLEDGED: '확인 중', FALSE_POSITIVE: '오탐', RESOLVED: '조치 완료', ESCALATED: '침해사고 전환'
}

export function severityClass(s) {
  return {
    CRITICAL: 'bg-red-600 text-white',
    HIGH: 'bg-orange-100 text-orange-700',
    MEDIUM: 'bg-yellow-100 text-yellow-800',
    LOW: 'bg-blue-50 text-blue-700',
    INFO: 'bg-gray-100 text-gray-600'
  }[s] || 'bg-gray-100 text-gray-600'
}

export function statusClass(s) {
  return {
    NEW: 'bg-red-50 text-red-700',
    ACKNOWLEDGED: 'bg-yellow-50 text-yellow-700',
    FALSE_POSITIVE: 'bg-gray-100 text-gray-500',
    RESOLVED: 'bg-green-50 text-green-700',
    ESCALATED: 'bg-purple-50 text-purple-700'
  }[s] || 'bg-gray-100 text-gray-600'
}

export function scoreClass(score, threshold) {
  if (score >= threshold * 2) return 'text-red-600'
  if (score >= threshold) return 'text-orange-600'
  if (score >= threshold / 2) return 'text-yellow-600'
  return 'text-gray-500'
}

export function formatDate(v) {
  if (!v) return '-'
  const d = new Date(v)
  if (isNaN(d)) return v
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}
