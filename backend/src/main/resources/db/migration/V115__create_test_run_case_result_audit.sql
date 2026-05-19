-- TC-008 / ADR-049 — Envers audit shadow for test_run_case_result.
--
-- test_run_id and test_case_id are preserved (RelationTargetAuditMode.NOT_AUDITED
-- on the JPA mappings), so result-row revisions can be traced back to their
-- parent run and linked case after the live row is deleted.
CREATE TABLE test_run_case_result_audit (
    id              UUID         NOT NULL,
    rev             INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype         SMALLINT     NOT NULL,
    test_run_id     UUID,
    test_case_id    UUID,
    test_case_uid   VARCHAR(50),
    test_case_title VARCHAR(200),
    snapshot_order  INTEGER,
    status          VARCHAR(20),
    notes           TEXT,
    created_at      TIMESTAMPTZ,
    updated_at      TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);
