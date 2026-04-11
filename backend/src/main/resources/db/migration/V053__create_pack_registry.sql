-- =============================================================================
-- V053: Pack Registry, Install Records, and Trust Policy tables
-- Implements GC-P016: Pack Registry, Resolution, and Trust Model
-- =============================================================================

-- ---------------------------------------------------------------------------
-- pack_registry_entry: catalog of available/discoverable packs
-- ---------------------------------------------------------------------------
CREATE TABLE pack_registry_entry (
    id                UUID         PRIMARY KEY,
    project_id        UUID         NOT NULL REFERENCES project(id),
    pack_id           VARCHAR(200) NOT NULL,
    pack_type         VARCHAR(30)  NOT NULL,
    publisher         VARCHAR(200),
    version           VARCHAR(50)  NOT NULL,
    description       TEXT,
    source_url        VARCHAR(2000),
    checksum          VARCHAR(128),
    signature_info    TEXT,
    compatibility     TEXT,
    dependencies      TEXT,
    provenance        TEXT,
    registry_metadata TEXT,
    catalog_status    VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',
    registered_at     TIMESTAMPTZ  NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_pack_registry_project_pack_version UNIQUE (project_id, pack_id, version)
);

CREATE INDEX idx_pack_registry_project ON pack_registry_entry (project_id);
CREATE INDEX idx_pack_registry_pack_id ON pack_registry_entry (project_id, pack_id);
CREATE INDEX idx_pack_registry_type ON pack_registry_entry (project_id, pack_type);
CREATE INDEX idx_pack_registry_status ON pack_registry_entry (project_id, catalog_status);

-- Envers audit table
CREATE TABLE pack_registry_entry_audit (
    id                UUID         NOT NULL,
    rev               INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype           SMALLINT     NOT NULL,
    pack_id           VARCHAR(200),
    pack_type         VARCHAR(30),
    publisher         VARCHAR(200),
    version           VARCHAR(50),
    description       TEXT,
    source_url        VARCHAR(2000),
    checksum          VARCHAR(128),
    signature_info    TEXT,
    compatibility     TEXT,
    dependencies      TEXT,
    provenance        TEXT,
    registry_metadata TEXT,
    catalog_status    VARCHAR(20),
    registered_at     TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);

-- ---------------------------------------------------------------------------
-- pack_install_record: auditable install/rejection decision records
-- ---------------------------------------------------------------------------
CREATE TABLE pack_install_record (
    id                  UUID         PRIMARY KEY,
    project_id          UUID         NOT NULL REFERENCES project(id),
    pack_id             VARCHAR(200) NOT NULL,
    pack_type           VARCHAR(30)  NOT NULL,
    requested_version   VARCHAR(50),
    resolved_version    VARCHAR(50),
    resolved_source     VARCHAR(2000),
    resolved_checksum   VARCHAR(128),
    signature_verified  BOOLEAN,
    trust_policy_id     VARCHAR(200),
    trust_outcome       VARCHAR(20)  NOT NULL,
    trust_reason        TEXT,
    install_outcome     VARCHAR(20)  NOT NULL,
    error_detail        TEXT,
    installed_entity_id UUID,
    performed_at        TIMESTAMPTZ  NOT NULL,
    performed_by        VARCHAR(255),
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_pack_install_record_project ON pack_install_record (project_id);
CREATE INDEX idx_pack_install_record_pack ON pack_install_record (project_id, pack_id);
CREATE INDEX idx_pack_install_record_outcome ON pack_install_record (project_id, install_outcome);

-- Envers audit table
CREATE TABLE pack_install_record_audit (
    id                  UUID         NOT NULL,
    rev                 INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype             SMALLINT     NOT NULL,
    pack_id             VARCHAR(200),
    pack_type           VARCHAR(30),
    requested_version   VARCHAR(50),
    resolved_version    VARCHAR(50),
    resolved_source     VARCHAR(2000),
    resolved_checksum   VARCHAR(128),
    signature_verified  BOOLEAN,
    trust_policy_id     VARCHAR(200),
    trust_outcome       VARCHAR(20),
    trust_reason        TEXT,
    install_outcome     VARCHAR(20),
    error_detail        TEXT,
    installed_entity_id UUID,
    performed_at        TIMESTAMPTZ,
    performed_by        VARCHAR(255),
    PRIMARY KEY (id, rev)
);

-- ---------------------------------------------------------------------------
-- trust_policy: configurable trust evaluation rules
-- ---------------------------------------------------------------------------
CREATE TABLE trust_policy (
    id               UUID         PRIMARY KEY,
    project_id       UUID         NOT NULL REFERENCES project(id),
    name             VARCHAR(200) NOT NULL,
    description      TEXT,
    default_outcome  VARCHAR(20)  NOT NULL,
    rules            TEXT,
    priority         INTEGER      NOT NULL,
    enabled          BOOLEAN      NOT NULL DEFAULT true,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_trust_policy_project_name UNIQUE (project_id, name)
);

CREATE INDEX idx_trust_policy_project ON trust_policy (project_id);
CREATE INDEX idx_trust_policy_priority ON trust_policy (project_id, priority);

-- Envers audit table
CREATE TABLE trust_policy_audit (
    id               UUID         NOT NULL,
    rev              INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype          SMALLINT     NOT NULL,
    name             VARCHAR(200),
    description      TEXT,
    default_outcome  VARCHAR(20),
    rules            TEXT,
    priority         INTEGER,
    enabled          BOOLEAN,
    PRIMARY KEY (id, rev)
);
