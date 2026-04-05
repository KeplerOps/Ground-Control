CREATE TABLE control (
    id                    UUID PRIMARY KEY,
    project_id            UUID         NOT NULL REFERENCES project(id),
    uid                   VARCHAR(50)  NOT NULL,
    title                 VARCHAR(200) NOT NULL,
    description           TEXT,
    objective             TEXT,
    control_function      VARCHAR(20)  NOT NULL,
    status                VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    owner                 VARCHAR(200),
    implementation_scope  TEXT,
    methodology_factors   TEXT,
    effectiveness         TEXT,
    category              VARCHAR(100),
    source                VARCHAR(200),
    created_at            TIMESTAMPTZ  NOT NULL,
    updated_at            TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_control_project_uid UNIQUE (project_id, uid)
);

CREATE INDEX idx_control_project ON control (project_id);
CREATE INDEX idx_control_status ON control (status);
CREATE INDEX idx_control_uid ON control (project_id, uid);

CREATE TABLE control_link (
    id                UUID PRIMARY KEY,
    control_id        UUID         NOT NULL REFERENCES control(id) ON DELETE CASCADE,
    target_type       VARCHAR(30)  NOT NULL,
    target_entity_id  UUID,
    target_identifier VARCHAR(500),
    link_type         VARCHAR(20)  NOT NULL,
    target_url        VARCHAR(2000) NOT NULL DEFAULT '',
    target_title      VARCHAR(255)  NOT NULL DEFAULT '',
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_control_link_ext UNIQUE (control_id, target_type, target_identifier, link_type)
);

CREATE UNIQUE INDEX uq_control_link_internal
    ON control_link(control_id, target_type, target_entity_id, link_type)
    WHERE target_entity_id IS NOT NULL;

CREATE INDEX idx_control_link_control ON control_link (control_id);
CREATE INDEX idx_control_link_target_entity ON control_link (target_type, target_entity_id);
