-- v1.31.0: 정보보호위원회 / 내부감사 / 보안 결함사항 테이블 추가

-- ──────────────────────────────────────────────
-- 1. 정보보호위원회 (committee)
-- ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS committee_meetings (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    year        INT          NOT NULL,
    session_no  INT          NOT NULL,
    title       VARCHAR(500) NOT NULL,
    meeting_date DATE,
    location    VARCHAR(300),
    attendees   TEXT,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PLANNED',
    created_by  BIGINT,
    created_at  DATETIME(6),
    updated_at  DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_committee_year_session (year, session_no),
    KEY idx_committee_year (year),
    CONSTRAINT fk_committee_creator FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS committee_files (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    meeting_id  BIGINT       NOT NULL,
    file_type   VARCHAR(20)  NOT NULL DEFAULT 'OTHER',
    title       VARCHAR(500) NOT NULL,
    file_name   VARCHAR(500),
    file_path   VARCHAR(1000),
    file_size   BIGINT,
    uploader_id BIGINT,
    created_at  DATETIME(6),
    updated_at  DATETIME(6),
    PRIMARY KEY (id),
    KEY idx_committee_files_meeting (meeting_id),
    CONSTRAINT fk_committee_file_meeting  FOREIGN KEY (meeting_id)  REFERENCES committee_meetings (id) ON DELETE CASCADE,
    CONSTRAINT fk_committee_file_uploader FOREIGN KEY (uploader_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ──────────────────────────────────────────────
-- 2. 내부감사 (internal_audit)
-- ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS internal_audits (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    year        INT          NOT NULL,
    title       VARCHAR(500) NOT NULL,
    audit_start DATE,
    audit_end   DATE,
    auditor     VARCHAR(300),
    status      VARCHAR(20)  NOT NULL DEFAULT 'PLANNED',
    description TEXT,
    created_by  BIGINT,
    created_at  DATETIME(6),
    updated_at  DATETIME(6),
    PRIMARY KEY (id),
    KEY idx_internal_audits_year (year),
    CONSTRAINT fk_internal_audit_creator FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS audit_targets (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    audit_id    BIGINT       NOT NULL,
    target_name VARCHAR(300) NOT NULL,
    target_type VARCHAR(100),
    description TEXT,
    sort_order  INT          NOT NULL DEFAULT 0,
    created_at  DATETIME(6),
    updated_at  DATETIME(6),
    PRIMARY KEY (id),
    KEY idx_audit_targets_audit (audit_id),
    CONSTRAINT fk_audit_target_audit FOREIGN KEY (audit_id) REFERENCES internal_audits (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS audit_items (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    audit_id       BIGINT       NOT NULL,
    target_id      BIGINT,
    item_name      VARCHAR(500) NOT NULL,
    check_method   TEXT,
    result         VARCHAR(10),
    finding        TEXT,
    action_required TEXT,
    sort_order     INT          NOT NULL DEFAULT 0,
    created_at     DATETIME(6),
    updated_at     DATETIME(6),
    PRIMARY KEY (id),
    KEY idx_audit_items_audit (audit_id),
    CONSTRAINT fk_audit_item_audit   FOREIGN KEY (audit_id)  REFERENCES internal_audits (id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_item_target  FOREIGN KEY (target_id) REFERENCES audit_targets (id)   ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS audit_files (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    audit_id    BIGINT       NOT NULL,
    title       VARCHAR(500) NOT NULL,
    file_name   VARCHAR(500),
    file_path   VARCHAR(1000),
    file_size   BIGINT,
    uploader_id BIGINT,
    created_at  DATETIME(6),
    updated_at  DATETIME(6),
    PRIMARY KEY (id),
    KEY idx_audit_files_audit (audit_id),
    CONSTRAINT fk_audit_file_audit    FOREIGN KEY (audit_id)    REFERENCES internal_audits (id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_file_uploader FOREIGN KEY (uploader_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ──────────────────────────────────────────────
-- 3. 보안 결함사항 (security_findings)
-- ──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS security_findings (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    year             INT          NOT NULL,
    audit_type       VARCHAR(20)  NOT NULL DEFAULT 'ISMS_P',
    domain           VARCHAR(200),
    requirement_code VARCHAR(50),
    requirement_name VARCHAR(300),
    finding_summary  VARCHAR(500) NOT NULL,
    finding_detail   TEXT,
    risk_level       VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    corrective_action TEXT,
    action_deadline  DATE,
    status           VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    resolved_at      DATE,
    resolver         VARCHAR(300),
    file_name        VARCHAR(500),
    file_path        VARCHAR(1000),
    file_size        BIGINT,
    created_by       BIGINT,
    created_at       DATETIME(6),
    updated_at       DATETIME(6),
    PRIMARY KEY (id),
    KEY idx_sec_findings_year   (year),
    KEY idx_sec_findings_status (status),
    CONSTRAINT fk_sec_finding_creator FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
