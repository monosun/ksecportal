-- v1.28.0 — 개인정보 컬럼 암호화(AES-256-GCM) 대비 컬럼 길이 확대
--
-- 암호문은 'PIENC1:' + Base64(IV ‖ 암호문 ‖ 태그) 형식이라 원문보다 길다.
-- ddl-auto: update 는 컬럼 길이를 늘려주지 않으므로 기존 DB에는 이 스크립트를 반드시 실행해야 한다.
-- 실행 후 백엔드를 재기동하면 기존 평문 행이 자동으로 암호화된다(PiColumnEncryptionBackfill).
--
-- 실행: docker compose exec -T db mysql -usecportal -psecportal123 secportal < db/migration/v1.28.0_pi_column_encryption.sql

-- 정보주체 권리행사 — 정보주체명·연락처
ALTER TABLE privacy_rights_requests
    MODIFY COLUMN subject_name VARCHAR(512) NOT NULL,
    MODIFY COLUMN contact      VARCHAR(512) NULL;

-- 수탁사 — 사업자등록번호·담당자·이메일·연락처
ALTER TABLE privacy_contractors
    MODIFY COLUMN business_number VARCHAR(255) NULL,
    MODIFY COLUMN contact_person  VARCHAR(512) NULL,
    MODIFY COLUMN contact_email   VARCHAR(512) NULL,
    MODIFY COLUMN contact_phone   VARCHAR(512) NULL;
