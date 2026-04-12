CREATE TABLE threat_model (
    id              UUID PRIMARY KEY,
    project_id      UUID         NOT NULL REFERENCES project(id),
    uid             VARCHAR(30)  NOT NULL,
    title           VARCHAR(200) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    threat_source   TEXT         NOT NULL,
    threat_event    TEXT         NOT NULL,
    effect          TEXT         NOT NULL,
    stride          VARCHAR(30),
    narrative       TEXT,
    created_by      VARCHAR(100),
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_threat_model_project_uid UNIQUE (project_id, uid)
);

CREATE INDEX idx_threat_model_project ON threat_model (project_id);
CREATE INDEX idx_threat_model_status  ON threat_model (status);
CREATE INDEX idx_threat_model_uid     ON threat_model (uid);
