-- Control pack: versioned installable content bundles (GC-P015, ADR-022)
CREATE TABLE control_pack (
    id               UUID         PRIMARY KEY,
    project_id       UUID         NOT NULL REFERENCES project(id),
    pack_id          VARCHAR(200) NOT NULL,
    version          VARCHAR(50)  NOT NULL,
    publisher        VARCHAR(200),
    description      TEXT,
    source_url       VARCHAR(2000),
    checksum         VARCHAR(128),
    compatibility    TEXT,
    pack_metadata    TEXT,
    lifecycle_state  VARCHAR(20)  NOT NULL DEFAULT 'INSTALLED',
    installed_at     TIMESTAMPTZ  NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_control_pack_project_pack_id UNIQUE (project_id, pack_id)
);

CREATE INDEX idx_control_pack_project ON control_pack (project_id);
CREATE INDEX idx_control_pack_lifecycle ON control_pack (project_id, lifecycle_state);

-- Audit table for control_pack
CREATE TABLE control_pack_audit (
    id               UUID         NOT NULL,
    rev              INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype          SMALLINT     NOT NULL,
    pack_id          VARCHAR(200),
    version          VARCHAR(50),
    publisher        VARCHAR(200),
    description      TEXT,
    source_url       VARCHAR(2000),
    checksum         VARCHAR(128),
    compatibility    TEXT,
    pack_metadata    TEXT,
    lifecycle_state  VARCHAR(20),
    installed_at     TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);

-- Control pack entry: individual control definitions within a pack
CREATE TABLE control_pack_entry (
    id                       UUID         PRIMARY KEY,
    control_pack_id          UUID         NOT NULL REFERENCES control_pack(id) ON DELETE CASCADE,
    control_id               UUID         NOT NULL REFERENCES control(id),
    entry_uid                VARCHAR(50)  NOT NULL,
    original_definition      TEXT         NOT NULL,
    expected_evidence        TEXT,
    implementation_guidance  TEXT,
    framework_mappings       TEXT,
    entry_status             VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at               TIMESTAMPTZ  NOT NULL,
    updated_at               TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_pack_entry_pack_uid UNIQUE (control_pack_id, entry_uid),
    CONSTRAINT uq_pack_entry_pack_control UNIQUE (control_pack_id, control_id)
);

CREATE INDEX idx_pack_entry_pack ON control_pack_entry (control_pack_id);
CREATE INDEX idx_pack_entry_control ON control_pack_entry (control_id);

-- Audit table for control_pack_entry
CREATE TABLE control_pack_entry_audit (
    id                       UUID         NOT NULL,
    rev                      INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype                  SMALLINT     NOT NULL,
    control_pack_id          UUID,
    entry_uid                VARCHAR(50),
    original_definition      TEXT,
    expected_evidence        TEXT,
    implementation_guidance  TEXT,
    framework_mappings       TEXT,
    entry_status             VARCHAR(20),
    PRIMARY KEY (id, rev)
);

-- Control pack override: field-level project-local tailoring
CREATE TABLE control_pack_override (
    id                       UUID         PRIMARY KEY,
    control_pack_entry_id    UUID         NOT NULL REFERENCES control_pack_entry(id) ON DELETE CASCADE,
    field_name               VARCHAR(100) NOT NULL,
    override_value           TEXT,
    reason                   VARCHAR(500),
    created_at               TIMESTAMPTZ  NOT NULL,
    updated_at               TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_pack_override_entry_field UNIQUE (control_pack_entry_id, field_name)
);

CREATE INDEX idx_pack_override_entry ON control_pack_override (control_pack_entry_id);

-- Audit table for control_pack_override
CREATE TABLE control_pack_override_audit (
    id                       UUID         NOT NULL,
    rev                      INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype                  SMALLINT     NOT NULL,
    control_pack_entry_id    UUID,
    field_name               VARCHAR(100),
    override_value           TEXT,
    reason                   VARCHAR(500),
    PRIMARY KEY (id, rev)
);
