-- 보안정책관리 — 정책 문서(규정·지침·가이드·절차서·매뉴얼)와 버전별 파일·개정사항 (신규 설치용 스키마)
-- 운영 중인 DB 는 ddl-auto: update 가 테이블을 자동 생성한다.

CREATE TABLE IF NOT EXISTS policy_mgmt_docs (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_no         VARCHAR(50),
    title          VARCHAR(300) NOT NULL,
    category       ENUM('REGULATION','GUIDELINE','GUIDE','PROCEDURE','MANUAL','OTHER') NOT NULL,
    department     VARCHAR(100),
    owner          VARCHAR(100),
    description    TEXT,
    status         ENUM('ACTIVE','ABOLISHED') NOT NULL DEFAULT 'ACTIVE',
    registrant_id  BIGINT,
    created_at     DATETIME(6),
    updated_at     DATETIME(6),
    KEY idx_policy_mgmt_docs_category (category),
    KEY idx_policy_mgmt_docs_status (status),
    CONSTRAINT fk_policy_mgmt_docs_registrant FOREIGN KEY (registrant_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS policy_mgmt_revisions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    doc_id          BIGINT NOT NULL,
    version         VARCHAR(50) NOT NULL,
    revision_type   ENUM('ENACTMENT','AMENDMENT','ABOLITION') NOT NULL DEFAULT 'AMENDMENT',
    revision_date   DATE NOT NULL,
    effective_date  DATE,
    approver        VARCHAR(100),
    change_reason   TEXT,
    change_details  TEXT,
    file_name       VARCHAR(500),
    file_path       VARCHAR(1000),
    file_size       BIGINT,
    uploader_id     BIGINT,
    created_at      DATETIME(6),
    updated_at      DATETIME(6),
    KEY idx_policy_mgmt_revisions_doc (doc_id),
    CONSTRAINT fk_policy_mgmt_revisions_doc FOREIGN KEY (doc_id) REFERENCES policy_mgmt_docs(id),
    CONSTRAINT fk_policy_mgmt_revisions_uploader FOREIGN KEY (uploader_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
