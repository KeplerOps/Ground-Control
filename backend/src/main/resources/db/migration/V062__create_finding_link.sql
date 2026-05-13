CREATE TABLE finding_link (
    id                 UUID PRIMARY KEY,
    finding_id         UUID          NOT NULL REFERENCES finding(id),
    target_type        VARCHAR(40)   NOT NULL,
    target_entity_id   UUID,
    target_identifier  VARCHAR(500),
    link_type          VARCHAR(20)   NOT NULL,
    target_url         VARCHAR(2000) NOT NULL DEFAULT '',
    target_title       VARCHAR(255)  NOT NULL DEFAULT '',
    created_at         TIMESTAMPTZ   NOT NULL,
    updated_at         TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uq_finding_link_identifier
        UNIQUE (finding_id, target_type, target_identifier, link_type),
    CONSTRAINT uq_finding_link_entity
        UNIQUE (finding_id, target_type, target_entity_id, link_type)
);

CREATE INDEX idx_fl_finding        ON finding_link (finding_id);
CREATE INDEX idx_fl_target_type    ON finding_link (target_type);
CREATE INDEX idx_fl_target_entity  ON finding_link (target_type, target_entity_id);
CREATE INDEX idx_fl_target_ident   ON finding_link (finding_id, target_type, target_identifier);
