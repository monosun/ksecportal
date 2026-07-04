-- v1.54.0: Okta SSO 연동을 위한 okta_id 컬럼 추가
ALTER TABLE users ADD COLUMN okta_id VARCHAR(255) UNIQUE NULL AFTER locked_until;
