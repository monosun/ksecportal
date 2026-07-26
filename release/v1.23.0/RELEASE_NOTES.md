# v1.23.0 릴리즈 노트

**릴리즈 일자**: 2026-07-27

**보안 가이드 및 자료 > 관련 사이트** 메뉴를 신설했습니다(보안용어집 아래).
보안·개인정보 업무에서 자주 찾는 외부 사이트를 등록해 두고, **각 사이트의 최신 게시물을 포털 안에서 모아 봅니다**.

## 신규 기능

### 보안 가이드 및 자료 > 관련 사이트

사이트마다 카드 하나로 표시되며, 카드 안에 그 사이트에서 가져온 **최신 게시물 최대 5건**이 함께 나옵니다.

| 영역 | 내용 |
|------|------|
| 카드 머리 | 사이트 이름(새 탭 이동) · 분류 배지 · 도메인 · 설명 |
| 본문 | 최신 게시물(제목 · 요약 · 게시일). 게시물 목록이 없는 사이트는 **사이트 소개 내용**을 대신 표시 |
| 카드 발 | 수집 상태(수집됨 / 소개만 / 실패)와 마지막 수집 시각 · 사이트 바로가기 |

- **통합 검색** — 사이트 이름·주소·설명뿐 아니라 **가져온 게시물 제목·요약까지** 함께 찾습니다.
- **분류 필터** — 분류 버튼에 사이트 수가 표시됩니다.
- **미사용 포함** — 사용 해제한 사이트도 꺼내 볼 수 있습니다(MANAGER 이상).

### 내용 수집 방식

| 단계 | 동작 |
|------|------|
| 1 | 등록된 **RSS/Atom 피드**에서 최신 게시물을 최대 5건 가져옵니다 |
| 2 | 피드 주소를 비워두면 홈페이지의 `<link rel="alternate">` 에서 **피드를 자동 탐색**합니다 |
| 3 | 피드가 없으면 홈페이지의 **og:description / meta description** 을 사이트 소개문으로 가져옵니다 |

- 내용 없이 `<meta http-equiv="refresh">` 로만 넘기는 첫 화면(국내 기관 사이트에 흔함)은 **한 번 따라갑니다**.
- 응답 헤더에 charset 이 없으면 XML 선언·meta 태그에서 인코딩을 찾아 **EUC-KR 피드도 깨지지 않게** 읽습니다.
- 게시일은 RFC-1123 · 두 자리 연도 RFC-822 · ISO-8601 · `dc:date` · `YYYY-MM-DD` 형식을 인식합니다.
- **매일 새벽 07:10 자동 수집**(`RELATED_SITE_CRON` 으로 변경). 화면에서 **전체 새로고침**·카드별 **새로고침**으로 즉시 다시 가져올 수 있습니다.
- 외부 접속이 막힌 환경에서는 수집이 실패해도 화면은 정상 동작하며, **링크와 실패 사유**만 표시됩니다. 이전에 가져온 게시물은 지우지 않고 그대로 둡니다.

### 기본 제공 사이트 (15개)

| 분류 | 사이트 |
|------|--------|
| 침해사고·취약점 | KISA 인터넷 보호나라&KrCERT, KNVD 보안공지, KNVD 취약점 정보, 미국 CISA 보안 권고, NVD |
| 유관기관 | 한국인터넷진흥원(KISA), 개인정보보호위원회, 개인정보 포털, 국가사이버안보센터, 금융보안원 |
| 법령·인증 | 국가법령정보센터, 개인정보보호위원회 고시·해설서 |
| 보안 동향 | 보안뉴스, 데일리시큐, OWASP |

> 목록이 **비어 있을 때만** 기본 사이트를 다시 채우므로, 쓰지 않는 사이트는 삭제보다 **사용 해제**를 권장합니다.

### 사이트 등록·수정·삭제 (MANAGER 이상)

사이트 이름 · 홈페이지 주소 · 게시물 목록(RSS/Atom) 주소 · 분류 · 정렬 순서 · 설명 · 사용 여부를 관리합니다.

- 주소는 `http://` 를 생략하면 `https://` 로 저장하며, **같은 주소는 중복 등록되지 않습니다**.
- 등록·주소 변경 직후 해당 사이트의 내용을 한 번 가져옵니다(수집 실패해도 등록은 유지).
- 삭제하면 가져온 게시물도 함께 지워집니다.

## 변경 사항

### 백엔드

- 신규 도메인 `relatedsite` — `RelatedSiteController` · `RelatedSiteService` · `SiteContentFetcher`(피드·HTML 파싱) · `RelatedSiteInitializer`(seed-when-empty) · `RelatedSiteScheduler`(일 1회 수집)
- 신규 테이블 `related_sites` · `related_site_items` (`ddl-auto: update` 로 자동 생성)
- 신규 API

```
GET    /api/related-sites            # 사이트 목록 + 가져온 게시물 (?keyword=&category=&activeOnly=)
GET    /api/related-sites/:id        # 사이트 1건
POST   /api/related-sites            # 등록 (MANAGER+)
PATCH  /api/related-sites/:id        # 수정 (MANAGER+)
DELETE /api/related-sites/:id        # 삭제 (MANAGER+)
POST   /api/related-sites/:id/refresh # 사이트 1건 새로고침 (MANAGER+)
POST   /api/related-sites/refresh     # 전체 새로고침 (MANAGER+)
```

- 신규 설정 `relatedsite.refresh-cron` (기본 `0 10 7 * * *`, 환경변수 `RELATED_SITE_CRON`)
- XML 파싱은 DOCTYPE·외부 엔티티를 모두 차단(XXE 방어), 등록 주소는 `http/https` 만 허용

### 프론트엔드

- 신규 화면 `views/relatedsite/RelatedSiteView.vue` · `RelatedSiteFormModal.vue`, 라우트 `/related-sites`
- 좌측 메뉴 **보안 가이드 및 자료 > 관련 사이트**(보안용어집 아래), i18n `nav.relatedSites`
- RBAC 메뉴 키 **`related_sites`** 추가 (전체 45개) — `navMenu.js` · `MenuKeys.java` · `RbacManagementView.vue` 동시 반영

## 업그레이드 안내

- 별도 마이그레이션은 필요 없습니다. 백엔드 기동 시 테이블이 생성되고 기본 사이트 15개가 자동 등록됩니다.
- 신규 메뉴이므로 **관리 > 권한관리**에서 역할별 `관련 사이트` 권한을 확인해 주세요.
- 화면 변경 반영을 위해 배포 후 브라우저 **하드 리프레시(Ctrl+Shift+R)** 가 필요합니다.
