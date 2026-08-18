-- v1.36.0 — ISMS-P 항목에 '예시 증적자료명' 컬럼 추가
--
-- 이행 가이드와 함께 표시할, 인증기준별로 통상 요구되는 증적 문서명 목록이다.
-- 줄바꿈으로 구분된 한 줄이 자료명 하나다.
--
-- 값 자체는 IsmsDataInitializer 가 기동할 때 isms_items.json 시드로부터
-- '비어 있는 항목만' 채우므로(seed-when-empty), 이 스크립트는 컬럼만 추가한다.
-- ddl-auto: update 로도 컬럼은 생성되지만, 컬럼 코멘트를 남기기 위해 명시한다.
--
-- 실행: docker compose exec -T db mysql -usecportal -psecportal123 secportal < db/migration/v1.36.0_isms_evidence_examples.sql

SET NAMES utf8mb4;

SET @exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'isms_items'
      AND COLUMN_NAME = 'evidence_examples'
);

SET @ddl := IF(@exists = 0,
    "ALTER TABLE isms_items ADD COLUMN evidence_examples TEXT COMMENT '예시 증적자료명 (줄바꿈 구분)' AFTER default_evidence_content",
    "SELECT '이미 존재하는 컬럼입니다 — 건너뜁니다' AS msg");

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
