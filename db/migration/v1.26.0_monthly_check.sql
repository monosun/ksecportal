-- v1.26.0 Migration: 월간 보안점검 테이블 추가
-- Run this against an existing database to add the Monthly Security Check feature

CREATE TABLE IF NOT EXISTS monthly_check_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    check_month VARCHAR(7) NOT NULL COMMENT 'YYYY-MM',
    priority ENUM('HIGH', 'MEDIUM', 'LOW') NOT NULL DEFAULT 'HIGH',
    category VARCHAR(50) NOT NULL,
    item_name VARCHAR(200) NOT NULL,
    check_method VARCHAR(500),
    check_example VARCHAR(500),
    result ENUM('COMPLETED', 'INCOMPLETE', 'NA') NOT NULL DEFAULT 'INCOMPLETE',
    notes TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);
