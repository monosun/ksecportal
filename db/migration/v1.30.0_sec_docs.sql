-- v1.30.0: 보안문서 관리 테이블

CREATE TABLE IF NOT EXISTS sec_docs (
    id             BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    document_key   VARCHAR(36)   NOT NULL,
    title          VARCHAR(500)  NOT NULL,
    description    TEXT,
    category       VARCHAR(50)   NOT NULL,
    version        VARCHAR(50)   NOT NULL DEFAULT '1.0',
    is_latest      BOOLEAN       NOT NULL DEFAULT TRUE,
    file_name      VARCHAR(500),
    file_path      VARCHAR(1000),
    file_size      BIGINT,
    uploader_id    BIGINT,
    created_at     DATETIME,
    updated_at     DATETIME,
    INDEX idx_sec_docs_doc_key  (document_key),
    INDEX idx_sec_docs_category (category),
    FOREIGN KEY (uploader_id) REFERENCES users(id) ON DELETE SET NULL
);
