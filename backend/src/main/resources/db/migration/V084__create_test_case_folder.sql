-- TC-005 / ADR-043 — Hierarchical Test Case Organization.
--
-- TestCaseFolder is the test-repository organisation aggregate. It is
-- project-scoped and self-referencing; a folder with parent_id IS NULL
-- sits at the project root. Sibling ordering is container-local: the
-- (project_id, parent_id, sort_order) index keeps drag-and-drop reorder
-- queries cheap without touching siblings outside the container.
--
-- Sibling-title uniqueness is enforced by two partial unique indexes
-- because PostgreSQL treats NULL parents as distinct under a plain
-- UNIQUE constraint: one index covers the project-root case (parent IS
-- NULL), and one covers non-root containers.
CREATE TABLE test_case_folder (
    id           UUID PRIMARY KEY,
    project_id   UUID         NOT NULL REFERENCES project(id),
    parent_id    UUID         REFERENCES test_case_folder(id),
    title        VARCHAR(200) NOT NULL,
    description  TEXT,
    sort_order   INTEGER      NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_test_case_folder_project ON test_case_folder (project_id);
CREATE INDEX idx_test_case_folder_parent ON test_case_folder (parent_id);
CREATE INDEX idx_test_case_folder_container
    ON test_case_folder (project_id, parent_id, sort_order);

CREATE UNIQUE INDEX uq_test_case_folder_title_root
    ON test_case_folder (project_id, title)
    WHERE parent_id IS NULL;

CREATE UNIQUE INDEX uq_test_case_folder_title_under_parent
    ON test_case_folder (project_id, parent_id, title)
    WHERE parent_id IS NOT NULL;
