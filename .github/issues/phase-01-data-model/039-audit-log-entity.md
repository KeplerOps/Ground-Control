---
title: "Implement audit log entity with append-only enforcement"
labels: [data-model, backend, security, audit]
phase: 1
priority: P0
---

## Description

Create the immutable audit log entity with hash chaining for tamper detection. Every state change in the system must be recorded. The audit log table must enforce append-only semantics — no UPDATE or DELETE allowed.

## References

- Data Model: Section 2.15 (Audit Log)
- Architecture: Section 6.3 (Audit Log Architecture)
- User Stories: US-7.4 (View Audit Logs)
- PRD: Section 7 (Non-Functional — Immutable audit log)

## Acceptance Criteria

- [ ] SQLAlchemy model: `AuditLogEntry` with all fields (tenant_id, timestamp, actor_id, actor_type, action, resource_type, resource_id, changes, ip_address, user_agent, previous_hash, entry_hash)
- [ ] Alembic migration with:
  - Indexes on (tenant_id, timestamp), (resource_type, resource_id), (actor_id, timestamp)
  - **Trigger or rule to PREVENT UPDATE and DELETE** on audit_log table
  - `CREATE RULE no_update AS ON UPDATE TO audit_log DO INSTEAD NOTHING;`
  - `CREATE RULE no_delete AS ON DELETE TO audit_log DO INSTEAD NOTHING;`
- [ ] Hash chaining implementation:
  - Each entry's `entry_hash` = SHA-256(entry_data + previous_hash)
  - Enables tamper detection by verifying the chain
- [ ] Audit logging service: `AuditLogger`:
  - `log(actor, action, resource_type, resource_id, changes, request_context)`
  - Automatically captures IP, user_agent, tenant_id from request context
  - Computes hash chain
- [ ] Actor types: `user`, `agent`, `system`
- [ ] Action types: `create`, `update`, `delete`, `login`, `logout`, `approve`, `reject`, `archive`
- [ ] `changes` field captures old/new values: `{"field": {"old": "x", "new": "y"}}`
- [ ] Read-only repository for querying logs (no mutation methods)
- [ ] Contracts: `@icontract.ensure(lambda result: result.entry_hash is not None)`
- [ ] Unit tests including tamper detection verification

## Technical Notes

- The audit log table should never have RLS disabled — always tenant-scoped reads
- Consider partitioning by month for performance (large tables)
- Hash chain verification: `verify_chain(start_id, end_id) → bool`
- For SIEM forwarding, add a background job that publishes new entries via webhook/syslog
- The `changes` field should use a diff helper that compares old and new model instances
