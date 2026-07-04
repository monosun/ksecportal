SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- Default admin user (password: Ksecurity!!!, must be changed on first login)
-- BCrypt hash of "Ksecurity!!!"
INSERT INTO users (email, password, name, role, department, must_change_password) VALUES
('secportal@monosun.com', '$2b$10$1eRhoyXuEomtPQBCMMjuDOXTgvz1uZ4LjF9hGC0zk585hdxK3d6nu', 'System Admin', 'ADMIN', 'IT Security', TRUE);

-- Sample policies
INSERT INTO policies (title, content, category, status, version, effective_date, author_id) VALUES
('정보보안 기본 정책', '# 정보보안 기본 정책\n\n## 1. 목적\n본 정책은 조직의 정보 자산을 보호하고 정보보안을 강화하기 위한 기본 원칙과 지침을 제시합니다.\n\n## 2. 적용 범위\n본 정책은 모든 임직원, 협력업체 직원, 외부 방문자에게 적용됩니다.\n\n## 3. 기본 원칙\n- 정보의 기밀성, 무결성, 가용성을 보장합니다.\n- 모든 임직원은 정보보안 교육을 이수해야 합니다.\n- 보안 사고 발생 시 즉시 보고해야 합니다.', 'GENERAL', 'PUBLISHED', '1.0', CURDATE(), 1),
('비밀번호 관리 정책', '# 비밀번호 관리 정책\n\n## 1. 비밀번호 복잡성 요구사항\n- 최소 8자 이상\n- 대문자, 소문자, 숫자, 특수문자 포함\n- 이전 5개 비밀번호 재사용 금지\n\n## 2. 비밀번호 변경 주기\n- 90일마다 변경 필수\n- 의심스러운 상황 발생 시 즉시 변경', 'ACCESS_CONTROL', 'PUBLISHED', '1.0', CURDATE(), 1);

-- Sample vulnerability
INSERT INTO vulnerabilities (title, description, cve_id, cvss_score, severity, status, asset_name, reporter_id) VALUES
('OpenSSL 취약점 점검 필요', 'CVE-2024-XXXX에 해당하는 OpenSSL 버전이 운영 서버에 설치되어 있어 패치가 필요합니다.', 'CVE-2024-0001', 7.5, 'HIGH', 'OPEN', 'prod-server-01', 1);

-- Sample training course
INSERT INTO training_courses (title, description, content_type, mandatory, created_by) VALUES
('정보보안 기초 교육', '모든 임직원이 반드시 이수해야 하는 기본 정보보안 교육 과정입니다. 개인정보 보호, 비밀번호 관리, 피싱 대응 방법을 다룹니다.', 'QUIZ_ONLY', TRUE, 1);

INSERT INTO quiz_questions (course_id, question, option_a, option_b, option_c, option_d, correct_answer, sort_order) VALUES
(1, '피싱 이메일을 받았을 때 가장 적절한 대응은?', '즉시 링크를 클릭하여 확인한다', '보안팀에 신고하고 이메일을 삭제한다', '동료에게 전달하여 의견을 구한다', '답장을 보내 발신자를 확인한다', 'B', 1),
(1, '비밀번호에 대한 올바른 설명은?', '여러 사이트에 동일한 비밀번호를 사용한다', '비밀번호를 메모지에 적어 모니터에 붙여둔다', '대문자, 소문자, 숫자, 특수문자를 조합하여 사용한다', '비밀번호를 동료와 공유한다', 'C', 2),
(1, '공공장소 Wi-Fi 사용 시 주의사항으로 올바른 것은?', '업무 문서를 자유롭게 열람할 수 있다', 'VPN을 사용하여 접속한다', '회사 계정으로 로그인해도 안전하다', '파일 공유 기능을 활성화한다', 'B', 3);
