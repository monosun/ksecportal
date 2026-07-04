-- Vulnerability comments (timeline notes)
CREATE TABLE IF NOT EXISTS vulnerability_comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vulnerability_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (vulnerability_id) REFERENCES vulnerabilities(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
