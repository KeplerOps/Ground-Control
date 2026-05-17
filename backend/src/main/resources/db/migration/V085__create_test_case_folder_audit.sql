-- TC-005 / ADR-043 — Envers audit shadow for test_case_folder.
--
-- Project FK is intentionally absent (@NotAudited on TestCaseFolder.project);
-- parent_id is audited so move/rebalance operations are reconstructable.
-- BaseEntity timestamps are mirrored so AuditRetentionJob.purgeOldAuditRecords
-- can age out folder revisions alongside the other test-case audit tables.
CREATE TABLE test_case_folder_audit (
    id           UUID         NOT NULL,
    rev          INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype      SMALLINT     NOT NULL,
    parent_id    UUID,
    title        VARCHAR(200),
    description  TEXT,
    sort_order   INTEGER,
    created_at   TIMESTAMPTZ,
    updated_at   TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);
