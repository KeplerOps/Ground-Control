---
title: "Implement AI-powered control mapping suggestions"
labels: [backend, agents, compliance, ai]
phase: 7
priority: P2
---

## Description

Build the endpoint where AI agents analyze control descriptions and suggest framework mappings with confidence scores.

## References

- User Stories: US-8.4 (Agent Suggests Control Mappings)
- Use Cases: UC-05 (extension 2a-2b — AI suggestions)

## Acceptance Criteria

- [ ] `POST /api/v1/controls/{id}/suggest-mappings` triggers agent analysis
- [ ] Response: ranked suggestions with requirement_id and confidence score
- [ ] Suggestions stored as pending (not auto-applied)
- [ ] Analyst can approve/reject each suggestion via API
- [ ] Approved suggestions create actual control-framework mappings
- [ ] Suggestion history retained for agent performance tracking
- [ ] Unit tests
