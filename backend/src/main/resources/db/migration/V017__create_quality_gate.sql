CREATE TABLE quality_gate (
    id            UUID PRIMARY KEY,
    project_id    UUID         NOT NULL REFERENCES project(id),
    name          VARCHAR(100) NOT NULL,
    description   TEXT,
    metric_type   VARCHAR(30)  NOT NULL,
    metric_param  VARCHAR(30),
    scope_status  VARCHAR(20),
    operator      VARCHAR(5)   NOT NULL,
    threshold     DOUBLE PRECISION NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_quality_gate_project_name UNIQUE (project_id, name)
);

CREATE INDEX idx_quality_gate_project_id ON quality_gate (project_id);
