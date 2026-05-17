CREATE TABLE test_case_gherkin_audit (
    id            UUID         NOT NULL,
    rev           INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype       SMALLINT     NOT NULL,
    test_case_id  UUID,
    source        TEXT,
    -- BaseEntity timestamps are @Audited (no @NotAudited override), so Envers
    -- writes them on every revision. The V075 pattern includes both columns
    -- on every audit table to prevent flush-time failures.
    created_at    TIMESTAMPTZ,
    updated_at    TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);
