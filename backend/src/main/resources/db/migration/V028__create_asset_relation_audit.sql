CREATE TABLE asset_relation_audit (
    id            UUID         NOT NULL,
    rev           INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype       SMALLINT     NOT NULL,
    source_id     UUID,
    target_id     UUID,
    relation_type VARCHAR(30),
    description   TEXT,
    created_at    TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id, rev)
);
