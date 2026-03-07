---
title: "Implement evidence lineage and chain of custody"
labels: [backend, evidence, domain-logic, audit]
phase: 4
priority: P1
---

## Description

Build the evidence lineage system that tracks the full chain of custody for every artifact.

## References

- User Stories: US-4.4 (Evidence Lineage and Chain of Custody)
- PRD: Section 4.4 (Evidence Lineage — full chain)

## Acceptance Criteria

- [ ] Lineage timeline: uploaded → linked → reviewed → approved (with actors and timestamps)
- [ ] Hash verification: confirm artifact integrity at any point
- [ ] Chain of custody report generation for a set of artifacts
- [ ] Events aggregated from audit log entries related to the artifact
- [ ] API endpoint: `GET /artifacts/{id}/lineage` returns ordered timeline
- [ ] Unit tests
