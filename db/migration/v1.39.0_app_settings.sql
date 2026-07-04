-- v1.39.0: app_settings table for global settings (login logo, etc.)
CREATE TABLE IF NOT EXISTS app_settings (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key   VARCHAR(100)  NOT NULL UNIQUE,
    setting_value MEDIUMTEXT,
    description   VARCHAR(255),
    updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO app_settings (setting_key, setting_value, description) VALUES
('login_logo',      'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCA4MCA4MCIgd2lkdGg9IjgwIiBoZWlnaHQ9IjgwIj48ZGVmcz48bGluZWFyR3JhZGllbnQgaWQ9ImJnIiB4MT0iMCIgeTE9IjAiIHgyPSIwIiB5Mj0iMSI+PHN0b3Agb2Zmc2V0PSIwIiBzdG9wLWNvbG9yPSIjMWUzYThhIi8+PHN0b3Agb2Zmc2V0PSIxIiBzdG9wLWNvbG9yPSIjMzEyZTgxIi8+PC9saW5lYXJHcmFkaWVudD48L2RlZnM+PHJlY3Qgd2lkdGg9IjgwIiBoZWlnaHQ9IjgwIiByeD0iMjAiIGZpbGw9InVybCgjYmcpIi8+PHBhdGggZD0iTTQwIDggTDE0IDE4IEwxNCA0MCBDMTQgNTcgMjUgNzAgNDAgNzYgQzU1IDcwIDY2IDU3IDY2IDQwIEw2NiAxOCBaIiBmaWxsPSJ3aGl0ZSIgb3BhY2l0eT0iMC45NSIvPjxyZWN0IHg9IjMwIiB5PSI0NiIgd2lkdGg9IjIwIiBoZWlnaHQ9IjE2IiByeD0iMyIgZmlsbD0iIzFlM2E4YSIvPjxwYXRoIGQ9Ik0zNCA0NiBMMzQgMzggQzM0IDMxIDQ2IDMxIDQ2IDM4IEw0NiA0NiIgZmlsbD0ibm9uZSIgc3Ryb2tlPSIjMWUzYThhIiBzdHJva2Utd2lkdGg9IjMuNSIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIi8+PGNpcmNsZSBjeD0iNDAiIGN5PSI1NCIgcj0iMi41IiBmaWxsPSJ3aGl0ZSIvPjxyZWN0IHg9IjM4LjUiIHk9IjU0IiB3aWR0aD0iMyIgaGVpZ2h0PSI1IiByeD0iMSIgZmlsbD0id2hpdGUiLz48L3N2Zz4=', 'Login screen logo (base64 data URL)'),
('login_logo_text', 'SecPortal', 'Logo text shown on login screen and sidebar')
ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value);
