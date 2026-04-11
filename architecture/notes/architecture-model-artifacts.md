# Architecture Model Artifacts

## Context

`GC-J002` adds support for C4 architecture models, architecture tests, and fitness functions as managed, versioned artifacts in the traceability graph.

The repository already has two relevant primitives:

- `TraceabilityLink` in the requirements domain for requirement-to-artifact links
- `VerificationResult` in the verification domain for execution outcomes tied to a traceability target

Those existing primitives are the default implementation path. `GC-J002` should not introduce a parallel artifact model unless a later requirement explicitly needs architecture artifacts to become first-class internal entities.

## Guardrails

### 1. Reuse existing artifact and verification surfaces

- Treat architecture artifacts as repo-versioned artifacts identified by stable repo-relative paths or existing Ground Control UIDs.
- Use `TraceabilityLink` for requirement linkage and reverse lookup.
- Use `VerificationResult` for recorded outcomes of architecture tests or formal fitness checks.
- Keep Git as the version store for artifact content. Ground Control stores metadata and traceability, not duplicate file bodies or version histories.

### 2. Keep architecture artifacts external unless they need first-class graph semantics

The mixed graph currently materializes first-class internal entities (`REQUIREMENT`, `CONTROL`, `RISK_SCENARIO`, `VERIFICATION_RESULT`, etc.). External artifacts remain requirement-anchored traceability targets.

Do not add:

- a new `GraphEntityType` for architecture artifacts
- a new artifact aggregate root or repository
- a second graph-projection path for files already represented by traceability links

If a future requirement needs architecture artifacts to have their own lifecycle, metadata schema, or graph edges independent of a requirement anchor, that is a new ADR-worthy decision.

### 3. Classify artifacts by existing semantics, not by new labels

Prefer the existing taxonomy:

- C4 model source files: `ArtifactType.SPEC` with repo-relative `artifactIdentifier`
- Rendered/static architecture diagrams with no executable source semantics: `ArtifactType.DOCUMENTATION`
- Architecture tests: `ArtifactType.TEST` with `LinkType.TESTS`
- Fitness functions implemented as executable tests: `ArtifactType.TEST` with `LinkType.TESTS`
- Fitness functions implemented as policies or rules: `ArtifactType.POLICY` or `ArtifactType.CONFIG` with `LinkType.CONSTRAINS`
- Formal architecture verification artifacts/results: `ArtifactType.SPEC` or `ArtifactType.PROOF` with `LinkType.VERIFIES`, plus `VerificationResult` when an execution outcome is recorded

Do not add `ARCHITECTURE_MODEL`, `ARCHITECTURE_TEST`, or `FITNESS_FUNCTION` enum values unless the existing taxonomy proves materially insufficient across API, MCP, frontend, and persistence layers.

### 4. Reuse the existing cross-cutting concerns

- Validation: keep request-shape validation in DTOs and business-rule validation in domain services via `DomainValidationException`
- Exception mapping: reuse `GlobalExceptionHandler`; do not introduce a feature-specific error envelope
- Write ownership: keep requirement-artifact writes in `TraceabilityService`; keep verification persistence in `VerificationResultService`
- Persistence: if a new audited entity ever becomes unavoidable, follow `BaseEntity` + JPA + Flyway + Envers patterns rather than ad hoc tables
- Logging: use existing structured SLF4J event-style logs with stable keys, and rely on `RequestLoggingFilter` for request correlation
- Architecture enforcement: preserve `api -> domain <- infrastructure` and existing ArchUnit rules

## Implementation Gotchas

- `artifactIdentifier` matching is exact. Competing encodings for the same file or artifact will fragment reverse lookup and sync behavior.
- If new `ArtifactType` values are introduced, the implementation must update backend enums, API docs, MCP enum contracts, frontend enum mirrors, and tests together. Avoid that unless there is no fit in the current taxonomy.
- `VerificationResult.target` already points at a `TraceabilityLink`. Do not bypass that relationship with direct file/path foreign keys or a second verification target schema.
- A C4 DSL/parser abstraction is not justified up front. Start with stable file-path traceability and only generalize if multiple concrete model formats actually need shared behavior.

## Non-Goals

- No blob storage or database-backed content store for architecture artifacts
- No generic artifact registry spanning all repo files
- No new workflow engine for architecture checks separate from existing traceability and verification flows
- No attempt to parse, render, or normalize every architecture modeling format as part of `GC-J002`
