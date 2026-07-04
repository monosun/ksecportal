# SecPortal 최초 설치 가이드

Git에서 받아 처음 설치하는 방법을 단계별로 안내합니다.

---

## 사전 요구사항

| 소프트웨어 | 최소 버전 | 확인 명령 |
|-----------|-----------|-----------|
| Docker Desktop | 4.x 이상 | `docker --version` |
| Docker Compose | v2 이상 | `docker compose version` |
| Git | 2.x 이상 | `git --version` |

> **Windows 사용자**: Docker Desktop 설치 시 WSL 2 백엔드를 활성화하세요.  
> **Mac 사용자**: Docker Desktop for Mac을 설치하면 Compose가 포함됩니다.

---

## 1단계: 소스코드 내려받기

```bash
git clone https://github.com/monosun/secportal.git
cd secportal
```

---

## 2단계: 환경변수 설정 (선택)

기본값으로도 로컬 실행이 가능하지만, 프로덕션 환경에서는 반드시 변경하세요.

```bash
# .env.example 파일을 복사해 .env 생성
cp .env.example .env
```

`.env` 파일 주요 항목:

```env
# DB 비밀번호 (프로덕션에서 반드시 변경)
DB_ROOT_PASSWORD=rootpassword
DB_PASSWORD=secportal123

# JWT 시크릿 키 (최소 32자, 프로덕션에서 반드시 변경)
JWT_SECRET=your-256-bit-secret-key-change-in-production

# 이메일 알림 설정 (선택 — 비어 있으면 이메일 발송 생략)
MAIL_USERNAME=your-gmail@gmail.com
MAIL_PASSWORD=your-gmail-app-password
```

> **이메일 알림 없이 실행**: `MAIL_USERNAME`, `MAIL_PASSWORD` 비워두면 됩니다.

---

## 3단계: 빌드 및 실행

```bash
docker compose up -d --build
```

처음 실행 시 이미지 빌드 때문에 **5~10분** 정도 소요됩니다.

### DB 초기화 파일 실행 순서

`db/init/` 폴더의 SQL 파일이 컨테이너 **최초 기동 시 자동으로** 순서대로 실행됩니다.

| 파일 | 내용 |
|------|------|
| `01_schema.sql` | 기본 테이블 11개 (users, policies, vulnerabilities 등) |
| `02_seed.sql` | 기본 관리자 계정 및 샘플 데이터 |
| `03_comments.sql` | 컬럼 주석 |
| `04_assets.sql` | 자산 관리 테이블 |
| `05_incidents.sql` | 인시던트 관리 테이블 |
| `06_isms.sql` | ISMS-P 이행 관리 테이블 |
| `07_extended_schema.sql` | v1.17.0~v1.42.0 추가 테이블 27개 |
| `08_extended_seed.sql` | 앱 설정 기본값 (로그인 로고, 세션 타임아웃 등) |
| `09_threat_seed.sql` | 위협 카탈로그 기본 항목 560개 (MRR-0001~MRR-0560) |

> **참고**: DB 볼륨이 이미 존재하면 init 파일은 재실행되지 않습니다.  
> 완전히 초기화하려면 `docker compose down -v` 후 다시 실행하세요.

### 실행 확인

```bash
docker compose ps
```

세 컨테이너가 모두 `Up` 상태여야 합니다:

```
NAME                 STATUS
secportal-db         Up (healthy)
secportal-backend    Up
secportal-frontend   Up
```

---

## 4단계: 접속

브라우저에서 **http://localhost** 접속

### 기본 관리자 계정

| 항목 | 값 |
|------|----|
| 이메일 | `secportal@monosun.com` |
| 비밀번호 | `Ksecurity!!!` |

> **최초 로그인 후 반드시 비밀번호를 변경하세요.**  
> 환경설정 → 비밀번호 변경

---

## 5단계: DB 마이그레이션 (버전 업그레이드 시)

최초 설치는 `db/init/` 스크립트가 자동으로 실행됩니다.  
**기존 운영 DB를 버전 업그레이드**할 때는 `db/migration/` 폴더의 SQL을 수동으로 실행하세요.

### v1.26.0 이전 → v1.42.0 전체 마이그레이션

```bash
# 마이그레이션 실행 예시
docker compose exec db mysql -usecportal -psecportal123 secportal \
  < db/migration/<파일명>.sql
```

| 버전 | 마이그레이션 파일 | 주요 내용 |
|------|-----------------|-----------|
| v1.17.0 | `v1.17.0_code_groups.sql` | 코드 그룹/코드 값 테이블 |
| v1.18.0 | `v1.18.0_notifications.sql` | 알림 설정, 받은 메시지함, 공지사항 |
| v1.19.0 | `v1.19.0_security_integrations.sql` | 보안 솔루션 연동, 이벤트 |
| v1.20.0 | `v1.20.0_sec_docs.sql` | 보안 문서 관리 |
| v1.21.0 | `v1.21.0_rbac.sql` | 커스텀 역할, 역할 권한, 사용자-역할 매핑 |
| v1.22.0 | `v1.22.0_committee.sql` | 보안위원회 회의록·첨부파일 |
| v1.23.0 | `v1.23.0_internal_audit.sql` | 내부 감사, 감사 대상·항목·파일 |
| v1.24.0 | `v1.24.0_security_findings.sql` | 지적 사항 관리 |
| v1.25.0 | `v1.25.0_phishing.sql` | 피싱 훈련 템플릿·대상자·캠페인 |
| v1.26.0 | `v1.26.0_threats.sql` | 위협 카탈로그 (사용자/기본 항목) |
| v1.27.0 | `v1.27.0_monthly_check_defaults.sql` | 정보보호의 날 기본 점검 항목 |
| v1.28.0 | `v1.28.0_risk_assessment.sql` | 위험평가 차수·항목 테이블 |
| v1.29.0 | `v1.29.0_mfa.sql` | users 테이블 MFA 컬럼 추가 |
| v1.30.0 | `v1.30.0_sec_docs.sql` | 보안 문서 카테고리 확장 |
| v1.30.1 | `v1.30.1_rbac.sql` | RBAC 권한 세분화 |
| v1.38.0 | `v1.38.0_app_settings.sql` | 앱 설정 테이블 |
| v1.41.0 | `v1.41.0_session_timeout.sql` | 세션 타임아웃 설정값 삽입 |
| v1.41.1 | `v1.41.1_threat_defaults_unique.sql` | 위협 기본 항목 중복 제거·유니크 제약 |
| v1.44.0 | `v1.44.0_isms_policy_mapping.sql` | ISMS-P 통제항목-정책 매핑 테이블 |
| v1.45.0 | `v1.45.0_isms_evidence_ref.sql` | isms_evidences 테이블 source_evidence_id 컬럼 추가 |
| v1.49.0 | `v1.49.0_backup.sql` | backup_history 테이블, 백업 관련 app_settings 키 추가 |
| v1.49.1 | `v1.49.1_risk_asset_environment.sql` | risk_assessments 테이블 asset_environment 컬럼 추가 |
| v1.49.2 | `v1.49.2_risk_asset_environment_backfill.sql` | 기존 위험평가 항목의 asset_environment 값 자산 테이블에서 백필 |

---

## 자주 묻는 질문

### 포트 충돌이 발생합니다

다른 프로그램이 80(HTTP), 3306(MySQL), 8080(Spring Boot) 포트를 사용 중일 때 발생합니다.

`docker-compose.yml`에서 포트를 변경하세요:

```yaml
frontend:
  ports:
    - "8888:80"    # http://localhost:8888 으로 접속
```

### 컨테이너가 시작되지 않습니다

```bash
# 로그 확인
docker compose logs backend
docker compose logs db
```

### 완전히 초기화하고 싶습니다

```bash
# 컨테이너 + 데이터 볼륨 모두 삭제
docker compose down -v

# 다시 시작
docker compose up -d --build
```

> **주의**: `-v` 옵션은 DB 데이터가 모두 삭제됩니다.

### 이메일 알림이 오지 않습니다

Gmail 앱 비밀번호를 사용해야 합니다:
1. Google 계정 → 보안 → 2단계 인증 활성화
2. 앱 비밀번호 생성 (앱: Mail, 기기: Windows/Mac)
3. 생성된 16자리 비밀번호를 `.env`의 `MAIL_PASSWORD`에 입력

---

## 중지 / 재시작

```bash
# 중지 (데이터 보존)
docker compose down

# 재시작
docker compose up -d

# 로그 실시간 확인
docker compose logs -f backend
```

---

## 업그레이드

```bash
git pull

# 필요한 경우 마이그레이션 SQL 실행 (db/migration/ 참고)

docker compose up -d --build
```

---

## 버전 확인

최신 버전: **v1.49.5**  
릴리즈 노트: [release/v1.49.5/RELEASE_NOTES.md](release/v1.49.5/RELEASE_NOTES.md)

전체 이력: [README.md — 릴리즈 히스토리](README.md#릴리즈-히스토리)
