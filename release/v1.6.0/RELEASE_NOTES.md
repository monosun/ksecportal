# v1.6.0 릴리즈 노트

**릴리즈 일자**: 2026-07-10

**소스 취약점 점검(SAST)** 기능을 신설한 릴리즈입니다. GitHub 저장소를 대상으로 GitHub 보안 알림(의존성·코드·시크릿)과 **내장 OWASP 정적분석(소스코드 직접 점검)** 을 함께 수행합니다.

## 신규 기능

### 소스 취약점 점검 (SAST)
**보안 운영 > 소스 취약점 점검** 메뉴가 추가되었습니다. (MANAGER 이상)

한 번의 점검으로 4개 카테고리를 함께 분석합니다:

- **의존성** — GitHub Dependabot 알림
- **코드** — GitHub Code scanning(CodeQL 등) 알림
- **시크릿** — GitHub Secret scanning 알림
- **SAST** — 저장소 소스를 직접(tarball) 내려받아 **OWASP Top 10:2021** 기준으로 정적분석

내장 SAST 엔진의 점검 항목(OWASP/CWE 매핑):

- A01 접근통제 — 전체 `permitAll()` 인가 우회
- A02 암호 실패 — 개인키 하드코딩, 취약 해시(MD5/SHA-1)·암호(DES/RC4/ECB), TLS 검증 비활성화, 안전하지 않은 난수
- A03 인젝션 — SQL 문자열 결합, OS 명령 실행 싱크, 동적 코드 실행(eval)
- A05 보안 설정 오류 — CORS 와일드카드, CSRF 비활성화, XXE, 디버그 모드
- A07 인증 실패 — 자격증명/비밀 하드코딩
- A08 무결성 실패 — 안전하지 않은 역직렬화

발견 항목마다 심각도, OWASP·CWE 분류, 파일·라인 위치, GitHub 링크를 제공하며, 심각도·카테고리별로 집계되어 점검 이력에 저장됩니다.

### 설정관리 — API 연동(GitHub)
**관리 > 설정관리 > API 연동** 탭에서 GitHub Personal Access Token, (선택) API Base URL(Enterprise 대응)을 등록하고 **연결 테스트**를 할 수 있습니다. 토큰 등록·조회는 ADMIN 전용이며 응답은 마스킹 처리됩니다.

## 특징

- GitHub 기능 활성화가 필요한 의존성·코드·시크릿과 달리, **SAST는 별도 활성화 없이** 소스를 직접 분석합니다.
- 저장소 tarball을 단일 요청으로 내려받아 메모리에서 분석하므로 API 호출·레이트리밋 부담이 적습니다.
- 테스트·빌드 산출물·벤더 디렉터리(node_modules, dist, target 등)는 점검에서 제외합니다.

## API 변경

| 메서드 | 경로 | 권한 | 설명 |
|--------|------|------|------|
| GET/PUT/POST | `/admin/github-config[/test]` | ADMIN | GitHub 연동 설정 조회·저장·연결 테스트 |
| GET | `/source-scan/repos` | MANAGER+ | 접근 가능한 저장소 목록 |
| POST | `/source-scan/run` | MANAGER+ | 점검 실행(의존성·코드·시크릿·SAST) |
| GET | `/source-scan/scans[/{id}]` | 인증 | 점검 이력·상세 |
| DELETE | `/source-scan/scans/{id}` | MANAGER+ | 점검 이력 삭제 |

## 문서

- 사용자 매뉴얼 — "소스 취약점 점검 (SAST)" 섹션 추가
- API 문서 — 소스 취약점 점검 엔드포인트 추가

## 업그레이드 방법

```bash
git pull
docker compose build backend frontend
docker compose up -d backend frontend
```

> 신규 테이블(`github_config`, `source_scans`, `source_scan_findings`)은 JPA(ddl-auto: update)로 자동 생성되며, 신규 설치용 `db/init/11_source_scan.sql`도 함께 제공됩니다.
