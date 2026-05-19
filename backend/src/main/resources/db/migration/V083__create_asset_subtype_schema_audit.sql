-- BaseEntity audits `created_at` and `updated_at` by default; omitting them
-- makes Envers fail at runtime with a missing-column SQL error on the first
-- revision write (matches the `operational_asset_audit` / `methodology_
-- profile_audit` convention).
CREATE TABLE asset_subtype_schema_audit (
    id              UUID         NOT NULL,
    rev             INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype         SMALLINT     NOT NULL,
    asset_type      VARCHAR(20),
    subtype         VARCHAR(100),
    schema_version  VARCHAR(50),
    description     TEXT,
    schema_body     TEXT,
    status          VARCHAR(20),
    created_at      TIMESTAMPTZ,
    updated_at      TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);
