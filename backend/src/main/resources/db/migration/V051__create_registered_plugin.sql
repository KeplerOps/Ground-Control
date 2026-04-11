CREATE TABLE registered_plugin (
    id               UUID         PRIMARY KEY,
    project_id       UUID         NOT NULL REFERENCES project(id),
    name             VARCHAR(100) NOT NULL,
    version          VARCHAR(50)  NOT NULL,
    description      TEXT,
    plugin_type      VARCHAR(30)  NOT NULL,
    capabilities     TEXT,
    metadata         TEXT,
    lifecycle_state  VARCHAR(20)  NOT NULL,
    enabled          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_registered_plugin_project_name UNIQUE (project_id, name)
);

CREATE INDEX idx_registered_plugin_project ON registered_plugin (project_id);
CREATE INDEX idx_registered_plugin_type ON registered_plugin (project_id, plugin_type);
