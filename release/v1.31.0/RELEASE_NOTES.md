# KSecPortal v1.31.0 릴리즈 노트

**릴리즈 일자**: 2026-08-08

위협 기본 항목 카탈로그를 **정리·검색·일괄 관리**할 수 있게 하고, Slack 연동에 **소켓 모드**를 추가했습니다. 내부감사 상세는 팝업 방식으로 통일했습니다.

---

## 신규 기능

### 1. 위협 기본 항목 — 항목별 필터 검색 (관리 > 코드관리 > 위협 기본 항목)

통합 키워드 검색 하나로 찾던 방식을 **컬럼별 필터**로 바꿨습니다.

| 필터 | 방식 |
|------|------|
| Risk ID | 텍스트 부분일치 |
| 위협명 | 텍스트 부분일치 |
| 유형 | 셀렉트 (데이터에서 추출) |
| 카테고리 | 셀렉트 — **선택한 유형에 종속** (유형 변경 시 무효한 카테고리 자동 해제) |
| 발생가능성 · 잠재영향 | 셀렉트 1~5 |

- 모든 조건은 AND로 결합, 하단에 `검색 결과 N건 / 전체 M건` 과 `필터 초기화` 표시

### 2. 위협 기본 항목 — 체크박스 선택 삭제

- 행 체크박스 + 선택 행 하이라이트
- **제목줄 체크박스는 현재 검색된 결과 전체(모든 페이지)를 선택/해제**, 일부 선택 시 indeterminate 표시
- 선택 시 상단에 `N건 선택됨` 바 노출 → `선택 삭제` → 확인 팝업 → 일괄 삭제
- 신규 API `POST /api/threats/defaults/bulk-delete` (ADMIN) — `deleteAllInBatch` 로 요청 1회 처리
- **필터를 변경하면 선택이 해제됩니다** — 화면에 보이지 않는 행이 선택된 채 삭제되는 것을 막기 위한 동작입니다

### 3. Slack 연동 방식에 소켓 모드 추가 (관리 > 설정관리 > 시스템 설정 > 승인 알림 설정)

기존 Webhook 방식에 더해 **소켓 모드 앱** 연동을 선택할 수 있습니다.

| 방식 | 입력 항목 |
|------|-----------|
| **Webhook** (기본) | Incoming Webhook URL — 기존과 동일 |
| **소켓 모드** | 봇 토큰(`xoxb-`), 앱 레벨 토큰(`xapp-`, 선택), 발송 채널 |

- 토큰은 저장 후 **마스킹**되어 조회됩니다. 입력란을 비우면 기존 토큰 유지, `-` 입력 시 삭제 (SMTP 비밀번호와 동일한 규칙)
- **연결 테스트** 버튼 — Webhook은 테스트 메시지 발송, 소켓 모드는 `auth.test`(봇 토큰·워크스페이스 확인) → `apps.connections.open`(앱 레벨 토큰) → 채널 지정 시 테스트 메시지 발송 순으로 점검
- 신규 API `POST /api/admin/notification-config/slack/test` (ADMIN)

> **소켓 모드의 범위**: Slack Socket Mode의 WebSocket은 *Slack → 앱* 방향(이벤트 수신) 전송 수단이고, *앱 → Slack* 메시지 발송은 소켓 모드 앱도 Web API `chat.postMessage`(봇 토큰)를 사용합니다. 이 포털은 Slack 이벤트를 수신하지 않으므로 상시 WebSocket 리스너는 두지 않고, **봇 토큰 기반 발송 + 앱 레벨 토큰으로 소켓 연결 발급 가능 여부 점검** 구조로 구현했습니다.

### 4. 내부감사 상세 팝업 (보안 거버넌스 > 내부감사)

- 감사 상세가 오른쪽 슬라이드 드로어에서 **중앙 모달 팝업**으로 변경 (`max-w-3xl` + `max-h-[92vh]` 내부 스크롤 — 프로젝트 모달 표준)
- 배경 클릭으로 닫힘, 헤더에 수정 버튼 추가 — 팝업에서 바로 수정하면 내용이 자동 갱신됩니다
- **점검항목 카드 클릭 시 항목 상세 팝업** — 점검대상·결과·점검방법·발견사항·조치필요사항을 줄바꿈 유지한 전문으로 표시

---

## 데이터 정리 — 위협 기본 항목 중복 제거 (558건 → 140건)

초기 시드가 14개 위협명을 40회 반복하며 `계정 탈취 #1` 처럼 **전역 일련번호**를 붙여 생성해 두어, 번호를 제외하면 동일 항목이 최대 6건씩 중복되어 있었습니다.

| 항목 | 값 |
|------|-----|
| 정리 전 | 558건 (전 행이 `#숫자` 접미사 보유) |
| 중복 그룹 | 139개 (그룹 크기 2~6건) |
| 삭제 | **418건** — 그룹별 가장 작은 id 1건만 유지 |
| 정리 후 | **140건**, 위협명 14종, `#숫자` 0건 |

중복 판정 기준은 **위협명(`#숫자` 제외) + 유형 + 카테고리 + 발생가능성 + 잠재영향** 입니다.

### 유니크 제약 변경 (스키마 변경)

`#숫자`를 제거하면 (위협명+유형+카테고리) 조합이 28개뿐이라 기존 제약과 112건이 충돌합니다. 같은 위협이라도 **위험도 조합이 다르면 별도 항목**으로 유지하기 위해 제약을 확장했습니다.

```
uq_threat_defaults_name_type_cat      (name, type, category)
  → uq_threat_defaults_name_type_cat_risk (name, type, category, likelihood, impact)
```

---

## 변경 사항 요약

### 백엔드
- `threat` — 기본 항목 일괄 삭제 `POST /threats/defaults/bulk-delete`(ADMIN), `ThreatDto.BulkDeleteRequest`, `ThreatService.deleteDefaults`
- `threat` — 중복 검사를 유니크 제약과 동일한 5개 키로 변경 (`existsByNameAndTypeAndCategoryAndLikelihoodAndImpact`), 오류 메시지 갱신
- `threat` — **`loadDefaults`(기본 항목 → 위협 관리 복사) 판별 기준을 이름 단독에서 5개 키로 변경.** 위협명이 14종으로 줄어 이름만으로 비교하면 140건 중 14건만 복사되는 문제를 해결
- `threat` — `checkDefaults` 의 하드코딩 상수 `DEFAULT_COUNT = 560` 제거, 실제 기본 항목 수 반환
- `notification` — Slack 소켓 모드 설정 키 4종(`slack-mode`·`slack-bot-token`·`slack-app-token`·`slack-channel`), 토큰 마스킹 및 유지/삭제 규칙, 설정 변경 감사 로그(`NOTIFICATION_CONFIG_UPDATED`)
- `notification` — `SlackService` 에 `chat.postMessage`·`auth.test`·`apps.connections.open` 호출 추가. Slack은 오류도 HTTP 200 + `{"ok":false}` 로 응답하므로 `ok`/`error` 를 파싱해 판정. Webhook 은 종전대로 `hooks.slack.com` 만 허용
- `notification` — 연결 테스트 `POST /admin/notification-config/slack/test`(ADMIN), `ApprovalNotificationService` 가 설정된 방식에 따라 분기 발송

### 프론트엔드
- `views/admin/CodeManagementView.vue` — 위협 기본 항목 항목별 필터 6종, 체크박스 선택 삭제, 검색 결과 전체 선택
- `views/admin/AdminSettingsView.vue` — Slack 연동 방식 선택 및 소켓 모드 입력·연결 테스트 UI (라우팅되지 않는 잔존 파일 `views/admin/SystemSettingsView.vue` 도 동일 내용으로 동기화)
- `views/internalaudit/InternalAuditView.vue` — 상세 드로어 → 모달 전환, 점검항목 상세 팝업 추가, 모달 z-index 정리
- `api/index.js` — `threatDefaultApi.bulkDelete`, `notificationConfigApi.testSlack`
- `i18n/{ko,en}.json` — `common.close` 추가

### DB
- 마이그레이션 `db/migration/v1.31.0_threat_defaults_dedup.sql` — 중복 삭제 → 유니크 제약 교체 → 위협명 정리 (순서 중요)
- 신규 설치용 `db/init/07_extended_schema.sql`(제약 정의), `db/init/09_threat_seed.sql`(정리된 140건으로 교체) 동시 갱신
- 알림 설정은 `notification_config` 키/값 테이블에 행만 추가되므로 스키마 변경 없음

> **기존 인스턴스**: 유니크 제약 변경과 데이터 정리는 `ddl-auto: update` 가 처리하지 못합니다. **마이그레이션 SQL을 반드시 실행**해야 합니다.

---

## 업그레이드 방법

```bash
# 1) 데이터 정리 + 제약 교체 (필수)
docker compose exec -T db mysql -usecportal -psecportal123 secportal < db/migration/v1.31.0_threat_defaults_dedup.sql

# 2) 이미지 빌드 및 배포
docker compose build backend frontend
docker compose up -d backend frontend
```

정리 결과 확인:

```sql
SELECT COUNT(*) total, SUM(name LIKE '%#%') with_hash FROM threat_defaults;
-- total=140, with_hash=0
```

배포 후 해시된 자산 캐시 때문에 브라우저 **하드 리프레시(Ctrl+Shift+R)** 가 필요합니다.

---

## 알려진 제한

- 검증은 **빌드·기동·스키마·데이터 정리 결과·엔드포인트 인증 게이트** 수준까지 수행했습니다. 로그인 토큰이 필요한 대화형 흐름(필터 조합, 선택 삭제 확인, 내부감사 팝업 편집)은 화면에서 확인이 필요합니다.
- Slack 소켓 모드는 실제 워크스페이스 토큰이 있어야 발송까지 검증할 수 있습니다. 화면의 **연결 테스트**로 확인하세요.
- 위협 관리(`threats`) 테이블의 기존 데이터는 이번 정리 대상이 아닙니다. 기본 항목에서 복사돼 들어간 `#숫자` 이름이 그대로 남아 있으며, 운영 중 편집됐을 수 있어 임의로 변경하지 않았습니다.
