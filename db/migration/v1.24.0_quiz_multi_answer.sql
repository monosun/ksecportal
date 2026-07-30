-- v1.24.0 — 퀴즈 복수 정답 허용
-- 정답 컬럼을 단일 문자(A~D)에서 복수 정답("A,C" 형태, 콤마 구분 정렬)까지 저장하도록 확장한다.
-- 기존 단일 정답 값('A' 등)은 그대로 유효하므로 데이터 변환은 필요 없다.

ALTER TABLE quiz_bank_questions
    MODIFY COLUMN correct_answer VARCHAR(7) NOT NULL;

ALTER TABLE quiz_questions
    MODIFY COLUMN correct_answer VARCHAR(7) NOT NULL;
