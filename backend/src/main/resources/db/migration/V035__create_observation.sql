CREATE TABLE observation (
    id                UUID PRIMARY KEY,
    asset_id          UUID          NOT NULL REFERENCES operational_asset(id) ON DELETE CASCADE,
    category          VARCHAR(30)   NOT NULL,
    observation_key   VARCHAR(200)  NOT NULL,
    observation_value TEXT          NOT NULL,
    source            VARCHAR(200)  NOT NULL,
    observed_at       TIMESTAMPTZ   NOT NULL,
    expires_at        TIMESTAMPTZ,
    confidence        VARCHAR(50),
    evidence_ref      VARCHAR(2000),
    created_at        TIMESTAMPTZ   NOT NULL,
    updated_at        TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uq_observation_asset_cat_key_time UNIQUE (asset_id, category, observation_key, observed_at)
);

CREATE INDEX idx_observation_asset    ON observation (asset_id);
CREATE INDEX idx_observation_category ON observation (category);
CREATE INDEX idx_observation_key      ON observation (asset_id, observation_key);
CREATE INDEX idx_observation_observed ON observation (observed_at);
