CREATE TABLE test_case_step (
    id                UUID PRIMARY KEY,
    test_case_id      UUID         NOT NULL REFERENCES test_case(id),
    step_number       INTEGER      NOT NULL,
    action            TEXT         NOT NULL,
    expected_result   TEXT         NOT NULL,
    actual_result     TEXT,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_test_case_step_number UNIQUE (test_case_id, step_number),
    CONSTRAINT ck_test_case_step_number_positive CHECK (step_number > 0)
);

CREATE INDEX idx_test_case_step_test_case ON test_case_step (test_case_id);
CREATE INDEX idx_test_case_step_order ON test_case_step (test_case_id, step_number);
