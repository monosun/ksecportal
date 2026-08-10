# KSecPortal v1.32.1 릴리즈 노트

**릴리즈 일자**: 2026-08-10

법제처 Open API 키에 **연결 테스트** 기능을 추가하고, 설정값 조회 API가 인증 없이 전체 노출되던 문제를 고쳤습니다.

---

## 신규 기능

### 법제처 Open API 연결 테스트 (관리 > 설정관리 > API 연동)

- 대시보드 **법령 개정** 위젯에 정보가 나타나지 않을 때, 원인이 "OC 코드가 잘못됨"인지 "기간 내 개정 이력이 실제로 없음"인지 구분할 수 없던 문제 대응.
- API 키 등록 후 **연결 테스트** 버튼으로 law.go.kr에 실제 검색 요청(`대한민국헌법`)을 보내 OC 코드 유효성을 바로 확인(`POST /api/law-proxy/test`, ADMIN). GitHub 연동 테스트와 동일한 UX(테스트 중 표시, 성공/실패 메시지 색상 구분).

## 보안 수정

### 설정값 공개 API의 민감정보 노출 차단

- `GET /api/public/app-settings`(로그인 화면 로고 표시용으로 인증 없이 열려 있던 엔드포인트)가 `lawApiKey`를 포함한 **전체 설정값을 그대로 반환**하고 있어, 로그인 없이도 등록된 법제처 API 키 등을 조회할 수 있었습니다.
- `/public/app-settings`는 로그인 화면에 실제로 필요한 `login_logo`·`login_logo_text`·`session_timeout_minutes`·`menu_order` 화이트리스트만 반환하도록 제한.
- 로그인 후 화면(설정관리·대시보드 등)이 쓰던 전체 설정값 조회는 신규 인증 엔드포인트 `GET /api/app-settings`(로그인만 되어 있으면 접근 가능)로 이전. 프론트엔드 8개 화면(설정관리·대시보드·보안 설정·시스템 설정·법령준수관리·법령검토 모달)이 대상.

## 업그레이드

```bash
docker compose build backend frontend
docker compose up -d backend frontend
```

DB 스키마 변경 없음.
