-- Step 1 schema. Portable between MySQL 8 and H2 (MySQL mode) so tests can run without a database.
-- Enums are stored as VARCHAR (mapped with @Enumerated(STRING)). app_user is added with JWT in a later step.

CREATE TABLE service (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    CONSTRAINT uk_service_name UNIQUE (name)
);

CREATE TABLE incident (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    code               VARCHAR(20),
    service_id         BIGINT       NOT NULL,
    title              VARCHAR(255) NOT NULL,
    symptoms           TEXT,
    severity           VARCHAR(20)  NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    root_cause         TEXT,
    lesson             TEXT,
    source             VARCHAR(20)  NOT NULL,
    failure_family     VARCHAR(100),
    memory_sync_status VARCHAR(20)  NOT NULL,
    memory_synced_at   DATETIME(6),
    opened_at          DATETIME(6)  NOT NULL,
    resolved_at        DATETIME(6),
    CONSTRAINT uk_incident_code UNIQUE (code),
    CONSTRAINT fk_incident_service FOREIGN KEY (service_id) REFERENCES service (id)
);
CREATE INDEX idx_incident_status ON incident (status);

CREATE TABLE alert (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    incident_id BIGINT       NOT NULL,
    service_id  BIGINT       NOT NULL,
    severity    VARCHAR(20)  NOT NULL,
    title       VARCHAR(255) NOT NULL,
    message     TEXT         NOT NULL,
    log_excerpt TEXT,
    received_at DATETIME(6)  NOT NULL,
    CONSTRAINT fk_alert_incident FOREIGN KEY (incident_id) REFERENCES incident (id),
    CONSTRAINT fk_alert_service FOREIGN KEY (service_id) REFERENCES service (id)
);

CREATE TABLE action_attempt (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    incident_id          BIGINT      NOT NULL,
    sequence_no          INT         NOT NULL,
    action_description   TEXT        NOT NULL,
    target_service_id    BIGINT,
    outcome              VARCHAR(20),
    observation          TEXT,
    explanation          TEXT,
    performed_at         DATETIME(6) NOT NULL,
    outcome_recorded_at  DATETIME(6),
    CONSTRAINT uk_action_incident_seq UNIQUE (incident_id, sequence_no),
    CONSTRAINT fk_action_incident FOREIGN KEY (incident_id) REFERENCES incident (id),
    CONSTRAINT fk_action_target_service FOREIGN KEY (target_service_id) REFERENCES service (id)
);

CREATE TABLE briefing_record (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    incident_id        BIGINT      NOT NULL,
    status             VARCHAR(20) NOT NULL,
    recall_query       TEXT        NOT NULL,
    hindsight_raw      JSON,
    llm_raw            JSON,
    briefing_json      JSON,
    validation_json    JSON,
    latency_ms_recall  INT,
    latency_ms_llm     INT,
    created_at         DATETIME(6) NOT NULL,
    CONSTRAINT fk_briefing_incident FOREIGN KEY (incident_id) REFERENCES incident (id)
);
CREATE INDEX idx_briefing_incident_created ON briefing_record (incident_id, created_at);
