-- TC-004 / ADR-042 — Envers audit parity for test_case.format. Audit
-- columns are nullable per Envers convention (revisions before the new
-- field existed carry NULL).
ALTER TABLE test_case_audit
    ADD COLUMN format VARCHAR(20);
