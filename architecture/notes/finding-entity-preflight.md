# Finding Entity Preflight

Issue: #279
Requirement: GC-V001

This is architecture guardrail guidance for implementation of GC-V001. It is
not an implementation plan.

## Boundary

`Finding` is the durable GRC issue record produced by audits, control tests,
policy checks, vulnerability analysis, exception review, and other assessment
activity. It owns finding type, severity, description, root-cause analysis,
status lifecycle, owner, due date, and the set of affected controls, risks,
operational assets, artifacts, observations, evidence, audits, and remediation
plans.

It must stay separate from:

- `Observation`, which is raw or analyst-captured evidence about an asset.
- `Control`, which is the expected governance or technical safeguard.
- `RiskScenario`, `RiskRegisterRecord`, `RiskAssessmentResult`, and
  `TreatmentPlan`, which own risk framing, assessment, register governance, and
  treatment decisions.
- `ThreatModel`, which owns upstream threat-analysis context.
- `TraceabilityLink`, `AssetLink`, `ControlLink`, `RiskScenarioLink`, and
  `ThreatModelLink`, which are link surfaces, not finding records.
- Verification or sweep findings in analysis reports, which are derived
  read-only outputs unless explicitly promoted to a first-class `Finding`.

Do not implement GC-V001 by adding finding fields onto an adjacent aggregate or
by treating every validation warning, sweep result, or observation as a finding.

## Incumbents To Reuse

- Domain shape: create `domain/findings/` with the existing `model`, `state`,
  `service`, and `repository` package pattern. Audited entities extend
  `BaseEntity`, are project-scoped through `Project`, and use Flyway migrations
  plus audit-table parity.
- REST shape: controllers stay thin, use request/response records, `@Valid`,
  Jackson enum binding, and `ProjectService.resolveProjectId` /
  `requireProjectId`; semantic validation belongs in services.
- Link shape: use one finding-owned link surface for finding outbound links,
  following the dual-mode `targetEntityId` / `targetIdentifier` shape used by
  `ThreatModelLink`, `RiskScenarioLink`, `ControlLink`, and `AssetLink`. Do not
  scatter finding links across multiple owner tables as the only source of
  truth for a finding.
- Internal target validation: extend `GraphTargetResolverService` for
  finding-owned links and update existing link target switches so `FINDING`
  becomes an internal target once the entity exists. `targetEntityId` is for
  first-class in-project entities; `targetIdentifier` is only for external or
  not-yet-modeled artifacts.
- Existing inbound placeholders: `AssetLinkTargetType`, `ControlLinkTargetType`,
  and `RiskScenarioLinkTargetType` already include `FINDING` as an unmodeled
  external target. Converting it to a modeled internal target must update the
  resolver, duplicate checks, reverse lookups, graph projection, and tests
  together.
- Graph: JPA remains the source of truth. Finding nodes and internal link edges
  belong in the mixed-graph path via `GraphEntityType`, `GraphIds`,
  `GraphProjectionContributor`, and `MixedGraphService`; do not add a bespoke
  finding graph endpoint or direct AGE writes from controllers or services.
- Status lifecycle: model `open -> remediation-in-progress ->
  remediation-complete -> verified-closed` as the same enum transition pattern
  used by `ControlStatus`, `RiskScenarioStatus`, `ThreatModelStatus`, and
  `TreatmentPlanStatus`. Because this is workflow logic, classify it as L2 per
  `docs/CODING_STANDARDS.md` and cover transition properties.
- Enum contract: if finding enums are mirrored to the frontend or MCP, treat the
  backend Java enum as semantic authority and extend the ADR-034
  `ENUM_CONTRACT_INVENTORY` gate instead of hand-maintaining unchecked mirrors.
- Audit and observability: use Envers, `ActorFilter`, `ActorHolder`,
  `GroundControlRevisionListener`, `RequestLoggingFilter`, and existing SLF4J
  lifecycle logs. Do not accept authenticated actor identity from request
  bodies.

## Cross-Cutting Layers

- Security: finding REST routes must live under `/api/v1/**` so both bearer and
  browser-session traffic pass the shared security path matrix. Bearer callers
  pass `IpAllowlistFilter`, `BearerTokenAuthFilter`, Spring authorization, and
  `ActorFilter`; browser callers pass the ADR-037 browser chain and the same
  `ApiPathMatrix`. Do not add endpoint-local auth, caller-supplied tokens, or
  routes outside the matrix.
- Request parsing and validation: Jackson enum binding rejects unknown finding
  type, severity, status, and link enum values. Bean Validation owns DTO shape
  such as required fields and size caps. Service validation owns project scope,
  status transitions, due-date business rules, duplicate UID/link detection, and
  internal-vs-external target semantics.
- Error envelope: use `NotFoundException`, `ConflictException`, and
  `DomainValidationException`; `GlobalExceptionHandler` and
  `shared.web.ErrorResponse` remain the only HTTP error contract. Error details
  may name field paths or target types, but must not echo raw descriptions,
  root-cause narratives, evidence payloads, tokens, headers, or stack traces.
- Persistence and migration: use PostgreSQL/Flyway with explicit indexes for
  project-scoped UID lookup, owner/status/due-date work queues, and link reverse
  lookups. Audit tables must track finding and finding-link mutations, and
  migration smoke coverage must include the new tables.
- OS/runtime exposure: GC-V001 should not require new secrets, environment
  variables, subprocesses, shell-outs, network clients, or CLI arguments. Any
  later scanner, ticketing, or evidence-ingestion integration must use
  `@ConfigurationProperties` with startup validation and keep secrets out of
  argv, logs, database narrative fields, and error envelopes.
- Frontend/API/MCP schema surfaces: API docs, frontend types/constants, and MCP
  schemas must mirror the backend DTOs and enums. Reuse existing translation and
  request helpers rather than adding finding-specific serializers, auth-header
  logic, or error parsers.
- Tests and policy: controller changes require matching `@WebMvcTest`; service
  rules need unit tests; lifecycle rules need state/property tests; link and
  graph behavior need resolver/projection tests; migrations need smoke coverage;
  `make policy` remains the repo-native completion guardrail.

## Extensibility

The primary seam is the finding-owned link target inventory. The next modeled
entity in this direction is likely first-class evidence, audit records, or
remediation plans. Adding one should require extending the target enum,
`GraphTargetResolverService`, graph projection, and tests, not creating a new
link substrate or tunneling typed targets through a generic metadata column.

The second seam is lifecycle ownership. Remediation-plan workflow may later own
task execution and verification evidence, but the finding should keep its own
status as the summary disposition of the issue. Do not let remediation-plan
states replace the finding lifecycle or vice versa.

## Gotchas And Anti-Patterns

- Do not conflate findings with observations; observations can support a
  finding, but they are not the governed deficiency record.
- Do not conflate findings with vulnerabilities; vulnerability is a finding
  type, not the whole aggregate.
- Do not store affected controls, risks, assets, observations, evidence, audits,
  or remediation plans only as free-text lists when first-class records exist.
- Do not keep `FINDING` as an external/unmodeled link target once the entity is
  first-class; that would make graph traversal and reverse lookup inconsistent.
- Do not copy the older raw-string-only link pattern. Use the current dual-mode
  internal/external target model and project-scoped resolver.
- Do not introduce duplicate exception hierarchies, validation frameworks,
  workflow engines, graph writers, audit writers, auth guards, or logging
  channels.
- Do not log full descriptions, root-cause analysis, evidence blobs, bearer
  tokens, or authorization headers.
- Do not add generic `metadata` as an escape hatch for severity, lifecycle,
  affected entities, root cause, owner, or due date.

## Non-Goals

- No implementation of GC-V001 in this preflight.
- No first-class evidence, audit, or remediation-plan aggregate unless a linked
  requirement explicitly scopes it.
- No scanner, ticketing, notification, workflow-engine, or external-ingestion
  integration.
- No replacement of controls, risks, observations, threat models, verification
  results, asset links, risk links, control links, or traceability links with a
  generic all-purpose finding graph.
