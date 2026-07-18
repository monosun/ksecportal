# SecPortal — 오픈소스 정보보호 포탈

스타트업·중소기업을 위한 **올인원 정보보안 관리 시스템**입니다.  
보안 정책, 취약점, 인시던트, 자산, 보안이벤트, 교육을 단일 플랫폼에서 관리합니다.

> **최신 버전: v1.12.0** — 개인정보 영향평가(DPIA) 보고서 첨부파일(구분별 다중 첨부·다운로드·첨부 건수 표시) ([릴리즈 노트](release/v1.12.0/RELEASE_NOTES.md))

```bash
# 빠른 시작
git clone https://github.com/monosun/ksecportal.git
cd ksecportal
docker compose up -d --build
```

브라우저에서 **http://localhost** 접속  
기본 관리자: `secportal@monosun.com` / `Ksecurity!!!` (최초 로그인 시 비밀번호 변경 필요)

---

## 주요 기능

| 모듈 | 기능 요약 |
|------|-----------|
| **보안 정책** | Markdown 작성, 카테고리·버전 관리, 수신 확인, 배포 워크플로, 엑셀 일괄 등록, PDF/CSV 다운로드 |
| **취약점 관리** | CVE/CVSS 등록, 심각도·상태 추적, 담당자 배정, 댓글, 기한 알림, 엑셀 일괄 등록, CSV 다운로드, **NVD API 자동 조회(CVE ID 입력 시 CVSS 점수·심각도·설명 자동 입력, NVD 링크)** |
| **위협 관리** | 위협 카탈로그 CRUD, 위협 유형·발생 가능성·영향도·위험 점수 관리, 색상 코드 위험도 표시, **기본 위협항목 추가(560개 Master Risk Register 일괄 로드, 중복 체크)**, **컬럼별 인라인 필터(이름·유형·카테고리·발생가능성·잠재영향·위험수준·수정일), 결과 건수 요약, 필터 초기화** |
| **위험평가** | 자산×위협 카르테지안 곱 전수 자동생성(차수 추가 시 1클릭), 위험 등급 자동 계산(발생가능성×영향도), 위험 처리 방안 결정(**수용·감소·회피·이전 4종 일괄처리**), 스냅샷 격리(자산·위협 변경에 영향 없음), 페이지네이션(10/20/50/100건), **엑셀 다운로드(전체 요약 시트 + 자산별 개별 시트, 위험등급 셀 색상 자동 적용)**, **위험수용 기준 점수 설정(수용 기준 이하 자동선택·일괄처리)**, **체크박스 전체/개별 선택**, **검색 필터(자산·위협·취약점·등급·처리방법·위험점수 범위)**, 요약 카드 차수 전체 기준 고정 |
| **위험 처리 계획** | 위험평가 **완료 차수**의 처리방법 '감소' 항목을 연동해 조치 계획 수립(조치계획·담당자·완료기한·진행률·상태), **연도/차수별 스냅샷**, 상태 탭·검색 필터·페이지네이션 |
| **위험처리 계획** | 처리 계획 등록, 진행률 프로그레스바, 회피/수용/이전/감소 탭 필터, 담당자·기한 관리 |
| **ISMS-P 통제항목 매핑** | 도메인·통제항목 2패널 레이아웃, ISMS-P 인증기준 직접 매핑 관리 |
| **보안 인시던트** | 8가지 유형, 5단계 상태 추적, 대응 타임라인, 엑셀 일괄 등록, PDF 다운로드 |
| **자산 관리** | IT 자산 인벤토리, 온프레미스+클라우드(AWS/GCP/Azure) 통합, 13가지 유형, 환경·중요도 분류, 월 비용·계약 만료·점검 일정 관리, PDF/CSV 다운로드, **ISMS-P 자산식별 기준(자산유형 6종·기밀성/무결성/가용성 개별 등급·개인정보 포함 여부·보안관리 대상·연계 시스템·운영중/중지/폐기 상태 관리)**, **자산유형별 현황·유형 단위 일괄 삭제**, **자산 시점(스냅샷) 이력(저장 시점 자산 목록 보관)**, 등록/수정/상세 팝업 UX |
| **SBOM 관리** | **CycloneDX 1.5 표준 기준** 소프트웨어(SW명+버전)별 구성요소 관리(type·group·name·version·PURL·SPDX 라이선스), **CycloneDX JSON 내보내기/가져오기(syft·cdxgen·trivy 등 SCA 도구 산출물 호환)**, 엑셀 템플릿 일괄등록(동일 SW 자동 병합·재업로드 시 중복 없이 갱신), SW 자산 등록 시 SBOM 맵핑(자산유형 SW 선택 시 등록된 SW 선택), 자산 상세에 맵핑 SW·라이브러리 수 표시 |
| **보안이벤트 관리** | 방화벽·IDS/IPS·WAF·SIEM·EDR 등 10종 보안솔루션 연동, 이벤트 실시간 모니터링, 심각도 필터, 30초 자동 새로고침 |
| **로그 통합관리** | 개인정보처리시스템·AD·NAC·망연계 로그 조회(연동 준비), 날짜·결과 필터, 통합검색 UI |
| **IT 및 정보보호 교육** | 코스 관리, 객관식 퀴즈(한 문항씩 풀이·진행률 바·문항 번호 점프·미답변 안내), **퀴즈 오답 리뷰(내가 선택한 답·정답·해설 표시)**, **문제은행(분류·난이도·키워드 검색, 엑셀 일괄 업로드, 난이도별 랜덤 출제, 기본 샘플 문항)**, **교육·훈련 결과 화면(교육 이수 현황·모의훈련 결과 탭)**, 이수율·점수 추적 |
| **모의 악성메일 훈련** | 피싱 이메일 템플릿 CRUD(HTML 편집·변수 치환), 발송대상 관리, 캠페인 생성·실시·완료·취소, 클릭/열람/신고 실시간 추적, **발송 처리 결과 로그(대상별 성공/실패·실패 사유·발송/열람/클릭 시각)**, 결과 통계 대시보드 |
| **ISMS-P 증적관리** | 101개 인증항목(관리체계·보호대책·개인정보 3개 섹션), 연도별 증적 CRUD, 준수 상태 4단계, CSV 다운로드, PDF 다운로드, 엑셀 일괄 등록, **다른 항목 증적 파일 참조 등록(파일 중복 업로드 없이 재사용)** |
| **월간 보안점검** | 정보보호의 날 기준 32개 표준 점검 항목, 년월 네비게이션(←→), 우선순위(상·중·하)·구분 필터, 완료율 프로그레스바, 결과 원클릭 토글, 항목 CRUD, **담당자 지정(사용자 검색·직접입력)**, **증적 파일 관리(파일 첨부·다운로드)**, **기본 항목 불러오기 초기화 경고·중복 체크**, **이전 월 항목 복사(결과는 미완료로 초기화·담당자 승계)** |
| **대시보드** | 위험현황(5×5 히트맵·등급별 통계·고위험 항목), 취약점 현황(심각도 바차트·기한초과 목록), 인시던트 현황(월별 추이·최근 5건), ISMS-P 이행률(코닉 게이지·도메인별 진행바), 증적 제출 현황(도메인별 제출/미제출 테이블), **KRCERT RSS 위젯(취약점 정보·보안공지 탭, 최근 7일치)** — **실 DB 데이터 실시간 반영** |
| **감사 로그** | 모든 주요 액션 자동 기록, IP 주소 자동 캡처, 날짜·시간 범위 검색, 관리자 조회 |
| **코드 관리** | 부서·분류 등 공통 코드 그룹/값 관리 (ADMIN), 회원가입 부서 드롭다운 연동, **월간 점검·위협 기본·위협 유형별 목록 20행 페이지네이션**, **개인정보 분류별 항목 코드 초기 데이터(13개 분류·74개 항목: 기본 식별정보·연락처·신분증·단말기·결제·신용·서비스이용·위치·복지·번호이동·미성년자대리인·마케팅·민감정보)** |
| **사용자 관리** | 계정 추가·삭제·역할 변경, 이메일 승인 워크플로우, 계정 엑셀 일괄 등록, CSV/PDF 다운로드 |
| **알림 / 수신함** | 이메일·Slack·Both·수신함 방식 승인 알림, 기한 초과 취약점 자동 알림(스케줄러), **수신함 비우기** |
| **리포트** | 전 메뉴 PDF/CSV, 한/영 파일명·헤더 자동 전환 |
| **보안문서 관리** | 보안 가이드·정책서·절차서 등 8종 문서 관리, 버전 이력 보관, 파일 첨부·다운로드, 카테고리·키워드 검색, **검색 대상 선택(제목·내용·파일명·버전·제작기관)**, **제작기관 입력(코드관리 SEC_DOC_ORG 선택 또는 직접 입력)** |
| **법령준수관리** | 업종별 적용 법령 카탈로그(21개 업종·77개 법령), **법령검토(법제처 Open API 실시간 전체 조문 조회·행정규칙 포함)**, 조문별 검토의견 작성, **검토이력(선택 법령 전체 세션 단위·조문 스냅샷·이전 검토 대비 변경 조문 빨간색 표시)**, Excel 검토 보고서(표지·법령정보·법령별 시트, 변경 조문 빨간색·[신설]/[변경] 마커) |
| **설정관리** | 보안 설정(로그인 잠금), Okta SSO, AI LLM 연동(OpenAI/Claude/Ollama), 업종 설정, 법제처 API 키, **회사정보 등록(회사명·대표자·홈페이지·연락처·주소·소개 — PDF/Excel 보고서에 자동 반영)** |
| **개인정보보호 — 수탁사 관리** | 개인정보 처리 수탁사 CRUD(수탁사명·사업자번호·대표자·위탁업무·**재수탁사**·계약기간·담당자 정보), **개인정보처리방침에서 수탁사 일괄등록(방침 URL의 위탁 표에서 수탁사·위탁업무·재수탁사 자동 추출 → 팝업에서 확인·수정 → 선택 항목 일괄 등록, 기존 등록분 중복 자동 제외)**, 수탁사별 점검 이력 관리(점검일·점검자·상태·결과), 점검별 증적 파일 업로드·다운로드·삭제 |
| **개인정보보호 — 수탁사 점검** | 점검항목 관리(기본 20개 템플릿 일괄 로드·CRUD), 연도별·수탁사별 점검 실시(통과/미흡/해당없음/미점검 4단계), 진행률 바, 기본 점검항목과 운영 점검항목 동기화 |
| **개인정보보호 — 처리현황·파일관리** | 개인정보 처리업무 등록(처리부서·목적·항목·보유기간·처리근거·처리시스템·업무흐름도·라이프사이클), **개인정보 흐름도**(업무별 라이프사이클 강조·업무흐름 단계 그래프·나가는 제공처), **전체 흐름 지도**(처리업무 → 시스템·파일 → 제공처, 미연결 파일·제공 누락 점검), 개인정보파일대장(DB 테이블·담당부서·민감정보/고유식별정보 포함 여부·보유 건수) |
| **개인정보보호 — 수집·이용/제공** | 수집동의서 버전관리(필수·선택 구분·이용목적·처리근거·개정본 생성), 제3자 제공·공동이용·국외이전 관리(제공받는 자·항목·목적·근거·계약, 위탁과 별도) |
| **개인정보보호 — 보유기간·파기** | 보유기간 만료·삭제 예정 추적(만료 D-day·연장사유·자동알림), 파기계획·승인·이력·증적, **파기완료 시 연계된 보유기간 항목 자동 파기완료 전이** |
| **개인정보보호 — 영향평가(DPIA)** | 영향평가 대상 선정·체크리스트·위험도(높음/보통/낮음)·개선계획·완료보고, 공공기관 PIA 및 민간 자체 영향평가 |
| **개인정보보호 — 유출관리** | 유출사고 등록, **신고기한 자동 산정(인지 후 72시간)·경과 경고**, 정보주체 통지, 관계기관 신고, 조사결과·재발방지 (보안 인시던트와 별도 운영) |
| **개인정보보호 — 정보주체 권리행사** | 열람·정정·삭제·처리정지·동의철회 접수·처리결과, **SLA 자동 산정(요청일 + 10일)·기한 초과 표시** |
| **개인정보보호 — 보호조치 관리** | 접근권한 점검·권한회수·암호화 적용현황·접속기록 점검결과·출력물 관리·반출관리·휴면계정 관리 (관리현황 중심) |
| **개인정보보호 — 현황보고서** | 경영진·ISMS-P 심사용 자동 집계 — 처리현황·개인정보파일·수탁사·제3자 제공·보유기간·파기·권리행사·유출사고·법령 준수현황 9개 영역 |
| **Okta SSO** | OAuth2 PKCE 방식 Okta 연동(클라이언트 시크릿 불필요), 기존 이메일 계정 자동 연동(okta_id 매핑), 신규 사용자 자동 프로비저닝(USER 역할), 관리자 환경설정 UI(활성화 토글·Client ID·Issuer·Redirect URI·연결 테스트), DB 또는 env var(OKTA_ENABLED) 설정 |
| **RBAC 권한관리** | 메뉴별 읽기·쓰기·삭제 권한 설정, Role 생성·사용자 배정, 다중 Role 권한 합산(OR), ADMIN은 항상 전체 권한 |
| **MFA / 보안 설정** | TOTP 기반 2단계 인증(Google Authenticator 호환), 로그인 실패 잠금(지수 백오프), ADMIN 전용 최대 실패 횟수·잠금 시간 설정 |
| **세션 타임아웃** | ADMIN에서 분 단위 만료시간 설정(5~1440분), 만료 2분 전 경고 모달(카운트다운·연장·로그아웃), JWT exp 클레임 기반 정확한 타이머 |
| **비밀번호 정책** | 최초 로그인 강제 변경(관리자 설정 계정), 강도 규칙 일관 적용(8자 이상·대소문자·숫자·특수문자 각각 포함), 프론트·백엔드 이중 검증 |
| **정보보호위원회** | 연도별·회차별(제1회·제2회…) 회의 관리, 상태(예정/완료/취소), 안건·회의록·기타 파일 첨부·다운로드 |
| **내부감사** | 연도별 감사 등록, 점검대상(시스템·서비스) 단위 점검항목 관리, 결과(양호/취약/해당없음)·발견사항·조치사항 기록, 감사 보고서 파일 첨부 |
| **보안 결함사항** | ISMS-P 인증심사·내부감사 결함 등록, 위험도(4단계)·처리상태 추적, 인증기준 코드·시정조치 계획·조치기한 관리, 증적 파일 첨부 |
| **백업 관리** | DB 전체 데이터 AES-256-GCM 암호화 백업(PBKDF2 키 파생), 즉시 다운로드·서버 저장 선택, 암호화 파일 복원(비밀번호 검증), 정기 백업 Cron 스케줄(활성화/비활성화), 보관 개수 자동 정리, 백업 이력 조회 |
| **UI 커스터마이징** | 6가지 컬러 테마(Monosun 포함), 폰트·글자 크기·사이드바 스타일·로고 설정 |
| **다국어** | 한국어 / 영어 전환 (vue-i18n) |

---

## 빠른 시작

### 사전 요구사항

- Docker Desktop 4.x 이상 (Docker Compose v2 포함)

### 실행

```bash
git clone https://github.com/monosun/ksecportal.git
cd secportal
docker compose up -d --build
```

브라우저에서 **http://localhost** 접속

**기본 관리자 계정**

| 항목 | 값 |
|------|----|
| 이메일 | `secportal@monosun.com` |
| 초기 비밀번호 | `Ksecurity!!!` |

> 최초 로그인 시 비밀번호 변경을 강제합니다. 새 비밀번호는 8자 이상이며 대·소문자·숫자·특수문자를 각각 포함해야 합니다.

> **주의** 프로덕션 배포 전 반드시 `.env`의 `JWT_SECRET`, `DB_ROOT_PASSWORD`, `DB_PASSWORD`를 변경하세요.

### 업그레이드

```bash
git pull
docker compose down
docker compose up -d --build
```

---

## 기술 스택

| 레이어 | 기술 |
|--------|------|
| **Frontend** | Vue 3, Vite, Pinia, Tailwind CSS, vue-i18n, Chart.js, Axios |
| **Backend** | Spring Boot 3.5, Spring Security 6.5 (JWT), Spring Data JPA, Hibernate 6.6 |
| **Runtime** | Java 21 (Temurin) |
| **Database** | MySQL 8.4 LTS |
| **인프라** | Docker Compose, Nginx (reverse proxy + SPA fallback) |
| **폰트** | Inter, Noto Sans KR (Google Fonts), Pretendard |

---

## 역할(Role) 정의

| Role | 권한 |
|------|------|
| **ADMIN** | 전체 조회·생성·수정·삭제, 사용자 관리, 감사 로그, 코드 관리, RBAC 권한관리, 리포트 |
| **MANAGER** | 정책·취약점·인시던트·자산·보안이벤트·보안문서 생성·수정, 리포트 다운로드 |
| **USER** | 조회, 정책 수신 확인, 취약점·인시던트 등록, 교육 이수 |

> RBAC 권한관리로 메뉴별 세분화 권한 추가 설정 가능 (ADMIN 설정)

---

## 환경변수

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `DB_ROOT_PASSWORD` | `rootpassword` | MySQL root 비밀번호 |
| `DB_USER` | `secportal` | 앱 DB 사용자 |
| `DB_PASSWORD` | `secportal123` | 앱 DB 비밀번호 |
| `JWT_SECRET` | (내장) | 최소 32자 비밀키 |
| `JWT_EXPIRATION` | `86400000` | 토큰 유효시간 (ms) |
| `MAIL_HOST` | `smtp.gmail.com` | SMTP 서버 |
| `MAIL_PORT` | `587` | SMTP 포트 |
| `MAIL_USERNAME` | — | 발신 이메일 (Gmail 권장) |
| `MAIL_PASSWORD` | — | Gmail 앱 비밀번호 (비어 있으면 발송 생략) |
| `APP_BASE_URL` | `http://localhost:8080/api` | 이메일 링크 기준 URL (설정관리 > 시스템 설정의 "이메일 발송 링크 도메인 주소"가 등록되어 있으면 그 값이 우선) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost,http://localhost:80` | CORS 허용 출처 |
| `JASYPT_ENCRYPTOR_PASSWORD` | `dev-local-key` | 설정값 암호화 마스터 키 |
| `OKTA_ENABLED` | `false` | Okta SSO 활성화 여부 (DB 설정 없을 때 폴백) |
| `OKTA_CLIENT_ID` | — | Okta 애플리케이션 Client ID |
| `OKTA_ISSUER` | — | Okta 도메인 (예: `https://dev-xxx.okta.com/oauth2/default`) |
| `OKTA_REDIRECT_URI` | `http://localhost/auth/okta/callback` | Okta 리다이렉트 URI |

---

## API 엔드포인트 요약

모든 API는 `/api` prefix. 인증 API 제외 JWT Bearer 토큰 필요.  
전체 스펙은 [docs/api.md](docs/api.md) 참고.

```
# 인증
POST   /api/auth/login
POST   /api/auth/register
GET    /api/auth/me
POST   /api/auth/refresh              # 세션 연장 (새 JWT 발급)

# 앱 설정
GET    /api/public/app-settings       # 전체 설정 조회 (인증 불필요)
PUT    /api/admin/app-settings/{key}  # 설정값 변경 (ADMIN)

# 보안 정책
GET    /api/policies                      # 목록 (status, category, keyword 필터)
POST   /api/policies                      # 생성 (MANAGER+)
GET    /api/policies/:id
PATCH  /api/policies/:id                  # 수정 (MANAGER+)
DELETE /api/policies/:id                  # 삭제 (ADMIN)
POST   /api/policies/:id/acknowledge      # 수신 확인

# 취약점
GET    /api/vulnerabilities               # 목록 (status, severity, keyword 필터)
GET    /api/vulnerabilities/stats         # 상태·심각도별 통계
POST   /api/vulnerabilities
GET    /api/vulnerabilities/:id
PATCH  /api/vulnerabilities/:id           # (MANAGER+)
DELETE /api/vulnerabilities/:id           # (ADMIN)
GET    /api/vulnerabilities/:id/comments
POST   /api/vulnerabilities/:id/comments

# 보안 인시던트
GET    /api/incidents                     # 목록 (keyword, severity, status, type 필터)
POST   /api/incidents
GET    /api/incidents/:id
PATCH  /api/incidents/:id                 # (MANAGER+)
DELETE /api/incidents/:id                 # (ADMIN)

# 자산 관리
GET    /api/assets                        # 목록 (keyword, type, criticality, cloudProvider, environment, active, status 필터)
POST   /api/assets                        # (MANAGER+)
GET    /api/assets/:id
PATCH  /api/assets/:id                    # (MANAGER+)
DELETE /api/assets/:id                    # (ADMIN)
GET    /api/assets/types/stats            # 자산유형별 자산 수 (MANAGER+)
DELETE /api/assets/by-type                # 유형 단위 일괄 삭제 (ADMIN)
GET    /api/assets/snapshots              # 시점(스냅샷) 이력 목록 (MANAGER+)
POST   /api/assets/snapshots              # 현재 자산 목록을 시점으로 저장 (MANAGER+)
GET    /api/assets/snapshots/:id/items    # 특정 시점의 자산 목록 (MANAGER+)
DELETE /api/assets/snapshots/:id          # 시점 이력 삭제 (ADMIN)

# SBOM 관리
GET    /api/sbom/software                 # SW 목록 (keyword 필터, 페이징)
GET    /api/sbom/software/all             # SW 전체 간략 목록 (자산 맵핑용)
GET    /api/sbom/software/:id             # SW 상세 (라이브러리 목록 포함)
POST   /api/sbom/software                 # SW 등록 (MANAGER+)
PATCH  /api/sbom/software/:id             # SW 수정 (MANAGER+)
DELETE /api/sbom/software/:id             # SW 삭제 — 맵핑된 자산은 자동 해제 (MANAGER+)
POST   /api/sbom/software/:id/components  # 라이브러리 추가 (MANAGER+)
PATCH  /api/sbom/components/:id           # 라이브러리 수정 (MANAGER+)
DELETE /api/sbom/components/:id           # 라이브러리 삭제 (MANAGER+)
GET    /api/sbom/software/:id/cyclonedx   # CycloneDX 1.5 JSON 내보내기
POST   /api/sbom/import/cyclonedx         # CycloneDX JSON 가져오기 (MANAGER+)

# 보안이벤트 관리
GET    /api/security-integrations         # 연동 솔루션 목록
POST   /api/security-integrations         # 연동 추가 (MANAGER+)
GET    /api/security-integrations/:id
PATCH  /api/security-integrations/:id     # 연동 수정 (MANAGER+)
DELETE /api/security-integrations/:id     # 연동 삭제 (MANAGER+)
GET    /api/security-integrations/:id/events   # 이벤트 목록 (페이지네이션)
POST   /api/security-integrations/:id/events   # 이벤트 수동 등록 (MANAGER+)
DELETE /api/security-integrations/events/:id   # 이벤트 삭제 (MANAGER+)

# IT 및 정보보호 교육
GET    /api/training/courses
POST   /api/training/courses              # (MANAGER+)
GET    /api/training/courses/:id
POST   /api/training/courses/:id/submit
DELETE /api/training/courses/:id          # (ADMIN)

# 보안 지표 (KPI)
GET    /api/metrics/summary

# 리포트 (MANAGER+)  ?lang=ko|en 파라미터로 한/영 파일명·헤더 자동 전환
GET    /api/reports/vulnerabilities/pdf
GET    /api/reports/training/pdf
GET    /api/reports/policies/pdf
GET    /api/reports/assets/pdf
GET    /api/reports/incidents/pdf
GET    /api/reports/isms/pdf              # ?year=YYYY 필요
GET    /api/reports/policies/csv
GET    /api/reports/vulnerabilities/csv
GET    /api/reports/assets/csv
GET    /api/reports/incidents/csv
GET    /api/reports/users/pdf             # (ADMIN)
GET    /api/reports/users/csv             # (ADMIN)

# 월간 보안점검
GET    /api/monthly-checks               # 목록 (?yearMonth=YYYY-MM)
GET    /api/monthly-checks/summary       # 완료 현황 (?yearMonth=YYYY-MM)
GET    /api/monthly-checks/months        # 기록이 있는 년월 목록
POST   /api/monthly-checks               # 항목 등록
POST   /api/monthly-checks/defaults      # 기본 32개 항목 일괄 등록 (?yearMonth=YYYY-MM)
GET    /api/monthly-checks/previous-month # 점검 내역이 있는 가장 최근 이전 월 (v1.8.0+)
POST   /api/monthly-checks/copy-previous  # 이전 월 항목 구성 복사 (v1.8.0+, ?yearMonth=YYYY-MM)
PATCH  /api/monthly-checks/:id           # 항목 수정
DELETE /api/monthly-checks/:id           # 항목 삭제

# 일괄 등록 (MANAGER+)
GET    /api/assets/bulk/template
POST   /api/assets/bulk
GET    /api/sbom/bulk/template
POST   /api/sbom/bulk
GET    /api/policies/bulk/template
POST   /api/policies/bulk
GET    /api/vulnerabilities/bulk/template
POST   /api/vulnerabilities/bulk
GET    /api/incidents/bulk/template
POST   /api/incidents/bulk

# 사용자 일괄 등록 (ADMIN)
GET    /api/admin/users/bulk/template
POST   /api/admin/users/bulk

# ISMS-P 증적관리
GET    /api/isms/items                    # 항목 목록 (year, domainCode 필터)
GET    /api/isms/items/:id
GET    /api/isms/items/:id/evidences      # 증적 목록 (year 필터)
POST   /api/isms/items/:id/evidences
PATCH  /api/isms/evidences/:id
DELETE /api/isms/evidences/:id
GET    /api/isms/summary
GET    /api/isms/export/csv
GET    /api/isms/import/template
POST   /api/isms/import

# 코드 관리
GET    /api/codes/:groupCode              # 코드값 목록 (인증 불필요 — 회원가입 부서 드롭다운)
GET    /api/admin/codes                   # 코드 그룹 목록 (ADMIN)
POST   /api/admin/codes                   # 코드 그룹 생성 (ADMIN)
PATCH  /api/admin/codes/:groupCode        # 코드 그룹 수정 (ADMIN)
DELETE /api/admin/codes/:groupCode        # 코드 그룹 삭제 (ADMIN)
GET    /api/admin/codes/:groupCode/values
POST   /api/admin/codes/:groupCode/values
PATCH  /api/admin/codes/:groupCode/values/:id
DELETE /api/admin/codes/:groupCode/values/:id

# 보안문서
GET    /api/sec-docs                        # 목록 (category, keyword, page, size)
GET    /api/sec-docs/:id                    # 상세
GET    /api/sec-docs/:id/versions           # 버전 이력
POST   /api/sec-docs                        # 등록 (MANAGER+, multipart)
POST   /api/sec-docs/:id/versions           # 새 버전 추가 (MANAGER+, multipart)
PATCH  /api/sec-docs/:id                    # 메타 수정 (MANAGER+)
DELETE /api/sec-docs/:id                    # 전체 버전 삭제 (MANAGER+)
DELETE /api/sec-docs/:id/version            # 단일 버전 삭제 (MANAGER+)
GET    /api/sec-docs/:id/download           # 파일 다운로드

# RBAC 권한관리 (ADMIN)
GET    /api/auth/my-permissions             # 현재 사용자 유효 권한
GET    /api/admin/roles
POST   /api/admin/roles
PUT    /api/admin/roles/:id
DELETE /api/admin/roles/:id
GET    /api/admin/roles/:id/users           # Role 배정 사용자 목록
POST   /api/admin/roles/:id/users/:userId   # 사용자 배정
DELETE /api/admin/roles/:id/users/:userId   # 사용자 제거

# 관리자 (ADMIN)
GET    /api/admin/users
POST   /api/admin/users
PATCH  /api/admin/users/:id
DELETE /api/admin/users/:id
POST   /api/admin/users/:id/unlock        # 비밀번호 오류 횟수 초기화·잠금 해제 (ADMIN)
GET    /api/admin/audit-logs              # ?dateFrom=&dateTo= (ISO 8601)
GET    /api/admin/users/simple            # 활성 사용자 목록 (담당자 선택용, MANAGER+)
GET    /api/admin/notification-config
PUT    /api/admin/notification-config

# 정보보호위원회 (MANAGER+ 쓰기)
GET    /api/committee/years
GET    /api/committee?year=YYYY
GET    /api/committee/:id
POST   /api/committee
PATCH  /api/committee/:id
DELETE /api/committee/:id
POST   /api/committee/:meetingId/files            # 파일 추가 (multipart)
DELETE /api/committee/files/:fileId
GET    /api/committee/files/:fileId/download

# 내부감사 (MANAGER+ 쓰기)
GET    /api/internal-audit/years
GET    /api/internal-audit?year=YYYY
GET    /api/internal-audit/:id                    # 상세 (targets+items+files)
POST   /api/internal-audit
PATCH  /api/internal-audit/:id
DELETE /api/internal-audit/:id
POST   /api/internal-audit/:auditId/targets
PATCH  /api/internal-audit/targets/:targetId
DELETE /api/internal-audit/targets/:targetId
POST   /api/internal-audit/:auditId/items
PATCH  /api/internal-audit/items/:itemId
DELETE /api/internal-audit/items/:itemId
POST   /api/internal-audit/:auditId/files         # 파일 추가 (multipart)
DELETE /api/internal-audit/files/:fileId
GET    /api/internal-audit/files/:fileId/download

# 보안 결함사항 (MANAGER+ 쓰기)
GET    /api/security-findings/years
GET    /api/security-findings                     # ?year, ?status, ?riskLevel, ?auditType, ?keyword, ?page, ?size
GET    /api/security-findings/:id
POST   /api/security-findings                     # (MANAGER+, multipart)
PATCH  /api/security-findings/:id                 # (MANAGER+, multipart)
DELETE /api/security-findings/:id
GET    /api/security-findings/:id/download

# Okta SSO (v1.54.0+)
GET    /api/auth/okta/config               # Okta 설정 조회 (인증 불필요, enabled 시 반환)
POST   /api/auth/okta/token               # Okta PKCE 코드 교환 → JWT 발급
GET    /api/admin/okta/test               # Okta 연결 테스트 (ADMIN)

# 수탁사 관리 (MANAGER+ 쓰기)
GET    /api/privacy/contractors                          # 수탁사 목록
POST   /api/privacy/contractors                          # 수탁사 등록
PATCH  /api/privacy/contractors/:id                      # 수탁사 수정
DELETE /api/privacy/contractors/:id                      # 수탁사 삭제
POST   /api/privacy/contractors/parse-policy             # 개인정보처리방침 URL 파싱 (v1.8.0+, 미리보기)
POST   /api/privacy/contractors/bulk                     # 수탁사 일괄 등록 (v1.8.0+, 중복 자동 제외)

# 피싱 모의훈련
GET    /api/phishing/send-logs                           # 발송 처리 결과 로그 (v1.8.0+)

# 메일서버(SMTP) 설정 (ADMIN, v1.8.0+)
GET    /api/admin/mail-config                            # 설정 조회 (비밀번호 마스킹)
PUT    /api/admin/mail-config                            # 설정 저장
POST   /api/admin/mail-config/test                       # 연결 테스트

# 수탁사 점검항목 관리 (MANAGER+, v1.52.0+)
GET    /api/privacy/contractor-check-items               # 점검항목 목록
POST   /api/privacy/contractor-check-items               # 점검항목 생성
PATCH  /api/privacy/contractor-check-items/:id           # 점검항목 수정
DELETE /api/privacy/contractor-check-items/:id           # 점검항목 삭제
POST   /api/privacy/contractor-check-items/sync-defaults # 기본항목에서 동기화

# 수탁사 점검 (MANAGER+, v1.52.0+)
GET    /api/privacy/contractors/:contractorId/checks               # 점검 목록 (?year=YYYY)
POST   /api/privacy/contractors/:contractorId/checks               # 점검 생성
GET    /api/privacy/contractors/:contractorId/checks/:checkId      # 점검 상세 (항목+결과)
PATCH  /api/privacy/contractor-checks/:checkId                     # 점검 수정
DELETE /api/privacy/contractor-checks/:checkId                     # 점검 삭제
GET    /api/privacy/contractor-checks/:checkId/results             # 항목별 결과 목록
PUT    /api/privacy/contractor-checks/:checkId/results/:itemId     # 결과 업데이트

# 개인정보보호 — 공통 CRUD 규약 (조회 인증 필요, 쓰기 MANAGER+)
#   GET /  ·  GET /:id  ·  POST /  ·  PATCH /:id  ·  DELETE /:id
/api/privacy/processing            # 개인정보 처리현황
/api/privacy/files                 # 개인정보 파일관리
/api/privacy/consents              # 개인정보 수집·이용 관리
/api/privacy/provisions            # 개인정보 제공관리 (제3자·공동이용·국외이전)
/api/privacy/retentions            # 개인정보 보유기간 관리
/api/privacy/disposals             # 개인정보 파기관리
/api/privacy/dpia                  # 개인정보 영향평가(DPIA)
/api/privacy/breaches              # 개인정보 유출관리
/api/privacy/rights-requests       # 정보주체 권리행사 관리
/api/privacy/safeguards            # 개인정보 보호조치 관리

# 개인정보보호 — 개별 엔드포인트
POST   /api/privacy/consents/:id/versions   # 동의서 개정본 생성 (복사 → DRAFT)
GET    /api/privacy/retentions/expiring     # 만료예정 목록 (?days=30)
GET    /api/privacy/report                  # 개인정보 현황보고서 (9개 영역 집계)

# 개인정보보호 — 보고서
GET    /api/reports/privacy/pdf     # 개인정보 현황보고서 PDF (MANAGER+, ?lang=ko|en)

# 시스템
GET    /api/system/version          # 백엔드 기술스택 버전 (인증 필요 — 환경설정 정보 탭)

# 이메일 승인 (인증 불필요 — 이메일 링크용)
GET    /api/admin/approve/:token
GET    /api/admin/reject/:token
```

---

## 디렉토리 구조

```
secportal/
├── docker-compose.yml
├── .env.example
├── nginx/
│   ├── nginx.conf
│   └── error-pages/             # HTTP 에러 페이지 (400·403·404·429·500·502·503·504)
├── db/
│   └── init/
│       ├── 01_schema.sql            # 전체 테이블 DDL
│       ├── 02_seed.sql              # 기본 관리자 + 샘플 데이터
│       ├── 03_comments.sql
│       ├── 04_assets.sql
│       ├── 05_incidents.sql
│       ├── 06_isms.sql              # ISMS-P 항목 마스터 (101개)
│       ├── 07_extended_schema.sql   # v1.17.0~v1.42.0 추가 테이블 27개
│       ├── 08_extended_seed.sql     # 앱 설정 기본값 (로그인 로고, 세션 타임아웃)
│       └── 09_threat_seed.sql       # 위협 카탈로그 기본 560개 (MRR-0001~MRR-0560)
├── backend/
│   ├── build.gradle
│   └── src/main/java/com/monosun/secportal/
│       ├── SecPortalApplication.java
│       ├── auth/                    # JWT 인증, 사용자 엔티티
│       ├── policy/                  # 보안 정책 관리
│       ├── vulnerability/           # 취약점 관리 + 댓글
│       ├── training/                # 교육 코스 + 퀴즈
│       ├── incident/                # 보안 인시던트
│       ├── asset/                   # IT 자산 관리
│       ├── sbom/                    # SBOM 관리 (SW·라이브러리 구성)
│       ├── security/                # 보안이벤트 관리 (연동·이벤트)
│       ├── admin/                   # 사용자 관리 (ADMIN)
│       ├── audit/                   # 감사 로그
│       ├── isms/                    # ISMS-P 증적관리
│       ├── monthlycheck/            # 월간 보안점검
│       ├── secdoc/                  # 보안문서 관리 (버전 이력)
│       ├── rbac/                    # RBAC 권한관리
│       ├── committee/               # 정보보호위원회 (회의·파일)
│       ├── internalaudit/           # 내부감사 (대상·항목·파일)
│       ├── secfinding/              # 보안 결함사항 (ISMS-P)
│       ├── code/                    # 공통 코드 관리
│       ├── metrics/                 # KPI 집계 API
│       ├── notification/            # 이메일·Slack 알림 + 스케줄러
│       ├── report/                  # PDF/CSV 리포트
│       └── common/                  # 공통 (응답 형식, 예외, 보안 설정)
└── frontend/
    └── src/
        ├── main.js
        ├── api/index.js             # Axios 클라이언트 + 모든 API 함수
        ├── stores/
        │   ├── auth.js              # Pinia 인증 스토어
        │   └── uiSettings.js        # UI 설정 스토어 (테마·폰트·로고)
        ├── router/index.js
        ├── i18n/
        │   ├── ko.json
        │   └── en.json
        ├── components/layout/
        │   ├── AppLayout.vue        # 사이드바 + 네비게이션
        │   └── icons.js
        └── views/
            ├── auth/                # 로그인, 회원가입
            ├── dashboard/           # 대시보드 현황 5종 (위험·취약점·인시던트·이행률·증적)
            ├── policy/              # 정책 목록·상세·폼
            ├── vulnerability/       # 취약점 목록·상세(댓글)·폼
            ├── threat/              # 위협 관리 (카탈로그 CRUD)
            ├── risk/                # 위험평가, 위험처리 계획
            ├── incident/            # 인시던트 목록·상세·폼
            ├── asset/               # 자산 목록·상세·폼
            ├── sbom/                # SBOM 관리 (SW·라이브러리·엑셀 일괄등록)
            ├── security/            # 보안이벤트 관리
            ├── log/                 # 로그 통합관리 (개인정보·AD·NAC·망연계·통합검색)
            ├── training/            # 교육 목록·상세(퀴즈)
            ├── isms/                # ISMS-P 증적관리, 통제항목 매핑
            ├── monthlycheck/        # 월간 보안점검
            ├── secdoc/              # 보안문서 관리
            ├── committee/           # 정보보호위원회
            ├── internalaudit/       # 내부감사
            ├── secfinding/          # 보안 결함사항
            ├── settings/            # UI 환경설정 (반응형 2열 그리드)
            └── admin/               # 사용자·코드·감사 로그·RBAC 관리
```

---

## 데이터베이스 테이블

| 테이블 | 설명 |
|--------|------|
| `users` | 사용자 (ADMIN / MANAGER / USER) |
| `policies` | 보안 정책 |
| `policy_acknowledgments` | 정책 수신 확인 기록 |
| `vulnerabilities` | 취약점 |
| `vulnerability_comments` | 취약점 댓글 |
| `training_courses` | 교육 코스 |
| `quiz_questions` | 퀴즈 문항 |
| `training_completions` | 교육 이수 기록 |
| `audit_logs` | 감사 로그 |
| `assets` | IT 자산 (sbom_software_id로 SBOM SW 맵핑) |
| `sbom_software` | SBOM SW 정보 (name+version UNIQUE) |
| `sbom_components` | SW별 포함 라이브러리 — CycloneDX component 필드 (type·group·name·version·purl·SPDX 라이선스) |
| `incidents` | 보안 인시던트 |
| `security_integrations` | 보안솔루션 연동 정보 |
| `security_events` | 보안 이벤트 수집 기록 |
| `code_groups` | 공통 코드 그룹 |
| `code_values` | 공통 코드값 |
| `notification_config` | 알림 방식 설정 |
| `inbox_messages` | 받은 메시지함 |
| `notices` | 공지사항 |
| `pending_admin_actions` | 이메일 승인 대기 중인 관리자 액션 |
| `isms_items` | ISMS-P 인증 항목 마스터 (101개) |
| `isms_evidences` | 연도별 증적 기록 |
| `monthly_check_items` | 월간 보안점검 항목 (년월·우선순위·점검결과) |
| `monthly_check_evidences` | 월간 보안점검 증적 (파일 첨부) |
| `sec_docs` | 보안문서 (버전 이력, document_key로 그룹화) |
| `custom_roles` | RBAC 커스텀 Role |
| `role_permissions` | Role별 메뉴 권한 (read/write/delete) |
| `user_custom_roles` | 사용자-Role 매핑 |
| `committee_meetings` | 정보보호위원회 회의 (연도·회차·개최일·상태) |
| `committee_files` | 회의 첨부파일 (안건·회의록·기타) |
| `internal_audits` | 내부감사 (연도·기간·담당자·상태) |
| `audit_targets` | 감사 점검대상 |
| `audit_items` | 감사 점검항목 (결과: PASS/FAIL/NA, 발견사항·조치사항) |
| `audit_files` | 감사 첨부파일 (보고서 등) |
| `security_findings` | 보안 결함사항 (ISMS-P 인증기준·위험도·시정조치·처리상태) |
| `threats` | 위협 카탈로그 (유형·카테고리·자산분류·발생가능성·잠재영향·위험점수) |
| `threat_defaults` | 위협 기본항목 마스터 (Master Risk Register 560개, 기본 로드용 참조 테이블, name+type+category UNIQUE) |
| `monthly_check_defaults` | 월간 보안점검 기본 항목 마스터 |
| `risk_assessment_rounds` | 위험평가 차수 (연도·차수번호·평가일·제목·상태) |
| `risk_assessments` | 위험평가 항목 (자산·위협 스냅샷, 발생가능성·영향도·등급·처리방안, 처리계획 필드 plan/plan_assignee/plan_due_date/plan_progress/plan_status) |
| `asset_snapshots` / `asset_snapshot_items` | 자산 시점(스냅샷) 이력 헤더·항목 (저장 시점 자산 목록 보관, v1.7.0) |
| `app_settings` | 앱 전역 설정 키-값 저장소 (로그인 로고·세션 타임아웃 등) |
| `isms_policy_mappings` | ISMS-P 통제항목 × 보안 정책 매핑 (N:N, isms_item_id+policy_id UNIQUE) |
| `privacy_contractors` | 개인정보 처리 수탁사 기본 정보 (재수탁사 `sub_contractor`, v1.8.0) |
| `mail_config` | 발송 메일서버(SMTP) 설정 — 단일 행, ADMIN 전용 (v1.8.0) |
| `contractor_inspections` | 수탁사별 점검 이력 (v1.51.0) |
| `contractor_inspection_files` | 수탁사 점검 증적 파일 (v1.51.0) |
| `contractor_check_item_defaults` | 수탁사 점검항목 기본 템플릿 (20개 초기값, v1.52.0) |
| `contractor_check_items` | 조직 운영 점검항목 목록 (v1.52.0) |
| `contractor_checks` | 연도별·수탁사별 점검 헤더 (v1.52.0) |
| `contractor_check_results` | 점검항목별 결과 (통과/미흡/해당없음/미점검, v1.52.0) |
| `privacy_processing_activities` | 개인정보 처리업무(처리현황) — 목적·항목·보유기간·처리근거·업무흐름도·라이프사이클 |
| `privacy_files` | 개인정보파일대장 — DB 테이블·민감정보/고유식별정보 포함 여부·보유 건수 |
| `privacy_consents` | 수집·이용 동의서 (버전·필수/선택·이용목적·처리근거) |
| `privacy_provisions` | 제3자 제공·공동이용·국외이전 (`provision_type`) |
| `privacy_retentions` | 보유기간 관리 — 만료예정일·삭제예정일·연장사유·자동알림 |
| `privacy_disposals` | 파기계획·승인·이력·증적 (`retention_id`로 보유기간 항목 연계) |
| `privacy_dpia` | 개인정보 영향평가(DPIA) — 체크리스트·위험도·개선계획·완료보고 |
| `privacy_breaches` | 개인정보 유출사고 — 인지일시·신고기한(72h)·통지·관계기관 신고·재발방지 |
| `privacy_rights_requests` | 정보주체 권리행사 — 열람/정정/삭제/처리정지/동의철회, 처리기한(10일) |
| `privacy_safeguards` | 개인정보 보호조치 관리현황 (`safeguard_type` 7종) |

---

## 릴리즈 히스토리

| 버전 | 주요 변경 |
|------|-----------|
| [v1.12.0](release/v1.12.0/RELEASE_NOTES.md) | 개인정보 영향평가(DPIA) **보고서 첨부파일** — 평가 1건에 구분별(결과보고서·개선이행·체크리스트·기타) **다중 첨부**, 문서 제목 지정(미입력 시 파일명), 다운로드·개별 삭제, 목록 **첨부 건수** 컬럼(집계 쿼리 1회), 평가 삭제 시 첨부 레코드·실제 파일 동반 삭제, 신규 테이블 `privacy_dpia_files`(ddl-auto 자동 생성·구분은 VARCHAR로 두어 마이그레이션 불요), `GET/POST /api/privacy/dpia/{id}/files`·`DELETE /api/privacy/dpia/files/{fileId}`·`GET /api/privacy/dpia/files/{fileId}/download` 추가, DPIA 응답에 `fileCount` 필드 추가 |
| [v1.11.1](release/v1.11.1/RELEASE_NOTES.md) | 대시보드 보안·법령 위젯 개선 — 취약점 정보 탭 분리(타이틀 **KRCERT 취약점정보**, 월 단위 조회기간 1주~12개월), 보안공지는 일 단위(1·3·7·14·30일) 유지, RSS 조회기간을 **서버 재조회**로 실제 반영(`GET /api/rss/krcert?days=`, 상한 90→366일)하는 버그픽스, 파비콘을 **설정관리 로고(login_logo)** 로 동적 적용(변경·삭제 즉시 반영) |
| [v1.11.0](release/v1.11.0/RELEASE_NOTES.md) | 설정관리>업종설정 **업종 내 개별 법령 선택**(부분 선택만 저장·법령준수관리/대시보드 반영), 개인정보 수집·이용 **동의서 버전 자동 관리**(버전 읽기전용·최초 1.0·이력 추가 시 자동증가·삭제 시 하위버전 롤백), 관리>메뉴관리 **상위 메뉴 추가·삭제·이름변경·하위 메뉴 상위 간 이동**(사이드바 즉시 반영·하위호환), 대시보드 보안·법령 위젯 **조회기간 선택**(법령 1주~12개월, RSS 1·3·7·14·30일 실제 날짜 필터)·**목록 스크롤**, 인프라 **MySQL 8.0→8.4 LTS 업그레이드**(인플레이스·볼륨 유지, 논리 덤프+볼륨 스냅샷 백업) |
| [v1.10.3](release/v1.10.3/RELEASE_NOTES.md) | 처리현황에서 제공 정보 직접 등록·수정(행 `제공` 버튼·흐름도 `제공 정보 관리`, 현재 처리업무 자동 연계, 국외이전 선택 시 이전 국가 필수, 처리업무 개인정보 항목 가져오기), 개인정보 현황보고서 한 화면 최적화(3열 그리드) 및 **PDF 다운로드**(`GET /api/reports/privacy/pdf`, 화면과 동일 집계 재사용·NanumGothic 임베딩), `GET /api/privacy/provisions?processingId=` 선택 필터 추가 |
| [v1.10.2](release/v1.10.2/RELEASE_NOTES.md) | 개인정보 처리현황 `처리시스템`을 자산관리 연계 콤보박스로 전환(자산유형 SERVICE + 유형 APPLICATION + 사용중 자산 목록에서 선택 또는 직접 입력, 자산명·담당부서 함께 표시), 파일관리 `운영 시스템`에도 동일 목록 적용(흐름 지도의 시스템명 일치 연결이 표기 불일치로 끊기는 것 예방). 스키마·API 변경 없음 |
| [v1.10.1](release/v1.10.1/RELEASE_NOTES.md) | 개인정보 흐름도 신설 — 업무별 흐름도(라이프사이클 해당 단계 강조, `업무흐름도` 텍스트를 `→`·줄바꿈 기준으로 단계 노드 분해, 나가는 제공처), 전체 흐름 지도(처리업무 → 시스템·개인정보파일 → 제공·국외이전; 파일은 시스템명 일치로, 제공처는 연계 처리업무 지정으로 연결), 미연결 개인정보파일·미연결 제공 누락 점검 섹션, 제공관리 `연계 처리업무` 선택 추가(`privacy_provisions.processing_id`, ddl-auto 자동 추가) |
| [v1.10.0](release/v1.10.0/RELEASE_NOTES.md) | 개인정보보호 메뉴 14개 재편(라이프사이클 순), 신규 11개 메뉴 — 처리현황·파일관리(민감정보/고유식별정보)·수집이용(동의서 버전관리·개정본)·제공관리(제3자/공동이용/국외이전)·보유기간(만료 D-day·자동알림)·파기관리(승인·증적·**보유기간 자동 전이**)·DPIA·유출관리(**신고기한 72h 자동산정**)·권리행사(**SLA 10일 자동산정**)·보호조치(7종)·현황보고서(9개 영역 집계), Spring Boot 3.3.5(EOL)→3.5.16·Spring Security 6.5.11·jasypt 4.0.4 보안 업그레이드, nginx 릴리즈 이미지 1.27→1.30-alpine, 환경설정 정보 탭 기술스택 실제 버전 자동 조회(`/api/system/version`) |
| [v1.8.0](release/v1.8.0/RELEASE_NOTES.md) | 개인정보처리방침 URL에서 수탁사·위탁업무·재수탁사 자동 추출·확인·수정 후 일괄등록(중복 자동 제외), 수탁사 재수탁사 항목 추가, 월간점검 이전 월 항목 복사, 피싱 발송 처리 결과 로그(성공/실패·사유), 메일서버(SMTP) 설정 화면(ADMIN·연결 테스트) |
| [v1.7.1](release/v1.7.1/RELEASE_NOTES.md) | 수탁사 점검 건별 이력 보관(연도·수탁사 유니크 제약 제거), 점검항목 기본 템플릿 코드관리 일원화·수탁사 점검항목 관리 메뉴 제거, 월간(32)·수탁사(20) 기본항목 시작 시 자동 시드 |
| [v1.7.0](release/v1.7.0/RELEASE_NOTES.md) | 자산 시점(스냅샷) 이력·자산유형별 현황, 위험 처리 계획(완료 차수 '감소' 항목 연동·차수 스냅샷), 등록/수정/상세 팝업 UX 전면 개편, 사용자 계정 잠금 관리, 위험평가 처리방법 '경감→감소'·일괄처리 4종·처리방법/취약점 필터 |
| [v1.6.0](release/v1.6.0/RELEASE_NOTES.md) | 소스 취약점 점검(SAST) — GitHub 의존성·코드·시크릿 알림 + 내장 OWASP 정적분석(소스코드 직접 점검), 설정관리 API 연동 |
| [v1.5.0](release/v1.5.0/RELEASE_NOTES.md) | 문제은행 분류별 현황·일괄 삭제, 일괄등록 동일 문제 자동 제외·등록 버튼 중복 클릭 방지, 문제은행 가져오기 선택 누락 버그 수정 |
| [v1.4.0](release/v1.4.0/RELEASE_NOTES.md) | 도움말 메뉴(웹 매뉴얼 뷰어), 사용자 추가 비밀번호 보기 토글, 매뉴얼 최신화·스크린샷 전면 갱신 |
| [v1.3.0](release/v1.3.0/RELEASE_NOTES.md) | SBOM CycloneDX 1.5 표준 전환 — JSON 내보내기/가져오기, type·group·PURL·SPDX 라이선스 |
| [v1.2.0](release/v1.2.0/RELEASE_NOTES.md) | SBOM 관리 — SW별 라이브러리 구성 관리, 엑셀 일괄등록, SW 자산 SBOM 맵핑 |
| [v1.1.0](release/v1.1.0/RELEASE_NOTES.md) | 문제은행, 퀴즈 한 문항씩 풀이·오답 리뷰, 교육·훈련 결과 화면 |
| [v1.0.0](release/v1.0.0/RELEASE_NOTES.md) | 최초 릴리즈 — 정보보호 포탈 전 기능 (아래 주요 기능 참조) |

---

## 관련 문서

| 문서 | 설명 |
|------|------|
| [INSTALL.md](INSTALL.md) | 최초 설치 단계별 가이드 |
| [security-guide.md](security-guide.md) | nginx·Spring Boot·Docker 보안 설정 상세 설명, HTTPS 적용 방법 |
| [jmx-setup-guide.md](jmx-setup-guide.md) | JMX 모니터링 활성화 방법 (로컬·운영 서버·SSH 터널·인증) |
| [java-monitoring-setup-guide.md](java-monitoring-setup-guide.md) | java-monitor 대시보드 연동 가이드 |
| [aws-deployment.md](aws-deployment.md) | AWS EC2 배포 가이드 |
| [api.md](api.md) | REST API 전체 명세 |

---

## 기여

PR과 이슈를 환영합니다.

1. `feature/기능명` 브랜치 생성
2. 구현 후 PR 요청
3. 리뷰 후 main 병합

---

## 라이선스

[MIT License](LICENSE) © 2025 Monosun — [한국어 번역본](LICENSE.ko.md)
