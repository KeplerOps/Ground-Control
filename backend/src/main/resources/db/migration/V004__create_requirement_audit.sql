CREATE TABLE requirement_audit (
    id               UUID         NOT NULL,
    rev              INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype          SMALLINT     NOT NULL,
    uid              VARCHAR(50),
    title            VARCHAR(255),
    statement        TEXT,
    rationale        TEXT,
    requirement_type VARCHAR(20),
    priority         VARCHAR(10),
    status           VARCHAR(20),
    wave             INTEGER,
    created_at       TIMESTAMP WITH TIME ZONE,
    updated_at       TIMESTAMP WITH TIME ZONE,
    archived_at      TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id, rev)
);
