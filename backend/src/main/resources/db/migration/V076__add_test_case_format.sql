-- TC-004 / ADR-042 — authored format discriminator on test_case.
--
-- DEFAULT 'STEP_BASED' back-fills existing rows so the column is safely
-- NOT NULL without separate DML. New writes must supply the value
-- explicitly via the TestCase constructor; the default is only the
-- migration-time backfill for pre-V076 rows.
ALTER TABLE test_case
    ADD COLUMN format VARCHAR(20) NOT NULL DEFAULT 'STEP_BASED';
