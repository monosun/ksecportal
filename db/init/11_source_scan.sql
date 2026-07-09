SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- GitHub 연동 설정 (단일 행 — 토큰은 ADMIN 전용 API로만 접근, 공개 app_settings에 저장 금지)
CREATE TABLE IF NOT EXISTS github_config (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    token        VARCHAR(500),
    api_base_url VARCHAR(300),
    owner        VARCHAR(200),
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 소스 취약점 점검 이력
CREATE TABLE IF NOT EXISTS source_scans (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    repository       VARCHAR(300) NOT NULL,             -- owner/repo
    status           VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS',  -- SUCCESS / PARTIAL / FAILED
    message          TEXT,
    critical_count   INT NOT NULL DEFAULT 0,
    high_count       INT NOT NULL DEFAULT 0,
    medium_count     INT NOT NULL DEFAULT 0,
    low_count        INT NOT NULL DEFAULT 0,
    dependency_count INT NOT NULL DEFAULT 0,
    code_count       INT NOT NULL DEFAULT 0,
    secret_count     INT NOT NULL DEFAULT 0,
    sast_count       INT NOT NULL DEFAULT 0,     -- 내장 OWASP 정적분석 발견 수
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 점검별 발견 항목
CREATE TABLE IF NOT EXISTS source_scan_findings (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    scan_id       BIGINT NOT NULL,
    category      VARCHAR(20) NOT NULL,   -- DEPENDENCY / CODE_SCANNING / SECRET / SAST
    severity      VARCHAR(10) NOT NULL,   -- CRITICAL / HIGH / MEDIUM / LOW / INFO
    severity_rank INT NOT NULL DEFAULT 4,
    title         VARCHAR(500) NOT NULL,
    identifier    VARCHAR(300),           -- 패키지명 / 룰 ID / 시크릿 유형
    location      VARCHAR(500),           -- manifest 경로 또는 파일:라인
    cve_id        VARCHAR(30),
    html_url      VARCHAR(500),
    CONSTRAINT fk_source_scan_findings_scan FOREIGN KEY (scan_id)
        REFERENCES source_scans (id) ON DELETE CASCADE
);
