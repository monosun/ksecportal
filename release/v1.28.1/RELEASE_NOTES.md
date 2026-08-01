# KSecPortal v1.28.1 릴리즈 노트

**릴리즈 일자**: 2026-08-01

## 요약

소스 취약점 점검(SAST)이 취약점이 아닌 코드를 지적한 **오탐 2건**에 억제 주석을 달았습니다. 기능 동작 변화는 없습니다.

## 변경 내용

| 위치 | 규칙 | 판단 |
|------|------|------|
| `backend/.../common/config/SecretsStartupCheck.java` | `SAST-HARDCODED-SECRET` (CWE-798, 높음) | 인증에 쓰는 비밀이 아니라 **샘플 비밀번호가 남아 있는지 비교하는 탐지용 상수**. 취약점을 잡는 코드가 스스로 걸린 사례 |
| `frontend/src/data/cweInfo.js` | `SAST-TLS-DISABLED` (CWE-295, 높음) | TLS 설정이 아니라 **화면에 표시하는 조치 안내 문구**. `TrustAllCerts`·`rejectUnauthorized:false` 문자열에 반응 |

- 두 줄에 `// sast:ignore <사유>` 주석을 달아 다음 점검부터 제외되게 했습니다(`SastEngine.scanFile` 이 억제 주석이 있는 줄을 건너뜁니다).
- 규칙을 좁히는 대신 줄 단위 억제를 택했습니다. 정규식 스캐너에서 예외를 규칙에 넣기 시작하면 실제 하드코딩·TLS 우회를 놓칠 위험이 커집니다.

## 조치 후 확인

변경을 **푸시한 뒤 소스 취약점 점검을 다시 실행**해야 목록에서 사라집니다. SAST 는 GitHub 저장소 tarball 을 내려받아 분석하기 때문입니다.

## 참고 — 오탐과 별개로 챙길 것

`secportal123` 같은 샘플 비밀번호 **자체는 실재하는 리스크**지만, 위치는 위 파일이 아니라 `docker-compose.yml`·`.env` 기본값입니다. 운영 배포에서는 다음을 권장합니다.

- `.env` 의 `DB_PASSWORD_ENC`·`JWT_SECRET_ENC`·`JASYPT_ENCRYPTOR_PASSWORD` 를 실제 값으로 교체
- `SECURITY_FAIL_ON_INSECURE_SECRETS=true` 로 두어 기본값이 남아 있으면 기동이 중단되도록 설정

## 업그레이드

```bash
docker compose build backend frontend
docker compose up -d backend frontend
```

DB 스키마 변경 없음. v1.28.0 에서 올라오는 경우 `db/migration/v1.28.0_pi_column_encryption.sql` 적용 여부만 확인하세요.
