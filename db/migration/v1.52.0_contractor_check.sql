-- ====================================================
-- v1.52.0: 수탁사 점검항목 관리 + 수탁사 점검
-- ====================================================

-- 1. 기본 점검항목 템플릿
CREATE TABLE contractor_check_item_defaults (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(100) NOT NULL,
    item_name VARCHAR(300) NOT NULL,
    check_method VARCHAR(500),
    check_standard VARCHAR(500),
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP
);

-- 2. 운영 점검항목 (조직이 관리하는 항목 목록)
CREATE TABLE contractor_check_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category VARCHAR(100) NOT NULL,
    item_name VARCHAR(300) NOT NULL,
    check_method VARCHAR(500),
    check_standard VARCHAR(500),
    is_required BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP
);

-- 3. 수탁사 점검 헤더 (연도 + 수탁사)
CREATE TABLE contractor_checks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contractor_id BIGINT NOT NULL,
    check_year INT NOT NULL,
    check_date DATE,
    inspector VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    notes TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (contractor_id) REFERENCES privacy_contractors(id) ON DELETE CASCADE,
    UNIQUE KEY uk_contractor_year (contractor_id, check_year),
    INDEX idx_check_contractor (contractor_id),
    INDEX idx_check_year (check_year)
);

-- 4. 점검 결과 (항목별)
CREATE TABLE contractor_check_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    check_id BIGINT NOT NULL,
    check_item_id BIGINT NOT NULL,
    result VARCHAR(20) NOT NULL DEFAULT 'NOT_CHECKED',
    notes TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (check_id) REFERENCES contractor_checks(id) ON DELETE CASCADE,
    FOREIGN KEY (check_item_id) REFERENCES contractor_check_items(id) ON DELETE CASCADE,
    UNIQUE KEY uk_check_item (check_id, check_item_id),
    INDEX idx_result_check (check_id)
);

-- 5. 기본 점검항목 씨드 데이터 (20개)
INSERT INTO contractor_check_item_defaults (category, item_name, check_method, check_standard, sort_order) VALUES
('계약 관리', '개인정보 처리 위탁 계약서 체결 여부', '계약서 원본 확인', '개인정보보호법 제26조 제1항 — 위탁계약서 필수 체결', 1),
('계약 관리', '위탁 계약서 내 개인정보 보호 조항 포함 여부', '계약서 조항 검토', '개인정보보호법 제26조 제1항 각호 조항 포함 여부', 2),
('계약 관리', '재위탁 사전 동의 절차 준수 여부', '재위탁 내역 및 동의 문서 확인', '개인정보보호법 제26조 제6항', 3),
('조직 및 인력', '개인정보보호 담당자 지정 여부', '담당자 지정 문서 확인', '내부 관리계획 수립 기준', 4),
('조직 및 인력', '임직원 개인정보보호 교육 실시 여부', '교육 이수 확인서·출석부 확인', '연 1회 이상 교육 실시', 5),
('조직 및 인력', '개인정보 처리 방침 수립 및 공개 여부', '처리 방침 게시 화면 확인', '개인정보보호법 제30조', 6),
('기술적 보호조치', '개인정보 접근 통제 및 권한 관리 현황', '접근 권한 목록 및 변경 이력 확인', '최소 권한 원칙 적용 여부', 7),
('기술적 보호조치', '개인정보 암호화 적용 여부 (전송·저장)', '암호화 설정 문서 및 DB 스키마 확인', '비밀번호·주민등록번호 등 암호화 필수 항목', 8),
('기술적 보호조치', '접속기록 보관 및 점검 실시 여부', '로그 보관 현황 및 정기 점검 내역 확인', '최소 6개월 이상 보관', 9),
('기술적 보호조치', '악성프로그램 방지 조치 현황', '백신 설치·업데이트 내역 확인', '최신 버전 유지 및 정기 검사', 10),
('기술적 보호조치', '개인정보 처리 시스템 취약점 점검 실시 여부', '취약점 점검 보고서 확인', '연 1회 이상 취약점 진단', 11),
('관리적 보호조치', '내부 관리계획 수립 및 이행 여부', '내부 관리계획 문서 확인', '개인정보 내부 관리계획 수립·시행', 12),
('관리적 보호조치', '개인정보 처리 현황 보고 체계 운영 여부', '보고 이력 및 보고서 확인', '정기적 처리 현황 파악 및 보고', 13),
('관리적 보호조치', '개인정보 보안 사고 발생 시 통보 절차 수립 여부', '사고 대응 절차서 확인', '개인정보보호법 제34조 — 유출 통지 의무', 14),
('관리적 보호조치', '개인정보 처리 대장 관리 여부', '처리 대장 현황 확인', '처리 항목·목적·보유기간 기록', 15),
('파기 및 보유', '개인정보 파기 절차 수립 및 이행 여부', '파기 대장 및 이행 증적 확인', '보유기간 경과 후 지체 없이 파기', 16),
('파기 및 보유', '개인정보 보유기간 초과 여부 점검 현황', '데이터 보유기간 목록 확인', '보유기간 만료 데이터 파기 이행', 17),
('물리적 보호조치', '개인정보 처리 구역 출입 통제 현황', '출입 통제 기록 및 시설 현황 확인', '비인가자 접근 차단 조치', 18),
('물리적 보호조치', '개인정보가 담긴 문서·매체의 잠금 보관 여부', '문서함·캐비닛 잠금 상태 확인', '분실·도난 방지 조치', 19),
('점검 및 감사', '수탁사 자체 개인정보보호 점검 실시 여부', '자체 점검 보고서 확인', '연 1회 이상 자체 점검 실시', 20);
