# KSecPortal v1.45.2 릴리즈 노트

**릴리즈 일자**: 2026-11-10

**[보안 수정]** 공개 저장소에 알려진 기본 관리자 비밀번호와 기본 비밀 값으로 운영되는 위험을 줄이고, 화면의 XSS 방어를 강화합니다.

---

## 1. 초기 비밀번호 변경을 서버에서 강제

- 지금까지는 초기 비밀번호 변경을 **화면(라우터)에서만** 강제해, 변경 전 계정도 API 를 직접 호출하면 모든 기능을 쓸 수 있었습니다.
- 이제 `mustChangePassword` 계정은 비밀번호 변경 전까지 업무 API 가 **403** 으로 거절됩니다. 허용 경로: `/auth/me`·`/auth/password`·`/auth/refresh`·`/public/**`·`/codes/**`
- 새 비밀번호가 현재 비밀번호와 같으면 거절합니다.

## 2. 비밀 값 키 교체 지원 (`PI_ENCRYPTION_KEY_PREVIOUS`)

- 공개 기본값인 `JASYPT_ENCRYPTOR_PASSWORD`(또는 `PI_ENCRYPTION_KEY`)를 바꾸면 기존 개인정보·연동 비밀 암호문을 읽을 수 없어 교체가 어려웠습니다.
- `.env` 에 **이전 키**를 `PI_ENCRYPTION_KEY_PREVIOUS` 로 넣고 재기동하면, 복호화는 새 키 → 이전 키 순으로 시도하고 기동 시 암호문 전체를 새 키로 다시 암호화합니다. 로그의 `키 교체: N건 재암호화, 복호화 불가 0건` 을 확인한 뒤 값을 비우세요.
- 교체 전에 만든 백업을 복원할 때도 같은 방법으로 새 키로 옮길 수 있습니다.
- 비상연락망 휴대전화·이메일(`emergency_contacts.mobile`·`email`)을 평문 백필·재암호화 대상에 추가했습니다.

## 3. 화면 보안 강화

- **CSP**: `script-src` 에서 `'unsafe-inline'` 제거, `base-uri 'self'`·`form-action 'self'` 추가 — 주입된 인라인 스크립트가 실행되지 않습니다.
- **로그인 화면 버전 표기 제거** — 버전은 로그인 후 환경설정 > 정보 탭에서 확인합니다.
- 프로덕션 빌드 **소스맵 비활성**을 `vite.config.js` 에 명시했습니다.
- 소스 보기 단축키(Ctrl+U, macOS Cmd+Option+U)를 누르면 화면을 새로고침합니다. 주소창의 `view-source:`·개발자 도구까지 막는 장치는 아닙니다.

## 4. 운영자 조치 (중요)

`.env` 에 아래 값이 공개 기본값으로 남아 있으면 반드시 교체하세요. `SECURITY_FAIL_ON_INSECURE_SECRETS=true` 로 두면 기본값이 남은 상태에서 기동이 중단됩니다.

| 값 | 교체 시 영향 |
|----|-------------|
| `JWT_SECRET` | 기존 로그인 세션이 모두 끊김(재로그인) |
| `DB_PASSWORD`·`DB_ROOT_PASSWORD` | MySQL 계정 비밀번호를 먼저 `ALTER USER` 로 바꾼 뒤 `.env` 수정 (`.env` 만 바꾸면 기존 볼륨에 반영되지 않음) |
| `JASYPT_ENCRYPTOR_PASSWORD` | 기존 `ENC(...)` 값은 새 키로 다시 만들고, 이전 키를 `PI_ENCRYPTION_KEY_PREVIOUS` 에 넣어 재기동 |

교체 전에 DB 를 백업하고, 새 키는 **저장소 밖에 따로 보관**하세요. 키를 잃으면 암호화된 개인정보는 복구할 수 없습니다.

## 5. DB 변경 — 없음

## 6. 업그레이드

```bash
git pull
docker compose build backend frontend
docker compose up -d backend frontend
```

배포 후 브라우저 강력 새로고침(Ctrl+Shift+R). 초기 비밀번호를 아직 바꾸지 않은 계정은 로그인 직후 변경 화면으로 이동합니다.
