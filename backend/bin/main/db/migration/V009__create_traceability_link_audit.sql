CREATE TABLE traceability_link_audit (
    id                  UUID         NOT NULL,
    rev                 INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype             SMALLINT     NOT NULL,
    requirement_id      UUID,
    artifact_type       VARCHAR(30),
    artifact_identifier VARCHAR(500),
    link_type           VARCHAR(20),
    sync_status         VARCHAR(10),
    artifact_url        VARCHAR(2000),
    artifact_title      VARCHAR(255),
    last_synced_at      TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE,
    updated_at          TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id, rev)
);
