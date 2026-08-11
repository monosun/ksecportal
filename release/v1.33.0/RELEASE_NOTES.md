# KSecPortal v1.33.0 릴리즈 노트

**릴리즈 일자**: 2026-08-12

위험평가 결과에 **위협 카테고리·대상자산유형**을 함께 표시하고, 모의 악성메일 훈련의 **신고 집계**를 실제로 동작하게 만들었습니다.
교육·훈련 결과와 재해복구·BCP 훈련은 **훈련 한 건 단위로 엑셀 내려받기**를 지원하며, 보안문서에서 **대용량 발표자료(PPT) 업로드가 실패하던 문제**를 바로잡았습니다.

---

## 1. 위험평가 — 카테고리·대상자산유형 표시

**정보보호관리체계 > 위험평가**, **위험 처리 계획** 목록에 위협의 **카테고리**와 **대상자산유형** 컬럼을 추가했습니다.

- 두 값은 평가 항목에 연결된 위협의 값을 **평가 시점 스냅샷으로 저장**합니다. 이후 위협을 수정해도 지난 차수 결과는 바뀌지 않습니다(자산명·위협명과 동일한 방식).
- **자산 자동 불러오기**와 **평가 추가/수정** 모두 선택한 위협에서 값을 가져오며, 직접 입력도 가능합니다.
- 대상자산유형은 코드값(`SERVER`) 대신 **한글 라벨(서버)** 로 표시합니다. 같은 화면의 기존 `자산유형` 컬럼과 필터 드롭다운도 한글 라벨로 통일했습니다.
- **엑셀 다운로드**(자산별 시트)에도 카테고리·대상자산유형 열이 포함됩니다.
- 위험 처리 계획의 **계획 수립 팝업** 상단 요약에도 두 값이 표시됩니다.

> **마이그레이션 필요** — `db/migration/v1.32.3_risk_threat_snapshot.sql`
> `risk_assessments` 에 `threat_category` · `threat_asset_types` 컬럼을 추가하고, 기존 평가 항목을 연결된 위협 값으로 백필합니다.

## 2. 모의 악성메일 훈련 — 신고 집계·상태 새로고침

기존에는 `reported_at` 컬럼과 신고 통계 화면만 있고 **신고를 기록하는 경로가 없어 신고 건수가 항상 0** 이었습니다.

- **신고 링크 추적 추가** — `GET /phishing/track/{token}/report` (인증 불필요, 열람·클릭 추적과 동일 구조). 신고 시 **신고·열람 시각을 기록하고 클릭은 기록하지 않습니다.** 신고는 올바른 대응이므로 클릭(실패)과 구분합니다.
- 템플릿 치환 변수 **`{REPORT_URL}`** 추가. 본문에 넣지 않아도 **발송 시 메일 하단에 신고 링크가 자동으로 붙어**, 기존 템플릿을 수정하지 않아도 신고율이 집계됩니다.
- 신고 완료 안내 페이지 **`/phishing-report-thanks.html`** 신설 — "클릭하지 않고 신고한 것이 가장 올바른 대응"임을 안내합니다.
- **모의훈련 현황** 목록의 `발송/열람/클릭` 열을 **`발송/열람/클릭/신고`** 로 확장했습니다.
- **작업** 열의 `결과` 앞에 **새로고침 아이콘** 추가 — 해당 훈련만 다시 읽어 집계·상태를 갱신합니다(결과 팝업이 열려 있으면 함께 갱신).

## 3. 교육·훈련 결과 — 훈련별 엑셀 내려받기

**교육 및 훈련 > 교육·훈련 결과** 세 탭 모두 **훈련 한 건 단위**로 결과를 엑셀(.xlsx)로 내려받습니다. (MANAGER 이상)

| 탭 | 버튼 위치 | 엑셀 구성 |
|----|-----------|-----------|
| 교육 결과 | 코스별 이수 현황의 각 코스 | **[교육 결과]** 개요·이수율·합격률·평균점수 + 이수자별 점수·합격여부·이수일시 / **[퀴즈 문항]** 문항·보기·정답·난이도·해설 |
| 모의훈련 결과 | 캠페인 목록의 다운로드 열 | 훈련 개요·발송/열람/클릭/신고 건수와 비율 + 대상자별 발송 결과·실패 사유·반응 시각 |
| 재해복구·BCP 훈련 결과 | 훈련 실시 이력의 다운로드 열 | 훈련 개요·목표 대비 실제 RTO/RPO·달성률·판정 + 단계별 결과 + 상황 설정·총평·개선사항 |

- **재해복구·BCP 훈련 > 훈련 실시 현황**의 **결과 팝업**에서도 **결과 엑셀 다운로드** 버튼으로 같은 파일을 받을 수 있습니다. 진행 중인 훈련도 내려받을 수 있으며 미기록 단계는 `미수행`으로 표시됩니다.
- 화면에서는 이름이 마스킹되지만 **엑셀에는 실제 이름·이메일이 담깁니다.** 증적 보관·공유 시 취급에 주의하세요.

## 4. 보안문서 — 발표자료(PPT) 업로드 실패 수정

**보안 가이드 및 자료 > 보안문서** 에서 용량이 큰 PPT 자료 업로드가 실패하고, **실패 사유도 표시되지 않던** 문제를 수정했습니다.

원인은 세 가지였습니다.

1. **프론트엔드 요청 타임아웃 30초** — 수십 MB 파일은 전송 도중 요청이 끊겼습니다. 이제 **FormData 요청은 타임아웃을 적용하지 않습니다**(보안문서뿐 아니라 모든 업로드 화면에 적용).
2. **업로드 용량 한도 50MB** — 이미지가 많은 발표자료는 쉽게 초과합니다. 백엔드 multipart·nginx `client_max_body_size` 를 **100MB** 로 상향했습니다(`MAX_FILE_SIZE` 환경변수로 조정 가능).
3. **오류 메시지가 전달되지 않음** — 확장자·용량 거절이 모두 `500 Internal server error` 로 뭉뚱그려지고, 프론트엔드는 5xx를 메시지 없이 버려 사용자에게는 "등록에 실패했습니다."만 보였습니다.

수정 내용:

- 첨부파일 검증 실패는 **400 + 실제 사유**(허용되지 않는 확장자와 허용 목록), 용량 초과는 **413 + 한도 안내**로 응답합니다. 타임아웃·연결 끊김도 원인이 보이도록 안내 문구를 표시합니다.
- 용량 초과 요청도 본문을 끝까지 읽도록 `server.tomcat.max-swallow-size: -1` 을 설정해, 연결이 끊겨 원인 없는 네트워크 오류로만 보이던 현상을 없앴습니다.
- **발표자료 확장자 확대** — 기존 `ppt`·`pptx` 에 더해 **pptm · pps · ppsx · ppsm · pot · potx · odp** 를 허용합니다. 그 밖에 `xlsm` · `odt` · `ods` · `hwp` · `hwpx` 도 추가했습니다.
- 파일 선택 즉시 **형식·용량을 확인해 그 자리에서 사유를 표시**하고, 첨부 항목에 허용 형식·최대 용량 안내를 넣었습니다.

---

## 적용 방법

```bash
# 1) 위험평가 스냅샷 컬럼 추가 + 기존 데이터 백필
docker compose exec -T db mysql -usecportal -psecportal123 secportal < db/migration/v1.32.3_risk_threat_snapshot.sql

# 2) 이미지 재빌드 · 배포 (nginx 설정 변경 포함 — frontend 재빌드 필수)
docker compose build backend frontend
docker compose up -d backend frontend
```

배포 후 브라우저에서 **하드 새로고침(Ctrl+Shift+R)** 이 필요합니다.

### 운영 배포 시 주의사항

- **배포 전 DB 백업이 먼저입니다.** 이번 마이그레이션은 되돌리는 스크립트를 제공하지 않습니다.
  `docker compose exec -T db mysqldump -usecportal -p<비밀번호> secportal > backup_$(date +%F).sql`
- **마이그레이션은 백엔드 재기동 전에 적용하세요.** `ddl-auto: update` 는 컬럼을 추가하지만 **기존 행의 백필은 하지 않습니다.** 마이그레이션 없이 올리면 기존 평가 항목의 카테고리·대상자산유형이 계속 빈 값으로 보입니다.
- **`db/init/*.sql` 은 신규 설치(빈 볼륨)에서만 실행됩니다.** 운영 인스턴스에는 `db/migration` 적용이 필수입니다.
- **frontend 이미지 재빌드 필수** — 이번 릴리즈는 `nginx/nginx.conf`(업로드 용량 100MB·본문 타임아웃)가 바뀌었습니다. nginx 설정은 frontend 이미지에 복사되므로 `up -d` 만 하면 반영되지 않습니다.
- **업로드 한도는 양쪽을 같이 맞추세요.** nginx `client_max_body_size` 와 백엔드 `MAX_FILE_SIZE`(기본 100MB) 중 한쪽만 바꾸면 큰 파일이 413 또는 원인 불명 오류로 실패합니다.
- **기동 확인** — `docker compose logs backend --tail 50` 에서 `Started SecPortalApplication` 확인. 기동 로그의 `[보안점검]` 경고(기본 JWT_SECRET·Jasypt 마스터 키·샘플 DB 비밀번호)가 남아 있으면 운영에서 반드시 교체하세요.
- **롤백 주의** — 이전 태그로 이미지를 되돌려도 **적용된 DB 마이그레이션은 되돌아가지 않습니다.** 추가된 컬럼은 이전 버전에서 무시되어 동작에는 지장이 없지만, 완전한 롤백이 필요하면 백업에서 복원하세요.

## 변경 파일 요약

**백엔드**
- `common/excel/ExcelWriter.java`, `common/excel/ExportSupport.java` — 결과 리포트 엑셀 공통 헬퍼(신규)
- `common/exception/GlobalExceptionHandler.java` — 파일 검증 실패·용량 초과·멀티파트 오류 응답 추가
- `common/service/FileStorageService.java` — 허용 확장자 확대, 오류 메시지 개선
- `risk/entity/RiskAssessment.java`, `risk/dto/RiskAssessmentDto.java`, `risk/service/RiskAssessmentService.java` — 위협 카테고리·대상자산유형 스냅샷, 엑셀 열 추가
- `phishing/controller/PhishingController.java`, `phishing/service/PhishingService.java` — 신고 추적, 메일 신고 링크, 캠페인 엑셀
- `training/controller/TrainingController.java`, `training/service/TrainingService.java` — 교육 결과 엑셀
- `bcp/controller/BcpController.java`, `bcp/service/BcpService.java` — BCP 훈련 결과 엑셀
- `resources/application.yml` — multipart 100MB, `max-swallow-size: -1`

**프론트엔드**
- `views/risk/RiskAssessmentView.vue`, `views/risk/RiskTreatmentView.vue` — 카테고리·대상자산유형 컬럼, 자산유형 한글 라벨
- `views/phishing/PhishingView.vue` — 상태 새로고침, 신고 열, `{REPORT_URL}` 안내
- `views/training/TrainingResultsView.vue`, `views/bcp/BcpTrainingView.vue` — 훈련별 엑셀 내려받기
- `views/secdoc/SecDocListView.vue` — 첨부 형식·용량 사전 검증과 안내
- `components/ExcelDownloadButton.vue` — 공통 다운로드 버튼(신규)
- `api/index.js` — 업로드 요청 타임아웃 해제, 413·네트워크 오류 안내
- `public/phishing-report-thanks.html` — 신고 완료 안내 페이지(신규)

**인프라·문서**
- `nginx/nginx.conf` — `client_max_body_size 100m`, `client_body_timeout 60s`
- `db/migration/v1.32.3_risk_threat_snapshot.sql`, `db/init/07_extended_schema.sql`
- `docs/user-manual.md` (in-app 도움말 동기화)
