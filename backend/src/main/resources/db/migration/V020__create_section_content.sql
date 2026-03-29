CREATE TABLE section_content (
    id              UUID PRIMARY KEY,
    section_id      UUID          NOT NULL REFERENCES section(id) ON DELETE CASCADE,
    content_type    VARCHAR(20)   NOT NULL,
    requirement_id  UUID          REFERENCES requirement(id),
    text_content    TEXT,
    sort_order      INTEGER       NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_content_type CHECK (
        (content_type = 'REQUIREMENT' AND requirement_id IS NOT NULL AND text_content IS NULL)
        OR (content_type = 'TEXT_BLOCK' AND requirement_id IS NULL AND text_content IS NOT NULL)
    )
);

CREATE INDEX idx_section_content_section_id ON section_content (section_id);
CREATE INDEX idx_section_content_requirement_id ON section_content (requirement_id);
