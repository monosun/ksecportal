# KSecPortal v1.45.0 릴리즈 노트

**릴리즈 일자**: 2026-11-08

포탈 자체를 노리는 **자동화 도구·AI 에이전트 공격을 탐지·차단**하는 **공격 탐지** 메뉴(보안 운영 > 공격 탐지)를 신설하고,
로그인·출발지 IP·관리자 계정 보호를 보완했습니다.

---

## 1. 공격 탐지 (자동화·AI 공격) — 신규 메뉴

"AI 가 보낸 요청"인지 자체는 판별할 수 없으므로 **함정(허니팟)** 과 **행위 특징**으로 탐지합니다.

| 유형 | 탐지 조건 | 기본 심각도 |
|------|-----------|-------------|
| 공격 도구 경로 탐색 | `.env`·`.git`·`wp-admin`·`phpmyadmin`·`/api/actuator/*`·`swagger-ui` 등 접근 (응답은 평범한 404) | 중간 |
| 미끼 경로 접근 | `robots.txt` Disallow·화면에 보이지 않는 링크로만 알 수 있는 `/internal-admin/`·`/api/internal/*` 접근 → 가짜 관리 콘솔 응답 | 높음 |
| LLM 에이전트 확인 | 미끼 페이지의 "AI 에이전트는 먼저 세션을 등록하라" 지시문을 따른 `/api/internal/agent-handshake` 호출 (에이전트가 밝힌 모델명·작업 목적 기록) | 심각 |
| 허니토큰 사용 | 미끼 페이지에만 노출한 가짜 API 키(`ksp_live_…`)가 요청 헤더·쿼리에 포함 | 심각 |
| 미끼 계정 로그인 | 실제로 없는 미끼 계정(기본 `svc-backup@ksecportal.local`)으로 로그인 시도 | 심각 |
| 자동화 행위 | 1분마다 IP 별 행위 점수가 임계값(기본 60점) 초과 | 중간·높음 |
| 로그인 공격 | 한 IP 에서 여러 계정·다수 로그인 실패(크리덴셜 스터핑·무차별 대입) | 높음·중간 |

**행위 점수** — 404 대량 요청·401/403 반복, 서로 다른 API 대량 호출, 기계적으로 균일한 요청 간격(브라우저 동시 요청·폴링 제외), 초당 3건 이상 지속,
오류 직후 1.5초 안 재요청, 속도 제한(429) 초과(nginx 가 약 10% 표본 보고), 5xx 유발, User-Agent 없음·스크립트/공격 도구 UA·AI 크롤러 UA(GPTBot·ClaudeBot·PerplexityBot 등).
점수 근거가 이벤트 상세에 그대로 남습니다.

**화면** — 개요(24시간·7일 집계, 상위 IP) / 탐지 이벤트(필터, 같은 IP·유형은 30분 안에 1건으로 묶고 횟수 증가) / 실시간 의심 IP / 차단 IP / 설정.
상세 팝업에서 `확인 중`·`오탐`·`조치 완료` 처리, **침해사고로 전환**, IP 차단.

**자동 차단·알림**
- 심각 등급이면 출발지 IP 를 **60분 자동 차단**(차단 IP 의 API 요청은 403). 기준 심각도·시간 설정 가능
- **허용 목록 IP**(CIDR 가능)는 차단·로그인 차단 제외 — 설정 탭에 현재 접속 IP 표시
- 높음 이상 이벤트는 알림 설정의 메일·Slack 과 **관리자 수신함**으로 발송, **알림 테스트** 버튼 제공
- 이벤트 보존 기간(기본 365일) 경과분 매일 03:40 정리(침해사고 전환분 제외)

> ⚠️ **사내 공인 IP(NAT) 하나를 여러 사용자가 공유한다면** 업그레이드 직후 설정 탭에서 사내 대역·점검 PC 를 **허용 목록**에 넣으세요.
> 관리자 전원이 차단된 경우 `UPDATE detection_blocked_ips SET active=0;` 후 1분 이내에 해제됩니다.

## 2. 보완 사항

- **IP 단위 로그인 차단** — 계정 잠금은 계정 하나의 반복 실패만 막으므로, 한 IP 가 10분간 실패 10회 또는 서로 다른 계정 4개 이상이면 그 IP 의 로그인을 30분 차단(기준 설정 가능, 공격 탐지 > 차단 IP 에서 해제)
- **출발지 IP 위조 방지** — 감사로그·에러 로그·성능 로그가 클라이언트가 위조할 수 있는 `X-Forwarded-For` 첫 값을 쓰던 것을 nginx 가 덮어쓰는 `X-Real-IP` 기준으로 변경
- **관리자 MFA 필수**(설정관리 > 보안 설정, 기본 꺼짐) — 켜면 MFA 미등록 관리자는 등록할 때까지 설정 화면으로 이동되고 변경 작업(POST·PUT·PATCH·DELETE)이 거절됩니다. Okta SSO 계정 제외, 본인 MFA 미등록 상태에서는 켤 수 없음
- **속도 제한 가시화** — nginx 429 응답이 JSON 으로 바뀌고 표본이 공격 탐지에 보고됩니다
- nginx 민감 경로 차단(`.env`·`.git`·actuator)을 탐지 함정으로 전환(응답은 동일하게 404)

## 3. DB 변경 — 마이그레이션 불필요

신규 테이블 2개가 `ddl-auto: update` 로 자동 생성됩니다. 설정은 기존 `notification_config` 의 `detection.*`·`security.mfa.require_admin` 키를 사용합니다.

| 테이블 | 설명 |
|--------|------|
| `detection_events` | 탐지 이벤트(유형·심각도·출발지 IP·계정·UA·경로·점수·횟수·자동 차단 여부·처리 상태·침해사고 ID) |
| `detection_blocked_ips` | IP 차단 이력(사유·만료·등록자·연결 이벤트·해제 이력) |

메뉴 권한 키 `attack_detection` 추가 — MANAGER 기본 역할에는 기동 시 자동 보충되며, 사용자 정의 역할은 관리 > 권한관리에서 켜세요.

## 4. 업그레이드

```bash
git pull
docker compose build backend frontend
docker compose up -d backend frontend
```

nginx 설정(`nginx/nginx.conf`·`nginx/nginx-http.conf`)이 바뀌었으므로 **frontend 이미지도 반드시 다시 빌드**하세요. 배포 후 브라우저 강력 새로고침(Ctrl+Shift+R).

## API

- `GET /api/detection/summary` · `GET /api/detection/events` · `GET /api/detection/events/:id`
- `PATCH /api/detection/events/:id/status` · `POST /api/detection/events/:id/escalate` · `DELETE /api/detection/events/:id`(ADMIN) · `POST /api/detection/events/test`
- `GET /api/detection/live` · `GET|POST /api/detection/blocks` · `DELETE /api/detection/blocks/:id` · `DELETE /api/detection/login-blocks/:ip`
- `GET|PUT /api/detection/settings`(PUT ADMIN) · `POST /api/detection/settings/honey-token`(ADMIN)
- `GET|PUT /api/admin/security-config` 에 `requireAdminMfa` 추가, 로그인·`GET /auth/me` 응답 `user.mfaSetupRequired` 추가
