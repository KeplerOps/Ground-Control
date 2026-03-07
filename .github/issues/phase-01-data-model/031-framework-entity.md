---
title: "Implement framework and requirements entities"
labels: [data-model, backend, compliance]
phase: 1
priority: P0
---

## Description

Create the framework and framework requirements entities, plus the control-framework mapping junction table. This enables cross-framework compliance mapping.

## References

- Data Model: Section 2.6 (Framework & Requirements, control_framework_mappings)
- PRD: Section 3 (Frameworks & Standards), Section 4.2 (Control-to-Framework Mapping)
- User Stories: US-2.2 (Map Controls Across Frameworks)
- Use Cases: UC-05 (Cross-Framework Control Mapping)

## Acceptance Criteria

- [ ] SQLAlchemy models: `Framework`, `FrameworkRequirement`, `ControlFrameworkMapping`
- [ ] Alembic migration with all tables, indexes, and unique constraints
- [ ] `FrameworkRequirement` supports hierarchy (`parent_id` self-referencing FK)
- [ ] `ControlFrameworkMapping` with mapping_type, notes, agent suggestion fields
- [ ] Repositories: `FrameworkRepository`, `FrameworkRequirementRepository`
- [ ] Pydantic schemas for all three entities (Create, Read, Update)
- [ ] Framework `tenant_id` is nullable (NULL = system/global framework)
- [ ] Hierarchical requirement query (tree structure)
- [ ] Unit tests including hierarchy traversal and many-to-many mapping
