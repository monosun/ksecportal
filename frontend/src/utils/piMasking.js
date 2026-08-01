/**
 * 개인정보 마스킹 규칙 구현.
 *
 * 마스킹 "방식"(부분 마스킹 / 전체 마스킹 / 암호화 저장 …)은 관리 > 코드관리 >
 * 개인정보 유형별 항목관리에서 항목별로 등록한 값을 그대로 따르고,
 * 실제로 어느 자리를 가릴지는 항목 종류(kind)별 함수가 담당한다.
 */

/** 코드관리에서 고를 수 있는 마스킹 방식 */
export const MASKING_TYPE = {
  PARTIAL: '부분 마스킹',
  FULL: '전체 마스킹',
  ENCRYPTED: '암호화 저장',
  HASHED: '일방향 암호화(해시)',
  NOT_COLLECTED: '미수집/즉시 파기',
  NONE: '마스킹 불필요',
}

const HIDDEN = '(비노출)'

const CORP_HINTS = ['주식회사', '(주)', '㈜', '유한회사', '법인', 'Corp', 'Inc', 'Ltd', 'LLC']

function stars(n) {
  return '*'.repeat(Math.max(n, 0))
}

/** 성명 — 가운데 글자를 가리고 2자리는 마지막 글자를 가린다. 법인명은 마스킹하지 않는다. */
export function maskName(v) {
  const s = String(v)
  if (CORP_HINTS.some(h => s.includes(h))) return s
  // 영문 이름처럼 공백으로 나뉜 경우 토큰별로 처리한다
  if (s.includes(' ')) return s.split(/\s+/).map(maskNameToken).join(' ')
  return maskNameToken(s)
}

function maskNameToken(s) {
  if (s.length <= 1) return s
  if (s.length === 2) return s[0] + '*'
  return s[0] + stars(s.length - 2) + s[s.length - 1]
}

/** 생년월일 — 연도만 남긴다 */
export function maskBirth(v) {
  const s = String(v)
  const dashed = s.match(/^(\d{4})[-./](\d{1,2})[-./](\d{1,2})/)
  if (dashed) return `${dashed[1]}-**-**`
  if (/^\d{8}$/.test(s)) return `${s.slice(0, 4)}****`
  if (/^\d{6}$/.test(s)) return `${s.slice(0, 2)}****`
  return maskGeneric(s)
}

/** 주민등록번호·외국인등록번호 — 생년월일 6자리만 남긴다 */
export function maskRrn(v) {
  const s = String(v)
  const m = s.match(/^(\d{6})-?(\d{6,7})$/)
  if (m) return `${m[1]}-${stars(m[2].length)}`
  return maskGeneric(s)
}

/** 여권번호 — 앞 2자리만 남긴다 */
export function maskPassport(v) {
  const s = String(v)
  return s.length <= 2 ? stars(s.length) : s.slice(0, 2) + stars(s.length - 2)
}

/** 운전면허번호 — 지역코드 2자리만 남긴다 */
export function maskDriverLicense(v) {
  const s = String(v)
  return s.replace(/^(\d{2})(.*)$/, (_, head, rest) => head + rest.replace(/[A-Za-z0-9]/g, '*'))
}

/** 사업자등록번호 — 앞 3자리만 남긴다 */
export function maskBizNo(v) {
  const s = String(v)
  const m = s.match(/^(\d{3})-?(\d{2})-?(\d{5})$/)
  if (m) return `${m[1]}-**-*****`
  return s.length <= 3 ? s : s.slice(0, 3) + s.slice(3).replace(/[A-Za-z0-9]/g, '*')
}

/** 이동전화번호 — 국번 4자리를 가린다 */
export function maskPhone(v) {
  // 국가번호(+82)는 국내 표기로 바꿔서 판단한다
  const s = String(v).trim().replace(/^\+82[-.\s]?/, '0')
  const digits = s.replace(/\D/g, '')
  if (digits.length < 7) return maskGeneric(s)

  // 구분자가 있으면 가운데 그룹만 가린다
  const m = s.match(/^(\d{2,4})([-.\s])(\d{3,4})\2(\d{4})$/)
  if (m) return `${m[1]}${m[2]}${stars(m[3].length)}${m[2]}${m[4]}`

  // 숫자만 있으면 국번(가운데)을 가린다 — 02 지역번호는 앞 2자리만 남긴다
  const headLen = digits.startsWith('02') ? 2 : 3
  return digits.slice(0, headLen) + stars(digits.length - headLen - 4) + digits.slice(-4)
}

/** 유선전화번호 — 국번을 가린다 */
export function maskLandline(v) {
  return maskPhone(v)
}

/** 이메일 — 아이디 앞 3자리만 남기고 도메인은 표시한다 */
export function maskEmail(v) {
  const s = String(v).trim()
  const at = s.lastIndexOf('@')
  if (at < 1) return maskGeneric(s)
  const id = s.slice(0, at)
  const domain = s.slice(at)
  const keep = id.length <= 3 ? 1 : 3
  return id.slice(0, keep) + stars(Math.max(id.length - keep, 3)) + domain
}

/** 연락처 — 값 모양을 보고 이메일/전화번호 규칙을 고른다 */
export function maskContact(v) {
  return String(v).includes('@') ? maskEmail(v) : maskPhone(v)
}

/** 주소 — 읍·면·동까지만 표시하고 상세주소를 가린다 */
export function maskAddress(v) {
  const parts = String(v).trim().split(/\s+/)
  if (parts.length <= 1) return maskGeneric(v)
  let keep = parts.findIndex(p => /[동읍면리가로길]$|[동읍면리]\d*$/.test(p))
  keep = keep >= 0 ? keep + 1 : Math.min(2, parts.length - 1)
  return [...parts.slice(0, keep), '***'].join(' ')
}

/** 우편번호 — 앞 2자리만 남긴다 */
export function maskPostal(v) {
  const s = String(v)
  return s.length <= 2 ? s : s.slice(0, 2) + stars(s.length - 2)
}

/** 계좌번호 — 앞 3자리·뒤 3자리만 남긴다 */
export function maskAccount(v) {
  const s = String(v)
  if (s.replace(/\D/g, '').length <= 6) return stars(s.length)
  return s.slice(0, 3) + s.slice(3, -3).replace(/[A-Za-z0-9]/g, '*') + s.slice(-3)
}

/** 카드번호 — 앞 6자리·뒤 4자리만 남긴다(PCI-DSS) */
export function maskCard(v) {
  const s = String(v)
  if (s.replace(/\D/g, '').length <= 10) return stars(s.length)
  return s.slice(0, 6) + s.slice(6, -4).replace(/[A-Za-z0-9]/g, '*') + s.slice(-4)
}

/** IP 주소 — 뒤 2옥텟을 가린다 */
export function maskIp(v) {
  const s = String(v).trim()
  const v4 = s.match(/^(\d{1,3})\.(\d{1,3})\.\d{1,3}\.\d{1,3}$/)
  if (v4) return `${v4[1]}.${v4[2]}.*.*`
  if (s.includes(':')) {
    const g = s.split(':')
    return g.slice(0, 2).join(':') + ':*'
  }
  return maskGeneric(s)
}

/** 단말식별번호(IMEI·USIM·Device ID) — 뒤 4자리만 남긴다 */
export function maskDeviceId(v) {
  const s = String(v)
  return s.length <= 4 ? stars(s.length) : stars(s.length - 4) + s.slice(-4)
}

/** MAC·AP 식별자 — 앞 3옥텟(OUI)만 남긴다 */
export function maskMac(v) {
  const s = String(v)
  const parts = s.split(/[:-]/)
  if (parts.length < 4) return maskGeneric(s)
  const sep = s.includes(':') ? ':' : '-'
  return [...parts.slice(0, 3), ...parts.slice(3).map(p => stars(p.length))].join(sep)
}

/** 좌표 — 소수점 2자리까지만 표시한다 */
export function maskCoords(v) {
  return String(v).replace(/-?\d+\.\d+/g, m => Number(m).toFixed(2))
}

/** 해시·토큰 — 앞 4자리만 남긴다 */
export function maskHead4(v) {
  const s = String(v)
  return s.length <= 4 ? stars(s.length) : s.slice(0, 4) + '****'
}

/** 종류를 특정할 수 없는 값 — 앞 1자리만 남긴다 */
export function maskGeneric(v) {
  const s = String(v)
  if (s.length <= 1) return '*'
  return s[0] + stars(Math.min(s.length - 1, 8))
}

/** 항목 종류(kind) → 마스킹 함수 */
export const MASKERS = {
  name: maskName,
  birth: maskBirth,
  rrn: maskRrn,
  passport: maskPassport,
  driverLicense: maskDriverLicense,
  bizNo: maskBizNo,
  phone: maskPhone,
  landline: maskLandline,
  email: maskEmail,
  contact: maskContact,
  address: maskAddress,
  postal: maskPostal,
  account: maskAccount,
  card: maskCard,
  ip: maskIp,
  deviceId: maskDeviceId,
  mac: maskMac,
  coords: maskCoords,
  token: maskHead4,
  generic: maskGeneric,
}

/**
 * 코드관리 항목명(label) → 항목 종류(kind).
 * 관리자가 항목을 새로 추가해도 이름에 포함된 키워드로 규칙을 찾아낸다.
 */
const KIND_KEYWORDS = [
  { kind: 'rrn', kw: ['주민등록번호', '외국인등록번호'] },
  { kind: 'passport', kw: ['여권번호'] },
  { kind: 'driverLicense', kw: ['운전면허'] },
  { kind: 'bizNo', kw: ['사업자등록번호'] },
  { kind: 'birth', kw: ['생년월일', '발급일자'] },
  { kind: 'email', kw: ['이메일'] },
  { kind: 'landline', kw: ['유선전화'] },
  { kind: 'phone', kw: ['전화번호', '연락처', '회선번호', '발신번호', '수신번호', '휴대전화'] },
  { kind: 'postal', kw: ['우편번호'] },
  { kind: 'address', kw: ['주소'] },
  { kind: 'card', kw: ['카드번호'] },
  { kind: 'account', kw: ['계좌번호'] },
  { kind: 'ip', kw: ['IP 주소', '접속로그'] },
  { kind: 'mac', kw: ['MAC', 'AP 정보'] },
  { kind: 'deviceId', kw: ['IMEI', 'USIM', 'eSIM', 'Device ID', '단말식별번호', 'RFID'] },
  { kind: 'coords', kw: ['위치정보', 'GPS', '기지국'] },
  { kind: 'token', kw: ['토큰', '쿠키', '연계정보', 'CI'] },
  { kind: 'name', kw: ['성명', '이름', '예금주', '납부자', '대표자'] },
]

export function kindOfLabel(label) {
  const s = String(label || '')
  for (const { kind, kw } of KIND_KEYWORDS) {
    if (kw.some(k => s.toUpperCase().includes(k.toUpperCase()))) return kind
  }
  return 'generic'
}

/**
 * 값 하나를 마스킹한다.
 *
 * @param {*} value 원본 값
 * @param {string} maskingType 코드관리에 등록된 마스킹 방식
 * @param {string} kind 항목 종류 (MASKERS 의 키)
 */
export function maskValue(value, maskingType, kind = 'generic') {
  if (value === null || value === undefined || value === '') return value
  const s = String(value)
  if (!s.trim() || s === '—' || s === '-') return value

  switch (maskingType) {
    case MASKING_TYPE.NONE:
      return value
    case MASKING_TYPE.FULL:
    case MASKING_TYPE.NOT_COLLECTED:
      return HIDDEN
    case MASKING_TYPE.HASHED:
      return maskHead4(s)
    default:
      // 부분 마스킹 · 암호화 저장 · 기준 미지정 — 항목 종류별 규칙을 적용한다
      return (MASKERS[kind] || maskGeneric)(s)
  }
}
