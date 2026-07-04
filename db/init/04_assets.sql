SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

CREATE TABLE IF NOT EXISTS assets (
    id                        BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                      VARCHAR(255) NOT NULL,
    asset_category            VARCHAR(20)  NULL,
    type                      VARCHAR(50)  NOT NULL,
    description               TEXT,
    owner                     VARCHAR(100),
    department                VARCHAR(100),
    operator                  VARCHAR(100),
    environment               VARCHAR(20)  NOT NULL DEFAULT 'PRODUCTION',
    location                  VARCHAR(200),
    status                    VARCHAR(20)  NOT NULL DEFAULT 'OPERATIONAL',
    cloud_provider            VARCHAR(20)  NOT NULL DEFAULT 'ON_PREMISE',
    cloud_resource_id         VARCHAR(255),
    region                    VARCHAR(100),
    ip_address                VARCHAR(100),
    os_type                   VARCHAR(100),
    spec                      TEXT,
    criticality               VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    confidentiality           VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM',
    integrity                 VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM',
    availability              VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM',
    personal_info_included    BOOLEAN      NOT NULL DEFAULT FALSE,
    personal_info_type        VARCHAR(200),
    personal_info_processing  BOOLEAN      NOT NULL DEFAULT FALSE,
    linked_systems            TEXT,
    access_control_target     BOOLEAN      NOT NULL DEFAULT FALSE,
    backup_target             BOOLEAN      NOT NULL DEFAULT FALSE,
    log_management_target     BOOLEAN      NOT NULL DEFAULT FALSE,
    active                    BOOLEAN      NOT NULL DEFAULT TRUE,
    monthly_cost              DECIMAL(12,2),
    contract_expiry           DATE,
    last_inspection_date      DATE,
    next_inspection_date      DATE,
    last_review_date          DATE,
    remarks                   TEXT,
    created_at                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 온프레미스 자산
INSERT INTO assets (name, asset_category, type, environment, cloud_provider, ip_address, os_type, spec, owner, department, description, status, criticality, confidentiality, integrity, availability, access_control_target, backup_target, log_management_target) VALUES
('메인 웹 서버',       'HW',   'SERVER',      'PRODUCTION', 'ON_PREMISE', '192.168.1.10', 'Ubuntu 22.04 LTS', 'CPU: 8코어 / RAM: 32GB / SSD: 500GB', 'System Admin', 'IT', '프로덕션 웹 애플리케이션 서버', 'OPERATIONAL', 'HIGH',   'HIGH',   'MEDIUM', 'HIGH',   TRUE, TRUE,  TRUE),
('데이터베이스 서버',   'SW',   'DATABASE',    'PRODUCTION', 'ON_PREMISE', '192.168.1.20', 'Ubuntu 22.04 LTS', 'CPU: 16코어 / RAM: 64GB / SSD: 2TB',  'System Admin', 'IT', 'MySQL 8.0 프로덕션 데이터베이스', 'OPERATIONAL', 'HIGH',   'HIGH',   'HIGH',   'HIGH',   TRUE, TRUE,  TRUE),
('사내 Wi-Fi AP',     'HW',   'NETWORK',     'PRODUCTION', 'ON_PREMISE', '192.168.1.1',  NULL,               NULL,                                   'Network Team', 'IT', '사무실 무선 네트워크 장비 (Cisco Catalyst)', 'OPERATIONAL', 'MEDIUM', 'MEDIUM', 'MEDIUM', 'HIGH',   TRUE, FALSE, TRUE),
('HR 시스템',         'SW',   'APPLICATION', 'PRODUCTION', 'ON_PREMISE', NULL,           NULL,               NULL,                                   'HR Team',      'HR', '인사관리 내부 애플리케이션', 'OPERATIONAL', 'HIGH',   'HIGH',   'HIGH',   'HIGH',   TRUE, FALSE, TRUE),
('개발자 워크스테이션', 'HW',  'WORKSTATION', 'DEVELOPMENT','ON_PREMISE', '192.168.2.50', 'Windows 11 Pro',   'CPU: Intel i9 / RAM: 32GB / SSD: 1TB', 'Dev Team',     'Development', '개발팀 업무용 PC', 'OPERATIONAL', 'LOW', 'LOW', 'LOW', 'MEDIUM', FALSE, FALSE, FALSE);

-- AWS 클라우드 자산
INSERT INTO assets (name, asset_category, type, environment, cloud_provider, cloud_resource_id, region, ip_address, os_type, spec, owner, department, description, status, criticality, confidentiality, integrity, availability, access_control_target, backup_target, log_management_target, monthly_cost) VALUES
('SecPortal 프로덕션 EC2', 'SW', 'EC2', 'PRODUCTION', 'AWS', 'i-0a1b2c3d4e5f67890', 'ap-northeast-2', '13.124.10.100', 'Amazon Linux 2023', 't3.medium (vCPU: 2 / RAM: 4GB)', 'System Admin', 'IT', '메인 서비스 EC2 인스턴스 (Docker Compose 운영)', 'OPERATIONAL', 'HIGH', 'HIGH', 'HIGH', 'HIGH', TRUE, TRUE, TRUE, 36000),
('SecPortal RDS MySQL',   'SW', 'RDS', 'PRODUCTION', 'AWS', 'secportal-prod-db',   'ap-northeast-2', NULL,            'MySQL 8.0',          'db.t3.medium (vCPU: 2 / RAM: 4GB / 스토리지: 100GB)', 'System Admin', 'IT', '프로덕션 RDS 인스턴스 (Multi-AZ)', 'OPERATIONAL', 'HIGH', 'HIGH', 'HIGH', 'HIGH', TRUE, TRUE, TRUE, 68000),
('정적 파일 S3 버킷',      'SW', 'S3',  'PRODUCTION', 'AWS', 'secportal-static-assets', 'ap-northeast-2', NULL,        NULL,                 '표준 스토리지 클래스', 'Dev Team', 'IT', 'Vue.js 빌드 산출물 및 첨부파일 저장', 'OPERATIONAL', 'MEDIUM', 'MEDIUM', 'MEDIUM', 'HIGH', TRUE, TRUE, FALSE, 5000),
('Application Load Balancer', 'SW', 'ELB', 'PRODUCTION', 'AWS', 'arn:aws:elasticloadbalancing:ap-northeast-2:123456789:loadbalancer/app/secportal-alb', 'ap-northeast-2', NULL, NULL, 'ALB (HTTP:80 / HTTPS:443)', 'System Admin', 'IT', '트래픽 분산 및 SSL 종료', 'OPERATIONAL', 'HIGH', 'HIGH', 'HIGH', 'HIGH', TRUE, FALSE, TRUE, 25000),
('개발 EC2 (스테이징)',    'SW', 'EC2', 'STAGING',    'AWS', 'i-0f9e8d7c6b5a43210', 'ap-northeast-2', '13.124.20.200', 'Amazon Linux 2023', 't3.small (vCPU: 2 / RAM: 2GB)', 'Dev Team', 'Development', '스테이징 환경 테스트 서버', 'OPERATIONAL', 'LOW', 'LOW', 'MEDIUM', 'MEDIUM', FALSE, FALSE, TRUE, 18000),
('CloudFront 배포',       'SW', 'CLOUD_OTHER', 'PRODUCTION', 'AWS', 'E1A2B3C4D5E6F7', 'ap-northeast-2', NULL, NULL, 'CDN 배포 (전 세계 엣지)', 'Dev Team', 'IT', '정적 자산 CDN 캐싱 (S3 오리진)', 'OPERATIONAL', 'MEDIUM', 'MEDIUM', 'MEDIUM', 'HIGH', FALSE, FALSE, FALSE, 8000);
