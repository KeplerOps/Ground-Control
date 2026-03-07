---
title: "Build Plugin SDK (Python)"
labels: [backend, plugins, sdk]
phase: 10
priority: P0
---

## Description

Build the Python Plugin SDK that plugin authors use to build Ground Control extensions.

## References

- API Spec: Section 8.3 (Plugin SDK — Python example)

## Acceptance Criteria

- [ ] Plugin SDK package: `ground-control-plugin-sdk`
- [ ] Base classes: `Plugin`, `@event_handler`, `@api_route`
- [ ] `GroundControlClient` — scoped API client for plugins:
  - `controls.list()`, `artifacts.upload()`, `artifacts.link()`
  - Respects declared permission scopes
- [ ] Plugin context: tenant_id, plugin config, logger
- [ ] Configuration schema validation (JSON Schema)
- [ ] Plugin development guide and example plugins
- [ ] Unit tests
