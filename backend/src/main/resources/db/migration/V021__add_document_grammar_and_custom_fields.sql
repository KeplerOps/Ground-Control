-- Grammar stored as TEXT (JSON string) on document
ALTER TABLE document ADD COLUMN grammar TEXT;

-- Custom field values stored as TEXT (JSON string) on requirement
ALTER TABLE requirement ADD COLUMN custom_fields TEXT;

-- Audit table must match (Requirement is @Audited via Hibernate Envers)
ALTER TABLE requirement_audit ADD COLUMN custom_fields TEXT;
