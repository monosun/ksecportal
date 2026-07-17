import { assetApi } from '@/api'

/**
 * 처리시스템 후보 목록 — 자산관리의 서비스(SERVICE) · 애플리케이션(APPLICATION) 자산.
 *
 * 개인정보 처리현황의 `처리시스템`과 파일관리의 `운영 시스템`이 같은 목록을 쓰도록 공유한다.
 * 두 값은 개인정보 흐름 지도에서 문자열 일치로 연결되므로 표기가 어긋나면 연결이 끊긴다.
 *
 * 자산 목록 API에 자산유형(assetCategory) 필터가 없어 유형(type)으로 조회한 뒤
 * 자산유형을 클라이언트에서 거른다.
 */
export async function loadServiceAssets() {
  const res = await assetApi.list({ type: 'APPLICATION', size: 500, active: true })
  const rows = res.data?.content || []
  return rows
    .filter(a => a.assetCategory === 'SERVICE')
    .map(a => ({
      value: a.name,
      label: a.department ? `${a.name} · ${a.department}` : a.name,
    }))
}
