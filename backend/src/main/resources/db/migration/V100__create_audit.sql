CREATE TABLE audit (
    id               UUID PRIMARY KEY,
    project_id       UUID         NOT NULL REFERENCES project(id),
    uid              VARCHAR(30)  NOT NULL,
    title            VARCHAR(200) NOT NULL,
    audit_type       VARCHAR(20)  NOT NULL,
    status           VARCHAR(30)  NOT NULL DEFAULT 'PLANNED',
    scope_description TEXT        NOT NULL,
    objectives       TEXT,
    phases           TEXT,
    team_members     TEXT,
    created_by       VARCHAR(100),
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_audit_project_uid UNIQUE (project_id, uid)
);

CREATE INDEX idx_audit_project    ON audit (project_id);
CREATE INDEX idx_audit_status     ON audit (status);
CREATE INDEX idx_audit_audit_type ON audit (audit_type);
CREATE INDEX idx_audit_uid        ON audit (uid);
