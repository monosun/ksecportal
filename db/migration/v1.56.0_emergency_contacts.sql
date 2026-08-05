-- v1.56.0: 비상연락망 (보안 운영)
--
-- 기본 연락 계통과 외부 신고기관 연락처는 EmergencyContactInitializer(seed-when-empty)가
-- 기동 시 채우므로 여기서 INSERT 하지 않는다.
-- mobile·email 은 EncryptedStringConverter(AES-256-GCM)로 암호문이 저장되므로 길이를 넉넉히 잡는다.

-- 연락망 그룹 (상황별·조직별 연락 계통)
CREATE TABLE IF NOT EXISTS emergency_contact_groups (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    contact_type ENUM('INTERNAL','EXTERNAL','PARTNER') NOT NULL DEFAULT 'INTERNAL',
    description  TEXT,
    sort_order   INT NOT NULL DEFAULT 0,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 연락처 (휴대전화·이메일은 저장 시 암호화)
CREATE TABLE IF NOT EXISTS emergency_contacts (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id      BIGINT NOT NULL,
    name          VARCHAR(255) NOT NULL,
    organization  VARCHAR(255),
    department    VARCHAR(255),
    position      VARCHAR(255),
    role_name     VARCHAR(255),
    contact_order INT NOT NULL DEFAULT 1,
    mobile        VARCHAR(512),
    office_phone  VARCHAR(100),
    email         VARCHAR(512),
    available24h  BOOLEAN NOT NULL DEFAULT FALSE,
    note          TEXT,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY group_id (group_id),
    FOREIGN KEY (group_id) REFERENCES emergency_contact_groups(id) ON DELETE CASCADE
);
