-- GC-M011: subtype discriminator and subtype-specific metadata on
-- operational_asset. Both columns are nullable so existing rows are not
-- retroactively classified beyond the existing AssetType.
ALTER TABLE operational_asset
    ADD COLUMN subtype  VARCHAR(100),
    ADD COLUMN metadata TEXT;

-- Index supports filter-by-classification scans (asset_type + subtype),
-- consistent with the GC-M012 filter indices on environment/criticality/
-- scope. Partial-null tolerant via the (asset_type, subtype) leading-prefix.
CREATE INDEX idx_asset_classification ON operational_asset(asset_type, subtype);
