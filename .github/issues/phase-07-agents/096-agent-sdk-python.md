---
title: "Build Agent SDK — Python package"
labels: [sdk, agents, python]
phase: 7
priority: P1
---

## Description

Build the Python Agent SDK as a standalone package (`ground-control-agent-sdk`) for building AI agents that interact with Ground Control.

## References

- API Spec: Section 9.1 (Python Agent SDK)
- PRD: Section 10 (v0.4 — Agent SDK Python)

## Acceptance Criteria

- [ ] Package: `sdks/python/` with `pyproject.toml`
- [ ] Published as: `ground-control-agent-sdk` on PyPI
- [ ] `AgentClient` class with methods:
  - `authenticate()` — OAuth2 client credentials
  - `get_assignments(status, campaign_type)` — retrieve pending work
  - `get_test_procedure(id)` — get full procedure context
  - `submit_results(procedure_id, steps, conclusion, confidence, notes)` — submit results
  - `upload_artifact(file_data, filename, tags)` — upload evidence
  - `link_artifact(artifact_id, entity_type, entity_id)` — link evidence
- [ ] Async-native (asyncio/httpx)
- [ ] Automatic token refresh
- [ ] Structured error handling (typed exceptions)
- [ ] Type hints on all public methods (PEP 561 compliant)
- [ ] Examples directory with sample agent scripts
- [ ] Unit tests
- [ ] Documentation (docstrings + README)
