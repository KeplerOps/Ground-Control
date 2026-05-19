-- Envers audit-table parity for the GC-M018 knowledge_state columns.
-- Audited columns must appear on the _audit table or Hibernate fails to
-- write revisions for those fields. Audit columns are nullable per the
-- Envers convention (mirrors V070 and V091).
ALTER TABLE operational_asset_audit
    ADD COLUMN knowledge_state VARCHAR(20);

ALTER TABLE asset_relation_audit
    ADD COLUMN knowledge_state VARCHAR(20);
