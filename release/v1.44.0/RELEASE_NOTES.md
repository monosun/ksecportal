# KSecPortal v1.44.0 릴리즈 노트

**릴리즈 일자**: 2026-11-08

ISMS-P 증적관리에 **항목별 전년도 증적 가져오기**와 **증적 유효기간**을 추가했습니다.

---

## 1. 항목별 전년도 증적 가져오기

전체 항목을 한 번에 복사하는 기존 `전년도 증적 가져오기` 외에, **통제항목 하나만** 이전 연도 증적을 골라 가져올 수 있습니다.

- 통제항목 팝업의 증적 영역에 **전년도 가져오기** 버튼, 증적 추가 폼에 `전년도 증적 가져오기` 탭 추가
- 이 항목의 **조회 연도 이전 증적**을 연도별로 묶어 표시 — 가장 최근 연도 증적이 기본 선택되며 연도 체크박스로 일괄 선택·해제
- 증적제목 · 내용 · 준수상태 · 첨부파일(실물 복제)을 복사하고, 선택 시 **현재 상태 · 의견**도 함께 가져옴(조회 연도에 작성된 내용이 없을 때만)
- 이미 증적이 있는 항목에서도 실행 가능(추가로 가져올지 확인)
- 가져온 증적에는 `📋 2025년 증적 가져옴` 표시가 붙고, 목록 상단 **가져오기 초기화**로 되돌릴 수 있습니다
- ISMS-P 메뉴 쓰기 권한이 있으면 사용할 수 있습니다

## 2. 증적 유효기간

통제항목마다 증적 유효기간을 정해 증적이 기간 안에 갱신되었는지 확인합니다.

- **설정** — 관리 > 코드관리 `ISMS-P 101항목` 탭 → 항목 **수정** → `증적 유효기간`(미설정 · 1개월 · 3개월(분기) · 6개월(반기) · 1년 · 2년 · 3년). 목록에 `증적 유효기간` 열 추가
- **만료일 = 증적 기준일 + 유효기간** — 기준일은 등록일이며, 증적 수정 시 **첨부파일을 새로 올리면 그 시점**으로 갱신
- **전년도에서 가져온 증적은 원본의 기준일을 승계** — 복사만으로 유효기간이 늘어나지 않습니다(일괄·항목별 가져오기 모두)
- **증적관리 목록** — `증적 유효기간` 열에 기간 · 만료일(해당 연도 증적 중 가장 늦은 만료일) · 상태 배지(`유효` / `D-n`: 30일 이내 / `만료`), 상태 필터에 **유효기간 만료·임박** 추가
- **통제항목 팝업** — 항목 정보에 `⏱ 증적 유효기간` 배지, 증적별 만료일·상태 표시, 증적 **수정** 화면에 유효기간 · 기준일 · 만료일 표시

## 3. DB 변경 — 마이그레이션 불필요

nullable 컬럼 2개가 `ddl-auto: update` 로 자동 추가됩니다. 신규 설치용 `db/init/06_isms.sql` 에도 반영했습니다.

| 테이블 | 컬럼 | 설명 |
|--------|------|------|
| `isms_items` | `evidence_validity_months INT NULL` | 증적 유효기간(개월), NULL = 미설정 |
| `isms_evidences` | `valid_from DATETIME NULL` | 유효기간 기준일시 (NULL 이면 `created_at` 기준) |

기존 증적은 `valid_from` 이 비어 있어 **등록일 기준**으로 계산됩니다. 업그레이드 전에 이미 전년도에서 가져온 증적은 원본 기준일 정보가 없으므로 가져온 날짜 기준으로 계산됩니다.

## 4. 업그레이드

```bash
git pull
docker compose build backend frontend
docker compose up -d backend frontend
```

배포 후 브라우저 강력 새로고침(Ctrl+Shift+R).

## API

- `GET /api/isms/items/:id/previous-evidences?year=` — 항목별 전년도 가져오기 후보
- `POST /api/isms/items/:id/copy-previous?year=` — `{ evidenceIds, includeNote }` → `{ targetYear, copiedEvidences, copiedNote }`
- `GET /api/isms/items` 응답에 `evidenceValidityMonths`, `evidenceExpiresAt`(year 지정 시) 추가
- `GET /api/isms/items/:id/evidences` 응답에 `copiedFromYear`, `validFrom`, `expiresAt` 추가
- `PATCH /api/isms/items/:id/defaults` 요청에 `evidenceValidityMonths` 추가(생략 시 미설정으로 저장, 120 초과 400)
