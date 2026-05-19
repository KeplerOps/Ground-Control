-- V091: evidence_artifact_audit (GC-M016 / ADR-045).
--
-- Envers shadow table for evidence_artifact. AuditRetentionJob ages out rows
-- here using the BaseEntity timestamps (created_at / updated_at), so the
-- shadow must include them — same convention as the other audit tables.
CREATE TABLE evidence_artifact_audit (
    id                          UUID         NOT NULL,
    rev                         INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype                     SMALLINT,
    project_id                  UUID,
    uid                         VARCHAR(50),
    title                       VARCHAR(200),
    summary                     TEXT,
    evidence_type               VARCHAR(40),
    derivation_method           VARCHAR(200),
    derived_at                  TIMESTAMPTZ,
    derived_by                  VARCHAR(200),
    assurance_level             VARCHAR(10),
    confidence                  VARCHAR(50),
    notes                       TEXT,
    superseded_by_artifact_id   UUID,
    sources                     TEXT,
    created_at                  TIMESTAMPTZ,
    updated_at                  TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);
