CREATE TABLE section (
    id            UUID PRIMARY KEY,
    document_id   UUID          NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    parent_id     UUID          REFERENCES section(id) ON DELETE CASCADE,
    title         VARCHAR(200)  NOT NULL,
    description   TEXT,
    sort_order    INTEGER       NOT NULL DEFAULT 0,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_section_parent_title UNIQUE (document_id, parent_id, title)
);

CREATE INDEX idx_section_document_id ON section (document_id);
CREATE INDEX idx_section_parent_id ON section (parent_id);
