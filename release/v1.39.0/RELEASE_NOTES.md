# KSecPortal v1.39.0 릴리즈 노트

**릴리즈 일자**: 2026-08-27

프로그램 처리 중 발생한 오류를 한 곳에 모아 관리하는 **관리 > 에러 로그 관리** 메뉴를 신설했습니다.
지금까지는 사용자가 "오류가 났다"고 알려 오면 컨테이너 로그(`docker compose logs backend`)를
직접 뒤져야 했고, 화면(브라우저)에서 난 오류는 아예 서버에 남지 않았습니다.
이제 **서버 오류·화면 JS 오류·자동 작업 실패**가 자동으로 수집되고,
담당자가 **확인 → 조치 → 정리**까지 화면에서 처리할 수 있습니다.

---

## 1. 에러 로그 관리 (신규 · ADMIN)

### 수집 대상

| 발생 위치 | 기록되는 오류 | 등급 |
|-----------|----------------|------|
| **서버(BACKEND)** | 전역 예외 처리기에 잡힌 처리 실패(500) | ERROR |
| | 파일 업로드 실패·용량 초과, 잘못된 요청 값 | WARN |
| **화면(FRONTEND)** | 브라우저 JavaScript 오류, 처리되지 않은 Promise 거부 | ERROR |
| **스케줄러(SCHEDULER)** | 정해진 시각에 도는 자동 작업의 실패 (`recordJobFailure`) | ERROR |

- 입력값 검증 실패·권한 오류·없는 경로 요청처럼 **안내 문구로 이미 처리되는 경우는 적재하지 않습니다.**
  특히 없는 경로 요청은 헬스체크·스캐너가 반복 호출하면 로그를 뒤덮기 때문에 제외했습니다.
- 기록은 **별도 트랜잭션(`REQUIRES_NEW`)** 에서 수행하고 예외를 삼키므로,
  로그 적재가 실패해도 원래 요청·응답에는 영향이 없습니다.
- 메시지는 4,000자, 스택트레이스는 20,000자에서 잘라 저장합니다.

### 화면 구성

- **요약 카드 4개** — 미확인 / 확인중 / 최근 24시간(그중 오류 등급 건수) / 전체 보관(최근 7일 건수).
  미확인·확인중 카드는 눌러 해당 상태만 걸러 볼 수 있습니다.
- **최근 7일 반복 오류 Top 5** — 같은 원인으로 반복되는 오류를 건수 순으로 표시합니다.
  묶음 기준(`fingerprint`)은 **예외 타입 + 메시지(숫자 제거) + 최상단 스택 프레임 + 요청 경로**의 SHA-256 해시라,
  ID·타임스탬프만 다른 같은 오류가 흩어지지 않습니다.
- **필터** — 등급(오류/경고), 발생 위치(서버/화면/스케줄러), 처리 상태, 검색어(오류 종류·메시지·요청 경로·사용자명),
  발생 기간, 행/페이지(화면 높이에 맞춰 자동 산정).
- **상세 팝업** — 오류 메시지, 요청 경로·HTTP 메서드·응답 코드, 사용자·접속 IP·브라우저(User-Agent),
  **스택트레이스**(복사 버튼 제공).

### 처리 관리

| 상태 | 사용 시점 |
|------|-----------|
| 미확인(NEW) | 기록된 직후의 기본 상태 |
| 확인중(IN_PROGRESS) | 원인을 확인하거나 수정 중 |
| 처리완료(RESOLVED) | 원인을 해결한 경우 |
| 무시(IGNORED) | 조치가 필요 없는 오류(일시적 네트워크 오류 등) |

- 상태와 함께 **처리 메모**(원인·조치 내용)를 남기면 **처리자·처리 일시**가 자동 기록됩니다.
  상태를 미확인으로 되돌리면 처리 이력은 지워집니다.
- 상태 변경·삭제는 **감사 로그**(`ERROR_LOG_STATUS_CHANGED`·`ERROR_LOG_DELETED`·`ERROR_LOG_PURGED`)에 남습니다.

### 내려받기와 정리

- **엑셀 내려받기** — 현재 조회 조건 그대로 최대 5,000건.
- **처리완료·무시 로그 비우기**, **오래된 로그 정리**(7·30·90·180일 이전 또는 전체).
- 보관 기간(기본 **90일**)이 지난 로그는 **매일 04:40** 자동 정리됩니다
  (`ERROR_LOG_RETENTION_DAYS` · `ERROR_LOG_PURGE_CRON` 로 조정).

### 개인정보 표시

사용자명·접속 IP는 감사 로그와 동일하게 **코드관리의 항목별 마스킹 기준**을 따르며,
마스킹 해제는 ADMIN 전용입니다(해제 시 `PI_UNMASK` 기록).

## 2. 화면(브라우저) 오류 자동 수집

`frontend/src/utils/errorReporter.js` 를 추가해 다음 세 경로의 오류를 서버로 보냅니다.

- Vue 컴포넌트 오류(`app.config.errorHandler` — 어느 훅에서 났는지도 함께 기록)
- 전역 `error` 이벤트
- 처리되지 않은 `unhandledrejection`

무한 전송을 막기 위한 안전장치를 뒀습니다.

- **로그인 상태에서만** 전송(비로그인 시에는 보낼 권한이 없어 생략)
- 한 접속당 **최대 20건**, 같은 오류(이름+메시지+스택 앞부분)는 **1회만** 전송
- `ResizeObserver loop` 등 브라우저 잡음성 오류는 제외
- 전송 실패는 조용히 무시 — 오류 보고가 사용자 흐름을 막지 않습니다
- axios 인터셉터가 이미 안내 문구로 바꿔 거부한 요청 오류는 **서버에 이미 기록**돼 있으므로 중복 전송하지 않습니다

## 3. 없는 경로 요청 — 500 → 404

지금까지 존재하지 않는 API 경로를 호출하면 `NoResourceFoundException` 이 최종 핸들러까지 흘러
**"Internal server error"(500)** 로 응답했습니다. 이제 **404 + "요청한 경로를 찾을 수 없습니다."** 로 응답합니다.
서버 장애가 아닌 요청이 장애로 보이던 문제를 함께 바로잡았습니다.

---

## 변경 파일

| 파일 | 변경 |
|------|------|
| `errorlog/entity/ErrorLog.java` | **신규** — 발생일시·등급·발생위치·처리상태·예외타입·메시지·스택트레이스·요청정보·사용자·지문(fingerprint)·처리메모 |
| `errorlog/repository/ErrorLogRepository.java` | **신규** — 조건 검색, 상태·기간별 집계, 반복 오류 집계, 기간·상태별 삭제 |
| `errorlog/service/ErrorLogService.java` | **신규** — 적재(서버·스케줄러·화면), 조회·통계, 처리 상태 변경, 정리(수동·자동), 엑셀 생성 |
| `errorlog/dto/ErrorLogDto.java` | **신규** — 목록·상세·처리요청·화면오류리포트·통계 DTO |
| `errorlog/controller/ErrorLogController.java` | **신규** — `/admin/error-logs` (ADMIN) 조회·통계·상세·처리·삭제·정리·엑셀 |
| `errorlog/controller/ClientErrorLogController.java` | **신규** — `POST /error-logs/client` (로그인 사용자) |
| `common/exception/GlobalExceptionHandler.java` | 오류 적재 연결(500·업로드 실패·잘못된 요청 값), `NoResourceFoundException` → **404** |
| `application.yml` | `errorlog.retention-days`(90) · `errorlog.purge-cron`(`0 40 4 * * *`) |
| `frontend/src/views/admin/ErrorLogView.vue` | **신규** — 요약 카드·반복 오류 Top 5·필터·목록·페이징·엑셀·정리 |
| `frontend/src/views/admin/ErrorLogDetailModal.vue` | **신규** — 상세·스택트레이스 복사·처리 상태/메모 저장·개별 삭제 |
| `frontend/src/views/admin/errorLogLabels.js` | **신규** — 상태·위치·등급 표기와 배지 색상 공통 정의 |
| `frontend/src/utils/errorReporter.js` | **신규** — 화면 오류 수집(전송 상한·중복 제거·잡음 필터) |
| `frontend/src/main.js` | 전역 오류 수집 연결 |
| `frontend/src/api/index.js` | `errorLogApi` 추가 |
| `frontend/src/router/index.js`, `components/layout/AppLayout.vue` | `/admin/error-logs` 라우트·관리 메뉴 항목 |
| `frontend/src/i18n/ko.json`, `en.json` | `admin.errorLogs` |
| `db/init/12_error_logs.sql`, `db/migration/v1.39.0_error_logs.sql` | **신규** — `error_logs` 테이블 |
| `README.md`, `docs/api.md`, `docs/user-manual.md`, `frontend/public/help/user-manual.md` | 문서 갱신(매뉴얼 19.7 신설, 이후 절 번호 이동) |

## DB 변경

**신규 테이블 `error_logs`** 가 추가됩니다.

`ddl-auto: update` 로도 테이블은 자동 생성되지만, 인덱스(`occurred_at`·`fingerprint`·`status`)와
`handled_by` 외래키를 정확히 맞추려면 마이그레이션 적용을 권장합니다.

```bash
docker compose exec -T db mysql -usecportal -psecportal123 secportal < db/migration/v1.39.0_error_logs.sql
```

## 알려진 제약

- 화면 오류는 **로그인 상태**에서만 수집됩니다. 로그인 화면 자체에서 난 오류는 남지 않습니다.
- 인증 필터 단계에서 실패한 요청(만료 토큰 등)은 전역 예외 처리기를 거치지 않아 기록되지 않습니다.
- 엑셀 내려받기는 **5,000건**까지입니다. 그보다 많으면 기간·조건을 좁혀 나눠 받으세요.
- 삭제한 로그는 복구할 수 없습니다. 원인 분석이 끝나기 전에는 정리하지 마세요.

## 업그레이드

```bash
git pull
docker compose exec -T db mysql -usecportal -psecportal123 secportal < db/migration/v1.39.0_error_logs.sql
docker compose build backend frontend
docker compose up -d backend frontend
```

- 운영 환경에서는 배포 전 **DB 백업**을 먼저 수행하세요.
- 배포 후 브라우저 **하드 새로고침(Ctrl+Shift+R)** 이 필요합니다.
- 보관 기간을 조정하려면 `.env` 에 `ERROR_LOG_RETENTION_DAYS`(일) · `ERROR_LOG_PURGE_CRON` 을 지정합니다.
  `ERROR_LOG_RETENTION_DAYS=0` 으로 두면 자동 정리를 끕니다.
