# SecPortal FAQ — 자주 묻는 오류 및 해결 방법

---

## 인증 / 보안 관련

### Q1. 만료된 JWT 토큰으로 API를 호출하면 어떤 응답이 오나요?

**A.** v1.7.2부터 모든 인증 실패는 일관된 JSON 응답을 반환합니다.

```json
HTTP 401 Unauthorized
{
  "success": false,
  "data": null,
  "message": "Authentication required"
}
```

토큰이 만료되면 프론트엔드에서 `/auth/login`으로 재로그인하도록 처리하세요.

---

### Q2. Spring Security 필터 체인에서 500 오류가 발생합니다

**증상**

서버 로그에 아래와 같은 스택 트레이스가 출력되며, 클라이언트는 500 응답을 받습니다.

```
at org.springframework.security.web.context.SecurityContextHolderFilter.doFilter(...)
at org.springframework.security.web.FilterChainProxy$VirtualFilterChain.doFilter(...)
...
```

**원인**

`SecurityConfig`에 `AuthenticationEntryPoint`와 `AccessDeniedHandler`가 설정되지 않은 경우,
Spring Security는 인증 실패 시 `/login`으로 리다이렉트를 시도합니다.  
REST API 서버에는 `/login` 페이지가 없으므로, 이 리다이렉트가 내부 오류로 연결될 수 있습니다.

**오류 흐름**

```
요청 도착
  → JwtAuthenticationFilter: 토큰 없음 or 만료 → SecurityContext 미설정
  → ExceptionTranslationFilter: 인증 실패 감지
  → AuthenticationEntryPoint 없음 → 기본 동작(redirect to /login)
  → /login 엔드포인트 없음 → 500 발생
```

**해결 방법 (v1.7.2 적용)**

`SecurityConfig`에 `exceptionHandling` 블록을 추가합니다.

```java
.exceptionHandling(ex -> ex
    .authenticationEntryPoint((request, response, authException) -> {
        response.setStatus(401);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
            ApiResponse.error("Authentication required"));
    })
    .accessDeniedHandler((request, response, accessDeniedException) -> {
        response.setStatus(403);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
            ApiResponse.error("Access denied"));
    })
)
```

**수정 후 동작**

| 상황 | 수정 전 | 수정 후 |
|------|---------|---------|
| 토큰 없이 보호된 엔드포인트 호출 | `500` (redirect → 오류) | `401 Authentication required` |
| 만료 토큰으로 보호된 엔드포인트 호출 | `500` (redirect → 오류) | `401 Authentication required` |
| 권한 부족 (ADMIN 전용 API를 USER가 호출) | `500` or 브라우저 오류 페이지 | `403 Access denied` |

---

### Q3. `GET /auth/me` 호출 시 500 오류가 발생합니다

**증상** (v1.7.1 이전)

```
500 Internal Server Error
NullPointerException at AuthController.java
```

**원인**

구버전 `SecurityConfig`에서 `/auth/**` 전체를 `permitAll()`로 설정하여,
토큰 없이도 `/auth/me` 컨트롤러까지 진입이 가능했습니다.  
컨트롤러에서 `@AuthenticationPrincipal UserDetails`가 `null`로 주입되어 NPE가 발생했습니다.

**해결 방법** (v1.7.1에서 수정 완료)

`permitAll()` 대상을 `/auth/login`, `/auth/register`로 축소했습니다.  
`/auth/me`는 JWT 필수 엔드포인트로 변경되었습니다.

> v1.7.1 이상으로 업그레이드하면 자동 해결됩니다.

---

### Q4. `403 Forbidden` 응답이 오는데 원인을 모르겠습니다

**체크리스트**

1. 해당 사용자의 `role`을 확인합니다. 각 API의 권한 요구사항:
   - `USER`: 일반 조회, 교육 수강, 정책 수신 확인
   - `MANAGER`: 생성·수정·삭제, 일괄 업로드, 리포트 다운로드
   - `ADMIN`: 사용자 관리, 감사 로그 조회, 전체 삭제

2. `GET /admin/users/:id` 응답에서 `role`, `active` 필드를 확인합니다.

3. `active: false`인 계정은 인증은 성공해도 권한이 없을 수 있습니다.

---

### Q5. 로그인 후 즉시 토큰이 만료됩니다

**원인 확인**

`.env` 파일의 `JWT_EXPIRATION` 값을 확인하세요.

```bash
# 기본값: 86400000 (24시간, 밀리초 단위)
JWT_EXPIRATION=86400000
```

값이 너무 작게 설정되거나 잘못된 단위(초 단위로 입력)가 적용되었을 수 있습니다.

**JWT_SECRET 길이 오류**

`JWT_SECRET`이 32자 미만이면 서버 기동 시 예외가 발생합니다.  
최소 32자 이상의 랜덤 문자열을 사용하세요.

```bash
# 예: 64자 랜덤 시크릿 생성
openssl rand -base64 48
```

---

## 설치 / 실행 관련

### Q6. Docker 컨테이너가 기동되지 않습니다

**백엔드 로그 확인**

```bash
docker compose logs backend --tail=50
```

**자주 발생하는 원인**

| 오류 메시지 | 원인 | 해결 방법 |
|-------------|------|-----------|
| `Connection refused` (DB) | MySQL이 아직 준비되지 않음 | 잠시 후 재시도 or `healthcheck` 확인 |
| `IllegalArgumentException: The specified key byte array is N bits...` | `JWT_SECRET`이 너무 짧음 | 32자 이상으로 변경 |
| `EncryptionOperationNotPossibleException` | `JASYPT_ENCRYPTOR_PASSWORD` 불일치 | `.env`의 마스터키 확인 |
| `Access denied for user` | DB 비밀번호 불일치 | `DB_PASSWORD` 통일 확인 |

---

### Q7. 파일 업로드(ISMS 증적)가 컨테이너 재시작 후 사라집니다

**원인**

`docker-compose.yml`에 `uploads` 볼륨이 마운트되지 않은 경우, 컨테이너 종료 시 파일이 소실됩니다.

**해결 방법**

`docker-compose.yml`의 `backend` 서비스에 볼륨을 추가합니다:

```yaml
services:
  backend:
    volumes:
      - ./uploads:/app/uploads
```

변경 후 재시작:

```bash
docker compose down && docker compose up -d
```

---

### Q8. `GET /actuator/health`가 403을 반환합니다

**원인**

`SecurityConfig`의 `permitAll` 설정에 `GET /actuator/health`가 누락된 경우입니다.

**확인**

`SecurityConfig.java`에 아래 줄이 있는지 확인합니다:

```java
.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
```

---

## 데이터 관련

### Q9. 엑셀 일괄 업로드 시 일부 행이 실패합니다

응답의 `errors` 배열에서 실패 원인을 확인합니다:

```json
{
  "total": 10,
  "success": 9,
  "failed": 1,
  "errors": [
    { "row": 3, "message": "카테고리 값이 유효하지 않습니다: UNKNOWN" }
  ]
}
```

**자주 발생하는 오류**

| 오류 메시지 | 해결 방법 |
|-------------|-----------|
| `필수 항목이 비어있습니다` | `*` 표시된 컬럼 값 입력 |
| `카테고리 값이 유효하지 않습니다` | API 문서의 허용 값 목록 확인 |
| `이미 존재하는 이메일` | 중복 이메일 제거 |
| `날짜 형식 오류` | `YYYY-MM-DD` 형식으로 입력 |

성공한 행은 이미 저장됩니다. 실패한 행만 수정 후 재업로드하세요.

---

### Q10. CSV/PDF 다운로드 파일명이 깨집니다

**원인**

브라우저가 `Content-Disposition: attachment; filename*=UTF-8''...` 헤더를 올바르게 처리하지 못하는 경우입니다.

**해결 방법**

- Chrome / Edge: 기본 지원 (문제 없음)
- Internet Explorer: 미지원 → Chrome 사용 권장
- 엑셀에서 열 때 인코딩 문제 발생 시 → 파일 저장 후 UTF-8로 열기
