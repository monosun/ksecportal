// 퀴즈 정답 문자열 유틸 — 정답은 단일 "A" 또는 복수 "A,C" 형태로 저장된다.
// 백엔드(QuizAnswers.java)와 동일한 규칙: A~E만 유효, 오름차순 정렬, 콤마 구분.

const VALID = ['A', 'B', 'C', 'D', 'E']

/** 보기 식별자 목록 — 화면에서 보기 A~E를 순회할 때 사용한다. */
export const OPTION_LETTERS = VALID

/** 정답 문자열 → 정렬된 보기 배열. "a c", "AC", "A,C" 모두 ['A','C']로 해석한다. */
export function answerLetters(value) {
  if (!value) return []
  const letters = new Set()
  for (const ch of String(value).toUpperCase()) {
    if (VALID.includes(ch)) letters.add(ch)
  }
  return VALID.filter(l => letters.has(l))
}

/** 보기 배열 → 저장 형식 문자열 ("A,C") */
export function toAnswerString(letters) {
  return answerLetters((letters ?? []).join(',')).join(',')
}

/** 복수 정답 문항인지 */
export function isMultiAnswer(value) {
  return answerLetters(value).length > 1
}

/** 화면 표시용 ("A, C") */
export function formatAnswer(value) {
  return answerLetters(value).join(', ')
}

/** 특정 보기가 정답에 포함되는지 */
export function isCorrectOption(value, letter) {
  return answerLetters(value).includes(letter)
}

/** 두 답안이 완전히 일치하는지 — 복수 정답은 모두 골라야 정답 */
export function answersMatch(correct, submitted) {
  const a = answerLetters(correct)
  const b = answerLetters(submitted)
  return a.length > 0 && a.length === b.length && a.every((l, i) => l === b[i])
}
