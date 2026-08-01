# KSecPortal v1.29.0 릴리즈 노트

**릴리즈 일자**: 2026-08-02

## 요약

모의 악성메일 훈련 기능을 대폭 보강했습니다. 훈련용 **기본 템플릿 예제 10종**을 제공하고, 실제 열람·클릭이 전혀 기록되지 않던 **추적 인증 오류를 수정**했으며, 발송 메일서버(SMTP)를 **설정관리 화면에서 직접 구성**할 수 있게 했습니다. 또한 모든 화면에서 쓰는 **전역 새로고침 버튼**을 추가하고, 사용자에게 노출되는 제품명을 **KSecPortal 로 통일**했습니다. 상세 사용 매뉴얼도 별도 문서로 추가했습니다.

## 변경 내용

### 모의 악성메일 훈련

- **기본 템플릿 예제 10종 제공.** IT·HR·FINANCE·DELIVERY·SECURITY·MARKETING·기타 카테고리와 쉬움/보통/어려움 난이도로 구성한 예시 피싱 메일을 기본 제공합니다. 템플릿이 하나도 없을 때만 채우는 **seed-when-empty 초기화기**(`PhishingTemplateInitializer` + `PhishingTemplateDefaults`)로, 신규 설치·구 볼륨·DB 재생성 어디서든 자체 복구됩니다. 각 본문은 인라인 스타일 HTML이며 `{TARGET_NAME}`·`{TARGET_EMAIL}`·`{CLICK_URL}`·`{OPEN_URL}` 치환 변수를 포함합니다.
- **열람/클릭 추적 401 오류 수정 (중요).** 추적 엔드포인트(`GET /api/phishing/track/{token}/open`·`/click`)가 Spring Security 화이트리스트에 없어 `Authentication required(401)` 로 막혀, 수신자의 **열람·클릭이 전혀 기록되지 않던** 문제를 수정했습니다. `SecurityConfig` 에 `/phishing/track/**` GET 공개(permitAll) 규칙을 추가했습니다. 대상자 식별은 추측이 어려운 고유 토큰으로만 이루어집니다.

### 설정관리 — 발송 메일서버(SMTP) 설정

- SMTP 설정 UI가 **라우팅되지 않은 화면**에만 있어 접근할 수 없던 것을, **관리 > 설정관리 > 시스템 설정** 탭으로 이식했습니다. 호스트·포트·계정·비밀번호·발신자·STARTTLS·인증·활성화 설정과 **테스트 메일 발송**을 제공합니다. 저장 시 비밀번호는 마스킹 표시되고, 빈 값이면 기존 유지·`-` 입력 시 삭제됩니다. 백엔드(`MailConfigService`)는 기존 로직을 그대로 사용합니다.
- 이로써 기본 환경변수(예시값)로 폴백해 발생하던 **`Authentication failed`** 발송 오류를 화면에서 바로 해결할 수 있습니다. (Gmail 은 앱 비밀번호 16자리 + 587 포트 사용, 465/SSL 전용 포트는 미지원)

### 전역 새로고침 버튼

- 좌측 **사이드바 프로필 영역**과 **모바일 상단바**에 새로고침 버튼을 추가했습니다. 현재 라우트 컴포넌트를 재마운트해, 브라우저 전체 새로고침 없이 **현재 화면 데이터만 다시 불러옵니다**(열람/클릭 등 비동기로 쌓이는 데이터 즉시 반영에 유용). 일반 페이지 이동의 기존 동작에는 영향이 없습니다.

### 브랜딩 통일 (KSecPortal)

- 피싱 인식 안내 페이지(`phishing-awareness.html`)의 `Powered by SecPortal` 및 프론트엔드 UI(로그인/회원가입 제목, 로고 alt·기본 로고 텍스트, 도움말 부제, 버전 표기, 메일 발신자 placeholder, i18n 로그인 제목)에 남아 있던 **SecPortal → KSecPortal** 로 통일했습니다. 자바 클래스명 `SecPortalApplication` 등 코드 식별자는 변경하지 않았습니다.

### 문서

- 신규 **`docs/phishing-training-manual.md`** — 모의 악성메일 훈련 상세 매뉴얼(사전 준비·템플릿 작성·치환 변수·추적 원리·결과 분석·한계·FAQ·API/DB 참조).
- **`docs/user-manual.md`** 14장에 악성메일 템플릿 작성 가이드(치환 변수, `{CLICK_URL}`/`{OPEN_URL}` 생성 원리, 도메인 설정, HTML 예시) 보강. 인앱 헬프 사본(`frontend/public/help/user-manual.md`) 동기화.

## 업그레이드

```bash
docker compose build backend frontend
docker compose up -d backend frontend
```

- **DB 스키마 변경 없음.** 모의훈련 관련 테이블은 기존 스키마를 사용하며, 기본 템플릿 10종은 `phishing_templates` 가 **비어 있을 때만** 자동 시드됩니다. 기존 템플릿이 있으면 시드되지 않습니다.
- `SecurityConfig` 변경(추적 경로 공개)은 백엔드 재기동으로 반영됩니다.
- 프론트 변경 반영은 이미지 재빌드·배포 후 브라우저 하드 리프레시(Ctrl+Shift+R)가 필요합니다.
