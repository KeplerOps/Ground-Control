-- TC-009 / ADR-050 — Pause/resume cursor on TestRun.
--
-- Two nullable UUID columns capture "where the tester left off" so the
-- runner UI can resume after a reload, tab close, or pause. They reference
-- test_run_case_result(id) and test_run_step_result(id) by identity but
-- intentionally do NOT carry FK constraints: deleting a stale case-result
-- or step-result should not be blocked by a stale cursor pointer, and the
-- service layer null-handles a cursor that no longer resolves.
--
-- The cursor is ephemeral UI navigation state, NOT historical evidence
-- of execution. The JPA mappings carry @NotAudited so Envers ignores
-- cursor movement; without that, every step recorded would also write a
-- TestRun revision (one cursor bump per step), bloating the audit log
-- with no compliance value. No corresponding alter to test_run_audit.
ALTER TABLE test_run
    ADD COLUMN current_case_result_id UUID,
    ADD COLUMN current_step_result_id UUID;
