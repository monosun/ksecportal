# KSecPortal v1.42.2 릴리즈 노트

**릴리즈 일자**: 2026-10-22

OWASP Top 10 기준 자체 보안 점검에서 확인된 **권한 누락·비밀값 평문 저장·키 관리 취약점**을 수정한 보안 패치입니다.
**마이그레이션 필수** — `v1.42.2_secret_column_encryption.sql` 을 백엔드 재기동 **전에** 적용하세요.

---

## 보안 수정

### 1. 쓰기·삭제 API 권한 누락 (A01 접근 통제 실패)

일부 변경 API에 서버 권한 검사가 없어, 메뉴가 보이지 않는 **일반 USER 계정도 API를 직접 호출하면** 데이터를 바꾸거나 지울 수 있었습니다.

| 대상 | 영향 | 수정 |
|------|------|------|
| ISMS-P 증적 등록·수정·삭제·첨부 삭제·참조 등록·**일괄 가져오기** | 인증심사 증적 삭제·변조 | 메뉴 권한 `isms` 쓰기/삭제 검사 |
| 월간 보안점검 등록·수정·삭제·**월 전체 삭제**·기본항목·이전 달 복사·증적 | 점검 기록 조작 | 메뉴 권한 `monthly_checks` 쓰기/삭제 검사 |
| 보안성 심의 **첨부(설계서) 교체** | 다른 사람의 심의 근거 자료를 승인 전에 바꿔치기 | 요청자 본인 또는 ADMIN·MANAGER 만, 요청자는 심의 완료 후 변경 불가 |

권한은 화면과 같은 **메뉴별 권한(권한관리 > 기본 역할·커스텀 Role)** 으로 판단합니다(`@menuAuth.canWrite(...)`).
관리자가 커스텀 Role 로 USER 에게 쓰기 권한을 준 경우는 그대로 동작하고, 권한이 없으면 `403` 이 반환됩니다.
조회·다운로드는 변경하지 않았습니다.

### 2. 비밀값 평문 저장 (A02 암호화 실패)

다음 값이 DB에 평문으로 저장되어, DB나 백업이 유출되면 MFA 무력화와 외부 시스템 접근으로 이어질 수 있었습니다.
개인정보 컬럼과 같은 **AES-256-GCM** 컬럼 암호화를 적용했습니다.

| 컬럼 | 내용 |
|------|------|
| `users.mfa_secret` | MFA(TOTP) 비밀키 |
| `mail_config.password` | SMTP 인증 비밀번호 |
| `security_integrations.api_key` | 보안 솔루션 연동 API 키 |
| `github_config.token` | 소스 취약점 점검용 GitHub 토큰 |

마이그레이션 적용 후 백엔드를 재기동하면 기존 평문 값이 자동으로 암호화됩니다(`PiColumnEncryptionBackfill`).

### 3. 공개된 기본 키로 암호화되는 문제 (A02·A05)

개인정보 암호화 키(`PI_ENCRYPTION_KEY`)를 비워 두면 Jasypt 마스터 키를 쓰는데, 그 기본값(`dev-local-key`)이 공개 저장소에 있어
키를 바꾸지 않은 설치는 **누구나 개인정보 암호문을 풀 수 있었습니다.** 기동 점검이 있었지만 경고만 했고,
중단 옵션을 켜도 백필이 기본 키로 데이터를 암호화한 **뒤에야** 실행되었습니다.

- 기동 점검(`SecretsStartupCheck`)을 **모든 시더·백필보다 먼저** 실행하도록 변경
- 점검 항목을 **차단**(공개 기본 키·짧은 키·샘플 DB 비밀번호·기본 JWT 시크릿·개인정보 암호화 꺼짐·약한 `PI_ENCRYPTION_KEY`)과 **권고**(평문 주입 등)로 분리 — 차단 항목만 기동을 멈춥니다
- `.env.example` 의 `SECURITY_FAIL_ON_INSECURE_SECRETS` 기본값을 **`true`** 로 변경
- `start.sh`·`start.ps1` 이 `.env` 를 자동 생성할 때(로컬 체험용)는 `false` 로 만들고 경고를 출력

### 4. Okta 로그인 토큰 수신자(`aud`) 미검증 (A07)

ID 토큰의 서명·issuer·만료만 확인하고 수신자(`aud`)를 확인하지 않았습니다. `aud` 에 설정된 client-id 가 없으면 로그인을 거부합니다.

### 5. 설정값 API 의 비밀값 노출 (A01)

`GET /api/app-settings` 가 로그인한 모든 사용자에게 **법제처 API 키(`lawApiKey`)·백업 기본 비밀번호(`backup.default.password`)** 를 그대로 반환했습니다.
ADMIN 이 아니면 `********` 로 가려서 반환합니다(대시보드의 "키 등록 여부" 표시는 그대로 동작).

## 영향 범위

- **DB 변경 있음** — 위 4개 컬럼 길이 확대(데이터 변경은 재기동 시 자동 암호화)
- API: 권한 없는 쓰기·삭제 요청은 `403`. 화면의 버튼은 그대로 보이며, 누르면 권한 오류가 표시됩니다
- `.env.example` 로 새로 만든 `.env` 는 기본 키가 남아 있으면 **백엔드가 기동되지 않습니다** — 2단계 "반드시 변경" 값을 바꾸세요

## 업그레이드

```bash
# 1) DB 백업
docker compose exec -T db mysqldump -usecportal -p<비밀번호> secportal > backup_$(date +%F).sql

# 2) 새 버전 받기
git pull                     # 또는 git checkout v1.42.2

# 3) 마이그레이션 (백엔드 재기동 전에)
docker compose exec -T db mysql --default-character-set=utf8mb4 -usecportal -p<비밀번호> secportal \
  < db/migration/v1.42.2_secret_column_encryption.sql

# 4) 다시 빌드·기동
docker compose build backend
docker compose up -d backend
docker compose logs backend --tail 50   # "[개인정보 암호화] 평문으로 남아 있던 ... 암호화했습니다" 확인
```

> - 마이그레이션을 빼고 재기동하면 `users.mfa_secret`(64자)에 암호문이 들어가지 않아 **MFA 등록이 실패**합니다.
> - 기존 `.env` 는 바뀌지 않으므로 `SECURITY_FAIL_ON_INSECURE_SECRETS` 는 이전 값을 유지합니다. 운영에서는 `true` 로 두기를 권장합니다.
> - **이미 기본 키(`dev-local-key`)로 운영 중이던 설치**는 키를 바꾸면 기존 암호문을 풀 수 없습니다(`(복호화 실패)`). 키 교체·재암호화 기능은 후속 릴리즈에서 제공할 예정이며, 그 전까지는 키를 바꾸지 말고 서버·`.env` 접근 통제를 강화하세요.

## 검증

격리된 검증 환경(별도 DB 볼륨)에서 다음을 확인했습니다.

- 기본 키 + `SECURITY_FAIL_ON_INSECURE_SECRETS=true` → 시더·백필 실행 전에 기동 중단(코드값·권한 행 0건)
- 안전한 키 → 정상 기동
- 기본 USER: ISMS 증적·월간점검 쓰기/삭제 `403`, 조회 `200` / MANAGER·ADMIN: `200`·`201`
- 커스텀 Role(`isms` 읽기·쓰기) 부여 USER: 증적 등록·수정 허용, 삭제 `403`, 다른 메뉴 쓰기 `403`
- 보안성 심의 첨부: 타 사용자 `403`, 요청자 완료 전 허용·완료 후 거부, MANAGER 허용
- `/app-settings`: USER·MANAGER 는 `********`, ADMIN 은 원문
- v1.42.1 스키마 + 평문 비밀값 → 마이그레이션 → 재기동 시 4건 암호화(`PIENC1:`), 암호화된 MFA 비밀키로 TOTP 로그인 성공

Okta `aud` 검증은 Okta 테넌트가 없어 실제 로그인으로는 확인하지 못했습니다(컴파일·코드 검토만).
