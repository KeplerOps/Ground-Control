-- TC-006 / ADR-044 — Test Plan Entity.
--
-- TestPlan is the top-level planning container for a testing effort: it owns
-- scope metadata (name, description), release coordinates (product, version,
-- build) as bounded scalar text (no Product/Release/Build aggregate exists),
-- a lifecycle status, planned start/end dates, and a stable UUID PK. The PK
-- is the load-bearing extensibility seam: future TestRun rows will FK back
-- via test_run.test_plan_id to satisfy "group multiple test runs under a
-- single plan" without storing run IDs as JSON on the plan.
CREATE TABLE test_plan (
    id          UUID PRIMARY KEY,
    project_id  UUID         NOT NULL REFERENCES project(id),
    uid         VARCHAR(50)  NOT NULL,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    product     VARCHAR(200),
    version     VARCHAR(100),
    build       VARCHAR(100),
    status      VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    start_date  DATE,
    end_date    DATE,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_test_plan_project_uid UNIQUE (project_id, uid)
);

CREATE INDEX idx_test_plan_project ON test_plan (project_id);
CREATE INDEX idx_test_plan_status ON test_plan (project_id, status);
