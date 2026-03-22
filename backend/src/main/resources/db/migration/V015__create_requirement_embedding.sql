CREATE TABLE requirement_embedding (
    id              UUID PRIMARY KEY,
    requirement_id  UUID NOT NULL REFERENCES requirement(id) ON DELETE CASCADE,
    content_hash    VARCHAR(64) NOT NULL,
    embedding       BYTEA NOT NULL,
    dimensions      INTEGER NOT NULL,
    model_id        VARCHAR(100) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_embedding_requirement UNIQUE (requirement_id)
);

CREATE INDEX idx_embedding_requirement_id ON requirement_embedding(requirement_id);
CREATE INDEX idx_embedding_model_id ON requirement_embedding(model_id);
