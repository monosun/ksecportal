# KSecPortal v1.26.0 릴리즈 노트

릴리즈 일자: 2026-08-01

## 요약

**관리 > 성능관리** 메뉴를 신설했습니다. 설정한 지연 기준(**기본 3초**)을 넘긴 **화면 요청**과 **SQL**을 감사 로그처럼 기록하고 조회할 수 있으며, 기준 시간은 화면에서 바꿀 수 있습니다.

## 상세

### 성능관리 신설 (관리 메뉴, ADMIN 전용)

**기록 대상**

- **화면(요청)** — 모든 API 요청의 처리 시간을 재서 기준을 넘으면 기록합니다. URL·HTTP 메서드·응답 코드·사용자·IP를 함께 남깁니다.
- **SQL** — 실제 실행된 구문의 소요시간을 재서 기준을 넘으면 전체 구문과 함께 기록합니다.

**기준 설정 (성능관리 화면에서 변경)**

| 항목 | 기본값 | 범위 |
|------|--------|------|
| 지연 기준 | **3초** | 0.1 ~ 600초 |
| 보관 기간 | 30일 | 1 ~ 3650일 |
| 성능 기록 사용 | 사용 | — |
| SQL 기록 포함 | 사용 | — |

- 설정은 `app_settings` 에 저장되며(`perf.threshold_ms` · `perf.enabled` · `perf.sql_enabled` · `perf.retention_days`), 저장 즉시 적용됩니다.
- 각 기록에는 **그 시점의 기준값**이 함께 저장되어, 이후 기준을 바꿔도 당시 판단 근거를 알 수 있습니다.

**조회 화면**

- 요약 카드(전체·화면·SQL·최대 소요시간), 유형/검색어/최소 소요시간/기간 필터, 페이징
- 행 클릭 시 전체 SQL 구문·쿼리스트링·IP·기록 시점 기준을 펼쳐 확인
- 5초 이상 주황색, 10초 이상 빨간색 강조
- 전체 또는 7·30·90일 이전 기록 삭제, 보관기간 초과 기록은 매일 03:20 자동 정리

**동작 방식**

- 기록은 요청 처리를 지연시키지 않도록 메모리 큐에 모았다가 **5초 간격으로 일괄 저장**합니다(큐 상한 2000건, 초과분은 버림).
- 지연 기준·사용 여부는 캐시된 값만 참조하고, 캐시는 스케줄러가 30초마다 갱신합니다 — 기록 경로에서 DB를 읽지 않으므로 되먹임이 발생하지 않습니다.
- 성능관리 화면 자체의 조회 요청과 성능 기록 저장 SQL은 기록 대상에서 제외합니다.
- SQL 계측은 외부 라이브러리 없이 JDK 동적 프록시로 DataSource → Connection → Statement 를 감싸 `execute*` 호출만 계측하며, 그 외에는 원본에 그대로 위임합니다.

## API

```
GET    /api/admin/performance/logs      # 기록 조회 (logType·keyword·minMs·dateFrom·dateTo·page·size)
GET    /api/admin/performance/stats     # 요약 통계
GET    /api/admin/performance/config    # 기준 조회
PUT    /api/admin/performance/config    # 기준 저장
DELETE /api/admin/performance/logs      # 기록 삭제 (days 미지정 시 전체)
```

모두 ADMIN 권한이 필요합니다.

## 데이터베이스 변경

신규 테이블 `slow_logs` (`ddl-auto: update` 로 재기동 시 자동 생성, 별도 마이그레이션 불필요).

## 업그레이드

```bash
docker compose build backend frontend
docker compose up -d backend frontend
```

배포 후 브라우저 하드 리프레시(Ctrl+Shift+R)가 필요합니다.
