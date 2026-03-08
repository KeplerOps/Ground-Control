# ADR-005: Event-Driven Architecture with Domain Events

## Status

Accepted

## Date

2026-03-07

## Context

Ground Control has many cross-cutting reactions to state changes: when a risk is created, notifications must be sent, workflows initiated, search indexes updated, and audit logs written. Tight coupling between these concerns would make the codebase fragile and hard to extend.

## Decision

Implement an internal domain event bus.

- Domain services publish events when state changes occur (e.g., `risk.created`, `finding.opened`)
- Handlers subscribe to events and react asynchronously
- Two execution modes:
  - Synchronous in-process handlers for simple, fast reactions
  - Async queue (Redis/Valkey streams or PostgreSQL LISTEN/NOTIFY) for background jobs
- Events are typed Python dataclasses with a defined schema
- Plugins can subscribe to domain events via the Plugin SDK

## Consequences

### Positive

- Services are decoupled — adding new reactions doesn't modify the publishing service
- Plugins can hook into domain events without core changes
- Background processing keeps API responses fast
- Event log provides a natural audit/debugging trail

### Negative

- Eventual consistency for async handlers — UI may show stale data briefly
- Event ordering can be tricky with concurrent operations
- Debugging event chains is harder than following synchronous call stacks

### Risks

- Event schema evolution must be backward-compatible to avoid breaking handlers
- Lost events (process crash before handler completes) require idempotent handlers and retry logic
