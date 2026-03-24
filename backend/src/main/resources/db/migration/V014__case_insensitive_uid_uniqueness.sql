-- Enforce case-insensitive UID uniqueness per project.
-- The old constraint treated 'OBS-001' and 'obs-001' as distinct,
-- allowing duplicate requirements to be created.

-- Drop the case-sensitive composite unique constraint FIRST so the
-- UPPER() normalisation cannot violate it when case-only duplicates exist.
ALTER TABLE requirement DROP CONSTRAINT IF EXISTS uq_requirement_project_uid;

-- Drop the old case-sensitive index
DROP INDEX IF EXISTS idx_requirement_project_uid;

-- Deduplicate: when multiple rows share the same (project_id, UPPER(uid)),
-- keep the most-recently-updated row and rename the others with a suffix
-- so no data is lost.
WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY project_id, UPPER(uid)
               ORDER BY updated_at DESC, id
           ) AS rn
    FROM requirement
)
UPDATE requirement r
SET uid = r.uid || '-DUP-' || (ranked.rn - 1)
FROM ranked
WHERE r.id = ranked.id AND ranked.rn > 1;

-- Normalize all existing UIDs to uppercase
UPDATE requirement SET uid = UPPER(uid);

-- Replace with a unique index on LOWER(uid) per project
-- (functional indexes can't be expressed as table constraints in Postgres)
CREATE UNIQUE INDEX uq_requirement_project_uid_ci ON requirement (project_id, LOWER(uid));
CREATE INDEX idx_requirement_project_uid_ci ON requirement (project_id, LOWER(uid));
