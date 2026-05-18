CREATE TABLE audit_audit (
    id                UUID        NOT NULL,
    rev               INTEGER     NOT NULL REFERENCES revinfo(rev),
    revtype           SMALLINT,
    uid               VARCHAR(30),
    title             VARCHAR(200),
    audit_type        VARCHAR(20),
    status            VARCHAR(30),
    scope_description TEXT,
    objectives        TEXT,
    phases            TEXT,
    team_members      TEXT,
    created_by        VARCHAR(100),
    created_at        TIMESTAMPTZ,
    updated_at        TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);
