-- GC-M012: ownership, stewardship, environment, criticality, mission/business
-- context, and assurance-scope designation on the operational asset.
-- Columns are nullable so existing rows are not retroactively designated.
ALTER TABLE operational_asset
    ADD COLUMN owner             VARCHAR(200),
    ADD COLUMN steward           VARCHAR(200),
    ADD COLUMN environment       VARCHAR(20),
    ADD COLUMN criticality       VARCHAR(20),
    ADD COLUMN business_context  TEXT,
    ADD COLUMN scope_designation VARCHAR(20);

-- Filter indices for the GC-M012 queryability clause: ownership /
-- environment / criticality / scope appear in risk, control, audit, and
-- reporting filters; index the small-cardinality enum columns so those
-- queries scale as the asset inventory grows.
CREATE INDEX idx_asset_environment       ON operational_asset(environment);
CREATE INDEX idx_asset_criticality       ON operational_asset(criticality);
CREATE INDEX idx_asset_scope_designation ON operational_asset(scope_designation);
