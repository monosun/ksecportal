-- SecPortal v1.27.0 마이그레이션
-- 월간 점검항목 담당자 및 증적 파일 관리 기능

-- 1. monthly_check_items 담당자 컬럼 추가
ALTER TABLE monthly_check_items
    ADD COLUMN IF NOT EXISTS assignee_id   BIGINT       NULL AFTER sort_order,
    ADD COLUMN IF NOT EXISTS assignee_text VARCHAR(100) NULL AFTER assignee_id;

ALTER TABLE monthly_check_items
    ADD CONSTRAINT fk_mci_assignee
        FOREIGN KEY (assignee_id) REFERENCES users(id) ON DELETE SET NULL;

-- 2. monthly_check_evidences 신규 테이블
CREATE TABLE IF NOT EXISTS monthly_check_evidences (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    check_item_id  BIGINT       NOT NULL,
    title          VARCHAR(200) NOT NULL,
    content        TEXT,
    file_name      VARCHAR(500),
    file_path      VARCHAR(1000),
    uploaded_by    BIGINT,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (check_item_id) REFERENCES monthly_check_items(id) ON DELETE CASCADE,
    FOREIGN KEY (uploaded_by)   REFERENCES users(id) ON DELETE SET NULL
);
