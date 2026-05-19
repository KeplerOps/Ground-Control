-- TC-008 / ADR-049 — Envers audit shadow for test_run.
--
-- project_id, test_plan_id, and test_suite_id are intentionally absent
-- (@NotAudited on the JPA mappings). BaseEntity timestamps (created_at,
-- updated_at) are mirrored so AuditRetentionJob.purgeOldAuditRecords can
-- age run revisions alongside the rest of the test-management audit
-- tables.
CREATE TABLE test_run_audit (
    id          UUID         NOT NULL,
    rev         INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype     SMALLINT     NOT NULL,
    uid         VARCHAR(50),
    name        VARCHAR(200),
    environment VARCHAR(100),
    version     VARCHAR(100),
    build       VARCHAR(100),
    status      VARCHAR(20),
    start_at    TIMESTAMPTZ,
    end_at      TIMESTAMPTZ,
    created_at  TIMESTAMPTZ,
    updated_at  TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);
