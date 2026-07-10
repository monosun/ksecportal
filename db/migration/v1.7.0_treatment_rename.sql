-- 위험평가 처리방법 '경감' → '감소' 용어 변경
-- ENUM 컬럼이므로 ① 신규 값 임시 추가 → ② 데이터 이관 → ③ ENUM 확정(기본값 포함) 순서로 처리한다.

-- ① '감소'를 허용 목록에 임시로 추가
ALTER TABLE risk_assessments
    MODIFY COLUMN treatment ENUM('수용', '경감', '회피', '이전', '감소') DEFAULT '경감';

-- ② 기존 '경감' 데이터를 '감소'로 이관
UPDATE risk_assessments SET treatment = '감소' WHERE treatment = '경감';

-- ③ ENUM을 최종 값으로 확정하고 기본값도 '감소'로 변경
ALTER TABLE risk_assessments
    MODIFY COLUMN treatment ENUM('수용', '감소', '회피', '이전') DEFAULT '감소';
