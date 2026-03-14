CREATE TABLE requirement_relation (
    id            UUID PRIMARY KEY,
    source_id     UUID        NOT NULL REFERENCES requirement(id) ON DELETE CASCADE,
    target_id     UUID        NOT NULL REFERENCES requirement(id) ON DELETE CASCADE,
    relation_type VARCHAR(20) NOT NULL,
    description   TEXT        DEFAULT '',
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (source_id, target_id, relation_type)
);

CREATE INDEX idx_relation_source ON requirement_relation(source_id);
CREATE INDEX idx_relation_target ON requirement_relation(target_id);
