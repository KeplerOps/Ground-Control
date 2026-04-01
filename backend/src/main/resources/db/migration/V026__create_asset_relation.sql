CREATE TABLE asset_relation (
    id            UUID PRIMARY KEY,
    source_id     UUID                     NOT NULL REFERENCES operational_asset(id) ON DELETE CASCADE,
    target_id     UUID                     NOT NULL REFERENCES operational_asset(id) ON DELETE CASCADE,
    relation_type VARCHAR(30)              NOT NULL,
    description   TEXT                     DEFAULT '',
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (source_id, target_id, relation_type)
);

CREATE INDEX idx_asset_rel_source ON asset_relation(source_id);
CREATE INDEX idx_asset_rel_target ON asset_relation(target_id);
