---
title: "Implement agent assignment retrieval and result submission"
labels: [backend, agents, api]
phase: 7
priority: P0
---

## Description

Build the APIs for agents to retrieve their assignments and submit structured test results.

## References

- API Spec: Section 4.9 (Agents — assignments, history)
- User Stories: US-8.2 (Agent Retrieves Assignments), US-8.3 (Agent Submits Results)
- Use Cases: UC-06 (Agent-Performed Testing)

## Acceptance Criteria

- [ ] `GET /api/v1/agents/{id}/assignments` — pending test procedures assigned to agent
  - Includes full context: control description, test steps, prior results, linked evidence
  - Filterable by campaign, priority, due date
- [ ] `POST /api/v1/test-procedures/{id}/results` — submit agent results:
  - Per-step results (actual_result, conclusion, evidence_ids)
  - Overall conclusion and confidence score
  - Provenance metadata (model, version, input_hash)
- [ ] Results automatically flagged as `agent_produced = true`
- [ ] Submission triggers notification to assigned human reviewer
- [ ] Invalid/incomplete payloads return descriptive 422 errors
- [ ] `GET /api/v1/agents/{id}/history` — agent action history
- [ ] Integration tests
