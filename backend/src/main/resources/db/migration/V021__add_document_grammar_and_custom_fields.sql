-- Grammar stored as JSONB on document
ALTER TABLE document ADD COLUMN grammar JSONB;

-- Custom field values stored as JSONB on requirement
ALTER TABLE requirement ADD COLUMN custom_fields JSONB;

-- Audit table must match (Requirement is @Audited via Hibernate Envers)
ALTER TABLE requirement_audit ADD COLUMN custom_fields JSONB;
