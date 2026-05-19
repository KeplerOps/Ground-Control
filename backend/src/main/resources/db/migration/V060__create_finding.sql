CREATE TABLE finding (
    id                  UUID PRIMARY KEY,
    project_id          UUID         NOT NULL REFERENCES project(id),
    uid                 VARCHAR(30)  NOT NULL,
    title               VARCHAR(200) NOT NULL,
    finding_type        VARCHAR(30)  NOT NULL,
    severity            VARCHAR(20)  NOT NULL,
    status              VARCHAR(30)  NOT NULL DEFAULT 'OPEN',
    description         TEXT         NOT NULL,
    root_cause_analysis TEXT,
    owner               VARCHAR(100),
    due_date            DATE,
    created_by          VARCHAR(100),
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_finding_project_uid UNIQUE (project_id, uid)
);

CREATE INDEX idx_finding_project      ON finding (project_id);
CREATE INDEX idx_finding_status       ON finding (status);
CREATE INDEX idx_finding_finding_type ON finding (finding_type);
CREATE INDEX idx_finding_severity     ON finding (severity);
CREATE INDEX idx_finding_owner        ON finding (owner);
CREATE INDEX idx_finding_due_date     ON finding (due_date);
CREATE INDEX idx_finding_uid          ON finding (uid);
