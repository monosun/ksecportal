# scripts/ 사용 안내

KSecPortal 운영·개발 보조 스크립트 모음입니다. **파이썬 스크립트는 저장소 루트에서 실행**하세요(입출력 경로가 루트 기준 상대경로입니다).
파이썬 스크립트는 표준 라이브러리만 사용합니다(Python 3.8 이상).

| 분류 | 파일 | 용도 |
|------|------|------|
| 배포·운영 | [`server-setup.sh`](#server-setupsh--새-서버-최초-세팅) | 새 Ubuntu 서버(EC2) 최초 1회 세팅 |
| 배포·운영 | [`build-release.sh`](#build-releasesh--ghcr-이미지-릴리즈-패키지) | 이미지 빌드·ghcr.io push·배포 아카이브 생성 |
| 기본 데이터 | [`dump_xlsx.py`](#dump_xlsxpy--엑셀-내용-텍스트-덤프) | 엑셀(xlsx) 내용을 텍스트로 확인 |
| 기본 데이터 | [`gen_glossary_defaults.py`](#gen_glossary_defaultspy--보안용어집-기본-데이터) | 보안용어집 기본 데이터 Java 상수 생성 |
| 기본 데이터 | [`gen_opstatus_defaults.py`](#gen_opstatus_defaultspy--운영현황관리-기본-항목) | 운영현황관리 기본 항목 Java 상수 생성 |
| 일회성 | [`rename_secportal_docs.py`](#제품명-변경-스크립트-일회성) | 문서의 제품명 `SecPortal` → `KSecPortal` |
| 일회성 | [`rename_subject_prefix.py`](#제품명-변경-스크립트-일회성) | 알림 제목 접두어 `[SecPortal]` → `[KSecPortal]` |

---

## 1. 배포·운영

### `server-setup.sh` — 새 서버 최초 세팅

```bash
sudo bash scripts/server-setup.sh
# 또는 서버에서 바로 실행
sudo bash <(curl -fsSL https://raw.githubusercontent.com/monosun/ksecportal/main/scripts/server-setup.sh)
```

- **root 권한 필요**, 시작 시 `y` 확인. Ubuntu 22.04 / 24.04 LTS 기준
- 수행 단계 (7단계)
  1. 시스템 업데이트·기본 패키지 설치
  2. 타임존 `Asia/Seoul`
  3. 스왑 메모리 생성(기본 2G)
  4. Docker 설치·서비스 등록
  5. 소스코드 클론
  6. `.env` 생성(`.env.example` 복사, JWT 비밀키 자동 생성) — 이미 있으면 건너뜀
  7. 매일 02:00 DB 자동 백업 cron 등록(`/etc/cron.d/secportal-backup`) — 이미 있으면 건너뜀
- 환경변수로 기본값 변경

  | 변수 | 기본값 |
  |------|--------|
  | `REPO_URL` | `https://github.com/monosun/ksecportal.git` |
  | `DEPLOY_DIR` | `/home/ubuntu/secportal` |
  | `SWAP_SIZE` | `2G` |

  ```bash
  sudo DEPLOY_DIR=/opt/ksecportal SWAP_SIZE=4G bash scripts/server-setup.sh
  ```
- 완료 후 `.env` 를 열어 DB 비밀번호 등 값을 채우고 `docker compose up -d` 로 기동합니다.

### `build-release.sh` — ghcr 이미지 릴리즈 패키지

소스코드 없이 **이미지만 받아 실행하는 서버**용 배포 패키지를 만듭니다.

```bash
bash scripts/build-release.sh v1.45.1 monosun              # <버전> <GitHub 조직/사용자>
bash scripts/build-release.sh v1.45.1 monosun --no-push    # 빌드·패키지만(레지스트리 업로드 없이 점검)
bash scripts/build-release.sh v1.45.1 monosun --skip-build # 이미 빌드한 로컬 이미지로 push·패키지
```

- 인자를 생략하면 버전·owner(기본 `monosun`)를 묻습니다. `backend/build.gradle` 버전과 다르면 확인을 받습니다.
- 수행 단계
  1. `dist/release/<버전>/` 에 배포 패키지 구성
     - `docker-compose.yml` — 루트 compose 에서 `build:` 를 `image: ghcr.io/<owner>/secportal-<서비스>:${VERSION}` 로 바꾸고, 디버깅용 DB 포트(127.0.0.1:3306) 노출을 제거
     - `db/init/*.sql`, `.env.example`(루트 것 + `GITHUB_OWNER`·`VERSION`), `release/<버전>/RELEASE_NOTES.md`, `QUICKSTART.md`, `VERSION`
  2. backend · frontend · gotenberg 이미지 빌드 — **운영 compose 와 같은 Dockerfile** 사용(frontend 는 nginx 보안·속도제한 설정과 도움말 문서 포함)
  3. `ghcr.io/<owner>/secportal-{backend,frontend,gotenberg}` 의 `:<버전>`·`:latest` push (**ghcr.io 로그인 필요** — PAT `write:packages`)
  4. `dist/secportal-<버전>.tar.gz` 생성 (`dist/` 는 git 추적 제외)
- 배포 서버에서

  ```bash
  tar -xzf secportal-v1.45.1.tar.gz && cd v1.45.1
  cp .env.example .env && nano .env      # 비밀번호·키·CORS·APP_BASE_URL 설정
  docker compose pull && docker compose up -d
  ```

  업그레이드는 `.env` 의 `VERSION` 을 바꾸고 `docker compose pull && docker compose up -d`. 아카이브는 `gh release upload <버전> dist/secportal-<버전>.tar.gz` 로 GitHub 릴리즈에 첨부할 수 있습니다.
- 일반 릴리즈 절차(`docker compose build` → git 태그 → `gh release`, 루트 `CLAUDE.md` 참고)와는 **별개**이며, 이미지 배포 서버를 운영할 때만 필요합니다.

---

## 2. 기본 데이터 생성 (엑셀 → Java 상수)

화면의 기본 데이터는 Java 상수로 들고 있다가 **테이블이 비어 있을 때만** 기동 시 채워집니다(seed-when-empty).
원본 엑셀이 바뀌면 아래 스크립트로 Java 파일을 다시 만들고 backend 이미지를 재빌드하세요. 이미 데이터가 있는 DB 에는 자동 반영되지 않습니다.

### `dump_xlsx.py` — 엑셀 내용 텍스트 덤프

```bash
python scripts/dump_xlsx.py "<파일.xlsx>" [최대행]
python scripts/dump_xlsx.py docs/security_in_computing_glossary.xlsx 30
```

- openpyxl 없이 모든 시트를 행 단위로 출력합니다. 시트 구조·열 위치 확인용
- 아래 두 생성 스크립트가 이 파일을 모듈로 import 합니다(같은 폴더에 있어야 함).

### `gen_glossary_defaults.py` — 보안용어집 기본 데이터

```bash
python scripts/gen_glossary_defaults.py
```

| 입력 | 출력(덮어씀) |
|------|--------------|
| `docs/security_in_computing_glossary.xlsx` — `보안 용어집` 시트 (번호·한글 용어·영문 표기·약어·분류·의미·관련 키워드) | `backend/src/main/java/com/monosun/secportal/glossary/GlossaryDefaults.java` |

### `gen_opstatus_defaults.py` — 운영현황관리 기본 항목

```bash
python scripts/gen_opstatus_defaults.py
```

| 입력 | 출력(덮어씀) |
|------|--------------|
| `docs/1.3.3.정보보호_관리체계_운영현황표v1.1_250424.xlsx` — 시트1 정보보호관리체계(ISMS), 시트2 개인정보보호관리체계 | `backend/src/main/java/com/monosun/secportal/opstatus/OperationStatusDefaults.java` |

- 월 표시(`○` / `예정`)는 계획 비트마스크(bit 0 = 1월)로 변환됩니다.

> ⚠️ 두 원본 엑셀은 저장소에 포함되어 있지 않습니다. 다시 생성하려면 원본 파일을 위 경로에 둔 뒤 실행하세요.

---

## 3. 제품명 변경 스크립트 (일회성)

제품명 통일(SecPortal → KSecPortal) 작업은 완료되었습니다. 지금은 **누락 점검용**으로만 사용합니다.
두 스크립트 모두 **기본은 미리보기**이며 `--apply` 를 붙여야 실제로 파일을 고칩니다.

```bash
python scripts/rename_secportal_docs.py             # .md 의 'SecPortal' 대상 목록만 출력
python scripts/rename_secportal_docs.py --apply     # 실제 변경

python scripts/rename_subject_prefix.py             # 알림 제목 [SecPortal] 대상(.java·.md·.vue·.js)
python scripts/rename_subject_prefix.py --apply
```

- 코드 식별자·패키지명(`com.monosun.secportal`)·컨테이너명·계정, `SecPortalApplication` 은 바꾸지 않습니다.
- `CLAUDE.md`, `.git`·`node_modules`·`dist`·`build`·`.gradle`·`backups` 는 제외합니다.
- 마지막 줄이 `대상(미적용) 파일 0개 / 0건` 이면 남은 곳이 없습니다.

---

`scripts/__pycache__/` 는 파이썬 실행 시 생기는 캐시이므로 지워도 됩니다(git 추적 대상 아님).
