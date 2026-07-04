# Okta SSO 연동 매뉴얼

SecPortal에 Okta Single Sign-On(SSO)을 연동하는 방법을 단계별로 설명합니다.  
이 매뉴얼을 따라 설정하면 조직 Okta 계정으로 SecPortal에 로그인할 수 있습니다.

## 목차

1. [동작 방식](#1-동작-방식)
2. [Okta 애플리케이션 생성](#2-okta-애플리케이션-생성)
3. [DB 마이그레이션](#3-db-마이그레이션)
4. [환경변수 설정](#4-환경변수-설정)
5. [서비스 재시작](#5-서비스-재시작)
6. [로그인 테스트](#6-로그인-테스트)
7. [사용자 계정 연동 방식](#7-사용자-계정-연동-방식)
8. [트러블슈팅](#8-트러블슈팅)
9. [비활성화 방법](#9-비활성화-방법)

---

## 1. 동작 방식

SecPortal은 **OAuth 2.0 PKCE(Proof Key for Code Exchange)** 방식으로 Okta와 연동합니다.  
클라이언트 시크릿 없이 동작하므로 SPA 환경에서도 안전합니다.

```
[사용자] → "Okta로 로그인" 클릭
     ↓
[브라우저] PKCE code_verifier / code_challenge 생성 → Okta /v1/authorize 리다이렉트
     ↓
[Okta] 로그인 화면 표시 → 인증 완료 → /auth/okta/callback?code=...&state=... 리다이렉트
     ↓
[브라우저] code + code_verifier → 백엔드 POST /auth/okta/token 호출
     ↓
[백엔드] Okta /v1/token 에서 ID 토큰 교환 → JWKS로 서명 검증 → 사용자 조회/생성
     ↓
[백엔드] SecPortal JWT 발급 → 브라우저에 반환
     ↓
[사용자] 대시보드 접속
```

**보안 특성:**
- 클라이언트 시크릿 불필요 (PKCE 방식)
- State 파라미터로 CSRF 방지
- Okta JWKS 엔드포인트로 ID 토큰 서명 자동 검증
- 기존 이메일 계정에 Okta ID 자동 연동

---

## 2. Okta 애플리케이션 생성

### 2-1. Okta 개발자 계정 준비

아직 Okta 계정이 없다면 [developer.okta.com](https://developer.okta.com)에서 무료 개발자 계정을 생성합니다.  
기업 Okta 테넌트가 있다면 해당 관리자 콘솔에서 진행합니다.

### 2-2. 신규 애플리케이션 생성

1. Okta 관리자 콘솔 접속: `https://dev-XXXXXX-admin.okta.com`
2. 좌측 메뉴 → **Applications** → **Applications** 클릭
3. **Create App Integration** 클릭

### 2-3. 앱 통합 유형 선택

| 항목 | 선택값 |
|------|--------|
| Sign-in method | **OIDC - OpenID Connect** |
| Application type | **Single-Page Application** |

**Next** 클릭

### 2-4. 앱 설정

| 항목 | 값 |
|------|-----|
| App integration name | `SecPortal` (원하는 이름) |
| Grant types | **Authorization Code** 체크, PKCE 자동 활성화 |
| Sign-in redirect URIs | 아래 표 참고 |
| Sign-out redirect URIs | (선택사항, 비워도 됨) |
| Controlled access | **Allow everyone in your organization to access** 또는 특정 그룹 지정 |

**Sign-in redirect URIs 설정:**

| 환경 | URI |
|------|-----|
| 개발 환경 | `http://localhost:5173/auth/okta/callback` |
| 운영 환경 | `https://yourdomain.com/auth/okta/callback` |

> 두 URI를 모두 추가하면 개발/운영 환경을 동시에 사용할 수 있습니다.

**Save** 클릭

### 2-5. 앱 정보 수집

앱 생성 후 **General** 탭에서 다음 값을 복사합니다:

| 항목 | 환경변수 |
|------|---------|
| **Client ID** | `OKTA_CLIENT_ID` |
| — | — |

좌측 메뉴 → **Security** → **API** → **Authorization Servers** 탭에서 `default` 서버의 **Issuer URI**를 복사합니다:

| 항목 | 예시 | 환경변수 |
|------|------|---------|
| **Issuer URI** | `https://dev-XXXXXX.okta.com/oauth2/default` | `OKTA_ISSUER` |

> **주의:** Issuer URI는 `https://dev-XXXXXX.okta.com` (도메인만) 이 아닌  
> `https://dev-XXXXXX.okta.com/oauth2/default` 형식이어야 합니다.

---

## 3. DB 마이그레이션

Okta 사용자 ID를 저장하는 컬럼을 추가합니다.

```bash
# Docker 환경
docker exec -i secportal-db mysql -u secportal -psecportal123 secportal \
  < db/migration/v1.54.0_okta_user_id.sql
```

또는 MySQL 클라이언트에서 직접 실행:

```sql
ALTER TABLE users ADD COLUMN okta_id VARCHAR(255) UNIQUE NULL AFTER locked_until;
```

적용 후 확인:

```sql
SHOW COLUMNS FROM users LIKE 'okta_id';
-- okta_id | varchar(255) | YES | UNI | NULL | 가 출력되면 정상
```

---

## 4. 환경변수 설정

### 4-1. `.env` 파일 수정

프로젝트 루트의 `.env` 파일에 다음 항목을 추가합니다:

```bash
# Okta SSO 연동
OKTA_ENABLED=true
OKTA_CLIENT_ID=0oaXXXXXXXXXXXXXXXXX
OKTA_ISSUER=https://dev-XXXXXX.okta.com/oauth2/default
OKTA_REDIRECT_URI=https://yourdomain.com/auth/okta/callback
```

**개발 환경 예시:**

```bash
OKTA_ENABLED=true
OKTA_CLIENT_ID=0oaABCDEF1234567890
OKTA_ISSUER=https://dev-123456.okta.com/oauth2/default
OKTA_REDIRECT_URI=http://localhost:5173/auth/okta/callback
```

**운영 환경 예시:**

```bash
OKTA_ENABLED=true
OKTA_CLIENT_ID=0oaABCDEF1234567890
OKTA_ISSUER=https://mycompany.okta.com/oauth2/default
OKTA_REDIRECT_URI=https://secportal.mycompany.com/auth/okta/callback
```

### 4-2. 환경변수 설명

| 변수명 | 필수 | 설명 |
|--------|------|------|
| `OKTA_ENABLED` | ✅ | `true` 로 설정해야 로그인 버튼이 표시됩니다 |
| `OKTA_CLIENT_ID` | ✅ | Okta 앱의 Client ID |
| `OKTA_ISSUER` | ✅ | Okta Authorization Server의 Issuer URI |
| `OKTA_REDIRECT_URI` | ✅ | Okta 앱에 등록한 Sign-in redirect URI와 **정확히 일치**해야 합니다 |

> `OKTA_ENABLED=false` (기본값)이면 로그인 화면에 Okta 버튼이 표시되지 않습니다.  
> Okta를 사용하지 않는 배포 환경에서는 나머지 변수를 비워두어도 됩니다.

---

## 5. 서비스 재시작

환경변수 변경 후 백엔드 컨테이너를 재시작합니다:

```bash
# 백엔드만 재시작
docker compose restart backend

# 또는 전체 재빌드 (코드 변경이 있는 경우)
docker compose up -d --build
```

백엔드 로그에서 정상 시작 확인:

```bash
docker compose logs -f backend | head -30
# "Started SecPortalApplication" 메시지 확인
```

---

## 6. 로그인 테스트

### 6-1. 로그인 화면 확인

SecPortal 로그인 화면(`/login`)에 접속하면 이메일/비밀번호 입력 폼 아래에 **"Okta로 로그인"** 버튼이 표시됩니다.

> 버튼이 표시되지 않으면 `OKTA_ENABLED=true` 설정 및 백엔드 재시작 여부를 확인합니다.

### 6-2. Okta SSO 로그인

1. **"Okta로 로그인"** 버튼 클릭
2. Okta 로그인 화면으로 리다이렉트됨
3. Okta 계정 이메일·비밀번호 입력 (또는 조직 IdP 로그인)
4. 인증 완료 후 SecPortal 대시보드로 자동 이동

### 6-3. API로 Okta 설정 상태 확인

```bash
curl https://yourdomain.com/api/auth/okta/config
```

정상 응답 예시:

```json
{
  "success": true,
  "data": {
    "enabled": true,
    "clientId": "0oaABCDEF1234567890",
    "issuer": "https://dev-123456.okta.com/oauth2/default",
    "redirectUri": "https://yourdomain.com/auth/okta/callback"
  }
}
```

---

## 7. 사용자 계정 연동 방식

Okta로 로그인하면 아래 순서로 SecPortal 사용자를 처리합니다:

```
Okta 로그인 완료
      ↓
Okta ID(sub)로 기존 사용자 조회
      ↓ 없으면
이메일로 기존 사용자 조회 → 있으면 Okta ID 연동 후 로그인
      ↓ 없으면
신규 사용자 자동 생성 (역할: USER)
```

### 자동 생성 계정 속성

| 항목 | 값 |
|------|-----|
| 이메일 | Okta 계정 이메일 |
| 이름 | Okta 프로필 이름 (`name` 클레임) |
| 역할 | `USER` (로그인 후 관리자가 변경 가능) |
| 비밀번호 | 설정 불가 (Okta 전용 계정) |
| MFA | 비활성화 (Okta 측에서 별도 관리) |

### 기존 SecPortal 계정과 자동 연동

SecPortal에 `hong@company.com`으로 가입된 사용자가 Okta(`hong@company.com`)로 처음 로그인하면 **자동으로 두 계정이 연결**됩니다.  
이후에는 이메일/비밀번호 또는 Okta 중 하나로 로그인할 수 있습니다.

연동 후 DB 상태 확인:

```sql
SELECT id, email, name, role, okta_id FROM users WHERE email = 'hong@company.com';
-- okta_id 컬럼에 Okta의 sub 값(예: 00uXXXXXXXXXXXXXXXXX)이 채워지면 연동 완료
```

### 역할 변경 (관리자)

Okta를 통해 처음 가입된 사용자는 기본 `USER` 역할로 생성됩니다.  
역할 변경은 SecPortal **관리자 → 사용자 관리** 메뉴에서 수행합니다.

---

## 8. 트러블슈팅

### Okta 버튼이 로그인 화면에 표시되지 않음

**원인:** 백엔드가 `OKTA_ENABLED=true`를 인식하지 못하거나 설정이 누락됨

```bash
# 백엔드 환경변수 확인
docker exec secportal-backend env | grep OKTA

# 예상 출력
OKTA_ENABLED=true
OKTA_CLIENT_ID=0oaXXXXXXXXXXXXXXXXX
OKTA_ISSUER=https://dev-XXXXXX.okta.com/oauth2/default
OKTA_REDIRECT_URI=https://yourdomain.com/auth/okta/callback
```

설정 확인 후 `docker compose restart backend` 실행

---

### "redirect_uri_mismatch" 오류 (Okta 화면에서 발생)

**원인:** `OKTA_REDIRECT_URI` 값이 Okta 앱에 등록된 URI와 다름

해결 방법:
1. Okta 관리자 콘솔 → Applications → SecPortal 앱 → **General** 탭
2. **Sign-in redirect URIs**에 현재 `OKTA_REDIRECT_URI` 값이 있는지 확인
3. 없으면 **Edit** → URI 추가 → **Save**

> 프로토콜(`http`/`https`), 도메인, 포트, 경로가 **모두 정확히 일치**해야 합니다.

---

### "Okta 토큰 검증에 실패했습니다" 오류

**원인 1:** `OKTA_ISSUER` 값이 잘못됨

```bash
# JWKS 엔드포인트 수동 확인 (curl이 응답을 반환해야 함)
curl https://dev-XXXXXX.okta.com/oauth2/default/v1/keys
# {"keys":[{"kty":"RSA",...}]} 형태가 반환되면 정상
```

**원인 2:** OIDC 디스커버리 엔드포인트 미응답

```bash
curl https://dev-XXXXXX.okta.com/oauth2/default/.well-known/openid-configuration
# {"issuer":"https://...","authorization_endpoint":"...",...} 가 반환되면 정상
```

백엔드 로그에서 상세 오류 확인:

```bash
docker compose logs backend | grep -i "okta\|jwt\|token"
```

---

### "비활성화된 계정입니다" 오류

**원인:** SecPortal에서 해당 이메일 계정이 비활성화(active=false)됨

관리자 계정으로 SecPortal 접속 → **사용자 관리** → 해당 계정 활성화 처리

```sql
-- 또는 DB 직접 수정
UPDATE users SET active = true WHERE email = 'user@company.com';
```

---

### Okta 로그인 후 "Okta 계정에 이메일 정보가 없습니다" 오류

**원인:** Okta ID 토큰에 `email` 클레임이 포함되지 않음

해결 방법:
1. Okta 관리자 콘솔 → **Applications** → SecPortal → **Sign On** 탭
2. **OpenID Connect ID Token** 섹션에서 **Edit** 클릭
3. **Groups claim type**: Expression, `Groups.startsWith("", "", 100)` 등 필요 시 설정
4. 또는 **API** → **Authorization Servers** → **default** → **Claims** 탭에서  
   `email` 클레임이 ID Token에 포함되는지 확인

기본 `openid profile email` 스코프로 요청하므로 Okta 앱에서 `email` 클레임이 활성화되어 있어야 합니다.

---

### 로그인 후 `state mismatch` 오류 (브라우저 콘솔)

**원인:** 브라우저의 `sessionStorage`가 Okta 리다이렉트 과정에서 초기화됨 (예: 새 창에서 열림)

- Okta 로그인 창이 팝업이 아닌 **동일 창에서** 열리는지 확인
- 브라우저 시크릿 모드에서는 정상 동작; 확장 프로그램이 `sessionStorage`를 차단하는지 확인

---

## 9. 비활성화 방법

Okta 연동을 일시적으로 끄려면 `.env`에서 값을 변경합니다:

```bash
OKTA_ENABLED=false
```

백엔드 재시작 후 로그인 화면에서 Okta 버튼이 사라집니다.  
기존에 Okta로 생성된 계정은 DB에 그대로 유지됩니다.  
해당 계정의 비밀번호를 재설정하려면 관리자가 **사용자 관리** 메뉴에서 임시 비밀번호를 지정합니다.

---

## 부록: Okta 그룹 기반 역할 자동 매핑 (고급)

기본 구현은 모든 Okta 사용자에게 `USER` 역할을 부여합니다.  
Okta 그룹(`SecPortal-Admin`, `SecPortal-Manager` 등)을 SecPortal 역할에 매핑하려면  
`OktaAuthService.findOrCreateUser()` 메서드를 아래와 같이 확장합니다.

**1. Okta 앱에서 그룹 클레임 추가**

Okta 관리자 콘솔 → **API** → **Authorization Servers** → `default` → **Claims** 탭:

| 항목 | 값 |
|------|-----|
| Name | `groups` |
| Include in token type | **ID Token** |
| Value type | **Groups** |
| Filter | Matches regex: `SecPortal.*` |

**2. OktaAuthService 수정**

```java
private User findOrCreateUser(String sub, String email, String name) {
    List<String> groups = jwt.getClaimAsStringList("groups");  // 메서드 시그니처에 jwt 추가 필요
    User.Role role = determineRole(groups);

    return userRepository.findByOktaId(sub).orElseGet(() ->
        userRepository.findByEmail(email).map(existing -> {
            existing.setOktaId(sub);
            existing.setRole(role);  // 매 로그인마다 역할 동기화
            return userRepository.save(existing);
        }).orElseGet(() -> {
            User newUser = User.builder()
                    .email(email)
                    .name(name != null ? name : email)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .oktaId(sub)
                    .role(role)
                    .build();
            return userRepository.save(newUser);
        })
    );
}

private User.Role determineRole(List<String> groups) {
    if (groups == null) return User.Role.USER;
    if (groups.contains("SecPortal-Admin")) return User.Role.ADMIN;
    if (groups.contains("SecPortal-Manager")) return User.Role.MANAGER;
    return User.Role.USER;
}
```
