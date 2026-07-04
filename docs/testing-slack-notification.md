# Slack 알림 연동 테스트 메뉴얼

SecPortal v1.14.0 이상에서 제공하는 **승인 알림 Slack 연동**의 설정부터 수신 확인까지의 전체 테스트 절차를 설명합니다.

---

## 목차

1. [사전 준비 — Slack Webhook URL 발급](#1-사전-준비--slack-webhook-url-발급)
2. [환경설정에서 Slack 연동 설정](#2-환경설정에서-slack-연동-설정)
3. [Webhook 직접 동작 확인 (curl)](#3-webhook-직접-동작-확인-curl)
4. [시나리오별 알림 발송 테스트](#4-시나리오별-알림-발송-테스트)
5. [감사 로그 확인](#5-감사-로그-확인)
6. [SSRF 방어 검증](#6-ssrf-방어-검증)
7. [트러블슈팅](#7-트러블슈팅)

---

## 1. 사전 준비 — Slack Webhook URL 발급

### 1-1. Slack Incoming Webhook 앱 설정

1. [https://api.slack.com/apps](https://api.slack.com/apps) 접속 (Slack 계정 로그인 필요)
2. **Create New App** → **From scratch** 클릭
3. App Name: `SecPortal` 입력 → 알림 받을 워크스페이스 선택 → **Create App**
4. 좌측 메뉴 **Incoming Webhooks** → 토글을 **On**
5. **Add New Webhook to Workspace** 클릭 → 채널 선택 (예: `#secportal-approval`) → **허용**
6. 생성된 URL 복사

발급된 URL 형식:
```
https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX
```

> **보안 주의**: 이 URL은 비밀번호와 동일하게 취급합니다. Git에 커밋하지 마세요.

---

## 2. 환경설정에서 Slack 연동 설정

### 2-1. 관리자 계정으로 로그인

| 항목 | 값 |
|------|----|
| URL | http://서버IP |
| 이메일 | `secportal@monosun.com` |
| 비밀번호 | `Admin1234!` (변경한 경우 변경된 비밀번호) |

### 2-2. 승인 알림 설정 메뉴 접근

1. 좌측 사이드바 → **환경설정** 클릭
2. **승인 알림 설정** 섹션으로 스크롤

### 2-3. Slack 알림 방식 설정

| 항목 | 설정값 |
|------|-------|
| 알림 방식 | `Slack` 또는 `이메일+Slack` |
| Slack Webhook URL | 1단계에서 발급한 URL 입력 |

**이메일+Slack** 선택 시: 이메일과 Slack 양쪽으로 모두 발송됩니다.

### 2-4. 저장 확인

**저장** 버튼 클릭 → 화면에 성공 메시지가 표시되면 설정 완료.

---

## 3. Webhook 직접 동작 확인 (curl)

알림 시나리오 테스트 전에 Webhook URL 자체가 유효한지 먼저 검증합니다.

### Windows (PowerShell)

```powershell
$webhookUrl = "https://hooks.slack.com/services/T00000000/B00000000/XXXX"
$body = '{"text":"SecPortal Slack 연동 직접 테스트 메시지입니다."}'
Invoke-RestMethod -Uri $webhookUrl -Method POST -ContentType "application/json" -Body $body
```

### macOS / Linux (curl)

```bash
curl -X POST https://hooks.slack.com/services/T00000000/B00000000/XXXX \
  -H 'Content-Type: application/json' \
  -d '{"text":"SecPortal Slack 연동 직접 테스트 메시지입니다."}'
```

**예상 응답**: `ok`

응답이 `ok`이면 Webhook URL은 정상입니다. 다음 단계로 진행합니다.

---

## 4. 시나리오별 알림 발송 테스트

아래 세 가지 시나리오에서 승인 알림이 발송됩니다. 각 시나리오를 순서대로 수행하며 Slack 채널 수신을 확인합니다.

---

### 시나리오 A — 신규 계정 생성 알림

**트리거**: ADMIN이 사용자를 직접 생성할 때 발송됩니다.

#### 테스트 절차

1. ADMIN 계정으로 로그인
2. 좌측 사이드바 → **사용자 관리**
3. **사용자 추가** 버튼 클릭
4. 아래 정보 입력:

| 항목 | 테스트 값 |
|------|---------|
| 이름 | `테스트유저` |
| 이메일 | `test-slack@example.com` |
| 비밀번호 | `Test1234!` |
| 역할 | `USER` |

5. **저장** 클릭

#### 예상 Slack 메시지

```
신규 계정 생성 알림
[SecPortal] 신규 계정 생성
이름: 테스트유저
이메일: test-slack@example.com
역할: USER
등록자: 관리자
```

---

### 시나리오 B — 계정 삭제 승인 요청 알림

**트리거**: ADMIN이 일반 사용자의 삭제를 요청할 때 발송됩니다. 승인/거부 URL이 메시지에 포함됩니다.

#### 테스트 절차

1. ADMIN 계정으로 로그인
2. 좌측 사이드바 → **사용자 관리**
3. 시나리오 A에서 생성한 `테스트유저` 행의 **삭제** 버튼 클릭
4. 확인 팝업에서 **삭제 요청** 클릭

#### 예상 Slack 메시지

```
[SecPortal] 계정 삭제 승인 요청
대상: 테스트유저 (test-slack@example.com)
요청자: 관리자
승인: http://서버IP/api/admin/approve/{token}
거부: http://서버IP/api/admin/reject/{token}
```

> 메시지 내 **승인** 링크를 브라우저에서 열면 계정이 비활성화됩니다.  
> **거부** 링크를 열면 요청이 취소됩니다.

---

### 시나리오 C — ADMIN 승격 승인 요청 알림

**트리거**: ADMIN이 일반 사용자를 ADMIN으로 승격 요청할 때 발송됩니다.

#### 테스트 절차 (API 방식)

UI가 아닌 API로 직접 테스트합니다.

**1. 로그인 → 토큰 획득**

```powershell
# Windows PowerShell
$loginRes = Invoke-RestMethod -Uri "http://localhost/api/auth/login" `
  -Method POST -ContentType "application/json" `
  -Body '{"email":"secportal@monosun.com","password":"Admin1234!"}'
$token = $loginRes.data.accessToken
```

```bash
# macOS / Linux
TOKEN=$(curl -s -X POST http://localhost/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"secportal@monosun.com","password":"Admin1234!"}' \
  | jq -r '.data.accessToken')
```

**2. 승격할 사용자 ID 조회**

```powershell
$headers = @{ Authorization = "Bearer $token" }
$users = Invoke-RestMethod -Uri "http://localhost/api/admin/users" -Headers $headers
$users.data.content | Select-Object id, email, role
```

**3. ADMIN 승격 요청**

```powershell
# {userId}를 실제 사용자 ID로 교체
Invoke-RestMethod -Uri "http://localhost/api/admin/users/{userId}/promote-admin" `
  -Method POST -Headers $headers
```

```bash
curl -X POST http://localhost/api/admin/users/{userId}/promote-admin \
  -H "Authorization: Bearer $TOKEN"
```

#### 예상 Slack 메시지

```
[SecPortal] ADMIN 권한 부여 승인 요청
대상: 테스트유저 (test-slack@example.com)
요청자: 관리자
승인: http://서버IP/api/admin/approve/{token}
거부: http://서버IP/api/admin/reject/{token}
```

---

## 5. 감사 로그 확인

Slack 발송 여부와 무관하게, 모든 승인 요청은 감사 로그에 기록됩니다.

### UI에서 확인

1. 좌측 사이드바 → **감사 로그**
2. 아래 액션 유형으로 필터:

| 시나리오 | 액션 유형 |
|---------|---------|
| 신규 계정 생성 | `USER_CREATED` |
| 계정 삭제 요청 | `USER_DELETE_REQUESTED` |
| ADMIN 승격 요청 | `USER_PROMOTE_ADMIN_REQUESTED` |
| 승인 처리 | `USER_DEACTIVATED` / `USER_PROMOTED_ADMIN` |
| 거부 처리 | `ADMIN_ACTION_REJECTED` |
| 로그인 실패 | `LOGIN_FAILED` |

### 백엔드 로그에서 확인

```bash
# Slack 전송 관련 로그 확인
docker compose logs backend | grep -i slack

# 정상 발송 시
# INFO  - Slack message sent successfully

# SSRF 차단 시
# WARN  - Rejected Slack webhook URL with disallowed host: evil.com

# URL 미설정 시
# WARN  - Slack webhook URL is not configured
```

---

## 6. SSRF 방어 검증

v1.15.0에서 `hooks.slack.com` 외 호스트로의 요청은 차단됩니다. 아래 절차로 검증합니다.

### 6-1. 잘못된 Webhook URL 설정

환경설정에서 Slack Webhook URL을 아래 값으로 임시 변경:

```
https://evil.example.com/webhook
```

### 6-2. 알림 발송 트리거

시나리오 A~C 중 하나 수행.

### 6-3. 결과 확인

```bash
docker compose logs backend | grep -i slack
# 출력: WARN - Rejected Slack webhook URL with disallowed host: evil.example.com
```

Slack 채널에는 메시지가 오지 않아야 합니다. 확인 후 올바른 URL로 복원합니다.

---

## 7. 트러블슈팅

### Slack 메시지가 오지 않는 경우

**① 알림 설정 조회**

```powershell
# 현재 알림 설정 확인
$headers = @{ Authorization = "Bearer $token" }
Invoke-RestMethod -Uri "http://localhost/api/admin/notification-config" -Headers $headers
```

응답 예시:
```json
{
  "method": "SLACK",
  "approvalEmail": "admin@example.com",
  "slackWebhookUrl": "https://hooks.slack.com/services/..."
}
```

- `method`가 `EMAIL`이면 Slack으로 발송되지 않습니다.
- `slackWebhookUrl`이 빈 문자열이면 URL을 입력해야 합니다.

**② 백엔드 컨테이너 로그 확인**

```bash
docker compose logs -f backend | grep -i slack
```

| 로그 메시지 | 원인 | 조치 |
|-----------|------|------|
| `Slack webhook URL is not configured` | URL 미설정 | 환경설정에서 URL 입력 |
| `Rejected Slack webhook URL with disallowed host` | 잘못된 URL | `hooks.slack.com` 주소 사용 |
| `Failed to send Slack message: ...` | 네트워크 오류 | 방화벽 또는 DNS 확인 |
| `Slack message sent successfully` | 정상 | — |
| `Slack responded with status 404` | Webhook URL 만료 | Slack에서 URL 재발급 |

**③ Webhook URL 유효성 직접 확인**

```bash
curl -X POST {슬랙_웹훅_URL} \
  -H 'Content-Type: application/json' \
  -d '{"text":"ping"}'
# 응답이 ok이면 URL 정상
# 응답이 no_team이거나 invalid_token이면 URL 재발급 필요
```

**④ 컨테이너 외부 통신 확인**

```bash
docker exec secportal-backend wget -q -O- https://hooks.slack.com 2>&1 | head -3
# 응답이 있으면 외부 통신 가능
# 타임아웃이면 방화벽 또는 프록시 설정 확인
```

---

### 알림 방식 선택 기준

| 상황 | 권장 알림 방식 |
|------|-------------|
| 이메일만 사용 | `이메일` |
| Slack 채널에서 팀 전체가 수신 | `Slack` |
| 이메일 수신 + Slack 동시 알림 | `이메일+Slack` |
| Slack Webhook URL 미보유 | `이메일` |

---

## 부록 — 테스트 체크리스트

```
[ ] 1. Slack Webhook URL 발급 완료
[ ] 2. 환경설정 > 승인 알림 설정에서 방식 = Slack (또는 이메일+Slack) 저장
[ ] 3. curl로 Webhook URL 직접 호출 → "ok" 응답 확인
[ ] 4. 시나리오 A: 신규 계정 생성 → Slack 채널 메시지 수신 확인
[ ] 5. 시나리오 B: 계정 삭제 요청 → Slack 채널 메시지 + 승인/거부 링크 수신 확인
[ ] 6. 시나리오 C: ADMIN 승격 요청 → Slack 채널 메시지 + 승인/거부 링크 수신 확인
[ ] 7. 감사 로그에서 USER_DELETE_REQUESTED / USER_PROMOTE_ADMIN_REQUESTED 확인
[ ] 8. 잘못된 URL로 SSRF 방어 검증 → 백엔드 로그에서 차단 확인
[ ] 9. Slack 승인 링크 클릭 → 계정 상태 변경 확인
[ ] 10. 백엔드 로그에서 "Slack message sent successfully" 확인
```
