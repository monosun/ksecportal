-- 보안정책 조(條) 세분화
--
-- 정책 1건 = 장(章) 구조를 유지한 채, 본문의 "### 제N조(제목)" 을 조 단위 레코드로 펼친다.
-- 지침/장 컬럼은 제목("개인정보보호 지침 - 제1장 총칙")에서 파생되어 애플리케이션이 채운다.
--
-- 적용:
--   docker compose exec -T db mysql -usecportal -psecportal123 secportal < db/migration/v1.57.0_policy_articles.sql
--
-- 주의 1: ddl-auto=update 가 새 컬럼/테이블은 기동 시 자동 추가하므로,
--         백엔드를 먼저 올린 환경에서는 이 스크립트가 "Duplicate column" 으로 실패한다.
--         그 경우 이미 반영된 것이므로 건너뛰면 된다. (인덱스만 필요하면 CREATE INDEX 만 실행)
-- 주의 2: 이 스크립트는 스키마만 만든다. 조 데이터는 백엔드 기동 시
--       PolicyArticleInitializer(policy_articles 가 비어 있을 때) 가 채우거나,
--       화면의 "조문 재등록"(POST /api/policies/articles/resync) 으로 다시 만든다.

-- 1) 정책(장) 정규화 컬럼
ALTER TABLE policies
    ADD COLUMN guideline_name VARCHAR(200) NULL COMMENT '지침명 (예: 개인정보보호 지침)',
    ADD COLUMN chapter_no     INT          NULL COMMENT '장 번호 (부칙 등은 NULL)',
    ADD COLUMN chapter_label  VARCHAR(50)  NULL COMMENT '장 표기 (제1장 / 부칙)',
    ADD COLUMN chapter_title  VARCHAR(200) NULL COMMENT '장 제목 (총칙)';

CREATE INDEX idx_policies_guideline ON policies (guideline_name, chapter_no);

-- 2) 조 테이블
CREATE TABLE IF NOT EXISTS policy_articles (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    policy_id      BIGINT       NOT NULL,
    article_no     INT          NULL COMMENT '조 번호 (전문은 NULL)',
    article_sub_no INT          NULL COMMENT '가지번호 — 제3조의2 의 2',
    article_label  VARCHAR(50)  NOT NULL COMMENT '조 표기 (제1조 / 제3조의2 / 전문)',
    title          VARCHAR(300) NOT NULL DEFAULT '' COMMENT '조 제목 (목적)',
    note           VARCHAR(200) NULL COMMENT '조 꼬리말 (<개정 2024.5.28>)',
    content        LONGTEXT     NULL COMMENT '조 본문',
    sort_order     INT          NOT NULL DEFAULT 0,
    created_at     DATETIME     NULL,
    updated_at     DATETIME     NULL,
    KEY idx_policy_article_policy (policy_id),
    KEY idx_policy_article_no (article_no),
    CONSTRAINT fk_policy_article_policy FOREIGN KEY (policy_id) REFERENCES policies (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
