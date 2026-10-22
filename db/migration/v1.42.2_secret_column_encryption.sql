-- v1.42.2 — 인증·연동 비밀값 컬럼 암호화(AES-256-GCM) 대비 컬럼 길이 확대
--
-- MFA(TOTP) 비밀키·SMTP 비밀번호·보안 솔루션 API 키·GitHub 토큰을 개인정보 컬럼과 같은 방식으로 암호화한다.
-- 암호문은 'PIENC1:' + Base64(IV ‖ 암호문 ‖ 태그) 형식이라 원문보다 길다.
-- ddl-auto: update 는 컬럼 길이를 늘려주지 않으므로 기존 DB에는 이 스크립트를 반드시 실행해야 한다.
-- 특히 users.mfa_secret(64자)은 늘리지 않으면 MFA 등록이 실패한다.
-- 실행 후 백엔드를 재기동하면 기존 평문 행이 자동으로 암호화된다(PiColumnEncryptionBackfill).
--
-- 실행: docker compose exec -T db mysql -usecportal -p<비밀번호> secportal < db/migration/v1.42.2_secret_column_encryption.sql

-- 사용자 — MFA(TOTP) 비밀키
ALTER TABLE users
    MODIFY COLUMN mfa_secret VARCHAR(255) NULL;

-- 메일 설정 — SMTP 인증 비밀번호
ALTER TABLE mail_config
    MODIFY COLUMN password VARCHAR(1024) NULL;

-- 보안 솔루션 연동 — API 키
ALTER TABLE security_integrations
    MODIFY COLUMN api_key VARCHAR(1024) NULL;

-- 소스 취약점 점검 — GitHub 토큰
ALTER TABLE github_config
    MODIFY COLUMN token VARCHAR(1024) NULL;
