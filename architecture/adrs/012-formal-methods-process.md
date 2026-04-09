# ADR-012: Formal Methods Development Process

## Status

Accepted

## Date

2026-03-08

## Context

Ground Control requires a **process** governing when and how to apply formal methods — not just a list of tools. ADR-003 (now superseded by ADR-013) established design-by-contract as a principle; this ADR codifies the methodology.

Industry precedent from Amazon s2n (specification-driven TLS), SPARK Ada, Dafny, and Frama-C demonstrates that **Specification-Driven Development (SDD)** — writing formal specifications before implementation — catches design errors earlier and at lower cost than test-after workflows.

This ADR codifies an SDD methodology for Ground Control. The process and assurance levels are **language-agnostic methodology** — the level definitions (L0-L3) apply universally regardless of target language. The tooling section reflects the current Java/Spring Boot stack (ADR-013) used for Ground Control's own code. ADR-014 generalizes the tooling to a pluggable verification architecture for the platform's polyglot output.

### Default Level Rationale

#### Pre-alpha (current): L0 default

During pre-alpha, the default assurance level is **L0 (Standard)**. The goal is development velocity — getting the platform's differentiating features (graph-based traceability, verification orchestration) built and working. The real cost of higher assurance levels is not writing contracts or tests but **waiting for tests to run**, which blocks the rapid iteration loop needed when the architecture is still taking shape.

Escalate above L0 only when the code genuinely demands it:
- **L1** for state transitions, security boundaries, and methods with non-obvious preconditions that would cause silent corruption if violated
- **L2** for the state machine and DAG operations (these have already proven their value via jqwik)

JML contracts already present in the codebase are retained — they serve as documentation even without strict test-per-contract enforcement.

#### Post-alpha target: L1 default

Once the platform's core features are operational and the CI pipeline can run verification autonomously, the default will rise to L1. The rationale for L1-as-default remains sound:

1. **Contracts are near-zero cost for an LLM.** Writing `// @ requires x != null;` takes one line.
2. **Contracts catch LLM drift.** A second specification channel — if the implementation doesn't satisfy the contract, the build fails.
3. **"Adoption friction" doesn't exist.** An AI agent follows the process it's given.
4. **The cost of a missed contract is higher than the cost of writing one** — once the machinery is running.

The transition from L0-default to L1-default will be a deliberate decision documented in a follow-up ADR when the platform reaches beta.

## Decision

### SDD Workflow

The core development loop for all L1+ code is:

1. **Classify** — Determine the assurance level before writing any code (see decision table below).
2. **Spec** — Write JML contracts (`requires`, `ensures`, `invariant`) that express the behavioral intent.
3. **Test** — Write a failing test that exercises the contract (both happy-path and contract-violation).
4. **Code** — Implement the method body to satisfy the contracts and pass the test.
5. **Verify** — Run `make policy` and `./gradlew check` (repo guardrails + compilation + OpenJML + tests + Spotless + ArchUnit).

This is "TDD for invariants": contracts are the failing specification, implementation satisfies them. Contracts and tests are complementary — contracts define *what must always hold*, tests verify *specific scenarios*.

### Assurance Levels

| Level | Name | Scope | Tools |
|-------|------|-------|-------|
| 0 | Standard | **Pre-alpha default.** All code unless escalated by the decision table below | `javac` + JUnit 5 |
| 1 | Contracted | State transitions, security boundaries, methods with non-obvious preconditions that cause silent corruption | + JML `requires`/`ensures`/`invariant` |
| 2 | Property-Verified | State machines, transition tables, DAG operations | + jqwik + TLA+ design specs (ADR-014) |
| 3 | Formally Specified | Tenant isolation, access control, audit (future) | + KeY proofs (Isabelle/HOL secondary) |

**Pre-alpha default is L0.** Escalate to L1 for code where a missed precondition causes silent corruption. Escalate to L2 for state machines and graph operations. See "Default Level Rationale" above for the planned transition to L1-default at beta.

### Level Decision Table

Apply these rules in order. Use the first match.

| If you are writing... | Level |
|-----------------------|-------|
| A state machine, transition table, or workflow | **L2** |
| DAG operations (cycle detection, topological sort, reachability) | **L2** |
| Security boundary logic (auth checks, permission guards) | **L1** |
| A domain model method where invalid input causes silent data corruption | **L1** |
| Everything else | **L0** |

When in doubt, use L0 during pre-alpha. The bar rises at beta.

### Current Code Classification

| Code | Level | Notes |
|------|-------|-------|
| `domain/requirements/model/Requirement.java` | L2 | State machine + cross-field invariant (`archivedAt`/`status`) |
| `domain/requirements/model/RequirementRelation.java` | L1 | Self-loop prevention precondition |
| `domain/requirements/state/Status.java` | L2 | Transition table with jqwik property tests |
| `domain/exception/` | L0 | Data carriers only |
| `api/GlobalExceptionHandler` | L0 | Mapping layer, no domain logic |
| `api/ErrorResponse` | L0 | Record (DTO) |
| `shared/logging/RequestLoggingFilter` | L0 | Glue code |
| `infrastructure/` | L0 | Adapter layer |

### Triage Guide (pre-alpha)

**L2 (property tests + design specs):**
- State machine transitions and transition tables
- Graph/DAG operations

**L1 (contracts on critical paths):**
- Security boundaries (authentication, authorization checks)
- Domain model methods where invalid input causes silent data corruption (e.g., status/archivedAt invariant)

**L0 (everything else):**
- Services, repositories, controllers, configuration, DTOs, glue code
- One test per significant behavior is sufficient — no two-tests-per-contract requirement during pre-alpha

### Tool Integration

**OpenJML ESC** (Extended Static Checking) — integrated in Phase 2B:
- Version: OpenJML 21-0.21 (JDK 21 series)
- Gradle tasks: `./gradlew downloadOpenJml` (fetches binary), `./gradlew openjmlEsc` (runs Z3 prover)
- Defined in: `backend/gradle/openjml.gradle.kts`, applied from `build.gradle.kts`
- Scope: `domain/requirements/state/` and `domain/verification/state/` only (pure enums with no framework annotations). Other `state/` packages (`assets`, `controls`, `riskscenarios`, `plugins`, `controlpacks`) contain L0 value enums not covered by ESC.
- Gradle up-to-date checking: skips when source files haven't changed (~1s no-op)
- **Known limitations**: OpenJML's bundled `CharSequence.jml` spec has invariant bugs that cause false positives on classes with `String` constructor parameters. JPA entities fail due to Hibernate's no-arg constructor leaving fields `null`. These classes remain at L1 (contract + test pairs). See `docs/CODING_STANDARDS.md` "OpenJML ESC Scoping" for design guidelines.

**OpenJML RAC** (Runtime Assertion Checking):
- Gradle task: `./gradlew openjmlRac` (compiles domain code with embedded assertions to `build/classes/rac`)
- Scope: full `domain/` package
- Status: task defined, not yet wired into test execution

**jqwik**: Property-based tests tagged `@Tag("slow")`. Run in CI, skippable locally via Gradle task filtering.

**TLA+** (design-level verification) — adopted per ADR-014:
- TLC model checker for exhaustive state space exploration
- Specs in `specs/tla/` (versioned with code)
- Scope: state machines, DAG invariants, materialization consistency, traceability completeness
- Complements code-level verification (JML/OpenJML) with design-level verification
- Higher ROI than expanding OpenJML ESC to additional classes

**KeY** (future): Formal proofs for L3 code. Deferred until L1 and L2 coverage is established.

### Drift Detection

During pre-alpha, the drift detection bar is relaxed:

1. **L1+ contracts should have at least one test** covering the contracted behavior. The strict two-tests-per-contract rule (happy-path + violation) applies post-alpha.
2. **OpenJML ESC re-verification** runs in CI on the `state/` package.
3. **L2 property tests** (jqwik) run in CI, tagged `@Tag("slow")` so they can be skipped locally for fast iteration.

Existing JML contracts without tests are acceptable during pre-alpha — they still serve as documentation. The post-alpha process will require test coverage for all contracts.

Repo-native guardrails complement the formal-methods toolchain. They enforce that workflow, migration, controller, and ADR changes stay synchronized with the documents and policy artifacts that make the methodology legible to both humans and agents.

### Relationship to TDD

SDD extends TDD by adding contracts as a specification layer:

- **TDD**: "Write a failing test, then make it pass."
- **SDD (post-alpha)**: "Write a failing contract, write a test that exercises it, then make both pass."
- **Pre-alpha**: Write code, add contracts where they prevent silent corruption, write tests for significant behaviors. Pragmatism over ceremony.

## Consequences

### Positive

- Bugs surface at contract boundaries with clear error messages, not as downstream corruption
- Spec-first catches design errors before code is written
- L1 default ensures contracts are written as a matter of course, not as an afterthought
- AI-generated code is validated against a second specification channel (contracts), catching implementation drift
- OpenJML ESC finds contract violations statically, without running the code
- JML contracts serve as executable documentation
- Single annotation language (JML) spans L1 through L3 — no tool-boundary translation

### Negative

- CI time increases with OpenJML ESC verification
- More JML annotations to maintain than with an L0 default
- jqwik property tests are slower than example-based JUnit tests

### Risks

- OpenJML + Hibernate proxies: JPA entity proxies may interfere with JML runtime checks. Mitigated by scoping RAC to domain logic methods, not framework-generated code.
- Over-contracting L0 code: enthusiasm may lead to contracts on configuration or DTOs. Mitigated by explicit L0 classification in the decision table — if it matches an L0 rule, do not contract it.

## Implementation Status

- Assurance levels L0-L3 are codified as the `AssuranceLevel` enum in `domain/verification/state/AssuranceLevel.java`.
- Verification outcomes are codified as the `VerificationStatus` enum in `domain/verification/state/VerificationStatus.java`.
- Both enums are used by the `VerificationResult` entity (ADR-014 §2, GC-F001).

## Related ADRs

- **ADR-013** (Java/Spring Boot Backend Rewrite) — Current tool names (JML, OpenJML, jqwik, KeY) for Ground Control's own code.
- **ADR-014** (Pluggable Verification Architecture) — Generalizes assurance levels from JML-specific to pluggable. The methodology defined here is universal; the tools per level depend on the target language. Adds TLA+ at L2 for design-level verification.
