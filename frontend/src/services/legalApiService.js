// 법제처 Open API 프록시 — 백엔드(/api/law-proxy)를 통해 law.go.kr 조문 조회
// API 키는 설정관리 > API 연동 탭에서 등록 (DB 저장)

import axios from 'axios'

const PROXY = '/api/law-proxy'

// JWT 인증 헤더 — api/index.js의 커스텀 인스턴스와 동일한 토큰 사용
// (전역 axios에는 인터셉터가 없어 헤더 없이 호출하면 401 → 정적 데이터 폴백됨)
function authHeaders() {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

// ── localStorage 키 (설정관리에서 DB 저장 여부 표시용) ──
const STORAGE_KEY = 'lawApiKeySet'
export function getLawApiKeySet()  { return localStorage.getItem(STORAGE_KEY) === 'true' }
export function setLawApiKeySet(v) { localStorage.setItem(STORAGE_KEY, v ? 'true' : 'false') }

// null/undefined/string/array → array
function toArr(val) {
  if (!val) return []
  return Array.isArray(val) ? val : [val]
}

// 법령명 정규화: 공백·영숫자 괄호·가운뎃점(·ㆍ) 제거
// law.go.kr은 'ㆍ'(U+318D), 데이터는 '·'(U+00B7)를 쓰는 경우가 있어 둘 다 제거
function norm(s) {
  return (s || '').replace(/\s+/g, '').replace(/[·ㆍ・‧]/g, '').replace(/\([^가-힣)]*\)/g, '').trim()
}
// 강화 정규화: 모든 괄호 내용 제거
function normFull(s) {
  return (s || '').replace(/\s+/g, '').replace(/[·ㆍ・‧]/g, '').replace(/\([^)]*\)/g, '').trim()
}

// 중첩 배열/문자열 → 하나의 문자열 (API가 배열로 주는 부칙내용·조문내용 대응)
function flatText(v, sep = ' ') {
  if (v == null) return ''
  if (Array.isArray(v)) return v.map(x => flatText(x, sep)).filter(Boolean).join(sep)
  return String(v)
}

// "제N조(제목) 본문..." → "본문..." (조 헤더 제거)
function extractIntro(rawContent) {
  const 조문내용 = flatText(rawContent)
  if (!조문내용) return ''
  const m = 조문내용.match(/^제[\d의]+조(?:\([^)]+\))?\s+(.+)/s)
  if (m) return m[1].trim()
  if (/^제[\d의]+조(?:\([^)]+\))?$/.test(조문내용.trim())) return ''
  return 조문내용.trim()
}

// ① ② ... ⑳ 집합
const CIRC = new Set([...'①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳'])
const startsWithCirc = s => s && CIRC.has(s.trim()[0])

// 단일 조문단위 → { no, title, clauses }
function parseArticleUnit(조문) {
  const rawNo = 조문.조문번호 || ''
  const 가지  = 조문.조문가지번호 || ''
  const no    = rawNo ? (가지 ? `제${rawNo}조의${가지}` : `제${rawNo}조`) : ''
  const title = 조문.조문제목 || ''
  const clauses = []
  const intro   = extractIntro(조문.조문내용)
  const 항      = 조문.항

  if (!항) {
    if (intro) clauses.push(intro)
  } else {
    const 항배열  = toArr(항)
    const isVirtual = 항배열.every(h => !h.항번호)

    if (isVirtual) {
      if (intro) clauses.push(intro)
      for (const h of 항배열) {
        const hBody = (h.항내용 || '').trim()
        if (hBody && hBody !== intro && !startsWithCirc(hBody)) clauses.push(hBody)
        for (const 호 of toArr(h.호)) {
          if (호.호내용) clauses.push(호.호내용)
          for (const 목 of toArr(호.목)) {
            if (목.목내용) clauses.push(목.목내용)
          }
        }
      }
    } else {
      for (const h of 항배열) {
        if (!h.항번호) continue
        if (h.항내용) clauses.push(h.항내용)
        for (const 호 of toArr(h.호)) {
          if (호.호내용) clauses.push(호.호내용)
          for (const 목 of toArr(호.목)) {
            if (목.목내용) clauses.push(목.목내용)
          }
        }
      }
    }
  }

  return { no, title, clauses }
}

// 조문단위 배열 재귀 파싱 (법령·행정규칙 공통)
// 일부 법령(시행령 등)은 장/절 헤더(전문) 안에 조문을 중첩해서 반환 → 재귀 처리
function parseArticleUnits(조문목록) {
  const articles = []
  for (const 조문 of 조문목록) {
    const 여부 = 조문.조문여부

    if (여부 === '전문') {
      const content = flatText(조문.조문내용).trim()
      const m = content.match(/^(제\d+[장편절관])\s*(.*)$/)
      if (m) articles.push({ no: m[1], title: m[2].trim(), clauses: [] })
      else if (content) articles.push({ no: '', title: content, clauses: [] })

      // ★ 핵심 수정: 장/절 헤더 안에 중첩된 조문단위 재귀 처리
      // (개인정보보호법 시행령 등 제1조·제2조가 제1장 전문 아래에 중첩되는 경우)
      const nested = toArr(조문.조문단위)
      if (nested.length > 0) articles.push(...parseArticleUnits(nested))
      continue
    }

    if (여부 === '조문') {
      articles.push(parseArticleUnit(조문))
      continue
    }

    if (여부 === '부칙') {
      if (조문.조문번호) {
        articles.push(parseArticleUnit(조문))
      } else {
        const content = flatText(조문.조문내용).trim()
        if (content) articles.push({ no: '부칙', title: content.replace(/^부칙\s*/, ''), clauses: [] })
      }
      // 부칙 내 중첩 조문도 재귀 처리
      const nested = toArr(조문.조문단위)
      if (nested.length > 0) articles.push(...parseArticleUnits(nested))
      continue
    }

    // 조문여부 미설정 — 조문번호가 있으면 일반 조문, 없으면 중첩 조문단위 재귀
    if (!여부) {
      if (조문.조문번호) {
        articles.push(parseArticleUnit(조문))
      }
      const nested = toArr(조문.조문단위)
      if (nested.length > 0) articles.push(...parseArticleUnits(nested))
    }
  }
  return articles
}

// API 응답 루트 → { no, title, clauses }[] 파싱 (법령·행정규칙 양쪽 지원)
function parseArticles(root) {
  if (!root) return []
  const articles = []

  // 법령: root.조문.조문단위 / 행정규칙: root.본문.조문.조문단위
  const primary   = toArr(root?.조문?.조문단위)
  const secondary = toArr(root?.본문?.조문?.조문단위)
  const 조문목록 = primary.length > 0 ? primary : secondary

  articles.push(...parseArticleUnits(조문목록))

  // 별도 부칙 섹션 (부칙내용이 중첩 배열로 오는 경우가 있어 flatText 처리 —
  //  배열에 .trim() 호출 시 예외 → 정적 데이터 폴백으로 시행령 조문이 일부만 보이던 문제)
  const 부칙목록 = toArr(root?.부칙?.부칙단위)
  for (const 부칙 of 부칙목록) {
    const 부칙내용 = flatText(부칙.부칙내용).trim()
    if (부칙내용) articles.push({ no: '부칙', title: 부칙내용.replace(/^부칙\s*/, ''), clauses: [] })
    const 부칙조문 = toArr(부칙.조문?.조문단위)
    for (const 조문 of 부칙조문) {
      if (조문.조문여부 === '조문' || 조문.조문번호) {
        articles.push(parseArticleUnit(조문))
      }
    }
  }

  return articles
}

// ── 행정규칙(고시·규정·세칙) 전문 파싱 ──────────────────────────────
// AdmRulService.조문내용: 조문별 문자열 배열 또는 전체가 한 덩어리인 문자열
// "제1편 총칙제1-1조(목적) 본문...<개정 ...>제1-2조(정의) ..." 형태로 붙어서 옴

// 조문 경계에 개행 삽입: 앞 문자가 공백이 아닌 위치의 "제N조(제목)" / "제N편·장·절"
// (본문 내 인용 "법 제29조..."는 앞에 공백이 있어 분리되지 않음)
function splitAdmrulBlob(text) {
  return text
    .replace(/(?<=\S)(?=제\d+(?:-\d+)?조(?:의\d+)?\s*\([^)]{1,80}\))/g, '\n')
    // 제목 괄호가 없는 조문 (보험업감독규정 "제1-1조 이 규정은..." 형태) —
    // 문장 종결(다.)·<개정> 태그·장절 헤더(총칙 등) 직후에만 분리해 본문 내 인용과 구분
    .replace(/(?<=[.>\])」]|총칙|통칙|보칙|벌칙)(?=제\d+(?:-\d+)?조(?:의\d+)?\s)/g, '\n')
    .replace(/(?<=[.>\])」])(?=제\d+[편장절관]\s)/g, '\n')
    .replace(/(?<=\S)(?=제\d+[편장절관]\s*(?:총칙|통칙|벌칙|보칙))/g, '\n')
    .split('\n')
}

// 본문을 ①②③ 항 경계에서 분리
function splitAdmrulClauses(body) {
  const t = (body || '').trim()
  if (!t) return []
  return t.split(/(?=[①②③④⑤⑥⑦⑧⑨⑩⑪⑫⑬⑭⑮⑯⑰⑱⑲⑳])/).map(s => s.trim()).filter(Boolean)
}

function parseAdmrulArticles(admRoot) {
  const articles = []
  const texts = []
  ;(function flat(v) {
    if (v == null) return
    if (Array.isArray(v)) v.forEach(flat)
    else texts.push(String(v))
  })(admRoot?.조문내용)

  for (const raw of texts) {
    for (const seg0 of splitAdmrulBlob(raw)) {
      const seg = seg0.trim()
      if (!seg) continue
      // 편/장/절 헤더 (짧은 제목 형태만)
      const hm = seg.match(/^(제\d+[편장절관])\s*(\S{0,30})$/)
      if (hm) {
        articles.push({ no: hm[1], title: hm[2].trim(), clauses: [] })
        continue
      }
      // "제N조(제목) 본문" / "제N-N조의M(제목) 본문"
      const am = seg.match(/^(제\d+(?:-\d+)?조(?:의\d+)?)\s*\(([^)]{1,80})\)\s*([\s\S]*)$/)
      if (am) {
        articles.push({ no: am[1], title: am[2], clauses: splitAdmrulClauses(am[3]) })
        continue
      }
      // 편/장 헤더 뒤에 붙은 텍스트 등 조문 형식이 아닌 블록
      const chm = seg.match(/^(제\d+[편장절관])\s*(.*)$/)
      if (chm && chm[2].length <= 40) {
        articles.push({ no: chm[1], title: chm[2].trim(), clauses: [] })
      } else {
        articles.push({ no: '', title: seg.slice(0, 40), clauses: seg.length > 40 ? [seg] : [] })
      }
    }
  }

  // 부칙 (부칙내용: 문자열 배열)
  ;(function flatB(v) {
    if (v == null) return
    if (Array.isArray(v)) { v.forEach(flatB); return }
    const t = String(v).trim()
    if (t) articles.push({ no: '부칙', title: t.replace(/^부칙\s*/, ''), clauses: [] })
  })(admRoot?.부칙?.부칙내용)

  return articles
}

// YYYYMMDD → YYYY.MM.DD
function formatDate(str) {
  if (!str) return '-'
  const s = String(str).replace(/\D/g, '')  // 숫자만 추출 (혹시 "2023.09.28" 형태도 대응)
  if (s.length !== 8) return String(str) || '-'
  return `${s.slice(0, 4)}.${s.slice(4, 6)}.${s.slice(6, 8)}`
}

// 기본정보에서 날짜·제개정 정보 추출 (여러 경로 시도, 행정규칙 포함)
function extractDatesFromRoot(root, meta) {
  // 가능한 경로 목록
  const 기본 = root?.기본정보 ?? root?.법령기본정보 ?? root?.기본 ?? root?.행정규칙기본정보
  if (!기본) return

  // 공포일자 (행정규칙은 발령일자)
  const 공포일자 = 기본.공포일자 ?? 기본.발령일자
  if (공포일자) meta.promulgationDate = formatDate(공포일자)

  // 시행일자
  const 시행일자 = 기본.시행일자
  if (시행일자) meta.enforcementDate = formatDate(시행일자)

  // 제개정
  const 제개정구분 = 기본.제개정구분명 || 기본.제개정구분
  if (제개정구분) meta.amendType = String(제개정구분)

  const 제개정일 = 기본.제개정일자
  if (제개정일) meta.amendDate = formatDate(제개정일)

  // 공포번호 (행정규칙은 발령번호)
  const 공포번호 = 기본.공포번호 ?? 기본.발령번호
  if (공포번호) meta.lawNumber = String(공포번호)
}

// 검색 결과에서 법령명 매칭 (정규화 → 전체 정규화 → 단건 → 부분 포함 순)
function matchHit(목록, lawName) {
  return 목록.find(l => norm(l.법령명한글) === norm(lawName))
    ?? 목록.find(l => normFull(l.법령명한글) === normFull(lawName))
    ?? (목록.length === 1 ? 목록[0] : null)
    // 마지막 수단: 법령명이 포함 관계일 때 (e.g. 검색어 "개인정보보호법시행령" ↔ API "개인정보보호법시행령(시행2024.)")
    ?? 목록.find(l => normFull(l.법령명한글).includes(normFull(lawName)))
    ?? 목록.find(l => normFull(lawName).includes(normFull(l.법령명한글)))
}

// 검색 + 매칭 (target 별)
// admrul(행정규칙) 검색 응답은 AdmRulSearch.admrul + 행정규칙명/행정규칙일련번호 구조 →
// 법령 검색과 같은 필드명으로 매핑해 matchHit을 공용으로 사용
async function searchHit(lawName, target) {
  try {
    // 검색어에서 괄호 주석 제거 — "... 법률 (AML)" 형태는 그대로 검색하면 0건
    const query = lawName.replace(/\([^)]*\)/g, ' ').replace(/\s+/g, ' ').trim()
    const { data } = await axios.get(PROXY + '/search', { params: { query, target }, headers: authHeaders() })
    const 목록 = target === 'admrul'
      ? toArr(data?.AdmRulSearch?.admrul).map(r => ({
          법령명한글:   r.행정규칙명,
          법령일련번호: r.행정규칙일련번호,
          법령구분명:   r.행정규칙종류,
          소관부처명:   r.소관부처명,
          공포일자:     r.발령일자,
          시행일자:     r.시행일자,
          공포번호:     r.발령번호,
          법령링크:     r.행정규칙상세링크,
        }))
      : toArr(data?.LawSearch?.law)
    if (!목록.length) return null
    return matchHit(목록, lawName)
  } catch {
    return null
  }
}

// 전문 조회 응답 → 조문 배열 (법령/행정규칙 자동 판별)
function parseContentData(contentData) {
  if (contentData?.AdmRulService) return parseAdmrulArticles(contentData.AdmRulService)
  const root = contentData?.법령 ?? contentData?.행정규칙
  return parseArticles(root)
}

// 법령명으로 조문 전체 가져오기 (Excel 다운로드용)
export async function fetchLawByName(lawName) {
  let hit = await searchHit(lawName, 'law')
  let target = 'law'
  if (!hit) { hit = await searchHit(lawName, 'admrul'); target = 'admrul' }
  if (!hit) return null

  const mst = hit.법령일련번호
  const { data: contentData } = await axios.get(PROXY + '/content', { params: { mst, target }, headers: authHeaders() })
  return parseContentData(contentData)
}

// 법령명으로 개정 메타정보만 가져오기 (대시보드 법령 개정 위젯용)
// 전문(content) 호출 없이 검색(search) 응답만으로 공포·시행·제개정 정보를 추출 → 가볍고 빠름
export async function fetchLawMeta(lawName) {
  let hit = await searchHit(lawName, 'law')
  if (!hit) hit = await searchHit(lawName, 'admrul')
  if (!hit) return null

  const rawLink = hit.법령상세링크 || hit.법령링크 || null
  const digits = (v) => String(v ?? '').replace(/\D/g, '')
  return {
    lawName:          hit.법령명한글 || lawName,
    lawType:          hit.법령구분명 || '',
    department:       hit.소관부처명 || '',
    promulgationRaw:  digits(hit.공포일자),        // YYYYMMDD (문자열 비교·정렬용)
    promulgationDate: formatDate(hit.공포일자),
    enforcementRaw:   digits(hit.시행일자),
    enforcementDate:  formatDate(hit.시행일자),
    amendType:        hit.제개정구분명 || '',
    lawNumber:        hit.공포번호 ? String(hit.공포번호) : '',
    link:             rawLink && !rawLink.startsWith('http') ? `https://www.law.go.kr${rawLink}` : rawLink,
  }
}

// 법령명으로 조문 + 메타정보 가져오기 (법령검토 모달용)
export async function fetchLawFull(lawName) {
  let hit = await searchHit(lawName, 'law')
  let target = 'law'
  if (!hit) { hit = await searchHit(lawName, 'admrul'); target = 'admrul' }
  if (!hit) return { articles: null, meta: null }

  // 검색 결과에서 1차 메타 추출
  const rawLink = hit.법령링크 || null
  const meta = {
    lawName:          hit.법령명한글 || lawName,
    lawType:          hit.법령구분명 || '-',
    department:       hit.소관부처명 || '-',
    promulgationDate: formatDate(hit.공포일자),
    enforcementDate:  formatDate(hit.시행일자),
    lawNumber:        hit.공포번호 ? String(hit.공포번호) : '-',
    link:             rawLink && !rawLink.startsWith('http') ? `https://www.law.go.kr${rawLink}` : rawLink,
  }

  const mst = hit.법령일련번호
  const { data: contentData } = await axios.get(PROXY + '/content', { params: { mst, target }, headers: authHeaders() })
  const root = contentData?.법령 ?? contentData?.행정규칙 ?? contentData?.AdmRulService

  // 전문(content) 기본정보에서 날짜 재추출 — 검색 결과보다 정확
  extractDatesFromRoot(root, meta)

  return {
    articles: parseContentData(contentData),
    meta,
  }
}
