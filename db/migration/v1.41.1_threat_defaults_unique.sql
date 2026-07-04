-- NULL을 빈 문자열로 정규화 (type, category)
UPDATE threat_defaults SET type = '' WHERE type IS NULL;
UPDATE threat_defaults SET category = '' WHERE category IS NULL;

-- 중복 제거: name+type+category 기준으로 id가 큰 것 삭제 (가장 오래된 것 유지)
DELETE td1 FROM threat_defaults td1
INNER JOIN threat_defaults td2
  ON  td1.name     = td2.name
  AND td1.type     = td2.type
  AND td1.category = td2.category
  AND td1.id       > td2.id;

-- 유니크 제약 추가
ALTER TABLE threat_defaults
  MODIFY COLUMN type     VARCHAR(50)  NOT NULL DEFAULT '',
  MODIFY COLUMN category VARCHAR(100) NOT NULL DEFAULT '';

ALTER TABLE threat_defaults
  ADD CONSTRAINT uq_threat_defaults_name_type_cat
  UNIQUE (name(191), type(50), category(100));
