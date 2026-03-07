---
title: "Implement outbound webhook system"
labels: [backend, integrations, events]
phase: 5
priority: P1
---

## Description

Build the outbound webhook system for notifying external systems of domain events.

## References

- API Spec: Section 6 (Webhook Events — subscription, payload, HMAC signing)

## Acceptance Criteria

- [ ] Webhook subscription management:
  - `POST /api/v1/webhooks` — create subscription (url, events, secret)
  - `GET /api/v1/webhooks` — list
  - `DELETE /api/v1/webhooks/{id}` — remove
- [ ] Webhook delivery:
  - Payload matches spec format (id, type, timestamp, tenant_id, data)
  - HMAC-SHA256 signing with shared secret (`X-GC-Signature` header)
  - Retry with exponential backoff on failure (3 attempts)
  - Timeout: 10 seconds per delivery
- [ ] Delivery logging: status, response code, retry count
- [ ] Webhook testing: `POST /api/v1/webhooks/{id}/test` sends test event
- [ ] Unit tests
