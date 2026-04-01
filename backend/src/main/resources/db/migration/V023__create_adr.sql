CREATE TABLE architecture_decision_record (
    id              UUID PRIMARY KEY,
    project_id      UUID          NOT NULL REFERENCES project(id),
    uid             VARCHAR(20)   NOT NULL,
    title           VARCHAR(200)  NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PROPOSED',
    decision_date   DATE          NOT NULL,
    context         TEXT,
    decision        TEXT,
    consequences    TEXT,
    superseded_by   VARCHAR(20),
    created_by      VARCHAR(100),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_adr_project_uid UNIQUE (project_id, uid)
);

CREATE INDEX idx_adr_project_id ON architecture_decision_record(project_id);
CREATE INDEX idx_adr_status ON architecture_decision_record(status);
