CREATE TABLE operational_asset (
    id          UUID PRIMARY KEY,
    project_id  UUID                     NOT NULL REFERENCES project(id),
    uid         VARCHAR(50)              NOT NULL,
    name        VARCHAR(200)             NOT NULL,
    description TEXT,
    asset_type  VARCHAR(20)              NOT NULL DEFAULT 'OTHER',
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_asset_project_uid UNIQUE (project_id, uid)
);

CREATE INDEX idx_asset_project_id ON operational_asset(project_id);
CREATE INDEX idx_asset_type ON operational_asset(asset_type);
