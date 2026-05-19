-- Envers audit-table parity for the GC-M012 OperationalAsset fields.
-- @Audited columns must appear in the _audit table or Hibernate fails to
-- write revisions for those fields.
ALTER TABLE operational_asset_audit
    ADD COLUMN owner             VARCHAR(200),
    ADD COLUMN steward           VARCHAR(200),
    ADD COLUMN environment       VARCHAR(20),
    ADD COLUMN criticality       VARCHAR(20),
    ADD COLUMN business_context  TEXT,
    ADD COLUMN scope_designation VARCHAR(20);
