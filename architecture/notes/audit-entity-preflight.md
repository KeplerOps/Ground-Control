# Audit Entity Preflight

Issue: #275
Requirement: GC-U001

This note records architecture guardrails for the upcoming Audit entity. It is
not an implementation plan.

## Boundary

GC-U001 is an audit-management aggregate: an internal, external, regulatory, or
special audit engagement with scope, objectives, timeline phases, assigned team,
and a status lifecycle. It is not the existing mutation audit trail.

Keep the business aggregate separate from:

- `api/audit/AuditController` and `domain/requirements/service/AuditService`,
  which expose Envers-backed requirement / traceability timeline history.
- `domain/audit`, `ActorHolder`, `GroundControlRevisionEntity`, and
  `GroundControlRevisionListener`, which own authenticated actor provenance for
  all audited mutations.
- `Finding`, which is the governed deficiency record that an audit may produce
  or follow up on.
- `EvidenceArtifact`, `Observation`, `ControlTest`, and
  `ControlEffectivenessAssessment`, which are evidence / testing records an
  audit may cite.
- `RiskRegisterRecord`, `RiskScenario`, `RiskAssessmentResult`, and
  `TreatmentPlan`, which own risk framing, analysis, decisions, and treatment.

Prefer a new `domain/audits` boundary for the business aggregate and keep the
existing `domain/audit` package reserved for cross-cutting Envers actor
infrastructure. REST routes should use `/api/v1/audits/**`; do not overload
the existing `/api/v1/audit/**` timeline route.

## Incumbents To Reuse

- **Domain shape:** follow the audited aggregate pattern: `model`, `state`,
  `repository`, and `service` packages; entities extend `BaseEntity`, are
  project-scoped through `Project`, and mark the project reference
  `@NotAudited`.
- **Persistence:** use Flyway for the canonical table plus `_audit` shadow,
  project-scoped UID uniqueness, status/team/date indexes where queries need
  them, and the same migration smoke / retention-table updates used by
  `Finding`, `EvidenceArtifact`, and `TestPlan`.
- **Status lifecycle:** model `PLANNED -> IN_PROGRESS -> DRAFT_REPORT ->
  FINAL_REPORT -> CLOSED` as a dedicated enum with `validTargets()` /
  `canTransitionTo()`. The lifecycle is workflow logic; classify it at least
  as L2 per `docs/CODING_STANDARDS.md` and cover the transition matrix.
- **Timeline phases:** represent planning, fieldwork, reporting, and follow-up
  as audit-domain phase data, not requirement status, finding status, or Envers
  revisions. If phase dates are stored as structured data, reuse
  `JacksonTextCollectionConverters` rather than feature-local JSON parsing.
- **Links:** use an audit-owned dual-mode link surface for outbound links to
  frameworks, assets, controls, risk scenarios / records, evidence, and prior
  findings. Follow the `targetEntityId` / `targetIdentifier` convention used by
  `FindingLink`, `ControlLink`, `RiskScenarioLink`, and `ThreatModelLink`.
  For compliance framework context, first reuse existing `ControlPack`,
  `ControlPackEntry`, and `frameworkMappings` where they represent installed
  framework/control content; otherwise treat framework references as external
  identifiers until a separate framework-catalog requirement exists.
- **Target validation:** extend `GraphTargetResolverService` with an
  audit-target validator for same-project internal targets. Internal references
  must resolve by UUID in the requested project; external or not-yet-modeled
  framework references use canonical string identifiers only. Do not bypass
  `ControlPack` / `ControlPackEntry` repositories for installed control-pack
  framework content.
- **Graph projection:** JPA remains the source of truth. Audit nodes and
  internal audit link edges belong in the existing mixed-graph projection via
  `GraphEntityType`, `GraphIds`, and `GraphProjectionContributor`. Do not write
  AGE rows from controllers or services.
- **API shape:** controllers stay thin, use records plus Bean Validation, and
  delegate semantic checks to services. Project resolution follows the existing
  `ProjectService.resolveProjectId` / `requireProjectId` convention chosen for
  comparable list/get endpoints.
- **MCP/frontend parity:** API-visible enum values and DTO fields must be
  mirrored through the existing frontend type and MCP tool patterns. If an enum
  becomes an ADR-034 inventory item, extend the inventory; otherwise add a
  focused mirror test like the existing entity-specific MCP tests.

## Cross-Cutting Layers

- **Security:** `/api/v1/audits/**` must remain under the shared path matrix.
  Bearer traffic passes `IpAllowlistFilter`, `BearerTokenAuthFilter`,
  authorization, and `ActorFilter`; browser traffic passes the ADR-037 session /
  CSRF chain and the same `ApiPathMatrix`. Do not add endpoint-local token,
  actor, or role fields.
- **Actor provenance:** Envers actor capture remains `ActorFilter` ->
  `ActorHolder` -> `GroundControlRevisionListener`. Team assignment is domain
  provenance; it does not replace the authenticated mutation actor.
- **Request parsing and validation:** Jackson owns UUID, date, and enum
  parsing. Bean Validation owns DTO shape, required fields, sizes, and
  collection bounds. Service/domain logic owns project scope, UID uniqueness,
  status-transition legality, phase date ordering, team-member shape, and
  internal/external link target rules.
- **Error envelope:** use `NotFoundException`, `ConflictException`, and
  `DomainValidationException` through `GlobalExceptionHandler` and
  `shared.web.ErrorResponse`. Details may name field paths, current / target
  statuses, and valid targets; they must not echo full scope narratives,
  objectives, evidence contents, tokens, cookies, headers, stack traces, or
  database constraint names.
- **Logging and observability:** use SLF4J lifecycle logs with low-cardinality
  fields such as audit UID/id, project identifier, status, audit type, and link
  counts. Let `RequestLoggingFilter`, MDC `request_id`, `ActorFilter`, and
  Envers provide request and actor context. Do not log scope descriptions,
  objectives, evidence payloads, raw team notes, bearer tokens, or request
  bodies.
- **Configuration and OS/runtime exposure:** GC-U001 should not require new
  secrets, environment variables, subprocesses, shell-outs, network clients,
  temp files, or process argv content. Any later calendar, ticketing, document,
  or evidence-ingestion integration must use validated `@ConfigurationProperties`
  and secret-safe invocation.
- **Policy and tests:** controller additions need matching `@WebMvcTest`;
  service rules need unit tests; lifecycle rules need transition/property
  coverage; target validation and projection need resolver/projection tests;
  migrations need smoke coverage; repo completion must run `make policy`.

## Extensibility

The load-bearing seam is the audit-owned link target inventory. The next
reasonable variation is promotion of currently external framework or artifact
references into first-class entities. That change should be a target enum /
resolver / projection edit, not a new link substrate or a JSON metadata escape
hatch.

Team assignment should also stay parameterized. Store assigned people or roles
as bounded audit-domain data for now; if users or groups become first-class
assignees later, migrate to internal IDs without changing audit identity,
status, or link semantics.

Timeline phases should allow future per-phase dates, owners, and completion
state without turning the audit lifecycle into a workflow engine. The audit
status remains the aggregate-level lifecycle; phases are planning detail.

## Gotchas And Anti-Patterns

- Do not use `domain/audit` for the GC-U001 aggregate; that package already
  means mutation-audit actor infrastructure.
- Do not overload `/api/v1/audit/**`; it already means audit trail / timeline.
- Do not implement audit findings by adding finding fields to the Audit
  aggregate. Use links to `Finding` records for prior findings and follow-up.
- Do not embed controls, assets, risks, evidence, or findings as free-text lists
  when first-class project-scoped targets exist.
- Do not reuse `TraceabilityLink` as the business audit-workpaper link table;
  traceability links remain requirement-to-artifact evidence.
- Do not conflate audit timeline phases with status lifecycle states or Envers
  revision history.
- Do not create duplicate validation frameworks, exception hierarchies, auth
  guards, graph writers, JSON parsers, logging channels, or workflow engines.
- Do not introduce a compliance-framework catalog solely for GC-U001 unless a
  separate requirement scopes that aggregate; external framework identifiers are
  acceptable until a first-class framework model exists.

## Non-Goals

- No implementation of GC-U001 in this preflight.
- No replacement of the Envers audit trail, actor provenance, or requirement
  audit timeline.
- No first-class compliance-framework catalog, workpaper repository, audit task
  workflow engine, calendar integration, ticketing integration, or UI workflow.
- No changes to finding, evidence, control-test, risk, asset, or traceability
  semantics except the link-target extensions needed for the Audit aggregate.
