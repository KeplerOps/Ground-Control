-- GC-L006: EVIDENCE was promoted from an external targetIdentifier-backed link to
-- an internal targetEntityId-backed link routing to GraphEntityType.EVIDENCE_ARTIFACT
-- in GraphTargetResolverService and the five non-audit projection contributors.
-- Legacy rows that pre-date the promotion still carry target_type='EVIDENCE' with
-- target_entity_id IS NULL and a free-text target_identifier. The post-GC-L006
-- validator rejects any rewrite of those rows, and the new contributors still
-- short-circuit them on null target_entity_id, so they would silently disappear
-- from the graph while remaining in the link tables.
--
-- This migration backfills those legacy rows by relabelling them as
-- target_type='EXTERNAL' and prefixing the preserved target_identifier with
-- 'legacy-evidence:' to guarantee no collision with an existing EXTERNAL row
-- under the (parent_id, target_type, target_identifier, link_type) unique
-- constraint. The original identifier remains accessible to any auditor or
-- replay tool that needs it; the row continues to project as no graph edge
-- (matching its prior behaviour); future writes are forced to follow the new
-- EVIDENCE-as-internal contract by the validator.

UPDATE asset_link
SET target_type = 'EXTERNAL',
    target_identifier = 'legacy-evidence:' || target_identifier,
    updated_at = NOW()
WHERE target_type = 'EVIDENCE'
  AND target_entity_id IS NULL
  AND target_identifier IS NOT NULL;

UPDATE control_link
SET target_type = 'EXTERNAL',
    target_identifier = 'legacy-evidence:' || target_identifier,
    updated_at = NOW()
WHERE target_type = 'EVIDENCE'
  AND target_entity_id IS NULL
  AND target_identifier IS NOT NULL;

UPDATE risk_scenario_link
SET target_type = 'EXTERNAL',
    target_identifier = 'legacy-evidence:' || target_identifier,
    updated_at = NOW()
WHERE target_type = 'EVIDENCE'
  AND target_entity_id IS NULL
  AND target_identifier IS NOT NULL;

UPDATE threat_model_link
SET target_type = 'EXTERNAL',
    target_identifier = 'legacy-evidence:' || target_identifier,
    updated_at = NOW()
WHERE target_type = 'EVIDENCE'
  AND target_entity_id IS NULL
  AND target_identifier IS NOT NULL;

UPDATE finding_link
SET target_type = 'EXTERNAL',
    target_identifier = 'legacy-evidence:' || target_identifier,
    updated_at = NOW()
WHERE target_type = 'EVIDENCE'
  AND target_entity_id IS NULL
  AND target_identifier IS NOT NULL;
