CREATE TABLE requirement_import (
    id          UUID PRIMARY KEY,
    source_type VARCHAR(20) NOT NULL,
    source_file VARCHAR(500) DEFAULT '',
    imported_at TIMESTAMP WITH TIME ZONE NOT NULL,
    stats       JSONB DEFAULT '{}'::jsonb,
    errors      JSONB DEFAULT '[]'::jsonb
);
