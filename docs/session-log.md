# SecPortal 문서화 작업 세션 로그

---

## 세션 2026-06-09 (4) — 완전 삭제 승인 요청 DB 스키마 핫픽스 (v1.25.1)

작업일: 2026-06-09  
작업 내용: v1.25.0 배포 후 완전 삭제 요청 시 `Data truncated for column 'action_type'` 오류 수정

### 원인

`pending_admin_actions.action_type` 컬럼이 Hibernate 초기 DDL 생성 시 MySQL `ENUM('DELETE_USER','PROMOTE_ADMIN')` 타입으로 만들어졌음. `ddl-auto=update`는 기존 ENUM 컬럼에 새 값을 추가하지 않으므로 `HARD_DELETE_USER`(16자) 삽입 시 truncation 오류 발생.

### 수정 내용

코드 변경 없음. DB 스키마 직접 수정:

```sql
ALTER TABLE pending_admin_actions
  MODIFY COLUMN action_type VARCHAR(30) NOT NULL;
```

이후 동일 컬럼에 새 ActionType 값 추가 시 ALTER TABLE 불필요 (VARCHAR는 Java enum과 독립적).

### 재현 방지

`@Enumerated(EnumType.STRING)` 컬럼을 새 DB에 생성할 경우 Hibernate가 MySQL ENUM으로 만들 수 있음.  
ActionType 같이 향후 값이 추가될 가능성이 있는 컬럼은 `columnDefinition = "VARCHAR(30)"` 명시 권장.

---

## 세션 2026-06-09 (3) — 완전 삭제 수신함 승인 흐름 적용 (v1.25.0)

작업일: 2026-06-09  
작업 내용: 완전 삭제 버튼 클릭 시 즉시 삭제 → 수신함 승인 요청 흐름으로 변경

### 수정 파일

| 파일 | 변경 내용 |
|---|---|
| `admin/entity/PendingAdminAction.java` | `ActionType` enum에 `HARD_DELETE_USER` 추가 |
| `admin/service/UserAdminService.java` | 기존 `hardDelete()` 제거 → `requestHardDelete()` + `doHardDelete()` 분리 / `approveAction()`에 `HARD_DELETE_USER` 케이스 추가 / 이메일 템플릿 `buildHardDeleteApprovalHtml()` 추가 |
| `admin/controller/UserAdminController.java` | `DELETE /admin/users/{id}/permanent` — 즉시 삭제 → `requestHardDelete()` 호출로 변경, `@AuthenticationPrincipal` 파라미터 추가 |
| `views/admin/UserManagementView.vue` | 이중 confirm 제거, 승인 요청 toast 메시지로 변경 (목록에서 즉시 제거 안 함) |

### 구현 상세

**요청 흐름:**
1. ADMIN이 비활성 계정 행의 "완전 삭제" 클릭
2. 단일 confirm 확인
3. `DELETE /admin/users/{id}/permanent` 호출 → `PendingAdminAction(HARD_DELETE_USER)` 생성 + 수신함 발송
4. ADMIN이 수신함에서 승인 → `approveAction()` → `doHardDelete()` 실행 (native SQL)
5. 거부 시 pending action만 REJECTED 처리, 계정 유지

**`doHardDelete()` 내부 처리:**
- em.clear() 제거: approveAction → InboxService.approve 공유 트랜잭션에서 JPA 세션 오염 방지
- native SQL `DELETE FROM users WHERE id = :id` 사용 (JPA deleteById 대신)
- audit_log는 삭제 전 기록 (actor user_id FK 안전)

---

## 세션 2026-06-09 (2) — 수신함·사용자 관리 버그 수정 및 완전 삭제 기능 (v1.24.0)

작업일: 2026-06-09  
작업 내용: 수신함 승인 흐름의 다수 버그 수정 및 사용자 완전 삭제 기능 추가

### 수정 파일

| 파일 | 변경 내용 |
|---|---|
| `admin/service/UserAdminService.java` | `sendInboxToAdmins` 요청자 본인 제외 조건 제거 / `hardDelete` 메서드 추가 (native SQL 연쇄 처리) / `EntityManager` 주입 |
| `admin/controller/UserAdminController.java` | `DELETE /admin/users/{id}/permanent` 엔드포인트 추가 |
| `inbox/entity/InboxMessage.java` | `ActionStatus` enum 및 `action_status` 컬럼 추가 (`PENDING`/`APPROVED`/`REJECTED`, DB DEFAULT 'PENDING') |
| `inbox/service/InboxService.java` | `approve()` / `reject()` 처리 시 `actionStatus` DB 저장 |
| `inbox/dto/InboxMessageDto.java` | 응답에 `actionStatus` 필드 포함 |
| `incident/entity/Incident.java` | `reporter_id` FK nullable 변경 |
| `vulnerability/entity/Vulnerability.java` | `reporter_id` FK nullable 변경 |
| `vulnerability/entity/VulnerabilityComment.java` | `user_id` FK nullable 변경 |
| `training/entity/TrainingCourse.java` | `created_by` FK nullable 변경 |
| `policy/entity/Policy.java` | `author_id` FK nullable 변경 |
| `views/admin/UserManagementView.vue` | confirm 다이얼로그 이메일 하드코딩 제거 / "완전 삭제" 버튼 추가 (이중 확인) |
| `views/inbox/InboxView.vue` | `actionDone` 메모리 ref 제거, API `actionStatus` 기반 버튼 표시 |
| `api/index.js` | `adminApi.hardDeleteUser` 추가 |

### 버그 수정 내역

| 버그 | 원인 | 수정 |
|---|---|---|
| 계정 삭제 confirm 팝업에 "이메일 발송" 문구 노출 | 알림 방식과 무관하게 이메일 주소 하드코딩 | 중립적 문구로 교체 |
| 계정 삭제 요청 시 수신함 메시지 미생성 | `sendInboxToAdmins`에서 요청자 본인 제외 → 1인 ADMIN 환경에서 수신자 없음 | 요청자 포함 전체 ADMIN에게 전송 |
| 수신함 승인/거부 후 재진입 시 버튼 다시 표시 | 처리 결과를 컴포넌트 메모리에만 보관, DB 미저장 | `action_status` 컬럼 추가 및 API 응답 기반 표시 |

### 신규 기능: 사용자 완전 삭제

- 사용자 관리 테이블의 각 계정 행에 **완전 삭제** 버튼 추가 (이중 confirm)
- `DELETE /admin/users/{id}/permanent` — ADMIN 전용
- 삭제 시 데이터 처리 전략:
  - **NULL 처리** (비즈니스 레코드 보존): audit_logs, incidents, vulnerabilities, vulnerability_comments, policies, training_courses, isms_evidences, notices
  - **레코드 삭제** (사용자 소유): inbox_messages, pending_admin_actions, policy_acknowledgments, training_completions

---

## 세션 2026-06-09 — 수신함 알림 설정 저장 버그 수정 (v1.23.1)

작업일: 2026-06-09  
작업 내용: 알림 방식이 INBOX로 설정되어 있어도 계정 삭제 시 이메일이 발송되던 버그 수정

### 원인 분석

**1차 원인: 컨테이너 미재시작**  
`ApprovalNotificationService`에 INBOX 분기가 추가된 후 백엔드 이미지는 빌드되었으나 컨테이너가 재생성되지 않아 구 이미지가 계속 실행됨.  
→ `docker compose up -d backend`로 컨테이너 재생성 완료. DB의 `notification.method = INBOX`는 이미 올바르게 저장되어 있었음.

**2차 원인: saveNotifyConfig payload 불순물 가능성 (방어적 수정)**  
`notifyCfg.value` 전체를 API payload로 전송 시, 향후 ref 오염 등으로 Jackson `Map<String, String>` 역직렬화 실패 우려.  
→ 명시적으로 `{ method, approvalEmail, slackWebhookUrl }` 3개 필드만 추출해서 전송하도록 수정.

### 수정 파일

| 파일 | 변경 내용 |
|---|---|
| `views/settings/UISettingsView.vue` | `saveNotifyConfig()`에서 payload를 `{ method, approvalEmail, slackWebhookUrl }` 3필드로 명시적 추출 전송 |

---

## 세션 2026-06-08 (2) — 공지사항 및 인앱 수신함 (v1.23.0)

작업일: 2026-06-08  
작업 내용: 대시보드 공지사항 배너, ADMIN 공지사항 관리, 인앱 메시지 수신함(승인 처리 포함) 구현

### 신규 파일

**Backend:**

| 파일 | 설명 |
|---|---|
| `notice/entity/Notice.java` | 공지사항 엔티티 (title, content, active, pinned, createdBy) |
| `notice/dto/NoticeDto.java` | 공지사항 DTO |
| `notice/repository/NoticeRepository.java` | 공지사항 Repository |
| `notice/service/NoticeService.java` | 공지사항 CRUD 서비스 |
| `notice/controller/NoticeController.java` | 공지사항 REST API (GET: 인증 사용자, CRUD: ADMIN) |
| `inbox/entity/InboxMessage.java` | 인앱 수신 메시지 엔티티 (recipient, type, title, content, actionToken) |
| `inbox/dto/InboxMessageDto.java` | 수신함 DTO |
| `inbox/repository/InboxMessageRepository.java` | 수신함 Repository |
| `inbox/service/InboxService.java` | 수신함 서비스 (목록, 읽음, 승인/거부) |
| `inbox/controller/InboxController.java` | 수신함 REST API |

**Frontend:**

| 파일 | 설명 |
|---|---|
| `components/dashboard/NoticeBar.vue` | 대시보드 최상단 공지사항 배너 |
| `views/admin/AdminNoticeView.vue` | ADMIN 공지사항 관리 페이지 |
| `views/inbox/InboxView.vue` | 수신함 페이지 (승인/거부 처리 포함) |
| `stores/inbox.js` | 미읽음 카운트 store (30초 폴링) |

### 수정 파일

| 파일 | 변경 내용 |
|---|---|
| `admin/service/UserAdminService.java` | 계정 삭제/ADMIN 승격 요청 시 모든 ADMIN에게 inbox 메시지 전송 |
| `auth/repository/UserRepository.java` | `findAllByRoleAndActiveTrue` 메서드 추가 |
| `components/layout/AppLayout.vue` | 수신함 메뉴 (미읽음 배지) 및 ADMIN 공지사항 관리 메뉴 추가, inbox 폴링 시작/종료 |
| `views/dashboard/DashboardView.vue` | NoticeBar 컴포넌트 삽입 |
| `views/settings/UISettingsView.vue` | 메시지 수신함 섹션 추가 (최근 10건, 승인/거부 버튼) |
| `router/index.js` | `/inbox`, `/admin/notices` 라우트 추가 |
| `api/index.js` | `noticeApi`, `inboxApi` 추가 |
| `i18n/ko.json`, `i18n/en.json` | notice, inbox 번역 키 추가 |

### 주요 흐름

1. ADMIN이 계정 삭제/권한 승격 요청 → 기존 이메일/Slack 발송 + **모든 다른 ADMIN에게 inbox 메시지 생성**
2. ADMIN은 사이드바 **수신함** 메뉴(미읽음 배지 표시)에서 승인/거부 처리
3. **환경설정 → 메시지 수신함** 섹션에서도 최근 메시지 확인 및 승인/거부 처리 가능
4. ADMIN은 **공지사항 관리**(`/admin/notices`)에서 공지 추가/수정/삭제
5. 모든 사용자는 **대시보드 최상단**에서 활성 공지사항 확인

---

## 세션 2026-06-08 — 로그인 화면 UX 개선 (v1.22.0)

작업일: 2026-06-08  
작업 내용: 로그인 화면에 아이디 저장 체크박스 및 비밀번호 보기 아이콘 추가

### 수정 파일

| 파일 | 변경 내용 |
|---|---|
| `frontend/src/views/auth/LoginView.vue` | 비밀번호 보기/숨기기 토글 아이콘, 아이디 저장 체크박스 추가 |
| `frontend/src/i18n/ko.json` | `auth.saveId`, `auth.showPassword`, `auth.hidePassword` 키 추가 |
| `frontend/src/i18n/en.json` | 동일 키 영문 번역 추가 |

### 구현 내용

#### 비밀번호 보기/숨기기 토글
- 비밀번호 입력창 오른쪽에 눈 아이콘 버튼 추가
- 클릭 시 `input[type]`을 `password` ↔ `text`로 전환
- 상태에 따라 눈 아이콘 / 사선 눈 아이콘으로 변경

#### 아이디 저장
- 체크박스 체크 후 로그인 시 이메일을 `localStorage`(`login_saved_email` 키)에 저장
- 체크 해제 후 로그인 시 저장값 삭제
- 페이지 진입 시 저장값이 있으면 이메일 자동 입력 + 체크박스 활성화

---

## 세션 2026-06-02 (3) — 브랜드 로고 적용

작업일: 2026-06-02  
작업 내용: 사이드바 및 로그인 화면 로고를 브랜드 로고 SVG로 교체

### 수정 파일

| 파일 | 변경 내용 |
|---|---|
| `frontend/src/components/layout/AppLayout.vue` | 사이드바 로고 영역: 방패 아이콘 + "SecPortal" 텍스트 → 브랜드 로고 SVG (height 22px) |
| `frontend/src/views/auth/LoginView.vue` | 로그인 화면 상단: 방패 아이콘 + 타이틀 → 브랜드 로고 SVG (height 36px) |

### 구현 방식

로고 SVG 원본을 인라인으로 삽입.  
`fill="currentColor"` + 부모 요소의 텍스트 색상 클래스로 다크/라이트 사이드바 자동 대응:

```html
<!-- 다크 사이드바: text-white → fill white -->
<!-- 라이트 사이드바: text-gray-900 → fill dark -->
<svg fill="currentColor" :class="isDark ? 'text-white' : 'text-gray-900'">
  ...
</svg>
```

외부 CDN 요청 없이 동작하므로 오프라인·인트라넷 환경에서도 로고가 정상 표시됩니다.

---

## 세션 2026-06-02 (2) — UI 리디자인 및 환경설정

작업일: 2026-06-02  
작업 내용: 핀테크 스타일 UI 리디자인 및 환경설정 메뉴 구현

### 구현 내용

#### 동적 테마 시스템

Tailwind CSS 색상 값을 모두 CSS Custom Properties(`var()`)로 전환하여 런타임 테마 변경을 구현.

```
tailwind.config.js  →  primary-500: 'var(--color-primary-500)'
main.css            →  :root { --color-primary-500: #0064FF; ... }
uiSettings.js       →  document.documentElement.style.setProperty(...)
```

#### 신규 파일

| 파일 | 역할 |
|---|---|
| `frontend/src/stores/uiSettings.js` | 테마/폰트/글자크기/사이드바 설정 스토어 |
| `frontend/src/views/settings/UISettingsView.vue` | `/settings` 환경설정 페이지 |

#### 수정 파일

| 파일 | 변경 내용 |
|---|---|
| `frontend/tailwind.config.js` | primary 색상을 CSS 변수로 전환, shade 300·400 추가 |
| `frontend/src/assets/main.css` | Pretendard 폰트 CDN, CSS 변수 기본값, 공통 컴포넌트 클래스 |
| `frontend/src/components/layout/AppLayout.vue` | 전면 리디자인, 다크/라이트 사이드바, 설정 메뉴 링크 |
| `frontend/src/router/index.js` | `/settings` 라우트 추가 |
| `frontend/src/i18n/ko.json` | `nav.settings`, `settings.*` 번역 키 추가 |
| `frontend/src/i18n/en.json` | 동일 |
| `frontend/src/main.js` | `useUiSettingsStore().init()` 앱 초기화 시 호출 |

#### 설정 항목

| 항목 | 선택지 | 기본값 |
|---|---|---|
| 컬러 테마 | Blue / Navy / Emerald / Purple / Rose | Blue |
| 폰트 | Pretendard / Noto Sans KR / 시스템 기본 | Pretendard |
| 글자 크기 | 작게(13px) / 보통(14px) / 크게(16px) | 보통(14px) |
| 사이드바 스타일 | 어두운 / 밝은 | 어두운 |

#### 주의사항

- Tailwind opacity modifier(`bg-primary-500/50`)는 CSS 변수 색상과 함께 사용 불가 — 빌드 타임에 hex 값을 알 수 없기 때문
- 사이드바 스타일은 CSS 변수 없이 Vue 반응형(`computed`)으로 처리 (`isDark` computed → 조건부 클래스)
- 글자 크기는 `html` 요소의 `font-size` 변경으로 rem 기반 전체 스케일이 함께 조정됨

### 문서 반영

- `docs/source-code.md`: 섹션 13에 UI 테마 시스템 설명, uiSettings 스토어 API 레퍼런스 추가

---

## 세션 2026-06-02 (1) — 버그 수정

작업일: 2026-06-02  
작업 내용: 로그인 화면 미표시 버그 수정 및 문서 반영

### 문제

`/api/auth/me` 가 무한 반복 호출되며 로그인 화면이 표시되지 않는 현상.

### 원인

`frontend/src/api/index.js` Axios 응답 인터셉터의 401 처리 로직에서 `localStorage.removeItem('token')`만 수행하고 Pinia store의 `token` ref를 초기화하지 않았음.

루프 순서:
1. localStorage에 만료된 토큰 존재
2. `isAuthenticated = !!token.value` → `true`
3. 라우터 가드: `/dashboard` 접근 → `fetchMe()` 호출 → 서버 401 반환
4. 인터셉터: localStorage만 삭제, store의 `token.value`는 유지
5. `router.push('/login')` → 라우터 가드에서 `isAuthenticated`가 여전히 `true` → Dashboard로 즉시 리다이렉트
6. 3번으로 돌아가 무한 반복

### 수정

**`frontend/src/api/index.js`** (401 처리):

```js
// 수정 전
localStorage.removeItem('token')
router.push('/login')

// 수정 후
const auth = useAuthStore()
auth.logout()    // token.value = null + localStorage 제거
router.push('/login')
```

### 문서 반영

- `docs/source-code.md`: Axios 인터셉터 코드 예시 및 주의 사항 업데이트

---

### 문제 2

배포 직후 `GET /api/auth/me` 502 Bad Gateway 발생, nginx 에러 로그:

```
connect() failed (111: Connection refused) while connecting to upstream,
upstream: "http://172.18.0.3:8080/api/auth/me"
```

### 원인 2

`docker-compose.yml`에서 백엔드 DB 접속 환경변수 3개가 로컬에서 삭제된 상태로 저장되어 있었음.

```yaml
# 누락된 환경변수
SPRING_DATASOURCE_URL: jdbc:mysql://db:3306/secportal?...
SPRING_DATASOURCE_USERNAME: ${DB_USER:-secportal}
SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD:-secportal123}
```

`application.yml` 기본값이 `localhost:3306`이어서 Docker 컨테이너 환경에서는 DB에 연결 불가 → Spring Boot 기동 실패 → nginx 502.

> **참고:** 해당 3줄은 저장소의 마지막 커밋에는 존재했으나 로컬 작업 디렉토리에서만 삭제된 상태였음. `--build` 재배포 시 변경된 로컬 파일 기준으로 컨테이너가 재생성되어 문제 발생.

### 수정 2

`docker-compose.yml` 환경변수 복구 후 백엔드 컨테이너 재시작.  
파일이 커밋 상태로 복원되었으므로 별도 커밋 불필요.

### 주의사항

- `docker compose up --build` 전 `git diff docker-compose.yml`로 설정 누락 여부 확인 권장
- Docker 환경에서 DB 호스트는 반드시 서비스명 `db`를 사용해야 하며 `localhost`는 동작하지 않음

---

## 세션 2026-05-16

작업일: 2026-05-16  
작업 내용: 소스코드 문서화 및 Slack 연동 메뉴얼 작성

---

## 1. 프로젝트 구조 탐색

프로젝트 전체 파일 목록을 확인하여 아래 구조를 파악했습니다.

```
D:\Project\monosun\
├── docker-compose.yml
├── .env.example
├── nginx\nginx.conf
├── db\init\
│   ├── 01_schema.sql
│   ├── 02_seed.sql
│   ├── 03_comments.sql
│   ├── 04_assets.sql
│   └── 05_incidents.sql
├── backend\
│   ├── build.gradle
│   ├── Dockerfile
│   ├── settings.gradle
│   └── src\main\java\com\monosun\secportal\
│       ├── SecPortalApplication.java
│       ├── auth\
│       ├── common\
│       ├── policy\
│       ├── vulnerability\
│       ├── incident\
│       ├── asset\
│       ├── training\
│       ├── audit\
│       ├── admin\
│       ├── metrics\
│       ├── notification\
│       └── report\
├── frontend\
│   ├── package.json
│   ├── vite.config.js
│   └── src\
│       ├── main.js
│       ├── App.vue
│       ├── api\index.js
│       ├── router\index.js
│       ├── stores\auth.js
│       ├── i18n\
│       ├── components\layout\
│       └── views\
│           ├── auth\
│           ├── dashboard\
│           ├── policy\
│           ├── vulnerability\
│           ├── incident\
│           ├── asset\
│           ├── training\
│           └── admin\
└── docs\
    ├── api.md
    ├── architecture.md
    ├── aws-deployment.md
    └── development-setup.md
```

---

## 2. 주요 소스파일 분석 결과

### 기술 스택 확인

**`backend/build.gradle`** 분석 결과:

| 항목 | 내용 |
|------|------|
| Spring Boot | 3.3.5 |
| Java | 17 |
| JWT | jjwt 0.12.6 |
| Database | MySQL (com.mysql:mysql-connector-j) |
| Email | spring-boot-starter-mail |
| PDF | openpdf 1.3.30 |
| Utilities | Lombok |

**`docker-compose.yml`** 분석 결과:

| 서비스 | 이미지 | 포트 |
|--------|--------|------|
| db | mysql:8.0 | 3306 |
| backend | 로컬 빌드 | 8080 |
| frontend | 로컬 빌드 (Nginx) | 80 |

- DB healthcheck: `mysqladmin ping` 통과 후 backend 기동
- 환경변수: `.env` 파일에서 주입 (`DB_PASSWORD`, `JWT_SECRET` 등)

---

### 인증 설계 확인

**`auth/entity/User.java`** 분석:
- `UserDetails` 직접 구현 → 별도 래퍼 없이 Spring Security 통합
- `active=false` 시 `isAccountNonLocked()` → 로그인 차단
- Role: `ADMIN | MANAGER | USER`

**`common/config/SecurityConfig.java`** 분석:
- CSRF 비활성화 (JWT Stateless 방식)
- 공개 경로: `/auth/**`, `/actuator/health`
- CORS: `allowedOriginPatterns("*")` — 개발환경 전체 허용
- `JwtAuthenticationFilter`를 `UsernamePasswordAuthenticationFilter` 앞에 등록

**`common/security/JwtTokenProvider.java`** 분석:
- HMAC-SHA256 서명
- Subject: 이메일 주소
- `JWT_SECRET`이 32자 미만이면 jjwt 예외 발생

---

### 도메인 엔티티 설계 확인

**`vulnerability/entity/Vulnerability.java`**:
```
severity: CRITICAL | HIGH | MEDIUM | LOW | INFO
status:   OPEN | IN_PROGRESS | RESOLVED | ACCEPTED | FALSE_POSITIVE
```
- `RESOLVED` 전환 시 `resolvedAt` 자동 기록
- `assignee` (담당자), `reporter` (등록자) 분리

**`incident/entity/Incident.java`**:
```
severity: CRITICAL | HIGH | MEDIUM | LOW
status:   OPEN | INVESTIGATING | CONTAINED | RESOLVED | CLOSED
type:     MALWARE | PHISHING | DATA_BREACH | UNAUTHORIZED_ACCESS
          DDOS | INSIDER_THREAT | PHYSICAL | OTHER
```

**`policy/entity/Policy.java`**:
```
category: GENERAL | ACCESS_CONTROL | DATA_PROTECTION | INCIDENT_RESPONSE
          NETWORK | PHYSICAL | VENDOR | OTHER
status:   DRAFT → REVIEW → PUBLISHED → ARCHIVED
```
- `PolicyAcknowledgment`: `(policy_id, user_id)` unique 제약으로 중복 확인 방지

**`asset/entity/Asset.java`**:
```
type:        SERVER | WORKSTATION | NETWORK | APPLICATION | DATABASE | CLOUD | OTHER
criticality: HIGH | MEDIUM | LOW
```
- `BaseEntity` 미상속 — `@PreUpdate`로 직접 `updatedAt` 관리

**`training/entity/TrainingCourse.java`**:
```
contentType: VIDEO | DOCUMENT | QUIZ_ONLY
passingScore: 기본 70점
```
- `QuizQuestion`: A/B/C/D 선택지, `sortOrder`로 출제 순서 제어
- `TrainingCompletion`: `(course_id, user_id)` unique → 재도전 시 UPDATE

---

### 알림 시스템 확인

**`notification/service/NotificationScheduler.java`**:
- cron: `${notification.overdue-check-cron}` (기본: 평일 오전 9시)
- `VulnerabilityRepository.findOverdue(today)` → 기한 초과 취약점 조회
- 담당자(`assignee`) 이메일로 HTML 알림 발송

**`notification/service/EmailService.java`**:
- `@Async` 비동기 발송
- `MAIL_PASSWORD` 미설정 시 `try/catch`로 예외 흡수 → 시스템 정상 동작

---

### 감사 로그 설계 확인

**`audit/service/AuditLogService.java`**:
- `@Transactional(propagation = REQUIRES_NEW)` — 메인 트랜잭션과 독립
- 현재 기록 시점: `VULN_CREATED`, `VULN_STATUS_CHANGED`, `VULN_DELETED`, `POLICY_ACKNOWLEDGED`

---

### 대시보드 지표 확인

**`metrics/service/MetricsService.java`** 집계 항목:

| 지표 | 계산 방식 |
|------|----------|
| totalAssets | `countByActive(true)` |
| highCriticalityAssets | `countByCriticality(HIGH)` |
| overdueVulns | `findOverdue(today).size()` |
| openVulns | `countByStatus(OPEN)` |
| openIncidents | `countByStatus(OPEN)` + `countByStatus(INVESTIGATING)` |
| criticalIncidents | `countBySeverity(CRITICAL)` |
| policyAckRate | 최소 1건 확인한 사용자 / 전체 사용자 × 100 |
| trainingCompletionRate | 최소 1건 통과한 사용자 / 전체 사용자 × 100 |
| vulnTrend | 최근 6개월 월별 취약점 등록 수 |

---

### 프론트엔드 확인

**`frontend/src/api/index.js`** API 객체 목록:

| 객체 | 대상 |
|------|------|
| `authApi` | /auth (login, register, me) |
| `policyApi` | /policies (CRUD + acknowledge) |
| `vulnApi` | /vulnerabilities (CRUD + stats) |
| `vulnCommentApi` | /vulnerabilities/:id/comments |
| `trainingApi` | /training/courses (CRUD + submitQuiz) |
| `incidentApi` | /incidents (CRUD) |
| `assetApi` | /assets (CRUD) |
| `adminApi` | /admin/users, /admin/audit-logs |
| `metricsApi` | /metrics/summary |
| `reportApi` | /reports/** (PDF) |
| `exportApi` | /reports/** (CSV/PDF 다운로드) |

Axios 인터셉터:
- 요청: `localStorage`의 JWT → `Authorization: Bearer` 헤더 자동 첨부
- 응답 401: 토큰 삭제 → `/login` 리다이렉트
- 응답 data unwrap: `res => res.data` → 컴포넌트에서 `response.data.xxx` 대신 `response.xxx`로 접근

**`frontend/src/router/index.js`** 라우트 목록:

| 경로 | 컴포넌트 | 접근 제한 |
|------|----------|----------|
| /login | LoginView | public |
| /register | RegisterView | public |
| /dashboard | DashboardView | 인증 필요 |
| /policies | PolicyListView | 인증 필요 |
| /vulnerabilities | VulnerabilityListView | 인증 필요 |
| /incidents | IncidentListView | 인증 필요 |
| /assets | AssetListView | 인증 필요 |
| /training | TrainingListView | 인증 필요 |
| /admin/users | UserManagementView | adminOnly |
| /admin/audit-logs | AuditLogView | adminOnly |

---

## 3. 기존 문서 현황 확인

`docs/` 디렉토리에 이미 4개 문서가 존재함을 확인:

| 파일 | 내용 |
|------|------|
| `architecture.md` | 전체 아키텍처, 패키지 구조, ERD, 보안 고려사항 |
| `api.md` | REST API 전체 명세 (엔드포인트, 요청/응답 예시) |
| `development-setup.md` | OS별 개발환경 구성, IDE 설정, 트러블슈팅 |
| `aws-deployment.md` | EC2 배포, HTTPS, CI/CD, 운영 매뉴얼, 장애 대응 |

---

## 4. 신규 생성 문서

### docs/source-code.md

소스코드를 처음 보는 개발자를 위한 도메인별 코드 해설 문서 (신규 생성)

**포함 내용**:
- 진입점 어노테이션 설명 (`@EnableJpaAuditing`, `@EnableScheduling`)
- 공통 인프라 코드 해설 (`BaseEntity`, `ApiResponse`, `GlobalExceptionHandler`)
- Spring Security 설정 상세 분석
- JWT 토큰 흐름 설명
- 8개 도메인별 엔티티 필드 + 서비스 핵심 로직 코드 발췌 설명
- 감사 로그 `REQUIRES_NEW` 전파 속성 사용 이유
- 메트릭 집계 코드 상세 분석
- 프론트엔드 Pinia 스토어, Axios, 라우터 가드 동작 흐름
- 새 도메인 추가 체크리스트 + 권한 패턴 정리

---

### docs/slack-integration.md

Slack 연동 구현 가이드 (신규 생성)

**포함 내용**:

1. **Slack App 생성 및 Webhook URL 발급** (단계별 UI 경로 안내)
2. **알림 시나리오 정의** — 이벤트별 채널/우선순위 매핑표
3. **`SlackService` 전체 구현 코드**
   - `WebClient` 기반 비동기 HTTP POST
   - 채널별 Webhook URL 분리 (`alerts`, `incidents`, `info`)
   - `slack.enabled=false`일 때 무조건 skip
4. **`SlackMessageBuilder` 유틸리티 코드**
   - 심각도별 이모지 매핑 (`:red_circle:`, `:rotating_light:` 등)
   - 취약점 기한 초과, 신규 인시던트, CRITICAL 취약점 메시지 포맷
5. **기존 코드 연동 위치**
   - `NotificationScheduler.java` 수정 방법 (이메일 + Slack 동시 발송)
   - `IncidentService.java` 수정 방법 (인시던트 생성 시 즉시 알림)
   - `VulnerabilityService.java` 수정 방법 (CRITICAL 등록 시 즉시 알림)
6. **환경변수 설정** — `application.yml`, `.env`, `.env.example` 수정 내용
7. **테스트 방법** — curl 직접 테스트, 스프링 테스트 코드, DB 테스트 데이터 SQL
8. **Block Kit 고급 포맷** — 버튼 포함 구조화 메시지 JSON 예시
9. **트러블슈팅** — 메시지 미수신, WebClient 빈 오류, 채널 초대 누락 등

---

## 5. 최종 docs 디렉토리 현황

```
docs\
├── architecture.md       # 기존 — 전체 아키텍처, ERD, 인증 흐름
├── api.md                # 기존 — REST API 전체 명세
├── development-setup.md  # 기존 — 개발환경 구성 가이드
├── aws-deployment.md     # 기존 — AWS 배포·운영 매뉴얼
├── source-code.md        # 신규 — 소스코드 도메인별 상세 해설
└── slack-integration.md  # 신규 — Slack 연동 구현 메뉴얼
```
