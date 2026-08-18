# KSecPortal v1.36.0 릴리즈 노트

**릴리즈 일자**: 2026-08-18

ISMS-P 증적관리의 **이행 가이드**에 인증기준별 **예시 증적자료명**을 추가했습니다.
"이 항목에는 어떤 문서를 올려야 하는가"를 화면에서 바로 확인할 수 있으며,
**101개 항목 전부에 항목당 5건**(총 505건)의 기본값을 제공합니다.

---

## 1. 이행 가이드에 예시 증적자료 표시

증적관리 > 통제항목 팝업에서 `💡 이행 가이드`를 펼치면, 기존 가이드 문장 아래에
**📄 예시 증적자료** 목록이 표시됩니다.

| 항목 | 예시 증적자료 |
|------|--------------|
| 1.1.1 경영진의 참여 | 정보보호위원회 회의록 · 경영진 정보보호 현황 보고자료 · 정보보호 활동 결재문서 · 최고경영자 지시사항 문서 · 정보보호 조직·예산 승인서 |
| 2.5.6 접근권한 검토 | 접근권한 검토 계획서 · 시스템별 권한 부여 현황표 · 권한 검토 결과보고서 · 부적절 권한 회수·조정 기록 · 권한 검토 승인 문서 |
| 2.9.5 로그 및 접속기록 점검 | 로그 점검 계획서 · 접속기록 점검 결과보고서 · 이상행위 분석 및 소명 기록 · 점검 결과 조치 이력 · 개인정보 접속기록 월 1회 점검 증빙 |
| 3.4.1 개인정보 파기 | 개인정보 파기 절차서 · 보유기간 만료 대상 목록 · 개인정보 파기 관리대장 · 파기확인서 및 파기 사진 · 자동 삭제 배치 실행 로그 |

- 화면 폭에 따라 **1열 또는 2열** 불릿으로 표시되며, 가이드를 편집하는 중에도 계속 보입니다.
- 연도와 무관한 **항목 속성**이라 어느 연도에서 보든 동일하게 표시됩니다.

## 2. 이행 가이드와 별도 필드 — 가이드를 고쳐도 예시는 남습니다

예시 증적자료명은 `isms_items.evidence_examples` 컬럼에 **이행 가이드와 분리 저장**합니다.

- 사용자가 이행 가이드 문장을 다시 쓰거나 비워도 예시 목록은 그대로 유지됩니다.
- 반대로 예시를 조직 실정에 맞게 고쳐도 가이드 문장은 영향을 받지 않습니다.

## 3. 코드관리에서 항목별 편집

관리 > 코드관리 > `ISMS-P 101항목` 탭에서 항목별로 편집합니다.

- 수정 팝업에 **예시 증적자료명** 입력란이 추가되었습니다. **한 줄에 자료명 하나**가 규칙입니다.
- 목록에 **예시 증적자료 건수** 열이 추가되어 어떤 항목이 비어 있는지 한눈에 보입니다.

## 4. 일괄등록 Excel 템플릿 반영

- `증적입력` 시트 맨 뒤에 **`예시 증적자료(참고)`** 열이 추가되어, 행마다 그 항목의 예시가 채워집니다.
- `항목목록(참고)` 시트에도 항목별 **예시 증적자료** 열이 추가되었습니다.
- 이 열은 **읽기 전용 참고 열**로 업로드 시 읽지 않습니다.
  **기존 0~5 열(항목코드·증적제목·증적내용·이행가이드·파일명/경로·준수상태)의 순서는 바꾸지 않았으므로,
  이전에 내려받은 템플릿도 그대로 업로드할 수 있습니다.**

---

## 변경 파일

| 파일 | 내용 |
|------|------|
| `backend/src/main/resources/isms_items.json` | 101개 항목에 `evidenceExamples` 시드 추가 (항목당 5건, 총 505건) |
| `isms/entity/IsmsItem.java` | `evidenceExamples` 컬럼(TEXT, 줄바꿈 구분) 추가 |
| `isms/dto/IsmsDto.java` | `ItemResponse`·`ItemDefaultsRequest` 에 필드 추가 |
| `isms/service/IsmsDataInitializer.java` | 시드 배열을 줄바꿈 문자열로 합쳐 저장, 빈 항목만 백필 |
| `isms/service/IsmsService.java` | 기본값 저장 처리, Excel 템플릿·참고 시트에 예시 열 추가 |
| `views/isms/IsmsEvidencePanel.vue` | 이행 가이드 영역에 예시 증적자료 목록 표시 |
| `views/admin/CodeManagementView.vue` | 예시 증적자료명 입력란·건수 열 |
| `db/init/06_isms.sql` | 신규 설치 스키마에 `evidence_examples` 컬럼 반영 |
| `db/migration/v1.36.0_isms_evidence_examples.sql` | 기존 DB용 컬럼 추가 마이그레이션 |
| `docs/api.md`, `docs/user-manual.md`, `README.md` | 문서 갱신 |

## API 변경

- `GET /api/isms/items`, `GET /api/isms/items/:id` 응답에 **`evidenceExamples`** 필드 추가
  (줄바꿈으로 구분된 자료명 목록, 값이 없으면 `null`)
- `PATCH /api/isms/items/:id/defaults` 요청에 **`evidenceExamples`** 필드 추가 *(MANAGER+)*

## DB 변경

`isms_items` 에 `evidence_examples TEXT` 컬럼이 추가됩니다.

- `ddl-auto: update` 로도 컬럼은 자동 생성되지만, **컬럼 코멘트를 남기려면** 마이그레이션을 적용하세요.
- 값은 기동 시 `IsmsDataInitializer` 가 **비어 있는 항목에만** 채웁니다(seed-when-empty).
  이미 입력해 둔 값은 덮어쓰지 않습니다.

## 업그레이드

```bash
git pull

# (선택) 컬럼 코멘트까지 반영하려면 — 미적용해도 ddl-auto 가 컬럼을 만듭니다
docker compose exec -T db mysql -usecportal -psecportal123 secportal \
  < db/migration/v1.36.0_isms_evidence_examples.sql

docker compose build backend frontend
docker compose up -d backend frontend
```

- 백엔드 기동 로그에 `ISMS-P items seeded: 0 new, 101 backfilled` 가 찍히면 시드가 반영된 것입니다.
- 배포 후 브라우저 **하드 새로고침(Ctrl+Shift+R)** 이 필요합니다.
- 운영 환경에서는 배포 전 **DB 백업**을 먼저 수행하세요.
