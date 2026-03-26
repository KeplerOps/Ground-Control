-- Grammar stored as JSONB on document
ALTER TABLE document ADD COLUMN grammar JSONB;

-- Custom field values stored as JSONB on requirement
ALTER TABLE requirement ADD COLUMN custom_fields JSONB;
