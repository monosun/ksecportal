-- ISMS-P 통제항목 ↔ 보안정책 매핑을 조(條) 단위까지 세분화
--
-- isms_policy_mappings.policy_article_id 가 NULL 이면 기존과 같은 "장(章) 전체" 매핑,
-- 값이 있으면 그 장 안의 조 하나를 가리킨다.
--
-- 적용:
--   docker compose exec -T db mysql -usecportal -psecportal123 secportal < db/migration/v1.58.0_isms_article_mapping.sql
--
-- *** 이 마이그레이션은 필수다 ***
-- ddl-auto=update 는 컬럼만 추가할 뿐 기존 유니크 제약 (isms_item_id, policy_id) 을 지우지 못한다.
-- 제약이 남아 있으면 같은 장의 두 번째 조를 매핑할 때 Duplicate entry 로 실패한다.
--
-- 스크립트는 여러 번 실행해도 안전하다(이미 반영된 항목은 건너뛴다).

DROP PROCEDURE IF EXISTS migrate_isms_article_mapping;
DELIMITER //
CREATE PROCEDURE migrate_isms_article_mapping()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE idx VARCHAR(191);

    -- 정확히 (isms_item_id, policy_id) 두 컬럼만으로 이뤄진 유니크 인덱스.
    -- db/init 의 uq_isms_policy 와 Hibernate 가 만든 UK... 는 이름이 환경마다 다를 수 있어 동적으로 찾는다.
    DECLARE legacy_uk CURSOR FOR
        SELECT s.INDEX_NAME
          FROM information_schema.STATISTICS s
         WHERE s.TABLE_SCHEMA = DATABASE()
           AND s.TABLE_NAME   = 'isms_policy_mappings'
           AND s.NON_UNIQUE   = 0
           AND s.INDEX_NAME  <> 'PRIMARY'
         GROUP BY s.INDEX_NAME
        HAVING COUNT(*) = 2
           AND SUM(s.COLUMN_NAME IN ('isms_item_id', 'policy_id')) = 2;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    -- 1) 조 참조 컬럼 (백엔드를 먼저 올렸다면 ddl-auto 가 이미 추가했을 수 있다)
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'isms_policy_mappings'
                      AND COLUMN_NAME = 'policy_article_id') THEN
        ALTER TABLE isms_policy_mappings
            ADD COLUMN policy_article_id BIGINT NULL COMMENT '조 단위 매핑 대상 (NULL 이면 장 전체)';
    END IF;

    -- 2) 조까지 포함한 유니크 제약을 **먼저** 만든다.
    --    이 인덱스가 isms_item_id 를 선두 컬럼으로 가져 fk_ipm_isms_item 을 떠받치므로,
    --    이걸 만들기 전에 기존 유니크 인덱스를 지우면 "needed in a foreign key constraint" 로 실패한다.
    --    (MySQL 은 NULL 을 서로 다른 값으로 보므로 장 전체 매핑의 중복은 애플리케이션에서 막는다)
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'isms_policy_mappings'
                      AND INDEX_NAME = 'uq_isms_policy_article') THEN
        ALTER TABLE isms_policy_mappings
            ADD UNIQUE KEY uq_isms_policy_article (isms_item_id, policy_id, policy_article_id);
    END IF;

    -- 3) 조 삭제 시 매핑 동반 삭제
    IF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'isms_policy_mappings'
                      AND CONSTRAINT_NAME = 'fk_ipm_policy_article') THEN
        ALTER TABLE isms_policy_mappings
            ADD CONSTRAINT fk_ipm_policy_article FOREIGN KEY (policy_article_id)
                REFERENCES policy_articles (id) ON DELETE CASCADE;
    END IF;

    -- 4) 이제 기존 (isms_item_id, policy_id) 유니크 제약을 지운다.
    OPEN legacy_uk;
    drop_loop: LOOP
        FETCH legacy_uk INTO idx;
        IF done = 1 THEN LEAVE drop_loop; END IF;
        SET @sql = CONCAT('ALTER TABLE isms_policy_mappings DROP INDEX `', idx, '`');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END LOOP;
    CLOSE legacy_uk;
END //
DELIMITER ;

CALL migrate_isms_article_mapping();
DROP PROCEDURE migrate_isms_article_mapping;
