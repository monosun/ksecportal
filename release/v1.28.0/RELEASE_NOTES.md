# KSecPortal v1.28.0 릴리즈 노트

**릴리즈 일자**: 2026-08-01

## 요약

개인정보를 **보여주는 단계**와 **저장하는 단계** 양쪽을 보강했습니다.

1. 관리 > 코드관리 > **개인정보 유형별 항목관리**에 등록한 **항목별 마스킹 방식**을 각 메뉴의 **목록 화면 표시에 실제로 적용**합니다. v1.27.0 에서 "기준"만 등록하던 것을 이번 버전부터 화면이 그대로 따르며, 원문 열람(마스킹 해제)은 ADMIN 만 가능하고 해제할 때마다 감사 로그로 남습니다.
2. 정보주체·수탁사 담당자 정보 **6개 컬럼을 AES-256-GCM 으로 암호화해 저장**합니다. 검색·정렬·로그인에 쓰이지 않는 컬럼만 골라 기능 영향 없이 적용했습니다.

## 주요 변경

### 1. 목록 화면 개인정보 자동 마스킹

| 화면 | 마스킹되는 항목 |
|------|----------------|
| 개인정보 > 정보주체 권리행사 | 정보주체명, 연락처 |
| 개인정보 > 수탁사 관리 | 사업자등록번호, 대표자, 담당자, 이메일, 연락처 |
| 관리 > 사용자 관리 | 이름, 이메일 |
| 관리 > 감사 로그 | 사용자명, 접속 IP |
| 관리 > 성능 관리 | 요청 IP |
| 교육·훈련 > 교육·훈련 결과 | 이수자 이름 |
| 모의 피싱훈련 | 대상자 이름·이메일, 발송 이력·훈련 결과의 수신자 |

- 항목의 **마스킹 방식**에 따라 표시가 달라집니다.
  - **부분 마스킹 / 암호화 저장** — 항목 종류에 맞춰 일부만 노출 (`홍*동`, `010-****-5678`, `abc****@test.com`, `192.168.*.*`, `123-**-*****`)
  - **전체 마스킹 / 미수집·즉시 파기** — `(비노출)`
  - **일방향 암호화(해시)** — 앞 4자리만 노출
  - **마스킹 불필요** — 원문 표시
- 기준이 등록되지 않은 항목은 **안전하게 부분 마스킹**으로 처리합니다.
- 마스킹된 값에 마우스를 올리면 적용된 기준(항목명·방식·규칙)이 툴팁으로 표시됩니다.

### 2. 마스킹 해제(원문 열람) — ADMIN 전용 · 감사 로그 기록

- 화면 상단에 **개인정보 마스킹 적용** 상태 표시가 나타나고, ADMIN 에게만 **마스킹 해제** 버튼이 보입니다.
- 해제하는 순간 감사 로그에 `PI_UNMASK` 로 기록됩니다(화면명·열람 사유 포함).
- 다시 **마스킹 적용**을 누르거나 로그아웃·새로고침하면 마스킹 상태로 돌아갑니다.
- 등록·수정 모달과 발송 대상 선택 목록은 업무 처리(동명이인 구분 등)를 위해 원문을 유지합니다.

### 3. 개인정보 저장 컬럼 암호화 (AES-256-GCM)

| 테이블 | 암호화 컬럼 |
|--------|------------|
| `privacy_rights_requests` | `subject_name`, `contact` |
| `privacy_contractors` | `business_number`, `contact_person`, `contact_email`, `contact_phone` |

- 저장 형식 `PIENC1:` + Base64(IV(12B) ‖ 암호문 ‖ 인증태그). 접두어가 없는 값은 평문으로 보고 그대로 통과하므로 백필 중이나 예전 백업 복원 직후에도 화면이 정상 동작합니다.
- 키는 `PI_ENCRYPTION_KEY`(미설정 시 Jasypt 마스터 키)의 SHA-256. `PI_ENCRYPTION_KEY_FILE` 로 파일 주입도 지원합니다.
- **기동 시 남아 있는 평문을 자동 암호화**합니다(`PiColumnEncryptionBackfill`). 이미 암호문이면 건너뛰므로 매 기동 반복 실행해도 안전합니다.
- 백업(JSON+gzip)에는 암호문 그대로 담겨 백업 파일 유출에도 안전한 반면, **복원 대상 인스턴스에 같은 키가 없으면 복호화되지 않습니다.**
- `users.email`·`users.name`·`audit_logs.ip_address` 는 로그인 조회 키·정렬·대량 로그라 **암호화 대상에서 제외**하고 화면 마스킹으로 보호합니다.
- 상세 검토 내용은 [docs/pi-db-encryption-impact.md](../../docs/pi-db-encryption-impact.md) 를 참고하세요.

> **주의**: 키를 잃으면 암호화된 개인정보는 복구할 수 없습니다. 키는 백업본과 분리해 보관하세요.

## 기술 변경

**백엔드**

- `GET /api/codes/pi-masking` — 활성 `PI_*` 항목 중 마스킹 방식이 등록된 기준 목록 (인증된 모든 사용자)
- `POST /api/codes/pi-masking/reveal` *(ADMIN)* — 마스킹 해제 감사 로그(`PI_UNMASK`) 기록, `204 No Content`
- `CodeDto.MaskingRule`, `CodeService.listPiMaskingRules()`, `CodeValueRepository.findByGroupCodeStartingWithAndActiveTrueOrderByGroupCodeAscSortOrderAsc()` 추가

**프론트엔드**

- `src/utils/piMasking.js` — 항목 종류별 마스킹 알고리즘(성명·생년월일·주민번호·여권·면허·사업자번호·전화·이메일·주소·우편번호·계좌·카드·IP·단말식별번호·MAC·좌표·해시 등)과 항목명 → 종류 매칭
- `src/stores/piMasking.js` — 기준 1회 로드·캐시, `mask(alias, value)` / `isMasked` / `ruleText` / `toggleReveal`, 권한이 없으면 해제 상태가 남아 있어도 다시 마스킹
- `src/components/privacy/PiMaskToggle.vue` — 마스킹 상태 표시 + ADMIN 전용 해제 토글
- `PrivacyCrudView` 컬럼에 `pi: '<항목 별칭>'` 옵션 추가 — 개인정보 컬럼을 선언하면 자동으로 마스킹·툴팁·상단 토글이 적용됩니다.

**암호화**

- `common/crypto/ColumnCipher`(AES-256-GCM), `EncryptedStringConverter`(JPA `AttributeConverter`), `ColumnCipherConfig`(설정 주입), `PiColumnEncryptionBackfill`(멱등 백필)
- `application.yml` 에 `app.privacy.column-encryption.enabled/key`, docker-compose·`.env.example` 에 `PI_ENCRYPTION_KEY`·`PI_ENCRYPTION_KEY_FILE`·`PI_COLUMN_ENCRYPTION_ENABLED` 추가
- 기동 보안점검에 개인정보 암호화 비활성 경고·전용 키 사용 안내 추가

## 업그레이드

**1) 컬럼 길이 확대 마이그레이션 (기존 DB 필수)**

암호문은 원문보다 길어 `ddl-auto: update` 로는 컬럼이 늘어나지 않습니다. 실행하지 않으면 해당 화면의 저장이 실패합니다.

```bash
docker compose exec -T db mysql -usecportal -psecportal123 secportal < db/migration/v1.28.0_pi_column_encryption.sql
```

**2) 빌드·배포**

```bash
docker compose build backend frontend
docker compose up -d backend frontend
```

재기동 시 남아 있던 평문이 자동으로 암호화되며, 로그에 `[개인정보 암호화] 평문으로 남아 있던 개인정보 n건을 암호화했습니다.` 가 남습니다. 배포 후 브라우저 하드 리프레시(Ctrl+Shift+R)가 필요합니다.

**3) (권장) 전용 암호화 키 설정**

`.env` 에 `PI_ENCRYPTION_KEY` 를 지정하면 Jasypt 마스터 키 교체와 분리할 수 있습니다. **단, 이미 암호화된 데이터가 있는 상태에서 키를 바꾸면 기존 값을 복호화할 수 없습니다.**
