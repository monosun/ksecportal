-- 개인정보 항목별 마스킹 기준 컬럼 추가
-- 적용: docker compose exec -T db mysql -usecportal -psecportal123 secportal < db/migration/v1.27.0_code_value_masking.sql
--
-- 참고: spring.jpa.hibernate.ddl-auto=update 환경에서는 백엔드 재기동 시 컬럼이 자동 추가되므로
--       이 스크립트는 ddl-auto 를 사용하지 않는 환경에서만 필요하다.
--       항목별 마스킹 기준 기본값은 PiMaskingDefaultInitializer 가 기동 시 자동으로 채운다
--       (마스킹 방식이 비어 있는 항목만 채우므로 관리자가 수정한 값은 유지된다).

ALTER TABLE code_values ADD COLUMN masking_type    VARCHAR(30)  NULL AFTER description;
ALTER TABLE code_values ADD COLUMN masking_rule    VARCHAR(300) NULL AFTER masking_type;
ALTER TABLE code_values ADD COLUMN masking_example VARCHAR(100) NULL AFTER masking_rule;
