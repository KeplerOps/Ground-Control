-- Create project table
CREATE TABLE project (
    id          UUID PRIMARY KEY,
    identifier  VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Insert default project
INSERT INTO project (id, identifier, name, description, created_at, updated_at)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'ground-control',
    'Ground Control',
    'Default project',
    NOW(),
    NOW()
);

-- Add project_id to requirement, backfill with default, set NOT NULL
ALTER TABLE requirement ADD COLUMN project_id UUID;
UPDATE requirement SET project_id = 'a0000000-0000-0000-0000-000000000001';
ALTER TABLE requirement ALTER COLUMN project_id SET NOT NULL;
ALTER TABLE requirement ADD CONSTRAINT fk_requirement_project FOREIGN KEY (project_id) REFERENCES project(id);

-- Drop old unique constraint on uid, add composite unique
ALTER TABLE requirement DROP CONSTRAINT IF EXISTS requirement_uid_key;
ALTER TABLE requirement ADD CONSTRAINT uq_requirement_project_uid UNIQUE (project_id, uid);

-- Indexes for project-scoped queries
CREATE INDEX idx_requirement_project_id ON requirement (project_id);
CREATE INDEX idx_requirement_project_status ON requirement (project_id, status);
CREATE INDEX idx_requirement_project_uid ON requirement (project_id, uid);

-- Add project_id to requirement_audit (Envers)
ALTER TABLE requirement_audit ADD COLUMN project_id UUID;
UPDATE requirement_audit SET project_id = 'a0000000-0000-0000-0000-000000000001';
