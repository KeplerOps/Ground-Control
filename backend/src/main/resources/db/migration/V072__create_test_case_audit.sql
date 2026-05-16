CREATE TABLE test_case_audit (
    id                          UUID         NOT NULL,
    rev                         INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype                     SMALLINT     NOT NULL,
    uid                         VARCHAR(50),
    title                       VARCHAR(200),
    description                 TEXT,
    preconditions               TEXT,
    postconditions              TEXT,
    priority                    VARCHAR(20),
    status                      VARCHAR(20),
    type                        VARCHAR(20),
    estimated_duration_seconds  BIGINT,
    PRIMARY KEY (id, rev)
);
