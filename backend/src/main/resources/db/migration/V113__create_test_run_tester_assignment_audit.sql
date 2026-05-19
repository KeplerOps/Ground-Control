-- TC-008 / ADR-049 — Envers audit shadow for test_run_tester_assignment.
--
-- test_run_id is preserved (RelationTargetAuditMode.NOT_AUDITED on the
-- JPA mapping), matching the test_suite_member_audit shape, so a tester
-- revision can be traced back to its parent run.
CREATE TABLE test_run_tester_assignment_audit (
    id           UUID        NOT NULL,
    rev          INTEGER     NOT NULL REFERENCES revinfo(rev),
    revtype      SMALLINT    NOT NULL,
    test_run_id  UUID,
    tester_name  VARCHAR(120),
    created_at   TIMESTAMPTZ,
    updated_at   TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);
