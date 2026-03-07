# ADR-007: Agent-First Design (AI Agents as First-Class Actors)

## Status

Accepted

## Date

2026-03-07

## Context

AI agents are increasingly performing GRC tasks: evidence collection, control testing, risk scoring, and compliance mapping. Most GRC platforms treat agents as an afterthought, bolting on API access without considering agent-specific needs like provenance tracking, confidence scoring, and human review workflows.

## Decision

Design Ground Control with AI agents as first-class actors alongside human users.

- Agents authenticate via OAuth2 client credentials or API keys
- Agent actions are tracked with full provenance (model, version, confidence score, input hash)
- All agent-generated artifacts are flagged with `actor_type: "agent"` in the audit log
- Agent results route through human review workflows by default (configurable)
- Agent SDKs (Python, TypeScript) provide idiomatic access to the API
- Agents can be assigned to assessments and test procedures like human users

## Consequences

### Positive

- Organizations can automate repetitive GRC tasks (evidence collection, control testing)
- Full provenance chain enables trust and auditability of AI-generated results
- Human-in-the-loop by default prevents unchecked AI action
- SDK simplifies agent development — lower barrier than raw API calls

### Negative

- Agent provenance tracking adds storage and processing overhead
- Human review workflows can become bottlenecks if not tuned
- Agent SDK is an additional API surface to maintain

### Risks

- Regulatory acceptance of AI-generated GRC artifacts varies by jurisdiction
- Agent quality varies — need confidence thresholds and fallback to human review
