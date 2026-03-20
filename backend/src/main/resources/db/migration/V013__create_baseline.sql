CREATE TABLE baseline (
    id              UUID PRIMARY KEY,
    project_id      UUID         NOT NULL REFERENCES project(id),
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    revision_number INTEGER      NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by      VARCHAR(100),
    CONSTRAINT uq_baseline_project_name UNIQUE (project_id, name)
);

CREATE INDEX idx_baseline_project_id ON baseline (project_id);
