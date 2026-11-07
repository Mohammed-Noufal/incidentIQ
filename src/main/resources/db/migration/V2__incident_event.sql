CREATE TABLE incident_event (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    incident_id BIGINT      NOT NULL,
    type        VARCHAR(30) NOT NULL,
    title       VARCHAR(255) NOT NULL,
    details     TEXT,
    occurred_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_incident_event_incident FOREIGN KEY (incident_id) REFERENCES incident (id)
);
CREATE INDEX idx_incident_event_incident_occurred ON incident_event (incident_id, occurred_at);
