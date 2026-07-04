-- v1.49.2: 기존 위험평가 항목의 자산 환경값 백필 (asset_id 연결된 레코드 대상)
UPDATE risk_assessments ra
    JOIN assets a ON ra.asset_id = a.id
SET ra.asset_environment = a.environment
WHERE ra.asset_environment IS NULL
  AND ra.asset_id IS NOT NULL;
