CREATE TABLE asset_link_audit (
    id                UUID         NOT NULL,
    rev               INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype           SMALLINT     NOT NULL,
    asset_id          UUID,
    target_type       VARCHAR(20),
    target_identifier VARCHAR(500),
    link_type         VARCHAR(20),
    target_url        VARCHAR(2000),
    target_title      VARCHAR(255),
    created_at        TIMESTAMP WITH TIME ZONE,
    updated_at        TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id, rev)
);
