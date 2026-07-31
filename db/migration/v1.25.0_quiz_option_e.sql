-- v1.25.0 — 퀴즈 보기 E 추가 (5지선다 지원)
-- 문제은행·교육 문항에 보기E 컬럼을 추가하고, 정답 컬럼을 'A,B,C,D,E'(9자)까지 담을 수 있게 확장한다.
-- 실행: docker compose exec -T db mysql -usecportal -psecportal123 secportal < db/migration/v1.25.0_quiz_option_e.sql

ALTER TABLE quiz_bank_questions
    ADD COLUMN option_e VARCHAR(500) NULL AFTER option_d,
    MODIFY COLUMN correct_answer VARCHAR(9) NOT NULL;

ALTER TABLE quiz_questions
    ADD COLUMN option_e VARCHAR(500) NULL AFTER option_d,
    MODIFY COLUMN correct_answer VARCHAR(9) NOT NULL;
