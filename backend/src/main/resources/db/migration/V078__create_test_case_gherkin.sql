CREATE TABLE test_case_gherkin (
    id            UUID PRIMARY KEY,
    test_case_id  UUID         NOT NULL REFERENCES test_case(id),
    source        TEXT         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_test_case_gherkin_test_case UNIQUE (test_case_id)
);

CREATE INDEX idx_test_case_gherkin_test_case ON test_case_gherkin (test_case_id);
