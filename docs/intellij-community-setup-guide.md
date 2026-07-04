# IntelliJ IDEA Community Edition 개발 환경설정 매뉴얼

## 목차
1. [다운로드 및 설치](#1-다운로드-및-설치)
2. [초기 설정](#2-초기-설정)
3. [JDK 설정](#3-jdk-설정)
4. [UI 및 에디터 설정](#4-ui-및-에디터-설정)
5. [플러그인 설치](#5-플러그인-설치)
6. [Git 연동](#6-git-연동)
7. [코드 스타일 설정](#7-코드-스타일-설정)
8. [단축키 정리](#8-단축키-정리)
9. [프로젝트 임포트](#9-프로젝트-임포트)

---

## 1. 다운로드 및 설치

### 1.1 다운로드
- 공식 사이트: https://www.jetbrains.com/idea/download/
- **Community Edition** (무료) 선택
- OS에 맞는 설치 파일 다운로드 (Windows `.exe` / macOS `.dmg` / Linux `.tar.gz`)

### 1.2 Windows 설치
1. 다운로드한 `.exe` 파일 실행
2. 설치 옵션 선택:
   - `Add "Open Folder as Project"` 체크 (탐색기 우클릭 메뉴 추가)
   - `Add launchers dir to the PATH` 체크 (터미널에서 `idea` 명령어 사용)
   - `.java`, `.groovy`, `.kt` 파일 연결 여부 선택
3. 설치 완료 후 IntelliJ 실행

### 1.3 macOS 설치
1. `.dmg` 파일 열기
2. `IntelliJ IDEA CE.app`을 `/Applications` 폴더로 드래그
3. Launchpad 또는 Spotlight에서 실행

---

## 2. 초기 설정

### 2.1 첫 실행 설정
- 기존 설정 가져오기 여부 선택 → 처음이면 `Do not import settings` 선택
- UI 테마 선택: `Darcula` (다크) 또는 `IntelliJ Light` (라이트)

### 2.2 한국어 언어팩 설치 (선택사항)
1. `Plugins` 메뉴 이동
2. Marketplace에서 `Korean Language Pack` 검색
3. `Install` 클릭 후 재시작

---

## 3. JDK 설정

### 3.1 JDK 다운로드 (미설치 시)
- **OpenJDK**: https://adoptium.net (Temurin 권장)
- 또는 IntelliJ 내에서 직접 다운로드 가능

### 3.2 프로젝트 SDK 설정
1. `File` → `Project Structure` (`Ctrl+Alt+Shift+S`)
2. `Platform Settings` → `SDKs` → `+` 클릭
3. `Add JDK` 선택 → JDK 설치 경로 지정
   - Windows 기본 경로 예시: `C:\Program Files\Eclipse Adoptium\jdk-17`
   - macOS 기본 경로 예시: `/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home`
4. `Project` 탭에서 `Project SDK` 및 `Project language level` 설정

### 3.3 IntelliJ 내 JDK 다운로드
1. `File` → `Project Structure` → `SDKs` → `+`
2. `Download JDK` 선택
3. 버전 및 벤더(Vendor) 선택 후 다운로드

---

## 4. UI 및 에디터 설정

### 4.1 설정 진입 방법
- `File` → `Settings` (Windows/Linux)
- `IntelliJ IDEA` → `Preferences` (macOS)
- 단축키: `Ctrl+Alt+S` (Windows) / `Cmd+,` (macOS)

### 4.2 테마 및 외관
| 경로 | 설정 내용 |
|------|-----------|
| `Appearance & Behavior` → `Appearance` | 테마 변경 (Darcula / Light) |
| `Appearance & Behavior` → `Appearance` → `Use custom font` | UI 폰트 변경 |

### 4.3 에디터 폰트 설정
`Editor` → `Font`
- Font: `JetBrains Mono` (기본값, 권장) 또는 `D2Coding`, `Fira Code`
- Size: `14` ~ `16`
- Line height: `1.2` ~ `1.4`
- `Enable ligatures` 체크: 코드 가독성 향상 (`->`, `=>` 등 기호 합자)

### 4.4 인코딩 설정
`Editor` → `File Encodings`
- `Global Encoding`: `UTF-8`
- `Project Encoding`: `UTF-8`
- `Default encoding for properties files`: `UTF-8`
- `Transparent native-to-ascii conversion` 체크

### 4.5 자동 저장 / 줄바꿈 설정
`Editor` → `General`
- `Auto-import`: Java 자동 임포트 활성화 권장
- `Ensure every saved file ends with a line break` 체크

`Editor` → `Code Style`
- `Line separator`: `Unix and macOS (\n)` 권장 (팀 협업 시)

### 4.6 탭 설정
`Editor` → `Code Style` → `Java` (또는 Kotlin 등)
- Indent 크기: 팀 컨벤션에 맞게 설정 (보통 4 space)
- `Use tab character` 체크 여부: 스페이스 권장

---

## 5. 플러그인 설치

`File` → `Settings` → `Plugins`에서 설치

### 5.1 필수 플러그인
| 플러그인 | 용도 |
|---------|------|
| `GitToolBox` | Git 상태를 에디터 라인에 표시 |
| `Lombok` | Lombok 어노테이션 지원 |
| `Maven Helper` | Maven 의존성 충돌 분석 |
| `.env files support` | .env 파일 구문 강조 |
| `IdeaVim` | Vim 키바인딩 사용 시 |

### 5.2 추천 플러그인
| 플러그인 | 용도 |
|---------|------|
| `Rainbow Brackets` | 괄호 쌍 색상 구분 |
| `String Manipulation` | 문자열 케이스 변환 유틸 |
| `Key Promoter X` | 단축키 학습 도우미 |
| `SonarLint` | 실시간 코드 품질 분석 |
| `Docker` | Docker 파일 지원 (Community는 제한적) |
| `HTTP Client` | REST API 테스트 내장 |

### 5.3 플러그인 설치 방법
1. `Plugins` → `Marketplace` 탭
2. 검색창에 플러그인 이름 입력
3. `Install` 클릭
4. 설치 완료 후 `Restart IDE` 클릭

---

## 6. Git 연동

### 6.1 Git 실행 파일 경로 설정
`Version Control` → `Git`
- `Path to Git executable` 확인: `git` 명령어 경로 지정
- `Test` 버튼으로 정상 인식 확인

### 6.2 GitHub / GitLab 계정 연동
`Version Control` → `GitHub` 또는 `GitLab`
- `Add Account` → 계정 로그인 또는 Token 입력

### 6.3 Git 기본 설정
터미널에서 사전 설정:
```bash
git config --global user.name "이름"
git config --global user.email "이메일"
git config --global core.autocrlf input   # macOS/Linux
git config --global core.autocrlf true    # Windows
```

### 6.4 프로젝트 Git 초기화
- 메뉴: `VCS` → `Enable Version Control Integration` → `Git`
- 또는 프로젝트 루트에서 `git init`

### 6.5 유용한 Git 설정
`Version Control` → `Commit`
- `Perform code analysis` 체크: 커밋 전 코드 분석
- `Check TODO` 체크: TODO 주석 경고

---

## 7. 코드 스타일 설정

### 7.1 코드 포맷터 설정
`Editor` → `Code Style` → 언어 선택 (Java / Kotlin 등)
- 팀 공유 설정 파일이 있으면 `Import Scheme` 사용
- Google Java Style 등 외부 설정 임포트 가능

### 7.2 자동 포맷 단축키
- 파일 전체 포맷: `Ctrl+Alt+L` (Windows) / `Cmd+Option+L` (macOS)
- 임포트 최적화: `Ctrl+Alt+O` (Windows) / `Cmd+Option+O` (macOS)

### 7.3 저장 시 자동 포맷 (선택사항)
`Tools` → `Actions on Save`
- `Reformat code` 체크
- `Optimize imports` 체크
- `Run code cleanup` 체크

---

## 8. 단축키 정리

### 8.1 필수 단축키 (Windows / macOS)

| 기능 | Windows | macOS |
|------|---------|-------|
| 전체 검색 (어디서나) | `Shift+Shift` | `Shift+Shift` |
| 파일 검색 | `Ctrl+Shift+N` | `Cmd+Shift+O` |
| 클래스 검색 | `Ctrl+N` | `Cmd+O` |
| 심볼 검색 | `Ctrl+Alt+Shift+N` | `Cmd+Option+O` |
| 액션 검색 | `Ctrl+Shift+A` | `Cmd+Shift+A` |
| 사용처 찾기 | `Alt+F7` | `Option+F7` |
| 정의 이동 | `Ctrl+B` | `Cmd+B` |
| 선언 이동 | `Ctrl+Alt+B` | `Cmd+Option+B` |
| 코드 포맷 | `Ctrl+Alt+L` | `Cmd+Option+L` |
| 임포트 최적화 | `Ctrl+Alt+O` | `Cmd+Option+O` |
| 자동완성 | `Ctrl+Space` | `Ctrl+Space` |
| 빠른 수정 | `Alt+Enter` | `Option+Enter` |
| 리팩터링 메뉴 | `Ctrl+Alt+Shift+T` | `Ctrl+T` |
| 이름 변경 | `Shift+F6` | `Shift+F6` |
| 실행 | `Shift+F10` | `Ctrl+R` |
| 디버그 | `Shift+F9` | `Ctrl+D` |
| 라인 복사 | `Ctrl+D` | `Cmd+D` |
| 라인 삭제 | `Ctrl+Y` | `Cmd+Delete` |
| 주석 토글 | `Ctrl+/` | `Cmd+/` |
| 블록 주석 | `Ctrl+Shift+/` | `Cmd+Option+/` |
| 터미널 열기 | `Alt+F12` | `Option+F12` |
| 설정 열기 | `Ctrl+Alt+S` | `Cmd+,` |
| 프로젝트 구조 | `Ctrl+Alt+Shift+S` | `Cmd+;` |

### 8.2 키맵 변경
`Settings` → `Keymap`
- 기본: `IntelliJ IDEA`
- Eclipse 키맵 사용 가능 (Eclipse 사용자 전환 시)
- 개별 단축키 더블클릭 → 변경 가능

---

## 9. 프로젝트 임포트

### 9.1 Maven 프로젝트
1. `File` → `Open` → `pom.xml` 선택
2. `Open as Project` 클릭
3. Maven 의존성 자동 다운로드 완료 대기

### 9.2 Gradle 프로젝트
1. `File` → `Open` → `build.gradle` 또는 `build.gradle.kts` 선택
2. `Open as Project` 클릭
3. Gradle sync 완료 대기

### 9.3 Spring Boot 프로젝트 생성 (Spring Initializr)
1. `File` → `New` → `Project`
2. `Spring Initializr` 선택 (플러그인 필요 시 설치)
3. Group, Artifact, Java 버전, 의존성 선택
4. `Create` 클릭

### 9.4 빌드 설정 확인
`File` → `Project Structure` → `Modules`
- 소스 루트(`src/main/java`)가 `Sources` 로 지정되어 있는지 확인
- 테스트 루트(`src/test/java`)가 `Tests` 로 지정되어 있는지 확인

---

## 부록: 권장 설정 체크리스트

- [ ] JDK 버전 설정 완료
- [ ] 파일 인코딩 UTF-8 설정
- [ ] 에디터 폰트 및 크기 설정
- [ ] Git 실행 파일 경로 확인
- [ ] Git 사용자 이름 / 이메일 설정
- [ ] 필요 플러그인 설치
- [ ] 코드 스타일 팀 컨벤션 맞춤 설정
- [ ] 저장 시 자동 포맷 설정 (선택)
- [ ] 단축키 숙지

---

*작성일: 2026-05-16 | IntelliJ IDEA Community Edition 기준*
