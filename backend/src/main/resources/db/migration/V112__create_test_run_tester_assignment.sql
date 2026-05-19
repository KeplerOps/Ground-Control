-- TC-008 / ADR-049 — Per-tester assignment on a TestRun.
--
-- Tester names are domain-provenance values (max 120 chars), not foreign
-- references to Spring Security's users / authorities tables (ADR-037
-- bounds those to session-credential storage). One row per tester per run.
CREATE TABLE test_run_tester_assignment (
    id           UUID PRIMARY KEY,
    test_run_id  UUID         NOT NULL REFERENCES test_run(id),
    tester_name  VARCHAR(120) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_test_run_tester UNIQUE (test_run_id, tester_name)
);

CREATE INDEX idx_test_run_tester_run ON test_run_tester_assignment (test_run_id);
