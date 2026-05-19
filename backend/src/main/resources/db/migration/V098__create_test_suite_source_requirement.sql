-- TC-007 / ADR-047 — Test Suite requirements-based source.
--
-- A row exists only when its parent test_suite has population_mode =
-- 'REQUIREMENTS_BASED'. ON DELETE CASCADE from test_suite so removing
-- a suite drops its source rows; ON DELETE RESTRICT from requirement
-- so a requirement cannot be silently removed while still feeding a
-- suite. The suite resolves its member test cases from each source
-- requirement via the existing traceability_link rows
-- (link_type = 'TESTS', artifact_type = 'TEST') at read time; the
-- source row is the rule, not the cached outcome.
CREATE TABLE test_suite_source_requirement (
    id             UUID PRIMARY KEY,
    test_suite_id  UUID         NOT NULL REFERENCES test_suite(id) ON DELETE CASCADE,
    requirement_id UUID         NOT NULL REFERENCES requirement(id),
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_test_suite_source_requirement_pair
        UNIQUE (test_suite_id, requirement_id)
);

CREATE INDEX idx_test_suite_source_requirement_requirement
    ON test_suite_source_requirement (requirement_id);
