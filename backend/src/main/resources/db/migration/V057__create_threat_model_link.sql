CREATE TABLE threat_model_link (
    id                 UUID PRIMARY KEY,
    threat_model_id    UUID          NOT NULL REFERENCES threat_model(id) ON DELETE CASCADE,
    target_type        VARCHAR(40)   NOT NULL,
    target_entity_id   UUID,
    target_identifier  VARCHAR(500),
    link_type          VARCHAR(20)   NOT NULL,
    target_url         VARCHAR(2000) NOT NULL DEFAULT '',
    target_title       VARCHAR(255)  NOT NULL DEFAULT '',
    created_at         TIMESTAMPTZ   NOT NULL,
    updated_at         TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uq_threat_model_link_identifier
        UNIQUE (threat_model_id, target_type, target_identifier, link_type),
    CONSTRAINT uq_threat_model_link_entity
        UNIQUE (threat_model_id, target_type, target_entity_id, link_type)
);

CREATE INDEX idx_tml_threat_model   ON threat_model_link (threat_model_id);
CREATE INDEX idx_tml_target_type    ON threat_model_link (target_type);
CREATE INDEX idx_tml_target_entity  ON threat_model_link (target_type, target_entity_id);
CREATE INDEX idx_tml_target_ident   ON threat_model_link (threat_model_id, target_type, target_identifier);
