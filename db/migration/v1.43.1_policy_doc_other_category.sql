-- v1.43.1 보안정책관리 — 분류에 '기타(OTHER)' 추가
-- policy_mgmt_docs.category 는 Hibernate 가 MySQL ENUM 으로 만들었고 ddl-auto: update 는 ENUM 값을 늘리지 않으므로 직접 확장한다.
-- 값 추가만 하므로 기존 데이터에 영향 없음. 여러 번 실행해도 안전.
--   docker compose exec -T db mysql -usecportal -psecportal123 secportal < db/migration/v1.43.1_policy_doc_other_category.sql

ALTER TABLE policy_mgmt_docs
    MODIFY category ENUM('REGULATION','GUIDELINE','GUIDE','PROCEDURE','MANUAL','OTHER') NOT NULL;

-- 문서번호 미입력 문서는 백엔드 기동 시 PolicyDocNumberInitializer 가 분류 키워드 번호(REG-001 등)로 자동 채운다.
