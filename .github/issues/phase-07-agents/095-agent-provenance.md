---
title: "Implement agent provenance tracking"
labels: [backend, agents, audit]
phase: 7
priority: P0
---

## Description

Build the provenance tracking system that records full context for every agent action, enabling reproducibility and trust verification.

## References

- PRD: Section 5.3 (Agent Provenance — identity, model, input hash, confidence, review status)

## Acceptance Criteria

- [ ] Provenance record for every agent action:
  - Agent identity (registered agent_id, human owner)
  - Model/version used (e.g., "claude-opus-4-6")
  - Input context hash (SHA-256 of inputs for reproducibility)
  - Confidence score (0.0 - 1.0)
  - Human review status: pending, approved, rejected
  - Timestamp and request context
- [ ] Provenance stored as JSONB on test procedures and other agent-touched entities
- [ ] Provenance query API: filter by agent, model, confidence, review status
- [ ] Dashboard data: agent performance metrics (approval rate, confidence distribution)
- [ ] Unit tests
