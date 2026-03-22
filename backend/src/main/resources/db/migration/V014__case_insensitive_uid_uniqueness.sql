-- Enforce case-insensitive UID uniqueness per project.
-- The old constraint treated 'OBS-001' and 'obs-001' as distinct,
-- allowing duplicate requirements to be created.

-- Normalize all existing UIDs to uppercase
UPDATE requirement SET uid = UPPER(uid);

-- Drop the case-sensitive composite unique constraint
ALTER TABLE requirement DROP CONSTRAINT IF EXISTS uq_requirement_project_uid;

-- Replace with a unique index on LOWER(uid) per project
-- (functional indexes can't be expressed as table constraints in Postgres)
CREATE UNIQUE INDEX uq_requirement_project_uid_ci ON requirement (project_id, LOWER(uid));

-- Drop the old case-sensitive index and recreate case-insensitive
DROP INDEX IF EXISTS idx_requirement_project_uid;
CREATE INDEX idx_requirement_project_uid_ci ON requirement (project_id, LOWER(uid));
