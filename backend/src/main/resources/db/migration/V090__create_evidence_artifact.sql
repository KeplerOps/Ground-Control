-- V090: evidence_artifact (GC-M016 / ADR-045).
--
-- First-class summarized-evidence aggregate. The supersede mechanism is
-- enforced in EvidenceArtifactService — once superseded_by_artifact_id is
-- non-null the service refuses further mutation of this row. No PUT/DELETE
-- controller surface exists; the only writes are inserts and the one-shot
-- supersede update.
CREATE TABLE evidence_artifact (
    id                          UUID PRIMARY KEY,
    project_id                  UUID         NOT NULL REFERENCES project(id),
    uid                         VARCHAR(50)  NOT NULL,
    title                       VARCHAR(200) NOT NULL,
    summary                     TEXT         NOT NULL,
    evidence_type               VARCHAR(40)  NOT NULL,
    derivation_method           VARCHAR(200) NOT NULL,
    derived_at                  TIMESTAMPTZ  NOT NULL,
    derived_by                  VARCHAR(200),
    assurance_level             VARCHAR(10),
    confidence                  VARCHAR(50),
    notes                       TEXT,
    superseded_by_artifact_id   UUID,
    -- JSON array of EvidenceSourceRef objects ({sourceKind, sourceEntityId,
    -- sourceIdentifier, role}). Each source's project scoping is enforced
    -- by the service layer at create time, not by the schema, so internal
    -- references don't need a per-kind join table for this many-to-many
    -- derivation seam.
    sources                     TEXT,
    created_at                  TIMESTAMPTZ  NOT NULL,
    updated_at                  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_evidence_artifact_project_uid UNIQUE (project_id, uid)
);

CREATE INDEX idx_evidence_artifact_project           ON evidence_artifact (project_id);
CREATE INDEX idx_evidence_artifact_project_derived   ON evidence_artifact (project_id, derived_at DESC);
CREATE INDEX idx_evidence_artifact_evidence_type     ON evidence_artifact (evidence_type);
CREATE INDEX idx_evidence_artifact_superseded_by     ON evidence_artifact (superseded_by_artifact_id);
