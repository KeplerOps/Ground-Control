CREATE TABLE audit_link (
    id                 UUID PRIMARY KEY,
    audit_id           UUID          NOT NULL REFERENCES audit(id),
    target_type        VARCHAR(40)   NOT NULL,
    target_entity_id   UUID,
    target_identifier  VARCHAR(500),
    link_type          VARCHAR(20)   NOT NULL,
    target_url         VARCHAR(2000) NOT NULL DEFAULT '',
    target_title       VARCHAR(255)  NOT NULL DEFAULT '',
    created_at         TIMESTAMPTZ   NOT NULL,
    updated_at         TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uq_audit_link_identifier
        UNIQUE (audit_id, target_type, target_identifier, link_type),
    CONSTRAINT uq_audit_link_entity
        UNIQUE (audit_id, target_type, target_entity_id, link_type)
);

CREATE INDEX idx_al_audit          ON audit_link (audit_id);
CREATE INDEX idx_al_target_type    ON audit_link (target_type);
CREATE INDEX idx_al_target_entity  ON audit_link (audit_id, target_type, target_entity_id);
CREATE INDEX idx_al_target_ident   ON audit_link (audit_id, target_type, target_identifier);
