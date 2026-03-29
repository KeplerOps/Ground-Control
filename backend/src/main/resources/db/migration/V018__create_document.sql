CREATE TABLE document (
    id            UUID PRIMARY KEY,
    project_id    UUID          NOT NULL REFERENCES project(id),
    title         VARCHAR(200)  NOT NULL,
    version       VARCHAR(50)   NOT NULL,
    description   TEXT,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by    VARCHAR(100),
    CONSTRAINT uq_document_project_title UNIQUE (project_id, title)
);

CREATE INDEX idx_document_project_id ON document (project_id);
