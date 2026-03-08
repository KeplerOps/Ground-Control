# ADR-012: Formal Methods Development Process

## Status

Accepted

## Date

2026-03-08

## Context

Ground Control requires a **process** governing when and how to apply formal methods — not just a list of tools. ADR-003 (now superseded by ADR-013) established design-by-contract as a principle; this ADR codifies the methodology.

Industry precedent from Amazon s2n (specification-driven TLS), SPARK Ada, Dafny, and Frama-C demonstrates that **Specification-Driven Development (SDD)** — writing formal specifications before implementation — catches design errors earlier and at lower cost than test-after workflows.

This ADR codifies an SDD methodology for Ground Control. The process and assurance levels are language-agnostic; the tooling section reflects the current Java/Spring Boot stack (ADR-013).

## Decision

### SDD Workflow

The core development loop for contracted code is:

1. **Spec** — Write JML contracts (`requires`, `ensures`, `invariant`) that express the behavioral intent.
2. **Test** — Write a failing test that exercises the contract (both happy-path and contract-violation).
3. **Code** — Implement the method body to satisfy the contracts and pass the test.
4. **Verify** — Run OpenJML ESC (static checking) and jqwik (property-based) to search for violations.

This is "TDD for invariants": contracts are the failing specification, implementation satisfies them. Contracts and tests are complementary — contracts define *what must always hold*, tests verify *specific scenarios*.

### Assurance Levels

Not all code warrants the same rigor. An escalation ladder prevents over-engineering:

| Level | Name | Scope | Tools |
|-------|------|-------|-------|
| 0 | Standard | Utils, config, controllers, glue code | `javac` + JUnit 5 |
| 1 | Contracted | Domain models, state machines, business rules | + JML `requires`/`ensures`/`invariant` via OpenJML RAC |
| 2 | Property-Verified | Core invariants, DAG operations, security boundaries | + jqwik + OpenJML ESC |
| 3 | Formally Specified | Tenant isolation, access control, audit (future) | + KeY proofs (Isabelle/HOL secondary) |

Level 0 is the explicit default. Contracts are earned, not mandated.

### Current Code Classification

| Code | Current Level | Target Level | Notes |
|------|--------------|--------------|-------|
| `domain/requirements/model/Requirement.java` | L1 (has `requires`) | L2 | Adding `ensures` + `invariant` + jqwik |
| `domain/requirements/model/RequirementRelation.java` | L1 (has `requires`) | L1 (sufficient) | Self-loop prevention only |
| `domain/requirements/state/Status.java` (transition table) | L1 (structural test) | L2 | Adding jqwik property tests |
| `domain/requirements/exception/` | L0 | L0 | Data carriers only |
| `api/` | L0 | L0 | Thin handler layer |
| `infrastructure/`, `shared/` | L0 | L0 | Configuration and adapters |

### Triage Guide

**ALWAYS contract (L1+):**
- State machine transitions
- Domain model invariants (e.g., "archivedAt only set when status is ARCHIVED")
- Security boundaries (authentication, authorization checks)
- Data integrity rules that cross multiple fields

**SOMETIMES contract:**
- Complex business rules with non-obvious edge cases
- Methods with preconditions that callers might violate
- Code paths where silent corruption is worse than a crash

**NEVER contract:**
- Simple getters/setters with no invariants
- Spring configuration classes
- Test code
- Glue code (controller routing, filter registration)

### Tool Integration

- **OpenJML ESC in CI**: Runs static checking on `src/main/java/.../domain/` after tests pass. Blocking — failures prevent merge.
- **OpenJML RAC**: Runtime assertion checking enabled in test and dev profiles. JML annotations in source are checked at runtime via `-javaagent`.
- **jqwik**: Property-based tests tagged `@Tag("slow")`. Run in CI, skippable locally via Gradle task filtering.
- **KeY** (future): Formal proofs for L3 code. Deferred until L1 and L2 coverage is established.

### Drift Detection

Every contract needs:
1. A **happy-path test** confirming the contracted behavior works.
2. A **contract-violation test** confirming the contract rejects invalid inputs.
3. **OpenJML ESC re-verification** on every PR (automated via CI).
4. **Structural property tests** verifying meta-properties of specification tables (e.g., "every Status enum value has a VALID_TRANSITIONS entry").

If a contract is added without corresponding tests, it is dead specification — no better than a comment.

### Relationship to TDD

SDD does not replace TDD. It extends it:

- **TDD**: "Write a failing test, then make it pass."
- **SDD**: "Write a failing contract, write a test that exercises it, then make both pass."

Contracts capture universal invariants ("this must always hold"). Tests capture specific scenarios ("when I do X, Y happens"). Both are needed.

## Consequences

### Positive

- Bugs surface at contract boundaries with clear error messages, not as downstream corruption
- Spec-first catches design errors before code is written
- Assurance levels prevent over-engineering — Level 0 is the default
- OpenJML ESC finds contract violations statically, without running the code
- JML contracts serve as executable documentation
- Single annotation language (JML) spans L1 through L3 — no tool-boundary translation

### Negative

- CI time increases with OpenJML ESC verification
- Learning curve for developers unfamiliar with JML, DbC, and SDD
- jqwik property tests are slower than example-based JUnit tests

### Risks

- OpenJML + Hibernate proxies: JPA entity proxies may interfere with JML runtime checks. Mitigated by scoping RAC to domain logic methods, not framework-generated code.
- Adoption friction: developers may resist writing contracts first. Mitigated by clear triage guide and assurance levels — most code stays at Level 0.
- Over-contracting: enthusiasm may lead to contracts on trivial code. Mitigated by "contracts are earned, not mandated" principle and code review.
