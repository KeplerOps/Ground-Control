-- GC-M018: knowledge / completeness dimension on the operational asset and
-- the asset relation. CONFIRMED is the conservative default for legacy
-- rows; PROVISIONAL and UNKNOWN are explicit opt-ins. The new column is
-- NOT NULL so "unknown" is never a NULL-vs-something ambiguity — UNKNOWN
-- is a first-class state, distinct from a missing assertion. The preflight
-- note records the rule that AssetType.OTHER / subtype = null / metadata
-- = null are NOT interchangeable with this knowledge state.
ALTER TABLE operational_asset
    ADD COLUMN knowledge_state VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED';

ALTER TABLE asset_relation
    ADD COLUMN knowledge_state VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED';

-- Small-cardinality filter indexes — risk, threat, control, audit, and
-- reporting workflows will filter on knowledge_state to separate confirmed
-- model facts from provisional or unknown coverage. Mirrors the GC-M012
-- filter-index pattern from V069.
CREATE INDEX idx_asset_knowledge_state          ON operational_asset(knowledge_state);
CREATE INDEX idx_asset_relation_knowledge_state ON asset_relation(knowledge_state);
