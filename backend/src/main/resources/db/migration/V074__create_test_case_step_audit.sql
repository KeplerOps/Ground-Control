CREATE TABLE test_case_step_audit (
    id                UUID         NOT NULL,
    rev               INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype           SMALLINT     NOT NULL,
    test_case_id      UUID,
    step_number       INTEGER,
    action            TEXT,
    expected_result   TEXT,
    actual_result     TEXT,
    -- BaseEntity timestamps are @Audited (no @NotAudited override), so Envers
    -- writes them on every revision. Omitting these columns would silently
    -- fail audit inserts at flush time. Matches the V058/V061/V066 pattern.
    created_at        TIMESTAMPTZ,
    updated_at        TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);
