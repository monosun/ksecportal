SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- SBOM 관리: SW 정보
CREATE TABLE IF NOT EXISTS sbom_software (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    version     VARCHAR(100) NOT NULL,
    vendor      VARCHAR(200),
    description TEXT,
    remarks     TEXT,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_sbom_software_name_version UNIQUE (name, version)
);

-- SBOM 관리: SW 버전에 포함된 라이브러리(구성요소) — CycloneDX component 필드 기준
CREATE TABLE IF NOT EXISTS sbom_components (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    software_id     BIGINT NOT NULL,
    component_type  VARCHAR(30) NOT NULL DEFAULT 'library',  -- CycloneDX component.type
    group_name      VARCHAR(200),                            -- CycloneDX component.group
    library_name    VARCHAR(200) NOT NULL,                   -- CycloneDX component.name
    library_version VARCHAR(100),                            -- CycloneDX component.version
    purl            VARCHAR(500),                            -- CycloneDX component.purl
    license         VARCHAR(100),                            -- SPDX ID (CycloneDX licenses.license.id)
    remarks         TEXT,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sbom_components_software FOREIGN KEY (software_id)
        REFERENCES sbom_software (id) ON DELETE CASCADE
);

-- 자산 → SBOM SW 매핑 (SW 자산 전용, 선택)
ALTER TABLE assets ADD COLUMN sbom_software_id BIGINT NULL;
ALTER TABLE assets ADD CONSTRAINT fk_assets_sbom_software FOREIGN KEY (sbom_software_id)
    REFERENCES sbom_software (id) ON DELETE SET NULL;
