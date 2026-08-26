-- v1.39.0 — 에러 로그 관리 테이블
-- 기존 인스턴스용 마이그레이션. (ddl-auto=update 로도 생성되지만 컬럼 타입·인덱스를 명확히 맞춘다)
-- 실행: docker compose exec -T db mysql -usecportal -psecportal123 secportal < db/migration/v1.39.0_error_logs.sql


CREATE TABLE IF NOT EXISTS error_logs (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    occurred_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    level          ENUM('ERROR','WARN') NOT NULL DEFAULT 'ERROR',
    source         ENUM('BACKEND','FRONTEND','SCHEDULER') NOT NULL DEFAULT 'BACKEND',
    status         ENUM('NEW','IN_PROGRESS','RESOLVED','IGNORED') NOT NULL DEFAULT 'NEW',
    exception_type VARCHAR(255),
    message        TEXT,
    stack_trace    LONGTEXT,
    http_method    VARCHAR(10),
    request_uri    VARCHAR(500),
    http_status    INT,
    user_id        BIGINT,
    user_name      VARCHAR(100),
    ip_address     VARCHAR(100),
    user_agent     VARCHAR(500),
    fingerprint    VARCHAR(64),
    note           TEXT,
    handled_by     BIGINT,
    handled_at     DATETIME,
    KEY idx_error_logs_occurred (occurred_at),
    KEY idx_error_logs_fingerprint (fingerprint),
    KEY idx_error_logs_status (status),
    KEY idx_error_logs_handled_by (handled_by),
    FOREIGN KEY (handled_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
