CREATE TABLE operational_asset_audit (
    id          UUID         NOT NULL,
    rev         INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype     SMALLINT     NOT NULL,
    uid         VARCHAR(50),
    name        VARCHAR(200),
    description TEXT,
    asset_type  VARCHAR(20),
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE,
    updated_at  TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id, rev)
);
