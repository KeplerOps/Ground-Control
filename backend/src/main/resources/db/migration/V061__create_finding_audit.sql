CREATE TABLE finding_audit (
    id                  UUID        NOT NULL,
    rev                 INTEGER     NOT NULL REFERENCES revinfo(rev),
    revtype             SMALLINT,
    uid                 VARCHAR(30),
    title               VARCHAR(200),
    finding_type        VARCHAR(30),
    severity            VARCHAR(20),
    status              VARCHAR(30),
    description         TEXT,
    root_cause_analysis TEXT,
    owner               VARCHAR(100),
    due_date            DATE,
    created_by          VARCHAR(100),
    created_at          TIMESTAMPTZ,
    updated_at          TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);
