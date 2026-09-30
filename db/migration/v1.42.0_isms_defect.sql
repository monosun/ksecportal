-- v1.42.0 — ISMS 결함관리
--
-- 정보보호 관리체계 > ISMS 결함관리 화면이 쓰는 두 테이블.
--   isms_defects         : 연도별 심사 결함 1건
--   isms_defect_reports  : 연도당 1건인 결함 조치 보고서
--
-- ddl-auto: update 로도 생성되지만, 인덱스·ENUM 정의를 명시적으로 남기기 위해 migration 으로도 제공한다.
-- 적용: docker compose exec -T db mysql -usecportal -psecportal123 secportal < db/migration/v1.42.0_isms_defect.sql

CREATE TABLE IF NOT EXISTS isms_defects (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    year            INT NOT NULL,
    audit_type      ENUM('INITIAL','FOLLOWUP','RENEWAL','INTERNAL','OTHER') NOT NULL DEFAULT 'RENEWAL',
    defect_no       VARCHAR(50),
    defect_type     ENUM('DEFECT','RECOMMENDATION','IMPROVEMENT') NOT NULL DEFAULT 'DEFECT',
    severity        ENUM('CRITICAL','HIGH','MEDIUM','LOW') NOT NULL DEFAULT 'MEDIUM',
    domain_name     VARCHAR(200),
    item_code       VARCHAR(50),
    item_name       VARCHAR(300),
    title           VARCHAR(500) NOT NULL,
    content         TEXT,
    cause           TEXT,
    action_plan     TEXT,
    action_result   TEXT,
    prevention_plan TEXT,
    due_date        DATE,
    completed_date  DATE,
    assignee        VARCHAR(100),
    department      VARCHAR(100),
    status          ENUM('OPEN','IN_PROGRESS','COMPLETED','HOLD') NOT NULL DEFAULT 'OPEN',
    file_name       VARCHAR(500),
    file_path       VARCHAR(1000),
    file_size       BIGINT,
    sort_order      INT DEFAULT 0,
    registrant_id   BIGINT,
    created_at      DATETIME(6),
    updated_at      DATETIME(6),
    KEY idx_isms_defects_year (year),
    KEY idx_isms_defects_status (status),
    CONSTRAINT fk_isms_defects_registrant FOREIGN KEY (registrant_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS isms_defect_reports (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    year             INT NOT NULL,
    title            VARCHAR(500),
    audit_type       ENUM('INITIAL','FOLLOWUP','RENEWAL','INTERNAL','OTHER') NOT NULL DEFAULT 'RENEWAL',
    audit_org        VARCHAR(200),
    auditors         VARCHAR(500),
    audit_scope      VARCHAR(1000),
    audit_start_date DATE,
    audit_end_date   DATE,
    summary          TEXT,
    content          TEXT,
    conclusion       TEXT,
    reported_at      DATE,
    reporter         VARCHAR(100),
    file_name        VARCHAR(500),
    file_path        VARCHAR(1000),
    file_size        BIGINT,
    registrant_id    BIGINT,
    created_at       DATETIME(6),
    updated_at       DATETIME(6),
    UNIQUE KEY uk_isms_defect_report_year (year),
    CONSTRAINT fk_isms_defect_reports_registrant FOREIGN KEY (registrant_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
