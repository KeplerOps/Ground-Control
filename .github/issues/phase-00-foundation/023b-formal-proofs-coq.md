---
title: "Set up Coq/Rocq formal proof infrastructure and initial proof targets"
labels: [foundation, quality, formal-methods]
phase: 0
priority: P1
---

## Description

Set up the Coq/Rocq proof assistant infrastructure and create initial proof skeletons for the critical invariants that testing alone cannot guarantee. Proofs model the domain logic — they verify properties of algorithms and state machines, not the Python code directly.

## References

- Coding Standards: Section 11 (Formal Methods)
- Architecture: Section 6 (Security Architecture — audit log, RBAC/ABAC, tenant isolation)
- Data Model: Section 2.15 (Audit Log — hash chain), Section 2.3 (Roles & Permissions)

## Proof Targets

### 1. Audit Log Integrity
- Model the hash chain: each entry hashes (content + previous_hash).
- Prove: any modification to a past entry is detectable (the chain breaks).
- Prove: entries can only be appended, never inserted or reordered.

### 2. RBAC/ABAC Policy Evaluation
- Model the permission format: `resource:action:scope`.
- Model role → permission assignment and user → role assignment.
- Prove: a user without a matching permission cannot pass an authorization check.
- Prove: no combination of valid role assignments can escalate beyond declared permissions.

### 3. Entity State Machines
- Model lifecycle states for: Finding (draft → open → remediation_in_progress → validation → closed), Assessment Campaign (planning → active → review → finalized → archived), Test Procedure (not_started → in_progress → completed → review → approved).
- Prove: only valid transitions are possible. No state can be reached that is not in the declared set.
- Prove: terminal states (closed, finalized, approved) cannot transition to non-terminal states without explicit re-open.

### 4. Tenant Isolation
- Model a query as a function from (tenant_id, query_params) → result_set.
- Prove: for any two distinct tenant_ids, the result sets are disjoint (no row appears in both).

## Acceptance Criteria

- [ ] `proofs/` directory created with subdirectories: `audit_log/`, `authorization/`, `state_machines/`, `tenant_isolation/`
- [ ] `proofs/README.md` with: what the proofs cover, how to install Coq, how to verify all proofs (`make` or `coq_makefile`)
- [ ] Coq project file (`_CoqProject`) and Makefile for building all proofs
- [ ] Audit log hash chain: proof of append-only tamper detection compiles
- [ ] State machine: at least one entity lifecycle (Finding) fully proved
- [ ] CI step: `make -C proofs` runs in GitHub Actions and fails if any proof breaks
- [ ] Each proof file documents which domain code it models (file path + function/class name)

## Notes

- Proofs model the logic, they don't extract to Python. The Python implementation must match the model — verified by review and testing.
- Start with the simplest proof (state machine transitions) to validate the toolchain, then tackle audit log and authorization.
- Use Coq stdlib where possible. Avoid heavy dependencies.
- Development uses rocq-mcp (MCP server for Coq/Rocq) for interactive proof development. See #006b for setup.
