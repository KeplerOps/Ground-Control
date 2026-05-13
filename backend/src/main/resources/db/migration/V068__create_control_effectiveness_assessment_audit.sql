CREATE TABLE control_effectiveness_assessment_audit (
    id                       UUID         NOT NULL,
    rev                      INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype                  SMALLINT,
    control_id               UUID,
    uid                      VARCHAR(50),
    design_effectiveness     VARCHAR(30),
    operating_effectiveness  VARCHAR(30),
    assessed_at              DATE,
    assessor                 VARCHAR(200),
    rationale                TEXT,
    notes                    TEXT,
    supporting_test_ids      TEXT,
    created_at               TIMESTAMPTZ,
    updated_at               TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);
