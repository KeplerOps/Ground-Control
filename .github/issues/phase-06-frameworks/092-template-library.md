---
title: "Implement template library (assessments, test procedures, workpapers)"
labels: [backend, compliance, content]
phase: 6
priority: P1
---

## Description

Build the template library system for reusable assessment structures, test procedures, and workpaper formats.

## References

- PRD: Section 6.3 (Template Library — assessment, test procedure, workpaper, finding, report templates)
- User Stories: US-3.1 (Campaign generates workpapers from templates)

## Acceptance Criteria

- [ ] Template entity model: name, type, version, content (JSONB), tags, tenant_id (nullable for global)
- [ ] Template types: assessment, test_procedure, workpaper, finding, report
- [ ] Template instantiation: creates concrete entity from template
- [ ] Version management for templates
- [ ] Shareable between tenants (if tenant_id = NULL → global)
- [ ] Import/export templates as YAML/JSON
- [ ] API endpoints: list, get, create, update, instantiate
- [ ] Seed templates for SOX ITGC and SOC 2 assessments
- [ ] Unit tests
