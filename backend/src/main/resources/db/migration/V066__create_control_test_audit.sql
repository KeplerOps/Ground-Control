CREATE TABLE control_test_audit (
    id                UUID         NOT NULL,
    rev               INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype           SMALLINT,
    control_id        UUID,
    uid               VARCHAR(50),
    methodology       VARCHAR(30),
    test_steps        TEXT,
    expected_results  TEXT,
    actual_results    TEXT,
    conclusion        VARCHAR(30),
    tester_identity   VARCHAR(200),
    test_date         DATE,
    notes             TEXT,
    created_at        TIMESTAMPTZ,
    updated_at        TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);
