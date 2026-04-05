CREATE TABLE control_audit (
    id                    UUID         NOT NULL,
    rev                   INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype               SMALLINT     NOT NULL,
    uid                   VARCHAR(50),
    title                 VARCHAR(200),
    description           TEXT,
    objective             TEXT,
    control_function      VARCHAR(20),
    status                VARCHAR(20),
    owner                 VARCHAR(200),
    implementation_scope  TEXT,
    methodology_factors   TEXT,
    effectiveness         TEXT,
    category              VARCHAR(100),
    source                VARCHAR(200),
    PRIMARY KEY (id, rev)
);

CREATE TABLE control_link_audit (
    id                UUID         NOT NULL,
    rev               INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype           SMALLINT     NOT NULL,
    control_id        UUID,
    target_type       VARCHAR(30),
    target_entity_id  UUID,
    target_identifier VARCHAR(500),
    link_type         VARCHAR(20),
    target_url        VARCHAR(2000),
    target_title      VARCHAR(255),
    PRIMARY KEY (id, rev)
);
