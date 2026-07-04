-- v1.44.0: ISMS-P 통제항목-정책 매핑 테이블 추가
-- 기존 운영 DB에 적용하려면 이 파일을 수동으로 실행하세요.
-- 최초 설치(fresh install)는 db/init/07_extended_schema.sql에 이미 포함되어 있습니다.

CREATE TABLE IF NOT EXISTS isms_policy_mappings (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    isms_item_id BIGINT NOT NULL,
    policy_id    BIGINT NOT NULL,
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_isms_policy (isms_item_id, policy_id),
    CONSTRAINT fk_ipm_isms_item FOREIGN KEY (isms_item_id) REFERENCES isms_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_ipm_policy    FOREIGN KEY (policy_id)    REFERENCES policies (id)    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
