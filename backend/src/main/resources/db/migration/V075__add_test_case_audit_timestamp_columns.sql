-- V072 (TC-001) created `test_case_audit` without the inherited BaseEntity
-- timestamp columns. `TestCase` extends BaseEntity with no @NotAudited
-- override on `createdAt` / `updatedAt`, so Envers should be writing them on
-- every revision — the missing columns make those INSERTs silently drop the
-- values (or fail at flush, depending on dialect). Forward-fix: add the
-- columns nullable so prior rows back-fill cleanly. Pattern matches V058 /
-- V061 / V066 audit tables. See codex pre-push review on issue #670.
ALTER TABLE test_case_audit
    ADD COLUMN created_at TIMESTAMPTZ,
    ADD COLUMN updated_at TIMESTAMPTZ;
