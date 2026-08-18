# API Reference

모든 엔드포인트는 `/api` prefix를 포함합니다.  
`/auth/login`, `/auth/register`를 제외한 모든 요청에 `Authorization: Bearer <token>` 헤더가 필요합니다.

**공통 응답 형식**

```json
{
  "success": true,
  "data": { ... },
  "message": null
}
```

오류 응답:
```json
{
  "success": false,
  "data": null,
  "message": "오류 메시지"
}
```

---

## 인증 (Auth)

### POST /auth/login

```json
// Request
{ "email": "secportal@monosun.com", "password": "Admin1234!" }

// Response
{
  "accessToken": "eyJhbGci...",
  "user": { "id": 1, "email": "secportal@monosun.com", "name": "System Admin", "role": "ADMIN" }
}
```

### POST /auth/register

```json
// Request
{ "email": "user@example.com", "password": "Pass1234!", "name": "홍길동", "department": "개발팀" }
```

### GET /auth/me

현재 로그인 사용자 정보 반환. **JWT 토큰 필수** (토큰 없거나 만료 시 `401` 반환).

---

## 보안 정책 (Policy)

### GET /policies

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `status` | String | `DRAFT`, `REVIEW`, `PUBLISHED`, `ARCHIVED` |
| `category` | String | `GENERAL`, `ACCESS_CONTROL`, `DATA_PROTECTION`, `INCIDENT_RESPONSE`, `NETWORK`, `PHYSICAL`, `VENDOR`, `OTHER` |
| `keyword` | String | 제목 검색 |
| `page` | int | 페이지 번호 (0-based) |
| `size` | int | 페이지 크기 (기본 20) |

```json
// Response (Page)
{
  "content": [
    {
      "id": 1, "title": "정보보안 기본 정책", "category": "GENERAL",
      "status": "PUBLISHED", "version": "1.0",
      "effectiveDate": "2025-01-01", "authorName": "System Admin",
      "guidelineName": "개인정보보호 지침", "chapterNo": 1,
      "chapterLabel": "제1장", "chapterTitle": "총칙", "articleCount": 4,
      "acknowledgmentCount": 5, "createdAt": "2025-01-01 09:00:00"
    }
  ],
  "totalElements": 10, "totalPages": 1
}
```

`guidelineName` / `chapter*` 는 제목(`"개인정보보호 지침 - 제1장 총칙"`)에서 파생되어 저장 시 자동으로 채워집니다.

### POST /policies *(MANAGER+)*

```json
{
  "title": "비밀번호 관리 정책",
  "content": "# 비밀번호 정책\n...",
  "category": "ACCESS_CONTROL",
  "status": "DRAFT",
  "version": "1.0",
  "effectiveDate": "2025-06-01"
}
```

### GET /policies/:id

### PATCH /policies/:id *(MANAGER+)*

변경할 필드만 전송 (partial update).

### DELETE /policies/:id *(ADMIN)*

### DELETE /policies?ids=1,2,3 *(ADMIN)*

목록에서 선택한 정책 일괄 삭제. 존재하지 않는 ID 는 건너뛰고 **실제 삭제 건수**를 반환합니다.
정책에 연결된 열람 확인 기록도 함께 삭제됩니다(`cascade = ALL`).

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `ids` | List&lt;Long&gt; | 삭제할 정책 ID 목록 (콤마 구분) |

### POST /policies/:id/acknowledge

정책 수신 확인. 중복 확인 시 409 반환.

---

## 보안 정책 조문 (Policy Article)

정책은 **지침 &gt; 장(章) &gt; 조(條)** 계층으로 다룹니다. 정책 1건이 장 하나이고,
본문의 `### 제N조(제목)` 머리글이 조 단위 레코드로 세분화되어 저장됩니다.
조는 본문의 파생 데이터라 정책을 저장할 때마다 다시 만들어집니다.

### GET /policies/articles

지침 &gt; 장 &gt; 조 계층 검색. 정렬은 지침 → 장 번호 → 조 순서 고정(번호 없는 부칙은 맨 뒤).

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `keyword` | String | 검색어 |
| `scope` | String | 검색 범위 — `ALL`(기본, 제목+본문+지침+장+조표기), `TITLE`(조 제목), `CONTENT`(본문·개정표기), `GUIDELINE`(지침명), `CHAPTER`(장 표기·제목), `ARTICLE`(조 표기) |
| `guideline` | String | 지침명 완전일치 |
| `chapterNo` | int | 장 번호 |
| `policyId` | Long | 특정 장(정책 id)으로 한정 |
| `articleNo` | int | 조 번호 |
| `status` | String | 정책 상태 |
| `category` | String | 정책 카테고리 |
| `page` / `size` | int | 페이징 (기본 20) |

```json
// Response (Page)
{
  "content": [
    {
      "id": 12, "policyId": 77, "policyTitle": "개인정보보호 지침 - 제8장 개인정보의 안전성 확보조치",
      "guidelineName": "개인정보보호 지침", "chapterNo": 8,
      "chapterLabel": "제8장", "chapterTitle": "개인정보의 안전성 확보조치",
      "articleNo": 31, "articleSubNo": null, "articleLabel": "제31조",
      "title": "접속기록의 보관 및 점검", "displayName": "제31조(접속기록의 보관 및 점검)",
      "note": "<개정 2024.5.28>", "content": "- 시스템 운영담당자는 …",
      "category": "DATA_PROTECTION", "status": "PUBLISHED", "version": "1.2"
    }
  ],
  "totalElements": 139, "totalPages": 7
}
```

### GET /policies/articles/facets

검색 필터 드롭다운용 지침 &gt; 장 목록.

```json
{
  "guidelines": [
    { "name": "개인정보보호 지침",
      "chapters": [
        { "policyId": 70, "chapterNo": 1, "chapterLabel": "제1장",
          "chapterTitle": "총칙", "label": "제1장 총칙" }
      ] }
  ],
  "totalArticles": 139
}
```

### GET /policies/:id/articles

특정 장에 속한 조 목록 (정렬 순서대로).

### POST /policies/articles/resync *(MANAGER+)*

전체 정책 본문을 다시 파싱해 조를 재등록하고 등록 건수를 반환합니다.
본문을 DB에서 직접 손봤거나 파싱 규칙이 바뀐 뒤에 사용합니다.

### GET /policies/bulk/template *(MANAGER+)*

엑셀 일괄 등록용 템플릿 다운로드 (`.xlsx`).  
컬럼: `제목*`, `카테고리*`, `내용`, `상태`, `버전`, `시행일(YYYY-MM-DD)`

### POST /policies/bulk *(MANAGER+, multipart/form-data)*

엑셀 파일 업로드 → 보안 정책 일괄 등록.

| 필드 | 타입 | 설명 |
|------|------|------|
| `file` | File | `.xlsx` 파일 |

```json
// Response
{
  "total": 10, "success": 9, "failed": 1,
  "errors": [{ "row": 3, "message": "카테고리 값이 유효하지 않습니다: UNKNOWN" }]
}
```

### POST /policies/documents/extract *(MANAGER+, multipart/form-data)*

개별 정책 등록용 — 문서 파일에서 **제목·본문 초안만** 뽑는다. **저장하지 않는다.**
화면(정책 등록 팝업 > `문서 파일로 등록`)에서 사용자가 확인·수정한 뒤 평소대로 `POST /policies` 로 저장한다.

| 필드 | 타입 | 설명 |
|------|------|------|
| `file` | File | `.pdf`, `.docx`, `.txt`, `.md` |

```json
// Response
{
  "title": "정보보호 지침",
  "content": "## 제1장 총칙

### 제1조(목적)
…",
  "articleCount": 8,
  "chapterCount": 4,
  "warnings": ["문서에서 장(章) 4개를 찾았습니다. …"]
}
```

### POST /policies/documents/import *(MANAGER+, multipart/form-data)*

지침 문서를 **장(章)별 정책으로 등록**한다. 장마다 정책 1건을 만들고 제목을
`"<지침명> - 제N장 <장제목>"` 으로 맞추므로, 저장 시 기존 경로와 똑같이 지침명·장 컬럼과
**조(條) 레코드가 자동 세분화**된다.

| 필드 | 타입 | 설명 |
|------|------|------|
| `file` | File | `.pdf`, `.docx`, `.txt`, `.md` (스캔 이미지 PDF 는 글자가 없어 실패) |
| `guidelineName` | String | 비우면 문서 제목 줄 또는 파일명에서 추측 |
| `category` | Enum | 장별 정책에 공통 적용 (기본 `GENERAL`) |
| `status` | Enum | 공통 적용 (기본 `DRAFT`) |
| `version` | String | 공통 적용 (기본 `1.0`) |
| `effectiveDate` | Date | `YYYY-MM-DD` |
| `dryRun` | Boolean | `true` 면 저장하지 않고 등록될 내용만 돌려준다(미리보기) |

```json
// Response
{
  "guidelineName": "정보보호 지침", "fileName": "정보보호지침.pdf", "dryRun": false,
  "created": 3, "updated": 1, "articleCount": 8,
  "chapters": [
    { "title": "정보보호 지침 - 제1장 총칙", "chapterLabel": "제1장", "chapterTitle": "총칙",
      "articleCount": 4, "contentLength": 1820, "existing": false, "existingPolicyId": null }
  ],
  "warnings": []
}
```

**같은 제목의 장이 이미 있으면 본문만 갱신한다.** 조 레코드는 조 표기 기준으로 재사용되므로
ISMS-P 통제항목에 걸어둔 **조 단위 매핑이 유지**된다.

문서 정리 규칙: 쪽번호(`- 12 -` 등)와 절반 이상의 쪽에 되풀이되는 머리말·꼬리말, 그리고 **목차**를 걷어내고,
`제N조(제목)` 줄을 `### 제N조(제목)` 마크다운 머리글로 올린다(장 머리글은 `## `).
본문 문장의 "제3조의 규정에 따라 …" 같은 표현은 제목 괄호가 없어 조로 잡히지 않는다.

**목차 제외** — 목차를 남기면 차례의 "제1장 …", "제1조(…)" 가 본문의 장·조로 잡혀 빈 장이 등록되므로 두 갈래로 걷어낸다.

1. `목 차` · `차례` · `CONTENTS` 머리글부터, **목차에 실린 장·조 머리글이 본문에서 다시 나오는 지점**까지
   (쪽번호가 없는 차례도 이 규칙으로 걸러진다)
2. `제1조(목적) ……… 3` 처럼 **점선이나 쪽번호가 붙은 줄** — 목차 머리글이 없는 문서에서도 제외

제외한 줄 수는 `warnings` 에 `"목차로 보이는 N줄을 제외했습니다."` 로 실려 온다.
끝에 숫자가 붙어도 **장·조 머리글이면서 60자 이하일 때만** 목차로 보므로 본문 문장은 지워지지 않는다.

---

## 취약점 (Vulnerability)

### GET /vulnerabilities

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `status` | String | `OPEN`, `IN_PROGRESS`, `RESOLVED`, `ACCEPTED`, `FALSE_POSITIVE` |
| `severity` | String | `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`, `INFO` |
| `keyword` | String | 제목 / CVE ID 검색 |

```json
// Response 항목
{
  "id": 1, "title": "Log4Shell 취약점", "cveId": "CVE-2021-44228",
  "cvssScore": 10.0, "severity": "CRITICAL", "status": "OPEN",
  "assetName": "메인 웹 서버", "assigneeName": "홍길동", "reporterName": "System Admin",
  "dueDate": "2025-06-01", "resolvedAt": null,
  "createdAt": "2025-05-01 09:00:00", "updatedAt": "2025-05-01 09:00:00"
}
```

### GET /vulnerabilities/stats

```json
{
  "status_OPEN": 5, "status_IN_PROGRESS": 3, "status_RESOLVED": 12,
  "status_ACCEPTED": 1, "status_FALSE_POSITIVE": 2,
  "severity_CRITICAL": 2, "severity_HIGH": 4, "severity_MEDIUM": 8,
  "severity_LOW": 6, "severity_INFO": 1
}
```

### POST /vulnerabilities

```json
{
  "title": "취약점 제목",
  "description": "상세 설명",
  "severity": "HIGH",
  "cveId": "CVE-2025-12345",
  "cvssScore": 8.1,
  "assetName": "웹 서버",
  "assigneeId": 2,
  "dueDate": "2025-07-01"
}
```

### PATCH /vulnerabilities/:id *(MANAGER+)*

### DELETE /vulnerabilities/:id *(ADMIN)*

### GET /vulnerabilities/bulk/template *(MANAGER+)*

엑셀 일괄 등록용 템플릿 다운로드 (`.xlsx`).  
컬럼: `취약점명*`, `심각도*`, `설명`, `CVE ID`, `CVSS 점수(0.0~10.0)`, `자산명`, `상태`, `조치 기한(YYYY-MM-DD)`

### POST /vulnerabilities/bulk *(MANAGER+, multipart/form-data)*

엑셀 파일 업로드 → 취약점 일괄 등록.

```json
// Response
{ "total": 5, "success": 5, "failed": 0, "errors": [] }
```

### GET /vulnerabilities/:id/comments

```json
[
  { "id": 1, "userId": 1, "userName": "System Admin", "content": "조치 시작함", "createdAt": "2025-05-10 14:00:00", "updatedAt": null }
]
```

`updatedAt` 은 수정된 적이 있는 댓글에만 값이 있다.

### POST /vulnerabilities/:id/comments

```json
{ "content": "취약점 패치 완료" }
```

### PATCH /vulnerabilities/:id/comments/:commentId

댓글 수정. **작성자 본인만** 가능하며, 타인의 댓글이면 400 (`본인이 작성한 댓글만 수정할 수 있습니다.`).

```json
{ "content": "수정한 댓글 내용" }
```

### DELETE /vulnerabilities/:id/comments/:commentId

댓글 삭제. **작성자 본인만** 가능하다.

---

## ISMS-P 증적관리 (ISMS)

### GET /isms/items

ISMS-P 인증 항목 목록 조회.

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `year` | int | 연도 지정 시 증적 건수 및 준수상태 포함 |
| `domainCode` | String | 도메인 필터 (예: `1.1`, `2.5`, `3.3`) |

```json
// Response 항목
{
  "id": 1, "itemCode": "1.1.1", "itemName": "경영진의 참여",
  "domainCode": "1.1", "domainName": "관리체계 기반 마련",
  "sectionNum": 1, "sectionName": "관리체계 수립 및 운영",
  "guide": "…이행가이드…",
  "defaultEvidenceTitle": "경영진의 참여 증적",
  "defaultEvidenceContent": "…증적예시…",
  "evidenceExamples": "정보보호위원회 회의록\n경영진 정보보호 현황 보고자료\n…",
  "evidenceCount": 2, "latestStatus": "COMPLIANT",
  "mappedPolicies": [
    { "id": 96, "title": "개인정보보호 지침 - 제1장 총칙", "status": "PUBLISHED", "category": "DATA_PROTECTION",
      "guidelineName": "개인정보보호 지침", "chapterLabel": "제1장", "chapterTitle": "총칙",
      "articleId": null },
    { "id": 96, "title": "개인정보보호 지침 - 제1장 총칙", "status": "PUBLISHED", "category": "DATA_PROTECTION",
      "guidelineName": "개인정보보호 지침", "chapterLabel": "제1장", "chapterTitle": "총칙",
      "articleId": 126, "articleLabel": "제2조", "articleTitle": "정의",
      "articleDisplayName": "제2조(정의)" }
  ]
}
```

`guide` / `defaultEvidenceTitle` / `defaultEvidenceContent` / `evidenceExamples` 는 항목별 기본값(연도 무관)으로, 일괄등록 템플릿의 기본값이자 관리 > 코드관리 `ISMS-P 101항목` 탭에서 편집한다.

`evidenceExamples` 는 **예시 증적자료명** 목록이다 — 줄바꿈(`\n`)으로 구분된 한 줄이 자료명 하나이며, 증적관리 화면의 이행 가이드 영역에 함께 표시된다. 101개 항목 모두 기본값을 시드하며, 비어 있는 항목에만 채워지므로(seed-when-empty) 사용자가 고친 값은 유지된다.

`mappedPolicies` 는 **지침 &gt; 장 &gt; 조** 계층을 그대로 싣는다. `articleId` 가 `null` 이면 **장(章) 전체** 매핑,
값이 있으면 그 장 안의 **조(條) 단위** 매핑이다. 같은 장에 장 전체와 여러 조가 함께 걸릴 수 있으므로
`id`(정책 id) 하나로는 항목을 구분할 수 없다 — 화면에서 키를 만들 때는 `articleId` 까지 조합해야 한다.
정렬은 정책 id → 장 전체 → 조 순서로 내려온다.

### GET /isms/items/:id

단일 항목 조회.

### PATCH /isms/items/:id/defaults *(MANAGER+)*

항목별 기본 증적제목·증적내용·이행가이드·예시 증적자료명 수정 (관리 > 코드관리 `ISMS-P 101항목` 탭).

```json
{ "defaultEvidenceTitle": "…", "defaultEvidenceContent": "…", "evidenceExamples": "자료명1\n자료명2", "guide": "…" }
```

### 통제항목 × 정책 매핑

정책은 **장(章) 전체** 또는 **조(條) 단위**로 매핑한다. 두 매핑은 서로 독립이라, 장 전체를 해제해도
같은 장의 조 매핑은 남는다. 이미 걸려 있는 매핑을 다시 요청하면 조용히 무시된다(중복 생성 없음).

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/isms/items/:itemId/policies/:policyId` | 장 전체 매핑 *(MANAGER+)* |
| DELETE | `/isms/items/:itemId/policies/:policyId` | 장 전체 매핑만 해제 *(MANAGER+)* |
| POST | `/isms/items/:itemId/articles/:articleId` | 조 단위 매핑 — 소속 장은 조에서 따라간다 *(MANAGER+)* |
| DELETE | `/isms/items/:itemId/articles/:articleId` | 조 매핑 해제 *(MANAGER+)* |

매핑할 조 목록은 `GET /policies/:id/articles` 로 가져온다.
정책 본문에서 사라진 조는 매핑도 함께 삭제된다(조 삭제 시 연쇄 삭제).
반대로 본문을 고쳐도 **같은 조 표기(제N조)는 레코드를 재사용**하므로 매핑이 유지된다.

### GET /isms/items/:id/evidences

항목별 증적 목록 조회.

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `year` | int | 연도 필터 (생략 시 전체) |

```json
// Response 항목
{
  "id": 1, "itemId": 1, "itemCode": "1.1.1", "itemName": "경영진의 참여",
  "year": 2026, "title": "정보보호 정책 승인 문서",
  "content": "최고경영자 서명 포함 정책서",
  "fileName": "security_policy_2026.pdf",
  "filePath": "isms/1/a3f9c2d1-uuid.pdf",
  "status": "COMPLIANT",
  "registrantId": 1, "registrantName": "System Admin",
  "createdAt": "2026-05-17 10:00:00", "updatedAt": "2026-05-17 10:00:00"
}
```

### POST /isms/items/:id/evidences *(multipart/form-data)*

증적 등록. 파일 첨부 선택 사항.

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `year` | int | ✅ | 연도 |
| `title` | String | ✅ | 증적 제목 |
| `content` | String | - | 증적 내용 |
| `status` | String | - | `COMPLIANT` / `PARTIAL` / `NON_COMPLIANT` / `NA` (기본: `COMPLIANT`) |
| `file` | File | - | 첨부 파일 (모든 형식 허용, 최대 10MB) |

### PATCH /isms/evidences/:id *(multipart/form-data)*

증적 수정. 변경할 필드만 전송. 파일 첨부 시 기존 파일 교체.

| 필드 | 타입 | 설명 |
|------|------|------|
| `title` | String | 증적 제목 |
| `content` | String | 증적 내용 |
| `status` | String | 준수상태 |
| `file` | File | 새 첨부 파일 (기존 파일 삭제 후 교체) |

### DELETE /isms/evidences/:id

증적 삭제 (첨부 파일도 함께 삭제).

### GET /isms/evidences/:id/file

첨부 파일 다운로드.

- 응답 헤더: `Content-Disposition: attachment; filename*=UTF-8''<원본파일명>`
- `filePath` 가 없는 증적은 404 반환

### DELETE /isms/evidences/:id/file

첨부 파일만 삭제 (증적 레코드는 유지). 업데이트된 증적 정보 반환.

### GET /isms/summary

연도별 전체 준수 현황 요약.

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `year` | int | 연도 (생략 시 현재 연도) |

```json
{
  "year": 2026, "totalItems": 101,
  "compliant": 40, "partial": 10, "nonCompliant": 5, "na": 3, "noEvidence": 43,
  "byDomain": [
    {
      "domainCode": "1.1", "domainName": "관리체계 기반 마련",
      "sectionNum": 1, "total": 6,
      "compliant": 4, "partial": 1, "nonCompliant": 0, "na": 0, "noEvidence": 1
    }
  ]
}
```

### GET /isms/copy-previous/status

가져오기 / 가져오기 초기화 버튼 상태.

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `year` | int | 대상 연도 (생략 시 현재 연도) |

```json
{
  "previousYear": 2025,      // 가져올 수 있는 이전 연도 (없으면 null)
  "copiedEvidences": 87,     // 대상 연도에 남아 있는 '가져오기로 생성된' 증적 (0 이면 초기화 불가)
  "copiedNotes": 12,
  "copiedFromYear": 2025     // 그 증적들의 원본 연도
}
```

### POST /isms/copy-previous *(MANAGER+)*

전년도(= `GET /isms/previous-year` 결과) 증적을 대상 연도로 복사한다.
증적제목·증적내용·준수상태·첨부파일(실물 복제)과 연도별 현재상태·의견을 함께 가져오며,
대상 연도에 **이미 증적이 있는 항목은 건너뛴다**(중복 복사 방지). 참조 증적은 원본 참조를 그대로 이어받는다.

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `year` | int | 복사 대상(붙여넣을) 연도 (생략 시 현재 연도) |

```json
// Response
{
  "sourceYear": 2025, "targetYear": 2026,
  "copiedEvidences": 87, "copiedNotes": 12, "skippedItems": 3
}
```

가져올 이전 연도 증적이 없으면 `400` (`가져올 이전 연도 증적이 없습니다.`).
복사된 레코드에는 `copied_from_year` 에 원본 연도가 기록되어 초기화(되돌리기) 대상으로 식별된다.

### DELETE /isms/copy-previous *(MANAGER+)*

**가져오기 초기화** — 대상 연도에서 가져오기로 생성된(`copied_from_year IS NOT NULL`) 증적·현재상태·의견을 삭제해
가져오기 전 상태로 되돌린다. 직접 등록·작성한 기록은 삭제되지 않으며, 원본 연도의 증적·첨부파일도 영향을 받지 않는다.
가져온 증적을 참조하는 증적이 있으면 참조가 깨지므로 함께 삭제하고 `removedReferences` 로 보고한다.

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `year` | int | 초기화할 연도 (생략 시 현재 연도) |

```json
// Response
{
  "targetYear": 2026, "copiedFromYear": 2025,
  "removedEvidences": 87, "removedNotes": 12, "removedReferences": 0
}
```

되돌릴 가져오기 내역이 없으면 `400` (`되돌릴 가져오기 내역이 없습니다.`).

### GET /isms/export/csv *(MANAGER+)*

연도별 전체 증적 CSV 다운로드 (UTF-8 BOM, Excel 호환).

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `year` | int | 연도 (생략 시 현재 연도) |

컬럼: `항목코드`, `항목명`, `섹션`, `도메인`, `증적제목`, `증적내용`, `파일명`, `준수상태`, `등록자`

### GET /isms/import/template *(MANAGER+)*

일괄 등록용 엑셀 템플릿 다운로드 (`.xlsx`, 3개 시트: `증적입력` / `입력규칙` / `항목목록(참고)`).
`증적입력` 시트 열: **항목코드 · 증적제목 · 증적내용 · 이행가이드 · 파일명/경로 · 준수상태 · 예시 증적자료(참고)**. 항목별로 한 행씩 미리 채워져 제공되며, `이행가이드` 열에는 해당 항목의 이행가이드(증적예시 포함)가 들어 있다(가이드 없는 항목은 빈 칸 — **이행가이드는 선택 입력**). 업로드 시 `이행가이드` 값이 있으면 해당 항목의 가이드를 갱신한다. 증적제목·증적내용·파일명·준수상태가 모두 빈 행(미작성 행)은 건너뛴다. 맨 뒤 `예시 증적자료(참고)` 열은 항목의 예시 증적자료명을 채워 내려주는 **읽기 전용 참고 열**로 업로드 시 읽지 않으며, 0~5 열의 순서를 바꾸지 않았으므로 이전에 내려받은 템플릿도 그대로 올릴 수 있다. `항목목록(참고)` 시트에도 항목별 예시 증적자료 열이 있다.

### POST /isms/import *(MANAGER+, multipart/form-data)*

엑셀(`.xlsx`) 또는 CSV 파일로 증적 일괄 등록.

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `year` | int | 등록 연도 (쿼리 파라미터) |
| `file` | File | `.xlsx` 또는 `.csv` |

```json
// Response
{
  "total": 10, "success": 9, "failed": 1,
  "errors": [{ "row": 5, "itemCode": "9.9.9", "message": "존재하지 않는 항목코드: 9.9.9" }]
}
```

---

## 보안 인시던트 (Incident)

### GET /incidents

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `keyword` | String | 제목 검색 |
| `severity` | String | `CRITICAL`, `HIGH`, `MEDIUM`, `LOW` |
| `status` | String | `OPEN`, `INVESTIGATING`, `CONTAINED`, `RESOLVED`, `CLOSED` |
| `type` | String | 아래 유형 참고 |

**인시던트 유형 (`type`)**: `MALWARE`, `PHISHING`, `DATA_BREACH`, `UNAUTHORIZED_ACCESS`, `DDOS`, `INSIDER_THREAT`, `PHYSICAL`, `OTHER`

### POST /incidents

```json
{
  "title": "랜섬웨어 감염 의심",
  "description": "재무팀 PC에서 파일 암호화 감지",
  "severity": "CRITICAL",
  "type": "MALWARE",
  "affectedSystems": "재무팀 PC (192.168.2.30)",
  "assigneeId": 2,
  "detectedAt": "2025-05-16T09:30:00"
}
```

### PATCH /incidents/:id *(MANAGER+)*

`status` 변경 시 `RESOLVED` / `CLOSED`로 설정되면 `resolvedAt`이 자동 기록됩니다.

### DELETE /incidents/:id *(ADMIN)*

### GET /incidents/bulk/template *(MANAGER+)*

엑셀 일괄 등록용 템플릿 다운로드 (`.xlsx`).  
컬럼: `제목*`, `유형*`, `심각도*`, `설명`, `영향 시스템`, `상태`, `탐지 시각(YYYY-MM-DD HH:mm)`

### POST /incidents/bulk *(MANAGER+, multipart/form-data)*

엑셀 파일 업로드 → 보안 인시던트 일괄 등록.

```json
// Response
{ "total": 8, "success": 7, "failed": 1,
  "errors": [{ "row": 4, "message": "유형 값이 유효하지 않습니다: HACK" }] }
```

---

## 자산 관리 (Asset)

### GET /assets

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `keyword` | String | 자산명 / IP / 담당자 검색 |
| `type` | String | `SERVER`, `WORKSTATION`, `NETWORK`, `APPLICATION`, `DATABASE`, `CLOUD`, `OTHER` |
| `assetCategory` | String | 자산유형 — `INFO`, `SW`, `HW`, `SERVICE`, `PERSONNEL`, `FACILITY` |
| `criticality` | String | `HIGH`, `MEDIUM`, `LOW` |
| `active` | Boolean | 운영 중 여부 |

### POST /assets *(MANAGER+)*

```json
{
  "name": "메인 API 서버",
  "type": "SERVER",
  "ipAddress": "192.168.1.10",
  "owner": "인프라팀",
  "department": "IT",
  "description": "프로덕션 API 서버",
  "criticality": "HIGH"
}
```

### PATCH /assets/:id *(MANAGER+)*

### DELETE /assets/:id *(ADMIN)*

> 자산유형(`assetCategory`)이 `SW`인 자산은 `sbomSoftwareId`로 SBOM 관리에 등록된 SW를 맵핑할 수 있습니다.
> 등록/수정 요청에 `sbomSoftwareId`(SW id)를 포함하면 맵핑되고, 수정 시 `0` 이하 값을 보내면 맵핑이 해제됩니다.
> 응답에는 `sbomSoftwareId`, `sbomSoftwareName`, `sbomSoftwareVersion`, `sbomComponentCount`가 포함됩니다.

### GET /assets/types/stats *(MANAGER+)*

자산유형별 자산 수. 응답: `[{ "type": "SERVER", "count": 12 }, ...]`

### DELETE /assets/by-type?type=SERVER *(ADMIN)*

해당 유형의 자산을 일괄 삭제하고 삭제 건수를 반환합니다.

### GET /assets/snapshots *(MANAGER+)*

자산 시점(스냅샷) 이력 목록. 응답 항목: `id`, `title`, `memo`, `assetCount`, `createdBy`, `createdAt`.

### POST /assets/snapshots *(MANAGER+)*

현재 자산 목록 전체를 하나의 시점으로 저장(복사 보관). 요청: `{ "title": "2026 상반기", "memo": "..." }`

### GET /assets/snapshots/:id/items *(MANAGER+)*

특정 시점에 저장된 자산 목록(스냅샷 항목).

### DELETE /assets/snapshots/:id *(ADMIN)*

시점 이력 삭제.

---

## 위험평가 · 위험 처리 계획 (Risk)

위험평가 차수(연도·차수)별로 자산×위협 항목을 평가하고, 완료 차수의 처리방법 '감소' 항목으로 처리 계획을 수립합니다. 처리방법은 `수용 / 감소 / 회피 / 이전`.

### GET /risk/rounds/:roundId/treatment-plans

해당 차수의 처리방법 '감소' 항목 목록(위험점수 높은 순). 응답에 처리계획 필드(`plan`, `planAssignee`, `planDueDate`, `planProgress`, `planStatus`)가 포함됩니다.

### PATCH /risk/assessments/:id/treatment-plan *(MANAGER+)*

개별 항목의 처리 계획 갱신. 요청: `{ "plan": "...", "planAssignee": "홍길동", "planDueDate": "2026-08-31", "planProgress": 60, "planStatus": "진행중" }`

### PATCH /risk/assessments/bulk-treatment *(MANAGER+)*

선택 항목의 처리방법 일괄 변경. 요청: `{ "ids": [1,2,3], "treatment": "감소" }`

---

## SBOM 관리 (SBOM)

소프트웨어(SW명+버전)별로 포함된 라이브러리·버전·라이선스를 **CycloneDX 1.5 표준 기준**으로 관리합니다.
컴포넌트 필드는 CycloneDX component에 대응합니다: `componentType`(type), `groupName`(group), `libraryName`(name), `libraryVersion`(version), `purl`, `license`(SPDX ID).

### GET /sbom/software

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `keyword` | String | SW명 / 버전 / 공급업체 검색 |
| `page`, `size` | int | 페이징 (기본 20건) |

### GET /sbom/software/all

자산 맵핑 드롭다운용 전체 SW 간략 목록 (`id`, `name`, `version`, `vendor`, `componentCount`).

### GET /sbom/software/:id

SW 상세 + 포함 라이브러리(`components`) 목록.

### POST /sbom/software *(MANAGER+)*

```json
{
  "name": "KSecPortal Backend",
  "version": "1.2.0",
  "vendor": "Monosun",
  "description": "보안포털 백엔드 서비스"
}
```

`name`+`version` 조합은 중복 등록할 수 없습니다.

### PATCH /sbom/software/:id *(MANAGER+)*

### DELETE /sbom/software/:id *(MANAGER+)*

SW 삭제 시 포함 라이브러리가 함께 삭제되고, 해당 SW에 맵핑된 자산은 맵핑이 자동 해제됩니다.

### POST /sbom/software/:id/components *(MANAGER+)*

```json
{
  "componentType": "library",
  "groupName": "org.springframework.boot",
  "libraryName": "spring-boot-starter-web",
  "libraryVersion": "3.3.5",
  "purl": "pkg:maven/org.springframework.boot/spring-boot-starter-web@3.3.5",
  "license": "Apache-2.0",
  "remarks": ""
}
```

`componentType`은 CycloneDX 1.5 component.type 허용값만 사용 가능:
`application`, `framework`, `library`(기본), `container`, `platform`, `operating-system`, `device`, `device-driver`, `firmware`, `file`, `machine-learning-model`, `data`

### PATCH /sbom/components/:componentId *(MANAGER+)*

### DELETE /sbom/components/:componentId *(MANAGER+)*

### GET /sbom/software/:id/cyclonedx

SW의 SBOM을 **CycloneDX 1.5 JSON**으로 내보냅니다 (`{SW명}-{버전}.cdx.json` 다운로드).
`metadata.component`에 SW 정보(type: application, name, version, supplier, description),
`components[]`에 라이브러리 목록(type, group, name, version, purl, licenses)이 포함됩니다.

### POST /sbom/import/cyclonedx *(MANAGER+, multipart)*

CycloneDX JSON 파일을 업로드해 SBOM을 가져옵니다. syft·cdxgen·trivy 등 SCA 도구 산출물을 그대로 사용할 수 있습니다.

- `bomFormat: "CycloneDX"` 검증, `metadata.component` → SW(name+version), `components[]` → 라이브러리
- 동일 SW명+버전이 이미 있으면 라이브러리 병합(동일 name+version 컴포넌트는 갱신)
- 응답 형식은 `POST /sbom/bulk`와 동일 (`total`/`success`/`failed`/`softwareCount`/`errors` — errors.row는 components 배열 인덱스)

### GET /sbom/bulk/template *(MANAGER+)*

엑셀 일괄등록 템플릿 다운로드 (`sbom-upload-template.xlsx`).
컬럼: `SW명*`, `SW버전*`, `공급업체`, `SW설명`, `라이브러리명*`, `라이브러리 버전`, `그룹(네임스페이스)`, `PURL`, `컴포넌트 유형`, `라이선스(SPDX ID)`, `비고`
— 한 행에 라이브러리 1건씩 입력하며, 같은 SW명+버전 행은 하나의 SW로 묶여 등록됩니다.

### POST /sbom/bulk *(MANAGER+, multipart)*

```json
{ "total": 5, "success": 5, "failed": 0, "softwareCount": 2, "errors": [] }
```

이미 등록된 SW명+버전이면 기존 SW에 라이브러리가 추가되고, 동일 라이브러리명+버전은 라이선스/비고만 갱신됩니다(재업로드 시 중복 없음).

---

## IT 및 정보보호 교육 (Training)

### GET /training/courses

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `keyword` | String | 코스명 **또는 설명** 검색 |
| `mandatory` | Boolean | 이수구분 — `true` 필수 / `false` 선택 |
| `contentType` | String | 교육유형 — `VIDEO`, `DOCUMENT`, `QUIZ_ONLY` |

응답에 현재 사용자의 `completed`, `score` 포함. (이수여부 필터는 사용자별 값이라 화면에서 처리)

### POST /training/courses *(MANAGER+)*

```json
{
  "title": "개인정보보호 기초",
  "description": "...",
  "contentType": "DOCUMENT",
  "contentUrl": "https://...",
  "passingScore": 70,
  "mandatory": true,
  "questions": [
    {
      "question": "개인정보 수집 시 필요한 것은?",
      "optionA": "동의서",
      "optionB": "계약서",
      "optionC": "영수증",
      "optionD": "명함",
      "optionE": null,
      "correctAnswer": "A",
      "difficulty": "중",
      "explanation": "개인정보 수집 시 정보주체의 동의가 필요합니다.",
      "sortOrder": 1
    }
  ]
}
```

### POST /training/courses/:id/submit

```json
// Request: { questionId: answer, ... }  — 복수 정답 문항은 "A,C" 형식
{ "1": "A", "2": "B", "3": "A,C" }

// Response
{ "score": 100, "passed": true, "correctCount": 3, "totalCount": 3 }
```

채점은 집합 비교입니다. 복수 정답 문항은 **정답 보기를 모두 정확히** 선택해야 정답으로 인정되며, 하나라도 빠지거나 오답이 섞이면 오답 처리됩니다.

### GET /training/results *(MANAGER+)*

코스별 이수 현황 요약 (이수 인원·합격 인원·평균 점수·전체 사용자 수).

### GET /training/results/completions *(MANAGER+)*

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `courseId` | Long | 코스별 필터 (생략 시 전체) |

사용자별 이수 내역 (이름·부서·점수·합격 여부·이수 일시).

---

## 문제은행 (Quiz Bank) *(MANAGER+)*

교육 코스와 독립적으로 퀴즈 문항을 관리합니다.

### GET /quiz-bank

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `category` | String | 분류 필터 |
| `difficulty` | String | 난이도 필터 (상/중/하) |
| `keyword` | String | 문제·선택지 검색 |
| `page`, `size` | int | 페이지네이션 (기본 0, 20) |

### GET /quiz-bank/categories

등록된 분류 목록.

### GET /quiz-bank/categories/stats

분류별 문제 수. `[{ "category": "개인정보보호", "count": 12 }, ...]` — 미분류 문항은 `category: null` 버킷으로 반환.

### DELETE /quiz-bank/by-category

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `category` | String | 삭제할 분류. 비어 있으면 미분류 문항을 삭제 |

해당 분류의 문항을 일괄 삭제하고 삭제 건수를 반환.

### POST /quiz-bank

```json
{
  "category": "개인정보보호",
  "difficulty": "중",
  "question": "개인정보 수집 시 필요한 것은?",
  "optionA": "동의서",
  "optionB": "계약서",
  "optionC": "영수증",
  "optionD": "명함",
  "optionE": null,
  "correctAnswer": "A",
  "explanation": "개인정보 수집 시 정보주체의 동의가 필요합니다."
}
```

`correctAnswer` 는 **복수 정답**을 지원합니다. 단일은 `"A"`, 복수는 `"A,C"` 형식으로 저장되며, 입력은 `"AC"` · `"a c"` · `"A/C"` 처럼 보내도 `"A,C"` 로 정규화됩니다. A~E 외의 문자가 있거나 정답이 하나도 없으면 400(BusinessException)이고, 정답으로 지정한 보기가 비어 있어도 거부됩니다. 보기는 `optionA`~`optionE`(A·B 필수, C~E 선택)까지 지원합니다.

### PATCH /quiz-bank/:id

### DELETE /quiz-bank/:id

### GET /quiz-bank/bulk/template

엑셀 일괄 등록 템플릿 다운로드.

### POST /quiz-bank/bulk

`multipart/form-data`의 `file`로 엑셀 업로드 — 성공(`successCount`)·실패(`failCount`)·중복 제외(`skippedCount`) 건수와 행별 오류(`errors`) 반환. 이미 등록된 문항이나 파일 내 중복 등 **동일한 문제(문제 텍스트 일치)는 등록하지 않고 제외**합니다.

---

## 보안 지표 (Metrics)

### GET /metrics/summary

```json
{
  "totalAssets": 25,
  "highCriticalityAssets": 8,
  "overdueVulns": 3,
  "openVulns": 12,
  "openIncidents": 2,
  "criticalIncidents": 1,
  "policyAckRate": 68.5,
  "trainingCompletionRate": 72.0,
  "vulnTrend": [
    { "month": "2024-12", "count": 3 },
    { "month": "2025-01", "count": 5 },
    { "month": "2025-02", "count": 2 },
    { "month": "2025-03", "count": 7 },
    { "month": "2025-04", "count": 4 },
    { "month": "2025-05", "count": 6 }
  ]
}
```

---

## 리포트 (Report) *(MANAGER+)*

모든 리포트 엔드포인트에 `lang=ko|en` 쿼리 파라미터를 지원합니다 (기본: `ko`).  
언어 설정에 따라 **파일명**과 **컬럼 헤더**가 자동으로 한국어 / 영문으로 전환됩니다.

### PDF 다운로드

| 엔드포인트 | 파라미터 | 설명 |
|------------|---------|------|
| `GET /reports/vulnerabilities/pdf` | `lang` | 취약점 현황 리포트 (통계 + 전체 목록) |
| `GET /reports/training/pdf` | — | 교육 이수 현황 리포트 |
| `GET /reports/policies/pdf` | `lang` | 보안 정책 리포트 (통계 + 전체 목록) |
| `GET /reports/assets/pdf` | `lang` | 자산 관리 리포트 (통계 + 전체 목록) |
| `GET /reports/incidents/pdf` | `lang` | 보안 인시던트 리포트 (통계 + 전체 목록) |
| `GET /reports/isms/pdf` | `year`, `lang` | ISMS-P 연도별 준수 현황 리포트 |
| `GET /reports/source-scan/{scanId}/pdf` | `lang` | 소스 취약점 점검(SAST) 결과 보고서 — 점검 1건(요약·심각도/카테고리 분포·발견 목록, 가로 A4) |
| `GET /reports/users/pdf` *(ADMIN)* | `lang` | 사용자 관리 리포트 (통계 + 전체 목록) |

### CSV 다운로드

| 엔드포인트 | 파라미터 | 설명 |
|------------|---------|------|
| `GET /reports/policies/csv` | `lang` | 보안정책 전체 데이터 (UTF-8 BOM, Excel 호환) |
| `GET /reports/vulnerabilities/csv` | `lang` | 취약점 전체 데이터 |
| `GET /reports/assets/csv` | `lang` | 자산 전체 데이터 |
| `GET /reports/incidents/csv` | `lang` | 인시던트 전체 데이터 |
| `GET /reports/users/csv` *(ADMIN)* | `lang` | 사용자 전체 데이터 |

모든 리포트 응답은 `Content-Disposition: attachment; filename*=UTF-8''...` 헤더와 함께 파일 다운로드됩니다.

---

## 관리자 (Admin) *(ADMIN)*

### GET /admin/users

```
?page=0&size=20
```

### GET /admin/users/simple *(MANAGER+)*

자산 등록 시 담당자 선택용 활성 사용자 목록 조회. 이름 오름차순 정렬.

```json
// Response
[
  { "id": 1, "name": "홍길동", "department": "보안팀" },
  { "id": 2, "name": "김철수", "department": "인프라팀" }
]
```

### PATCH /admin/users/:id

```json
{ "role": "MANAGER", "active": true, "department": "보안팀" }
```

> 응답에는 계정 상태 필드 `active`, `failedLoginAttempts`(비밀번호 오류 횟수), `locked`(잠김 여부), `lockedUntil`이 포함됩니다.

### POST /admin/users/:id/unlock *(ADMIN)*

비밀번호 오류 횟수를 0으로 초기화하고 계정 잠금을 해제합니다. (로그인 성공 시와 동일한 처리)

### GET /admin/users/bulk/template *(ADMIN)*

사용자 계정 일괄 등록용 엑셀 템플릿 다운로드 (`.xlsx`).  
컬럼: `이메일*`, `이름*`, `비밀번호*`, `역할`, `부서`

### POST /admin/users/bulk *(ADMIN, multipart/form-data)*

엑셀 파일 업로드 → 사용자 계정 일괄 등록. 비밀번호 BCrypt 자동 해싱 적용.  
이메일 중복 행은 오류 처리 후 나머지 행 계속 진행.

```json
// Response
{ "total": 5, "success": 4, "failed": 1,
  "errors": [{ "row": 3, "message": "이미 존재하는 이메일: user@example.com" }] }
```

### GET /admin/audit-logs

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `action` | String | 예: `VULN_CREATED`, `POLICY_UPDATED` |
| `resourceType` | String | 예: `VULNERABILITY`, `POLICY` |
| `userId` | Long | 특정 사용자 필터 |
| `dateFrom` | String | 시작 일시 (ISO 8601, 예: `2026-05-01T00:00:00`) |
| `dateTo` | String | 종료 일시 (ISO 8601, 예: `2026-05-31T23:59:59`) |

```json
// 응답 항목
{
  "id": 1, "userName": "System Admin", "action": "VULN_CREATED",
  "resourceType": "VULNERABILITY", "resourceId": 5,
  "detail": "Log4Shell 취약점", "ipAddress": "127.0.0.1",
  "createdAt": "2025-05-16 09:00:00"
}
```

---

## 성능관리 (Performance, v1.26.0)

지연 기준(기본 3초)을 넘긴 화면 요청·SQL 기록. 모두 **ADMIN** 권한이 필요합니다.

### GET /admin/performance/logs

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `logType` | String | `SCREEN`(화면 요청) / `SQL`. 미지정 시 전체 |
| `keyword` | String | 대상(URL·SQL 구문) 부분 일치 |
| `minMs` | Long | 최소 소요시간(ms) |
| `dateFrom` · `dateTo` | String | 발생 일시 범위 (ISO 8601) |

```json
// 응답 항목
{
  "id": 12, "logType": "SQL", "target": "select a1_0.id ... from assets a1_0",
  "detail": "select a1_0.id ... from assets a1_0 where ...",
  "durationMs": 4175, "thresholdMs": 3000,
  "username": "admin@example.com", "ipAddress": null,
  "httpMethod": null, "statusCode": null,
  "occurredAt": "2026-08-01T03:10:22"
}
```

`thresholdMs` 는 **기록 시점의 기준값**이므로 이후 기준을 바꿔도 당시 판단 근거가 유지됩니다.

### GET /admin/performance/stats

`{ "total": 128, "screenCount": 90, "sqlCount": 38, "maxDurationMs": 8421 }`

### GET · PUT /admin/performance/config

```json
{ "thresholdMs": 3000, "enabled": true, "sqlEnabled": true, "retentionDays": 30 }
```

PUT 은 보낸 필드만 반영합니다. `thresholdMs` 는 100~600000, `retentionDays` 는 1~3650 범위로 보정되며, 값은 `app_settings` 의 `perf.threshold_ms` · `perf.enabled` · `perf.sql_enabled` · `perf.retention_days` 에 저장됩니다.

### DELETE /admin/performance/logs

`days` 를 주면 그보다 오래된 기록만, 없으면 전체를 삭제하고 삭제 건수를 반환합니다.

---

## 페이지네이션

Spring Data Pageable을 사용합니다.

| 파라미터 | 설명 | 기본값 |
|----------|------|--------|
| `page` | 페이지 번호 (0-based) | 0 |
| `size` | 페이지 크기 | 엔드포인트별 상이 (20~50) |
| `sort` | 정렬 (예: `createdAt,desc`) | — |

페이지 응답 공통 구조:

```json
{
  "content": [...],
  "totalElements": 100,
  "totalPages": 5,
  "number": 0,
  "size": 20,
  "first": true,
  "last": false
}
```

---

## HTTP 상태 코드

| 코드 | 의미 |
|------|------|
| `200 OK` | 성공 |
| `201 Created` | 리소스 생성 성공 |
| `204 No Content` | 삭제 성공 |
| `400 Bad Request` | 유효성 검사 실패 |
| `401 Unauthorized` | 토큰 없음 또는 만료 |
| `403 Forbidden` | 권한 부족 |
| `404 Not Found` | 리소스 없음 |
| `409 Conflict` | 중복 (예: 이미 수신 확인한 정책) |

### 인증 오류 응답 상세 (v1.7.2+)

토큰이 없거나 만료된 경우 모든 보호된 엔드포인트는 일관된 JSON을 반환합니다.

```json
HTTP 401 Unauthorized
{
  "success": false,
  "data": null,
  "message": "Authentication required"
}
```

권한이 부족한 경우:

```json
HTTP 403 Forbidden
{
  "success": false,
  "data": null,
  "message": "Access denied"
}
```

> **v1.7.2 이전**: 인증 실패 시 Spring Security 기본 동작(`/login` 리다이렉트)으로 인해 500 오류가 발생할 수 있었습니다.  
> **v1.7.2 이후**: `AuthenticationEntryPoint` 및 `AccessDeniedHandler`가 명시적으로 설정되어 항상 JSON으로 응답합니다.

---

---

## 월간 보안점검 (Monthly Check)

### GET /monthly-checks
월-년 기준 점검 항목 목록 조회

**Query Parameters**
| 파라미터 | 필수 | 설명 |
|----------|------|------|
| `yearMonth` | ✅ | 조회할 년월 (예: `2026-06`) |

**Response**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "yearMonth": "2026-06",
      "priority": "HIGH",
      "category": "계정관리",
      "itemName": "퇴직자 계정 삭제 여부",
      "checkMethod": "인사시스템 퇴직자 명단과 시스템 계정 목록 비교",
      "checkExample": "퇴사자 A의 AD, VPN, AWS 계정 삭제 확인",
      "result": "INCOMPLETE",
      "notes": null,
      "sortOrder": 10,
      "createdAt": "2026-06-10T09:00:00",
      "updatedAt": "2026-06-10T09:00:00"
    }
  ]
}
```

**priority 값:** `HIGH` (상, 매월 필수) | `MEDIUM` (중, 매월 권장) | `LOW` (하, 분기/반기)  
**result 값:** `COMPLETED` | `INCOMPLETE` | `NA`

---

### GET /monthly-checks/summary
월별 점검 완료 현황 요약

**Query Parameters:** `yearMonth` (필수)

**Response**
```json
{
  "success": true,
  "data": {
    "total": 32,
    "completed": 15,
    "incomplete": 14,
    "na": 3
  }
}
```

---

### GET /monthly-checks/months
점검 기록이 있는 년월 목록 (내림차순)

**Response**
```json
{ "success": true, "data": ["2026-06", "2026-05", "2026-04"] }
```

---

### POST /monthly-checks
점검 항목 수동 등록

**Request Body**
```json
{
  "yearMonth": "2026-06",
  "priority": "HIGH",
  "category": "계정관리",
  "itemName": "퇴직자 계정 삭제 여부",
  "checkMethod": "인사시스템과 계정 목록 비교",
  "checkExample": "퇴사자 AD, VPN 계정 삭제 확인",
  "result": "INCOMPLETE",
  "notes": "",
  "sortOrder": 10
}
```

---

### POST /monthly-checks/defaults
기본 32개 점검 항목 일괄 등록

**Query Parameters:** `yearMonth` (필수)

기존 항목과 중복 추가됩니다. 빈 월에만 사용을 권장합니다.

---

### PATCH /monthly-checks/:id
점검 항목 수정 (부분 업데이트)

**Request Body** — 변경할 필드만 포함
```json
{
  "result": "COMPLETED",
  "notes": "2026-06-10 확인 완료"
}
```

---

### DELETE /monthly-checks/:id
점검 항목 삭제

### GET /monthly-checks/previous-month *(v1.8.0)*

대상 월 이전에 점검 내역이 있는 **가장 최근 월**을 반환합니다. 없으면 `data`가 `null`입니다.

**Query**: `yearMonth` (`YYYY-MM`)

### POST /monthly-checks/copy-previous *(v1.8.0)*

가장 최근 점검 월의 항목 구성을 대상 월로 복사합니다. 점검 결과는 미완료로 초기화하고 담당자는 승계하며, 증적·비고는 복사하지 않습니다. 대상 월에 기존 항목이 있으면 증적 파일과 함께 삭제한 뒤 복사합니다.

**Query**: `yearMonth` (`YYYY-MM`)
**Response**: 복사된 점검 항목 목록

---

## 개인정보보호 — 수탁사 (Contractor)

### GET /privacy/contractors
수탁사 목록 (점검 건수 포함)

### POST /privacy/contractors *(MANAGER+)*
수탁사 등록. 본문: `name`(필수), `businessNumber`, `representative`, `serviceType`(위탁업무), `subContractor`(재수탁사, v1.8.0), `contractStart`, `contractEnd`, `contactPerson`, `contactEmail`, `contactPhone`, `status`(`ACTIVE`/`INACTIVE`), `notes`

### PATCH /privacy/contractors/:id *(MANAGER+)*
수탁사 수정 (전달된 필드만 반영)

### DELETE /privacy/contractors/:id *(MANAGER+)*
수탁사 삭제 (점검 이력·증적 파일 함께 삭제)

### POST /privacy/contractors/parse-policy *(MANAGER+, v1.8.0)*

개인정보처리방침 URL을 읽어 위탁 표에서 수탁사·위탁업무·재수탁사를 추출합니다. **저장하지 않는 미리보기 전용**입니다.

**Request**
```json
{ "url": "https://example.com/privacy" }
```

**Response**
```json
{
  "sourceUrl": "https://example.com/privacy",
  "tableCount": 1,
  "items": [
    { "name": "OO정보통신(주)", "serviceType": "결제서비스 대행", "subContractor": null, "existing": false }
  ]
}
```

- `existing` — 이름이 같은 수탁사가 이미 등록되어 있는지 (공백 무시 비교)
- 개인정보 **제3자 제공** 표는 위탁 표와 구분하여 제외됩니다
- `rowspan`/`colspan` 병합 셀을 펼쳐 해석합니다
- 위탁 표를 자바스크립트로 렌더링하는 페이지는 인식하지 못하며, 표를 찾지 못하면 `400`과 안내 메시지를 반환합니다
- 내부망 주소(loopback·사설 IP)로의 요청은 차단됩니다

### POST /privacy/contractors/bulk *(MANAGER+, v1.8.0)*

수탁사 일괄 등록. 이미 등록된 이름과 요청 내 중복은 **건너뜁니다**(공백 무시 비교).

**Request**
```json
{ "items": [ { "name": "OO정보통신(주)", "serviceType": "결제서비스 대행", "subContractor": null, "status": "ACTIVE" } ] }
```

**Response**
```json
{ "created": 1, "skipped": 0, "skippedNames": [] }
```

---

## 개인정보 마스킹 기준 (PI Masking, v1.28.0)

관리 > 코드관리 > 개인정보 유형별 항목관리에 등록된 항목별 마스킹 기준입니다. 목록 화면의 개인정보 표시를 가리는 데 사용합니다.

### GET /codes/pi-masking

활성 상태이고 마스킹 방식이 등록된 개인정보 항목(`PI_*` 그룹)의 기준을 반환합니다. 인증된 모든 사용자가 조회할 수 있습니다.

```json
[
  { "groupCode": "PI_CONTACT", "label": "이메일 주소", "maskingType": "부분 마스킹",
    "maskingRule": "아이디 앞 3자리만 표시하고 나머지는 * 로 대체, 도메인은 표시", "maskingExample": "abc****@test.com" }
]
```

### POST /codes/pi-masking/reveal *(ADMIN)*

목록 화면의 마스킹 해제(원문 열람)를 감사 로그(`PI_UNMASK`)에 기록합니다. 응답은 `204 No Content`.

```json
{ "screen": "수탁사 관리", "reason": "목록 화면 원문 열람" }
```

---

## 모의 악성메일 훈련 (Phishing)

### GET /phishing/send-logs *(v1.8.0)*

피싱 메일 **발송 처리 결과 로그**를 최신순으로 조회합니다. 발송이 시도된(결과가 기록된) 대상만 반환합니다.

**Response 항목**: `campaignId`, `campaignName`, `targetName`, `targetEmail`, `department`, `sendStatus`(`SUCCESS`/`FAILED`), `sendError`, `sentAt`, `openedAt`, `clickedAt`

---

## 비상연락망 (Emergency Contacts)

상황별 연락 계통과 연락처. 조회는 인증 사용자, 그룹·연락처 쓰기는 `MANAGER` 이상, 그룹 삭제는 `ADMIN`입니다.

| Method | Path | 설명 |
|--------|------|------|
| GET | `/emergency-contacts/groups` | 연락 그룹 목록 — 연락처를 `contactOrder` 순으로 포함 |
| GET | `/emergency-contacts/groups/{id}` | 그룹 상세 |
| POST | `/emergency-contacts/groups` | 그룹 등록 *(MANAGER+)* |
| PATCH | `/emergency-contacts/groups/{id}` | 그룹 수정 *(MANAGER+)* |
| PATCH | `/emergency-contacts/groups/{id}/toggle` | 활성/비활성 전환 *(MANAGER+)* |
| DELETE | `/emergency-contacts/groups/{id}` | 그룹 삭제 *(ADMIN)* — 연락처가 남아 있으면 거부 |
| POST | `/emergency-contacts/contacts` | 연락처 등록 *(MANAGER+)* |
| PATCH | `/emergency-contacts/contacts/{id}` | 연락처 수정 — `groupId` 변경 시 그룹 이동 *(MANAGER+)* |
| PATCH | `/emergency-contacts/contacts/{id}/toggle` | 활성/비활성 전환 *(MANAGER+)* |
| DELETE | `/emergency-contacts/contacts/{id}` | 연락처 삭제 *(MANAGER+)* |

**그룹 본문**: `name`, `contactType`(`INTERNAL`/`EXTERNAL`/`PARTNER`), `description`, `sortOrder`(생략 시 마지막 순서)
**연락처 본문**: `groupId`, `name`, `organization`, `department`, `position`, `roleName`, `contactOrder`(생략 시 그룹 내 마지막), `mobile`, `officePhone`, `email`, `available24h`, `note`

> 목록 화면의 휴대전화·이메일 마스킹 해제는 다른 화면과 달리 **MANAGER 도 가능**합니다(해제한 화면에서만 유효, `POST /codes/pi-masking/reveal` 로 감사 기록). 마스킹은 화면 표시 통제이며, API 응답 자체는 읽기 권한이 있는 사용자에게 복호화된 값을 반환합니다.
>
> `mobile`·`email` 은 `EncryptedStringConverter`(AES-256-GCM)로 저장·복호화됩니다. `officePhone` 은 기관·부서 대표번호로 쓰여 평문으로 둡니다. 감사 로그에는 그룹명·이름만 남고 연락처 값은 기록하지 않습니다.
>
> 기본 연락 계통 6종과 외부 신고기관 연락처 4건(KISA 118, 경찰청 182, 국정원 111)은 `EmergencyContactInitializer` 가 테이블이 비어 있을 때만 시드합니다.

---

## 재해복구·BCP 훈련 (BCP)

재해 시나리오 기반의 재해복구(DR)·업무연속성(BCP) 훈련. 조회는 인증 사용자, 쓰기는 `MANAGER` 이상, 삭제는 `ADMIN`입니다.

### 훈련 시나리오

| Method | Path | 설명 |
|--------|------|------|
| GET | `/bcp/scenarios` | 시나리오 목록 (대응 단계 포함) |
| GET | `/bcp/scenarios/{id}` | 시나리오 상세 |
| POST | `/bcp/scenarios` | 시나리오 등록 *(MANAGER+)* |
| PATCH | `/bcp/scenarios/{id}` | 시나리오 수정 — `steps`는 전량 교체 *(MANAGER+)* |
| PATCH | `/bcp/scenarios/{id}/toggle` | 활성/비활성 전환 *(MANAGER+)* |
| DELETE | `/bcp/scenarios/{id}` | 삭제 *(ADMIN)* — 실시 이력이 있으면 거부 |

**요청 본문**: `name`, `category`, `difficulty`(`EASY`/`MEDIUM`/`HARD`), `targetSystem`, `rtoMinutes`, `rpoMinutes`, `situation`, `objective`, `description`, `steps[]`
**단계(step)**: `stepOrder`, `title`, `roleName`, `action`, `targetMinutes`, `successCriteria`

> 시나리오 예제 7종은 `BcpScenarioInitializer`가 테이블이 비어 있을 때만 시드합니다.

### 훈련 실시

| Method | Path | 설명 |
|--------|------|------|
| GET | `/bcp/exercises` | 훈련 목록 (단계 집계 포함) |
| GET | `/bcp/exercises/{id}` | 훈련 상세 + 단계별 수행 결과 |
| POST | `/bcp/exercises` | 훈련 등록 — 시나리오 단계를 복사 *(MANAGER+)* |
| POST | `/bcp/exercises/{id}/start` | 훈련 시작 (`DRAFT` → `RUNNING`) *(MANAGER+)* |
| PATCH | `/bcp/exercises/{id}/steps/{stepId}` | 단계 수행 결과 기록 *(MANAGER+)* |
| POST | `/bcp/exercises/{id}/complete` | 완료 처리 — 달성률·판정 산출 *(MANAGER+)* |
| POST | `/bcp/exercises/{id}/cancel` | 취소 *(MANAGER+)* — 완료된 훈련은 불가 |
| DELETE | `/bcp/exercises/{id}` | 삭제 *(ADMIN)* |

**등록 본문**: `name`, `scenarioId`, `method`(`TABLETOP`/`SIMULATION`/`FAILOVER`), `plannedAt`, `leaderName`, `participants`, `participantCount`, `description`
**단계 기록 본문**: `result`(`PASS`/`PARTIAL`/`FAIL`/`PENDING`), `actualMinutes`, `note`
**완료 본문**: `actualRtoMinutes`, `actualRpoMinutes`, `summary`, `improvement`

완료 시 단계 결과를 가중 평균(`PASS` 1.0 · `PARTIAL` 0.5 · `FAIL` 0)하여 `score`(0~100)를 산출하고, 80% 이상 `PASS` · 60% 이상 `PARTIAL` · 그 미만 `FAIL`로 `result`를 판정합니다. 미기록(`PENDING`) 단계가 남아 있으면 완료 처리가 거부됩니다.

**목록 응답 항목**: `scenarioName`, `category`, `method`, `status`, `score`, `result`, `rtoMinutes`/`actualRtoMinutes`/`rtoMet`, `rpoMinutes`/`actualRpoMinutes`, `totalSteps`, `passedSteps`, `partialSteps`, `failedSteps`, `pendingSteps`, `summary`, `improvement`

---

## 메일서버 설정 (Mail Config) *(ADMIN, v1.8.0)*

발송 메일서버(SMTP) 설정. 활성화 시 이 설정으로 발송하고, 비활성/미설정 시 `application.yml`의 `spring.mail.*`로 폴백합니다.

### GET /admin/mail-config
설정 조회. 비밀번호는 **마스킹**되어 반환됩니다.

### PUT /admin/mail-config
설정 저장. 본문: `host`, `port`, `username`, `password`, `fromAddress`, `fromName`, `useAuth`, `useStartTls`, `enabled`

### POST /admin/mail-config/test
연결 테스트 — 지정한 수신 주소로 테스트 메일을 발송하고 성공/실패와 사유를 반환합니다.

---

## 소스 취약점 점검 (Source Scan / SAST)

GitHub 저장소를 대상으로 의존성(Dependabot)·코드(Code scanning)·시크릿(Secret scanning) 알림과 내장 OWASP 정적분석(SAST)을 수행합니다.

### GET /admin/github-config *(ADMIN)*

GitHub 연동 설정 조회. 토큰은 마스킹되어 반환됩니다 (`tokenStored`, `tokenMasked`, `apiBaseUrl`, `owner`).

### PUT /admin/github-config *(ADMIN)*

연동 설정 저장. `token`이 빈 값이면 기존 토큰 유지, `"-"`이면 삭제.

```json
{ "token": "ghp_xxx", "apiBaseUrl": "https://api.github.com", "owner": "monosun" }
```

### POST /admin/github-config/test *(ADMIN)*

토큰으로 GitHub 연결을 시험하고 로그인 계정을 반환.

### GET /source-scan/repos *(MANAGER+)*

토큰으로 접근 가능한 저장소 목록 (`fullName`, `privateRepo`, `defaultBranch` 등).

### POST /source-scan/run *(MANAGER+)*

점검 실행. 4개 카테고리(의존성·코드·시크릿·SAST)를 함께 점검하고 상세 결과를 반환.

```json
{ "repository": "owner/repo" }
```

응답 `scan`에 카테고리별 발견 수(`dependencyCount`·`codeCount`·`secretCount`·`sastCount`)와 심각도별 집계, `findings[]`에 항목별 상세(`category`·`severity`·`title`·`identifier`(OWASP·CWE)·`location`(파일:라인)·`htmlUrl`)가 포함됩니다.

### GET /source-scan/scans

점검 이력 (페이지네이션).

### GET /source-scan/scans/:id

점검 상세 (scan + findings).

### DELETE /source-scan/scans/:id *(MANAGER+)*

점검 이력 삭제.

---

## 보안성 심의 (Security Design Review)

신규 시스템 구축·변경 시 설계 단계의 보안 요구사항 충족 여부를 검토한다.
심의 요청은 로그인 사용자 누구나 가능하고, **검토·결과 등록·삭제는 MANAGER+** 이다.

### GET /security-reviews

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `status` | String | `REQUESTED`, `IN_REVIEW`, `REVISION`, `COMPLETED` |
| `reviewType` | String | `NEW`, `CHANGE`, `INTEGRATION`, `DECOMMISSION` |
| `keyword` | String | 제목 · 시스템명 · 요청부서 |
| `page`, `size` | int | 페이지네이션 |

응답 항목에 검토 진행 현황(`itemTotal`, `itemChecked`, `itemFailed`)이 포함된다(목록에서는 `items` 생략).

### GET /security-reviews/summary

상태별·결과별 건수: `requested`, `inReview`, `revision`, `completed`, `approved`, `conditional`, `rejected`.

### GET /security-reviews/:id

심의 상세 + 검토 체크리스트(`items`) 전체.

### POST /security-reviews *(multipart/form-data)*

심의 요청 등록. 등록 시 **기본 검토 체크리스트 20개 항목이 자동 생성**된다.

| 필드 | 타입 | 설명 |
|------|------|------|
| `title` | String | 심의 제목 (필수) |
| `systemName` | String | 대상 시스템·서비스 (필수) |
| `reviewType` | String | 심의 구분 (기본 `NEW`) |
| `department` | String | 요청 부서 |
| `description` | String | 구축·변경 개요 |
| `handlesPersonalData` | Boolean | 개인정보 처리 여부 |
| `internetFacing` | Boolean | 인터넷 공개 여부 |
| `targetDate` | Date | 오픈(적용) 예정일 |
| `file` | File | 설계서 첨부 (선택) |

### PATCH /security-reviews/:id *(MANAGER+)*

요청 정보·진행 상태 수정. `status`를 `COMPLETED`로 직접 바꿀 수 없다(결과 등록 API 사용 — `400`).

### POST /security-reviews/:id/decision *(MANAGER+)*

심의 결과 확정. **미검토(PENDING) 항목이 남아 있으면 `400`** 으로 막는다.

```json
{ "decision": "APPROVED", "reviewComment": "운영 이관 전 취약점 점검 결과 제출 조건" }
```

`decision`: `APPROVED`(승인) · `CONDITIONAL`(조건부 승인) · `REJECTED`(반려)

### 검토 항목 · 첨부

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `POST` | `/security-reviews/{id}/items` | 검토 항목 추가 (MANAGER+) |
| `PATCH` | `/security-reviews/items/{itemId}` | 검토 결과(`PASS`/`FAIL`/`NA`)·의견 저장 (MANAGER+) |
| `DELETE` | `/security-reviews/items/{itemId}` | 검토 항목 삭제 (MANAGER+) |
| `POST` | `/security-reviews/{id}/file` | 설계서 첨부·교체 (multipart) |
| `GET` | `/security-reviews/{id}/file` | 설계서 다운로드 |
| `DELETE` | `/security-reviews/{id}` | 심의 삭제 (MANAGER+) |

첫 검토 결과가 입력되면 상태가 `REQUESTED` → `IN_REVIEW` 로 자동 전환된다.

---

## 관련 사이트 (Related Site, v1.23.0)

보안·개인정보 업무에 참고하는 외부 사이트를 등록하고, 각 사이트의 최신 게시물(RSS/Atom) 또는
사이트 소개문(og 메타)을 가져와 보관한다. **조회는 로그인 사용자, 등록·수정·삭제·새로고침은 MANAGER+** 이다.

### GET /related-sites

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `keyword` | String | 사이트명 · 주소 · 분류 · 설명 · 수집 소개문 |
| `category` | String | 분류 (예: `유관기관`) |
| `activeOnly` | Boolean | 기본 `true`. `false` 면 미사용 사이트도 포함 |

응답은 사이트별로 가져온 게시물(`items`, 최대 5건)을 함께 담는다.

```json
{
  "id": 2,
  "name": "KISA 취약점 정보포털(KNVD) 보안공지",
  "url": "https://knvd.krcert.or.kr",
  "feedUrl": "https://knvd.krcert.or.kr/rss/security/notice",
  "category": "침해사고·취약점",
  "description": "국내 보안공지 — 제품 취약점 패치·긴급 대응 권고",
  "sortOrder": 20,
  "active": true,
  "fetchStatus": "FEED",
  "fetchMessage": null,
  "fetchedSummary": null,
  "lastFetchedAt": "2026-07-27T00:49:50",
  "items": [
    {
      "id": 11,
      "title": "Zoom 제품 보안 업데이트 권고",
      "link": "https://knvd.krcert.or.kr/detailSecNoticeView.do?bulletin_writing_sequence=...",
      "summary": "□ 개요 o Zoom社는 자사 제품에서 발생하는 취약점을 해결한 보안 업데이트 발표",
      "publishedText": "Mon, 20 Jul 2026 03:52:28 GMT",
      "publishedAt": "2026-07-20T12:52:28"
    }
  ]
}
```

`fetchStatus` 값

| 값 | 의미 |
|----|------|
| `NONE` | 아직 한 번도 가져오지 않음 |
| `FEED` | 게시물 목록(피드) 수집 성공 |
| `SUMMARY` | 피드가 없어 사이트 소개문(og 메타)만 수집 |
| `EMPTY` | 접속했으나 가져올 내용 없음 |
| `ERROR` | 접속 실패 (망 차단 · 타임아웃 · 오류) — 사유는 `fetchMessage` |

### POST /related-sites *(MANAGER+)*

| 필드 | 타입 | 설명 |
|------|------|------|
| `name` | String | 사이트 이름 (필수) |
| `url` | String | 홈페이지 주소 (필수, 중복 불가). `http(s)://` 생략 시 `https://` 로 저장 |
| `feedUrl` | String | RSS/Atom 주소. 비우면 홈페이지에서 자동 탐색 |
| `category` | String | 분류 |
| `description` | String | 설명 |
| `sortOrder` | int | 정렬 순서 (비우면 자동 부여) |
| `active` | Boolean | 사용 여부 (기본 `true`) |

등록 직후 해당 사이트의 내용을 한 번 가져온다(수집 실패해도 등록은 유지).

### 그 외

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `GET` | `/related-sites/{id}` | 사이트 1건 + 게시물 |
| `PATCH` | `/related-sites/{id}` | 사이트 수정 (MANAGER+). 주소·피드 변경 시 즉시 재수집 |
| `DELETE` | `/related-sites/{id}` | 삭제 (MANAGER+). 가져온 게시물도 함께 삭제 |
| `POST` | `/related-sites/{id}/refresh` | 사이트 1건 새로고침 (MANAGER+) |
| `POST` | `/related-sites/refresh` | 사용 중인 사이트 전체 새로고침 (MANAGER+) — `{ total, succeeded, failed, items, sites }` |

전체 수집은 `relatedsite.refresh-cron`(기본 `0 10 7 * * *`, 환경변수 `RELATED_SITE_CRON`) 스케줄로도 하루 한 번 실행된다.

---

## 위협 카탈로그 기본 항목 (Threat Defaults)

위험평가에 사용할 위협 마스터. 기본 140건이 시드되며 관리>코드관리>위협 기본 항목에서 관리합니다.

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `GET` | `/threats/defaults?page=0&size=20` | 기본 항목 목록 (페이지네이션) |
| `GET` | `/threats/defaults/check` | 위협 관리로 복사 가능 여부·기본 항목 수 |
| `POST` | `/threats/defaults` | 기본 항목을 위협 관리(`threats`)로 복사 |
| `POST` | `/threats/defaults/item` | 기본 항목 추가 (ADMIN) |
| `PATCH` | `/threats/defaults/{id}` | 기본 항목 수정 (ADMIN) |
| `DELETE` | `/threats/defaults/{id}` | 기본 항목 1건 삭제 (ADMIN) |
| `POST` | `/threats/defaults/bulk-delete` | **선택 일괄 삭제 (ADMIN, v1.31.0)** |

### POST /threats/defaults/bulk-delete *(ADMIN, v1.31.0)*

```json
// Request
{ "ids": [12, 34, 56] }

// Response
{ "deleted": 3 }
```

존재하는 항목만 삭제하고 실제 삭제 건수를 반환합니다. `ids` 가 비어 있으면 `400`.

> **중복 판정 기준(v1.31.0)**: 기본 항목의 유니크 제약은 `(위협명, 유형, 카테고리, 발생가능성, 잠재영향)` 입니다. 같은 위협명이라도 위험도 조합이 다르면 별도 항목으로 등록할 수 있습니다.

---

## 알림 설정 (Notification Config) *(ADMIN)*

승인 알림(계정 삭제·ADMIN 승격 요청)의 수신 방식과 Slack 연동을 관리합니다.

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `GET` | `/admin/notification-config` | 설정 조회 — 토큰은 마스킹 |
| `PUT` | `/admin/notification-config` | 설정 저장 |
| `POST` | `/admin/notification-config/slack/test` | **Slack 연동 테스트 (v1.31.0)** |

### GET /admin/notification-config

```json
{
  "method": "SLACK",              // EMAIL | SLACK | BOTH | INBOX
  "approvalEmail": "sec@example.com",
  "slackWebhookUrl": "https://hooks.slack.com/services/...",
  "slackMode": "SOCKET",          // WEBHOOK | SOCKET
  "slackChannel": "#security-alert",
  "slackBotTokenStored": true,
  "slackBotTokenMasked": "xoxb-1****ab",
  "slackAppTokenStored": false,
  "slackAppTokenMasked": ""
}
```

### PUT /admin/notification-config

```json
{
  "method": "SLACK",
  "approvalEmail": "sec@example.com",
  "slackMode": "SOCKET",
  "slackChannel": "#security-alert",
  "slackBotToken": "xoxb-...",    // 빈 값이면 기존 유지, "-" 이면 삭제
  "slackAppToken": ""
}
```

전달한 키만 반영되며, 응답은 `GET` 과 동일한 형식(토큰 마스킹)입니다.

### POST /admin/notification-config/slack/test *(v1.31.0)*

저장된 설정 기준으로 연동을 점검합니다(요청 본문 없음).

- `WEBHOOK` — Webhook URL로 테스트 메시지 발송
- `SOCKET` — `auth.test`(봇 토큰) → `apps.connections.open`(앱 레벨 토큰, 설정된 경우) → 채널 지정 시 `chat.postMessage` 테스트 발송

```json
{ "mode": "SOCKET", "success": true,
  "message": "봇 토큰 정상 (워크스페이스: monosun, 봇: secportal) / 소켓 연결 발급 정상 (앱 레벨 토큰 유효) / #security-alert 채널로 메시지를 전송했습니다." }
```

> 소켓 모드에서도 **발송은 Web API `chat.postMessage`(봇 토큰)** 를 사용합니다. Socket Mode의 WebSocket은 Slack → 앱 방향(이벤트 수신) 전송 수단이라 상시 리스너는 두지 않으며, 앱 레벨 토큰은 소켓 연결 발급 가능 여부 점검에만 사용합니다.

---

## 관련 문서

- [FAQ — 자주 묻는 오류](faq.md)
- [아키텍처](architecture.md)
- [개발 환경 설정](development-setup.md)
