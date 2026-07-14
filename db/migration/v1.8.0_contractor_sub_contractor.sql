-- 수탁사: 재수탁사 컬럼 추가 + 위탁업무 길이 확장
-- 개인정보처리방침에서 수탁사 일괄등록 기능(v1.8.0)에 필요.
-- ddl-auto=update 는 컬럼 추가만 하고 기존 컬럼 타입은 바꾸지 않으므로
-- service_type 확장은 이 마이그레이션으로 직접 적용해야 한다.

ALTER TABLE privacy_contractors
    MODIFY COLUMN service_type VARCHAR(500) COMMENT '위탁 업무';

-- sub_contractor 는 ddl-auto 로도 추가되지만, 마이그레이션만 적용하는 환경을 위해 함께 정의한다.
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'privacy_contractors'
      AND COLUMN_NAME = 'sub_contractor'
);
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE privacy_contractors ADD COLUMN sub_contractor VARCHAR(500) COMMENT ''재수탁사'' AFTER service_type',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
