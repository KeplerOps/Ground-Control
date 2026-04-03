CREATE TABLE asset_external_id_audit (
    id            UUID         NOT NULL,
    rev           INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype       SMALLINT,
    asset_id      UUID,
    source_system VARCHAR(100),
    source_id     VARCHAR(500),
    collected_at  TIMESTAMPTZ,
    confidence    VARCHAR(50),
    created_at    TIMESTAMPTZ,
    updated_at    TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);
