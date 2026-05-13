CREATE TABLE control_test (
    id                UUID PRIMARY KEY,
    project_id        UUID         NOT NULL REFERENCES project(id),
    control_id        UUID         NOT NULL REFERENCES control(id),
    uid               VARCHAR(50)  NOT NULL,
    methodology       VARCHAR(30)  NOT NULL,
    test_steps        TEXT         NOT NULL,
    expected_results  TEXT         NOT NULL,
    actual_results    TEXT         NOT NULL,
    conclusion        VARCHAR(30)  NOT NULL,
    tester_identity   VARCHAR(200) NOT NULL,
    test_date         DATE         NOT NULL,
    notes             TEXT,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_control_test_project_uid UNIQUE (project_id, uid)
);

CREATE INDEX idx_control_test_project   ON control_test (project_id);
CREATE INDEX idx_control_test_control   ON control_test (control_id);
CREATE INDEX idx_control_test_test_date ON control_test (test_date);
