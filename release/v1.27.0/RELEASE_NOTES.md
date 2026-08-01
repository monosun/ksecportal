# KSecPortal v1.27.0 릴리즈 노트

**릴리즈 일자**: 2026-08-01

개인정보 항목별 **마스킹 기준** 관리, 서버 저장 백업 파일의 **비밀번호 확인 다운로드(암호 해제 선택)**, 기동 시 **비밀 값(DB 비밀번호 등) 암호화·보호 조치**를 추가했습니다.

---

## 1. 개인정보 유형별 관리 — 항목별 마스킹 기준

**관리 > 코드관리 > 개인정보 유형별 관리**

- 개인정보 항목마다 **마스킹 방식·마스킹 규칙·표시 예시**를 등록할 수 있습니다.
  - 마스킹 방식: `부분 마스킹` / `전체 마스킹` / `암호화 저장` / `일방향 암호화(해시)` / `미수집·즉시 파기` / `마스킹 불필요`
  - 마스킹 규칙: 어느 자리를 어떻게 가릴지에 대한 기준 (최대 300자)
  - 표시 예시: 실제 표기 예 (예: `900101-*******`, `010-****-1234`)
- 목록에 마스킹 방식(색상 배지)·규칙·예시 열이 추가되고, 유형별 **마스킹 기준 지정 현황(n/전체)** 이 표시됩니다.
- 항목 추가·수정 모달의 **권장 기준 불러오기** 버튼으로 항목명에 맞는 권장 마스킹 기준을 자동 입력할 수 있습니다 (주민등록번호·카드번호·이메일·위치정보 등 21종 규칙 매칭).
- 기본 제공 개인정보 항목 **86개**에 개인정보보호법·안전성 확보조치 기준을 반영한 마스킹 기준이 기본값으로 채워집니다.
  - `PiMaskingDefaultInitializer` 가 기동 시 **마스킹 방식이 비어 있는 항목만** 채우므로, 신규 설치·기존 볼륨 모두 자동 적용되고 관리자가 수정한 값은 유지됩니다.

**스키마**: `code_values` 에 `masking_type`(30)·`masking_rule`(300)·`masking_example`(100) 컬럼 추가 (`ddl-auto` 자동 반영, 참고 스크립트 `db/migration/v1.27.0_code_value_masking.sql`).

## 2. 백업 관리 — 서버 저장 파일 비밀번호 확인 다운로드

**관리 > 백업 관리 > 서버 저장 백업 파일**

- 다운로드 시 **백업 생성에 사용한 비밀번호를 입력**해야 하며, 비밀번호가 틀리면 파일이 내려가지 않습니다.
- 받는 형식을 선택할 수 있습니다.
  - **암호화된 원본(.bak)**: 비밀번호만 검증하고 암호화 파일 그대로 다운로드 (복원용)
  - **암호 해제(.json)**: 서버가 복호화·압축해제한 원본 데이터를 JSON 으로 다운로드
- 다운로드 시도는 성공·실패 모두 **백업 이력**에 `DOWNLOAD` 유형으로 기록됩니다.
- 복호화 다운로드 시 평문 데이터 취급 주의 안내를 화면에 표시합니다.

**API 변경**: `GET /api/admin/backup/files/:filename/download` → **`POST`** (본문 `{ password, decrypt }`).
다운로드(blob) 요청 실패 시에도 서버 메시지가 화면에 표시되도록 axios 응답 인터셉터를 보완했습니다.

## 3. 기동 시 비밀 값 보호 조치

- **설정값 암호화 도구** 신설 — 관리 > 설정관리 > 보안 설정에서 비밀 값을 `ENC(...)` 로 암호화하고 복사할 수 있습니다.
- `.env` 의 아래 변수에 `ENC(...)` 값을 넣으면 기동 시 마스터 키로 복호화되어 사용되고, 설정 파일에는 평문이 남지 않습니다.
  - `DB_PASSWORD_ENC` (백엔드 DB 비밀번호) / `MAIL_PASSWORD_ENC` / `JWT_SECRET_ENC`
  - MySQL 컨테이너 초기화용 `DB_PASSWORD` 는 평문 유지가 필요합니다.
- **마스터 키 파일 주입** 지원 — `JASYPT_ENCRYPTOR_PASSWORD_FILE`(docker secret 등)을 지정하면 엔트리포인트가 파일에서 키를 읽어 사용하므로 `docker inspect`·프로세스 환경에 키가 노출되지 않습니다. `SPRING_DATASOURCE_PASSWORD_FILE`·`JWT_SECRET_FILE`·`MAIL_PASSWORD_FILE` 도 동일하게 지원합니다.
- **기동 시 자동 보안점검**(`SecretsStartupCheck`) — 마스터 키가 기본값·16자 미만인지, DB 비밀번호가 평문/샘플 값인지, JWT 시크릿이 기본값·32자 미만인지 점검해 로그에 남깁니다.
  - `SECURITY_FAIL_ON_INSECURE_SECRETS=true` 로 두면 위 상태에서 **기동을 중단**합니다 (운영 권장).

---

## 변경 파일 요약

**백엔드**
- `code/entity/CodeValue`, `code/dto/CodeDto`, `code/service/CodeService` — 마스킹 필드 3종
- `code/service/PiMaskingDefaultInitializer` (신규) — 개인정보 86항목 마스킹 기준 기본값 시더
- `code/service/CodeDataInitializer` — `@Order(0)` (시더 순서 보장)
- `backup/controller/BackupController`, `backup/service/BackupService`, `backup/dto/BackupDto` — 비밀번호 검증 다운로드·복호화 다운로드
- `common/config/SecretsStartupCheck` (신규) — 기동 시 비밀 값 점검

**프론트엔드**
- `views/admin/CodeManagementView.vue` — 마스킹 기준 열·모달·권장 기준 불러오기
- `views/admin/BackupManagementView.vue` — 다운로드 비밀번호/형식 선택 모달
- `views/admin/AdminSettingsView.vue` — 설정값 암호화 도구
- `api/index.js` — `adminToolsApi`, `backupApi.downloadFile(filename, password, decrypt)`, blob 오류 메시지 처리

**인프라·문서**
- `docker-compose.yml` — `*_ENC` 우선 주입, 마스터 키 파일 주입, `SECURITY_FAIL_ON_INSECURE_SECRETS`
- `backend/docker-entrypoint.sh` — `*_FILE` 비밀 값 파일 주입
- `.env.example`, `README.md`, `docs/user-manual.md`, `db/init/07_extended_schema.sql`, `db/migration/v1.27.0_code_value_masking.sql`

## 업그레이드

```bash
docker compose build backend frontend
docker compose up -d backend frontend
```

- 컬럼 추가는 `ddl-auto: update` 로 자동 반영되며, 마스킹 기준 기본값은 기동 시 자동으로 채워집니다.
- 브라우저는 하드 리프레시(Ctrl+Shift+R) 하세요.
