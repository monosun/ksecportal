-- ============================================================
-- 위협 기본 항목(threat_defaults) 중복 정리 + 위협명 #일련번호 제거
--
-- 배경: 초기 시드가 14개 위협명을 40회 반복하며 '계정 탈취 #1' 처럼
--       전역 일련번호를 붙여 생성해 두어, 번호를 떼면 동일 항목이 최대 6건씩
--       중복되어 있었다(558건 → 실제 고유 조합 140건).
--
-- 처리:
--   1) (위협명(#이하 제외), 유형, 카테고리, 발생가능성, 잠재영향) 기준 중복 제거
--      — 각 그룹에서 가장 작은 id 1건만 유지 (418건 삭제)
--   2) 유니크 제약을 name+type+category → 위 5개 컬럼으로 확장
--      (같은 위협명이라도 위험도 조합이 다르면 별도 항목으로 유지하기 위함)
--   3) 남은 항목의 위협명에서 ' #숫자' 접미사 제거
--
-- 결과: 558건 → 140건 (삭제 418건), 위협명 14종
-- ============================================================

SET NAMES utf8mb4;

-- 1) 중복 제거 — 각 그룹의 최소 id 유지
DELETE t FROM threat_defaults t
WHERE t.id NOT IN (
  SELECT keep_id FROM (
    SELECT MIN(id) AS keep_id
    FROM threat_defaults
    GROUP BY TRIM(SUBSTRING_INDEX(name, '#', 1)), type, category, likelihood, impact
  ) k
);

-- 2) 유니크 제약 확장 (기존 제약은 #숫자 제거 후 충돌하므로 반드시 먼저 교체)
ALTER TABLE threat_defaults DROP INDEX uq_threat_defaults_name_type_cat;

ALTER TABLE threat_defaults
  ADD CONSTRAINT uq_threat_defaults_name_type_cat_risk
  UNIQUE (name(191), type(50), category(100), likelihood, impact);

-- 3) 위협명에서 ' #숫자' 접미사 제거
UPDATE threat_defaults
SET name = TRIM(SUBSTRING_INDEX(name, '#', 1))
WHERE name LIKE '%#%';
