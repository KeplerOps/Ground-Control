CREATE TABLE methodology_profile_audit (
    id            UUID         NOT NULL,
    rev           INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype       SMALLINT,
    profile_key   VARCHAR(100),
    name          VARCHAR(200),
    version       VARCHAR(50),
    family        VARCHAR(30),
    description   TEXT,
    input_schema  TEXT,
    output_schema TEXT,
    status        VARCHAR(20),
    created_at    TIMESTAMPTZ,
    updated_at    TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);

CREATE TABLE risk_register_record_audit (
    id                  UUID         NOT NULL,
    rev                 INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype             SMALLINT,
    uid                 VARCHAR(50),
    title               VARCHAR(200),
    owner               VARCHAR(200),
    status              VARCHAR(20),
    review_cadence      VARCHAR(100),
    next_review_at      TIMESTAMPTZ,
    category_tags       TEXT,
    decision_metadata   TEXT,
    asset_scope_summary TEXT,
    created_at          TIMESTAMPTZ,
    updated_at          TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);

CREATE TABLE risk_register_record_scenario_audit (
    rev                    INTEGER  NOT NULL REFERENCES revinfo(rev),
    revtype                SMALLINT,
    risk_register_record_id UUID    NOT NULL,
    risk_scenario_id        UUID    NOT NULL,
    PRIMARY KEY (rev, risk_register_record_id, risk_scenario_id)
);

CREATE TABLE risk_assessment_result_audit (
    id                     UUID         NOT NULL,
    rev                    INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype                SMALLINT,
    risk_scenario_id       UUID,
    methodology_profile_id UUID,
    risk_register_record_id UUID,
    analyst_identity       VARCHAR(200),
    assumptions            TEXT,
    input_factors          TEXT,
    observation_date       TIMESTAMPTZ,
    assessment_at          TIMESTAMPTZ,
    time_horizon           VARCHAR(100),
    confidence             VARCHAR(50),
    uncertainty_metadata   TEXT,
    computed_outputs       TEXT,
    approval_state         VARCHAR(20),
    evidence_refs          TEXT,
    notes                  TEXT,
    created_at             TIMESTAMPTZ,
    updated_at             TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);

CREATE TABLE risk_assessment_result_observation_audit (
    rev                       INTEGER  NOT NULL REFERENCES revinfo(rev),
    revtype                   SMALLINT,
    risk_assessment_result_id UUID     NOT NULL,
    observation_id            UUID     NOT NULL,
    PRIMARY KEY (rev, risk_assessment_result_id, observation_id)
);

CREATE TABLE treatment_plan_audit (
    id                      UUID         NOT NULL,
    rev                     INTEGER      NOT NULL REFERENCES revinfo(rev),
    revtype                 SMALLINT,
    uid                     VARCHAR(50),
    title                   VARCHAR(200),
    risk_register_record_id UUID,
    risk_scenario_id        UUID,
    strategy                VARCHAR(20),
    owner                   VARCHAR(200),
    rationale               TEXT,
    due_date                TIMESTAMPTZ,
    status                  VARCHAR(20),
    action_items            TEXT,
    reassessment_triggers   TEXT,
    created_at              TIMESTAMPTZ,
    updated_at              TIMESTAMPTZ,
    PRIMARY KEY (id, rev)
);

ALTER TABLE asset_link_audit ALTER COLUMN target_type TYPE VARCHAR(40);
ALTER TABLE asset_link_audit ADD COLUMN target_entity_id UUID;

ALTER TABLE risk_scenario_link_audit ALTER COLUMN target_type TYPE VARCHAR(40);
ALTER TABLE risk_scenario_link_audit ADD COLUMN target_entity_id UUID;
