-- TC-007 / ADR-047 — Test Suite Entity (root).
--
-- A TestSuite is the selection container for test cases inside a project.
-- One row carries a project-scoped UID, name, optional description, an
-- immutable population_mode discriminator, and the per-mode criteria
-- columns that QUERY_BASED suites populate. STATIC and REQUIREMENTS_BASED
-- suites carry their population in sibling tables (test_suite_member /
-- test_suite_source_requirement). The CHECK constraint on population_mode
-- backstops the entity-side invariant in case a hand-written migration
-- bypasses the JPA layer.
CREATE TABLE test_suite (
    id                   UUID PRIMARY KEY,
    project_id           UUID         NOT NULL REFERENCES project(id),
    uid                  VARCHAR(50)  NOT NULL,
    name                 VARCHAR(200) NOT NULL,
    description          TEXT,
    population_mode      VARCHAR(20)  NOT NULL,
    criteria_status      VARCHAR(20),
    criteria_type        VARCHAR(20),
    criteria_priority    VARCHAR(20),
    criteria_format      VARCHAR(20),
    criteria_folder_id   UUID         REFERENCES test_case_folder(id),
    criteria_text_search VARCHAR(200),
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_test_suite_project_uid UNIQUE (project_id, uid),
    CONSTRAINT ck_test_suite_population_mode
        CHECK (population_mode IN ('STATIC', 'REQUIREMENTS_BASED', 'QUERY_BASED'))
);

CREATE INDEX idx_test_suite_project ON test_suite (project_id);
CREATE INDEX idx_test_suite_population_mode ON test_suite (project_id, population_mode);
