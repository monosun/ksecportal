SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================================
-- 앱 기본 설정값 (app_settings)
-- ============================================================

INSERT INTO app_settings (setting_key, setting_value, description) VALUES
('session_timeout_minutes', '60',
 'Session timeout in minutes (min 5, max 1440)')
ON DUPLICATE KEY UPDATE description = VALUES(description);

INSERT INTO app_settings (setting_key, setting_value, description) VALUES
('login_logo_text', 'SecPortal',
 'Logo text shown on login screen and sidebar')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- 기본 로그인 로고 (방패 아이콘 SVG, 관리자가 환경설정에서 교체 가능)
INSERT INTO app_settings (setting_key, setting_value, description) VALUES
('login_logo',
 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCA4MCA4MCIgd2lkdGg9IjgwIiBoZWlnaHQ9IjgwIj48ZGVmcz48bGluZWFyR3JhZGllbnQgaWQ9ImJnIiB4MT0iMCIgeTE9IjAiIHgyPSIwIiB5Mj0iMSI+PHN0b3Agb2Zmc2V0PSIwIiBzdG9wLWNvbG9yPSIjMWUzYThhIi8+PHN0b3Agb2Zmc2V0PSIxIiBzdG9wLWNvbG9yPSIjMzEyZTgxIi8+PC9saW5lYXJHcmFkaWVudD48L2RlZnM+PHJlY3Qgd2lkdGg9IjgwIiBoZWlnaHQ9IjgwIiByeD0iMjAiIGZpbGw9InVybCgjYmcpIi8+PHBhdGggZD0iTTQwIDggTDE0IDE4IEwxNCA0MCBDMTQgNTcgMjUgNzAgNDAgNzYgQzU1IDcwIDY2IDU3IDY2IDQwIEw2NiAxOCBaIiBmaWxsPSJ3aGl0ZSIgb3BhY2l0eT0iMC45NSIvPjxyZWN0IHg9IjMwIiB5PSI0NiIgd2lkdGg9IjIwIiBoZWlnaHQ9IjE2IiByeD0iMyIgZmlsbD0iIzFlM2E4YSIvPjxwYXRoIGQ9Ik0zNCA0NiBMMzQgMzggQzM0IDMxIDQ2IDMxIDQ2IDM4IEw0NiA0NiIgZmlsbD0ibm9uZSIgc3Ryb2tlPSIjMWUzYThhIiBzdHJva2Utd2lkdGg9IjMuNSIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIi8+PGNpcmNsZSBjeD0iNDAiIGN5PSI1NCIgcj0iMi41IiBmaWxsPSJ3aGl0ZSIvPjxyZWN0IHg9IjM4LjUiIHk9IjU0IiB3aWR0aD0iMyIgaGVpZ2h0PSI1IiByeD0iMSIgZmlsbD0id2hpdGUiLz48L3N2Zz4=',
 'Login screen logo (base64 data URL)')
ON DUPLICATE KEY UPDATE description = VALUES(description);
