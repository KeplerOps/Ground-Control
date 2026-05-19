-- TC-007 / ADR-047 — Envers audit shadow for test_suite_source_requirement.
--
-- The identity-defining FKs (test_suite_id, requirement_id) stay in the
-- audit shadow so a source-row revision can be traced back to its parent
-- suite and the linked requirement after the live row has been deleted.
-- Both relations are mapped with @Audited(targetAuditMode = NOT_AUDITED)
-- on the JPA side so Envers writes the FK values without chasing the
-- target entities through the audit graph.
CREATE TABLE test_suite_source_requirement_audit (
    id             UUID         NOT NULL,
    rev            INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype        SMALLINT     NOT NULL,
    test_suite_id  UUID,
    requirement_id UUID,
    created_at     TIMESTAMPTZ,
    updated_at     TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);
