INSERT INTO app_settings (setting_key, setting_value, description)
VALUES ('session_timeout_minutes', '60', 'Session timeout in minutes (min 5, max 1440)')
ON DUPLICATE KEY UPDATE description = VALUES(description);
