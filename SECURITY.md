# Security Policy / 보안 정책

## Supported versions / 지원 버전

Security fixes are made on the latest release on `main`. Please upgrade to the latest
[release](https://github.com/monosun/ksecportal/releases) before reporting.

보안 수정은 `main` 브랜치의 최신 릴리즈에만 반영됩니다. 신고 전에 최신 릴리즈로 업데이트해 주세요.

| Version | Supported |
|---------|-----------|
| 1.42.x (latest) | ✅ |
| < 1.42 | ❌ |

## Reporting a vulnerability / 취약점 신고

**Please do not open a public GitHub issue for security vulnerabilities.**
보안 취약점은 공개 이슈로 올리지 말아 주세요.

Primary security contact / 보안 담당 연락처: **javaatoz@gmail.com**

Please include / 다음 내용을 포함해 주세요:

- Affected version (or commit) and component (backend API, frontend, deployment config)
  영향받는 버전(또는 커밋)과 구성요소(백엔드 API, 프론트엔드, 배포 설정)
- Steps to reproduce or a proof of concept / 재현 절차 또는 PoC
- Impact (e.g. authentication bypass, privilege escalation, personal-information exposure)
  영향(예: 인증 우회, 권한 상승, 개인정보 노출)

We aim to acknowledge reports within **3 business days** and to share a fix plan within
**14 days**. Fixes are published as a new release, and reporters are credited in the release
notes unless they prefer otherwise.

접수 후 **3영업일 이내** 회신하고, **14일 이내** 조치 계획을 공유합니다. 수정은 새 릴리즈로 배포하며,
원하지 않으시는 경우를 제외하고 릴리즈 노트에 신고자를 기재합니다.

## Scope / 범위

In scope: this repository's code and its default Docker Compose deployment
(`docker-compose.yml`, `nginx/`, `backend/`, `frontend/`, `db/`).

Out of scope: vulnerabilities that require an already-compromised host or administrator account,
insecure values left unchanged by a deployer (see `INSTALL.md` for hardening, including
`SECURITY_FAIL_ON_INSECURE_SECRETS`), and issues in third-party dependencies with no
KSecPortal-specific impact (please report those upstream).

범위 외: 이미 장악된 호스트·관리자 계정이 전제인 경우, 배포자가 기본값을 바꾸지 않은 설정
(`INSTALL.md`의 보안 강화 항목 및 `SECURITY_FAIL_ON_INSECURE_SECRETS` 참고), KSecPortal 고유 영향이
없는 서드파티 의존성 취약점(해당 프로젝트에 신고).
