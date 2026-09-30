-- v1.42.0 — 보안 결함사항에 출처(직접 등록 / ISMS 결함관리 연동) 구분 추가
--
-- 보안 운영 > 보안 결함사항 화면이 ISMS-P 심사 결함과 그 외 보안결함을 함께 보여주도록 바뀌었다.
-- ISMS 결함관리(isms_defects)에 등록한 결함은 source_type='ISMS_DEFECT' 행으로 자동 반영되고,
-- 화면에서 직접 등록한 건은 source_type='MANUAL' 이다.
--
-- ddl-auto: update 는 컬럼만 추가할 뿐 기존 행 backfill·유니크 제약·인덱스는 만들지 않으므로
-- 운영 중인 DB 에는 이 스크립트를 적용할 것.
--
-- 실행: docker compose exec -T db mysql -usecportal -psecportal123 secportal < db/migration/v1.42.0_security_finding_source.sql

SET NAMES utf8mb4;

-- ── 1) source_type ──────────────────────────────────────────────────────────
SET @exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'security_findings' AND COLUMN_NAME = 'source_type'
);
SET @ddl := IF(@exists = 0,
    "ALTER TABLE security_findings ADD COLUMN source_type VARCHAR(20) COMMENT 'MANUAL=직접 등록, ISMS_DEFECT=ISMS 결함관리 연동' AFTER created_by",
    "SELECT '이미 존재하는 컬럼입니다 — 건너뜁니다' AS msg");
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ── 2) source_defect_id ─────────────────────────────────────────────────────
SET @exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'security_findings' AND COLUMN_NAME = 'source_defect_id'
);
SET @ddl := IF(@exists = 0,
    "ALTER TABLE security_findings ADD COLUMN source_defect_id BIGINT COMMENT '원본 isms_defects.id' AFTER source_type",
    "SELECT '이미 존재하는 컬럼입니다 — 건너뜁니다' AS msg");
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ── 3) source_label ─────────────────────────────────────────────────────────
SET @exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'security_findings' AND COLUMN_NAME = 'source_label'
);
SET @ddl := IF(@exists = 0,
    "ALTER TABLE security_findings ADD COLUMN source_label VARCHAR(100) COMMENT '출처 표시 라벨 (예: ISMS-P 갱신심사 · 결함-01)' AFTER source_defect_id",
    "SELECT '이미 존재하는 컬럼입니다 — 건너뜁니다' AS msg");
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ── 4) 기존 행 backfill — 이전에 쌓인 결함사항은 모두 직접 등록한 건이다 ──────
UPDATE security_findings SET source_type = 'MANUAL' WHERE source_type IS NULL;

-- ── 5) ISMS 결함 1건 = 결함사항 1건 보장 ────────────────────────────────────
SET @exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'security_findings'
      AND INDEX_NAME = 'uk_sec_findings_source_defect'
);
SET @ddl := IF(@exists = 0,
    "ALTER TABLE security_findings ADD UNIQUE KEY uk_sec_findings_source_defect (source_defect_id)",
    "SELECT '이미 존재하는 제약입니다 — 건너뜁니다' AS msg");
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ── 6) 목록 필터용 인덱스 ───────────────────────────────────────────────────
SET @exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'security_findings'
      AND INDEX_NAME = 'idx_sec_findings_source_type'
);
SET @ddl := IF(@exists = 0,
    "ALTER TABLE security_findings ADD KEY idx_sec_findings_source_type (source_type)",
    "SELECT '이미 존재하는 인덱스입니다 — 건너뜁니다' AS msg");
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
