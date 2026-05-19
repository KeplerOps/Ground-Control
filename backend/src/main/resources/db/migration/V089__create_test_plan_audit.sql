-- TC-006 / ADR-044 — Envers audit shadow for test_plan.
--
-- Project FK is intentionally absent (@NotAudited on TestPlan.project).
-- BaseEntity timestamps (created_at, updated_at) are mirrored so
-- AuditRetentionJob.purgeOldAuditRecords can age plan revisions out
-- alongside the other test-management audit tables.
CREATE TABLE test_plan_audit (
    id          UUID         NOT NULL,
    rev         INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype     SMALLINT     NOT NULL,
    uid         VARCHAR(50),
    name        VARCHAR(200),
    description TEXT,
    product     VARCHAR(200),
    version     VARCHAR(100),
    build       VARCHAR(100),
    status      VARCHAR(20),
    start_date  DATE,
    end_date    DATE,
    created_at  TIMESTAMPTZ,
    updated_at  TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);
