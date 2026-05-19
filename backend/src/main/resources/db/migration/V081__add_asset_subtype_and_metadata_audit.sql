-- Envers audit-table parity for the GC-M011 OperationalAsset fields.
-- @Audited columns must appear in the _audit table or Hibernate fails to
-- write revisions for those fields.
ALTER TABLE operational_asset_audit
    ADD COLUMN subtype  VARCHAR(100),
    ADD COLUMN metadata TEXT;
