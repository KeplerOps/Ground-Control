CREATE TABLE threat_model_audit (
    id              UUID        NOT NULL,
    rev             INTEGER     NOT NULL REFERENCES revinfo(rev),
    revtype         SMALLINT,
    uid             VARCHAR(30),
    title           VARCHAR(200),
    status          VARCHAR(20),
    threat_source   TEXT,
    threat_event    TEXT,
    effect          TEXT,
    stride          VARCHAR(30),
    narrative       TEXT,
    created_by      VARCHAR(100),
    created_at      TIMESTAMPTZ,
    updated_at      TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);
