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
      "acknowledgmentCount": 5, "createdAt": "2025-01-01 09:00:00"
    }
  ],
  "totalElements": 10, "totalPages": 1
}
```

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

### POST /policies/:id/acknowledge

정책 수신 확인. 중복 확인 시 409 반환.

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
  { "id": 1, "userId": 1, "userName": "System Admin", "content": "조치 시작함", "createdAt": "2025-05-10 14:00:00" }
]
```

### POST /vulnerabilities/:id/comments

```json
{ "content": "취약점 패치 완료" }
```

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
  "evidenceCount": 2, "latestStatus": "COMPLIANT"
}
```

### GET /isms/items/:id

단일 항목 조회.

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

### GET /isms/export/csv *(MANAGER+)*

연도별 전체 증적 CSV 다운로드 (UTF-8 BOM, Excel 호환).

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `year` | int | 연도 (생략 시 현재 연도) |

컬럼: `항목코드`, `항목명`, `섹션`, `도메인`, `증적제목`, `증적내용`, `파일명`, `준수상태`, `등록자`

### GET /isms/import/template *(MANAGER+)*

일괄 등록용 엑셀 템플릿 다운로드 (`.xlsx`, 3개 시트).

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
| `keyword` | String | 코스명 검색 |

응답에 현재 사용자의 `completed`, `score` 포함.

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
// Request: { questionId: answer, ... }
{ "1": "A", "2": "B", "3": "C" }

// Response
{ "score": 100, "passed": true, "correctCount": 3, "totalCount": 3 }
```

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
  "correctAnswer": "A",
  "explanation": "개인정보 수집 시 정보주체의 동의가 필요합니다."
}
```

### PATCH /quiz-bank/:id

### DELETE /quiz-bank/:id

### GET /quiz-bank/bulk/template

엑셀 일괄 등록 템플릿 다운로드.

### POST /quiz-bank/bulk

`multipart/form-data`의 `file`로 엑셀 업로드 — 성공/실패 건수와 행별 오류 반환.

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

---

## 관련 문서

- [FAQ — 자주 묻는 오류](faq.md)
- [아키텍처](architecture.md)
- [개발 환경 설정](development-setup.md)
