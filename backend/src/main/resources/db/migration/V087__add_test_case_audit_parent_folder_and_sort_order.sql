-- TC-005 / ADR-043 — Envers audit parity for test_case placement columns.
-- Audit columns are nullable per Envers convention (revisions before the
-- new field existed carry NULL).
ALTER TABLE test_case_audit
    ADD COLUMN parent_folder_id UUID,
    ADD COLUMN sort_order       INTEGER;
