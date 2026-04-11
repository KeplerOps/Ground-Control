-- =============================================================================
-- V054: Add typed control-pack artifact payloads to pack registry entries
-- =============================================================================

ALTER TABLE pack_registry_entry
    ADD COLUMN control_pack_entries TEXT;

ALTER TABLE pack_registry_entry_audit
    ADD COLUMN control_pack_entries TEXT;
