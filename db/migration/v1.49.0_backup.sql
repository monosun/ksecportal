-- v1.49.0: backup_history table for tracking backup operations
CREATE TABLE IF NOT EXISTS backup_history (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    filename    VARCHAR(255),
    file_size   BIGINT,
    backup_type VARCHAR(20)  NOT NULL DEFAULT 'MANUAL',  -- MANUAL | SCHEDULED
    status      VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS', -- SUCCESS | FAILED
    message     VARCHAR(500),
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO app_settings (setting_key, setting_value, description) VALUES
('backup.enabled',          'false',       '정기 백업 활성화 여부'),
('backup.cron',             '0 0 2 * * ?', '정기 백업 Cron 표현식 (기본: 매일 새벽 2시)'),
('backup.keep.count',       '10',          '백업 파일 최대 보관 개수'),
('backup.default.password', '',            '정기 백업 기본 비밀번호')
ON DUPLICATE KEY UPDATE description = VALUES(description);
