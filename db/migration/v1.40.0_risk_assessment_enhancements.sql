-- v1.40.0: risk_assessments - add asset_type and threat_type snapshot columns
ALTER TABLE risk_assessments
  ADD COLUMN asset_type VARCHAR(100) AFTER asset_name,
  ADD COLUMN threat_type VARCHAR(100) AFTER threat_name;
