# Evidence Derivation And Temporal State History Preflight

Issue: #725
Requirement: GC-M016

This is architecture guardrail guidance for implementing evidence derivation
and temporal assurance history. It is not an implementation plan.

## Boundary

GC-M016 needs three layers to remain distinguishable:

- **Current observed state**: a projection over observation history, such as
  "latest non-expired observation for asset/category/key." This is a read model,
  not the only record.
- **Historical observations**: point-in-time facts from assets, tests, scans, or
  attestations. The observed value, source, observed-at time, and evidence
  reference are part of the historical fact and must not be overwritten to mean
  a new observation.
- **Summarized evidence or assurance conclusions**: durable artifacts derived
  from observations, control tests, attestations, verification results, and risk
  assessments. A summary has its own derivation time, inputs, method/profile,
  confidence/assurance, and consumer-facing identity; it must not erase or
  replace the source facts.

Envers audit history is mutation provenance, not the business history model for
observations or assurance. Use Envers to answer who changed a row and when; use
domain rows and links to answer what was observed, what was derived from it, and
what conclusion was current at a given point in time.

## Incumbents To Reuse

- `Observation`, `ObservationService`, and `ObservationRepository` already own
  asset state facts, `observedAt` / `expiresAt`, project-scoped lookup, and the
  `listLatest` current-state projection. Do not create a second observation
  schema for asset/system facts.
- `ControlTest` and `ControlEffectivenessAssessment` already own control test
  evidence and control assurance ratings. Do not reinterpret them as generic
  observations or collapse them into `Control.effectiveness`.
- `VerificationResult` already owns prover/tool evidence for requirements and
  traceability targets. Use its existing common schema and `evidence` payload for
  verifier output; do not add per-prover evidence tables or a second verifier
  result hierarchy.
- `RiskAssessmentResult` already owns methodology-scoped risk conclusions,
  observation links, evidence refs, approval state, and method-specific input /
  output payloads. Do not write risk conclusions from evidence controllers or
  duplicate residual-risk storage.
- `Finding` / `FindingLink`, `AssetLink`, `RiskScenarioLink`, `ControlLink`,
  and `GraphTargetResolverService` already define same-project link validation
  for first-class internal targets versus external evidence identifiers.
- `TraceabilityLink`, `ArtifactType`, and `LinkType` remain the requirement
  traceability contract. Reuse ADR-011 identifier conventions and reverse lookup
  rather than inventing evidence-specific artifact encodings.
- `StatusDriftService` is the canonical example of read-only derived evidence:
  it derives findings from project-scoped canonical data without mutating
  lifecycle state, shelling out, or reading global caches.
- `BaseEntity`, audited JPA entities, Flyway migrations plus `_audit` tables,
  `AuditRetentionJob.AUDIT_TABLES`, and `MigrationSmokeTest` are the persistence
  pattern for durable business entities whose history matters.
- `JacksonTextCollectionConverters` is the existing JSON/TEXT persistence helper
  for structured payloads. Do not instantiate feature-local `ObjectMapper`
  parsers for evidence payloads.

## Cross-Cutting Layers

- **Security path**: endpoints must stay under `/api/v1/**` and use the existing
  ADR-026 chains: `IpAllowlistFilter`, `BearerTokenAuthFilter`, centralized
  `ApiPathMatrix` authorization, then `ActorFilter`. Do not add feature-local
  auth checks, caller-supplied actor fields, or routes outside the matrix.
- **Actor provenance**: `ActorFilter`, `ActorHolder`, MDC `actor_id`, Envers
  revision metadata, and `GroundControlRevisionListener` remain the audit actor
  mechanism. Domain provenance fields such as source system, tester, assessor,
  analyst, or attestor identify who performed work; they do not spoof who wrote
  the row.
- **Request validation**: DTOs own `@Valid` shape checks, sizes, required
  fields, enum parsing, and collection bounds. Services own same-project
  validation, source-target compatibility, duplicate derivation semantics,
  temporal checks, evidence input existence, and method/profile compatibility.
- **Error envelope**: use `NotFoundException`, `ConflictException`, and
  `DomainValidationException` with structured detail. `GlobalExceptionHandler`
  and `shared.web.ErrorResponse` are the only HTTP error contract. Do not echo
  raw evidence payloads, secrets, tokens, scan output, full test results, or
  attestation blobs in messages or detail maps.
- **Logging and observability**: use SLF4J with stable low-cardinality event
  names and IDs. Log derived artifact IDs, source counts, method/profile IDs,
  and project IDs where useful; never log bearer tokens, raw request bodies,
  raw observations, full evidence payloads, verifier output, or attestation
  content.
- **Configuration and OS/runtime exposure**: GC-M016 should not require new
  secrets, subprocesses, shell-outs, network clients, or CLI argv. Any later
  external collector or verifier adapter must use `@ConfigurationProperties`
  with validation, bounded execution, and secret-safe process invocation.
- **Graph and links**: first-class derived artifacts need `GraphEntityType`,
  `GraphIds`, a projection contributor, and same-project validation through the
  existing resolver/link services. Do not write AGE rows directly from
  controllers or bypass source-of-truth JPA persistence.
- **Frontend/API enum mirrors**: any API-visible enum for source type, evidence
  type, derivation method, assurance status, confidence, or graph entity must
  follow ADR-034 single-source enum policy and update frontend/MCP mirrors and
  tests where applicable.
- **Workflow gates**: controller changes need `@WebMvcTest`; semantic rules need
  service tests; graph/link changes need projection/resolver tests; migrations
  need smoke coverage; repo work completes through `make policy`.

## Extensibility Seams

The source-reference seam belongs at the derivation boundary: support internal
sources via validated project-scoped entity IDs, and external/unmodeled sources
via canonical identifiers. The shape must be parameterized by source kind and
role so a future attestation, scanner observation, control test, verification
result, or manual review does not require a new controller branch or duplicate
link table.

The derivation-method seam belongs in domain data, not controllers: capture the
method/profile/version that produced a summary, the derivation timestamp, the
input snapshot references, and confidence or assurance. New summarizers should
add method/profile data and validation rules rather than changing source facts.

The current-state seam is a projection over history. Consumers should be able to
ask for latest/current state independently from historical rows and summarized
evidence artifacts. Do not make "current" mean "the row was updated in place."

## Gotchas And Anti-Patterns

- Do not use Envers audit tables as the primary query path for observation or
  assurance history. Envers is mutation audit; business history needs durable
  domain rows.
- Do not mutate `Observation.observationValue`, `observedAt`, source, or source
  evidence to represent a new fact. Append a new observation or create an
  explicit correction/amendment concept with provenance.
- Do not collapse observations, tests, attestations, verification results, risk
  assessments, findings, and evidence summaries into one polymorphic blob.
- Do not duplicate JSON parsing, validation, exception hierarchies, security
  filters, graph materializers, traceability schemas, or workflow logic.
- Do not infer assurance from lifecycle status alone: `ControlStatus`,
  requirement `Status`, and risk approval state are not evidence by themselves.
- Do not let a summarized evidence artifact become an untraceable cache. It must
  retain source references and derivation metadata sufficient to defend the
  conclusion later.
- Do not read project- or repo-unscoped caches from a project-scoped derivation
  path, and do not shell out or scan the filesystem from domain analysis.
- Do not encode first-class internal targets as raw strings when a
  project-scoped UUID target exists.
- Do not expose raw attestation/test/verifier payloads through error envelopes,
  logs, or broad list responses without a deliberate redaction contract.

## Non-Goals

- No implementation of GC-M016 behavior, migrations, controllers, MCP tools,
  frontend views, graph projection, or derivation engine in this preflight.
- No replacement of `Observation`, `ControlTest`,
  `ControlEffectivenessAssessment`, `VerificationResult`,
  `RiskAssessmentResult`, `Finding`, or `TraceabilityLink`.
- No external evidence ingestion adapter, background scheduler, ticketing
  integration, verifier adapter, or workflow engine.
- No change to ADR-026 security, ADR-033 actor provenance, ADR-034 enum policy,
  ADR-035 MCP curation, or repo workflow gates.
