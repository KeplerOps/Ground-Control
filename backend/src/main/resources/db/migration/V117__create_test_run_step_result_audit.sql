-- TC-009 / ADR-050 — Envers audit shadow for test_run_step_result.
--
-- test_run_case_result_id and test_case_step_id are preserved
-- (RelationTargetAuditMode.NOT_AUDITED on the JPA mappings) so step-result
-- revisions remain traceable to their parent case-result and linked
-- authored step after the live row is deleted, mirroring the
-- test_run_case_result_audit shape from V115.
CREATE TABLE test_run_step_result_audit (
    id                          UUID         NOT NULL,
    rev                         INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype                     SMALLINT     NOT NULL,
    test_run_case_result_id     UUID,
    test_case_step_id           UUID,
    step_number_snapshot        INTEGER,
    action_snapshot             TEXT,
    expected_result_snapshot    TEXT,
    snapshot_order              INTEGER,
    status                      VARCHAR(20),
    comment                     TEXT,
    executed_at                 TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ,
    updated_at                  TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);
