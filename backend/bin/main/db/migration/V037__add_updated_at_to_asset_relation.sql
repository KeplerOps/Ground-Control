ALTER TABLE asset_relation
    ADD COLUMN updated_at TIMESTAMPTZ;

UPDATE asset_relation
SET updated_at = created_at
WHERE updated_at IS NULL;

ALTER TABLE asset_relation
    ALTER COLUMN updated_at SET NOT NULL;
