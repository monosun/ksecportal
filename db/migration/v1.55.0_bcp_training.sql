-- v1.55.0: 재해복구(DR)·업무연속성(BCP) 훈련
--
-- 시나리오 예제는 BcpScenarioInitializer(seed-when-empty)가 기동 시 채우므로 여기서 INSERT 하지 않는다.

-- 훈련 시나리오
CREATE TABLE IF NOT EXISTS bcp_scenarios (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    category      VARCHAR(100) NOT NULL,
    difficulty    ENUM('EASY','MEDIUM','HARD') NOT NULL DEFAULT 'MEDIUM',
    target_system VARCHAR(255),
    rto_minutes   INT,
    rpo_minutes   INT,
    situation     TEXT,
    objective     TEXT,
    description   TEXT,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_by    BIGINT,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

-- 시나리오 대응 단계
CREATE TABLE IF NOT EXISTS bcp_scenario_steps (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    scenario_id      BIGINT NOT NULL,
    step_order       INT NOT NULL,
    title            VARCHAR(255) NOT NULL,
    role_name        VARCHAR(100),
    action           TEXT,
    target_minutes   INT,
    success_criteria TEXT,
    FOREIGN KEY (scenario_id) REFERENCES bcp_scenarios(id) ON DELETE CASCADE
);

-- 훈련 실시
CREATE TABLE IF NOT EXISTS bcp_exercises (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    name               VARCHAR(255) NOT NULL,
    scenario_id        BIGINT NOT NULL,
    method             ENUM('TABLETOP','SIMULATION','FAILOVER') NOT NULL DEFAULT 'TABLETOP',
    status             ENUM('DRAFT','RUNNING','COMPLETED','CANCELLED') NOT NULL DEFAULT 'DRAFT',
    planned_at         DATETIME,
    started_at         DATETIME,
    ended_at           DATETIME,
    leader_name        VARCHAR(100),
    participants       TEXT,
    participant_count  INT,
    actual_rto_minutes INT,
    actual_rpo_minutes INT,
    score              INT,
    result             ENUM('PASS','PARTIAL','FAIL'),
    summary            TEXT,
    improvement        TEXT,
    description        TEXT,
    created_by         BIGINT,
    created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (scenario_id) REFERENCES bcp_scenarios(id),
    FOREIGN KEY (created_by)  REFERENCES users(id) ON DELETE SET NULL
);

-- 훈련 단계별 수행 결과 (실시 시점에 시나리오 단계를 복사해 보관)
CREATE TABLE IF NOT EXISTS bcp_exercise_steps (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    exercise_id      BIGINT NOT NULL,
    step_order       INT NOT NULL,
    title            VARCHAR(255) NOT NULL,
    role_name        VARCHAR(100),
    action           TEXT,
    target_minutes   INT,
    success_criteria TEXT,
    actual_minutes   INT,
    result           ENUM('PENDING','PASS','PARTIAL','FAIL') NOT NULL DEFAULT 'PENDING',
    note             TEXT,
    completed_at     DATETIME,
    FOREIGN KEY (exercise_id) REFERENCES bcp_exercises(id) ON DELETE CASCADE
);
