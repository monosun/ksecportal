# SecPortal 보안 설정 가이드

운영 환경에 적용된 nginx, Spring Boot, Docker Compose 보안 설정을 설명합니다.

---

## 목차

1. [nginx 보안 설정](#1-nginx-보안-설정)
2. [Spring Boot 보안 설정](#2-spring-boot-보안-설정)
3. [Docker Compose 보안 설정](#3-docker-compose-보안-설정)
4. [HTTPS 적용 방법](#4-https-적용-방법)
5. [보안 설정 확인 방법](#5-보안-설정-확인-방법)

---

## 1. nginx 보안 설정

관련 파일: `nginx/nginx.conf`, `nginx/nginx-http.conf`

### 1-1. Server 헤더 완전 제거

```nginx
# nginx/nginx.conf (server 블록 내)
more_clear_headers 'Server';
more_clear_headers 'X-Powered-By';
```

`server_tokens off`는 버전 번호만 숨기고 `Server: nginx`는 노출됩니다.  
`more_clear_headers`(headers-more 모듈)를 사용하면 헤더 자체를 제거합니다.

> **모듈 설치**: `frontend/Dockerfile`에서 `debian:bookworm-slim` + `libnginx-mod-http-headers-more-filter` 사용.  
> Debian의 nginx 패키지와 버전이 일치하므로 ABI 충돌 없이 설치됩니다.

```nginx
# nginx/nginx-http.conf (http 블록 내)
server_tokens off;          # 버전 숨김 (headers-more의 보조 설정)
```

### 1-2. HTTP 보안 헤더

```nginx
# nginx/nginx.conf (server 블록 내)
add_header X-Frame-Options           "SAMEORIGIN"                         always;
add_header X-Content-Type-Options    "nosniff"                            always;
add_header X-XSS-Protection          "1; mode=block"                      always;
add_header Referrer-Policy           "strict-origin-when-cross-origin"    always;
add_header Permissions-Policy        "camera=(), microphone=(), geolocation=(), payment=()" always;
add_header Content-Security-Policy
    "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline';
     img-src 'self' data: blob:; font-src 'self' data:; connect-src 'self'; frame-ancestors 'none';"
    always;
```

| 헤더 | 목적 |
|------|------|
| `X-Frame-Options: SAMEORIGIN` | 클릭재킹 방지 — 동일 출처에서만 iframe 허용 |
| `X-Content-Type-Options: nosniff` | MIME 스니핑 방지 |
| `X-XSS-Protection` | 구형 브라우저 XSS 필터 활성화 |
| `Referrer-Policy` | 외부 링크 클릭 시 URL 전체 대신 출처만 전달 |
| `Permissions-Policy` | 카메라·마이크·위치정보·결제 API 접근 차단 |
| `Content-Security-Policy` | 승인된 출처에서만 리소스 로드 허용 |

> **CSP 조정**: 외부 CDN이나 폰트를 사용하는 경우 `connect-src`, `font-src` 등에 해당 도메인을 추가하세요.

### 1-3. HTTPS 적용 후 HSTS 활성화

`nginx/nginx.conf`에서 아래 주석을 해제합니다.

```nginx
# 주석 해제
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
```

| 항목 | 값 | 의미 |
|------|-----|------|
| `max-age` | `31536000` (1년) | 브라우저가 1년간 HTTPS 강제 |
| `includeSubDomains` | — | 서브도메인에도 적용 |
| `preload` | — | 브라우저 HSTS preload 목록 등재 가능 |

### 1-4. Rate Limiting (요청 속도 제한)

```nginx
# nginx/nginx-http.conf (http 블록 내 — 존 선언)
limit_req_zone $binary_remote_addr zone=auth:10m   rate=5r/m;
limit_req_zone $binary_remote_addr zone=upload:10m rate=10r/m;
limit_req_zone $binary_remote_addr zone=api:10m    rate=30r/s;
```

```nginx
# nginx/nginx.conf (location 블록 내 — 존 적용)
location = /api/auth/login    { limit_req zone=auth   burst=3  nodelay; ... }
location ~* /api/.+/files$    { limit_req zone=upload burst=5  nodelay; ... }
location /api/                { limit_req zone=api    burst=50 nodelay; ... }
```

| 구간 | 제한 | Burst | 초과 시 |
|------|------|-------|---------|
| 로그인·회원가입 | 5회/분 | 3 | `429 Too Many Requests` |
| 파일 업로드 | 10회/분 | 5 | `429 Too Many Requests` |
| 일반 API | 30회/초 | 50 | `429 Too Many Requests` |

> **조정 방법**: 운영 환경 트래픽에 따라 `rate`, `burst` 값을 변경하세요.  
> nginx를 재시작(`docker compose restart frontend`)하면 즉시 적용됩니다.

### 1-5. 민감 경로 차단

```nginx
# .env, .git, .htaccess 등 숨김 파일 차단
location ~* /\.(env|git|svn|htaccess|htpasswd) {
    deny all;
    return 404;
}

# /actuator/** 전체 차단 (/actuator/health 제외)
location /api/actuator/ {
    deny all;
    return 403;
}
location = /api/actuator/health {
    proxy_pass http://backend:8080/api/actuator/health;
    access_log off;
}
```

### 1-6. 프록시 타임아웃

```nginx
proxy_connect_timeout 10s;   # 백엔드 연결 대기
proxy_read_timeout    30s;   # 응답 대기 (파일 업로드: 60s)
proxy_send_timeout    10s;   # 요청 전송 대기
```

---

## 2. Spring Boot 보안 설정

관련 파일: `backend/src/main/java/.../config/SecurityConfig.java`, `application.yml`

### 2-1. HTTP 보안 헤더 (Spring Security)

```java
// SecurityConfig.java
http.headers(headers -> headers
    .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
    .httpStrictTransportSecurity(hsts -> hsts
        .includeSubDomains(true)
        .maxAgeInSeconds(31536000)
    )
    .referrerPolicy(r -> r.policy(
        ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN
    ))
    .permissionsPolicy(p -> p.policy(
        "camera=(), microphone=(), geolocation=(), payment=()"
    ))
)
```

nginx와 Spring Security 양쪽에서 헤더를 설정하므로 nginx가 다운되어도 보안 헤더가 유지됩니다.

### 2-2. CORS 설정

```java
config.setAllowedOrigins(Arrays.asList(allowedOrigins));          // 환경변수로 관리
config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
config.setAllowedHeaders(List.of(
    "Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"
));
config.setExposedHeaders(List.of("Content-Disposition"));
config.setAllowCredentials(true);
config.setMaxAge(3600L);                                           // preflight 캐싱
```

허용 출처는 `.env` 파일 또는 환경변수로 설정합니다.

```env
CORS_ALLOWED_ORIGINS=https://your-domain.com
```

> **주의**: `allowedHeaders`에 와일드카드(`*`)를 사용하지 마세요.  
> 불필요한 헤더를 통한 정보 유출이나 CORS 우회 가능성이 있습니다.

### 2-3. Actuator 접근 제한

```java
// SecurityConfig.java
.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
.requestMatchers("/actuator/**").denyAll()
```

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health        # health 엔드포인트만 노출
  endpoint:
    health:
      show-details: never      # 상세 정보(DB 연결 상태 등) 숨김
  server:
    port: -1                   # 별도 관리 포트 비활성화
```

> `/actuator/env`, `/actuator/beans` 등은 내부 정보를 노출하므로 운영 환경에서 반드시 비활성화합니다.

### 2-4. 에러 응답 마스킹

```yaml
# application.yml
server:
  error:
    include-stacktrace: never          # 스택트레이스 노출 차단
    include-message: never             # 예외 메시지 노출 차단
    include-binding-errors: never      # 유효성 검증 에러 상세 노출 차단
```

운영 환경에서 스택트레이스가 노출되면 내부 패키지 구조·라이브러리 버전 등이 외부에 알려집니다.

### 2-5. JWT 시크릿 키 관리

```yaml
# application.yml (기본값 — 반드시 변경)
jwt:
  secret: ${JWT_SECRET:your-256-bit-secret-key-change-in-production-minimum-32-chars}
```

```env
# .env (운영 환경)
JWT_SECRET=<32자 이상의 무작위 문자열>
```

시크릿 키 생성 예시:

```bash
openssl rand -hex 32
```

---

## 3. Docker Compose 보안 설정

관련 파일: `docker-compose.yml`

### 3-1. 포트 노출 최소화

```yaml
# DB: 로컬호스트에만 바인딩 (운영 시 이 줄 자체를 제거 권장)
db:
  ports:
    - "127.0.0.1:3306:3306"

# Backend: 외부 포트 노출 없음 — nginx가 80포트로 프록시
backend:
  # ports 섹션 없음
  # 로컬 디버깅 필요 시: "127.0.0.1:8080:8080" 임시 추가

# Frontend(nginx): 80포트만 외부 노출
frontend:
  ports:
    - "80:80"
```

| 서비스 | 외부 노출 포트 | 이유 |
|--------|--------------|------|
| db (MySQL) | 없음 (로컬 디버깅 시 127.0.0.1:3306만) | 인터넷에서 직접 DB 접근 차단 |
| backend (Spring Boot) | 없음 | nginx 경유 접근만 허용 |
| frontend (nginx) | 80 (HTTP) | 사용자 접근 포트 |

### 3-2. 환경변수 보안

민감한 값은 `.env` 파일로 관리하고, `.gitignore`에 포함시켜 버전관리에서 제외합니다.

```env
# .env 예시 (git에 포함하지 마세요)
DB_ROOT_PASSWORD=<강력한 비밀번호>
DB_USER=secportal
DB_PASSWORD=<강력한 비밀번호>
JWT_SECRET=<32자 이상 무작위 문자열>
JASYPT_ENCRYPTOR_PASSWORD=<암호화 마스터 키>
CORS_ALLOWED_ORIGINS=https://your-domain.com
```

```bash
# 시크릿 키 생성
openssl rand -hex 32
```

### 3-3. 컨테이너 재시작 정책

```yaml
restart: unless-stopped
```

컨테이너가 예외로 종료되면 자동 재시작됩니다. 단, `docker compose stop`으로 명시적으로 중단한 경우에는 재시작하지 않습니다.

---

## 4. HTTPS 적용 방법

### 4-1. Let's Encrypt + Certbot (권장)

```bash
# Certbot 설치 (Ubuntu/Debian)
sudo apt install certbot

# 인증서 발급 (standalone 모드 — nginx 중단 후)
sudo certbot certonly --standalone -d your-domain.com

# 인증서 경로
# /etc/letsencrypt/live/your-domain.com/fullchain.pem
# /etc/letsencrypt/live/your-domain.com/privkey.pem
```

### 4-2. nginx HTTPS 설정

`nginx/nginx.conf`에 443 블록을 추가합니다.

```nginx
# HTTP → HTTPS 리다이렉트
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$host$request_uri;
}

# HTTPS 서버
server {
    listen 443 ssl;
    server_name your-domain.com;

    ssl_certificate     /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;

    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache   shared:SSL:10m;
    ssl_session_timeout 10m;

    # HSTS 활성화 (nginx.conf의 주석 해제)
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;

    # 기존 설정 유지 ...
}
```

### 4-3. docker-compose.yml에 443 포트 추가

```yaml
frontend:
  ports:
    - "80:80"
    - "443:443"
  volumes:
    - /etc/letsencrypt:/etc/letsencrypt:ro   # 인증서 마운트
```

---

## 5. 보안 설정 확인 방법

### 5-1. 응답 헤더 확인

```bash
curl -sI http://localhost

# 확인 항목
# Server 헤더 → 없어야 함
# X-Frame-Options → SAMEORIGIN
# X-Content-Type-Options → nosniff
# Content-Security-Policy → 설정된 정책 표시
```

### 5-2. Rate Limiting 확인

```bash
# 로그인 rate limit 테스트 (6번째 요청에서 429 응답)
for i in $(seq 1 8); do
  echo -n "요청 $i: "
  curl -s -o /dev/null -w "%{http_code}\n" \
    -X POST http://localhost/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"test@test.com","password":"wrong"}'
done
```

### 5-3. 차단된 경로 확인

```bash
# actuator 차단 확인 (403 응답)
curl -sI http://localhost/api/actuator/beans | grep HTTP

# health는 허용 (200 응답)
curl -sI http://localhost/api/actuator/health | grep HTTP

# 숨김 파일 차단 확인 (404 응답)
curl -sI http://localhost/.env | grep HTTP
```

### 5-4. 외부 보안 스캐너

- [Mozilla Observatory](https://observatory.mozilla.org) — 보안 헤더 종합 등급
- [Security Headers](https://securityheaders.com) — 헤더별 상세 분석
- [SSL Labs](https://www.ssllabs.com/ssltest/) — HTTPS/TLS 설정 등급 (HTTPS 적용 후)
