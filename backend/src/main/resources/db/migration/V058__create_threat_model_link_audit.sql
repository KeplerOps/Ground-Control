CREATE TABLE threat_model_link_audit (
    id                 UUID         NOT NULL,
    rev                INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype            SMALLINT,
    threat_model_id    UUID,
    target_type        VARCHAR(40),
    target_entity_id   UUID,
    target_identifier  VARCHAR(500),
    link_type          VARCHAR(20),
    target_url         VARCHAR(2000),
    target_title       VARCHAR(255),
    created_at         TIMESTAMPTZ,
    updated_at         TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);
