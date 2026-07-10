-- 수탁사 점검을 건별 이력으로 보관하도록 (contractor_id, check_year) 유니크 제약 제거
-- (기존: 한 수탁사·연도당 1건만 가능 → 재점검 시 덮어써짐 / 변경: 건별 이력 누적)
ALTER TABLE contractor_checks DROP INDEX uk_contractor_year;
