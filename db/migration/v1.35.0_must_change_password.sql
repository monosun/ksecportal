-- v1.35.0: Add must_change_password flag to users table
-- Existing admin accounts should set this to TRUE to force a password reset.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
