ALTER TABLE asset_relation_audit
    ADD COLUMN updated_at TIMESTAMPTZ;

UPDATE asset_relation_audit
SET updated_at = created_at
WHERE updated_at IS NULL;
