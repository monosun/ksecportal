SET NAMES utf8mb4;

-- v1.48.0 자산관리 필드 확장 (ISMS-P 자산식별 기준)
ALTER TABLE assets
  ADD COLUMN asset_category           VARCHAR(20)  NULL              COMMENT '자산유형: INFO, SW, HW, SERVICE, PERSONNEL, FACILITY',
  ADD COLUMN operator                 VARCHAR(100) NULL              COMMENT '운영담당자(실무)',
  ADD COLUMN location                 VARCHAR(200) NULL              COMMENT '위치(AWS, IDC, 사무실 등)',
  ADD COLUMN status                   VARCHAR(20)  NOT NULL DEFAULT 'OPERATIONAL' COMMENT '상태: OPERATIONAL, SUSPENDED, DISPOSED',
  ADD COLUMN confidentiality          VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM' COMMENT '기밀성(C): HIGH, MEDIUM, LOW',
  ADD COLUMN integrity                VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM' COMMENT '무결성(I): HIGH, MEDIUM, LOW',
  ADD COLUMN availability             VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM' COMMENT '가용성(A): HIGH, MEDIUM, LOW',
  ADD COLUMN personal_info_included   BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '개인정보 포함 여부',
  ADD COLUMN personal_info_type       VARCHAR(200) NULL              COMMENT '개인정보 유형(고객정보, 임직원정보 등)',
  ADD COLUMN personal_info_processing BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '개인정보 처리 여부',
  ADD COLUMN linked_systems           TEXT         NULL              COMMENT '연계 시스템',
  ADD COLUMN access_control_target    BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '접근권한 관리 대상 여부',
  ADD COLUMN backup_target            BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '백업 대상 여부',
  ADD COLUMN log_management_target    BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '로그 관리 대상 여부',
  ADD COLUMN last_review_date         DATE         NULL              COMMENT '최종 검토일(정기 검토 증적)',
  ADD COLUMN remarks                  TEXT         NULL              COMMENT '비고(특이사항)';

-- 기존 active 플래그를 status 로 마이그레이션
UPDATE assets SET status = CASE WHEN active = TRUE THEN 'OPERATIONAL' ELSE 'SUSPENDED' END;
