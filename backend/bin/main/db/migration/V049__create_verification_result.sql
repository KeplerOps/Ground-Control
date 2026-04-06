CREATE TABLE verification_result (
    id               UUID         PRIMARY KEY,
    project_id       UUID         NOT NULL REFERENCES project(id),
    target_id        UUID         REFERENCES traceability_link(id),
    requirement_id   UUID         REFERENCES requirement(id),
    prover           VARCHAR(100) NOT NULL,
    property         TEXT,
    result           VARCHAR(20)  NOT NULL,
    assurance_level  VARCHAR(5)   NOT NULL,
    evidence         TEXT,
    verified_at      TIMESTAMPTZ  NOT NULL,
    expires_at       TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_verification_result_project ON verification_result (project_id);
CREATE INDEX idx_verification_result_requirement ON verification_result (requirement_id);
CREATE INDEX idx_verification_result_target ON verification_result (target_id);
CREATE INDEX idx_verification_result_result ON verification_result (result);
CREATE INDEX idx_verification_result_prover ON verification_result (project_id, prover);
