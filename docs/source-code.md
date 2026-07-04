# 소스코드 상세 설명

SecPortal 소스코드를 처음 보는 개발자를 위한 도메인별 코드 해설입니다.  
실제 파일 경로와 클래스명을 기준으로 설명합니다.

## 목차

1. [진입점 및 공통 인프라](#1-진입점-및-공통-인프라)
2. [인증 도메인 (auth)](#2-인증-도메인-auth)
3. [보안 정책 도메인 (policy)](#3-보안-정책-도메인-policy)
4. [취약점 도메인 (vulnerability)](#4-취약점-도메인-vulnerability)
5. [인시던트 도메인 (incident)](#5-인시던트-도메인-incident)
6. [자산 도메인 (asset)](#6-자산-도메인-asset)
7. [교육 도메인 (training)](#7-교육-도메인-training)
8. [감사 로그 (audit)](#8-감사-로그-audit)
9. [관리자 기능 (admin)](#9-관리자-기능-admin)
10. [메트릭·대시보드 (metrics)](#10-메트릭대시보드-metrics)
11. [알림 시스템 (notification)](#11-알림-시스템-notification)
12. [보고서 (report)](#12-보고서-report)
13. [프론트엔드 코드](#13-프론트엔드-코드)
    - [13-0. UI 테마 시스템](#13-0-ui-테마-시스템-개요)
    - [uiSettings 스토어](#frontendsrcstoresuisettingsjs-ui-설정-스토어)
    - [환경설정 페이지](#frontendsrcviewssettingsuisettingsviewvue-환경설정-페이지)
14. [데이터베이스 초기화 SQL](#14-데이터베이스-초기화-sql)

---

## 1. 진입점 및 공통 인프라

### SecPortalApplication.java

```
backend/src/main/java/com/monosun/secportal/SecPortalApplication.java
```

```java
@SpringBootApplication
@EnableJpaAuditing      // BaseEntity의 createdAt/updatedAt 자동 기록 활성화
@EnableScheduling       // NotificationScheduler의 @Scheduled 활성화
public class SecPortalApplication {
    public static void main(String[] args) {
        SpringApplication.run(SecPortalApplication.class, args);
    }
}
```

두 어노테이션이 핵심입니다.
- `@EnableJpaAuditing` 없으면 모든 엔티티의 생성/수정 시각이 null이 됩니다.
- `@EnableScheduling` 없으면 취약점 기한 초과 알림 스케줄러가 동작하지 않습니다.

---

### common/entity/BaseEntity.java

```
backend/src/main/java/com/monosun/secportal/common/entity/BaseEntity.java
```

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

`User`, `Policy`, `Vulnerability`, `Incident`, `TrainingCourse` 등 대부분의 엔티티가 이 클래스를 상속합니다.  
`Asset`은 예외로, `@PreUpdate`를 직접 사용합니다.

---

### common/response/ApiResponse.java

모든 REST 응답은 이 래퍼로 감쌉니다.

```json
// 성공
{ "success": true, "data": { ... }, "message": null }

// 실패
{ "success": false, "data": null, "message": "오류 메시지" }
```

컨트롤러에서 사용 예:
```java
return ApiResponse.ok(service.get(id));       // 200
return ApiResponse.created(service.create()); // 201
return ApiResponse.noContent();               // 204
```

---

### common/exception/GlobalExceptionHandler.java

`@RestControllerAdvice`로 전역 예외를 처리합니다.

| 예외 | HTTP 코드 |
|------|----------|
| `ResourceNotFoundException` | 404 |
| `BusinessException` | 400 |
| `MethodArgumentNotValidException` | 400 (Bean Validation 실패) |
| `AccessDeniedException` | 403 |
| 그 외 | 500 |

---

### common/config/SecurityConfig.java

Spring Security 핵심 설정:

```java
.csrf(AbstractHttpConfigurer::disable)           // JWT 기반이므로 CSRF 불필요
.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))  // 세션 미사용
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/auth/**").permitAll()      // 로그인/회원가입은 공개
    .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
    .anyRequest().authenticated()                // 그 외 인증 필수
)
.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
```

CORS 설정 — 개발 환경에서는 모든 Origin을 허용합니다(`allowedOriginPatterns: "*"`).  
프로덕션에서는 실제 도메인으로 제한하는 것을 권장합니다.

---

### common/security/JwtTokenProvider.java

jjwt 0.12.6을 사용하는 JWT 유틸리티:

```java
// 토큰 생성 (로그인 시 호출)
public String generateToken(Authentication authentication) {
    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    return Jwts.builder()
            .subject(userDetails.getUsername())  // 이메일이 subject
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(key)    // HMAC-SHA256
            .compact();
}

// 토큰에서 이메일 추출
public String getUsernameFromToken(String token) { ... }

// 서명 검증 + 만료 확인
public boolean validateToken(String token) { ... }
```

`jwt.secret`은 UTF-8 바이트 배열로 변환하여 HMAC 키를 생성합니다.  
키 길이가 32바이트(256비트) 미만이면 jjwt가 예외를 던지므로, `.env`의 `JWT_SECRET`은 반드시 32자 이상이어야 합니다.

---

### common/security/JwtAuthenticationFilter.java

모든 요청에 앞서 실행되는 `OncePerRequestFilter`:

```
요청 도착
  → Authorization 헤더에서 Bearer 토큰 추출
  → JwtTokenProvider.validateToken() 검증
  → UserDetailsService.loadUserByUsername() 사용자 로드
  → SecurityContextHolder에 인증 객체 저장
  → 다음 필터로 전달
```

토큰이 없거나 유효하지 않으면 SecurityContext에 아무것도 설정하지 않습니다.  
이후 Spring Security가 인증 없는 요청을 401로 차단합니다.

---

## 2. 인증 도메인 (auth)

### auth/entity/User.java

```java
@Entity @Table(name = "users")
public class User extends BaseEntity implements UserDetails {
    private String email;     // 로그인 ID이자 username
    private String password;  // BCrypt 해시
    private String name;
    private Role role;        // ADMIN | MANAGER | USER
    private String department;
    private boolean active;

    @Override
    public String getUsername() { return email; }   // Spring Security 연동

    @Override
    public boolean isAccountNonLocked() { return active; }  // active=false면 로그인 차단
}
```

`UserDetails`를 직접 구현하므로 별도의 `UserDetails` 래퍼 클래스 없이 바로 Spring Security와 통합됩니다.

역할 체계:
- `ADMIN`: 전체 관리 (사용자 수정, 감사 로그 조회, 삭제 권한)
- `MANAGER`: 정책·취약점·자산·인시던트 생성·수정, 보고서 출력
- `USER`: 조회, 정책 열람 확인, 취약점 댓글

---

### auth/service/AuthService.java

```java
// 로그인
public AuthDto.TokenResponse login(AuthDto.LoginRequest request) {
    Authentication auth = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
    );
    String token = tokenProvider.generateToken(auth);
    User user = (User) auth.getPrincipal();
    return new AuthDto.TokenResponse(token, AuthDto.UserInfo.from(user));
}

// 회원가입
public AuthDto.UserInfo register(AuthDto.RegisterRequest request) {
    // 중복 이메일 체크
    if (userRepository.existsByEmail(request.getEmail())) {
        throw new BusinessException("이미 사용 중인 이메일입니다");
    }
    User user = User.builder()
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))  // BCrypt
            .name(request.getName())
            .role(User.Role.USER)        // 신규 가입자는 USER 역할
            .department(request.getDepartment())
            .build();
    return AuthDto.UserInfo.from(userRepository.save(user));
}
```

`AuthenticationManager`는 `SecurityConfig`에서 빈으로 등록됩니다.

---

## 3. 보안 정책 도메인 (policy)

### 주요 엔티티

**Policy** (`policy/entity/Policy.java`):
```
policies 테이블
├── title, content (LONGTEXT — 마크다운 내용)
├── category: GENERAL | ACCESS_CONTROL | DATA_PROTECTION | INCIDENT_RESPONSE | NETWORK | PHYSICAL | VENDOR | OTHER
├── status: DRAFT → REVIEW → PUBLISHED → ARCHIVED
├── version (기본값 "1.0")
├── effectiveDate
└── author_id → users
```

**PolicyAcknowledgment** (`policy/entity/PolicyAcknowledgment.java`):
```
policy_acknowledgments 테이블
├── policy_id → policies (unique 제약: policy_id + user_id)
└── user_id → users
```
한 사용자가 같은 정책을 두 번 확인하면 409 Conflict 응답.

---

### policy/service/PolicyService.java

정책 수신 확인 로직:
```java
public void acknowledge(Long policyId, Long userId) {
    Policy policy = findById(policyId);
    if (policy.getStatus() != Policy.Status.PUBLISHED) {
        throw new BusinessException("공개된 정책만 확인할 수 있습니다");
    }
    if (ackRepository.existsByPolicyIdAndUserId(policyId, userId)) {
        throw new BusinessException("이미 확인한 정책입니다");
    }
    ackRepository.save(PolicyAcknowledgment.builder()
            .policy(policy).user(userRepository.getReferenceById(userId)).build());
    auditLogService.log("POLICY_ACKNOWLEDGED", "POLICY", policyId, policy.getTitle());
}
```

---

## 4. 취약점 도메인 (vulnerability)

### 주요 엔티티

**Vulnerability** (`vulnerability/entity/Vulnerability.java`):
```
vulnerabilities 테이블
├── title, description (TEXT)
├── cveId, cvssScore (DECIMAL 3,1 — 예: 10.0)
├── severity: CRITICAL | HIGH | MEDIUM | LOW | INFO
├── status: OPEN | IN_PROGRESS | RESOLVED | ACCEPTED | FALSE_POSITIVE
├── assetName (자유 텍스트 — Asset 엔티티와 FK 연결 없음)
├── assignee_id → users (담당자)
├── reporter_id → users (등록자)
├── dueDate (처리 기한)
└── resolvedAt (RESOLVED 상태 전환 시 자동 기록)
```

**VulnerabilityComment** (`vulnerability/entity/VulnerabilityComment.java`):
```
vulnerability_comments 테이블
├── vulnerability_id → vulnerabilities
├── user_id → users
└── content (TEXT)
```

---

### vulnerability/service/VulnerabilityService.java

상태 전환 시 자동 처리:
```java
if (request.getStatus() == Vulnerability.Status.RESOLVED) {
    vuln.setResolvedAt(LocalDateTime.now());  // 해결 시각 자동 기록
}
auditLogService.log("VULN_STATUS_CHANGED", "VULNERABILITY", id, detail);
```

통계 집계:
```java
public Map<String, Long> getStats() {
    Map<String, Long> stats = new HashMap<>();
    for (Vulnerability.Status s : Vulnerability.Status.values()) {
        stats.put("status_" + s.name(), vulnRepository.countByStatus(s));
    }
    for (Vulnerability.Severity s : Vulnerability.Severity.values()) {
        stats.put("severity_" + s.name(), vulnRepository.countBySeverity(s));
    }
    return stats;
}
```

---

### vulnerability/repository/VulnerabilityRepository.java

핵심 커스텀 쿼리:

```java
// 검색 (status/severity/keyword 동적 필터)
@Query("SELECT v FROM Vulnerability v WHERE " +
       "(:status IS NULL OR v.status = :status) AND " +
       "(:severity IS NULL OR v.severity = :severity) AND " +
       "(:keyword IS NULL OR v.title LIKE %:keyword% OR v.cveId LIKE %:keyword%)")
Page<Vulnerability> search(...);

// 기한 초과 취약점 (OPEN 또는 IN_PROGRESS이며 dueDate가 오늘 이전)
@Query("SELECT v FROM Vulnerability v WHERE v.status IN ('OPEN','IN_PROGRESS') AND v.dueDate < :today")
List<Vulnerability> findOverdue(LocalDate today);
```

---

## 5. 인시던트 도메인 (incident)

### 주요 엔티티

**Incident** (`incident/entity/Incident.java`):
```
incidents 테이블
├── title, description (TEXT)
├── severity: CRITICAL | HIGH | MEDIUM | LOW
├── status: OPEN | INVESTIGATING | CONTAINED | RESOLVED | CLOSED
├── type: MALWARE | PHISHING | DATA_BREACH | UNAUTHORIZED_ACCESS | DDOS | INSIDER_THREAT | PHYSICAL | OTHER
├── affectedSystems (TEXT — 자유 형식)
├── reporter_id, assignee_id → users
├── detectedAt (인시던트 발생 시각)
└── resolvedAt (RESOLVED/CLOSED 전환 시 자동 기록)
```

상태 전환 흐름:
```
OPEN → INVESTIGATING → CONTAINED → RESOLVED → CLOSED
```

---

## 6. 자산 도메인 (asset)

### 주요 엔티티

**Asset** (`asset/entity/Asset.java`):

`BaseEntity`를 상속하지 않고 직접 `createdAt`/`updatedAt`을 관리합니다:
```java
@PreUpdate
void onUpdate() { this.updatedAt = LocalDateTime.now(); }
```

```
assets 테이블
├── name, type: SERVER | WORKSTATION | NETWORK | APPLICATION | DATABASE | CLOUD | OTHER
├── ipAddress, owner, department, description
├── criticality: HIGH | MEDIUM | LOW
└── active (운영 중 여부)
```

`MetricsService`에서 `assetRepository.countByActive(true)`와 `countByCriticality(HIGH)`로 대시보드 지표를 계산합니다.

---

## 7. 교육 도메인 (training)

### 주요 엔티티

**TrainingCourse** (`training/entity/TrainingCourse.java`):
```
training_courses 테이블
├── title, description
├── contentType: VIDEO | DOCUMENT | QUIZ_ONLY
├── contentUrl (비디오 URL 또는 문서 링크)
├── passingScore (기본 70점)
├── mandatory (필수 교육 여부)
└── questions → quiz_questions (1:N)
```

**QuizQuestion** (`training/entity/QuizQuestion.java`):
```
quiz_questions 테이블
├── course_id → training_courses
├── question, optionA/B/C/D
├── correctAnswer (A/B/C/D)
└── sortOrder (출제 순서)
```

**TrainingCompletion** (`training/entity/TrainingCompletion.java`):
```
training_completions 테이블
├── course_id, user_id (unique 제약)
├── score (0~100)
└── passed (passingScore 이상이면 true)
```

---

### training/service/TrainingService.java

퀴즈 채점 로직:
```java
public TrainingDto.SubmitResult submitQuiz(Long courseId, Map<Long, String> answers, Long userId) {
    TrainingCourse course = findById(courseId);
    List<QuizQuestion> questions = course.getQuestions();

    long correct = questions.stream()
            .filter(q -> q.getCorrectAnswer().equals(answers.get(q.getId())))
            .count();

    int score = questions.isEmpty() ? 100 :
            (int) Math.round(correct * 100.0 / questions.size());
    boolean passed = score >= course.getPassingScore();

    // 이미 완료한 경우 점수 갱신
    TrainingCompletion completion = completionRepository
            .findByCourseIdAndUserId(courseId, userId)
            .orElse(TrainingCompletion.builder().course(course).user(...).build());
    completion.setScore(score);
    completion.setPassed(passed);
    completionRepository.save(completion);

    return new TrainingDto.SubmitResult(score, passed, (int) correct, questions.size());
}
```

---

## 8. 감사 로그 (audit)

### audit/entity/AuditLog.java

```
audit_logs 테이블
├── user_id → users (null 가능 — 시스템 액션)
├── action (예: VULN_CREATED, POLICY_UPDATED)
├── resourceType (예: VULNERABILITY, POLICY)
├── resourceId
├── detail (자유 텍스트)
├── ipAddress (요청 IP)
└── createdAt (불변 — @Column(updatable = false))
```

---

### audit/service/AuditLogService.java

핵심 설계 포인트:

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void log(String action, String resourceType, Long resourceId, String detail) {
    // ...
}
```

`REQUIRES_NEW` 전파 속성이 핵심입니다:
- 호출한 서비스의 트랜잭션과 완전히 독립된 새 트랜잭션에서 실행됩니다.
- 메인 비즈니스 로직이 롤백되더라도 감사 로그는 별도로 커밋됩니다.
- 감사 로그 기록이 실패해도 `try/catch`가 잡아서 메인 로직에 영향이 없습니다.

사용자 추출:
```java
User user = null;
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
if (auth != null && auth.getPrincipal() instanceof User u) {
    user = u;
}
// user가 null이면 "System" 액션으로 기록
```

현재 감사 로그가 기록되는 시점:

| 액션 | 기록 위치 |
|------|----------|
| VULN_CREATED | VulnerabilityService.create() |
| VULN_STATUS_CHANGED | VulnerabilityService.update() |
| VULN_DELETED | VulnerabilityService.delete() |
| POLICY_ACKNOWLEDGED | PolicyService.acknowledge() |
| *(그 외는 서비스마다 추가 필요)* | — |

---

## 9. 관리자 기능 (admin)

### admin/controller/UserAdminController.java

```java
@GetMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public ApiResponse<Page<UserAdminDto.Response>> list(...) { ... }

@PatchMapping("/admin/users/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ApiResponse<UserAdminDto.Response> update(...) { ... }
```

`@PreAuthorize("hasRole('ADMIN')")`이 메서드 레벨 권한 검사를 담당합니다.  
이 어노테이션은 `SecurityConfig`의 `@EnableMethodSecurity` 덕분에 동작합니다.

수정 가능 필드: `role`, `active`, `department`

---

### admin/controller/AuditLogController.java

```java
@GetMapping("/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public ApiResponse<Page<AuditLogDto.Response>> list(
    @RequestParam(required = false) String action,
    @RequestParam(required = false) String resourceType,
    @RequestParam(required = false) Long userId,
    Pageable pageable) { ... }
```

---

## 10. 메트릭·대시보드 (metrics)

### metrics/service/MetricsService.java

대시보드 `GET /api/metrics/summary` 응답을 만드는 핵심 서비스:

```java
public MetricsDto.Summary summary() {
    long totalUsers = userRepository.count();
    long totalAssets = assetRepository.countByActive(true);
    long highAssets = assetRepository.countByCriticality(Asset.Criticality.HIGH);
    long overdueVulns = vulnRepository.findOverdue(LocalDate.now()).size();
    long openVulns = vulnRepository.countByStatus(Vulnerability.Status.OPEN);
    long openIncidents = incidentRepository.countByStatus(Incident.Status.OPEN)
                       + incidentRepository.countByStatus(Incident.Status.INVESTIGATING);
    long criticalIncidents = incidentRepository.countBySeverity(Incident.Severity.CRITICAL);

    // 정책 열람 확인율 = 최소 1개 정책을 확인한 사용자 수 / 전체 사용자
    double policyAckRate = ...;

    // 교육 이수율 = 최소 1개 코스를 통과한 사용자 수 / 전체 사용자
    double trainingRate = ...;

    // 최근 6개월 취약점 등록 추이 (월별)
    List<MetricsDto.TrendPoint> trend = buildTrend();

    return MetricsDto.Summary.builder()...build();
}
```

월별 추이 계산:
```java
private List<MetricsDto.TrendPoint> buildTrend() {
    LocalDateTime since = LocalDateTime.now().minusMonths(5)...;
    List<Vulnerability> recent = vulnRepository.findCreatedAfter(since);

    Map<String, Long> byMonth = recent.stream()
            .collect(Collectors.groupingBy(
                v -> v.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                Collectors.counting()
            ));

    // 현재부터 5달 전까지 6개 포인트 생성 (데이터 없는 달은 0)
    for (int i = 5; i >= 0; i--) {
        String month = LocalDateTime.now().minusMonths(i).format(MONTH_FMT);
        trend.add(...byMonth.getOrDefault(month, 0L)...);
    }
}
```

---

## 11. 알림 시스템 (notification)

### notification/service/EmailService.java

```java
@Async  // 별도 스레드에서 비동기 실행 (HTTP 응답 차단 없음)
public void send(String to, String subject, String htmlBody) {
    try {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);  // true = HTML 형식
        mailSender.send(msg);
    } catch (Exception e) {
        log.warn("Failed to send email to {}: {}", to, e.getMessage());
        // 예외를 잡아서 스케줄러 실행이 중단되지 않도록 처리
    }
}
```

`MAIL_PASSWORD`가 없으면 SMTP 연결 실패가 여기서 잡혀서 로그로만 기록됩니다.  
개발 환경에서는 이메일 설정 없이도 시스템이 정상 동작합니다.

---

### notification/service/NotificationScheduler.java

```java
@Scheduled(cron = "${notification.overdue-check-cron}")
// 기본값: "0 0 9 * * MON-FRI" = 평일 오전 9시
public void notifyOverdueVulnerabilities() {
    LocalDate today = LocalDate.now();
    List<Vulnerability> overdue = vulnerabilityRepository.findOverdue(today);

    for (Vulnerability v : overdue) {
        if (v.getAssignee() != null && v.getAssignee().getEmail() != null) {
            String subject = "[SecPortal] 취약점 처리 기한 초과: " + v.getTitle();
            emailService.send(v.getAssignee().getEmail(), subject, buildOverdueEmailHtml(v));
        }
    }
}
```

cron 표현식 변경: `.env`의 `NOTIFICATION_CRON` 환경변수로 조정합니다.

---

## 12. 보고서 (report)

### report/service/ReportService.java

PDF 생성은 OpenPDF(`com.github.librepdf:openpdf`)를 사용합니다.

```java
// 취약점 PDF 보고서
public byte[] generateVulnerabilityPdf() {
    Document document = new Document();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PdfWriter.getInstance(document, out);
    document.open();
    // 제목, 통계 표, 취약점 목록 표 추가
    document.close();
    return out.toByteArray();
}

// CSV 내보내기 (UTF-8 BOM 포함 — Excel 한글 깨짐 방지)
public byte[] generateVulnerabilityCsv() {
    StringBuilder sb = new StringBuilder();
    sb.append('﻿'); // BOM
    sb.append("ID,제목,심각도,상태,CVE ID,담당자,기한\n");
    for (Vulnerability v : vulnRepository.findAll()) {
        sb.append(...).append('\n');
    }
    return sb.toString().getBytes(StandardCharsets.UTF_8);
}
```

보고서 컨트롤러는 `@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")`로 MANAGER 이상만 접근 가능합니다.

---

## 13. 프론트엔드 코드

### 13-0. UI 테마 시스템 개요

미니멀한 핀테크 스타일의 디자인 시스템을 기반으로 하며, 사용자가 **환경설정 메뉴(`/settings`)** 에서 UI 설정을 직접 변경할 수 있습니다.

| 설정 항목 | 선택지 | 기본값 |
|---|---|---|
| 컬러 테마 | Blue / Navy / Emerald / Purple / Rose | Blue |
| 폰트 | Pretendard / Noto Sans KR / 시스템 기본 | Pretendard |
| 글자 크기 | 작게(13px) / 보통(14px) / 크게(16px) | 보통(14px) |
| 사이드바 스타일 | 어두운 / 밝은 | 어두운 |

모든 설정은 `localStorage`에 저장되어 새로고침 후에도 유지됩니다.

#### 동적 테마 구현 방식

CSS 변수(Custom Properties)와 Tailwind CSS를 연결하는 방식으로 런타임 테마 변경을 구현합니다.

**1단계 — `tailwind.config.js`에서 CSS 변수로 색상 정의**

```js
colors: {
  primary: {
    50:  'var(--color-primary-50)',
    100: 'var(--color-primary-100)',
    500: 'var(--color-primary-500)',
    // ...
  }
}
```

Tailwind가 빌드 시점에 `bg-primary-500 { background-color: var(--color-primary-500) }` 형태로 컴파일합니다.

**2단계 — `main.css`의 `:root`에 기본값 정의**

```css
:root {
  --color-primary-500: #0064FF;  /* Blue */
  --font-sans: 'Pretendard Variable', ...;
  --font-size-base: 14px;
}
html { font-size: var(--font-size-base); }
body { font-family: var(--font-sans); }
```

**3단계 — `uiSettings.js` 스토어에서 런타임 변수 교체**

```js
function applyTheme(key) {
  Object.entries(THEMES[key].colors).forEach(([shade, hex]) => {
    document.documentElement.style.setProperty(`--color-primary-${shade}`, hex)
  })
}
```

이 방식의 장점: Tailwind 클래스는 정적으로 컴파일되어 번들 크기가 늘어나지 않으면서도, CSS 변수 교체만으로 전체 색상이 즉시 반영됩니다.

> **주의 — opacity 수정자(modifier) 사용 불가**  
> `bg-primary-500/50` 같은 Tailwind opacity modifier는 Tailwind가 실제 색상 hex 값을 알아야 RGB 분리가 가능합니다. CSS 변수를 사용하면 빌드 타임에 hex 값을 알 수 없으므로 **opacity modifier가 동작하지 않습니다.**  
> 배경 반투명이 필요한 경우 별도 CSS 변수를 정의하거나 `opacity` 유틸리티를 사용하세요.

---

### frontend/src/stores/uiSettings.js (UI 설정 스토어)

```
frontend/src/stores/uiSettings.js
```

테마 설정을 관리하는 Pinia 스토어. 앱 초기화 시(`main.js`) `init()`을 호출하여 저장된 설정을 CSS 변수에 즉시 반영합니다.

```js
// main.js에서 Pinia 등록 직후 호출
import { useUiSettingsStore } from './stores/uiSettings'
useUiSettingsStore().init()
```

**내보내는 상수 (컴포넌트에서 직접 임포트 가능)**

| 상수 | 타입 | 내용 |
|---|---|---|
| `THEMES` | `Record<string, { label, swatch, colors }>` | 컬러 테마 정의 |
| `FONTS`  | `Record<string, { label, value }>` | 폰트 정의 |
| `FONT_SIZES` | `Record<string, { label, value }>` | 글자 크기 정의 |

**스토어 액션**

| 액션 | 동작 |
|---|---|
| `init()` | localStorage에서 설정 로드 후 CSS 변수 일괄 적용 |
| `setTheme(key)` | 컬러 CSS 변수 교체 + localStorage 저장 |
| `setFont(key)` | `--font-sans` 변수 교체 + localStorage 저장 |
| `setFontSize(key)` | `--font-size-base` 변수 교체 + localStorage 저장 |
| `setSidebarStyle(style)` | `'dark'` \| `'light'` 저장 (CSS 변수 없음, Vue 반응형으로 처리) |

---

### frontend/src/views/settings/UISettingsView.vue (환경설정 페이지)

경로: `/settings` — 사이드바 하단 ⚙️ 아이콘으로 접근.

각 설정 항목을 카드 UI로 구성하며, 변경 즉시 실시간으로 반영됩니다 (별도 저장 버튼 없음).

---

### frontend/src/stores/auth.js (Pinia 스토어)

```javascript
export const useAuthStore = defineStore('auth', () => {
    const user = ref(null)
    const token = ref(localStorage.getItem('token'))

    const isAuthenticated = computed(() => !!token.value)
    const isAdmin = computed(() => user.value?.role === 'ADMIN')
    const isManager = computed(() => ['ADMIN', 'MANAGER'].includes(user.value?.role))

    async function login(email, password) {
        const res = await authApi.login({ email, password })
        token.value = res.accessToken
        user.value = res.user
        localStorage.setItem('token', res.accessToken)
    }

    async function fetchMe() {
        user.value = await authApi.me()
    }

    function logout() {
        token.value = null
        user.value = null
        localStorage.removeItem('token')
    }

    return { user, token, isAuthenticated, isAdmin, isManager, login, fetchMe, logout }
})
```

---

### frontend/src/api/index.js (Axios 클라이언트)

```javascript
const api = axios.create({
    baseURL: '/api',   // Nginx 또는 Vite proxy가 :8080으로 전달
    timeout: 10000
})

// 요청마다 JWT 토큰 자동 첨부
api.interceptors.request.use(config => {
    const token = localStorage.getItem('token')
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
})

// 401이면 스토어 전체 초기화 후 로그인 페이지로 이동
api.interceptors.response.use(
    res => res.data,         // ApiResponse의 data 필드만 반환
    err => {
        if (err.response?.status === 401) {
            const auth = useAuthStore()
            auth.logout()    // token.value = null + localStorage 제거
            router.push('/login')
        }
        return Promise.reject(err.response?.data?.message || 'Request failed')
    }
)
```

`res => res.data` 처리로 인해 뷰 컴포넌트에서는 `response.data.content`가 아닌 `response.content`로 바로 접근합니다.

> **주의 — 401 처리 시 반드시 `auth.logout()` 호출**  
> `localStorage.removeItem('token')`만 하면 Pinia store의 `token` ref가 초기화되지 않아 `isAuthenticated`가 여전히 `true`로 남습니다.  
> 결과적으로 라우터 가드가 `/login`에서 Dashboard로 즉시 리다이렉트하고, Dashboard에서 다시 `fetchMe()`를 호출하면서 401 루프가 발생합니다.  
> `auth.logout()`은 `token.value = null`과 `localStorage` 제거를 함께 수행하므로 반드시 이 메서드를 사용해야 합니다.

---

### frontend/src/router/index.js (라우터 가드)

```javascript
router.beforeEach(async (to) => {
    const auth = useAuthStore()

    // 비인증 사용자가 보호된 경로 접근 시 로그인 페이지로
    if (!to.meta.public && !auth.isAuthenticated) {
        return { name: 'Login' }
    }

    // 이미 로그인된 사용자가 로그인/회원가입 접근 시 대시보드로
    if (to.meta.public && auth.isAuthenticated) {
        return { name: 'Dashboard' }
    }

    // 페이지 새로고침 시 토큰은 있지만 user 객체가 없는 경우 복원
    if (auth.isAuthenticated && !auth.user) {
        await auth.fetchMe()
    }

    // admin 전용 페이지 접근 권한 검사
    if (to.meta.adminOnly && !auth.isAdmin) {
        return { name: 'Dashboard' }
    }
})
```

---

### frontend/src/i18n/index.js

`ko.json`(기본)과 `en.json` 두 언어를 지원합니다.  
`localStorage`의 `locale` 키로 언어 선택이 영속됩니다.

```javascript
// 컴포넌트에서 사용
{{ $t('common.save') }}
{{ $t('vulnerability.severity.CRITICAL') }}
```

새 도메인 추가 시 `ko.json`과 `en.json` 양쪽에 키를 추가해야 합니다.

---

## 14. 데이터베이스 초기화 SQL

`db/init/` 디렉토리의 파일들은 MySQL 컨테이너 최초 시작 시 알파벳 순으로 실행됩니다.

> **배포 전 체크포인트 — `docker-compose.yml` DB 환경변수 확인**
>
> `application.yml`의 `SPRING_DATASOURCE_URL` 기본값이 `localhost:3306`이므로, Docker 환경에서 해당 환경변수가 누락되면 Spring Boot가 DB에 연결하지 못하고 기동 실패 → nginx 502 발생.
> `docker compose up --build` 실행 전 아래 환경변수가 `docker-compose.yml`에 존재하는지 반드시 확인:
>
> ```yaml
> SPRING_DATASOURCE_URL: jdbc:mysql://db:3306/secportal?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
> SPRING_DATASOURCE_USERNAME: ${DB_USER:-secportal}
> SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-secportal123}
> ```
>
> Docker 컨테이너 간 통신은 서비스명(`db`)으로 이루어지므로 `localhost`는 동작하지 않음.

### 01_schema.sql — 테이블 정의

전체 DDL이 포함됩니다. 주요 제약 조건:
- `users.email` — UNIQUE
- `policy_acknowledgments(policy_id, user_id)` — UNIQUE (한 사용자가 같은 정책 중복 확인 방지)
- `training_completions(course_id, user_id)` — UNIQUE (퀴즈 재도전 시 UPDATE 처리)
- `ddl-auto: validate` 설정이므로 엔티티와 스키마가 정확히 일치해야 함

### 02_seed.sql — 기본 데이터

초기 관리자 계정 (비밀번호: `Admin1234!`의 BCrypt 해시):
```sql
INSERT INTO users (email, password, name, role, department, active)
VALUES ('secportal@monosun.com', '$2a$10$...', 'System Admin', 'ADMIN', '보안팀', true);
```

샘플 정책, 취약점, 교육 코스 데이터도 포함됩니다.

### 03_comments.sql — 취약점 댓글 샘플
### 04_assets.sql — 자산 테이블 + 샘플 5건
### 05_incidents.sql — 인시던트 테이블 + 샘플 3건

---

## 코드 확장 가이드

### 새 도메인 추가 체크리스트

1. `db/init/0N_newdomain.sql` — 테이블 DDL 작성
2. `newdomain/entity/NewEntity.java` — `BaseEntity` 상속
3. `newdomain/repository/NewEntityRepository.java` — `JpaRepository` 확장
4. `newdomain/dto/NewEntityDto.java` — `CreateRequest`, `UpdateRequest`, `Response` 중첩 클래스
5. `newdomain/service/NewEntityService.java` — 비즈니스 로직 + `auditLogService.log()` 호출
6. `newdomain/controller/NewEntityController.java` — `@PreAuthorize` 권한 설정
7. `docs/api.md` — API 명세 추가
8. 프론트엔드: `src/api/index.js` + `src/views/newdomain/` + `src/router/index.js`

### 권한 설정 패턴

```java
// 조회: 인증된 모든 사용자 (SecurityConfig에서 처리)
@GetMapping("/{id}")
public ApiResponse<...> get(...) { ... }

// 생성·수정: MANAGER 이상
@PostMapping
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public ApiResponse<...> create(...) { ... }

// 삭제: ADMIN만
@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ApiResponse<Void> delete(...) { ... }
```
