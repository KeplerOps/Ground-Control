CREATE TABLE verification_result_audit (
    id               UUID         NOT NULL,
    rev              INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype          SMALLINT     NOT NULL,
    prover           VARCHAR(100),
    property         TEXT,
    result           VARCHAR(20),
    assurance_level  VARCHAR(5),
    evidence         TEXT,
    verified_at      TIMESTAMPTZ,
    expires_at       TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);
