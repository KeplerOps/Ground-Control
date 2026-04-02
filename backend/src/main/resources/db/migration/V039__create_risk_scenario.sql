CREATE TABLE risk_scenario (
    id                UUID PRIMARY KEY,
    project_id        UUID          NOT NULL REFERENCES project(id),
    uid               VARCHAR(20)   NOT NULL,
    title             VARCHAR(200)  NOT NULL,
    status            VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',
    threat_source     TEXT          NOT NULL,
    threat_event      TEXT          NOT NULL,
    affected_object   TEXT          NOT NULL,
    vulnerability     TEXT,
    consequence       TEXT          NOT NULL,
    time_horizon      VARCHAR(100)  NOT NULL,
    observation_refs  TEXT,
    topology_context  TEXT,
    created_by        VARCHAR(100),
    created_at        TIMESTAMPTZ   NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uq_risk_scenario_project_uid UNIQUE (project_id, uid)
);

CREATE INDEX idx_risk_scenario_project ON risk_scenario (project_id);
CREATE INDEX idx_risk_scenario_status  ON risk_scenario (status);
CREATE INDEX idx_risk_scenario_uid     ON risk_scenario (uid);
