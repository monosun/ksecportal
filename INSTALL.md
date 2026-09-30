# KSecPortal 설치 가이드

Git에서 소스를 받아 처음 설치하고, 이후 버전을 올리는 방법까지 단계별로 안내합니다.
모든 구성 요소는 **Docker Compose** 로 실행되므로 호스트에 Java·Gradle·Node.js 를 설치할 필요가 없습니다.

> 기준 버전: **v1.42.0** · 빠른 요약은 [README — 빠른 시작](README.md#빠른-시작)을 참고하세요.

---

## 사전 요구사항

| 소프트웨어 | 최소 버전 | 확인 명령 |
|-----------|-----------|-----------|
| Docker Desktop (또는 Docker Engine) | 4.x 이상 | `docker --version` |
| Docker Compose | v2 이상 | `docker compose version` |
| Git | 2.x 이상 | `git --version` |

| 자원 | 권장 |
|------|------|
| 메모리 | Docker 에 **4GB 이상** 할당 (빌드 중 멈추면 늘려 주세요) |
| 디스크 | 여유 **10GB 이상** — 이미지만 약 4.5GB(문서 변환 서버 2.6GB · MySQL 1.1GB · 백엔드 0.6GB · 프론트엔드 0.15GB) |
| 포트 | 호스트 **80** (웹), **127.0.0.1:3306** (MySQL, 로컬 디버깅용) |

> **Windows**: Docker Desktop 설치 시 WSL 2 백엔드를 사용하세요. 스크립트는 `start.ps1` / `stop.ps1` 을 씁니다.
> **macOS / Linux**: `start.sh` / `stop.sh` 를 씁니다(최초 1회 `chmod +x start.sh stop.sh`).
> Docker 설치부터 차근차근 따라 하려면 [docs/docker-setup.md](docs/docker-setup.md)를 보세요.

---

## 1단계: 소스코드 내려받기

```bash
git clone https://github.com/monosun/ksecportal.git
cd ksecportal
```

특정 버전으로 설치하려면 태그를 체크아웃합니다: `git checkout v1.42.0`

---

## 2단계: 환경변수 설정

```bash
cp .env.example .env        # Windows PowerShell: copy .env.example .env
```

> `.env` 가 없으면 `start.sh` / `start.ps1` 이 `.env.example` 을 복사해 자동으로 만듭니다.
> **로컬 체험·테스트**는 기본값 그대로도 동작하지만, **운영 환경에서는 아래 "반드시 변경" 항목을 모두 바꾸세요.**

### 반드시 변경 (운영)

| 변수 | 설명 |
|------|------|
| `DB_ROOT_PASSWORD` · `DB_PASSWORD` | MySQL root / 앱 계정 비밀번호. **DB 볼륨을 처음 만들 때만** 적용되므로 설치 전에 정하세요 |
| `JWT_SECRET` | 로그인 토큰 서명 키 — 32자 이상 임의 문자열 |
| `JASYPT_ENCRYPTOR_PASSWORD` | 설정값 암호문 `ENC(...)` 을 푸는 **마스터 키** — 16자 이상. 파일로 주입하려면 `JASYPT_ENCRYPTOR_PASSWORD_FILE` |
| `PI_ENCRYPTION_KEY` | 개인정보 컬럼(AES-256-GCM) 암호화 키. 비우면 마스터 키를 사용합니다. 파일 주입은 `PI_ENCRYPTION_KEY_FILE` |
| `APP_BASE_URL` | 알림 메일 속 링크의 기준 주소 (예: `https://portal.example.com/api`) |
| `CORS_ALLOWED_ORIGINS` | 접속 주소(오리진) 목록, 쉼표 구분 (예: `https://portal.example.com`) |

> ⚠️ **마스터 키와 개인정보 암호화 키는 잃어버리면 복구할 수 없습니다.** 암호화된 개인정보와 `ENC(...)` 설정값을 되살릴 방법이 없으니
> DB 백업과 **분리해서** 보관하세요. 운용 중에 키를 바꾸면 기존 값을 풀 수 없습니다.

### 선택

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `DB_PASSWORD_ENC` · `JWT_SECRET_ENC` · `MAIL_PASSWORD_ENC` | (비어 있음) | 평문 대신 쓸 `ENC(...)` 암호문. 값이 있으면 평문 변수보다 우선합니다 (아래 "비밀 값 암호화") |
| `SECURITY_FAIL_ON_INSECURE_SECRETS` | `false` | `true` 면 기본값·평문 비밀이 남아 있을 때 백엔드 기동을 중단합니다 (**운영 권장**) |
| `PI_COLUMN_ENCRYPTION_ENABLED` | `true` | `false` 면 개인정보를 평문 저장 (권장하지 않음) |
| `MAIL_HOST` · `MAIL_PORT` · `MAIL_USERNAME` · `MAIL_PASSWORD` | `smtp.gmail.com` · `587` | 알림 메일. **587(STARTTLS)만 지원**하며 465(SSL)는 지원하지 않습니다. 비밀번호가 비면 발송을 건너뜁니다. 설치 후 **관리 > 설정관리 > 시스템 설정**의 발송 메일서버(SMTP)에서도 설정할 수 있으며, 그쪽이 켜져 있으면 우선합니다 |
| `DOC_CONVERT_URL` · `DOC_CONVERT_TIMEOUT` · `DOC_CONVERT_MAX_MB` | `http://gotenberg:3000` · `120` · `80` | 보안문서 PPT·워드 미리보기용 변환 서버. URL 을 비우면 변환 미리보기가 꺼집니다 |
| `ERROR_LOG_RETENTION_DAYS` · `ERROR_LOG_PURGE_CRON` | `90` · `0 40 4 * * *` | 관리 > 에러 로그 관리 보관 기간(0 = 자동 정리 끔)과 정리 시각 |
| `JWT_EXPIRATION` | `86400000` | 토큰 유효 시간(ms) |
| `MAX_FILE_SIZE` | `100MB` | 첨부 업로드 한도. 바꾸면 `nginx/nginx.conf` 의 `client_max_body_size` 도 같이 맞추고 frontend 를 재빌드하세요 |

### 비밀 값 암호화 (권장)

설치 후 **관리 > 설정관리 > 보안 설정**의 **설정값 암호화** 도구로 DB 비밀번호·JWT 시크릿·메일 비밀번호를 `ENC(...)` 로 바꾸고,
`.env` 의 `*_ENC` 변수에 넣은 뒤 백엔드를 재기동하면 평문 비밀을 `.env` 에서 지울 수 있습니다.
`ENC(...)` 는 **그 값을 만들 때의 마스터 키**로만 풀리므로, 마스터 키를 먼저 확정한 뒤 만드세요.

---

## 3단계: 빌드 및 실행

```bash
# macOS / Linux
./start.sh --build

# Windows (PowerShell)
.\start.ps1 -Build

# 스크립트 없이 직접
docker compose up -d --build
```

처음에는 이미지 빌드(백엔드 컴파일·프론트엔드 빌드·한글 글꼴을 넣은 문서 변환 서버)로 **10~20분** 정도 걸립니다.
이후 실행은 캐시를 써서 1분 안팎입니다.

| 스크립트 옵션 | 동작 |
|---------------|------|
| `--build` / `-Build` | 이미지를 다시 빌드한 뒤 기동 |
| `--clean` / `-Clean` | **DB 볼륨까지 삭제**하고 새로 설치 (데이터 모두 삭제) |
| `--logs` / `-Logs` | 기동 후 로그를 계속 출력 |

### 컨테이너 구성

| 컨테이너 | 역할 | 외부 포트 |
|----------|------|-----------|
| `secportal-db` | MySQL 8.4 (utf8mb4, Asia/Seoul) | `127.0.0.1:3306` — 로컬 디버깅용 |
| `secportal-backend` | Spring Boot API (context-path `/api`) | 없음 — nginx 가 프록시 |
| `secportal-gotenberg` | 오피스 문서 → PDF 변환 (보안문서 미리보기) | 없음 |
| `secportal-frontend` | nginx — 화면 제공 + `/api/*` → backend 프록시 | `80` |

### DB 초기화 (최초 기동 시 자동)

DB 볼륨이 **비어 있을 때만** `db/init/` 의 SQL 이 파일명 순서대로 실행됩니다.

| 파일 | 내용 |
|------|------|
| `01_schema.sql` | 기본 테이블 (사용자·보안정책·취약점·알림 등) |
| `02_seed.sql` | 기본 관리자 계정 |
| `03_comments.sql` | 취약점 코멘트(타임라인) |
| `04_assets.sql` | 자산 관리 |
| `05_incidents.sql` | 보안 인시던트 |
| `06_isms.sql` | ISMS-P 인증 항목 마스터·증적 |
| `07_extended_schema.sql` | 확장 테이블 35개 (코드·알림·보안문서·RBAC·위원회·내부감사·보안 결함사항·모의훈련·위협·위험평가·개인정보 등) |
| `08_extended_seed.sql` | 앱 설정 기본값 (로그인 로고·세션 타임아웃 등) |
| `09_threat_seed.sql` | 위협 카탈로그 기본 항목 140개 |
| `10_sbom.sql` | SBOM 관리 |
| `11_source_scan.sql` | 소스 취약점 점검(SAST)·GitHub 연동 설정 |
| `12_error_logs.sql` | 에러 로그 관리 |
| `13_isms_defect.sql` | ISMS 결함관리 (결함·연도별 조치 보고서) |

이어서 백엔드가 기동하면서 **비어 있는 기본 데이터를 스스로 채웁니다**(seed-when-empty).
공통 코드, 월간 보안점검·수탁사 점검 기본 항목, 운영현황 기본 항목, 보안용어집, 문제은행, 모의훈련 메일 템플릿,
재해복구 시나리오, 비상연락망 기본 계통, 관련 사이트, RBAC 기본 역할 권한이 여기서 들어갑니다.
그래서 init SQL 에 없는 기본 데이터도 첫 기동 뒤에는 화면에 보입니다.

### 실행 확인

```bash
docker compose ps
```

```
NAME                  STATUS
secportal-db          Up (healthy)
secportal-backend     Up
secportal-gotenberg   Up (healthy)
secportal-frontend    Up
```

```bash
docker compose logs backend --tail 50    # "Started SecPortalApplication" 이 보이면 기동 완료
```

---

## 4단계: 접속

브라우저에서 **http://localhost** 로 접속합니다.

| 항목 | 값 |
|------|----|
| 이메일 | `secportal@monosun.com` |
| 초기 비밀번호 | `Ksecurity!!!` |

최초 로그인하면 **비밀번호 변경 화면이 강제로** 나옵니다.
새 비밀번호는 8자 이상이며 대문자·소문자·숫자·특수문자를 각각 포함해야 합니다.

---

## 5단계: 설치 후 초기 설정 (권장)

| 순서 | 메뉴 | 할 일 |
|------|------|-------|
| 1 | 관리 > 설정관리 | **회사정보** 탭(회사명 — 보고서 표지에 사용), **시스템 설정** 탭(발송 메일서버 SMTP), **API 연동** 탭(법제처 Open API OC 코드 등) |
| 2 | 관리 > 설정관리 > 보안 설정 | 비밀 값을 `ENC(...)` 로 만들어 `.env` 의 `*_ENC` 에 반영 (2단계 참고) |
| 3 | 관리 > 사용자 관리 · 권한관리 | 담당자 계정 생성, 역할(ADMIN/MANAGER/USER)·메뉴 권한 지정. 새 메뉴는 ADMIN 과 기본 MANAGER 역할에 자동 추가되고, USER·사용자 정의 역할은 직접 부여합니다 |
| 4 | 관리 > 백업 관리 | 정기 암호화 백업 스케줄 설정 |

### 운영 환경 보안 점검

- `docker-compose.yml` 의 db `ports: "127.0.0.1:3306:3306"` 줄은 로컬 디버깅용입니다. **운영에서는 제거**하세요.
- `.env` 에 `SECURITY_FAIL_ON_INSECURE_SECRETS=true` 를 두면 기본 비밀이 남은 채로 기동되는 것을 막습니다.
  기동 로그의 `[보안점검]` 경고가 없어야 합니다.
- HTTPS 는 앞단 로드밸런서나 리버스 프록시에서 처리하세요 — [docs/aws-deployment.md](docs/aws-deployment.md) 참고.
- 첨부파일은 `./uploads/`, 서버 백업 파일은 `./backups/` 에 저장됩니다(호스트 폴더). DB 와 함께 백업하세요.

---

## 6단계: 업그레이드

```bash
# 1) DB 백업
docker compose exec -T db mysqldump -usecportal -p<비밀번호> secportal > backup_$(date +%F).sql

# 2) 새 버전 받기
git pull            # 또는 git checkout vX.Y.Z

# 3) 지나친 버전의 마이그레이션을 오래된 순서로 적용 (아래 표)
docker compose exec -T db mysql --default-character-set=utf8mb4 -usecportal -p<비밀번호> secportal \
  < db/migration/<파일명>.sql

# 4) 다시 빌드·기동
docker compose build backend frontend
docker compose up -d
```

배포한 뒤에는 브라우저에서 **하드 새로고침(Ctrl+Shift+R)** 을 해야 새 화면이 보입니다.

> - `ddl-auto: update` 는 **테이블·컬럼 추가만** 자동으로 합니다. 제약·ENUM 변경과 데이터 보정은 마이그레이션을 적용해야 합니다.
> - `db/init` 은 **기존 볼륨에서는 다시 실행되지 않습니다.** 운영 DB 는 항상 `db/migration` 으로 올리세요.
> - 한글이 들어간 SQL 은 `--default-character-set=utf8mb4` 를 꼭 붙이세요. 빠뜨리면 한글이 `?` 로 저장됩니다.
> - 마이그레이션을 되돌리는 스크립트는 없습니다. 롤백이 필요하면 1)의 백업으로 복원하세요.
> - 자세한 주의사항: [README — 운영 배포 시 주의사항](README.md#운영-배포-시-주의사항)

### 버전별 필수 마이그레이션

마이그레이션 **파일명의 버전은 릴리즈 버전과 다를 수 있습니다**(공개 이전의 내부 번호를 그대로 쓴 파일이 있음).
아래 표의 **"필요한 릴리즈"** 기준으로, 현재 버전보다 높은 행을 위에서부터 차례로 적용하세요.
모든 스크립트는 여러 번 실행해도 안전하도록 작성되어 있습니다.

| 필요한 릴리즈 | 마이그레이션 파일 | 내용 |
|--------------|------------------|------|
| v1.7.0 | `v1.7.0_treatment_rename.sql` | 위험 처리방법 '경감' → '감소' 용어 변경 (ENUM) |
| v1.7.1 | `v1.7.1_contractor_check_history.sql` | 수탁사 점검을 건별 이력으로 보관 (유니크 제약 변경) |
| v1.8.0 | `v1.8.0_contractor_sub_contractor.sql` | 수탁사 재수탁사 컬럼 · 위탁업무 길이 확장 |
| v1.24.0 | `v1.24.0_quiz_multi_answer.sql` | 퀴즈 복수 정답 |
| v1.25.0 | `v1.25.0_quiz_option_e.sql` | 퀴즈 보기 E (5지선다) |
| v1.25.0 | `v1.25.0_threat_asset_types.sql` | 위협 카탈로그 대상 자산유형(복수) |
| v1.27.0 | `v1.27.0_code_value_masking.sql` | 개인정보 항목별 마스킹 기준 |
| v1.28.0 | `v1.28.0_pi_column_encryption.sql` | 개인정보 컬럼 암호화 대비 컬럼 길이 확대 |
| v1.30.0 | `v1.55.0_bcp_training.sql` | 재해복구·BCP 훈련 |
| v1.30.0 | `v1.56.0_emergency_contacts.sql` | 비상연락망 |
| v1.31.0 | `v1.31.0_threat_defaults_dedup.sql` | 위협 기본 항목 중복 정리(558 → 140건)·일련번호 제거 |
| v1.33.0 | `v1.32.3_risk_threat_snapshot.sql` | 위험평가 항목에 위협 카테고리·대상 자산유형 스냅샷 |
| v1.34.0 | `v1.57.0_policy_articles.sql` | 보안정책 지침 > 장 > 조 세분화 |
| v1.35.0 | `v1.58.0_isms_article_mapping.sql` | ISMS-P 통제항목 ↔ 정책 매핑을 조 단위로 |
| v1.36.0 | `v1.36.0_isms_evidence_examples.sql` | ISMS-P 항목 예시 증적자료명 |
| v1.39.0 | `v1.39.0_error_logs.sql` | 에러 로그 관리 |
| v1.42.0 | `v1.42.0_isms_defect.sql` | ISMS 결함관리 테이블 |
| v1.42.0 | `v1.42.0_security_finding_source.sql` | 보안 결함사항 출처 구분·기존 행 보정·유니크 제약 |

> `db/migration/` 에 있는 나머지 파일(`v1.26.0_*`, `v1.29.0_*`, `v1.44.0_*` ~ `v1.54.0_*` 등)은 **v1.0.0 공개 이전** 내부 버전용입니다.
> v1.0.0 이후 설치본은 이미 `db/init` 에 반영되어 있으므로 적용할 필요가 없습니다.

각 버전의 자세한 변경 내용은 `release/vX.Y.Z/RELEASE_NOTES.md` 를 참고하세요.

---

## 중지 / 재시작

```bash
./stop.sh                    # 중지 (데이터 보존) — Windows: .\stop.ps1
./start.sh                   # 재시작            — Windows: .\start.ps1

docker compose restart backend       # 백엔드만 재기동 (.env 변경 반영 시에는 up -d 사용)
docker compose logs -f backend       # 로그 실시간 확인
```

---

## 자주 묻는 질문

### 포트 충돌이 발생합니다 (`port is already allocated`)

호스트에 공개하는 포트는 **80**(웹)과 **127.0.0.1:3306**(MySQL)뿐입니다. 이미 쓰고 있는 프로그램을 끄거나 `docker-compose.yml` 에서 바꾸세요.

```yaml
frontend:
  ports:
    - "8888:80"    # http://localhost:8888 으로 접속
```

접속 주소를 바꾸면 `.env` 의 `CORS_ALLOWED_ORIGINS` 에도 새 주소를 넣으세요.

### 백엔드가 시작되지 않습니다

```bash
docker compose logs backend --tail 80
```

| 로그 메시지 | 원인 / 조치 |
|-------------|-------------|
| `Access denied for user` | `.env` 의 DB 비밀번호가 볼륨을 만들 때의 값과 다릅니다. 처음 값으로 되돌리거나, MySQL 에서 비밀번호를 바꾸세요 |
| `Communications link failure` | DB 가 아직 준비 중입니다. 30초 뒤 `docker compose up -d backend` |
| `Failed to bind properties under 'logging.group'` | `ENC(...)` 값과 **마스터 키가 맞지 않습니다**. `JASYPT_ENCRYPTOR_PASSWORD` 를 ENC 를 만들 때의 키로 되돌리거나 `*_ENC` 값을 새로 만드세요 |
| `[보안점검]` 후 기동 중단 | `SECURITY_FAIL_ON_INSECURE_SECRETS=true` 인데 기본 비밀이 남아 있습니다. 2단계 "반드시 변경" 항목을 바꾸세요 |

### 로그인이 되지 않습니다 / 초기 데이터가 없습니다

DB 볼륨이 이전 설치에서 남아 있으면 `db/init` 이 실행되지 않습니다. 새로 설치해도 되는 경우에만 초기화하세요.

```bash
./start.sh --clean --build      # Windows: .\start.ps1 -Clean -Build   (DB 데이터 모두 삭제)
```

### 보안문서 PPT·워드 미리보기가 안 됩니다

`docker compose ps` 에서 `secportal-gotenberg` 가 `Up` 인지 확인하세요. 변환 서버가 없거나 `DOC_CONVERT_URL` 이 비어 있으면
미리보기 대신 "다운로드해 확인" 안내가 나옵니다. `DOC_CONVERT_MAX_MB` 보다 큰 파일도 변환하지 않습니다.

### 이메일 알림이 오지 않습니다

- 포트는 **587(STARTTLS)** 을 쓰세요. 465(SSL)는 지원하지 않습니다.
- Gmail 은 계정 비밀번호가 아니라 **앱 비밀번호**(2단계 인증 활성화 후 발급하는 16자리)를 넣어야 합니다.
- **관리 > 설정관리 > 시스템 설정**에서 SMTP 를 켜 둔 경우 `.env` 의 `MAIL_*` 보다 그 값이 우선합니다.

### 빌드가 중간에 멈춥니다

Docker 메모리가 부족한 경우가 많습니다. Docker Desktop → Settings → Resources → **Memory 4GB 이상**으로 올린 뒤 다시 빌드하세요.

### MySQL 에서 한글이 `?` 로 보입니다

CLI 접속 시 문자셋을 지정하세요. 데이터 자체는 정상입니다.

```bash
docker compose exec -T db mysql --default-character-set=utf8mb4 -usecportal -p<비밀번호> secportal
```

---

## 관련 문서

| 문서 | 내용 |
|------|------|
| [docs/docker-setup.md](docs/docker-setup.md) | 새 PC 에 Docker Desktop 설치부터 실행까지 |
| [docs/development-setup.md](docs/development-setup.md) | 코드 수정·개발 환경 (IDE·핫 리로드) |
| [docs/aws-deployment.md](docs/aws-deployment.md) · [docs/ec2-deploy-setup.md](docs/ec2-deploy-setup.md) | AWS EC2 운영 배포·HTTPS |
| [docs/user-manual.md](docs/user-manual.md) | 사용자 매뉴얼 (화면 안의 도움말과 같음) |

---

## 버전 확인

최신 버전: **v1.42.0** — 릴리즈 노트: [release/v1.42.0/RELEASE_NOTES.md](release/v1.42.0/RELEASE_NOTES.md)

전체 이력: [README.md — 릴리즈 히스토리](README.md#릴리즈-히스토리)
