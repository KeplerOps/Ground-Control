CREATE TABLE control_effectiveness_assessment (
    id                       UUID PRIMARY KEY,
    project_id               UUID         NOT NULL REFERENCES project(id),
    control_id               UUID         NOT NULL REFERENCES control(id),
    uid                      VARCHAR(50)  NOT NULL,
    design_effectiveness     VARCHAR(30)  NOT NULL,
    operating_effectiveness  VARCHAR(30)  NOT NULL,
    assessed_at              DATE         NOT NULL,
    assessor                 VARCHAR(200) NOT NULL,
    rationale                TEXT,
    notes                    TEXT,
    -- JSON array of ControlTest UUID strings; each must resolve to a ControlTest in the same
    -- project (enforced at the service layer, not the schema, so we don't need a join table for
    -- this many-to-many seam). Nullable / empty when the assessment has no supporting tests.
    supporting_test_ids      TEXT,
    created_at               TIMESTAMPTZ  NOT NULL,
    updated_at               TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_control_effectiveness_assessment_project_uid UNIQUE (project_id, uid)
);

CREATE INDEX idx_control_effectiveness_assessment_project     ON control_effectiveness_assessment (project_id);
CREATE INDEX idx_control_effectiveness_assessment_control     ON control_effectiveness_assessment (control_id);
CREATE INDEX idx_control_effectiveness_assessment_assessed_at ON control_effectiveness_assessment (assessed_at);
