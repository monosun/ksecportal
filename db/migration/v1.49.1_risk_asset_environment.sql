-- v1.49.1: 위험평가 자산 환경(운영/개발 등) 스냅샷 컬럼 추가
ALTER TABLE risk_assessments
    ADD COLUMN asset_environment VARCHAR(50) NULL AFTER asset_type;
