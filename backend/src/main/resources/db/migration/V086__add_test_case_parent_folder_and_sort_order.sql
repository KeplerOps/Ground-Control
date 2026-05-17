-- TC-005 / ADR-043 — Hierarchical placement for existing test_case rows.
--
-- parent_folder_id IS NULL means the test case sits at the project root.
-- The NOT NULL DEFAULT 0 on sort_order makes the ALTER TABLE safe; the
-- explicit back-fill below replaces the default with a dense per-container
-- ranking so existing rows have deterministic ordering immediately, not
-- a wall of ties at 0. New writes set sort_order explicitly via
-- TestCaseService (max+1 of the target container when the caller omits a
-- value).
ALTER TABLE test_case
    ADD COLUMN parent_folder_id UUID REFERENCES test_case_folder(id),
    ADD COLUMN sort_order       INTEGER NOT NULL DEFAULT 0;

CREATE INDEX idx_test_case_parent_folder ON test_case (parent_folder_id);
CREATE INDEX idx_test_case_container
    ON test_case (project_id, parent_folder_id, sort_order);

-- Back-fill: assign sort_order = row_number per (project_id, parent_folder_id)
-- - 1, ordered deterministically by (created_at, id). Without this every
-- pre-V086 row would sit at sort_order = 0 and tree/reorder reads would
-- have to fall back to a tie-breaker for every container in the system.
WITH numbered AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY project_id, parent_folder_id
               ORDER BY created_at, id
           ) - 1 AS new_sort_order
    FROM test_case
)
UPDATE test_case t
SET sort_order = numbered.new_sort_order
FROM numbered
WHERE t.id = numbered.id;
