// 스프레드시트 수식 삽입(Formula Injection) 방어.
// 사용자 입력이 = + - @ 또는 탭·CR 로 시작하면 엑셀이 셀을 편집(활성화)하는 순간 수식으로 해석하므로
// 내보낼 때 작은따옴표를 앞에 붙여 텍스트로 고정한다. (백엔드 SpreadsheetSafe 와 같은 규칙)
const DANGEROUS = /^[=+\-@\t\r]/

export function safeCell(v) {
  return typeof v === 'string' && DANGEROUS.test(v) ? `'${v}` : v
}

/** aoa_to_sheet 에 넘길 2차원 배열의 모든 문자열 셀에 safeCell 적용 */
export function safeRows(rows) {
  return rows.map(r => r.map(safeCell))
}
