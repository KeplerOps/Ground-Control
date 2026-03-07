---
title: "Implement policy as code framework for authorization"
labels: [foundation, backend, security, cross-cutting]
phase: 0
priority: P1
---

## Description

Establish a policy-as-code framework where authorization policies are defined as declarative, version-controlled, testable artifacts rather than hard-coded `if/else` logic. Policies govern who can do what on which resources under what conditions.

**Approach:** Use Open Policy Agent (OPA) with Rego policies, or a lightweight Python-native policy engine with YAML-defined rules. The policy engine should support both RBAC and ABAC patterns described in the architecture.

## References

- Architecture: Section 4 (Authentication & Authorization — RBAC + ABAC)
- Data Model: Section 2.3 (Role & Permission)
- User Stories: US-7.2 (Manage Users and Roles)
- Use Cases: UC-07 (Configure SSO and Provision Users)

## Acceptance Criteria

- [ ] `architecture/policies/` directory with policy definitions:
  - `rbac.rego` (or `rbac.yaml`) — role-based access rules
  - `abac.rego` (or `abac.yaml`) — attribute-based access rules
  - `tenant_isolation.rego` — tenant boundary enforcement
  - `agent_permissions.rego` — agent-specific restrictions
- [ ] Policy engine module: `backend/src/ground_control/domain/auth/policy_engine.py`:
  - `evaluate(subject, action, resource, context) → Decision`
  - Decisions: `allow`, `deny` with reason
  - Context includes: tenant_id, business_unit, role, resource attributes
- [ ] Default policies implementing the authorization model:
  ```
  # Permission format: resource:action:scope
  risks:read:*                    → Risk Manager, Auditor, CISO
  risks:write:bu=engineering      → Risk Manager (scoped to BU)
  assessments:approve:campaign=*  → Audit Manager
  agents:execute:scope=testing    → Agent role
  audit_logs:read:*               → Admin only
  ```
- [ ] Policy tests:
  - Unit tests for each policy (test allow AND deny cases)
  - `make policy-test` target
  - Policies tested in CI
- [ ] Policy change audit trail (policy files are version-controlled)
- [ ] ADR: `architecture/adrs/011-policy-as-code.md`

## Technical Notes

- **Option A: OPA/Rego** — industry standard, powerful, but adds a service dependency
  - Can embed via `opa-python-client` or `regorus` (Rust OPA engine with Python bindings)
  - Rego policies are highly testable (`opa test`)
- **Option B: Python-native** — simpler, fewer dependencies
  - Define policies in YAML, evaluate with a custom engine
  - Less expressive but sufficient for RBAC+ABAC patterns
  - Easier to integrate with SQLAlchemy queries (generate WHERE clauses)
- Recommended: Start with Python-native, migrate to OPA if complexity grows
- Policies must be hot-reloadable (no restart to change policies)
- The policy engine is used by authorization middleware (#058) and decorators
