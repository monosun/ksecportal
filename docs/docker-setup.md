# 신규 노트북 Docker 환경 구성 가이드

SecPortal을 새 노트북에서 Docker로 실행하는 방법입니다.  
코드 편집이나 개발 도구 없이 **Docker Desktop + Git만** 있으면 됩니다.

---

## 목차

1. [Docker Desktop 설치](#1-docker-desktop-설치)
2. [Git 설치 및 소스 받기](#2-git-설치-및-소스-받기)
3. [환경변수 설정](#3-환경변수-설정)
4. [첫 실행](#4-첫-실행)
5. [접속 확인](#5-접속-확인)
6. [일상 운영 명령어](#6-일상-운영-명령어)
7. [트러블슈팅](#7-트러블슈팅)

---

## 1. Docker Desktop 설치

### Windows

#### 1-1. WSL 2 활성화 (관리자 PowerShell에서 실행)

```powershell
wsl --install
```

> 설치 후 **재부팅** 필요. 재부팅 시 Ubuntu 초기 설정 창이 뜨면 사용자명·비밀번호를 설정합니다.

WSL 버전 확인:
```powershell
wsl --status   # Default Version: 2 확인
```

#### 1-2. Docker Desktop 설치

1. https://docs.docker.com/desktop/install/windows-install/ 에서 **Docker Desktop for Windows** 다운로드
2. 설치 파일 실행 → **Use WSL 2 instead of Hyper-V** 체크 → Install
3. 설치 완료 후 재시작
4. Docker Desktop 실행 → 우하단 고래 아이콘이 **초록색**이면 정상

#### 1-3. 설치 확인

```powershell
docker --version          # Docker version 26.x.x
docker compose version    # Docker Compose version 2.x.x
```

---

### macOS

```bash
# Homebrew가 없으면 먼저 설치
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Docker Desktop 설치
brew install --cask docker

# Docker Desktop 앱 실행 (Applications → Docker)
# 상단 메뉴바 고래 아이콘이 초록색이면 정상
```

설치 확인:
```bash
docker --version
docker compose version
```

---

## 2. Git 설치 및 소스 받기

### Git 설치

**Windows**: https://git-scm.com 에서 다운로드 → 기본 설정으로 설치

**macOS**:
```bash
brew install git
```

### 리포지토리 클론

```bash
git clone https://github.com/monosun/secportal.git
cd secportal
```

---

## 3. 환경변수 설정

```bash
# Windows (PowerShell)
copy .env.example .env

# macOS / Linux
cp .env.example .env
```

`.env` 파일을 메모장(또는 텍스트 편집기)으로 열어 아래 항목을 수정합니다.

```bash
# 반드시 변경
DB_ROOT_PASSWORD=강력한루트비밀번호
DB_PASSWORD=강력한앱비밀번호
JWT_SECRET=32자이상의시크릿키를여기에입력하세요예시값

# 선택 (이메일 알림 미사용 시 비워두어도 됩니다)
MAIL_USERNAME=
MAIL_PASSWORD=
```

> **개발/테스트 목적**이라면 `.env.example`의 기본값 그대로 사용해도 동작합니다.

---

## 4. 첫 실행

### Windows (PowerShell)

```powershell
.\start.ps1
```

### macOS / Linux

```bash
chmod +x start.sh   # 최초 1회
./start.sh
```

> 최초 실행 시 Docker 이미지 빌드로 **5~10분** 소요됩니다.  
> 이후 실행부터는 캐시를 사용해 1분 내로 완료됩니다.

정상 기동 시 출력 예시:
```
✅ secportal-db      Up (healthy)
✅ secportal-backend Up
✅ secportal-frontend Up

🚀 SecPortal 실행 완료
   접속 주소: http://localhost
```

---

## 5. 접속 확인

브라우저에서 **http://localhost** 접속

| 항목 | 값 |
|------|----|
| 관리자 이메일 | `secportal@monosun.com` |
| 관리자 비밀번호 | `Admin1234!` |

> 첫 로그인 후 **반드시 비밀번호를 변경**하세요.  
> 우측 상단 프로필 → 비밀번호 변경

---

## 6. 일상 운영 명령어

### 시작 / 중단

| 작업 | Windows | macOS / Linux |
|------|---------|--------------|
| 시작 | `.\start.ps1` | `./start.sh` |
| 중단 (데이터 유지) | `.\stop.ps1` | `./stop.sh` |
| 재시작 | `.\stop.ps1` → `.\start.ps1` | `./stop.sh` → `./start.sh` |

### 최신 코드 반영

```bash
git pull origin main

# Windows
.\start.ps1 -Build

# macOS / Linux
./start.sh --build
```

### 컨테이너 상태 확인

```bash
docker compose ps
```

```
NAME                  STATUS          PORTS
secportal-frontend    Up              0.0.0.0:80->80/tcp
secportal-backend     Up              0.0.0.0:8080->8080/tcp
secportal-db          Up (healthy)    0.0.0.0:3306->3306/tcp
```

### 로그 확인

```bash
# 전체 로그
docker compose logs -f

# 서비스별 로그
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f db
```

### 업로드 파일 백업

ISMS-P 증적 첨부 파일은 `./uploads/` 폴더에 저장됩니다.

```bash
# Windows (PowerShell)
Compress-Archive -Path uploads -DestinationPath "uploads_backup_$(Get-Date -Format yyyyMMdd).zip"

# macOS / Linux
tar -czf uploads_backup_$(date +%Y%m%d).tar.gz uploads/
```

---

## 7. 트러블슈팅

### Docker Desktop이 시작되지 않음 (Windows)

WSL 2가 제대로 설치되지 않은 경우입니다.

```powershell
# 관리자 PowerShell에서 실행
wsl --update
wsl --set-default-version 2
```

이후 Docker Desktop 재시작.

### 포트 충돌: `port is already allocated`

80, 8080, 3306 포트가 이미 사용 중인 경우입니다.

```powershell
# Windows — 사용 중인 프로세스 확인
netstat -ano | findstr ":80 "
netstat -ano | findstr ":8080"
taskkill /PID <PID번호> /F
```

```bash
# macOS / Linux
sudo lsof -ti:80 | xargs kill -9
sudo lsof -ti:8080 | xargs kill -9
```

### 백엔드가 시작되지 않음

DB 연결 실패 또는 환경변수 오류인 경우가 많습니다.

```bash
docker compose logs backend | tail -30
```

로그에서 오류 메시지 확인 후:
- `Access denied` → `.env`의 DB 비밀번호 확인
- `Communications link failure` → DB 컨테이너가 아직 준비 중, 30초 후 재시도

### DB 초기 데이터가 없음 (로그인 불가)

```bash
# DB 볼륨을 포함해 완전 초기화
# Windows
.\stop.ps1 -Clean
.\start.ps1

# macOS / Linux
./stop.sh --clean
./start.sh
```

> `-Clean` / `--clean` 옵션은 **DB 데이터가 모두 삭제**됩니다. 운영 중인 경우 주의하세요.

### Docker Desktop 메모리 부족 (빌드 중 멈춤)

Docker Desktop → Settings → Resources → **Memory: 최소 4GB** 설정 후 Apply & Restart.

### Windows에서 `./start.sh` 실행 안 됨

Windows 환경에서는 `.ps1` 스크립트를 사용하세요.

```powershell
.\start.ps1
```

---

## 부록: 개발 환경 구성

코드 수정이나 기능 개발이 필요한 경우 개발자 셋업 가이드를 참고하세요.

→ [개발 환경 구성 가이드](development-setup.md) (IDE, Gradle, Node.js, 핫 리로드 등)

---

## 부록: 디렉터리 구조

```
secportal/
├── docker-compose.yml   ← 컨테이너 구성
├── .env                 ← 환경변수 (직접 편집)
├── .env.example         ← 환경변수 템플릿
├── start.ps1            ← 시작 스크립트 (Windows)
├── start.sh             ← 시작 스크립트 (macOS/Linux)
├── stop.ps1             ← 중단 스크립트 (Windows)
├── stop.sh              ← 중단 스크립트 (macOS/Linux)
├── uploads/             ← ISMS-P 증적 첨부 파일 저장소
├── backend/             ← Spring Boot 소스
├── frontend/            ← Vue 3 소스
└── db/init/             ← MySQL 초기화 SQL
```
