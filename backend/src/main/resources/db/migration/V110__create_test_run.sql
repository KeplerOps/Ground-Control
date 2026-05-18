-- TC-008 / ADR-049 — Test Run Entity (root).
--
-- A TestRun is the execution-time record for one pass through a TestSuite
-- against a TestPlan for a specific environment / version / build window.
-- The aggregate is project-scoped (project_id, uid) unique, references the
-- driving plan and suite via FKs, and carries release-coordinate scalars
-- consistent with TestPlan. status uses a dedicated TestRunStatus
-- vocabulary distinct from TestCaseStatus / TestPlanStatus /
-- TestSuitePopulationMode / VerificationStatus / ControlTestConclusion;
-- the CHECK constraint backstops the JPA-side invariant.
CREATE TABLE test_run (
    id              UUID PRIMARY KEY,
    project_id      UUID         NOT NULL REFERENCES project(id),
    test_plan_id    UUID         NOT NULL REFERENCES test_plan(id),
    test_suite_id   UUID         NOT NULL REFERENCES test_suite(id),
    uid             VARCHAR(50)  NOT NULL,
    name            VARCHAR(200) NOT NULL,
    environment     VARCHAR(100),
    version         VARCHAR(100),
    build           VARCHAR(100),
    status          VARCHAR(20)  NOT NULL DEFAULT 'PLANNED',
    start_at        TIMESTAMPTZ,
    end_at          TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_test_run_project_uid UNIQUE (project_id, uid),
    CONSTRAINT ck_test_run_status
        CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'COMPLETED', 'ABORTED', 'ARCHIVED'))
);

CREATE INDEX idx_test_run_project    ON test_run (project_id);
CREATE INDEX idx_test_run_status     ON test_run (project_id, status);
CREATE INDEX idx_test_run_plan       ON test_run (test_plan_id);
CREATE INDEX idx_test_run_suite      ON test_run (test_suite_id);
