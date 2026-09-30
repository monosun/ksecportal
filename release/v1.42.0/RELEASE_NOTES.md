# KSecPortal v1.42.0 릴리즈 노트

**릴리즈 일자**: 2026-10-01

**정보보호 관리체계 > ISMS 결함관리**를 새로 만들어, ISMS-P 인증심사에서 지적받은 결함과
연도별 결함 조치 보고서를 한 곳에서 관리할 수 있게 했습니다.
여기에 등록한 결함은 **보안 운영 > 보안 결함사항**에 자동으로 반영되어,
보안 결함사항 화면은 **ISMS-P 심사 결함 + 그 외 보안결함**을 함께 보는 화면이 되었습니다.

---

## 1. ISMS 결함관리 (신규)

ISMS-P 증적관리와 같은 **연도 단위** 화면입니다. 한 연도는 **결함 N건 + 결함 조치 보고서 1건**으로 구성됩니다.

### 연도 선택과 요약

- 오른쪽 위 **‹ 2026년 ›** 로 연도를 옮기면 목록·요약·보고서가 함께 바뀝니다.
- 요약 카드: **전체 결함 / 미조치 / 조치중 / 조치완료 / 기한초과 / 조치율**
  - 기한초과 — 조치완료가 아닌데 조치기한이 지난 건수(목록에서도 빨간 글씨)
  - 조치율 — `조치완료 ÷ 전체`
- **CSV 다운로드** — 해당 연도 결함 전체(엑셀에서 한글 깨짐 없음)

### 결함 내역 탭

| 항목 | 내용 |
|------|------|
| 심사 구분 | 최초심사 / 사후심사 / 갱신심사 / 내부심사 / 기타 |
| 결함 구분 | 결함 / 권고 / 개선사항 |
| 인증기준 | **ISMS-P 101개 인증기준** 드롭다운 — 기준 코드·기준명·분야 자동 입력(직접 입력 가능) |
| 결함 기록 | 결함 제목(필수)·결함 내용·원인·조치 계획·조치 내용·재발방지 대책 |
| 관리 | 중요도(매우높음~낮음)·조치 상태(미조치/조치중/조치완료/보류)·조치 기한·완료일·담당 부서·담당자·표시 순서 |
| 첨부 | 결함 통보서·조치 증빙 1건 |

상단 필터(심사구분·결함구분·중요도·조치상태)와 검색어(결함번호·제목·내용·인증기준 코드)로 좁혀 보고,
행을 클릭하면 상세 팝업에서 결함 내용부터 재발방지 대책까지 구분해 보여 줍니다.

### 보고서 내용 탭

연도당 **결함 조치 보고서 1건** — 심사 개요(제목·심사 구분·심사기관·심사 기간·심사원·인증 범위·보고일·보고자),
심사 총평, 보고서 본문, 종합 의견·결론, 원본 보고서 첨부. 탭 옆 배지로 **작성됨 / 미작성**을 표시합니다.
제목을 비우면 `{연도}년 ISMS-P 결함 조치 보고서` 로 채워집니다.

### 권한

조회는 메뉴 권한이 있는 사용자, 등록·수정·삭제는 **MANAGER 이상**입니다.
RBAC 메뉴 키 `isms_defects` 가 추가되었습니다(권한관리 화면에서 역할별로 지정).

## 2. 보안 결함사항 — ISMS-P 결함 자동 연동

- ISMS 결함관리에서 결함을 **등록·수정·삭제하면 보안 결함사항에 즉시 반영**됩니다(결함 1건 = 결함사항 1건).
  - 결함 내용·원인 → 결함 상세, 조치 계획·내용·재발방지 대책 → 시정조치, 중요도 → 위험도, 조치 상태 → 처리 상태로 옮겨집니다.
  - 첨부파일은 복제하지 않고 원본 파일을 함께 가리킵니다.
- 목록에 **출처** 열(ISMS-P 결함 / 직접 등록 배지 + `ISMS-P 갱신심사 · 결함-01` 같은 원본 표시)과 **출처 필터**가 생겼습니다.
- 가져온 결함은 이 화면에서 **읽기 전용**입니다(API 도 `PATCH`·`DELETE` 를 400 으로 거부). `원본 ›` 버튼이나 상세의 **원본 열기**로 ISMS 결함관리의 해당 결함으로 이동합니다.
- ISMS-P 와 무관한 보안결함(내부점검·외부 진단·감사 지적 등)은 종전처럼 **직접 등록**합니다.
- 기능 추가 전에 등록된 결함이나 동기화가 어긋난 건은 **서버 기동 시 자동으로 보충 반영**됩니다.

## 3. 버그 수정

- **보안 결함사항 필터가 동작하지 않던 문제** — 연도·심사유형·출처·위험도·상태 드롭다운을 바꾸면
  목록 요청이 `page=[object Event]` 로 나가 서버가 400 으로 거부했고, 목록이 갱신되지 않았습니다. 이제 선택 즉시 목록이 바뀝니다.

---

## 변경 파일

| 파일 | 변경 |
|------|------|
| `ismsdefect/**` (신규) | 결함·조치 보고서 엔티티/리포지토리/서비스/컨트롤러, `IsmsDefectFindingSync`(결함사항 동기화), `IsmsDefectFindingBackfill`(기동 시 보충) |
| `secfinding/**` | `sourceType`·`sourceDefectId`·`sourceLabel` 추가, 출처 필터, 연동 건 수정·삭제 거부 |
| `rbac/MenuKeys.java`, `RbacManagementView.vue`, `navMenu.js` | 메뉴 키 `isms_defects` |
| `frontend/src/views/isms/IsmsDefectView.vue` (신규), `router/index.js`, `api/index.js`, `i18n/*.json` | ISMS 결함관리 화면·라우트·API |
| `frontend/src/views/secfinding/SecFindingView.vue` | 출처 열·필터·원본 이동, 필터 변경 시 400 오류 수정 |
| `db/init/07_extended_schema.sql`, `db/init/13_isms_defect.sql` | 신규 설치 스키마 |
| `db/migration/v1.42.0_*.sql` | 운영 DB 마이그레이션 |
| `db/sample/isms_defect_sample.sql` | 화면 확인용 예시 데이터(선택) |
| `README.md`, `docs/api.md`, `docs/user-manual.md`, `frontend/public/help/user-manual.md` | 문서 갱신(매뉴얼 8.4 ISMS 결함관리 신설, 보안 결함사항 절 보강) |

## DB 변경

| 대상 | 내용 |
|------|------|
| `isms_defects` (신규) | 연도별 심사 결함 |
| `isms_defect_reports` (신규) | 연도별 결함 조치 보고서(연도 유니크) |
| `security_findings` | `source_type`·`source_defect_id`(유니크)·`source_label` 컬럼, 기존 행 `MANUAL` backfill |

**운영 DB 는 마이그레이션을 적용해야 합니다.** (`ddl-auto` 는 컬럼만 추가하고 backfill·유니크 제약은 만들지 않습니다)

```bash
docker compose exec -T db mysql --default-character-set=utf8mb4 -usecportal -psecportal123 secportal < db/migration/v1.42.0_isms_defect.sql
docker compose exec -T db mysql --default-character-set=utf8mb4 -usecportal -psecportal123 secportal < db/migration/v1.42.0_security_finding_source.sql
```

두 스크립트 모두 여러 번 실행해도 안전합니다.

## 알려진 제약

- 연동은 **ISMS 결함관리 → 보안 결함사항 단방향**입니다. 가져온 건은 원본에서만 고칠 수 있습니다.
- 결함 조치 보고서는 입력 내용과 첨부 원본을 보관하며, PDF 로 생성하지는 않습니다.

## 업그레이드

```bash
git pull
# 위 DB 변경의 마이그레이션 2건 적용
docker compose build backend frontend
docker compose up -d backend frontend
```

- 배포 후 브라우저 **하드 새로고침(Ctrl+Shift+R)** 이 필요합니다.
- 새 메뉴는 ADMIN 과 기본 MANAGER 역할에 기동 시 자동으로 추가됩니다. USER·사용자 정의 역할은 **관리 > 권한관리**에서 `ISMS 결함관리` 권한을 부여하세요.
- 운영 환경에서는 배포 전 **DB 백업**을 권장합니다.
