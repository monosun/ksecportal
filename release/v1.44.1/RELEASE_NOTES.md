# KSecPortal v1.44.1 릴리즈 노트

**릴리즈 일자**: 2026-11-08

**[보안 수정]** GitHub 코드 스캔(CodeQL)·Dependabot·시크릿 스캔에서 나온 **심각·높음** 항목을 조치하고,
소스코드 점검의 Dependabot 조회 실패와 CodeQL Java 분석 경고를 해결했습니다.

---

## 1. SSRF(서버 측 요청 위조) 차단 — 심각

### 관련 사이트 수집
등록한 사이트 주소·자동 탐색한 피드·`meta refresh`·리다이렉트를 서버가 그대로 따라가, 서버 자신(127.0.0.1)·
내부 컨테이너(`db:3306`, `backend:8080`)·**클라우드 메타데이터(169.254.169.254)** 로 요청을 보낼 수 있었습니다.

- 공용 검사기 `OutboundUrlGuard` 신설 — 접속 전에 호스트가 풀리는 **모든 IP** 를 검사
  - http/https 만 허용, 계정정보(`user:pass@`) 포함 URL 거부
  - **항상 차단**: 루프백, 링크로컬·메타데이터(169.254.x), any-local, 멀티캐스트, IPv4-mapped IPv6 우회
  - **기본 차단**: 사설망 10/8 · 172.16/12 · 192.168/16 · 100.64/10 · fc00::/7
- 자동 리다이렉트를 끄고 **최대 3회까지 홉마다 다시 검사**
- 폐쇄망에서 사내 사이트를 등록해 쓰는 설치는 `.env` 에 `RELATED_SITE_ALLOW_PRIVATE_NETWORK=true`
  (사설망만 허용 — 루프백·메타데이터는 계속 차단)

> 업그레이드 후 사내(사설망) 주소로 등록된 관련 사이트는 수집이 `내부망 주소로의 접속은 허용되지 않습니다` 로 실패합니다.
> 필요한 경우에만 위 설정을 켜세요.

### 법제처 API 프록시
호스트는 law.go.kr 로 고정이지만 `target`·`mst` 값의 `&` 가 인코딩되지 않아 상류 요청에 임의 파라미터를
끼워 넣을 수 있었습니다(예: `target=law&OC=…`).
- `target` 은 `law`/`admrul` 만 허용, `mst` 는 숫자만(아니면 400), 검색어는 1~100자

## 2. ReDoS(정규식 서비스 거부) 제거 — 높음 17건

외부 입력에 적용되는 정규식이 특정 입력에서 백트래킹이 폭증했습니다.
**공백 1,000자짜리 한 줄에 쪽번호 판별 정규식이 152초**가 걸렸으며, 업로드한 정책 문서의 모든 줄에 적용되던 패턴입니다.

- 정책 문서 추출(`PolicyDocumentExtractor`) · 장/조 구조 분석(`PolicyStructureParser`) 패턴을 백트래킹 없는 형태로 재작성
  (소유 수량자, 탐욕 꼬리 + 코드에서 strip, 괄호 제목 `[^)]*+`), 점선·쪽번호 제거는 정규식 대신 역방향 스캔
  — 기존 패턴과 **결과가 같음을 표본으로 검증**, 20만 자 입력도 수 ms
- 소스코드 점검 내장 SAST 규칙(ECB·CORS·yaml.load)의 반복 수량자 범위 제한
- 관련 사이트 문자셋 탐지 패턴 범위 제한
- 비밀번호 강도 검사를 정규식 대신 한 번 훑는 방식으로 변경, **비밀번호 최대 128자** 제한(BCrypt 는 72바이트까지만 사용)

## 3. 프론트엔드 의존성 업데이트 — 심각 1 · 높음 27건 (Dependabot)

| 패키지 | 변경 | 비고 |
|--------|------|------|
| axios | 1.17.0 → 1.21.0 | follow-redirects 1.16.2 · form-data 4.0.6 포함 |
| dompurify | 3.4.8 → 3.4.17 | SAFE_FOR_TEMPLATES 우회(심각) 등 |
| vue | 3.5.35 → 3.5.45 | @vue/server-renderer XSS |
| vue-i18n | 10.0.8 → 11.4.18 | 10.x 는 패치 없음 — 사용 중인 API 영향 없음 |
| xlsx (SheetJS) | 0.18.5 → 0.20.3 | npm 배포본은 0.18.5 에서 중단 → SheetJS 공식 CDN 패키지 사용 |
| vite | 5.4 → 6.4.5 | 빌드 도구 |
| postcss · nanoid · source-map-js · browserslist | 패치 버전 | 빌드 도구 |

## 4. 도움말 화면 HTML 이스케이프 보강 — 높음

매뉴얼 이미지의 `alt`·`src` 속성값에서 따옴표를 이스케이프하지 않던 문제, 목차 슬러그의 불완전한 태그 제거를 수정했습니다.
(최종 HTML 은 이미 DOMPurify 로 정화되고 있어 실제 악용 경로는 없었습니다 — 심층 방어)

## 5. 오탐 처리

- **CSRF 보호 비활성화(CodeQL)** — 인증은 `Authorization: Bearer` 헤더(JWT)로만 하고 쿠키·세션을 쓰지 않아(STATELESS)
  CSRF 가 성립하지 않습니다. 사유를 `SecurityConfig` 에 주석으로 남기고 경고를 오탐으로 종료했습니다.
- **Slack Incoming Webhook URL 노출(시크릿 스캔)** — 문서의 예시 값(`T00000000/B00000000/XXXX…`)이며
  **전체 git 이력에 실제 웹훅은 없었습니다.** 예시를 `<WORKSPACE_ID>/<CHANNEL_ID>/<TOKEN>` 형태로 바꿔 재탐지를 막고 오탐으로 종료했습니다.

## 6. 기타 수정

- **소스코드 점검 — Dependabot 조회 실패 수정** (`HTTP 400: Pagination using the page parameter is not supported`)
  GitHub Dependabot alerts API 가 페이지 번호 방식을 더 이상 받지 않아 첫 요청부터 실패했습니다.
  모든 GitHub 목록 조회를 응답 `Link: rel="next"` 헤더를 따르는 방식으로 변경(커서·페이지 방식 공통, 다른 호스트로는 따라가지 않음).
- **CodeQL Java 분석 경고 해결** — Gradle Wrapper(8.10.2) 추가, `build.gradle` 의 `sourceCompatibility` 를 `java { }` 블록으로 이동
  (Gradle 9 에서 최상위 설정이 제거돼 CodeQL 이 의존성 정보를 추출하지 못했음). Docker 빌드 방식은 그대로입니다.

## 7. 업그레이드

DB 변경 없음(마이그레이션 불필요).

```bash
git pull
docker compose build backend frontend
docker compose up -d backend frontend
```

배포 후 브라우저 강력 새로고침(Ctrl+Shift+R). 폐쇄망 사내 사이트를 관련 사이트로 쓰는 경우 `.env` 에
`RELATED_SITE_ALLOW_PRIVATE_NETWORK=true` 를 추가하세요.

## API

- `GET /api/law-proxy/search` — `target` 은 `law`/`admrul` 외 값이면 `law` 로 처리, 검색어 1~100자 아니면 400
- `GET /api/law-proxy/content` — `mst` 가 숫자가 아니면 400
- 회원가입·비밀번호 변경 — 128자 초과 비밀번호 400
