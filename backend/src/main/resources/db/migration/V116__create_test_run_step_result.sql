-- TC-009 / ADR-050 — Per-step execution result on a TestRunCaseResult.
--
-- One row per snapshotted step per case-result, created at run-create time
-- by snapshotting the live TestCaseSteps in step-number order. The snapshot
-- preserves authored step content (action_snapshot / expected_result_snapshot
-- / step_number_snapshot) so later edits to the authored TestCaseStep — or
-- a renumbering of the case's steps — never rewrite the run's historical
-- evidence. test_case_step_id keeps the link to the live step for join
-- queries; the snapshot fields are authoritative for replay.
--
-- The status vocabulary mirrors TestRunCaseResultStatus (TC-008 / ADR-049)
-- intentionally: a tester needs the same five outcomes (NOT_RUN / PASSED /
-- FAILED / BLOCKED / SKIPPED) at the step level as at the case level.
-- Inventing a parallel enum would split documentation and frontend mirrors
-- for no semantic gain (ADR-034 contract).
CREATE TABLE test_run_step_result (
    id                          UUID PRIMARY KEY,
    test_run_case_result_id     UUID         NOT NULL REFERENCES test_run_case_result(id),
    test_case_step_id           UUID         NOT NULL REFERENCES test_case_step(id),
    -- Snapshot fields. The authored step may later be renumbered or have its
    -- action / expected_result rewritten; the run replays exactly what the
    -- tester saw at run-create time.
    step_number_snapshot        INTEGER      NOT NULL,
    action_snapshot             TEXT         NOT NULL,
    expected_result_snapshot    TEXT         NOT NULL,
    -- snapshot_order preserves the resolver's deterministic order at create
    -- time (step_number_asc), so reads always replay the create-time
    -- sequence even if the authored step list is later resorted. Kept as a
    -- separate column from step_number_snapshot so a future renumbering of
    -- the authored steps (TC-002 supports it) doesn't drag a run's ordering
    -- with it.
    snapshot_order              INTEGER      NOT NULL,
    status                      VARCHAR(20)  NOT NULL DEFAULT 'NOT_RUN',
    comment                     TEXT,
    executed_at                 TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ  NOT NULL,
    updated_at                  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_test_run_step_result
        UNIQUE (test_run_case_result_id, test_case_step_id),
    CONSTRAINT uq_test_run_step_result_order
        UNIQUE (test_run_case_result_id, snapshot_order),
    CONSTRAINT ck_test_run_step_result_status
        CHECK (status IN ('NOT_RUN', 'PASSED', 'FAILED', 'BLOCKED', 'SKIPPED'))
);

CREATE INDEX idx_test_run_step_result_case
    ON test_run_step_result (test_run_case_result_id);
CREATE INDEX idx_test_run_step_result_step
    ON test_run_step_result (test_case_step_id);
CREATE INDEX idx_test_run_step_result_status
    ON test_run_step_result (test_run_case_result_id, status);
