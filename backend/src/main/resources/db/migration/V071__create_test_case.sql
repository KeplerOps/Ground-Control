CREATE TABLE test_case (
    id                          UUID PRIMARY KEY,
    project_id                  UUID         NOT NULL REFERENCES project(id),
    uid                         VARCHAR(50)  NOT NULL,
    title                       VARCHAR(200) NOT NULL,
    description                 TEXT,
    preconditions               TEXT,
    postconditions              TEXT,
    priority                    VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    status                      VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    type                        VARCHAR(20)  NOT NULL,
    estimated_duration_seconds  BIGINT,
    created_at                  TIMESTAMPTZ  NOT NULL,
    updated_at                  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_test_case_project_uid UNIQUE (project_id, uid)
);

CREATE INDEX idx_test_case_project ON test_case (project_id);
CREATE INDEX idx_test_case_status ON test_case (status);
CREATE INDEX idx_test_case_priority ON test_case (priority);
CREATE INDEX idx_test_case_type ON test_case (type);
CREATE INDEX idx_test_case_uid ON test_case (project_id, uid);
