SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================================
-- v1.17.0 ~ v1.42.0 추가 테이블
-- 최초 설치 시 01_schema.sql 이후 자동으로 실행됩니다.
-- ============================================================

-- 앱 설정
CREATE TABLE IF NOT EXISTS app_settings (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value MEDIUMTEXT,
    description VARCHAR(255),
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 코드 그룹
CREATE TABLE IF NOT EXISTS code_groups (
    group_code  VARCHAR(50)  NOT NULL PRIMARY KEY,
    group_name  VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    sort_order  INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 코드 값
CREATE TABLE IF NOT EXISTS code_values (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_code VARCHAR(50)  NOT NULL,
    value      VARCHAR(100) NOT NULL,
    label      VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    active     BIT(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 알림 설정
CREATE TABLE IF NOT EXISTS notification_config (
    config_key   VARCHAR(100) NOT NULL PRIMARY KEY,
    config_value VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 받은 메시지함 (내부 알림)
CREATE TABLE IF NOT EXISTS inbox_messages (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id  BIGINT NOT NULL,
    title         VARCHAR(200) NOT NULL,
    content       TEXT,
    type          ENUM('APPROVAL_REQUEST', 'INFO', 'SYSTEM') NOT NULL,
    is_read       BIT(1) NOT NULL DEFAULT 0,
    action_token  VARCHAR(100),
    action_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at    DATETIME(6) NOT NULL,
    FOREIGN KEY (recipient_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 공지사항
CREATE TABLE IF NOT EXISTS notices (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    title         VARCHAR(200) NOT NULL,
    content       TEXT,
    pinned        BIT(1) NOT NULL DEFAULT 0,
    active        BIT(1) NOT NULL DEFAULT 1,
    created_by_id BIGINT,
    created_at    DATETIME(6),
    updated_at    DATETIME(6),
    FOREIGN KEY (created_by_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 보안 솔루션 연동
CREATE TABLE IF NOT EXISTS security_integrations (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(255) NOT NULL,
    solution_type  VARCHAR(255) NOT NULL,
    vendor         VARCHAR(255),
    host           VARCHAR(255),
    api_key        VARCHAR(255),
    status         ENUM('CONNECTED', 'DISCONNECTED', 'ERROR') NOT NULL DEFAULT 'DISCONNECTED',
    description    TEXT,
    last_sync_at   DATETIME(6),
    created_at     DATETIME(6),
    updated_at     DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 보안 이벤트 (솔루션 연동 수집 로그)
CREATE TABLE IF NOT EXISTS security_events (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    integration_id BIGINT NOT NULL,
    event_type     VARCHAR(255) NOT NULL,
    severity       ENUM('CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO') NOT NULL,
    message        TEXT NOT NULL,
    source_ip      VARCHAR(255),
    destination_ip VARCHAR(255),
    detected_at    DATETIME(6),
    created_at     DATETIME(6),
    FOREIGN KEY (integration_id) REFERENCES security_integrations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 보안 문서 관리
CREATE TABLE IF NOT EXISTS sec_docs (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_key VARCHAR(36) NOT NULL,
    title        VARCHAR(500) NOT NULL,
    category     ENUM('POLICY','PROCEDURE','STANDARD','GUIDE','TEMPLATE','CHECKLIST','REPORT','OTHER') NOT NULL,
    version      VARCHAR(50) NOT NULL,
    description  TEXT,
    file_name    VARCHAR(500),
    file_path    VARCHAR(1000),
    file_size    BIGINT,
    is_latest    BIT(1) NOT NULL DEFAULT 1,
    uploader_id  BIGINT,
    created_at   DATETIME(6),
    updated_at   DATETIME(6),
    KEY idx_sec_docs_doc_key (document_key),
    KEY idx_sec_docs_category (category),
    FOREIGN KEY (uploader_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 커스텀 역할 (RBAC)
CREATE TABLE IF NOT EXISTS custom_roles (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at  DATETIME(6),
    updated_at  DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 역할별 메뉴 권한
CREATE TABLE IF NOT EXISTS role_permissions (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id    BIGINT NOT NULL,
    menu_key   VARCHAR(100) NOT NULL,
    can_read   BIT(1) NOT NULL DEFAULT 1,
    can_write  BIT(1) NOT NULL DEFAULT 0,
    can_delete BIT(1) NOT NULL DEFAULT 0,
    UNIQUE KEY uk_role_menu (role_id, menu_key),
    FOREIGN KEY (role_id) REFERENCES custom_roles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 사용자-커스텀역할 매핑
CREATE TABLE IF NOT EXISTS user_custom_roles (
    role_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, user_id),
    FOREIGN KEY (role_id) REFERENCES custom_roles(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 보안위원회 회의록
CREATE TABLE IF NOT EXISTS committee_meetings (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    year        INT NOT NULL,
    session_no  INT NOT NULL,
    title       VARCHAR(500) NOT NULL,
    meeting_date DATE,
    location    VARCHAR(300),
    attendees   TEXT,
    status      ENUM('PLANNED', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'PLANNED',
    created_by  BIGINT,
    created_at  DATETIME(6),
    updated_at  DATETIME(6),
    UNIQUE KEY idx_committee_year_session (year, session_no),
    KEY idx_committee_year (year),
    FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 보안위원회 첨부파일
CREATE TABLE IF NOT EXISTS committee_files (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    meeting_id  BIGINT NOT NULL,
    title       VARCHAR(500) NOT NULL,
    file_type   ENUM('AGENDA', 'MINUTES', 'OTHER') NOT NULL DEFAULT 'OTHER',
    file_name   VARCHAR(500),
    file_path   VARCHAR(1000),
    file_size   BIGINT,
    uploader_id BIGINT,
    created_at  DATETIME(6),
    updated_at  DATETIME(6),
    KEY idx_committee_files_meeting (meeting_id),
    FOREIGN KEY (meeting_id) REFERENCES committee_meetings(id),
    FOREIGN KEY (uploader_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 내부 감사
CREATE TABLE IF NOT EXISTS internal_audits (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    year        INT NOT NULL,
    title       VARCHAR(500) NOT NULL,
    description TEXT,
    auditor     VARCHAR(300),
    audit_start DATE,
    audit_end   DATE,
    status      ENUM('PLANNED', 'IN_PROGRESS', 'COMPLETED') NOT NULL DEFAULT 'PLANNED',
    created_by  BIGINT,
    created_at  DATETIME(6),
    updated_at  DATETIME(6),
    KEY idx_internal_audits_year (year),
    FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 내부 감사 대상
CREATE TABLE IF NOT EXISTS audit_targets (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    audit_id    BIGINT NOT NULL,
    target_name VARCHAR(300) NOT NULL,
    target_type VARCHAR(100),
    description TEXT,
    sort_order  INT,
    created_at  DATETIME(6),
    updated_at  DATETIME(6),
    KEY idx_audit_targets_audit (audit_id),
    FOREIGN KEY (audit_id) REFERENCES internal_audits(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 내부 감사 체크항목
CREATE TABLE IF NOT EXISTS audit_items (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    audit_id        BIGINT NOT NULL,
    target_id       BIGINT,
    item_name       VARCHAR(500) NOT NULL,
    check_method    TEXT,
    result          ENUM('PASS', 'FAIL', 'NA'),
    finding         TEXT,
    action_required TEXT,
    sort_order      INT,
    created_at      DATETIME(6),
    updated_at      DATETIME(6),
    KEY idx_audit_items_audit (audit_id),
    FOREIGN KEY (audit_id) REFERENCES internal_audits(id),
    FOREIGN KEY (target_id) REFERENCES audit_targets(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 내부 감사 첨부파일
CREATE TABLE IF NOT EXISTS audit_files (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    audit_id    BIGINT NOT NULL,
    title       VARCHAR(500) NOT NULL,
    file_name   VARCHAR(500),
    file_path   VARCHAR(1000),
    file_size   BIGINT,
    uploader_id BIGINT,
    created_at  DATETIME(6),
    updated_at  DATETIME(6),
    KEY idx_audit_files_audit (audit_id),
    FOREIGN KEY (audit_id) REFERENCES internal_audits(id),
    FOREIGN KEY (uploader_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 지적 사항 (감사 결과)
CREATE TABLE IF NOT EXISTS security_findings (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    year             INT NOT NULL,
    audit_type       ENUM('INTERNAL', 'ISMS_P', 'OTHER') NOT NULL,
    finding_summary  VARCHAR(500) NOT NULL,
    finding_detail   TEXT,
    domain           VARCHAR(200),
    requirement_code VARCHAR(50),
    requirement_name VARCHAR(300),
    risk_level       ENUM('CRITICAL', 'HIGH', 'MEDIUM', 'LOW') NOT NULL,
    status           ENUM('OPEN', 'IN_PROGRESS', 'RESOLVED', 'ACCEPTED') NOT NULL DEFAULT 'OPEN',
    corrective_action TEXT,
    action_deadline  DATE,
    resolver         VARCHAR(300),
    resolved_at      DATE,
    file_name        VARCHAR(500),
    file_path        VARCHAR(1000),
    file_size        BIGINT,
    created_by       BIGINT,
    created_at       DATETIME(6),
    updated_at       DATETIME(6),
    KEY idx_sec_findings_year (year),
    KEY idx_sec_findings_status (status),
    FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 피싱 훈련 템플릿
CREATE TABLE IF NOT EXISTS phishing_templates (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    category     VARCHAR(255) NOT NULL,
    difficulty   ENUM('EASY', 'MEDIUM', 'HARD') NOT NULL DEFAULT 'MEDIUM',
    subject      VARCHAR(255) NOT NULL,
    sender_name  VARCHAR(255) NOT NULL,
    sender_email VARCHAR(255) NOT NULL,
    body_html    LONGTEXT NOT NULL,
    description  TEXT,
    created_by   BIGINT,
    created_at   DATETIME(6),
    updated_at   DATETIME(6),
    FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 피싱 훈련 대상자
CREATE TABLE IF NOT EXISTS phishing_targets (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    department VARCHAR(100),
    position   VARCHAR(100),
    active     BIT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6),
    updated_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 피싱 훈련 캠페인
CREATE TABLE IF NOT EXISTS phishing_campaigns (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    description  TEXT,
    template_id  BIGINT NOT NULL,
    status       ENUM('DRAFT', 'RUNNING', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'DRAFT',
    scheduled_at DATETIME(6),
    created_by   BIGINT NOT NULL,
    created_at   DATETIME(6),
    updated_at   DATETIME(6),
    FOREIGN KEY (template_id) REFERENCES phishing_templates(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 피싱 캠페인 발송 대상
CREATE TABLE IF NOT EXISTS phishing_campaign_targets (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id    BIGINT NOT NULL,
    target_id      BIGINT NOT NULL,
    tracking_token VARCHAR(64) UNIQUE,
    sent_at        DATETIME(6),
    opened_at      DATETIME(6),
    clicked_at     DATETIME(6),
    reported_at    DATETIME(6),
    FOREIGN KEY (campaign_id) REFERENCES phishing_campaigns(id),
    FOREIGN KEY (target_id) REFERENCES phishing_targets(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 위협 카탈로그 (사용자 등록)
CREATE TABLE IF NOT EXISTS threats (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    type         VARCHAR(50) NOT NULL,
    category     VARCHAR(100),
    asset_detail VARCHAR(100),
    likelihood   INT NOT NULL DEFAULT 3,
    impact       INT NOT NULL DEFAULT 3,
    description  TEXT,
    remark       VARCHAR(500),
    created_at   DATETIME(6),
    updated_at   DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 위협 카탈로그 기본 항목
CREATE TABLE IF NOT EXISTS threat_defaults (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    risk_id      VARCHAR(20) NOT NULL,
    name         VARCHAR(255) NOT NULL,
    type         VARCHAR(50) NOT NULL DEFAULT '',
    category     VARCHAR(100) NOT NULL DEFAULT '',
    asset_detail VARCHAR(100),
    likelihood   INT NOT NULL DEFAULT 3,
    impact       INT NOT NULL DEFAULT 3,
    description  TEXT,
    UNIQUE KEY uq_threat_defaults_name_type_cat (name(191), type, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 정보보호의 날 점검 기본 항목
CREATE TABLE IF NOT EXISTS monthly_check_defaults (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    priority      ENUM('HIGH', 'MEDIUM', 'LOW') NOT NULL DEFAULT 'HIGH',
    category      VARCHAR(50) NOT NULL,
    item_name     VARCHAR(200) NOT NULL,
    check_method  VARCHAR(500),
    check_example VARCHAR(500),
    sort_order    INT NOT NULL DEFAULT 0,
    active        TINYINT(1) NOT NULL DEFAULT 1,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 위험평가 차수
CREATE TABLE IF NOT EXISTS risk_assessment_rounds (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    year        INT NOT NULL,
    round_no    INT NOT NULL DEFAULT 1,
    round_date  DATE NOT NULL,
    title       VARCHAR(255),
    status      ENUM('IN_PROGRESS', 'COMPLETED') NOT NULL DEFAULT 'IN_PROGRESS',
    created_by  BIGINT,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_year_round (year, round_no),
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 위험평가 항목
CREATE TABLE IF NOT EXISTS risk_assessments (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    round_id      BIGINT NOT NULL,
    asset_id      BIGINT,
    asset_name    VARCHAR(255),
    asset_type    VARCHAR(100),
    threat_id     BIGINT,
    threat_name   VARCHAR(255),
    threat_type   VARCHAR(100),
    vulnerability VARCHAR(255),
    likelihood    INT NOT NULL DEFAULT 3,
    impact        INT NOT NULL DEFAULT 3,
    risk_grade    ENUM('HIGH', 'MEDIUM', 'LOW') NOT NULL DEFAULT 'MEDIUM',
    treatment     ENUM('수용', '감소', '회피', '이전') DEFAULT '감소',
    notes         TEXT,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY round_id (round_id),
    FOREIGN KEY (round_id) REFERENCES risk_assessment_rounds(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ISMS-P 통제항목-정책 매핑 (v1.44.0)
CREATE TABLE IF NOT EXISTS isms_policy_mappings (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    isms_item_id BIGINT NOT NULL,
    policy_id    BIGINT NOT NULL,
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_isms_policy (isms_item_id, policy_id),
    CONSTRAINT fk_ipm_isms_item FOREIGN KEY (isms_item_id) REFERENCES isms_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_ipm_policy    FOREIGN KEY (policy_id)    REFERENCES policies (id)    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
