CREATE TABLE risk_scenario_link (
    id                  UUID PRIMARY KEY,
    risk_scenario_id    UUID          NOT NULL REFERENCES risk_scenario(id) ON DELETE CASCADE,
    target_type         VARCHAR(20)   NOT NULL,
    target_identifier   VARCHAR(500)  NOT NULL,
    link_type           VARCHAR(20)   NOT NULL,
    target_url          VARCHAR(2000) DEFAULT '',
    target_title        VARCHAR(255)  DEFAULT '',
    created_at          TIMESTAMPTZ   NOT NULL,
    updated_at          TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uq_risk_scenario_link UNIQUE (risk_scenario_id, target_type, target_identifier, link_type)
);

CREATE INDEX idx_rsl_scenario    ON risk_scenario_link (risk_scenario_id);
CREATE INDEX idx_rsl_target_type ON risk_scenario_link (target_type);
CREATE INDEX idx_rsl_target      ON risk_scenario_link (risk_scenario_id, target_type, target_identifier);
