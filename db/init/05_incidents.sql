SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

CREATE TABLE IF NOT EXISTS incidents (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    title            VARCHAR(255) NOT NULL,
    description      TEXT,
    severity         VARCHAR(20)  NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    type             VARCHAR(30)  NOT NULL,
    affected_systems TEXT,
    reporter_id      BIGINT       NOT NULL,
    assignee_id      BIGINT,
    detected_at      DATETIME,
    resolved_at      DATETIME,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (reporter_id) REFERENCES users(id),
    FOREIGN KEY (assignee_id) REFERENCES users(id)
);

INSERT INTO incidents (title, description, severity, status, type, affected_systems, reporter_id, detected_at) VALUES
('랜섬웨어 감염 의심', '재무팀 PC에서 파일 확장자 변경이 감지되어 랜섬웨어 감염이 의심됩니다. 해당 PC는 네트워크에서 격리 조치했습니다.', 'CRITICAL', 'INVESTIGATING', 'MALWARE', '재무팀 PC (192.168.2.30)', 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
('피싱 이메일 대량 수신', '공급사를 사칭한 피싱 이메일이 임직원 50명에게 발송되었습니다. 악성 첨부파일 클릭 여부를 확인 중입니다.', 'HIGH', 'CONTAINED', 'PHISHING', '전체 임직원 이메일 계정', 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
('개발 서버 비인가 접근 시도', '외부 IP에서 개발 서버 SSH 포트 무차별 대입 공격이 탐지되었습니다.', 'MEDIUM', 'RESOLVED', 'UNAUTHORIZED_ACCESS', '개발 서버 (192.168.1.50)', 1, DATE_SUB(NOW(), INTERVAL 7 DAY));
