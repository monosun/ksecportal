# 아키텍처 문서

## 전체 구성

```
┌─────────────────────────────────────────────────────────┐
│                     Docker Compose                       │
│                                                         │
│  ┌──────────┐    ┌──────────────┐    ┌───────────────┐ │
│  │  Nginx   │───▶│  Spring Boot │───▶│   MySQL 8     │ │
│  │  :80     │    │  :8080       │    │   :3306       │ │
│  │  (Vue 3) │    │  (REST API)  │    │               │ │
│  └──────────┘    └──────────────┘    └───────────────┘ │
└─────────────────────────────────────────────────────────┘
```

- **Nginx**: Vue 3 SPA 서빙 + `/api/**` 요청을 백엔드로 프록시
- **Spring Boot**: REST API 서버, JWT 인증, 스케줄러
- **MySQL**: 영속 데이터, Docker named volume으로 데이터 유지

---

## 백엔드 패키지 구조

패키지 루트: `com.monosun.secportal`

```
secportal/
├── SecPortalApplication.java     @SpringBootApplication
│                                 @EnableJpaAuditing
│                                 @EnableScheduling
│
├── common/
│   ├── config/SecurityConfig     Spring Security 6, CORS, STATELESS
│   ├── entity/BaseEntity         @CreatedDate, @LastModifiedDate (JPA Auditing)
│   ├── response/ApiResponse      공통 응답 래퍼 (ok/created/noContent/error)
│   ├── exception/                GlobalExceptionHandler, BusinessException, ResourceNotFoundException
│   └── security/
│       ├── JwtTokenProvider      jjwt 0.12.6, HS256
│       └── JwtAuthenticationFilter  OncePerRequestFilter
│
├── auth/                         사용자 인증
│   ├── entity/User               UserDetails 구현, Role(ADMIN/MANAGER/USER)
│   ├── repository/UserRepository
│   ├── dto/AuthDto               LoginRequest, RegisterRequest, TokenResponse, UserInfo
│   ├── service/AuthService       login(AuthenticationManager), register(BCrypt)
│   ├── service/UserDetailsServiceImpl
│   └── controller/AuthController  /auth/**
│
├── policy/                       보안 정책
│   ├── entity/Policy             Category, Status enum
│   ├── entity/PolicyAcknowledgment  unique(policy_id, user_id)
│   ├── repository/
│   ├── dto/PolicyDto
│   ├── service/PolicyService     감사 로그 연동
│   └── controller/PolicyController
│
├── vulnerability/                취약점 관리
│   ├── entity/Vulnerability      Severity, Status enum
│   ├── entity/VulnerabilityComment
│   ├── repository/
│   ├── dto/VulnerabilityDto
│   ├── service/VulnerabilityService
│   └── controller/VulnerabilityController
│
├── incident/                     보안 인시던트
│   ├── entity/Incident           Severity, Status, IncidentType enum
│   ├── repository/IncidentRepository
│   ├── dto/IncidentDto
│   ├── service/IncidentService
│   └── controller/IncidentController
│
├── asset/                        IT 자산 관리
│   ├── entity/Asset              AssetType, Criticality enum, sbomSoftware N:1 맵핑
│   ├── repository/AssetRepository
│   ├── dto/AssetDto
│   ├── service/AssetService, AssetBulkService
│   └── controller/AssetController
│
├── sbom/                         SBOM 관리 (SW·라이브러리 구성)
│   ├── entity/SbomSoftware       name+version UNIQUE
│   ├── entity/SbomComponent      SW 1:N 라이브러리
│   ├── repository/
│   ├── dto/SbomDto, SbomBulkUploadResult
│   ├── service/SbomService, SbomBulkService  엑셀 템플릿·일괄등록
│   └── controller/SbomController  /sbom/software, /sbom/components, /sbom/bulk
│
├── training/                     IT 및 정보보호 교육
│   ├── entity/TrainingCourse     ContentType enum
│   ├── entity/QuizQuestion       난이도·해설(explanation) 포함
│   ├── entity/TrainingCompletion  unique(course_id, user_id)
│   ├── entity/QuizBankQuestion   문제은행 (코스 독립, quiz_bank_questions)
│   ├── repository/
│   ├── dto/TrainingDto, QuizBankDto
│   ├── service/TrainingService, QuizBankService
│   ├── service/QuizBankDataInitializer  기본 샘플 문항 시드
│   └── controller/TrainingController, QuizBankController
│
├── audit/                        감사 로그
│   ├── entity/AuditLog           REQUIRES_NEW 트랜잭션, SecurityContext에서 사용자 추출
│   ├── repository/AuditLogRepository
│   ├── dto/AuditLogDto
│   ├── service/AuditLogService
│   └── controller/AuditLogController  /admin/audit-logs (ADMIN)
│
├── admin/                        사용자 관리
│   ├── dto/UserAdminDto
│   ├── service/UserAdminService
│   └── controller/UserAdminController  /admin/users (ADMIN)
│
├── metrics/                      KPI 집계
│   ├── dto/MetricsDto
│   ├── service/MetricsService    다중 리포지토리 집계
│   └── controller/MetricsController  /metrics/summary
│
├── notification/                 이메일 알림
│   ├── service/EmailService      JavaMailSender, @Async
│   └── service/NotificationScheduler  @Scheduled (기한 초과 취약점)
│
└── report/                       PDF / CSV 리포트
    ├── service/ReportService     OpenPDF, CSV 빌더
    └── controller/ReportController  /reports/** (MANAGER+)
```

---

## 인증 흐름

### 이메일/비밀번호 인증

```
Client                    Nginx              Spring Boot
  │                         │                    │
  ├─ POST /api/auth/login ─▶│──────────────────▶│
  │                         │               (AuthenticationManager)
  │                         │               (BCrypt 검증)
  │◀── { accessToken } ────│◀──────────────────│
  │                         │                    │
  ├─ GET /api/... ─────────▶│──────────────────▶│
  │  Authorization: Bearer  │          (JwtAuthenticationFilter)
  │                         │          (SecurityContextHolder 설정)
  │◀── Response ───────────│◀──────────────────│
```

1. 클라이언트가 `POST /api/auth/login`으로 이메일/비밀번호 전송
2. `AuthService`가 `AuthenticationManager`로 인증, BCrypt 검증
3. 성공 시 `JwtTokenProvider.generateToken()`으로 JWT 발급
4. 이후 요청마다 `JwtAuthenticationFilter`가 `Authorization` 헤더에서 토큰 추출 및 검증
5. `SecurityContextHolder`에 `UsernamePasswordAuthenticationToken` 저장
6. `@PreAuthorize` 어노테이션으로 Role 기반 접근 제어

### Okta SSO 인증 (v1.54.0+, PKCE 방식)

```
Browser          SecPortal SPA          Okta               Spring Boot
  │                   │                   │                    │
  ├─ 로그인 클릭 ──▶ │                   │                    │
  │                   │ (state, code_verifier 생성)            │
  │                   ├─ redirect ───────▶│                    │
  │                   │  (client_id, code_challenge)           │
  │◀── Okta 로그인 ─ │                   │                    │
  ├─ ID/PW 입력 ─────────────────────▶  │                    │
  │                   │◀── code ─────────│                    │
  │                   │  (OktaCallbackView: state 검증)        │
  │                   ├─ POST /api/auth/okta/token ──────────▶│
  │                   │  (code + code_verifier)                │
  │                   │                   │ (OktaAuthService) │
  │                   │                   │ ← POST /token ────│
  │                   │                   │ → id_token ───────│
  │                   │                   │   (JWK 검증)       │
  │                   │                   │   (users 매핑)     │
  │                   │◀── { accessToken } ─────────────────  │
  │◀── 대시보드 ─────│                   │                    │
```

- `code_verifier`/`code_challenge`는 브라우저 세션스토리지에 임시 저장
- `state` 파라미터로 CSRF 방지
- 기존 계정: `users.okta_id`에 Okta Subject 매핑 (최초 로그인 시 이메일로 자동 연결)
- 신규 계정: `USER` 역할로 자동 생성
- `JwtDecoder` 캐시: issuer 변경 감지 시 자동 갱신

---

## 감사 로그 설계

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void log(String action, String resourceType, Long resourceId, String detail) {
    // 호출한 트랜잭션과 독립 실행
    // 실패해도 비즈니스 로직에 영향 없음
}
```

- **전파 속성**: `REQUIRES_NEW` — 기존 트랜잭션과 완전히 독립
- **사용자 추출**: `SecurityContextHolder`에서 현재 인증 사용자 조회
- **예외 처리**: `try/catch`로 감싸서 로그 실패가 메인 플로에 영향 없음
- **시스템 액션**: 사용자 없이 호출 시 `userName = "System"` 기록

---

## 이메일 알림 스케줄러

```
application.yml: notification.overdue-check-cron: "0 0 9 * * MON-FRI"
                                                   └ 평일 오전 9시 실행

NotificationScheduler.notifyOverdueVulnerabilities()
  → VulnerabilityRepository.findOverdue(today)   (OPEN/IN_PROGRESS & dueDate < today)
  → 담당자(assignee) 이메일로 HTML 알림 발송
  → EmailService.send() @Async 비동기 처리
```

`MAIL_PASSWORD`가 비어 있으면 JavaMail이 연결 실패하지만 `EmailService`의 `try/catch`가 잡아서 스케줄러는 정상 동작합니다.

---

## 데이터베이스 스키마

`ddl-auto: validate` 설정으로 애플리케이션 기동 시 엔티티와 실제 DB 스키마를 비교합니다.  
스키마 변경은 반드시 `db/init/*.sql` 파일을 먼저 수정해야 합니다.

### ERD (주요 관계)

```
users
  │
  ├─1:N── policies (author_id)
  │         └─1:N── policy_acknowledgments (user_id)
  │
  ├─1:N── vulnerabilities (reporter_id, assignee_id)
  │         └─1:N── vulnerability_comments (user_id)
  │
  ├─1:N── incidents (reporter_id, assignee_id)
  │
  ├─1:N── training_completions (user_id)
  │
  └─1:N── audit_logs (user_id)

training_courses
  └─1:N── quiz_questions
  └─1:N── training_completions (course_id)

quiz_bank_questions  (독립 테이블 — 문제은행, JPA 자동 생성)

sbom_software
  └─1:N── sbom_components (software_id, ON DELETE CASCADE)

assets
  └─N:1── sbom_software (sbom_software_id, nullable — SW 자산 SBOM 맵핑)
```

### 초기화 파일 실행 순서

| 파일 | 내용 |
|------|------|
| `01_schema.sql` | 전체 DDL (users, policies, vulnerabilities, training, audit_logs) |
| `02_seed.sql` | 기본 관리자 + 샘플 정책·취약점·교육 데이터 |
| `03_comments.sql` | `vulnerability_comments` 테이블 |
| `04_assets.sql` | `assets` 테이블 + 샘플 5건 |
| `05_incidents.sql` | `incidents` 테이블 + 샘플 3건 |
| `06_isms.sql` | ISMS-P 인증항목·증적 테이블 |
| `07_extended_schema.sql` | 확장 스키마 (RBAC·위원회·내부감사 등) |
| `08_extended_seed.sql` | 확장 기능 기본 데이터 |
| `09_threat_seed.sql` | 위협 카탈로그 기본 560개 |
| `10_sbom.sql` | `sbom_software`·`sbom_components` 테이블 + `assets.sbom_software_id` 컬럼 |

---

## 프론트엔드 구조

### 상태 관리 (Pinia)

```javascript
// stores/auth.js
{
  user: ref(null),
  token: ref(localStorage.getItem('token')),
  isAuthenticated: computed(() => !!token.value),
  isAdmin: computed(() => user.value?.role === 'ADMIN'),
  isManager: computed(() => ['ADMIN', 'MANAGER'].includes(user.value?.role))
}
```

### API 클라이언트 (`api/index.js`)

- `axios` 인스턴스 (`baseURL: '/api'`, `timeout: 10s`)
- Request 인터셉터: `localStorage`에서 토큰 읽어 헤더 주입
- Response 인터셉터: 401 시 토큰 제거 후 `/login`으로 리다이렉트
- 내보내는 API 객체: `authApi`, `policyApi`, `vulnApi`, `vulnCommentApi`, `trainingApi`, `incidentApi`, `assetApi`, `adminApi`, `metricsApi`, `reportApi`, `exportApi`

### 라우터 가드

```javascript
router.beforeEach(async (to) => {
  if (!to.meta.public && !auth.isAuthenticated) → /login
  if (to.meta.public && auth.isAuthenticated)   → /dashboard
  if (auth.isAuthenticated && !auth.user)       → fetchMe()
  if (to.meta.adminOnly && !auth.isAdmin)       → /dashboard
})
```

### i18n

`localStorage`의 `locale` 키로 언어 영속 저장.  
지원 언어: `ko` (기본), `en`

---

## 보안 고려사항

| 항목 | 구현 방식 |
|------|----------|
| 인증 | JWT (HS256), Stateless 세션 / Okta SSO (PKCE, v1.54.0+) |
| 비밀번호 | BCrypt 해싱 |
| 권한 검사 | Spring Security `@PreAuthorize` + `@EnableMethodSecurity` |
| CORS | `SecurityConfig`에서 허용 Origin 설정 |
| SQL Injection | Spring Data JPA / JPQL 파라미터 바인딩 |
| XSS | Vue 3 기본 이스케이프 |
| 감사 추적 | 모든 생성·수정·삭제 액션 `audit_logs` 기록 |
