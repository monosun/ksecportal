-- ─────────────────────────────────────────────────────────────────────────────
-- 에러 로그 (관리 > 에러 로그 관리)
-- 프로그램 처리 중 발생한 오류(서버 예외·스케줄러 실패·화면 JS 오류)를 적재한다.
-- 보관 기간(errorlog.retention-days, 기본 90일)이 지난 로그는 매일 새벽 자동 정리된다.
-- ─────────────────────────────────────────────────────────────────────────────

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
