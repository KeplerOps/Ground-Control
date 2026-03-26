-- A requirement may belong to at most one section (GC-B011).
-- Partial unique index: only one section_content row per requirement_id
-- where content_type is REQUIREMENT.
CREATE UNIQUE INDEX uq_section_content_requirement
    ON section_content (requirement_id)
    WHERE content_type = 'REQUIREMENT';
