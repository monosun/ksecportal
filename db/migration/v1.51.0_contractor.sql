-- 수탁사 관리 (Privacy Contractor Management)

CREATE TABLE IF NOT EXISTS privacy_contractors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL COMMENT '수탁사명',
    business_number VARCHAR(20) COMMENT '사업자등록번호',
    representative VARCHAR(100) COMMENT '대표자',
    service_type VARCHAR(200) COMMENT '위탁 업무 유형',
    contract_start DATE COMMENT '계약 시작일',
    contract_end DATE COMMENT '계약 종료일',
    contact_person VARCHAR(100) COMMENT '담당자명',
    contact_email VARCHAR(200) COMMENT '담당자 이메일',
    contact_phone VARCHAR(50) COMMENT '담당자 연락처',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE',
    notes TEXT COMMENT '비고',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_contractor_status (status),
    INDEX idx_contractor_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS contractor_inspections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contractor_id BIGINT NOT NULL COMMENT '수탁사 ID',
    inspection_date DATE NOT NULL COMMENT '점검일',
    inspector VARCHAR(100) COMMENT '점검자',
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED' COMMENT 'PLANNED/COMPLETED/ISSUE_FOUND',
    result TEXT COMMENT '점검 결과',
    notes TEXT COMMENT '비고',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_inspection_contractor FOREIGN KEY (contractor_id) REFERENCES privacy_contractors(id) ON DELETE CASCADE,
    INDEX idx_inspection_contractor (contractor_id),
    INDEX idx_inspection_date (inspection_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS contractor_inspection_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inspection_id BIGINT NOT NULL COMMENT '점검 ID',
    title VARCHAR(500) NOT NULL COMMENT '파일 제목',
    file_name VARCHAR(500) COMMENT '원본 파일명',
    file_path VARCHAR(1000) COMMENT '저장 경로',
    file_size BIGINT COMMENT '파일 크기(bytes)',
    uploader_id BIGINT COMMENT '업로더 ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_insp_file_inspection FOREIGN KEY (inspection_id) REFERENCES contractor_inspections(id) ON DELETE CASCADE,
    CONSTRAINT fk_insp_file_uploader FOREIGN KEY (uploader_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_insp_file_inspection (inspection_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
