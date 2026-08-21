# KSecPortal v1.38.0 릴리즈 노트

**릴리즈 일자**: 2026-08-22

보안문서에 올린 **PPT·워드 문서를 내려받지 않고 화면에서 바로** 볼 수 있게 했습니다.
서버가 PDF 로 변환해 보여주며, 한 번 변환한 문서는 다음부터 즉시 열립니다.
더불어 **RSS·법제처(law.go.kr) 연결 오류를 화면에 그대로 표시**하도록 바꿔,
"게시물이 없습니다"·"검색 결과 없음"으로 보이던 접속 장애를 구분할 수 있게 했습니다.

---

## 1. 보안문서 — PPT·워드 미리보기 (서버 PDF 변환)

보안문서 목록에서 파일명을 누르면 뜨는 미리보기 팝업이 **PPT·워드까지 지원**합니다.

| 형식 | 표시 방식 |
|------|----------|
| PDF · 이미지 · 엑셀/CSV · 텍스트 | 기존과 동일하게 화면에서 바로 표시 |
| **PPT (ppt·pptx·pptm·pps·ppsx·ppsm·pot·potx·odp)** | **서버가 PDF 로 변환**해 PDF 와 똑같이 표시 |
| **워드 (doc·docx·odt·rtf)** | **서버가 PDF 로 변환**해 표시 |
| 한글(hwp·hwpx) · ZIP 등 | 미지원 — 안내 문구와 다운로드 버튼 |

- 변환은 **Gotenberg**(LibreOffice 를 감싼 변환 API) 컨테이너가 맡습니다. 백엔드 이미지에 LibreOffice 와
  한글 폰트를 넣으면 이미지가 수백 MB 커지므로 **별도 컨테이너**로 분리했습니다(한글 폰트 포함).
- 변환 결과는 업로드 루트 아래 `preview/<원본경로>.pdf` 로 **캐시**됩니다. 저장 파일명이 UUID 라
  내용이 바뀌지 않으므로 캐시 무효화가 필요 없고, **원본(문서·버전) 삭제 시 캐시도 함께 삭제**됩니다.
- 같은 문서를 여러 사용자가 동시에 열어도 **경로별 잠금**으로 변환은 한 번만 수행합니다.
- 첫 변환은 수십 초가 걸릴 수 있어 `PDF로 변환하는 중...` 을 표시하고, nginx·axios 제한시간을
  이 경로에 한해 **180초**로 늘렸습니다.
- 기본 **80MB** 를 넘는 파일은 변환하지 않고 다운로드 안내만 합니다(`DOC_CONVERT_MAX_MB`).
- `DOC_CONVERT_URL` 을 비우면 변환 미리보기가 꺼지고 기존처럼 다운로드 안내만 표시됩니다.

## 2. 접속 오류를 화면에 표시 — RSS · 법제처

지금까지 외부 연동이 끊기면 화면에는 **"최근 N일간 게시물이 없습니다"**, **"검색 결과가 없습니다"**,
**"Internal server error"** 로만 보여 원인을 알 수 없었습니다. 이제 **사유가 그대로 표시**됩니다.

### RSS (대시보드 보안·법령 정보)

- 응답이 `{ items, errors }` 형태로 바뀌어 **피드별 실패 사유**가 함께 내려옵니다.
  피드 하나가 죽어도 나머지 피드는 정상 표시하고, 실패한 피드만 목록 위에 빨간 줄로 알립니다.
  예) `취약점 정보 접속 오류 — knvd.krcert.or.kr 응답 시간이 초과되었습니다`
- 사유는 예외 종류별로 구분합니다 — **DNS 조회 실패 · 응답 시간 초과 · 연결 거부·차단 · SSL 실패 ·
  HTTP 응답 오류(상태코드) · RSS 형식 해석 실패 · 빈 응답**.

### 법제처 Open API (대시보드 법령 개정 · 법령검토 · 업종 법령 검색)

- 프록시(`/api/law-proxy`)가 실패 사유를 담은 **502 + `{ message }`** 로 응답합니다(기존에는 500).
- **OC 코드가 잘못돼 JSON 대신 오류 HTML 이 오던 경우**를 판별해
  "API 키(OC 코드)를 확인하세요" 로 안내합니다.
- 공용 RestTemplate 에는 제한시간이 없어 law.go.kr 이 응답하지 않으면 요청이 매달렸습니다.
  이 프록시 전용으로 **연결 5초 · 읽기 20초** 제한을 두어 오류가 제때 표시됩니다.
- 화면별 표시: **대시보드 법령 탭**(전부 실패 → 오류 블록, 일부 실패 → `법령 N건을 조회하지 못했습니다 — 사유`),
  **법령검토 팝업**(실시간 조회 실패 시 내장 조문으로 대체했음을 사유와 함께 안내),
  **설정관리 > 업종 설정 법령 검색**(연결 실패를 "검색 결과 없음" 대신 오류 메시지로).

### 문서 변환 실패

- 변환 서버가 거절하면 **응답 코드와 사유**를 안내 문구에 포함합니다.
- 변환 중 예상치 못한 서버 오류가 나도 500 대신 **원인 안내 + 다운로드 버튼**을 보여줍니다.
- 미리보기 팝업의 오류 화면에 경고 아이콘과 형식별 안내(`PPTX 문서를 PDF로 변환하지 못했습니다…`)를 추가했습니다.

## 3. 보안용어집 — 화면 폭에 맞춘 정의 표시

- 용어 카드를 **화면 폭에 따라 1~4단**으로 자동 배치합니다(넓은 모니터에서 오른쪽 여백·과도한 줄 길이 해소).
- 긴 정의는 기본 **4줄**까지 보여 카드 높이를 고르게 맞추고, **더보기/접기**로 개별 확장합니다.
- 검색창 옆 **정의 펼치기/접기** 버튼으로 화면 전체를 한 번에 펼치고 접을 수 있으며,
  **초기화** 는 검색어·분류와 함께 펼침 상태도 되돌립니다.

## 4. ISMS-P 통제항목 매핑 — 조 제목 잘림 개선

- 정책 추가 패널 폭을 **20rem → 30rem** 으로 넓히고, 조 제목·정책 제목·지침 경로를
  자르지 않고 **줄바꿈**해 전부 보이도록 바꿨습니다.

---

## 변경 파일

| 파일 | 변경 |
|------|------|
| `gotenberg/Dockerfile`, `gotenberg/korean-fonts.conf` | **신규** — 한글 폰트를 포함한 문서 변환 컨테이너 |
| `docker-compose.yml` | **신규 서비스 `gotenberg`**(포트 미노출·헬스체크·`--api-timeout=180s`), 백엔드 `DOC_CONVERT_*` 환경변수 |
| `nginx/nginx.conf` | `/api/sec-docs/:id/preview` 전용 location — `proxy_read_timeout 180s` |
| `common/service/DocumentPreviewService.java` | **신규** — 변환 요청·캐시·경로별 잠금·실패 사유 안내 |
| `common/service/FileStorageService.java` | `resolvePath()` 추가 (업로드 루트 밖 경로 거부) |
| `secdoc/controller/SecDocController.java`, `secdoc/service/SecDocService.java` | `GET /api/sec-docs/:id/preview`, 삭제 시 변환 캐시 정리 |
| `rss/dto/RssResultDto.java` | **신규** — `{ items, errors[] }` |
| `rss/service/RssService.java`, `rss/controller/RssController.java` | 피드별 실패 사유 수집·반환 (**응답 형식 변경**) |
| `legal/controller/LawApiProxyController.java` | 실패 사유 502 응답, 비-JSON 응답 판별, 전용 타임아웃 |
| `frontend/src/components/FilePreviewModal.vue` | `pdfLoader` 로 오피스 문서 PDF 미리보기, 오류 안내 보강 |
| `frontend/src/views/secdoc/SecDocListView.vue`, `frontend/src/api/index.js` | 미리보기 PDF 로더 연결(`fetchPreviewPdf`, 180초) |
| `frontend/src/views/dashboard/DashboardView.vue` | RSS·법령 오류/경고 표시 |
| `frontend/src/services/legalApiService.js`, `components/LegalReviewModal.vue`, `views/admin/AdminSettingsView.vue` | 법제처 오류 메시지 전달·표시 |
| `frontend/src/views/glossary/GlossaryView.vue` | 정의 다단 배치·4줄 접기·전체 펼치기 |
| `frontend/src/views/isms/IsmsControlMappingView.vue` | 정책 추가 패널 폭 확대·제목 줄바꿈 |
| `.env.example`, `application.yml` | `DOC_CONVERT_URL` · `DOC_CONVERT_TIMEOUT` · `DOC_CONVERT_MAX_MB` |
| `README.md`, `docs/user-manual.md`, `frontend/public/help/user-manual.md` | 문서 갱신 |

## DB 변경

**없습니다.** 마이그레이션도 필요하지 않습니다.

## 알려진 제약

- **한글 파일(`.hwp`/`.hwpx`)** 은 LibreOffice 변환 품질이 일정하지 않아 미리보기 대상에서 제외했습니다(다운로드 안내).
- 변환본은 원본을 최대한 옮기지만 **특수 글꼴·애니메이션·일부 도형**은 다르게 보일 수 있습니다.
- 첫 변환은 문서 크기에 따라 수십 초가 걸립니다(이후 캐시).
- `gotenberg` 컨테이너가 내려가 있으면 미리보기 대신 **"문서 변환 서버에 연결하지 못했습니다"** 안내가 표시됩니다.

## 업그레이드

```bash
git pull
docker compose build gotenberg backend frontend
docker compose up -d gotenberg backend frontend
```

- **변환 컨테이너(`gotenberg`)가 새로 추가**되어 첫 빌드에 이미지 내려받기·폰트 설치 시간이 필요합니다.
- **RSS 응답 형식이 `{ items, errors }` 로 바뀌었으므로 백엔드와 프론트엔드를 함께 배포**해야 합니다.
- DB 마이그레이션은 없습니다.
- 배포 후 브라우저 **하드 새로고침(Ctrl+Shift+R)** 이 필요합니다.
- 운영 환경에서는 배포 전 **DB 백업**을 먼저 수행하세요.
- 변환 미리보기를 쓰지 않으려면 `DOC_CONVERT_URL` 을 빈 값으로 두면 됩니다(기존 동작 유지).
