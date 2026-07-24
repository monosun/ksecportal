/**
 * 브라우저 CSPRNG(Web Crypto) 기반 난수 유틸.
 *
 * 언어 기본 난수(비암호학적 PRNG)는 예측 가능하므로(CWE-330) 식별자 생성·문항 무작위
 * 추출처럼 결과를 추측당하면 곤란한 곳에서는 이 유틸을 사용한다.
 */

/** 0 이상 max 미만의 정수 — 모듈로 편향 없이 뽑는다 */
export function randomInt(max) {
  if (!Number.isInteger(max) || max <= 0) return 0
  const limit = Math.floor(0xffffffff / max) * max   // 균등 분포를 깨는 구간은 버리고 다시 뽑는다
  const buf = new Uint32Array(1)
  let value
  do {
    crypto.getRandomValues(buf)
    value = buf[0]
  } while (value >= limit)
  return value % max
}

/** Fisher–Yates 셔플 (원본 배열을 그대로 섞어 반환) */
export function shuffle(arr) {
  for (let i = arr.length - 1; i > 0; i--) {
    const j = randomInt(i + 1)
    ;[arr[i], arr[j]] = [arr[j], arr[i]]
  }
  return arr
}

/** 화면 내부 식별자용 임의 문자열 */
export function randomId(prefix = '') {
  const id = typeof crypto.randomUUID === 'function'
    ? crypto.randomUUID()
    : Array.from(crypto.getRandomValues(new Uint8Array(16)))
        .map(b => b.toString(16).padStart(2, '0')).join('')
  return prefix ? `${prefix}_${id}` : id
}
