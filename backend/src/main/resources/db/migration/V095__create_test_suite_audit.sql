-- TC-007 / ADR-047 — Envers audit shadow for test_suite.
--
-- Project FK is intentionally absent (@NotAudited on TestSuite.project),
-- mirroring test_plan_audit / test_case_folder_audit. BaseEntity
-- timestamps are mirrored so AuditRetentionJob.purgeOldAuditRecords can
-- age suite revisions out alongside the other test-management audit
-- tables. population_mode is recorded so the audit log shows which mode
-- the suite was created in (the field is immutable so it never changes
-- across revisions, but recording it once at INSERT makes the shadow
-- self-describing).
CREATE TABLE test_suite_audit (
    id                   UUID         NOT NULL,
    rev                  INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype              SMALLINT     NOT NULL,
    uid                  VARCHAR(50),
    name                 VARCHAR(200),
    description          TEXT,
    population_mode      VARCHAR(20),
    criteria_status      VARCHAR(20),
    criteria_type        VARCHAR(20),
    criteria_priority    VARCHAR(20),
    criteria_format      VARCHAR(20),
    criteria_folder_id   UUID,
    criteria_text_search VARCHAR(200),
    created_at           TIMESTAMPTZ,
    updated_at           TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);
