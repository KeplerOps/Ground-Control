---
title: "Implement ABAC policy engine"
labels: [backend, auth, security]
phase: 3
priority: P1
---

## Description

Build the attribute-based access control engine that evaluates fine-grained access policies based on subject, resource, action, and context attributes. Integrates with the policy-as-code framework (#025).

## References

- Architecture: Section 4 (Authorization — ABAC)
- Issue #025 (Policy as Code Framework)

## Acceptance Criteria

- [ ] ABAC policy evaluator implementing policies from #025:
  - Tenant isolation (always enforced)
  - Business unit scoping
  - Assessment scoping (auditor sees only assigned work)
  - Data classification restrictions
- [ ] Policy inputs: subject attributes (roles, BU, tenant), resource attributes (owner, classification, tenant), action, environment (time, IP)
- [ ] Composable with RBAC: RBAC provides base permissions, ABAC further restricts
- [ ] Decision: `allow` or `deny(reason)`
- [ ] Policy evaluation is fast (< 1ms per decision — critical path)
- [ ] Hot-reloadable policies (no restart required)
- [ ] Unit tests with comprehensive allow/deny scenarios
