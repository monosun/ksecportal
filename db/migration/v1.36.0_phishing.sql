-- v1.36.0: 모의 악성메일 훈련 (피싱 시뮬레이션)

-- 악성메일 템플릿
CREATE TABLE IF NOT EXISTS phishing_templates (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255)  NOT NULL,
    category    VARCHAR(100)  NOT NULL,
    difficulty  ENUM('EASY','MEDIUM','HARD') NOT NULL DEFAULT 'MEDIUM',
    subject     VARCHAR(255)  NOT NULL,
    sender_name VARCHAR(255)  NOT NULL,
    sender_email VARCHAR(255) NOT NULL,
    body_html   LONGTEXT      NOT NULL,
    description TEXT,
    created_by  BIGINT,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

-- 발송 대상
CREATE TABLE IF NOT EXISTS phishing_targets (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    email       VARCHAR(255)  NOT NULL,
    department  VARCHAR(100),
    position    VARCHAR(100),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 모의훈련 캠페인
CREATE TABLE IF NOT EXISTS phishing_campaigns (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    template_id  BIGINT NOT NULL,
    status       ENUM('DRAFT','RUNNING','COMPLETED','CANCELLED') NOT NULL DEFAULT 'DRAFT',
    scheduled_at DATETIME,
    description  TEXT,
    created_by   BIGINT NOT NULL,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (template_id) REFERENCES phishing_templates(id),
    FOREIGN KEY (created_by)  REFERENCES users(id)
);

-- 캠페인별 발송 대상 및 결과 추적
CREATE TABLE IF NOT EXISTS phishing_campaign_targets (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id     BIGINT NOT NULL,
    target_id       BIGINT NOT NULL,
    tracking_token  VARCHAR(64) UNIQUE,
    sent_at         DATETIME,
    opened_at       DATETIME,
    clicked_at      DATETIME,
    reported_at     DATETIME,
    FOREIGN KEY (campaign_id) REFERENCES phishing_campaigns(id) ON DELETE CASCADE,
    FOREIGN KEY (target_id)   REFERENCES phishing_targets(id)
);

-- 기본 템플릿 샘플
INSERT INTO phishing_templates (name, category, difficulty, subject, sender_name, sender_email, body_html, description) VALUES
(
  'IT 시스템 비밀번호 재설정 요청',
  'IT',
  'EASY',
  '[긴급] 계정 비밀번호 재설정이 필요합니다',
  'IT 지원팀',
  'it-support@company-internal.com',
  '<div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px">
<div style="background:#1a56db;padding:15px 20px;border-radius:8px 8px 0 0">
  <h2 style="color:#fff;margin:0;font-size:18px">IT 보안 알림</h2>
</div>
<div style="background:#fff;border:1px solid #e5e7eb;border-top:none;padding:25px;border-radius:0 0 8px 8px">
  <p style="color:#374151">{TARGET_NAME}님 안녕하세요,</p>
  <p style="color:#374151">회사 보안 정책에 따라 귀하의 계정 비밀번호를 <strong>24시간 이내</strong>에 재설정해야 합니다.</p>
  <p style="color:#374151">아래 버튼을 클릭하여 비밀번호를 변경하세요:</p>
  <div style="text-align:center;margin:30px 0">
    <a href="{CLICK_URL}" style="background:#1a56db;color:#fff;padding:12px 28px;border-radius:6px;text-decoration:none;font-weight:bold;display:inline-block">비밀번호 재설정</a>
  </div>
  <p style="color:#6b7280;font-size:13px">이 메일을 요청하지 않으셨다면 IT 지원팀에 즉시 연락해 주세요.</p>
  <img src="{OPEN_URL}" width="1" height="1" style="display:none" alt="">
</div>
</div>',
  '비밀번호 재설정을 위장한 기본 난이도 피싱 템플릿'
),
(
  '택배 배송 실패 알림',
  'DELIVERY',
  'MEDIUM',
  '배송 실패 안내 — 주소 확인 필요',
  '배송 알림 시스템',
  'noreply@delivery-notice.net',
  '<div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;padding:20px">
<div style="background:#f59e0b;padding:15px 20px;border-radius:8px 8px 0 0">
  <h2 style="color:#fff;margin:0;font-size:18px">배송 실패 알림</h2>
</div>
<div style="background:#fff;border:1px solid #e5e7eb;border-top:none;padding:25px;border-radius:0 0 8px 8px">
  <p style="color:#374151">{TARGET_NAME}님께,</p>
  <p style="color:#374151">귀하의 소포가 <strong>주소 불일치</strong>로 인해 배송에 실패하였습니다.</p>
  <p style="color:#374151">72시간 이내에 주소를 확인하지 않으면 반송 처리됩니다.</p>
  <div style="text-align:center;margin:30px 0">
    <a href="{CLICK_URL}" style="background:#f59e0b;color:#fff;padding:12px 28px;border-radius:6px;text-decoration:none;font-weight:bold;display:inline-block">배송 정보 확인</a>
  </div>
  <p style="color:#6b7280;font-size:12px">운송장 번호: 3849201847561</p>
  <img src="{OPEN_URL}" width="1" height="1" style="display:none" alt="">
</div>
</div>',
  '택배 배송 실패를 위장한 중간 난이도 피싱 템플릿'
);
