-- TC-007 / ADR-047 — Test Suite static membership.
--
-- A row exists only when its parent test_suite has population_mode = 'STATIC'.
-- ON DELETE CASCADE from test_suite so removing a suite drops all its
-- member rows; ON DELETE RESTRICT from test_case so a test case cannot
-- be silently removed while still part of a suite. position is
-- author-defined and contiguous within a suite (the service compacts on
-- remove / reorder); the (suite, position) index supports ordered reads.
CREATE TABLE test_suite_member (
    id            UUID PRIMARY KEY,
    test_suite_id UUID         NOT NULL REFERENCES test_suite(id) ON DELETE CASCADE,
    test_case_id  UUID         NOT NULL REFERENCES test_case(id),
    position      INTEGER      NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_test_suite_member_pair UNIQUE (test_suite_id, test_case_id),
    -- Codex pre-push cycle 2: per-suite position uniqueness backstops the
    -- service-level shift/compact logic against duplicate / gapped
    -- positions. DEFERRABLE INITIALLY DEFERRED lets the service shift
    -- multiple rows in a single transaction without temporary mid-flush
    -- collisions (the constraint is only enforced at COMMIT).
    CONSTRAINT uq_test_suite_member_position
        UNIQUE (test_suite_id, position) DEFERRABLE INITIALLY DEFERRED
);

CREATE INDEX idx_test_suite_member_order
    ON test_suite_member (test_suite_id, position);
CREATE INDEX idx_test_suite_member_test_case
    ON test_suite_member (test_case_id);
