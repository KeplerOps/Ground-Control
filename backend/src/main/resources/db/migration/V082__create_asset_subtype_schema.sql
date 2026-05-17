-- GC-M011: per-project per-(asset_type, subtype) schema registry. Exactly
-- one ACTIVE row per (project_id, asset_type, subtype) is enforced in the
-- service layer (AssetService.registerSubtypeSchema auto-deprecates the
-- prior ACTIVE row on a new registration); the unique constraint here is
-- on the full key so prior version rows persist as DEPRECATED.
CREATE TABLE asset_subtype_schema (
    id              UUID PRIMARY KEY,
    project_id      UUID         NOT NULL REFERENCES project(id),
    asset_type      VARCHAR(20)  NOT NULL,
    subtype         VARCHAR(100) NOT NULL,
    schema_version  VARCHAR(50)  NOT NULL,
    description     TEXT,
    schema_body     TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_asset_subtype_schema_version
        UNIQUE (project_id, asset_type, subtype, schema_version)
);

CREATE INDEX idx_asset_subtype_schema_project ON asset_subtype_schema (project_id);
CREATE INDEX idx_asset_subtype_schema_lookup ON asset_subtype_schema (project_id, asset_type, subtype, status);

-- Database-enforced one-ACTIVE-per-(project_id, asset_type, subtype) invariant.
-- Without this, a service-layer read-then-write could race two concurrent
-- registrations into both committing ACTIVE rows for the same triple, which
-- breaks `findByProjectIdAndAssetTypeAndSubtypeAndStatus(... ACTIVE)`'s
-- single-row assumption and silently picks one schema for validation.
CREATE UNIQUE INDEX uk_asset_subtype_schema_active
    ON asset_subtype_schema (project_id, asset_type, subtype)
    WHERE status = 'ACTIVE';
