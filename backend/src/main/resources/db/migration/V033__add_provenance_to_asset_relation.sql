ALTER TABLE asset_relation
    ADD COLUMN source_system      VARCHAR(100),
    ADD COLUMN external_source_id VARCHAR(500),
    ADD COLUMN collected_at       TIMESTAMPTZ,
    ADD COLUMN confidence         VARCHAR(50);
