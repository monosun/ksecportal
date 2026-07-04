-- v1.29.0: MFA(TOTP) + 로그인 실패 잠금 기능 추가

ALTER TABLE users
    ADD COLUMN mfa_enabled            BOOLEAN       NOT NULL DEFAULT FALSE,
    ADD COLUMN mfa_secret             VARCHAR(64)   NULL,
    ADD COLUMN failed_login_attempts  INT           NOT NULL DEFAULT 0,
    ADD COLUMN locked_until           DATETIME      NULL;

-- 기본 보안 설정값 (기존에 없는 경우에만 삽입)
INSERT IGNORE INTO notification_config (config_key, config_value) VALUES
    ('security.login.max_attempts',  '5'),
    ('security.login.lockout_minutes', '10');
