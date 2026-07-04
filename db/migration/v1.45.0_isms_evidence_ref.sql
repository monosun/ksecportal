-- v1.45.0 마이그레이션: ISMS 증적 참조 등록
-- 기존 운영 DB에 적용할 경우 아래 명령 실행:
--   docker compose exec db mysql -usecportal -psecportal123 secportal < db/migration/v1.45.0_isms_evidence_ref.sql

ALTER TABLE isms_evidences
  ADD COLUMN source_evidence_id BIGINT NULL COMMENT '참조 증적 ID (자기참조)',
  ADD CONSTRAINT fk_ie_source FOREIGN KEY (source_evidence_id) REFERENCES isms_evidences(id) ON DELETE SET NULL;
