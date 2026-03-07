---
title: "Implement domain event bus (in-process + async)"
labels: [backend, architecture, events]
phase: 5
priority: P0
---

## Description

Build the internal event bus that decouples domain services. Support both synchronous (in-process) and asynchronous (Redis-backed queue) event handling.

## References

- Architecture: Section 3.5 (Event Bus — events table, sync/async)
- Architecture: Section 7 (ARQ or Celery for background jobs)

## Acceptance Criteria

- [ ] `backend/src/ground_control/events/`:
  - `bus.py` — `EventBus` class: `publish(event)`, `subscribe(event_type, handler)`
  - `types.py` — domain event base class with `event_type`, `timestamp`, `tenant_id`, `actor_id`, `data`
  - `handlers.py` — handler registration and dispatch
- [ ] Event types per architecture spec:
  - `risk.created`, `risk.score_changed`
  - `control.updated`
  - `assessment.completed`
  - `test_procedure.result_submitted`
  - `artifact.uploaded`
  - `finding.opened`, `finding.closed`
  - `agent.result_submitted`
  - `plugin.installed`
- [ ] Sync handlers: execute in-process (for simple reactions like cache invalidation)
- [ ] Async handlers: enqueue to Redis/Valkey streams for background processing
- [ ] Event replay capability (for rebuilding state or debugging)
- [ ] Event publishing is transactional (publish only on commit, not on rollback)
- [ ] Unit tests for publish/subscribe, sync/async dispatch
