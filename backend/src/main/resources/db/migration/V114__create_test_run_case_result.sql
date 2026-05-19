-- TC-008 / ADR-049 — Per-case execution result on a TestRun.
--
-- One row per snapshotted test case per run; the snapshot is the canonical
-- membership of the run. test_case_uid and test_case_title capture the
-- case identity at snapshot time so later edits to the linked TestCase
-- never rewrite the run's historical evidence. status uses a dedicated
-- TestRunCaseResultStatus vocabulary; the CHECK constraint backstops the
-- JPA-side invariant.
CREATE TABLE test_run_case_result (
    id              UUID PRIMARY KEY,
    test_run_id     UUID         NOT NULL REFERENCES test_run(id),
    test_case_id    UUID         NOT NULL REFERENCES test_case(id),
    test_case_uid   VARCHAR(50)  NOT NULL,
    test_case_title VARCHAR(200) NOT NULL,
    -- snapshot_order preserves the position the resolver returned at create
    -- time. STATIC suites surface author-defined `TestSuiteMember.position`
    -- order; QUERY_BASED / REQUIREMENTS_BASED suites surface deterministic
    -- UID order. Persisting the index here keeps the run-side snapshot
    -- contract honest: reads always replay the resolved-at-create order
    -- even if the linked TestCase.uid is renamed later.
    snapshot_order  INTEGER      NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'NOT_RUN',
    notes           TEXT,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_test_run_case_result UNIQUE (test_run_id, test_case_id),
    CONSTRAINT uq_test_run_case_result_order UNIQUE (test_run_id, snapshot_order),
    CONSTRAINT ck_test_run_case_result_status
        CHECK (status IN ('NOT_RUN', 'PASSED', 'FAILED', 'BLOCKED', 'SKIPPED'))
);

CREATE INDEX idx_test_run_case_result_run    ON test_run_case_result (test_run_id);
CREATE INDEX idx_test_run_case_result_status ON test_run_case_result (test_run_id, status);
CREATE INDEX idx_test_run_case_result_case   ON test_run_case_result (test_case_id);
