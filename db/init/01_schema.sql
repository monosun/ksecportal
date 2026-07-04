SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- Users
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    role ENUM('ADMIN', 'MANAGER', 'USER') NOT NULL DEFAULT 'USER',
    department VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until DATETIME(6),
    mfa_enabled BIT(1) NOT NULL DEFAULT 0,
    mfa_secret VARCHAR(64),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Security Policies
CREATE TABLE IF NOT EXISTS policies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content LONGTEXT NOT NULL,
    category ENUM('GENERAL', 'ACCESS_CONTROL', 'DATA_PROTECTION', 'INCIDENT_RESPONSE', 'NETWORK', 'PHYSICAL', 'VENDOR', 'OTHER') NOT NULL DEFAULT 'GENERAL',
    status ENUM('DRAFT', 'REVIEW', 'PUBLISHED', 'ARCHIVED') NOT NULL DEFAULT 'DRAFT',
    version VARCHAR(20) NOT NULL DEFAULT '1.0',
    effective_date DATE,
    author_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (author_id) REFERENCES users(id)
);

-- Policy Acknowledgments
CREATE TABLE IF NOT EXISTS policy_acknowledgments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    policy_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    acknowledged_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_policy_user (policy_id, user_id),
    FOREIGN KEY (policy_id) REFERENCES policies(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Vulnerabilities
CREATE TABLE IF NOT EXISTS vulnerabilities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    cve_id VARCHAR(50),
    cvss_score DECIMAL(3,1),
    severity ENUM('CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO') NOT NULL,
    status ENUM('OPEN', 'IN_PROGRESS', 'RESOLVED', 'ACCEPTED', 'FALSE_POSITIVE') NOT NULL DEFAULT 'OPEN',
    asset_name VARCHAR(255),
    assignee_id BIGINT,
    reporter_id BIGINT NOT NULL,
    due_date DATE,
    resolved_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (assignee_id) REFERENCES users(id),
    FOREIGN KEY (reporter_id) REFERENCES users(id)
);

-- Training Courses
CREATE TABLE IF NOT EXISTS training_courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    content_type ENUM('VIDEO', 'DOCUMENT', 'QUIZ_ONLY') NOT NULL DEFAULT 'DOCUMENT',
    content_url VARCHAR(500),
    passing_score INT NOT NULL DEFAULT 70,
    mandatory BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- Training Quiz Questions
CREATE TABLE IF NOT EXISTS quiz_questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    option_a VARCHAR(500) NOT NULL,
    option_b VARCHAR(500) NOT NULL,
    option_c VARCHAR(500),
    option_d VARCHAR(500),
    correct_answer CHAR(1) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    FOREIGN KEY (course_id) REFERENCES training_courses(id) ON DELETE CASCADE
);

-- Training Completions
CREATE TABLE IF NOT EXISTS training_completions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    score INT,
    passed BOOLEAN,
    completed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_course_user (course_id, user_id),
    FOREIGN KEY (course_id) REFERENCES training_courses(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Pending Admin Actions (email approval workflow)
CREATE TABLE IF NOT EXISTS pending_admin_actions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(36) NOT NULL UNIQUE,
    action_type ENUM('DELETE_USER', 'PROMOTE_ADMIN') NOT NULL,
    target_user_id BIGINT NOT NULL,
    requester_id BIGINT NOT NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED', 'EXPIRED') NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    FOREIGN KEY (target_user_id) REFERENCES users(id),
    FOREIGN KEY (requester_id) REFERENCES users(id)
);

-- Monthly Security Check Items (정보보호의 날 점검)
CREATE TABLE IF NOT EXISTS monthly_check_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    check_month VARCHAR(7) NOT NULL COMMENT 'YYYY-MM',
    priority ENUM('HIGH', 'MEDIUM', 'LOW') NOT NULL DEFAULT 'HIGH',
    category VARCHAR(50) NOT NULL,
    item_name VARCHAR(200) NOT NULL,
    check_method VARCHAR(500),
    check_example VARCHAR(500),
    result ENUM('COMPLETED', 'INCOMPLETE', 'NA') NOT NULL DEFAULT 'INCOMPLETE',
    notes TEXT,
    sort_order INT NOT NULL DEFAULT 0,
    assignee_id BIGINT,
    assignee_text VARCHAR(100),
    created_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (assignee_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Monthly Security Check Evidences (점검 증적 파일)
CREATE TABLE IF NOT EXISTS monthly_check_evidences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    check_item_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    file_name VARCHAR(500),
    file_path VARCHAR(1000),
    uploaded_by BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (check_item_id) REFERENCES monthly_check_items(id) ON DELETE CASCADE,
    FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Audit Logs
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id BIGINT,
    detail TEXT,
    ip_address VARCHAR(45),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);
