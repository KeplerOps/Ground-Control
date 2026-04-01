CREATE TABLE architecture_decision_record_audit (
    id              UUID         NOT NULL,
    rev             INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype         SMALLINT     NOT NULL,
    project_id      UUID,
    uid             VARCHAR(20),
    title           VARCHAR(200),
    status          VARCHAR(20),
    decision_date   DATE,
    context         TEXT,
    decision        TEXT,
    consequences    TEXT,
    superseded_by   VARCHAR(20),
    created_by      VARCHAR(100),
    created_at      TIMESTAMP WITH TIME ZONE,
    updated_at      TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id, rev)
);
