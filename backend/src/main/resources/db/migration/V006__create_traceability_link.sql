CREATE TABLE traceability_link (
    id                  UUID PRIMARY KEY,
    requirement_id      UUID         NOT NULL REFERENCES requirement(id) ON DELETE CASCADE,
    artifact_type       VARCHAR(30)  NOT NULL,
    artifact_identifier VARCHAR(500) NOT NULL,
    link_type           VARCHAR(20)  NOT NULL,
    sync_status         VARCHAR(10)  NOT NULL DEFAULT 'SYNCED',
    artifact_url        VARCHAR(2000) DEFAULT '',
    artifact_title      VARCHAR(255) DEFAULT '',
    last_synced_at      TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (requirement_id, artifact_type, artifact_identifier, link_type)
);

CREATE INDEX idx_traceability_link_requirement ON traceability_link(requirement_id);
CREATE INDEX idx_traceability_link_artifact_type ON traceability_link(artifact_type);
CREATE INDEX idx_traceability_link_link_type ON traceability_link(link_type);
