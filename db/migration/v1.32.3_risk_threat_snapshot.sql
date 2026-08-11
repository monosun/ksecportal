-- v1.32.3: 위험평가 항목에 위협 카테고리 / 대상 자산유형 스냅샷 컬럼 추가 + 기존 데이터 백필
ALTER TABLE risk_assessments
    ADD COLUMN threat_category    VARCHAR(100) NULL AFTER threat_type,
    ADD COLUMN threat_asset_types VARCHAR(500) NULL AFTER threat_category;

-- 기존 평가 항목: threat_id 로 연결된 위협의 현재 값으로 백필
UPDATE risk_assessments ra
    JOIN threats t ON ra.threat_id = t.id
SET ra.threat_category    = t.category,
    ra.threat_asset_types = t.asset_types
WHERE ra.threat_id IS NOT NULL
  AND ra.threat_category IS NULL
  AND ra.threat_asset_types IS NULL;
