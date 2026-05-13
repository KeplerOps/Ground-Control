-- Drop the ON DELETE CASCADE FK on every audited link table so a parent delete
-- can no longer remove child rows at the DB level (which bypasses Hibernate
-- Envers and leaves *_link_audit incomplete). Service-layer delete methods
-- now delete child links via the repository before deleting the parent, so
-- the DB-level cascade was only a fallback and is no longer needed. ADR-038
-- captures the rationale; the same pattern applies to all five link tables
-- (asset_link, control_link, risk_scenario_link, threat_model_link,
-- finding_link). finding_link was authored without cascade in V062.
--
-- PostgreSQL auto-generates FK constraint names as <table>_<column>_fkey for
-- inline REFERENCES clauses; the four migrations below used that form, so the
-- default name is the one we drop. Each ADD CONSTRAINT installs a deliberately
-- named replacement so future migrations and operators can refer to it
-- explicitly.

ALTER TABLE asset_link
    DROP CONSTRAINT IF EXISTS asset_link_asset_id_fkey;
ALTER TABLE asset_link
    ADD CONSTRAINT fk_asset_link_asset FOREIGN KEY (asset_id) REFERENCES operational_asset(id);

ALTER TABLE risk_scenario_link
    DROP CONSTRAINT IF EXISTS risk_scenario_link_risk_scenario_id_fkey;
ALTER TABLE risk_scenario_link
    ADD CONSTRAINT fk_risk_scenario_link_scenario
        FOREIGN KEY (risk_scenario_id) REFERENCES risk_scenario(id);

ALTER TABLE control_link
    DROP CONSTRAINT IF EXISTS control_link_control_id_fkey;
ALTER TABLE control_link
    ADD CONSTRAINT fk_control_link_control FOREIGN KEY (control_id) REFERENCES control(id);

ALTER TABLE threat_model_link
    DROP CONSTRAINT IF EXISTS threat_model_link_threat_model_id_fkey;
ALTER TABLE threat_model_link
    ADD CONSTRAINT fk_threat_model_link_threat_model
        FOREIGN KEY (threat_model_id) REFERENCES threat_model(id);
