# 개발 환경 구성 가이드

다른 PC에서 SecPortal 개발 환경을 처음 구성하는 방법입니다.

## 목차

1. [사전 요구사항](#1-사전-요구사항)
2. [소스 코드 가져오기](#2-소스-코드-가져오기)
3. [환경변수 설정](#3-환경변수-설정)
4. [실행 방법 A: Docker Compose (권장)](#4-실행-방법-a-docker-compose-권장)
5. [실행 방법 B: 개별 서비스 실행 (개발용)](#5-실행-방법-b-개별-서비스-실행-개발용)
6. [IDE 설정](#6-ide-설정)
7. [자주 하는 작업](#7-자주-하는-작업)
8. [트러블슈팅](#8-트러블슈팅)

---

## 1. 사전 요구사항

### 1-1. OS별 필수 소프트웨어

#### Windows

| 소프트웨어 | 버전 | 설치 방법 |
|-----------|------|----------|
| Git | 최신 | https://git-scm.com |
| Docker Desktop | 4.x 이상 | https://docs.docker.com/desktop/windows/ |
| JDK 17 | Temurin 17 | https://adoptium.net |
| Node.js | 20 LTS | https://nodejs.org |
| IntelliJ IDEA | Community 이상 | https://www.jetbrains.com/idea/ |
| VS Code | 최신 | https://code.visualstudio.com |

> Docker Desktop 설치 시 WSL 2 백엔드를 사용하도록 설정합니다.  
> Docker Desktop → Settings → General → **Use WSL 2 based engine** 체크

#### macOS

```bash
# Homebrew 설치 (없으면)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 필수 패키지
brew install git node@20
brew install --cask docker temurin@17 intellij-idea visual-studio-code

# Docker Desktop 실행 후 상단 메뉴바 아이콘 클릭해서 시작 확인
```

#### Ubuntu / Debian

```bash
# Git
sudo apt update && sudo apt install -y git

# Docker
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
newgrp docker

# JDK 17
sudo apt install -y openjdk-17-jdk
java -version  # openjdk 17.x.x 확인

# Node.js 20 (nvm 사용 권장)
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.0/install.sh | bash
source ~/.bashrc
nvm install 20
nvm use 20
node -v  # v20.x.x 확인
```

### 1-2. 설치 확인

```bash
git --version        # git 2.x.x
docker --version     # Docker 26.x.x
docker compose version  # Docker Compose 2.x.x
java -version        # openjdk 17.x.x
node -v              # v20.x.x
npm -v               # 10.x.x
```

---

## 2. 소스 코드 가져오기

### 2-1. 리포지토리 클론

```bash
git clone https://github.com/your-org/secportal.git
cd secportal
```

### 2-2. 디렉토리 구조 확인

```
secportal/
├── docker-compose.yml
├── .env.example
├── nginx/nginx.conf
├── db/init/            ← MySQL 초기화 SQL (01~05)
├── backend/            ← Spring Boot
│   ├── build.gradle
│   ├── Dockerfile
│   └── src/
└── frontend/           ← Vue 3
    ├── package.json
    ├── vite.config.js
    └── src/
```

---

## 3. 환경변수 설정

```bash
cp .env.example .env
```

`.env` 파일 열기 (개발 환경용 — 기본값 그대로 사용 가능):

```bash
# 개발 환경은 기본값으로도 동작합니다
DB_ROOT_PASSWORD=rootpassword
DB_USER=secportal
DB_PASSWORD=secportal123
JWT_SECRET=dev-secret-key-minimum-32-characters-here
JWT_EXPIRATION=86400000

# 이메일 알림은 비워두면 발송 생략 (개발 시 무관)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
```

---

## 4. 실행 방법 A: Docker Compose (권장)

코드 수정 없이 전체 스택을 한 번에 실행합니다.  
프론트엔드·백엔드·DB 모두 Docker 컨테이너로 뜹니다.

### 4-1. 전체 빌드 및 실행

제공된 스크립트를 사용하면 `.env` 자동 생성, 헬스체크 대기, 접속 정보 출력까지 자동으로 처리됩니다.

**Windows (PowerShell)**:
```powershell
.\start.ps1            # 기본 시작 (최초 빌드 자동 포함)
.\start.ps1 -Build     # 코드 변경 후 이미지 재빌드
.\start.ps1 -Clean     # DB 포함 완전 초기화
.\start.ps1 -Logs      # 시작 후 로그 스트리밍
.\stop.ps1             # 중단 (데이터 유지)
.\stop.ps1 -Clean      # 중단 + DB 데이터 삭제
```

**macOS / Linux**:
```bash
chmod +x start.sh stop.sh   # 최초 1회
./start.sh             # 기본 시작
./start.sh --build     # 코드 변경 후 이미지 재빌드
./start.sh --clean     # DB 포함 완전 초기화
./start.sh --logs      # 시작 후 로그 스트리밍
./stop.sh              # 중단 (데이터 유지)
./stop.sh --clean      # 중단 + DB 데이터 삭제
```

스크립트 없이 직접 실행할 경우:
```bash
# 최초 실행 (빌드 포함, 5~10분 소요)
docker compose up -d --build

# 컨테이너 상태 확인
docker compose ps
```

| 컨테이너 | 포트 | 역할 |
|---------|------|------|
| secportal-frontend | :80 | Vue 3 SPA (Nginx) |
| secportal-backend | :8080 | Spring Boot REST API |
| secportal-db | :3306 | MySQL 8 |

### 4-2. 접속 확인

| URL | 설명 |
|-----|------|
| http://localhost | 웹 애플리케이션 |
| http://localhost:8080/api/auth/me | 백엔드 API |

기본 관리자 계정:
- 이메일: `secportal@monosun.com`
- 비밀번호: `Admin1234!`

### 4-3. 코드 수정 후 재빌드

```bash
# 백엔드 코드 수정 후
docker compose up -d --build backend

# 프론트엔드 코드 수정 후
docker compose up -d --build frontend

# 전체 재빌드
docker compose up -d --build
```

### 4-4. 중단 및 정리

```powershell
# Windows
.\stop.ps1           # 중단 (데이터 유지)
.\stop.ps1 -Clean    # 완전 초기화 (DB 데이터 삭제 — 주의!)
```

```bash
# macOS / Linux
./stop.sh            # 중단 (데이터 유지)
./stop.sh --clean    # 완전 초기화 (DB 데이터 삭제 — 주의!)
```

---

## 5. 실행 방법 B: 개별 서비스 실행 (개발용)

백엔드/프론트엔드를 로컬에서 직접 실행하면 코드 변경 시 즉시 반영됩니다.  
(핫 리로드 활용 가능)

### 5-1. DB만 Docker로 실행

```bash
docker compose up db -d

# DB가 준비될 때까지 대기 (healthy 상태 확인)
docker compose ps db
```

### 5-2. 백엔드 실행 (Spring Boot)

**Windows (PowerShell)**:
```powershell
cd backend
.\gradlew.bat bootRun
```

**macOS / Linux**:
```bash
cd backend
./gradlew bootRun
```

> 최초 실행 시 Gradle 의존성 다운로드로 2~3분 소요됩니다.

백엔드가 정상 기동되면 아래 로그가 출력됩니다:
```
Started SecPortalApplication in 8.xxx seconds
Tomcat started on port 8080
```

### 5-3. 프론트엔드 실행 (Vite 개발 서버)

새 터미널을 열고:

```bash
cd frontend
npm install       # 최초 1회
npm run dev
```

```
  VITE v5.x.x  ready in xxx ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: http://192.168.x.x:5173/
```

브라우저에서 `http://localhost:5173` 접속.  
`/api` 요청은 `vite.config.js`의 proxy 설정으로 백엔드(`:8080`)로 전달됩니다.

### 5-4. 개발 포트 정리

| 서비스 | 주소 |
|-------|------|
| 프론트엔드 (Vite) | http://localhost:5173 |
| 백엔드 (Spring Boot) | http://localhost:8080 |
| DB (MySQL) | localhost:3306 |

---

## 6. IDE 설정

### 6-1. IntelliJ IDEA (백엔드)

**프로젝트 열기**:
```
File → Open → secportal/backend 폴더 선택
```

**Lombok 플러그인 설치** (없는 경우):
```
File → Settings → Plugins → Marketplace → "Lombok" 검색 → Install
```

**Annotation Processing 활성화**:
```
File → Settings → Build, Execution, Deployment
  → Compiler → Annotation Processors
  → "Enable annotation processing" 체크
```

**환경변수 설정** (로컬 실행 시):

Run/Debug Configurations → SecPortalApplication → Environment variables:
```
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/secportal?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
SPRING_DATASOURCE_USERNAME=secportal
SPRING_DATASOURCE_PASSWORD=secportal123
JWT_SECRET=dev-secret-key-minimum-32-characters-here
```

### 6-2. VS Code (프론트엔드)

**프로젝트 열기**:
```
File → Open Folder → secportal/frontend 폴더 선택
```

**권장 익스텐션** (`.vscode/extensions.json` 자동 제안):

```json
{
  "recommendations": [
    "Vue.volar",
    "dbaeumer.vscode-eslint",
    "bradlc.vscode-tailwindcss",
    "esbenp.prettier-vscode"
  ]
}
```

VS Code에서 `Ctrl+Shift+P` → "Extensions: Show Recommended Extensions" 후 설치.

---

## 7. 자주 하는 작업

### 7-1. DB 스키마 변경

스키마는 `db/init/` SQL 파일로만 관리합니다 (`ddl-auto: none` — Hibernate가 스키마에 관여하지 않습니다).

```
db/init/01_schema.sql  ← DDL 변경
백엔드 엔티티(.java)   ← 동일하게 수정
```

변경 후 DB 컨테이너를 재초기화합니다:

```powershell
# Windows — DB 볼륨 포함 초기화 후 재시작
.\start.ps1 -Clean
```

```bash
# macOS / Linux
./start.sh --clean
```

### 7-2. 새 API 엔드포인트 추가 (백엔드)

1. `db/init/` SQL 수정 (테이블 변경 시)
2. `entity/` 엔티티 클래스 작성
3. `repository/` JPA 레포지토리 작성
4. `dto/` 요청/응답 DTO 작성
5. `service/` 비즈니스 로직 작성
6. `controller/` 컨트롤러 작성 + `@PreAuthorize` 권한 설정
7. `AuditLogService.log()` 호출 (생성/수정/삭제 액션)
8. `docs/api.md` 업데이트

### 7-3. 새 페이지 추가 (프론트엔드)

1. `src/views/모듈명/NewView.vue` 생성
2. `src/router/index.js`에 라우트 추가
3. `src/api/index.js`에 API 함수 추가
4. `src/components/layout/AppLayout.vue`의 `navItems`에 메뉴 추가
5. `src/i18n/ko.json`, `en.json`에 i18n 키 추가

### 7-4. i18n 키 추가

`src/i18n/ko.json`:
```json
{
  "myModule": {
    "title": "새 모듈",
    "create": "새로 만들기"
  }
}
```

`src/i18n/en.json`:
```json
{
  "myModule": {
    "title": "New Module",
    "create": "Create New"
  }
}
```

Vue 템플릿에서 사용:
```html
{{ $t('myModule.title') }}
```

### 7-5. 감사 로그 기록 추가

서비스 클래스에서:
```java
auditLogService.log("MYMODULE_CREATED", "MY_MODULE", entity.getId(), entity.getTitle());
```

### 7-6. Git 브랜치 전략

```bash
# 새 기능 개발
git checkout -b feature/기능명
# 개발 완료 후
git push origin feature/기능명
# GitHub에서 PR 생성 → 리뷰 → main 병합
```

---

## 8. 트러블슈팅

### DB 초기화 SQL이 실행되지 않음

Docker volume이 이미 존재하면 초기화 SQL이 재실행되지 않습니다.

```bash
docker compose down -v   # 볼륨 포함 삭제
docker compose up -d
```

### 백엔드 기동 실패: `SchemaManagementException`

`ddl-auto: validate`로 설정된 경우 엔티티와 DB 스키마 불일치 시 발생합니다.  
현재 프로젝트는 `ddl-auto: none`으로 설정되어 있으므로 정상적으로는 발생하지 않습니다.  
만약 발생한다면 `application.yml`의 `ddl-auto` 값을 확인하세요.

```bash
# 백엔드 로그 확인
docker compose logs backend | grep -A5 "ERROR"
```

### Windows에서 Gradle 빌드 실패: `Unexpected character: '﻿'`

Windows에서 파일 생성 시 UTF-8 BOM이 포함되면 Gradle이 파싱 오류를 냅니다.  
PowerShell로 BOM을 일괄 제거합니다:

```powershell
# build.gradle BOM 제거
$path = "backend\build.gradle"
$bytes = [System.IO.File]::ReadAllBytes($path)
if ($bytes[0] -eq 0xEF) {
    [System.IO.File]::WriteAllBytes($path, $bytes[3..($bytes.Length-1)])
}

# 소스 파일 전체 BOM 제거
Get-ChildItem "backend\src" -Recurse -Include "*.java" | ForEach-Object {
    $b = [System.IO.File]::ReadAllBytes($_.FullName)
    if ($b[0] -eq 0xEF -and $b[1] -eq 0xBB -and $b[2] -eq 0xBF) {
        [System.IO.File]::WriteAllBytes($_.FullName, $b[3..($b.Length-1)])
    }
}
```

### 포트 충돌: `Port 8080 is already in use`

```bash
# Windows (PowerShell)
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# macOS / Linux
lsof -ti:8080 | xargs kill -9
```

### 프론트엔드 API 요청 실패: `Network Error`

Vite 개발 서버에서 `/api` 요청이 백엔드로 프록시되지 않을 때:

`frontend/vite.config.js` 확인:
```javascript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

### Docker Desktop 메모리 부족 (Windows)

Docker Desktop → Settings → Resources → Memory를 **최소 4GB**로 설정.

### Gradle 빌드 실패: `Java heap space`

```bash
# backend/gradle.properties 파일에 추가
org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=512m
```

### MySQL 접속 불가 (격리 테스트)

```bash
# DB 컨테이너에 직접 접속
docker exec -it secportal-db mysql -u secportal -psecportal123 secportal

# 테이블 목록 확인
SHOW TABLES;

# 데이터 확인
SELECT id, email, role FROM users;
```

### 로그인이 안 될 때 (시드 데이터 확인)

```bash
docker exec -it secportal-db mysql -u secportal -psecportal123 secportal \
  -e "SELECT email, role, active FROM users;"
```

관리자 계정이 없으면 `02_seed.sql`이 실행되지 않은 것입니다 → DB 볼륨 초기화.

---

## 부록: 환경별 설정 차이

| 항목 | 로컬 개발 | Docker Compose | AWS 프로덕션 |
|------|----------|----------------|-------------|
| DB 주소 | `localhost:3306` | `db:3306` (컨테이너명) | `db:3306` |
| 프론트 접속 | `:5173` (Vite) | `:80` (Nginx) | `:443` (HTTPS) |
| API 프록시 | Vite proxy | Nginx `/api/` | Nginx `/api/` |
| 환경변수 | `.env` 로컬 파일 | `.env` 로컬 파일 | `.env` 서버 파일 |
| Hot reload | 지원 (Vite/Spring DevTools) | 미지원 | 미지원 |
| 디버거 연결 | IntelliJ 직접 | 원격 디버그 설정 필요 | 미사용 |
