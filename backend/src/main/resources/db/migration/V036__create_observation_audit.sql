CREATE TABLE observation_audit (
    id                UUID         NOT NULL,
    rev               INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype           SMALLINT,
    asset_id          UUID,
    category          VARCHAR(30),
    observation_key   VARCHAR(200),
    observation_value TEXT,
    source            VARCHAR(200),
    observed_at       TIMESTAMPTZ,
    expires_at        TIMESTAMPTZ,
    confidence        VARCHAR(50),
    evidence_ref      VARCHAR(2000),
    created_at        TIMESTAMPTZ,
    updated_at        TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);
