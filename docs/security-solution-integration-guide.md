# 보안솔루션 연동 개발 가이드

KSecPortal **보안이벤트 관리**에 새로운 보안솔루션(방화벽·IDS/IPS·WAF·SIEM·EDR·DLP 등)을 연동할 때 지켜야 할 규격과 절차를 정의합니다.
연동을 개발하는 담당자(솔루션 벤더 · 사내 연동 개발자)와 포털을 확장하는 개발자 모두를 대상으로 합니다.

## 목차

1. [연동 방식 개요](#1-연동-방식-개요)
2. [연동 전 준비](#2-연동-전-준비)
3. [API 규격](#3-api-규격)
4. [필드 규격 및 매핑 규칙](#4-필드-규격-및-매핑-규칙)
5. [솔루션 유형별 매핑 예시](#5-솔루션-유형별-매핑-예시)
6. [전송 규칙 — 배치·재시도·중복 방지](#6-전송-규칙--배치재시도중복-방지)
7. [연동 상태 관리](#7-연동-상태-관리)
8. [보안 요구사항](#8-보안-요구사항)
9. [연동 예제](#9-연동-예제)
10. [포털 확장 가이드 (포털 개발자용)](#10-포털-확장-가이드-포털-개발자용)
11. [연동 완료 체크리스트](#11-연동-완료-체크리스트)
12. [현재 구현 범위와 제약](#12-현재-구현-범위와-제약)

---

## 1. 연동 방식 개요

KSecPortal은 **Push 방식**으로 이벤트를 수집합니다. 포털이 솔루션을 폴링하지 않고, **솔루션(또는 중계 수집기)이 포털 REST API로 이벤트를 전송**합니다.

```
[보안솔루션]                [연동 어댑터/수집기]              [KSecPortal]
 이벤트 발생  ──syslog/API──►  정규화 · 심각도 매핑  ──HTTPS──►  POST /api/security-integrations/{id}/events
                                    │                                │
                                    └── 실패 시 재시도 큐              └── 보안이벤트 관리 화면 · 통계
```

- **연동(Integration)** = 포털에 등록된 솔루션 1대(또는 1개 클러스터). 이벤트는 항상 특정 연동에 속합니다.
- **이벤트(Event)** = 정규화된 단일 보안 이벤트. 포털은 원본 로그 전체가 아니라 **요약된 이벤트**를 보관합니다.
- 원본 로그 보관·상관분석은 SIEM의 역할이며, 포털은 **보고·현황 관리용 이벤트**를 다룹니다. 전송량을 이 관점에서 설계하세요.

> **정규화는 연동 어댑터의 책임입니다.** 포털은 솔루션별 로그 포맷을 해석하지 않습니다. 어댑터가 아래 필드 규격에 맞춰 변환해 전송해야 합니다.

---

## 2. 연동 전 준비

### 2.1 솔루션 유형 코드 확인 · 추가

연동의 `solutionType`은 **코드관리(`SECURITY_SOLUTION_TYPE` 코드그룹)** 값만 사용합니다.

기본 제공 값: `FIREWALL`, `IDS_IPS`, `WAF`, `SIEM`, `EDR`, `DLP`, `ANTIVIRUS`, `NAC`, `VPN`, `OTHER`

목록에 없는 유형이면 **관리 > 코드관리 > `SECURITY_SOLUTION_TYPE`** 에 값을 먼저 추가합니다.

- `value`: 영문 대문자 스네이크 케이스(예: `MAIL_SECURITY`)
- `label`: 화면에 표시할 한글명(예: `메일보안`)
- 코드가 없으면 연동 등록 화면의 유형 선택 목록이 비어 등록할 수 없습니다.

### 2.2 연동 등록

**보안 운영 > 보안이벤트 관리 > 연동 추가**에서 등록합니다. (ADMIN / MANAGER 권한)

| 항목 | 필수 | 설명 |
|------|------|------|
| 연동명(`name`) | ✅ | 장비 단위로 식별 가능한 이름 (예: `본사 WAF #1`) |
| 솔루션 유형(`solutionType`) | ✅ | 위 코드값 |
| 벤더(`vendor`) | | 제조사명 |
| 호스트(`host`) | | 관리 IP 또는 FQDN |
| API 키(`apiKey`) | | 포털이 해당 솔루션을 조회할 때 쓰는 키(보관용) — **이벤트 전송 인증에는 쓰이지 않습니다**([12장](#12-현재-구현-범위와-제약) 참고) |
| 설명(`description`) | | 연동 범위·담당자 등 |

등록 후 부여되는 **연동 ID(`{id}`)** 가 이벤트 전송 경로에 들어갑니다.

### 2.3 전송 계정 준비

이벤트 전송 API는 포털 **JWT 인증**을 사용하며 **ADMIN 또는 MANAGER** 권한이 필요합니다.

- 연동 전용 계정을 별도로 만들어 사용합니다(사람 계정 공용 금지).
- 계정 정보는 어댑터의 설정 파일·시크릿 저장소에 두고 소스코드에 하드코딩하지 않습니다.

---

## 3. API 규격

기본 경로: `/api/security-integrations` · 인증: `Authorization: Bearer <JWT>` · 요청/응답 `Content-Type: application/json; charset=UTF-8`

모든 응답은 공통 래퍼를 사용합니다.

```json
{ "success": true, "message": null, "data": { } }
```

### 3.1 토큰 발급

```
POST /api/auth/login
{ "email": "integration-bot@example.com", "password": "••••••" }
→ data.accessToken 을 이후 요청의 Bearer 토큰으로 사용 (data.expiresIn 은 만료까지 남은 밀리초)
```

> MFA가 켜진 계정은 `data.mfaRequired = true` 와 `tempToken` 만 돌아와 자동 로그인이 불가능합니다.
> **연동 전용 계정은 MFA를 사용하지 않는 계정으로 준비**하세요.

### 3.2 연동 목록 조회

```
GET /api/security-integrations
```

어댑터가 기동 시 자신의 연동 ID를 확인하는 용도로 사용할 수 있습니다.

### 3.3 이벤트 전송 (핵심)

```
POST /api/security-integrations/{id}/events        # ADMIN | MANAGER
```

```json
{
  "severity": "HIGH",
  "eventType": "SQL_INJECTION_BLOCKED",
  "sourceIp": "203.0.113.24",
  "destinationIp": "10.10.20.15",
  "message": "SQL Injection 시도 차단 - URI /login, rule 942100",
  "detectedAt": "2026-07-24T13:22:41"
}
```

응답: `201 Created`, `data` 에 저장된 이벤트(이벤트 ID 포함).

### 3.4 이벤트 조회 · 삭제

```
GET    /api/security-integrations/{id}/events?page=0&size=50
DELETE /api/security-integrations/events/{eventId}      # ADMIN | MANAGER
```

### 3.5 응답 코드 처리

| 코드 | 의미 | 어댑터 처리 |
|------|------|-------------|
| `201` | 등록 성공 | 다음 이벤트 진행 |
| `400` | 필수 필드 누락·형식 오류 | **재시도 금지**, 오류 로그 후 폐기(또는 DLQ) |
| `401` | 토큰 없음·만료 | 토큰 재발급 후 1회 재시도 |
| `403` | 권한 부족(USER 계정) | 재시도 금지, 계정 권한 확인 |
| `404` | 연동 ID 없음(삭제됨) | 재시도 금지, 연동 재등록 필요 |
| `5xx` / 타임아웃 | 서버·네트워크 오류 | 지수 백오프 재시도 |

---

## 4. 필드 규격 및 매핑 규칙

| 필드 | 타입 | 필수 | 규격 |
|------|------|------|------|
| `severity` | enum | ✅ | `CRITICAL` \| `HIGH` \| `MEDIUM` \| `LOW` \| `INFO` |
| `eventType` | string | ✅ | 영문 대문자 스네이크 케이스, 60자 이내 권장 (예: `PORT_SCAN_DETECTED`) |
| `message` | string | ✅ | 한 줄 요약. 무엇을 · 어디서 · 어떻게 처리했는지. 500자 이내 권장 |
| `sourceIp` | string | | 출발지 IP. 사설/공인 구분 없이 원본 그대로. IPv6 허용 |
| `destinationIp` | string | | 목적지 IP |
| `detectedAt` | datetime | | 솔루션이 **탐지한 시각** (`yyyy-MM-ddTHH:mm:ss`) |

### 4.1 심각도 매핑

솔루션 등급을 포털 5단계로 변환합니다. **매핑표를 연동 문서에 명시하고 임의로 바꾸지 않습니다.**

| 포털 등급 | 기준 | 예시 |
|-----------|------|------|
| `CRITICAL` | 침해 성공·확산 우려, 즉시 대응 | 랜섬웨어 실행, 관리자 계정 탈취, 대량 유출 |
| `HIGH` | 공격 성공 가능성 높음 / 차단됐으나 표적 공격 | 웹셸 업로드 시도, 반복 인젝션 공격 |
| `MEDIUM` | 정책 위반·의심 행위 | 비인가 포트 접근, 대량 다운로드 |
| `LOW` | 경미한 위반·노이즈성 탐지 | 단발 스캔, 차단된 저위험 시그니처 |
| `INFO` | 정보성 | 정책 변경, 시그니처 업데이트, 연동 상태 |

- CVSS 점수를 쓰는 솔루션: 9.0↑ `CRITICAL`, 7.0↑ `HIGH`, 4.0↑ `MEDIUM`, 0.1↑ `LOW`.
- 5단계 외 등급(예: `Emergency`)은 가장 가까운 상위 등급으로 올려 매핑합니다.

### 4.2 `eventType` 명명 규칙

- `<대상/기법>_<결과>` 형태를 권장: `BRUTE_FORCE_BLOCKED`, `MALWARE_QUARANTINED`, `USB_WRITE_DENIED`
- 솔루션 고유 룰 ID는 `eventType`이 아니라 `message`에 넣습니다(룰 ID는 유형이 아님).
- 같은 의미의 이벤트는 **솔루션이 달라도 같은 `eventType`** 을 쓰는 것을 권장합니다(통계 집계가 목적).

### 4.3 시각(`detectedAt`) 규칙

- 형식: `yyyy-MM-ddTHH:mm:ss` (예: `2026-07-24T13:22:41`)
- **KST 로컬 시각**으로 보냅니다. 솔루션이 UTC로 기록하면 어댑터가 +09:00 변환 후 전송합니다.
- 생략하면 포털 수신 시각만 남아 탐지-수집 지연을 구분할 수 없으므로 **가급적 채워 보냅니다.**

### 4.4 `message` 작성 규칙

- 한 줄로 요약하고 개행·탭·원본 로그 전체 붙여넣기를 하지 않습니다.
- **개인정보·자격증명을 넣지 않습니다**(주민등록번호·계정 비밀번호·토큰·카드번호). 필요한 경우 마스킹(`user***@corp.com`).
- 필요한 부가정보는 `키=값` 형태로 덧붙입니다: `URI=/admin, rule=942100, action=block`

---

## 5. 솔루션 유형별 매핑 예시

| 유형 | 원본 예시 | `eventType` | `severity` | `message` 예시 |
|------|-----------|-------------|-----------|----------------|
| `FIREWALL` | deny inbound 203.0.113.7→10.0.0.5:3389 | `POLICY_DENY` | `MEDIUM` | `비인가 RDP 접근 차단 - port=3389, policy=DENY_EXT_RDP` |
| `IDS_IPS` | ET SCAN Nmap | `PORT_SCAN_DETECTED` | `LOW` | `포트 스캔 탐지 - sid=2009582, count=120` |
| `WAF` | ModSecurity 942100 | `SQL_INJECTION_BLOCKED` | `HIGH` | `SQL Injection 차단 - URI=/login, rule=942100` |
| `EDR` | Ransomware behavior blocked | `RANSOMWARE_BLOCKED` | `CRITICAL` | `랜섬웨어 행위 차단 - host=PC-1023, proc=enc.exe` |
| `DLP` | 개인정보 포함 메일 발송 차단 | `DATA_EXFIL_BLOCKED` | `HIGH` | `개인정보 포함 메일 차단 - 채널=SMTP, 항목=주민번호(건수 12)` |
| `ANTIVIRUS` | Trojan quarantined | `MALWARE_QUARANTINED` | `HIGH` | `악성코드 격리 - name=Trojan.GenericKD, host=PC-2201` |
| `NAC` | 비인가 단말 차단 | `UNAUTHORIZED_DEVICE_BLOCKED` | `MEDIUM` | `비인가 단말 접속 차단 - mac=00:11:22:33:44:55` |
| `VPN` | 다중 실패 로그인 | `BRUTE_FORCE_DETECTED` | `HIGH` | `VPN 로그인 연속 실패 - account=user***, count=15` |
| `SIEM` | 상관분석 룰 발생 | `CORRELATION_ALERT` | `HIGH` | `상관분석 경보 - rule=계정탈취 의심 3단계` |

---

## 6. 전송 규칙 — 배치·재시도·중복 방지

- **전송 단위**: 이벤트 API는 **단건 등록**입니다. 다건은 순차 호출하되 **초당 10건 이하**를 권장합니다(대량 유입 시 화면·DB 부하).
- **집약 전송**: 동일 `eventType` + `sourceIp` 가 짧은 시간에 반복되면 어댑터에서 **집약**해 1건으로 보내고 `count=N`을 `message`에 남깁니다. 원본 1:1 전송은 금지합니다.
- **중복 방지**: 포털은 중복 판정을 하지 않습니다. 어댑터가 **전송 완료 오프셋(마지막 처리 로그 시각/ID)** 을 보관해 재전송을 막아야 합니다.
- **재시도**: `5xx`·타임아웃만 재시도하며 **지수 백오프(1s → 2s → 4s → 8s, 최대 5회)** 를 사용합니다. 실패분은 로컬 큐에 보관 후 복구 시 순서대로 전송합니다.
- **타임아웃**: connect 5초 / read 10초 권장.
- **전송 지연 목표**: 탐지 후 5분 이내 전송(`CRITICAL`은 1분 이내).

---

## 7. 연동 상태 관리

연동은 `CONNECTED` / `DISCONNECTED` / `ERROR` 상태를 가집니다.

```
PATCH /api/security-integrations/{id}     # ADMIN | MANAGER
{ "status": "CONNECTED" }
```

- 어댑터는 **기동 성공 시 `CONNECTED`**, 솔루션 연결 실패가 지속되면 **`ERROR`**, 정상 종료 시 **`DISCONNECTED`** 로 갱신하는 것을 권장합니다.
- 상태는 화면 좌측 연동 목록의 점 색(초록/회색/빨강)으로 표시됩니다.
- `lastSyncAt`(마지막 수집 시각)은 서버가 자동 갱신하지 않습니다. 필요하면 상태 갱신과 함께 운영 규칙을 정해 관리하세요.

---

## 8. 보안 요구사항

연동 개발 시 **반드시** 지켜야 합니다.

1. **전송 구간 암호화** — 포털 접근은 HTTPS만 사용합니다(평문 HTTP 금지).
2. **전용 계정 · 최소 권한** — 연동 전용 계정을 쓰고 다른 용도로 공유하지 않습니다. 사용하지 않게 되면 즉시 비활성화합니다.
3. **자격증명 보관** — 토큰·비밀번호·API 키는 설정 파일 권한 제한(600) 또는 시크릿 저장소에 보관하고, 로그·소스·이슈트래커에 남기지 않습니다.
4. **개인정보 최소화** — `message`에 개인정보를 담지 않습니다. 계정·이메일은 마스킹합니다. (개인정보보호법 제29조 안전조치 의무)
5. **원본 로그 미전송** — 페이로드 전문·패킷 덤프는 보내지 않습니다. 필요한 경우 솔루션 콘솔 링크나 룰 ID만 남깁니다.
6. **토큰 수명 관리** — 토큰 만료 시 자동 재발급하되, 재발급 실패가 반복되면 `ERROR` 상태로 표시하고 알림을 발생시킵니다.
7. **감사 추적** — 어댑터는 전송 성공/실패 건수를 자체 로그로 남겨 대사(reconciliation)가 가능해야 합니다.

---

## 9. 연동 예제

### 9.1 curl

```bash
TOKEN=$(curl -s -X POST https://portal.example.com/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"integration-bot@example.com","password":"'"$BOT_PW"'"}' | jq -r .data.accessToken)

curl -X POST https://portal.example.com/api/security-integrations/3/events \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "severity": "HIGH",
    "eventType": "SQL_INJECTION_BLOCKED",
    "sourceIp": "203.0.113.24",
    "destinationIp": "10.10.20.15",
    "message": "SQL Injection 차단 - URI=/login, rule=942100",
    "detectedAt": "2026-07-24T13:22:41"
  }'
```

### 9.2 Python 어댑터 골격

```python
import time, requests
from datetime import datetime

BASE = "https://portal.example.com/api"
INTEGRATION_ID = 3

class PortalClient:
    def __init__(self, email, password):
        self.email, self.password, self.token = email, password, None

    def _login(self):
        r = requests.post(f"{BASE}/auth/login",
                          json={"email": self.email, "password": self.password}, timeout=(5, 10))
        r.raise_for_status()
        self.token = r.json()["data"]["accessToken"]

    def send(self, event):
        if not self.token:
            self._login()
        for attempt in range(5):
            r = requests.post(f"{BASE}/security-integrations/{INTEGRATION_ID}/events",
                              json=event,
                              headers={"Authorization": f"Bearer {self.token}"},
                              timeout=(5, 10))
            if r.status_code == 201:
                return True
            if r.status_code == 401:          # 토큰 만료 → 1회 재발급
                self._login()
                continue
            if r.status_code in (400, 403, 404):   # 재시도 무의미
                log_dead_letter(event, r.text)
                return False
            time.sleep(2 ** attempt)          # 5xx·타임아웃 → 지수 백오프
        return False

def to_event(raw):
    """솔루션 로그 → 포털 규격 정규화 (심각도 매핑표는 4.1 참고)"""
    return {
        "severity": SEVERITY_MAP.get(raw["level"], "MEDIUM"),
        "eventType": EVENT_TYPE_MAP.get(raw["rule_id"], "UNKNOWN_EVENT"),
        "sourceIp": raw.get("src_ip"),
        "destinationIp": raw.get("dst_ip"),
        "message": f'{raw["rule_name"]} - rule={raw["rule_id"]}, action={raw["action"]}',
        "detectedAt": datetime.fromtimestamp(raw["ts"]).strftime("%Y-%m-%dT%H:%M:%S"),
    }
```

---

## 10. 포털 확장 가이드 (포털 개발자용)

새 솔루션 유형을 포털에 정식 추가할 때 손대야 할 지점입니다.

| 대상 | 파일 | 할 일 |
|------|------|-------|
| 기본 코드 시드 | `backend/.../code/service/CodeDataInitializer.java` (`initSecuritySolutionTypes`) | 새 유형을 목록에 추가. **기존 설치본은 코드그룹이 이미 있으면 시드를 건너뛰므로**, 운영 반영은 코드관리 화면 또는 마이그레이션으로 별도 처리 |
| 화면 약칭 | `frontend/src/views/security/SecurityEventView.vue` (`solutionTypeAbbr`) | 배지에 쓸 3자 내외 약칭 추가(미등록 시 앞 3글자 자동 사용) |
| 이벤트 등급 | `backend/.../security/entity/SecurityEvent.java` (`EventSeverity`) | 5단계 고정. **변경 시 MySQL ENUM 마이그레이션 필요** |
| API 문서 | `docs/api.md` | 엔드포인트·필드 변경 시 갱신 |

새 필드를 이벤트에 추가하는 경우: `SecurityEvent` 엔티티 → `SecurityDto.EventCreateRequest`/`EventResponse` → 화면 표 순서로 반영하고, **nullable 컬럼으로 추가**하면 `ddl-auto: update` 로 자동 반영됩니다(신규 설치 스키마 `db/init/*.sql` 도 함께 갱신).

---

## 11. 연동 완료 체크리스트

연동 개발자는 아래를 모두 확인한 뒤 운영 반영합니다.

- [ ] `SECURITY_SOLUTION_TYPE` 에 해당 솔루션 유형 코드가 있다
- [ ] 포털에 연동을 등록하고 **연동 ID**를 어댑터 설정에 반영했다
- [ ] 연동 전용 계정(MANAGER 이상)을 발급받아 시크릿으로 보관했다
- [ ] 심각도 매핑표를 문서화했고 5단계 밖 등급이 없다
- [ ] `eventType` 명명 규칙(대문자 스네이크)을 따랐다
- [ ] `detectedAt` 을 KST 로컬 시각으로 보낸다
- [ ] `message` 에 개인정보·자격증명·원본 페이로드가 없다
- [ ] 반복 이벤트 집약(1건 + `count=N`) 로직이 있다
- [ ] 전송 오프셋을 보관해 재기동 시 중복 전송이 없다
- [ ] `5xx`만 지수 백오프로 재시도하고 `4xx`는 DLQ로 보낸다
- [ ] 기동/장애/종료 시 연동 상태(`CONNECTED`/`ERROR`/`DISCONNECTED`)를 갱신한다
- [ ] 테스트 이벤트 5종(각 심각도)을 전송해 화면 표시·필터·통계를 확인했다
- [ ] 운영 전환 전 테스트 이벤트를 삭제했다

---

## 12. 현재 구현 범위와 제약

연동 설계 시 아래를 전제로 합니다. (2026-07 기준 구현)

| 항목 | 현재 상태 |
|------|-----------|
| 수집 방식 | **Push 전용**. 포털이 솔루션을 폴링하는 스케줄러는 없습니다. |
| 이벤트 등록 | **단건 등록만** 지원(`POST .../events`). 벌크 등록 API는 없습니다. |
| 인증 | 포털 **JWT(ADMIN/MANAGER)**. 연동의 `apiKey` 필드는 **저장만 되고 이벤트 인증에 사용되지 않습니다** — 솔루션 조회용 자격증명 보관 목적입니다. |
| 중복 제거 | 서버 측 중복 판정 없음. 어댑터 책임입니다. |
| `lastSyncAt` | 이벤트 수신 시 자동 갱신되지 않습니다. |
| 원본 로그 | 저장하지 않습니다. 요약 이벤트만 보관합니다. |
| 이벤트 보존 | 자동 삭제(리텐션) 정책이 없습니다. 필요 시 이벤트 삭제 API로 정리합니다. |

> 벌크 수집·전용 API 키 인증·자동 폴링이 필요하면 별도 요구사항으로 제기해 주세요. 위 제약을 전제로 어댑터를 설계해야 합니다.
