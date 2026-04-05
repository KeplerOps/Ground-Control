CREATE TABLE asset_external_id (
    id            UUID PRIMARY KEY,
    asset_id      UUID         NOT NULL REFERENCES operational_asset(id) ON DELETE CASCADE,
    source_system VARCHAR(100) NOT NULL,
    source_id     VARCHAR(500) NOT NULL,
    collected_at  TIMESTAMPTZ,
    confidence    VARCHAR(50),
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_asset_external_id UNIQUE (asset_id, source_system, source_id)
);

CREATE INDEX idx_asset_ext_id_asset ON asset_external_id (asset_id);
CREATE INDEX idx_asset_ext_id_source ON asset_external_id (source_system, source_id);
